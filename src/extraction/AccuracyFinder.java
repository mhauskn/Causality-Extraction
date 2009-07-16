package extraction;

import java.util.ArrayList;

import mallet.Include;
import haus.io.FileReader;

public class AccuracyFinder {
	//FileReader ansR = new FileReader("crf/crf_bare.txt");
	FileReader ansR = new FileReader("crf/100_bare.txt");
	//FileReader outR = new FileReader("crf/collective2.txt");
	FileReader outR = new FileReader("crf/heuristic.txt");
	String[] labels, output;
	
	Evaluator lPhrasal = new Evaluator("Loose Phrasal");
	Evaluator ePhrasal = new Evaluator("Exact Phrasal");
	Evaluator tok = new Evaluator("Token");
	Evaluator ftok = new Evaluator("Focused Token");
	
	public void run () {
		ArrayList<String> ansToks = new ArrayList<String>(), 
			outToks = new ArrayList<String>();
		String ansS = ansR.getNextLine(), outS = outR.getNextLine();
		while (ansS != null && outS != null) {
			ansToks.add(Include.getLabel(ansS));
			//outToks.add(Include.getToken(outS));
			outToks.add(Include.getLabel(outS));
			
			if (Include.hasSentDelim(ansS)) {
				ansToks.remove(ansToks.size()-1);
				outToks.remove(outToks.size()-1);
				
				labels = haus.misc.Conversions.toStrArray(ansToks);
				output = haus.misc.Conversions.toStrArray(outToks);
				
				doEval();
				
				ansToks.clear();
				outToks.clear();
			}
			ansS = ansR.getNextLine(); outS = outR.getNextLine();
		}
	}
	
	void doEval () {
		int[] acp,aep,ocp,oep;
		acp = getIndex(labels,Include.CAUSE_TAG);
		aep = getIndex(labels,Include.EFFECT_TAG);
		ocp = getIndex(output,Include.CAUSE_TAG);
		oep = getIndex(output,Include.EFFECT_TAG);
		
		if (acp == null || aep == null) {
			System.out.println("Nulled Answer -- Need to fix the corpus!");
			return;
		}
				
		lPhrasal.getLoosePhrasalAgreement(acp, aep, ocp, oep);
		ePhrasal.getExactPhrasalAgreement(acp, aep, ocp, oep);
		tok.getTokenAgreement(output, labels);
	}
	
	/**
	 * Gets an int[] index for a given label type. Essentially match starts
	 * with first token of that label type and ends with last token of the label
	 * type
	 */
	public static int[] getIndex (String[] feats, String toMatch) {
		int start = -1;
		int end = -1;
		boolean running = false;
		for (int i = 0; i < feats.length; i++) {
			String feat = feats[i];
			if (feat.startsWith(toMatch)) {
				if (!running && start == -1)
					start = i;
				running = true;
			} else {
				if (running)
					end = i -1;
				running = false;
			}
		}
		if (running)
			return new int[] { start, feats.length-1};
		if (start != -1 && end != -1)
			return new int[] { start, end};
		return null;
	}
	
	public static void main (String[] args) {
		AccuracyFinder f = new AccuracyFinder();
		f.run();
		f.lPhrasal.print();
		f.ePhrasal.print();
		f.tok.print();
		f.ftok.print();
	}
}

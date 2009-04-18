package extraction;

import java.util.ArrayList;

import mallet.Include;
import haus.io.FileReader;

public class AccuracyFinder {
	FileReader ansR = new FileReader("crf/crf_bare.txt");
	FileReader outR = new FileReader("crf/collective2.txt");
	int correct = 0, incorrect = 0, reversed = 0; 
	String[] ans, out;
	
	void run () {
		ArrayList<String> ansToks = new ArrayList<String>(), 
			outToks = new ArrayList<String>();
		String ansS = ansR.getNextLine(), outS = outR.getNextLine();
		while (ansS != null && outS != null) {
			ansToks.add(Include.getLabel(ansS));
			outToks.add(Include.getToken(outS));
			
			if (Include.hasSentDelim(ansS)) {
				ansToks.remove(ansToks.size()-1);
				outToks.remove(outToks.size()-1);
				
				ans = haus.misc.Conversions.toStrArray(ansToks);
				out = haus.misc.Conversions.toStrArray(outToks);
				
				checkAnswer();
				
				ansToks.clear();
				outToks.clear();
			}
			ansS = ansR.getNextLine(); outS = outR.getNextLine();
		}
	}
	
	void checkAnswer () {
		int[] acp, ocp, aef, oef;
		acp = getIndex(ans,Include.CAUSE_TAG);
		aef = getIndex(ans,Include.EFFECT_TAG);
		ocp = getIndex(out,Include.CAUSE_TAG);
		oef = getIndex(out,Include.EFFECT_TAG);
		
		if (ocp == null || oef == null) {
			System.out.println("Nulled!");
			incorrect++;
			return;
		}
		
		if (!Reln.seperate(acp,ocp) && !Reln.seperate(aef,oef) &&
				Reln.seperate(acp, oef) && Reln.seperate(aef, ocp)) {
			correct++;
		} else if (!Reln.seperate(acp,oef) && !Reln.seperate(aef,ocp) &&
				Reln.seperate(acp, ocp) && Reln.seperate(aef, oef)) {
			incorrect++;
			reversed++;
		} else {
			incorrect++;
		}
	}
	
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
		System.out.println("correct " + f.correct + " incorrect " + f.incorrect + " reversed " + f.reversed);

	}
}

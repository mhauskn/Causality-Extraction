package extraction;

import haus.io.Closer;
import haus.io.DataWriter;
import haus.io.FileReader;
import haus.io.IO;

import java.util.ArrayList;

import parser.Stanford.StanfordParser;

import mallet.Include;

/**
 * Extract Cause Phrase and Effect Phrase based
 * upon simple heuristics
 */
public class CpEpExtractor extends IO<String,String> {
	//public static String file_in = "crf/wrong.txt";
	public static String file_in = "crf/crf_bare.txt";
	//public static String file_in = "crf/test.txt";
	public static String file_out = "crf/extracted.txt";
	
	//DataWriter wrong = new DataWriter("crf/wrong2.txt");
	//DataWriter nulled = new DataWriter("crf/nulled.txt");
	DataWriter eagerw = new DataWriter("crf/eager.txt");
	DataWriter verbalw = new DataWriter("crf/verbal.txt");
	DataWriter nonverbalw = new DataWriter("crf/nonverbal.txt");

	
	int cnt = 0;
	
	String[] toks, feats, labels, output;
	
	ArrayList<String> _toks = new ArrayList<String>();
	ArrayList<String> _feats = new ArrayList<String>();
	ArrayList<String> _labels = new ArrayList<String>();
	
	String last_tok, last_feat, last_label;
	
	EagerReln eager = new EagerReln();
	VerbalReln verbal = new VerbalReln();
	NonVerbalReln nonVerbal = new NonVerbalReln();
	
	int correct = 0, incorrect = 0; 
	int eageri = 0, verbali = 0, nonVerbali = 0;
	int eagerf = 0, verbalf = 0, nonVerbalf = 0;
	String type;
	
	/**
	 * Map function for our input. Gets one sentence and divides it 
	 * into different parts.
	 */
	public void map (String line) {
		_toks.add(Include.getToken(line));
		_feats.add(Include.getFeature(line) + " ");
		_labels.add(Include.getLabel(line));
		
		if (Include.hasSentDelim(line)) {
			last_tok = _toks.remove(_toks.size()-1);
			last_feat = _feats.remove(_feats.size()-1);
			last_label = _labels.remove(_labels.size()-1);
			
			toks = haus.misc.Conversions.toStrArray(_toks);
			feats = haus.misc.Conversions.toStrArray(_feats);
			labels = haus.misc.Conversions.toStrArray(_labels);
			
			extractCpEp();
			
			_toks.clear();
			_feats.clear();
			_labels.clear();
		}
	}
	
	public void extractCpEp () {
		if (eager.matchesPattern(toks, feats)) {
			output = eager.extract();
			eageri++;
			type = "eager";
		} else if (verbal.matchesPattern(toks, feats)) {
			output = verbal.extract();
			verbali++;
			type = "verbal";
		} else if (nonVerbal.matchesPattern(toks, feats)) {
			output = nonVerbal.extract();
			nonVerbali++;
			type = "nonverbal";
		}
		//for (int i = 0; i < toks.length; i++)
		//	out.add(toks[i] + " " + output[i] + " " + labels[i]);
		checkAnswer();
	}
	
	void checkAnswer () {
		int[] acp, ocp, aef, oef;
		acp = getIndex(labels,Include.CAUSE_TAG);
		aef = getIndex(labels,Include.EFFECT_TAG);
		ocp = getIndex(output,Include.CAUSE_TAG);
		oef = getIndex(output,Include.EFFECT_TAG);
		
		if (acp == null || aef == null || ocp == null || oef == null) {
			System.out.println("Nulled!");
			incorrect++;
			printSent();
			writeWrong();
			return;
		}
		
		if (!Reln.seperate(acp,ocp) && !Reln.seperate(aef,oef) &&
				Reln.seperate(acp, oef) && Reln.seperate(aef, ocp)) {
			System.out.println("Correct!");
			correct++;
			return;
		}
		incorrect++;
		writeWrong();
		printSent();
	}
	
	public static int[] getIndex (String[] feats, String toMatch) {
		int start = -1;
		boolean running = false;
		for (int i = 0; i < feats.length; i++) {
			String feat = feats[i];
			if (feat.startsWith(toMatch)) {
				if (!running)
					start = i;
				running = true;
			} else
				if (running)
					return new int[] { start, i-1 };
		}
		if (running)
			return new int[] { start, feats.length-1};
		return null;
	}
	
	void writeWrong () {
		if (type.equals("eager")) {
			writeSent(eagerw);
			eagerf++;
		} else if (type.equals("verbal")) {
			writeSent(verbalw);
			verbalf++;
		} else {
			writeSent(nonverbalw);
			nonVerbalf++;
		}
	}
	
	void writeSent (DataWriter w) {
		for (int i = 0; i < toks.length; i++)
			w.writeln(toks[i] + " " + feats[i] + " " + labels[i] + " " + output[i]);
		w.writeln(last_tok + " " + last_feat + " " + last_label);
	}
	
	void printSent () {
		for (String s : toks)
			System.out.print(s + " ");
		System.out.println();
	}
	
	public static void main (String[] args) {
		Closer c = new Closer();
		CpEpExtractor ext = new CpEpExtractor();
		StanfordParser.getStanfordParser(c);
		ext.setInput(new FileReader(file_in));
		ext.setOutput(new DataWriter(file_out, c));

		ext.mapInput();
		c.close();
		System.out.println("correct " + ext.correct + " incorrect " + ext.incorrect);
		System.out.println("Occurences: eager " + ext.eageri + " verbal " + ext.verbali + " nonverbal " + ext.nonVerbali);
		System.out.println("Fails: eager " + ext.eagerf + " verbal " + ext.verbalf + " nonverbal " + ext.nonVerbalf);
		ext.verbalw.close();
		ext.eagerw.close();
		ext.nonverbalw.close();
	}
}

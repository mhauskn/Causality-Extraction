package extraction;

import haus.io.Closer;
import haus.io.DataWriter;
import haus.io.FileReader;
import haus.io.IO;

import java.util.ArrayList;
import java.util.Hashtable;

import parser.Stanford.StanfordParser;
import stream.CRFDataStream;

import mallet.Include;

/**
 * Extract Cause Phrase and Effect Phrase based
 * upon simple heuristics
 */
public class CpEpExtractor extends IO<String,String> {
	//public static String file_in = "crf/crf_bare.txt";
	public static String file_in = "crf/100_bare.txt";
	public static String file_out = "crf/heuristic.txt";

	int cnt = 0;
	
	String[] toks, feats, labels, output;
	
	ArrayList<String> _toks = new ArrayList<String>();
	ArrayList<String> _feats = new ArrayList<String>();
	ArrayList<String> _labels = new ArrayList<String>();
	
	String last_tok, last_feat, last_label;
	
	EagerReln eager = new EagerReln();
	VerbalReln verbal = new VerbalReln();
	NonVerbalReln nonVerbal = new NonVerbalReln();
	
	int eageri = 0, verbali = 0, nonVerbali = 0;
	int eagerf = 0, verbalf = 0, nonVerbalf = 0;
	String type;
	
	/**
	 * Map function for our input. Gets one sentence and divides it 
	 * into different parts.
	 */
	public void mapInput (String line) {
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
		for (int i = 0; i < toks.length; i++)
			out.add(toks[i].trim() + " " + feats[i].trim() + " " + labels[i].trim() + " " + output[i].trim());
		out.add(last_tok + " " + last_feat + " " + last_label);
	}
	
	/**
	 * Finds the tokens which compose our CCP and returns them stemmed
	 * in the arraylist.
	 */
	ArrayList<String> getRelnToks () {
		ArrayList<String> out = new ArrayList<String>();
		for (int i = 0; i < feats.length; i++)
			if (Reln.containsCCP(feats[i]))
				out.add(toks[i]);
		return out;
	}
	
	void addRelnToks (String relnType, ArrayList<String> relnFeats, boolean pos) {
		Reln toAdd = null;
		Hashtable<String,Integer> ht = null;
		if (type.equals("eager"))
			toAdd = eager;
		else if (type.equals("verbal"))
			toAdd = verbal;
		else
			toAdd = nonVerbal;
		if (pos)
			ht = toAdd.pos;
		else
			ht = toAdd.neg;
		for (String tok : relnFeats) {
			if (ht.containsKey(tok))
				ht.put(tok, ht.get(tok) + 1);
			else
				ht.put(tok, 1);
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
	
	void writeWrong () {
		if (type.equals("eager")) {
			//writeSent(eagerw);
			eagerf++;
		} else if (type.equals("verbal")) {
			//writeSent(verbalw);
			verbalf++;
		} else {
			//writeSent(nonverbalw);
			nonVerbalf++;
		}
	}
	
	void writeSent (DataWriter w) {
		for (int i = 0; i < toks.length; i++)
			w.writeln(toks[i] + " \t" + feats[i] + "\t" + labels[i] + "\t" + output[i]);
		w.writeln(last_tok + " " + last_feat + " " + last_label);
	}
	
	void printSent () {
		for (String s : toks)
			System.out.print(s + " ");
		System.out.println();
	}
	
	/**
	 * Our output data takes the following form:
	 * 
	 * toks feats labels output
	 */
	public static class OutputFormatter extends CRFDataStream {
		public boolean containsDelim(String line) {
			return false;
		}

		public String getToken(String line) {
			return line.split(" ")[0];
		}
		
		public String getFeatures(String line) {
			return line.split(" ")[1];
		}

		public String getLabel(String line) {
			return line.split(" ")[2];
		}
		
		public String getOutput(String line) {
			return line.split(" ")[3];
		}

		public String[] getLabelClasses() {
			return new String[] { Include.CAUSE_TAG, Include.EFFECT_TAG };
		}
	}
	
	public static void main (String[] args) {
		Closer c = new Closer();
		CpEpExtractor ext = new CpEpExtractor();
		StanfordParser.getStanfordParser(c);
		ext.setInput(new FileReader(file_in));
		ext.setOutput(new DataWriter(file_out, c));

		ext.mapInput();
		c.close();
		System.out.println("Occurences: eager " + ext.eageri + " verbal " + ext.verbali + " nonverbal " + ext.nonVerbali);
		System.out.println("Fails: eager " + ext.eagerf + " verbal " + ext.verbalf + " nonverbal " + ext.nonVerbalf);
	}
}

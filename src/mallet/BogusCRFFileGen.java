package mallet;

import java.util.ArrayList;

import analysis.features.AdjacentFeature;

import haus.io.DataWriter;

/**
 * Creates bogus files for our CRF to work with.
 *
 */
public class BogusCRFFileGen {
	public static final String out_file = "crf/bogusCRF.txt";
	public int numSentences = 500;
	public int wordsPerSent = 20;
	public int featsPerLine = 5;
	
	String[] words;
	String[] feat0;
	String[] feat1;
	String[] labels = new String[] {"N","R"};
		
	public void genWords (int numWords) {
		words = new String[numWords];
		for (int i = 0; i < numWords; i++)
			words[i] = "word" + i;
	}
	
	public void genFeats (int numWords) {
		feat0 = new String[numWords];
		for (int i = 0; i < numWords; i++)
			feat0[i] = "feat0_" + i;
	}
	
	public void genFeats2 (int numWords) {
		feat1 = new String[numWords];
		for (int i = 0; i < numWords; i++)
			feat1[i] = "feat1_" + i;
	}
		
	public void genfile () {
		int pos = 0; int neg = 0;
		DataWriter writer = new DataWriter(out_file);
		genWords(20);
		genFeats(10);
		for (int i = 0; i < numSentences; i++) {
			for (int j = 0; j < ((int)(Math.random() * wordsPerSent)) + 1; j++) {
				String word = words[((int) (Math.random() * words.length))];
				//String label = labels[(((int) (Math.random() * 100)) < 98 ? 0:1)];
				String label = "N";
				if (word.equals("word0")) {
					if (((int)(Math.random() * 4)) >= 1) {
						label = "R";
						pos++;
					} else 
						neg++;
				}
				writer.writeln(word + " " + label);
			}
			writer.writeln(Include.SENT_DELIM);
		}
		writer.close();
		System.out.println("pos " + pos + " neg " + neg + " totl " + (neg + pos));
	}
	
	//feats
	public void genfile3 () {
		DataWriter writer = new DataWriter(out_file);
		genWords(2);
		genFeats(2);
		genFeats2(2);
		boolean lastgood = false;
		for (int i = 0; i < numSentences; i++) {
			for (int j = 0; j < ((int)(Math.random() * 20)) + 1; j++) {
				String word = words[((int) (Math.random() * words.length))];
				String feat = feat0[((int) (Math.random() * feat0.length))];
				//String feat2 = feat1[((int) (Math.random() * feat1.length))];
				writer.writeln(word + " " + feat + " " /*+ feat2*/ + (lastgood? " R":" N"));
				if (feat.equals("feat0_1"))
					lastgood= true;
				else
					lastgood=false;
			}
			writer.writeln(Include.SENT_DELIM);
			lastgood=false;
		}
		writer.close();
	}
	
	//Preword dependency
	public void genfile4 () {
		DataWriter writer = new DataWriter(out_file);
		ArrayList<String> words2 = new ArrayList<String>();
		genWords(2);
		String lastword = "";
		for (int i = 0; i < numSentences; i++) {
			for (int j = 0; j < ((int)(Math.random() * 20)) + 1; j++) { 
				String word = words[((int) (Math.random() * words.length))];
				words2.add(word + " " + lastword);
				lastword = word;
			}
			words2.add(Include.SENT_DELIM_REDUX);
			lastword = "";
		}
		String[] toks = new String[words2.size()];
		toks = words2.toArray(toks);
		for (int i = 0; i < toks.length; i++) {
			writer.writeln(toks[i] + " " + "N");
		}
		writer.close();
	}
	
	//feats using new AdjFeature
	public void genfile5 () {
		DataWriter writer = new DataWriter(out_file);
		ArrayList<String> feats = new ArrayList<String>();
		ArrayList<String> labels = new ArrayList<String>();
		genWords(2);
		genFeats(2);
		genFeats2(2);
		boolean lastgood = false;
		for (int i = 0; i < numSentences; i++) {
			for (int j = 0; j < ((int)(Math.random() * 20)) + 1; j++) {
				String word = words[((int) (Math.random() * words.length))];
				String feat = feat0[((int) (Math.random() * feat0.length))];
				feats.add(word + " " + feat);
				labels.add((lastgood? "R" : "N"));
				//String feat2 = feat1[((int) (Math.random() * feat1.length))];
				if (feat.equals("feat0_1"))
					lastgood= true;
				else
					lastgood=false;
			}
			feats.add(Include.SENT_DELIM_REDUX);
			labels.add("N");
			lastgood=false;
		}
		AdjacentFeature f = new AdjacentFeature();
		String[] feats2 = f.getFeature(haus.misc.Conversions.toStrArray(feats));
		for (int i = 0; i < feats2.length; i++) {
			writer.writeln(feats.get(i) + " " + feats2[i] + " " + labels.get(i));
		}
		
		writer.close();
	}
	
	public static void main (String[] args) {
		BogusCRFFileGen gen = new BogusCRFFileGen();
		gen.genfile();
	}
}

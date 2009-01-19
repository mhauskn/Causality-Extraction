package analysis;

import java.util.ArrayList;

import edu.stanford.nlp.trees.Tree;
import parser.StanfordParser;

public class StanfordPhraseFinder {
	public static final String NP_START = "NPStart";
	public static final String NP_MID = "NPMiddle";
	public static final String NP_END = "NPEnd";
	
	StanfordParser sp = null;
	Tree t = null;
	String[] tokens = null;
	int currentWordIndex;
	ArrayList<Integer[]> nps = null;
	
	int npStart; //Start index of our Noun Phrase
	int npEnd; //End index of our Noun Phrase
	
	public StanfordPhraseFinder (StanfordParser _sp) {
		sp = _sp;
		nps = new ArrayList<Integer[]>();
	}
	
	/**
	 * @deprecated
	 */
	public void addFeature (String[] features, String[] _tokens) {
		currentWordIndex = 0;
		nps.clear();
		tokens = _tokens;
		npStart = 0;
		npEnd = Integer.MAX_VALUE;
		t = sp.getParseTree(tokens);
		getBestNP(t);
		for (Integer[] i : nps) {
			for (int j = i[0]; j <= i[1]; j++) {
				if (j == i[0]) {
					features[j] += NP_START;
				} else if (j == i[1]) {
					features[j] += NP_END;
				} else {
					features[j] += NP_MID;
				}
			}
		}
	}
	
	public String[] getFeature (String[] _tokens) {
		String[] feature = new String[_tokens.length];
		currentWordIndex = 0;
		nps.clear();
		tokens = _tokens;
		npStart = 0;
		npEnd = Integer.MAX_VALUE;
		t = sp.getParseTree(tokens);
		getBestNP(t);
		for (Integer[] i : nps) {
			for (int j = i[0]; j <= i[1]; j++) {
				if (j == i[0]) {
					feature[j] = NP_START;
				} else if (j == i[1]) {
					feature[j] = NP_END;
				} else {
					feature[j] = NP_MID;
				}
			}
		}
		return feature;
	}
	
	/**
	 * Finds the most precise Noun Phrase (NP) in the
	 * given tree
	 */
	public void getBestNP (Tree t) {
		if (t.isLeaf()) {
			handlePhraseEnd();
			return;
		}
		Tree[] children = t.children();
		if (t.label().toString().equals("NP")) {
			npStart = currentWordIndex;
			npEnd = currentWordIndex + children.length - 1;
		}
		for (int i = 0; i < children.length; i++) {
			Tree child = children[i];
			getBestNP(child);
		}
	}
	
	/**
	 * Checks for the end of a Noun Phrase and if found, adds to the
	 * end of our list.
	 */
	public void handlePhraseEnd () {
		currentWordIndex++;
		if (currentWordIndex <= npEnd || npStart == -1 || npEnd == -1)
			return;
		Integer[] np = new Integer[2];
		np[0] = npStart;
		np[1] = npEnd;
		nps.add(np);
		npStart = -1;
		npEnd = -1;
	}
	
	public static void main (String[] args) {
		StanfordParser sp2 = new StanfordParser("stanford/englishPCFG.ser.gz");
		StanfordPhraseFinder spf = new StanfordPhraseFinder(sp2);
		String sent = "The strongest rain ever recorded in India shut down the financial hub of Mumbai, snapped communication lines, closed airports and forced thousands of people to sleep in their offices or walk home during the night, officials said today.";
		String[] words = sent.split(" ");
		String[] features = new String[words.length];
		spf.addFeature(features, words);
		for (int i = 0; i < words.length; i++) {
			System.out.println(words[i] + " --> " + features[i]);
		}/*
		for (Integer[] i : spf.nps) {
			System.out.print(i[0] + " " + i[1] + "....");
			for (int j = i[0]; j <= i[1]; j++) {
				System.out.print(words[j] + " ");
			}
			System.out.println("");
		}*/
	}
}

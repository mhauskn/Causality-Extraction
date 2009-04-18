package extraction;

import java.util.Arrays;
import java.util.Hashtable;

import edu.stanford.nlp.trees.Tree;

import parser.Stanford.StanfordParser;

public abstract class Reln {
	public static final String VERBAL_KEYS = "(for|by|from|attributed|reflecting)";
	public static final String NON_VERBAL_KEYS = "(and)";
	
	String cause = mallet.Include.CAUSE_TAG;
	String effect = mallet.Include.EFFECT_TAG;
	String non = mallet.Include.NEITHER_TAG;
	
	String[] toks, feats, out;
	StanfordParser sp = StanfordParser.getStanfordParser();
	Tree[] leaves;
	
	int[] ccpBound;
	
	Hashtable<String,Integer> pos = new Hashtable<String,Integer>();
	Hashtable<String,Integer> neg = new Hashtable<String,Integer>();
	
	public boolean matchesPattern (String[] _toks, String[] _feats) {
		toks = _toks;
		feats = _feats;
		out = new String[toks.length];
		Arrays.fill(out, "");
		reset();
		ccpBound = getRelnIndex(feats);
		return matchesPattern();
	}
	
	public abstract boolean matchesPattern ();
	
	public abstract void getResult ();
	
	public String[] extract () {
		getResult();
		return out;
	}
	
	void reset () {};
	
	/**
	 * Determines if a given feature : N,R,RI,RE,RB
	 * contains the positive CCP feature.
	 */
	public static boolean containsCCP (String feat) {
		return turk.Include.hasPosRelnFeat(feat);
	}
	
	/**
	 * Checks if two integer arrays are seperate from each other
	 * encoding is assumed:
	 * i1[0] = i1 low i1[1] = i1 high
	 */
	public static boolean seperate (int[] i1, int[] i2) {
		return i2[0] > i1[1] || i1[0] > i2[1];
	}
	
	/**
	 * Gets the low and high bounds of the first CCP grouping
	 * in the sentence. 
	 */
	public static int[] getRelnIndex (String[] feats) {
		int start = -1;
		boolean running = false;
		for (int i = 0; i < feats.length; i++) {
			String feat = feats[i];
			if (containsCCP(feat)) {
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
	
	boolean containsReversalKeyword (int[] featsLocs, String keywords) {
		for (int i = featsLocs[0]; i <= featsLocs[1]; i++)
			if (toks[i].matches(keywords))
				return true;
		return false;
	}
}

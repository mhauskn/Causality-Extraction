package analysis.postFeatures.RelnTypes;

import java.util.Arrays;

import parser.Stanford.StanfordParser;

import edu.stanford.nlp.trees.Tree;

public abstract class TypeReln {
	StanfordParser sp;
	Tree t;
	Tree[] leaves;
	
	String[] out;
	String[] toks;
	String[] feats;
	
	/**
	 * Computes how confident we are that this type of TypedReln can match a given
	 * sentence. Confidence scores range from 0.0 - 1.0. 
	 */
	public double computeConfidence (String[] _toks, String[] _feats, Tree root, 
			Tree[] _leaves, StanfordParser _sp) {
		sp = _sp;
		out = new String[_toks.length];
		Arrays.fill(out, "");
		t = root;
		leaves = _leaves;
		toks = _toks;
		feats = _feats;
		return getMetric();
	}
	
	/**
	 * Determines if a given feature : N,R,RI,RE,RB
	 * contains the positive CCP feature.
	 */
	public boolean containsCCP (String feat) {
		return turk.Include.hasPosRelnFeat(feat);
	}
	
	/**
	 * Performs the actual computation of the desired metric
	 */
	public abstract double getMetric ();
	
	/**
	 * Returns the array of features
	 */
	public String[] getResults () {
		return out;
	}
	
	/**
	 * Given an integer array with the start/end word indexes,
	 * this method will write the feature to everything in-between
	 */
	void writeFeat (int[] arr1, String featName) {
		for (int i = 0; i < toks.length; i++)
			if (i >= arr1[0] && i <= arr1[1]) 
				out[i] += featName + " ";
	}
	
	/**
	 * Looks for those features which have no distinct
	 * feature added and adds our empty feature.
	 */
	void writeEmptyFeat (String featName) {
		for (int i = 0; i < toks.length; i++)
			if (feats[i].length() == 0) 
				out[i] += featName + " ";
	}
}

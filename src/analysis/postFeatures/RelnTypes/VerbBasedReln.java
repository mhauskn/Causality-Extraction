package analysis.postFeatures.RelnTypes;

import haus.misc.Condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import analysis.postFeatures.RelnDep;

import parser.Stanford.StanfordParser;
import parser.Stanford.TreeOps;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;

/**
 * This class contains methods for generating features 
 * useful to identify verb based cue phrase relations.
 */
public class VerbBasedReln {
	public static final String root1_feat = "key_area1";
	public static final String root2_feat = "key_area2";
	public static final String neg_feat = "non_key";
	
	ArrayList<Integer> locations = new ArrayList<Integer>();
	
	int root1, root2;
	int [] r1arr, r2arr;
	int len;
	
	Tree t;
	Tree[] leaves;
	
	String[] out;
	
	/**
	 * Returns true if this relation is a verb-based
	 * relation.
	 * @return
	 */
	public double computeConfidence (String[] toks, String[] feats, Tree root, Tree[] _leaves) {
		out = new String[toks.length];
		Arrays.fill(out, "");
		t = root;
		leaves = _leaves;
		locations.clear();
		r1arr = null;
		r2arr = null;
		root1 = -1; root2 = -1;
		int len = toks.length;
		
		String[] pos_tags = TreeOps.getPOSTags(t);
		for (int i = 0; i < len; i++)
			if (turk.Include.hasPosRelnFeat(feats[i]) &&
					pos_tags[i].matches(StanfordParser.VP_REGEXP))
				locations.add(i);
		if (locations.size() == 0)
			return 0.0;
		return createVerbFeats();
	}
	
	/**
	 * This method is used when our cue phrase is a verb. It will
	 * create relevant features to identifying the CP/EP when dealing
	 * with a verb based Cue Phrase.
	 * 
	 * The overall procedure is as follows:
	 * 1. Locate verb
	 * 2. Look for nsubj relation involving verb
	 * 3. The other end of this relation will contain CP.
	 * 4. Expand CP to largest Np or S not containing reln
	 * 5. Expand reln to containing VP - This will be EP
	 */
	double createVerbFeats () {
		Collection<TypedDependency> td_collect = TreeOps.getDependencies(t);
		ArrayList<TypedDependency> workable = new ArrayList<TypedDependency>();
		for (TypedDependency td : td_collect) {
			String gov = td.gov().toString();
			int govIndex = TreeOps.getDependencyValue(gov);
			if (locations.contains(govIndex) && td.reln().toString().equals("nsubj"))
				workable.add(td);
		}
		// Find a good dependency
		for (TypedDependency td : workable) {
			String gov = td.gov().toString();
			int govIndex = TreeOps.getDependencyValue(gov);
			String dep = td.dep().toString();
			root1 = TreeOps.getDependencyValue(dep);
			root2 = govIndex;
			expandInfluence();
			if (r1arr == null || r2arr == null || !seperate(r1arr, r2arr))
				continue;
			writeVerbFeat();
			return 1.0;
		}
		// This code is reached only if there are no good deps
		root2 = locations.get(locations.size()-1);
		Tree base2 = TreeOps.expand(t, leaves[root2], new StopOnRegexp(StanfordParser.VerbPhrase));
		if (base2 == null) { return 0.0; }
		r2arr = TreeOps.getSubTreeBoundaries(t, base2);
		root1 = r2arr[0] - 1;
		if (root1 < 0) { return 0.0; }
		
		// Get a valid base1 tree
		Tree r1exp = TreeOps.expand(t, leaves[root1], new StopOnRegexp(RelnDep.NP_OR_S));
		// Look lower for a valid tree
		while (root1 != 0 && (r1exp == null || !seperate(TreeOps.getSubTreeBoundaries(t, r1exp),r2arr))) {
			r1exp = TreeOps.expand(t, leaves[--root1], new StopOnRegexp(RelnDep.NP_OR_S));
		}
		if (r1exp == null) {
			root1 = r2arr[1] + 1;
			while (root1 != leaves.length-1 && (r1exp == null || !seperate(TreeOps.getSubTreeBoundaries(t, r1exp),r2arr))) {
				r1exp = TreeOps.expand(t, leaves[root1++], new StopOnRegexp(RelnDep.NP_OR_S));
			}
		}
		// Expand our valid r1 tree
		if (r1exp == null) { return 0.0; }
		Tree base1 = null;
		while (r1exp != null && seperate(TreeOps.getSubTreeBoundaries(t, r1exp),r2arr)) {
			base1 = r1exp;
			r1exp = TreeOps.expand(t, r1exp.parent(t), new StopOnRegexp(RelnDep.NP_OR_S));
		}
		if (base1 == null) { return 0.0; }
		r1arr = TreeOps.getSubTreeBoundaries(t, base1);
		if (!seperate(r1arr, r2arr)) return 0.0;
		writeVerbFeat();
		return 0.5;
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
	 * Expand in a non-overlapping manner the influence of each of our different 
	 * phrases.
	 */
	void expandInfluence () {
		Tree base1 = TreeOps.expand(t, leaves[root1], new StopOnRegexp(RelnDep.NP_OR_S));
		Tree base2 = TreeOps.expand(t, leaves[root2], new StopOnRegexp(StanfordParser.VerbPhrase));
		if (base1 == null || base2 == null) return;
		Tree r1exp = base1;
		Tree r2exp = base2;
		while (r1exp != null && r2exp != null && 
				seperate(TreeOps.getSubTreeBoundaries(t, r1exp),
				TreeOps.getSubTreeBoundaries(t, r2exp))) {
			base1 = r1exp;
			base2 = r2exp;
			r1exp = TreeOps.expand(t, base1.parent(t), new StopOnRegexp(RelnDep.NP_OR_S));
			r2exp = TreeOps.expand(t, base2.parent(t), new StopOnRegexp(StanfordParser.VerbPhrase));
		}
		r1arr = TreeOps.getSubTreeBoundaries(t, base1);
		r2arr = TreeOps.getSubTreeBoundaries(t, base2);
	}
	
	/**
	 * Stops once regexp is met
	 */
	class StopOnRegexp implements Condition<Tree> {
		String regexp;
		public StopOnRegexp (String r) {
			regexp = r;
		}
		public boolean satisfied (Tree t) {
			return t.label().toString().matches(regexp);
		}
	}
	
	/**
	 * Write the features for our verb governed sentence
	 */
	void writeVerbFeat () {
		for (int i = 0; i < len; i++) {
			if (i >= r1arr[0] && i <= r1arr[1]) 
				out[i] += root1_feat + " ";
			else if (i >= r2arr[0] && i <= r2arr[1]) 
				out[i] += root2_feat + " ";
			else
				out[i] += neg_feat + " ";
		}
	}
	
	/**
	 * Returns the array of features
	 */
	public String[] getResults () {
		return out;
	}
}

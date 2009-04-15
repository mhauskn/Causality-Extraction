package analysis.postFeatures.RelnTypes;

import haus.misc.Condition;

import java.util.ArrayList;
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
public class VerbBasedReln extends TypeReln {
	public static final String root1_feat = "verbalArea1";
	public static final String root2_feat = "verbalArea2";
	public static final String neg_feat = "verbalNeg";
		
	int root1, root2;
	int [] r1arr, r2arr;
	
	String[] exp_toks;
	ArrayList<Integer> locations = new ArrayList<Integer>();
	String[] pos_tags;
	Collection<TypedDependency> td_collect;
	
	/**
	 * Returns our confidence score that this sentence
	 * can be handled by the verb based metric
	 */
	public double getMetric () {
		r1arr = null;
		r2arr = null;
		root1 = -1; root2 = -1;
		pos_tags = TreeOps.getPOSTags(t);
		getLocs();
		if (locations.size() <= 0)
			return 0.0;
		return createVerbFeats();
	}
	
	/**
	 * Looks through our contracted POS tags for 
	 * valid verbal CCP locations
	 */
	void getLocs () {
		locations.clear();
		for (int i = 0; i < toks.length; i++)
			if (containsCCP(feats[i]) && pos_tags[i].matches(StanfordParser.VP_REGEXP))
				locations.add(i);
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
		if (hasGoodTD())
			return 1.0;
		return inferTD();
	}
	
	/**
	 * Checks for quality Typed Dependencies which 
	 * will indicate that we can identify CP/EP
	 * with high certainty.
	 */
	boolean hasGoodTD () {
		td_collect = TreeOps.getDependencies(t);
		ArrayList<TypedDependency> workable = new ArrayList<TypedDependency>();
		for (TypedDependency td : td_collect) {
			String gov = td.gov().toString();
			int govIndex = TreeOps.getDependencyValue(gov);
			String reln = td.reln().toString();
			if (locations.contains(govIndex) && 
					(reln.equals("nsubj") || reln.equals("nsubjpass")))
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
			return true;
		}
		return false;
	}
	
	/**
	 * Attempts to infer a likely location which the TD
	 * would point to lacking an explicit TD. This generally
	 * is a bad strategy and is not very preferable.
	 */
	double inferTD () {
		root2 = locations.get(locations.size()-1);
		Tree base2 = TreeOps.expandWhile(t, leaves[root2].parent(t), 
				new TreeOps.regexpMatcher(StanfordParser.VP_REGEXP));
		if (base2 == null) { return 0.0; }
		r2arr = TreeOps.getSubTreeBoundaries(t, base2);
		
		// Get NP and expand upon it
		Tree base1 = null;
		root1 = findPrevNP(r2arr[0]-1);
		if (root1 >= 0) { 
			base1 = TreeOps.expandWhile(t, leaves[root1].parent(t), 
					new sepRegexpMatcher(RelnDep.NP_OR_S,r2arr,t));
		} else {
			root1 = findNextNP(r2arr[1]+1);
			if (root1 >= 0)
				base1 = TreeOps.expandWhile(t, leaves[root1].parent(t), 
						new sepRegexpMatcher(RelnDep.NP_OR_S,r2arr,t));
		}
		if (base1 == null) { return 0.0; }
		r1arr = TreeOps.getSubTreeBoundaries(t, base1);
		if (!seperate(r1arr, r2arr)) return 0.0;
		writeVerbFeat();
		return 0.5;
	}
	
	/**
	 * Condition is true as long as we match a certain regexp and 
	 * do not step into another tree's bounds.
	 */
	public static class sepRegexpMatcher implements Condition<Tree> {
		String regexp;
		int[] bounds;
		int lower;
		int upper;
		Tree root; 
		
		public sepRegexpMatcher (String r, int[] _bounds, Tree _root) {
			regexp = r;
			bounds = _bounds;
			root = _root;
		}
		public boolean satisfied (Tree t) {
			return t.label().toString().matches(regexp) && 
				seperate(TreeOps.getSubTreeBoundaries(root, t),bounds);
		}
	}
	
	/**
	 * Finds the previous tag matching the NP regexp
	 */
	int findPrevNP (int start) {
		int curr = start;
		while (curr >= 0) {
			if (pos_tags[curr].matches(StanfordParser.NP_REGEXP))
				return curr;
			curr--;
		}
		return -1;
	}
	
	/**
	 * Finds the previous tag matching the NP regexp
	 */
	int findNextNP (int start) {
		int curr = start;
		while (curr < pos_tags.length) {
			if (pos_tags[curr].matches(StanfordParser.NP_REGEXP))
				return curr;
			curr++;
		}
		return -1;
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
		Tree base1 = TreeOps.expandUntil(t, leaves[root1], new TreeOps.regexpMatcher(RelnDep.NP_OR_S));
		Tree base2 = TreeOps.expandUntil(t, leaves[root2], new TreeOps.regexpMatcher(StanfordParser.VerbPhrase));
		if (base1 == null || base2 == null) return;
		Tree r1exp = base1;
		Tree r2exp = base2;
		while (r1exp != null && r2exp != null && 
				seperate(TreeOps.getSubTreeBoundaries(t, r1exp),
				TreeOps.getSubTreeBoundaries(t, r2exp))) {
			base1 = r1exp;
			base2 = r2exp;
			r1exp = TreeOps.expandUntil(t, base1.parent(t), new TreeOps.regexpMatcher(RelnDep.NP_OR_S));
			r2exp = TreeOps.expandUntil(t, base2.parent(t), new TreeOps.regexpMatcher(StanfordParser.VerbPhrase));
		}
		r1arr = TreeOps.getSubTreeBoundaries(t, base1);
		r2arr = TreeOps.getSubTreeBoundaries(t, base2);
	}
	
	
	
	/**
	 * Write the features for our verb governed sentence
	 */
	void writeVerbFeat () {
		for (int i = 0; i < toks.length; i++) {
			if (i >= r1arr[0] && i <= r1arr[1]) 
				out[i] += root1_feat + " ";
			else if (i >= r2arr[0] && i <= r2arr[1]) 
				out[i] += root2_feat + " ";
			else
				out[i] += neg_feat + " ";
		}
	}
}

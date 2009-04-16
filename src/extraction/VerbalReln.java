package extraction;

import java.util.ArrayList;
import java.util.Collection;

import analysis.postFeatures.RelnDep;
import analysis.postFeatures.RelnTypes.VerbBasedReln.sepRegexpMatcher;

import parser.Stanford.StanfordParser;
import parser.Stanford.TreeOps;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;

public class VerbalReln extends Reln {
	int root1, root2;
	int [] r1arr, r2arr;
	Tree t;
	
	String[] exp_toks;
	ArrayList<Integer> locations = new ArrayList<Integer>();
	String[] pos_tags;
	Collection<TypedDependency> td_collect;
	
	
	public boolean matchesPattern() {
		t = sp.getParseTree(toks);
		leaves = TreeOps.getLeaves(t);
		pos_tags = TreeOps.getPOSTags(t);
		getVerbalLocations();
		if (locations.size() <= 0)
			return false;
		return true;
	}
	
	/**
	 * Looks through our contracted POS tags for 
	 * valid verbal CCP locations
	 */
	void getVerbalLocations () {
		for (int i = 0; i < toks.length; i++)
			if (containsCCP(feats[i]) && pos_tags[i].matches(StanfordParser.VP_REGEXP)) { 
				String label = leaves[i].parent(t).parent(t).label().toString();
				if (label.equals("VP"))
					locations.add(i);
			}
	}
	
	public void getResult() {
		if (hasGoodTD())
			return;
		inferTD();
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
					(reln.equals("nsubj") || 
					reln.equals("nsubjpass") ||
					reln.equals("dep")))
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
		if (base1 == null)
			r1arr = new int[] { 0, r2arr[0] == 0 ? r2arr[1] + 1 : r2arr[0] -1}; 
		else
			r1arr = TreeOps.getSubTreeBoundaries(t, base1);
		if (!seperate(r1arr, r2arr)) return 0.0;
		writeVerbFeat();
		return 0.5;
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
	 * Write the features for our verb governed sentence
	 */
	void writeVerbFeat () {
		String area1, area2;
		boolean reversed = containsReversalKeyword(ccpBound);
		if (reversed) {
			area1 = effect; area2 = cause;
			for (int i = 0; i < toks.length; i++) {
				if (i >= r1arr[0] && i <= ccpBound[1]) 
					out[i] += area1 + " ";
				else if (i >= r2arr[0] && i <= r2arr[1]) 
					out[i] += area2 + " ";
				else
					out[i] += non + " ";
			}
		} else {
			area1 = cause; area2 = effect;
			for (int i = 0; i < toks.length; i++) {
				if (i >= r1arr[0] && i <= r1arr[1]) 
					out[i] += area1 + " ";
				else if (i >= r2arr[0] && i <= r2arr[1]) 
					out[i] += area2 + " ";
				else
					out[i] += non + " ";
			}
		}
	}
	
	void reset () {
		r1arr = null;
		r2arr = null;
		root1 = -1; root2 = -1;
		locations.clear();
	}
}

package extraction;

import parser.Stanford.TreeOps;
import edu.stanford.nlp.trees.Tree;

/**
 * Assumes EP reln CP where EP and CP are maximal
 * adjacent NP or S units.
 */
public class NonVerbalReln extends Reln {
	String SUB = "(S|SBAR)";
	String NPS = "(S|NP)";
	Tree t;
	int[] left, right;
	
	public boolean matchesPattern() {
		return true;
	}

	public void getResult() {
		findLeftNP();
		findRightNP();
		write();
	}
	
	/**
	 * Finds the maximal non-contig left NP or S
	 */
	void findLeftNP () {
		int leftStart = ccpBound[0] - 1;
		if (leftStart < 0) {
			System.out.println("Alert! We should not handle this sentence!");
			return;
		}
		Tree base = TreeOps.expandUntil(t, leaves[leftStart], new TreeOps.regexpMatcher(SUB));
		if (base == null) base = t;
		int[] bounds = TreeOps.getSubTreeBoundaries(t, base);
		left = new int[] { bounds[0], leftStart};
	}
	
	/**
	 * Finds maximal non-contig right NP or S
	 */
	void findRightNP () {
		int rightStart = ccpBound[1];
		Tree exp = null;
		while (exp == null)
			exp = expandWhileSeperate (++rightStart, ccpBound);
		right = TreeOps.getSubTreeBoundaries(t, exp);
	}
	
	/**
	 * Expands the given leaf index while it is seperate from 
	 * our hazard array
	 */
	Tree expandWhileSeperate (int index, int[] hazard) {
		Tree base = TreeOps.expandUntil(t, leaves[index], new TreeOps.regexpMatcher(NPS));
		if (base == null) return null;
		Tree exp = base;
		int[] expBounds = TreeOps.getSubTreeBoundaries(t, exp);
		while (exp != null && seperate(expBounds ,hazard)) {
			base = exp;
			exp = TreeOps.expandUntil(t, base.parent(t), new TreeOps.regexpMatcher(NPS));
			expBounds = TreeOps.getSubTreeBoundaries(t, exp);
		}
		return base;
	}
	
	void write () {
		for (int i = 0; i < toks.length; i++) {
			if (i >= left[0] && i <= left[1])
				out[i] = effect;
			else if (i >= right[0] && i <= right[1])
				out[i] = cause;
			else
				out[i] = non;
		}
	}
	
	void reset () {
		t = sp.getParseTree(toks);
		leaves = TreeOps.getLeaves(t);
	}
}

package extraction;

import java.util.ArrayList;

import parser.Stanford.StanfordParser;
import parser.Stanford.TreeOps;

import edu.stanford.nlp.trees.Tree;

/**
 * The bi-phrasal-relation is desinged to detect relations consisting
 * of two main phrases and starting with the CCP at the start of the 
 * sentence. Ex:
 * 
 * [If]R [the stock market drops,]C [people will lose jobs.]E
 * 
 * It does this in the following steps:
 * 1. Look for CCP at start of sentence
 * 2. Identify the two main phrases
 * 3. Look for comma then NP,VP at same level
 * 4. Give a label to each of these two phrases
 * and let CRF sort out which is the CP/EP 
 */
public class EagerReln extends Reln {	
	ArrayList<Integer> commaLocs;
		
	Tree exp;
	
	Tree area1, area2;
	int goodLoc;
	
	/**
	 * Returns true if our sentence matches the pattern matched
	 * by this relation
	 */
	public boolean matchesPattern () {
		ccpBound = findCCPEnd();
		doCommaSearch();
		if (commaLocs.size() == 0)
			return false;
		if (hasGoodPattern())
			return true;
		if (ccpBound[0] == 0)
			return true;
		//for (int i : commaLocs)
		//	if (locateNPVP(i))
		//		return true;
		return false;
	}
	
	/**
	 * Finds the index of the end of our CCP
	 */
	int[] findCCPEnd () {
		boolean seenCCP = false;
		int start = 0;
		int offset = 0;
		for (int i = 0; i < feats.length; i++) {
			String feat = feats[i];
			if (StanfordParser.isPunc(toks[i].charAt(toks[i].length()-1)+""))
				offset++;
			if (containsCCP(feat)) {
				if (!seenCCP)
					start = i + offset;
				seenCCP = true;
			} else if (seenCCP)
				return new int[] { start, i-1 + offset };
		}
		if (seenCCP)
			return new int[] { start, feats.length-1 + offset};
		return null;
	}
	
	/**
	 * Looks for SBAR then , then NP then VP
	 */
	boolean hasGoodPattern () {
		Tree posNP = null, posVP = null;
		for (int i = ccpBound[0]; i <= ccpBound[1]; i++) {
			Tree leaf = leaves[i];
			Tree parent = leaf.parent(exp);
			Tree grandParent = parent.parent(exp);
			Tree ggParent = grandParent.parent(exp);
			
			if (!grandParent.label().toString().equals("SBAR") && 
					!(grandParent.label().toString().equals("PP") &&
							ggParent.label().toString().equals("S")))
				continue;
			Tree[] children;
			//if (grandParent.label().toString().equals("SBAR"))
			//	children = grandParent.children();
			//else
				children = ggParent.children();
			boolean comma = false, np = false, vp = false;
			for (Tree child : children) {
				String label = child.label().toString();
				if (label.equals(","))
					comma = true;
				if (label.equals("NP") && comma) {
					np = true;
					posNP = child;
				}
				if (label.equals("VP") && np) {
					vp = true;
					posVP = child;
				}
			}
			if (comma && np && vp) {
				area1 = posNP;
				area2 = posVP;
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Searches our leaves for commas. 
	 * These commas should be followed by NP VP
	 */
	void doCommaSearch () {
		for (int i = 0; i < leaves.length; i++) {
			String label = leaves[i].label().toString();
			if (label.equals(",") && i >= ccpBound[1])
				commaLocs.add(i);
		}
	}
	
	/**
	 * Checks if any of our commas are followed by NP VP
	 */
	boolean locateNPVP (int loc) {
		Tree comma = leaves[loc].parent(exp);
		Tree nextChild = TreeOps.getNextChild(comma, exp);
		if (nextChild == null) return false;
		Tree nextNextChild = TreeOps.getNextChild(nextChild, exp);
		while (nextChild != null && nextNextChild != null) {
			if (nextChild.label().toString().equals(StanfordParser.NounPhrase) &&
					nextNextChild.label().toString().equals(StanfordParser.VerbPhrase)) {
				area1 = nextChild;
				area2 = nextNextChild;
				goodLoc = loc;
				return true;
			}
			if (nextChild.equals(comma) || nextNextChild.equals(comma))
				return false;
			nextChild = TreeOps.getNextChild(nextChild, exp);
			nextNextChild = TreeOps.getNextChild(nextNextChild, exp);
		}
		return false;
	}
	
	public void getResult () {
		int[] bound1 = null, bound2 = null;
		
		if (area1 == null || area2 == null) {
			goodLoc = commaLocs.get(0);
			bound1 = new int[] { 0, goodLoc };
			bound2 = new int[] { goodLoc + 1, toks.length-1};
		} else {
			bound1 = TreeOps.getSubTreeBoundaries(exp, area1);
			bound2 = TreeOps.getSubTreeBoundaries(exp, area2);
			goodLoc = bound1[0] -1;
		}
		
		if (bound1 == null || bound2 == null) {
			System.out.println("NUlled");
		}
				
		if (!seperate(bound1, bound2)) {
			System.out.println("Inseperable relns... We have a problem!");
			System.exit(1);
		}
		
		int cnt = 0;
		for (int i = 0; i < leaves.length; i++) {
			if (StanfordParser.isPunc(leaves[i].label().toString()))
				continue;
			if (i < goodLoc)
				out[cnt] += cause + " ";
			else if (i >= goodLoc && i <= bound2[1])
				out[cnt] += effect + " ";
			else
				out[cnt] += non + " ";
			cnt++;
		}
	}
	
	void reset () {
		area1 = null; area2 = null;
		goodLoc = -1;
		exp = sp.getExpandedParseTree(toks);
		leaves = TreeOps.getLeaves(exp);
		commaLocs = new ArrayList<Integer>();
	}
}

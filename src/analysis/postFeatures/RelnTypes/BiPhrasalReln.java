package analysis.postFeatures.RelnTypes;

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
public class BiPhrasalReln extends TypeReln {
	public static final String feat1 = "biPhrasalArea1";
	public static final String feat2 = "biPhrasalArea2";
	public static final String neg = "biPhrasalNeg";
	
	ArrayList<Integer> commaLocs = new ArrayList<Integer>();
	double conf = 0.0;
	
	Tree area1, area2;
	
	/**
	 * Determines if our sentence will be best handled 
	 * by the Bi Phrasal Relation
	 */
	public double getMetric() {
		commaLocs.clear();
		conf = 0.0;
		
		if (!containsCCP(feats[0]))
			return 0.0;
		doCommaSearch();
		if (commaLocs.size() == 0)
			return 0.0;
		for (int i : commaLocs)
			if (locateNPVP(i)) {
				conf = 1.0;
				writeFeat();
				break;
			}
		return conf;
	}
	
	/**
	 * Searches our leaves for commas. 
	 * These commas should be followed by NP VP
	 */
	void doCommaSearch () {
		for (int i = 0; i < leaves.length; i++)
			if (leaves[i].label().toString().equals(","))
				commaLocs.add(i);
	}
	
	/**
	 * Checks if any of our commas are followed by NP VP
	 */
	boolean locateNPVP (int loc) {
		Tree comma = leaves[loc].parent(t);
		Tree nextChild = TreeOps.getNextChild(comma, t);
		Tree nextnextChild = TreeOps.getNextChild(nextChild, t);
		if (nextChild == null || nextnextChild == null)
			return false;
		if (!nextChild.label().toString().equals(StanfordParser.NounPhrase) ||
				!nextnextChild.label().toString().equals(StanfordParser.VerbPhrase))
			return false;
		area1 = nextChild;
		area2 = nextnextChild;
		return true;
	}
	
	void writeFeat () {
		int[] bound1 = TreeOps.getSubTreeBoundaries(t, area1);
		int[] bound2 = TreeOps.getSubTreeBoundaries(t, area2);
		
		if (!VerbBasedReln.seperate(bound1, bound2)) {
			System.out.println("Inseperable relns... We have a problem!");
			System.exit(1);
		}
		
		int cnt = 0;
		for (int i = 0; i < leaves.length; i++) {
			if (StanfordParser.isPunc(leaves[i]))
				continue;
			if (i < bound1[0])
				out[cnt] += feat1 + " ";
			else if (i >= bound1[0] && i <= bound2[1])
				out[cnt] += feat2 + " ";
			else
				out[cnt] += neg + " ";
			cnt++;
		}
	}
}

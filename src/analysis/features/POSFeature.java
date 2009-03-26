package analysis.features;

import analysis.Feature;
import parser.Stanford.StanfordParser;
import parser.Stanford.TreeOps;

/**
 * Creates POS Tags for a given sentence.
 * Employs the Stanford Parser to create these POS tags.
 */
public class POSFeature implements Feature {
	public static String feat_name = "tok_pos:";
	StanfordParser sp;
	
	public POSFeature (StanfordParser _sp) {
		sp = _sp;
	}
	
	/**
	 * Returns an array of POS tags corresponding to the words of 
	 * the given sentence.
	 */
	public String[] getFeature(String[] tokens) {
		String[] tags = TreeOps.getPOSTags(sp.getParseTree(tokens));
		String[] out = new String[tags.length];
		for (int i = 0; i < tags.length; i++)
			out[i] = feat_name + tags[i];
		return out;
	}

}

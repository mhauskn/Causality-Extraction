package analysis.features;

import analysis.Feature;
import parser.StanfordParser;

/**
 * Creates POS Tags for a given sentence.
 * Employs the Stanford Parser to create these POS tags.
 */
public class POSFeature implements Feature {
	StanfordParser sp;
	
	public POSFeature (StanfordParser _sp) {
		sp = _sp;
	}
	
	/**
	 * Returns an array of POS tags corresponding to the words of 
	 * the given sentence.
	 */
	public String[] getFeature(String[] tokens) {
		return sp.getPOSTags(sp.getParseTree(tokens));
	}

}

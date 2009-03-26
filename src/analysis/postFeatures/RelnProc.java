package analysis.postFeatures;

import java.util.Arrays;

import stemmer.BasicStemmer;

import analysis.PostFeature;

/**
 * Processes our Relation or Cue Phrase and applies things like 
 * stemming to it.
 */
public class RelnProc implements PostFeature {
	public static final String cp_feat_name = turk.Include.relnFeat;
	public static final String cp_stm = "reln_stm:";

	BasicStemmer stemmer = new BasicStemmer();
	
	String[] tokens, features, out;
	
	public String[] getFeature(String[] _tokens, String[] _features) {
		tokens = _tokens;
		features = _features;
		out = new String[tokens.length];
		Arrays.fill(out, "");
		stemReln();
		return out;
	}
	
	/**
	 * Gets the stem of the Cue Phrase and adds that as a feature 
	 * for every token. Ideally we want to get the whole (multi-word)
	 * phrase, stem each word, and concatenate
	 */
	void stemReln () {
		String relnStm = "";
		for (int i = 0; i < tokens.length; i++) {
			String feat = features[i];
			String tok = tokens[i];
			if (turk.Include.hasPosRelnFeat(feat))
				relnStm += cp_stm + stemmer.stem(tok) + " ";
		}
		for (int i = 0; i < tokens.length; i++)
			out[i] += relnStm;
	}
}

package analysis.features;

import analysis.Feature;
import parser.Stanford.StanfordParser;
import edu.stanford.nlp.trees.Tree;

/**
 * Tags Noun Phrases and Verb Phrases
 */
public class NpVpAggFeature implements Feature {
	public static final String NP_TAG = "NounPhrase";
	public static final String VP_TAG = "VerbPhrase";
	
	StanfordParser sp = null;
	Tree t = null;
	String[] tokens = null;
	String[] feature;
	int relStart;
	int currWord;
	int currPhrase;
	boolean innp = false;
	boolean invp = false;
	
	public NpVpAggFeature (StanfordParser _sp) {
		sp = _sp;
	}
	
	public NpVpAggFeature () {
		sp = StanfordParser.getStanfordParser();
	}
	
	public String[] getFeature (String[] _tokens) {
		t = sp.getParseTree(_tokens);
		return getFeature(_tokens, t);
	}
	
	public String[] getFeature (String[] _tokens, Tree t) {
		currWord = 0;
		currPhrase = 0;
		feature = new String[_tokens.length];
		tokens = _tokens;
		aggregate(t);
		return feature;
	}
	
	/**
	 * This is called after the getFeature call and will normalize
	 * the indexes of the np/vp around the given index
	 * @param _relStart: given index
	 */
	public String[] normalizeFeatureTokens (int _relStart) {
		relStart = _relStart;
		String initType = feature[relStart];
		String type = feature[relStart];
		int npindex = 0;
		int vpindex = 0;
		for (int i = relStart; i >= 0; i--) {
			String label = feature[i];
			if (label.equals(type)) {
				if (label.equals(NP_TAG))
					feature[i] += "" + npindex;
				else if (label.equals(VP_TAG))
					feature[i] += "" + vpindex;
			} else {
				type = feature[i];
				if (label.equals(NP_TAG))
					feature[i] += "" + --npindex;
				else
					feature[i] += "" + --vpindex;
			}
		}
		type = initType;
		npindex = 0; vpindex = 0;
		for (int i = relStart +1; i < feature.length; i++) {
			String label = feature[i];
			if (label.equals(type)) {
				if (label.equals(NP_TAG))
					feature[i] += "" + npindex;
				else if (label.equals(VP_TAG))
					feature[i] += "" + vpindex;
			} else {
				type = feature[i];
				if (label.equals(NP_TAG))
					feature[i] += "" + ++npindex;
				else
					feature[i] += "" + ++vpindex;
			}
		}
		return feature;
	}
	
	/**
	 * Depth first traversal through tree to aggregate nps/vps
	 */
	public void aggregate (Tree t) {
		if (t.isLeaf()) {
			if (innp)
				feature[currWord++] = NP_TAG;// + currPhrase;
			else if (invp)
				feature[currWord++] = VP_TAG;// + currPhrase;
			return;
		}
		Tree[] children = t.children();
		if (t.label().toString().matches(parser.Stanford.StanfordParser.NP_REGEXP)) {
			if (invp)
				currPhrase++;
			innp = true;
			invp = false;
		} else if (t.label().toString().matches(parser.Stanford.StanfordParser.VP_REGEXP)) {
			if (innp)
				currPhrase++;
			invp = true;
			innp = false;
		}
		for (int i = 0; i < children.length; i++) {
			Tree child = children[i];
			aggregate(child);
		}
	}
}

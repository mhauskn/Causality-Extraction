package analysis.features;

import java.util.ArrayList;

import analysis.Feature;

import stemmer.BasicStemmer;
import wordnet.JWI;

/**
 * Looks up the hypernyms of a given word via wordnet.
 */
public class HypernymFeature implements Feature {
	BasicStemmer stemmer = null;
	JWI jwi	= null;
	
	public HypernymFeature () {
		stemmer = new BasicStemmer();
		jwi = new JWI();
	}

	public void addFeature (String[] features, String[] tokens, String[] pos_tags) {
		for (int i = 0; i < tokens.length; i++) {
			String tok = tokens[i];
			String tag = pos_tags[i];
			String stm = stemmer.stem(tok);
			features[i] += " " + stm;
			if (tag.charAt(0) != 'n' && tag.charAt(0) != 'v')
				continue;
			boolean isNoun = tag.charAt(0) == 'n';
			ArrayList<String> hier = isNoun ? jwi.getNounHypernyms(tok) : jwi.getVerbHypernyms(tok);
			if (hier != null) {
				for (String s : hier)
					features[i] += " " + s;
			} else {
				hier = isNoun ? jwi.getNounHypernyms(stm) : jwi.getVerbHypernyms(stm);
				if (hier != null)
					for (String s : hier)
						features[i] += " " + s;
			}
		}
	}

	@Override
	public String[] getFeature(String[] tokens) {
		// TODO Auto-generated method stub
		return null;
	}
}

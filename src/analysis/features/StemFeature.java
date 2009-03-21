package analysis.features;

import analysis.Feature;
import stemmer.BasicStemmer;

public class StemFeature implements Feature {
	BasicStemmer stemmer = new BasicStemmer();
	
	public String[] getFeature (String[] tokens) {
		String[] feature = new String[tokens.length];
		
		for (int i = 0; i < tokens.length; i++) {
			String tok = tokens[i];
			String stem = stemmer.stem(tok);
			if (stem == null)
				feature[i] = "";
			else
				feature[i] = "stm_" + stem;
		}
		
		return feature;
	}
}

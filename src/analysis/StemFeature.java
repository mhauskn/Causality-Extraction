package analysis;

import stemmer.BasicStemmer;

public class StemFeature {
	BasicStemmer stemmer = new BasicStemmer();
	
	public String[] getFeature (String[] tokens) {
		String[] feature = new String[tokens.length];
		
		for (int i = 0; i < tokens.length; i++) {
			String tok = tokens[i];
			String stem = stemmer.stem(tok);
			if (stem == null)
				feature[i] = "";
			else
				feature[i] = stem + " ";
		}
		
		return feature;
	}
}

package analysis;

public class AdjacentWordFeature {
	public static final String tok_iden = "tokAt(";
	
	/**
	 * Returns the adjacent words as a feature.
	 * @param tokens The words of the original sentence
	 * @param numAdj The number of adjacent words to use in the feature
	 * @deprecated
	 * @return
	 */
	public static String[] getFeature (String[] tokens, int numAdj) {
		String[] feature = new String[tokens.length];

		for (int i = 0; i < tokens.length; i++) {
			feature[i] = "";
			if (tokens[i].contains(mallet.Include.SENT_DELIM_REDUX))
				continue;
			int num = -1;
			int scalar = 1;
			int mult = -1;
			boolean preDelim = false; boolean postDelim = false;
			for (int j = i - 1; j >= i - numAdj; mult*=-1, j+=(++scalar)*mult, num=j-i) {
				if (j < 0 || j >= tokens.length)
					feature[i] += genFeature(num, "Null");
				else if (tokens[j].contains(mallet.Include.SENT_DELIM_REDUX) || 
						(preDelim && j<i) || (postDelim && j>i)) {
					feature[i] += genFeature(num, "Null");
					if (j < i)
						preDelim = true;
					else
						postDelim = true;
				}
				else
					feature[i] += genFeature(num, tokens[j]);
			}
		}
		
		return feature;
	}
	
	/**
	 * Generates a feature at the given word location with the 
	 * given token
	 */
	static String genFeature (int loc, String token) {
		return tok_iden + loc + "):" + token + " ";
	}
}

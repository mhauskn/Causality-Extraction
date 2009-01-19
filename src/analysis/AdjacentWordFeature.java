package analysis;

public class AdjacentWordFeature {
	
	/**
	 * Returns the adjacent words as a feature.
	 * @param tokens The words of the original sentence
	 * @param numAdj The number of adjacent words to use in the feature
	 * @return
	 */
	public static String[] getFeature (String[] tokens, int numAdj) {
		String[] feature = new String[tokens.length];

		for (int i = 0; i < tokens.length; i++) {
			feature[i] = "";
			for (int j = i - numAdj; j <= i + numAdj; j++)
				if (j >= 0 && j < tokens.length && j != i)
					feature[i] += tokens[j] + " ";
		}
		
		return feature;
	}
	
	public static void main (String[] args) {
		String[] tokens = new String[] {"The","dog","ate","the","homework."};
		AdjacentWordFeature.getFeature(tokens, 3);
	}
}

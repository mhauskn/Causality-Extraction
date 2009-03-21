package analysis;

/**
 * The features interface specifies a common method for all of our features.
 */
public interface Feature {
	/**
	 * Given a string of tokens representing our sentence, this 
	 * method will apply the feature and return a resultant string array.
	 */
	public String[] getFeature (String[] tokens);
}

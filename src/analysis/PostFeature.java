package analysis;

/**
 * A post feature depend on more information than the simple feature.
 * For example many features depend on knowing where the cue phrase
 * is located in the sentence.
 */
public interface PostFeature {
	/**
	 * The Post Feature will be given some extra information in the form of our 
	 * feature array
	 * @param tokens The words in the sentence
	 * @param features The extra information needed
	 */
	public String[] getFeature (String[] tokens, String[] features);
}

package mallet;

public class Include {
	public static final String MALLET_DIR = "mallet_out";
	
	public static final String SENT_DELIM = "_END_OF_SENTENCE_ N";
	public static final String SENT_DELIM_REDUX = "_END_OF_SENTENCE_";
	public static final String CAUSE_TAG = "C";
	public static final String CAUSE_BEGIN_TAG = "CB";
	public static final String CAUSE_INTERMEDIATE_TAG = "CI";
	public static final String CAUSE_END_TAG = "CE";
	
	public static final String EFFECT_TAG = "E";
	public static final String EFFECT_BEGIN_TAG = "EB";
	public static final String EFFECT_INTERMEDIATE_TAG = "EI";
	public static final String EFFECT_END_TAG = "EE";
	
	public static final String NEITHER_TAG = "N";
	
	
	/**
	 * Removes the class labels from tuples of the form
	 * <token> <feat1> ... <featN> <class>
	 */
	public static String[] removeClassLabels (String[] tuples) {
		String[] out = new String[tuples.length];
		for (int i = 0; i < tuples.length; i++) {
			out[i] = tuples[i].substring(0, tuples[i].lastIndexOf(' '));
		}
		return out;
	}
}
 
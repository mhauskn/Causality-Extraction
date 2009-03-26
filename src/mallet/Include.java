package mallet;

import haus.io.FileReader;

import java.util.ArrayList;

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
	
	public static final String RELN_TAG = "R";
	public static final String RELN_BEGIN_TAG = "RB";
	public static final String RELN_INTERMEDIATE_TAG = "RI";
	public static final String RELN_END_TAG = "RE";
	
	public static final String NEITHER_TAG = "N";
	
	public static final String CAUSE_REGEXP = "(" + CAUSE_TAG + "|" + CAUSE_BEGIN_TAG +
		"|" + CAUSE_INTERMEDIATE_TAG + "|" + CAUSE_END_TAG + ")";
	
	public static final String EFFECT_REGEXP = "(" + EFFECT_TAG + "|" + EFFECT_BEGIN_TAG + 
		"|" + EFFECT_INTERMEDIATE_TAG + "|" + EFFECT_END_TAG + ")";
	
	
	// ------------------ Methods for Dealing with CRF Files ----------------------
	
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
	
	/**
	 * Gets the token from a tuple of the form
	 * <token> <feat1> ... <featN> <class>
	 */
	public static String getToken (String line) {
		return line.substring(0, line.indexOf(' '));
	}
	
	/**
	 * Gets the list of features from a tuple of the form
	 * <token> <feat1> ... <featN> <class>
	 */
	public static String getFeature (String line) {
		int startInd = line.indexOf(' ');
		int endInd = line.lastIndexOf(' ');
		if (startInd == -1 || startInd == endInd)
			return "";
		return line.substring(startInd + 1,endInd);
	}
	
	/**
	 * Gets the label from a tuple of the form
	 * <token> <feat1> ... <featN> <classLabel>
	 */
	public static String getLabel (String line) {
		return line.substring(line.lastIndexOf(' ')+1, line.length());
	}
	
	/**
	 * Reads a file deliminated by SENT_DELIM tags into an 
	 * arraylist where each entry is a sentence.
	 */
	public static ArrayList<String> readSentDelimFile (String file) {
		ArrayList<String> sentences = new ArrayList<String>();
		FileReader reader = new FileReader(file);
		String sent = "";
		String line;
		while ((line = reader.getNextLine()) != null) {
			sent += line + "\n";
			if (line.contains(SENT_DELIM_REDUX)) {
				sentences.add(sent);
				sent = "";
			}
		}
		return sentences;
	}
	
	/**
	 * Checks if a given string contains the sentence 
	 * delim token
	 */
	public static boolean hasSentDelim (String str) {
		return str.contains(SENT_DELIM_REDUX);
	}
}
 
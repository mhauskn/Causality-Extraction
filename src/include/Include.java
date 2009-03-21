package include;

import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

public class Include 
{
	public static final String CAUSE_PHRASE = "CP";
	
	public static final String EFFECT_PHRASE = "EP";
			
	public static final String END_SENT_PUNC = ".!?;";
	
	public static final String MISC_PUNC = ".?!:;-()[]'\"/,";
	
	public static final String NOUN_REGEXP = "(nn|nns|np)";
	
	public static final String EXTENDED_NOUN_REGEXP = "(nn\\$|nns\\$|np\\$)";
	
	public static final String CAUSE_TAG = "<CAUSE>";
	
	public static final String EFFECT_TAG = "<EFFECT>";
	
	public static TokenizerFactory TOKENIZER_FACTORY 
		= new RegExTokenizerFactory("(-|'|\\d|\\p{L})+|\\S");
	
	//---------------Some File Paths-------------------//
	public static final String hmmFile = "Hmm/pos-en-general-brown.HiddenMarkovModel";
	
	public static final String pcfgPath = "stanford/englishPCFG.ser.gz";
	
	public static final String lookupPath = "stanford/sentence_lookup";
	
	//---------------Methods-------------------//
	
	/**
	 * Removes punctuation from the given string
	 */
	public static String removePunctuation (String s) {
		return s.replaceAll("[\\.!\\?;:\"',]", "");
	}
}

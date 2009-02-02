package stemmer;

import java.util.List;

import edu.mit.jwi.morph.SimpleStemmer;

public class BasicStemmer {
	SimpleStemmer ss = null;
	
	public BasicStemmer() {
		ss = new SimpleStemmer();
	}
	
	/**
	 * Stems a single word, returning the stemmed version 
	 * or the original word if no stem exists.
	 */
	public String stem (String word) {
		List<String> stems = ss.findStems(word);
		if (!stems.isEmpty())
			return stems.get(stems.size()-1);
		return word;
	}
	
	/**
	 * Splits and stems a sentence
	 */
	public String[] stemSentence (String sentence) {
		String[] words = sentence.split(" ");
		String[] out = new String[words.length];
		
		for (int i = 0; i < words.length; i++) 
			out[i] = stem(words[i]);
		return out;
	}
	
	public static void main (String[] args) {
		String toStem = "smoking";
		BasicStemmer bs = new BasicStemmer();
		System.out.println(bs.stem(toStem));
	}
}

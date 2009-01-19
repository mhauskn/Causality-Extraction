package pos;

import include.Include;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

import com.aliasi.util.Streams;

public class PosTagger {	
	HmmDecoder hmmDec;
	
	static TokenizerFactory TOKENIZER_FACTORY 
    	= new RegExTokenizerFactory("(-|'|\\d|\\p{L})+|\\S");
	
	public PosTagger (String _hmm) {
		hmmDec = getHmmDecoder(_hmm);
	}
	
	private HmmDecoder getHmmDecoder (String hmmLocation) {
		try {
			FileInputStream fileIn = new FileInputStream(hmmLocation);
	        ObjectInputStream objIn = new ObjectInputStream(fileIn);
	        HiddenMarkovModel hmm = (HiddenMarkovModel) objIn.readObject();
	        Streams.closeInputStream(objIn);
	        return new HmmDecoder(hmm);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String tag (String s) {
		StringBuilder out = new StringBuilder();
		char[] cs = s.toCharArray();
		Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(cs,0,cs.length);
        String[] tokens = tokenizer.tokenize();
        
        String[] tags = hmmDec.firstBest(tokens);
        for (int i = 0; i < tokens.length; ++i)
            out.append(tokens[i] + "_" + tags[i] + " ");
        return out.toString();
	}
	
	public String[] getTags (String[] tokens) {
        return hmmDec.firstBest(tokens);
	}
	
	public ArrayList<String> getNouns (String s) {
		ArrayList<String> out = new ArrayList<String>();
		char[] cs = s.toCharArray();
		Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(cs,0,cs.length);
        String[] tokens = tokenizer.tokenize();
        
        String[] tags = hmmDec.firstBest(tokens);
        for (int i = 0; i < tokens.length; ++i) {
        	if (tags[i].matches(Include.NOUN_REGEXP)) { 
        		out.add(tokens[i].toLowerCase());
        	}
        }
        return out;
	}
}

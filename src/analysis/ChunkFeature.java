package analysis;

import include.Include;
import chunker.AbstractPhraseChunker;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.tokenizer.Tokenizer;

public class ChunkFeature {
	AbstractPhraseChunker ac = null;
	
	public ChunkFeature (AbstractPhraseChunker _ac) {
		ac = _ac;
	}
	
	public String recoverSentence (String[] tokens) {
		String sent = "";
		for (int i = 0; i < tokens.length; i++) {
			sent += tokens[i] + " ";
		}
		return sent;
	}
	
	public String[] getFeature (String[] tokens) {
		String[] feature = new String[tokens.length];
		String sent = recoverSentence(tokens);
		Chunking ck = ac.chunkSentence(sent);
		for (Chunk chunk : ck.chunkSet()) { //Add Chunks to feature
			int index = 0;
	        String type = chunk.type();
	        int start = chunk.start();
	        int end = chunk.end();
	        char[] phrase = sent.substring(start, end).toCharArray();
	        Tokenizer tokenizer = Include.TOKENIZER_FACTORY.tokenizer(phrase,0,phrase.length);
	        String[] small = tokenizer.tokenize();
	        for (int i = index; i < tokens.length; i++) {
	        	if (index >= small.length)
	        		break;
	        	while (index < small.length && small[index].equals(tokens[i])) {
	        		feature[i] = type + "Phrase";
	        		index++;
	        	}
	        }
	    }
		return feature;
	}
	
	/**
	 * @deprecated Use getFeature!
	 */
	public void addFeature (String[] features, String[] tokens) {
		String sent = recoverSentence(tokens);
		Chunking ck = ac.chunkSentence(sent);
		for (Chunk chunk : ck.chunkSet()) { //Add Chunks to feature
			int index = 0;
	        String type = chunk.type();
	        int start = chunk.start();
	        int end = chunk.end();
	        char[] phrase = sent.substring(start, end).toCharArray();
	        Tokenizer tokenizer = Include.TOKENIZER_FACTORY.tokenizer(phrase,0,phrase.length);
	        String[] small = tokenizer.tokenize();
	        for (int i = index; i < tokens.length; i++) {
	        	if (index >= small.length)
	        		break;
	        	while (index < small.length && small[index].equals(tokens[i])) {
	        		features[i] = features[i] + " " + type + "Phrase";
	        		index++;
	        	}
	        }
	    }
	}
}

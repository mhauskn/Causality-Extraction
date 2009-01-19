package chunker;

import java.io.File;
import java.io.IOException;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.AbstractExternalizable;
import com.aliasi.util.FastCache;

/**
 * AbstractPhraseChunker provides another level of abstraction for 
 * our PhraseChunker. 
 * @author epn
 *
 */
public class AbstractPhraseChunker 
{
	private static final int CACHE_SIZE = 50000;
		
	private PhraseChunker chunker;
	
	public AbstractPhraseChunker (String hmmLocation)
	{
		// parse input params
		File hmmFile = new File(hmmLocation);
        FastCache<String,double[]> cache = new FastCache<String,double[]>(CACHE_SIZE);

        // read HMM for pos tagging
        HiddenMarkovModel posHmm;
        try {
            posHmm
                = (HiddenMarkovModel)
                AbstractExternalizable.readObject(hmmFile);
        } catch (IOException e) {
            System.out.println("Exception reading model=" + e);
            e.printStackTrace(System.out);
            return;
        } catch (ClassNotFoundException e) {
            System.out.println("Exception reading model=" + e);
            e.printStackTrace(System.out);
            return;
        }

        // construct chunker
        HmmDecoder posTagger  = new HmmDecoder(posHmm,null,cache);
        TokenizerFactory tokenizerFactory = new IndoEuropeanTokenizerFactory();
        chunker = new PhraseChunker(posTagger,tokenizerFactory);
	}
	
	/** 
	 * Chunks a sentence into constituent parts and prints the result
	 * correctly.
	 * @param str The sentence to chunk
	 */
	public void showChunking (String str)
	{
		Chunking chunking = chunker.chunk(str);
        CharSequence cs = chunking.charSequence();
        System.out.println("\n" + cs);
        for (Chunk chunk : chunking.chunkSet()) {
            String type = chunk.type();
            int start = chunk.start();
            int end = chunk.end();
            CharSequence text = cs.subSequence(start,end);
            System.out.println("  " + type + "(" + start + "," + end + ") " + text);
        }
	}
	
	public Chunking chunkSentence (String sentence)
	{
		return chunker.chunk(sentence);
	}
}

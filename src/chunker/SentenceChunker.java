package chunker;

import com.aliasi.sentences.MedlineSentenceModel;
import com.aliasi.sentences.SentenceModel;

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;

import java.util.ArrayList;

import parser.AbstractParser;

/**
 * Chunks blocks of text into sentences
 * @author epn
 *
 */
public class SentenceChunker {
	
	/**
	 * Parser will give us text to chunk into sentences
	 */
	private AbstractParser parser;
	
	private ArrayList<String> sentenceList = new ArrayList<String>();
	
	private static final TokenizerFactory TOKENIZER_FACTORY = new IndoEuropeanTokenizerFactory();
    
	private static final SentenceModel SENTENCE_MODEL  = new MedlineSentenceModel();
	
	private static long sentenceCount = 0;

	public SentenceChunker (AbstractParser ap)
	{
		parser = ap;
	}
	
	public String getNextSentence ()
	{
		sentenceCount++;
		
		if (sentenceList.size() > 0)
			return sentenceList.remove(0);
		
		// Keep parsing blocks until we get more sentences
		boolean hasMoreText = true;
		while (hasMoreText && sentenceList.size() == 0)
			hasMoreText = parseNextBlock();
		
		if (!hasMoreText)
			return null;
				
		return sentenceList.remove(0);
	}
	
	public long getSentenceCount ()
	{
		return sentenceCount;
	}
    	
    /**
     * Parses the next block of text and stores the sentences
     * @return false if we a truly out of text to parse.
     */
    private boolean parseNextBlock ()
    {
    	String text = parser.getTextBlock();
    	
    	if (text == null)
    		return false;

    	ArrayList<String> tokenList = new ArrayList<String>();
    	ArrayList<String> whiteList = new ArrayList<String>();
    	
    	Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(text.toCharArray(),0,text.length());
    	tokenizer.tokenize(tokenList,whiteList);

    	String[] tokens = new String[tokenList.size()];
    	String[] whites = new String[whiteList.size()];
    	tokenList.toArray(tokens);
    	whiteList.toArray(whites);
    		
    	int[] sentenceBoundaries = SENTENCE_MODEL.boundaryIndices(tokens,whites);
    			
    	if (sentenceBoundaries.length < 1) 
    	    return true;
    		
    	int sentStartTok = 0;
    	int sentEndTok = 0;
    	for (int i = 0; i < sentenceBoundaries.length; ++i) 
    	{
    	    sentEndTok = sentenceBoundaries[i];
    	    String currSent = "";
    	    //System.out.println("SENTENCE "+(i+1)+": ");
    	    for (int j=sentStartTok; j<=sentEndTok; j++) 
    	    {
    	    	//System.out.print(tokens[j]+whites[j+1]);
    	    	currSent += tokens[j]+whites[j+1];
    	    }
    	    sentenceList.add(currSent);
    	    sentStartTok = sentEndTok+1;
    	}
    	return true;
    }
}

package parser.Stanford;

import include.Include;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import haus.io.Closable;
import haus.io.Closer;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.Tree;

/**
 * The Stanford parser is a wrapper around stanford's great parser. It's useful 
 * for getting syntatic dependencies, parses and POS.
 */
public class StanfordParser implements Closable {
	public static final String sep_punc = Include.END_SENT_PUNC + Include.INTRA_SENT_PUNC;
	
	public static final String NP_REGEXP = "(NN|NNS|NP|NNP|NNPS)";
	public static final String VP_REGEXP = "(VP|VB|VBN|VBG|VBD|VBP|VBZ)";
	public static final String V_REGEXP = "(VB|VBN|VBG|VBD|VBP)";
	public static final String DT_REGEXP = "DT";
	
	public static final String NounPhrase = "NP";
	public static final String VerbPhrase = "VP";
		
	public static final int MAX_SENT_LEN = 80;
	
	Hashtable<String,Tree> lookup = null;
	Hashtable<String,Tree> contracted_lookup = null;
	
	boolean lookup_modified = false;
	
	LexicalizedParser lp = null;
	
	private static StanfordParser ref;
		
	private StanfordParser () {}
	
	public static StanfordParser getStanfordParser ()
	{
		if (ref == null)
			ref = new StanfordParser();
		return ref;
	}
	
	public static StanfordParser getStanfordParser (Closer c)
	{
		if (ref == null)
			ref = new StanfordParser();
		ref.register(c);
		return ref;
	}
	
	/**
	 * This method assumes that we wish to modify our lookup hashtable
	 * by adding new entries and serializing them at the end of the
	 * program
	 */
	void register (Closer c) {
		c.registerClosable(this);
	}
	
	/**
	 * De-serializes our stored sentence lookup
	 */
	@SuppressWarnings("unchecked")
	void initLookup () {
		if (haus.io.FileReader.exists(include.Include.lookupPath))
			lookup = (Hashtable<String, Tree>) 
				haus.io.Serializer.deserialize(include.Include.lookupPath);
		else 
			lookup = new Hashtable<String,Tree>();
	}
	
	/**
	 * Initializes the parser with the PCFG file. We don't want to do this 
	 * if possible -- best to rely on our previously hashed sentences.
	 */
	void initLexParser () {
		lp = new LexicalizedParser(include.Include.pcfgPath);
	    lp.setOptionFlags(new String[]{"-maxLength", Integer.toString(MAX_SENT_LEN), "-retainTmpSubcategories"});
	}
	
	/**
	 * Returns a parse tree for the given sentence.
	 * This parse tree will be a smaller version with punctuation 
	 * left at the end of each node. We use a separate HT to 
	 * retrieve these nodes so that the same sentence can give
	 * us both a expanded and a contracted HT without modifications.
	 */
	@SuppressWarnings("unchecked")
	public Tree getParseTree (String[] tokens) {
		Tree t;
		removeQuotes(tokens);
		tokens = separatePunc(tokens);
		String key = hashFunc(tokens);
		if (contracted_lookup == null) 
			contracted_lookup = (Hashtable<String, Tree>) 
				haus.io.Serializer.deserialize(include.Include.lookupPath);
		if (contracted_lookup.containsKey(key))
			t = contracted_lookup.get(key);
		else {
			if (lp == null)
				initLexParser();
			t = (Tree) lp.apply(Arrays.asList(tokens));
		}
		return TreeOps.combinePunc(t);
	}
	
	/**
	 * Returns an expanded parse tree for the given sentence. This 
	 * parse tree will include separate nodes for punctuation.
	 */
	public Tree getExpandedParseTree (String[] tokens) {
		removeQuotes(tokens);
		tokens = separatePunc(tokens);
		String key = hashFunc(tokens);
		if (lookup == null) initLookup();
		if (lookup.containsKey(key))
			return lookup.get(key);
		
		//System.out.println("Sentence Not Found: " + key);
		if (lp == null)
			initLexParser();
		Tree t = (Tree) lp.apply(Arrays.asList(tokens));
		lookup.put(key, t);
		lookup_modified = true;
		return t;
	}
	
	/**
	 * Checks if a given leaf is simply a punctuation mark such as a 
	 * comma or a period.
	 */
	public static boolean isPunc (String s) {
		return StanfordParser.sep_punc.contains(s);
	}
		
	/**
	 * Creates a string representation of string array.
	 * This is useful for serving as a hash key.
	 */
	String hashFunc (String[] toks) {
		String out = "";
		for (String s : toks)
			out += s + " ";
		return out;
	}
	
	/**
	 * Serializes our quick-lookup hash table
	 */
	public void close () {
		if (lookup != null && lookup_modified) 
			haus.io.Serializer.serialize(lookup, include.Include.lookupPath);
	}
	
	/**
	 * Removes quotes from our tokens. These quotes have been seen 
	 * to really mess up Parses.
	 */
	void removeQuotes (String[] tokens) {
		for (int i = 0; i < tokens.length; i++)
			tokens[i] = tokens[i].replaceAll("(''|``)", "");
	}
	
	/**
	 * Separates punctuation: messy, --> messy ,
	 * This punctuation needs to be separated otherwise the parser
	 * will mis-interpret the words.
	 */
	public static String[] separatePunc (String[] tokens) {
		ArrayList<String> out = new ArrayList<String>();
		for (int i = 0; i < tokens.length; i++) {
			String tok = tokens[i];
			if (hasSepPunc(tok)) {
				out.add(tok.substring(0, tok.length()-1));
				out.add("" + tok.charAt(tok.length()-1));
			} else
				out.add(tok);
		}
		
		return haus.misc.Conversions.toStrArray(out);
	}
	
	/**
	 * Checks if a string has punctuation which needs to be separated 
	 * from it. Ex: messy, --> messy ,
	 */
	public static boolean hasSepPunc (String tok) {
		char last_char = tok.charAt(tok.length()-1);
		return tok.length() > 1 && sep_punc.indexOf(last_char) >= 0;
	}
}

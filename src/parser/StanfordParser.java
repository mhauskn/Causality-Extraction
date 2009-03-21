package parser;

import include.Include;
import io.InteractiveReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

/**
 * The Stanford parser is a wrapper around stanford's great parser. It's useful 
 * for getting syntatic dependencies, parses and POS.
 */
public class StanfordParser {
	public static final String NP_REGEXP = "(NN|NNS|NP|NNP|NNPS)";
	public static final String VP_REGEXP = "(VP|VB|VBN|VBG|VBD)";
	public static final String V_REGEXP = "(VB|VBN|VBG|VBD)";
	public static final String DT_REGEXP = "DT";
	
	public static final String NounPhrase = "NP";
	public static final String VerbPhrase = "VP";
		
	public static final int MAX_SENT_LEN = 80;
	
	Hashtable<String[],Tree> lookup = null;
	LexicalizedParser lp = null;
	
	@SuppressWarnings("unchecked")
	public StanfordParser () {
	    lookup = (Hashtable<String[], Tree>) haus.io.Serializer.deserialize(include.Include.lookupPath);
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
	 * Returns a parse Tree for the given sentence. Ideally we want to
	 * do quick lookup in our HT, but if not possible then we will 
	 * actually do the parse.
	 */
	public Tree getParseTree (String[] tokens) {
		if (lookup.containsKey(tokens))
			return lookup.get(tokens);
		else {
			if (lp == null)
				initLexParser();
			return (Tree) lp.apply(Arrays.asList(tokens));
		}
	}
	
	/**
	 * Gives an informative parse of a sentence displaying the 
	 * different types of available information.
	 */
	public void parseSentenceTest (String sentence) {
		System.out.println("Original Sentence: " + sentence);
		String[] sent = sentence.split(" ");
		Tree parse = getParseTree(sent);
		
		System.out.println("---------PENN PRINT-----------");
	    parse.pennPrint();
	    System.out.println();
	    System.out.println("---------END PENN PRINT-----------");

	    TreebankLanguagePack tlp = new PennTreebankLanguagePack();
	    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
	    Collection<TypedDependency> tdl = gs.typedDependenciesCollapsed(); // This is the good one
	    
	    System.out.println("---------TDL PRINT-----------");
	    System.out.println(tdl);
	    System.out.println();
	    System.out.println("---------END TDL PRINT-----------");

	    System.out.println("---------TREE PRINT-----------");
	    TreePrint tp = new TreePrint("penn,typedDependenciesCollapsed");
	    tp.printTree(parse);
	    System.out.println("---------END TREE PRINT-----------");
	}
	
	//------------------- Tree Operations -----------------------
	
	/**
	 * Returns a list of POS tags corresponding to the words of the sentence
	 */
	public String[] getPOSTags (Tree t) {
		ArrayList<String> tags = new ArrayList<String>();
		for (Tree leaf : t.getLeaves()) {
			Tree parent = leaf.parent(t);
			String pos  = parent.label().toString();
			tags.add(pos);
		}
		return haus.misc.Conversions.toStrArray(tags);
	}
	
	/**
	 * Returns a collection of dependencies between words of the sentence
	 */
	public Collection<TypedDependency> getDependencies (Tree parse) {
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
	    return gs.typedDependenciesCollapsed();
	}
	
	//------------------- End Tree Operations ------------------
	
	public String getFeatures (String sentence)
	{
		if (sentence.length() == 0)
			return "";
		
		String[] sent = sentence.split(" ");
		ArrayList<String> words = new ArrayList<String>();
		for (int i = 0; i < sent.length; i++) {
			String word = sent[i].trim();
			if (word.length() > 0) {
				words.add(word);
			}
		}
		
		if (words.size() == 0)
			return "";
		
		//Does this worK?
		sent = words.<String>toArray(new String[words.size()]);
		
		//sent = new String[words.size()];
		//for (int i = 0; i < words.size(); i++)
		//	sent[i] = words.get(i);
		
		String[] features = new String[words.size()];
		String[] labels = new String[words.size()];
		
		for (int i = 0; i < words.size(); i++) {
			String word = words.get(i);
			if (word.indexOf(Include.CAUSE_TAG) != -1) {
				sent[i] = word.replaceAll(Include.CAUSE_TAG, "");
				labels[i] = "CAUSE";
			} else if (word.indexOf(Include.EFFECT_TAG) != -1) {
				sent[i] = word.replaceAll(Include.EFFECT_TAG, "");
				labels[i] = "EFFECT";
			} else {
				labels[i] = "NON_CAUSAL";
			}
		}
		
		for (int i = 0; i < features.length; i++)
	    	features[i] = "";
		
		Tree parse = (Tree) lp.apply(Arrays.asList(sent));

	    TreebankLanguagePack tlp = new PennTreebankLanguagePack();
	    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
	    Collection<TypedDependency> tdl = gs.typedDependenciesCollapsed();
		
		Iterator<TypedDependency> it = tdl.iterator();
		while (it.hasNext()) {
			TypedDependency td = it.next();
			String reln = td.reln().toString();
			
			TreeGraphNode dep = td.dep();
			int arrayIndexDep = getDependencyValue(dep.toString());
			features[arrayIndexDep-1] += "is_" + reln + " ";
			
			TreeGraphNode gov = td.gov();
			int arrayIndexGov = getDependencyValue(gov.toString());
			features[arrayIndexGov-1] += "has_" + reln + " ";
		}
		
		features[features.length-1] += "STOPWORD ";
		
		String out = "";
		
		for (int i = 0; i < sent.length; i++) {
			out += sent[i] + " " + features[i] + labels[i] + "\n";
		}
		
		return out;
	}
	
	/**
	 * Seperates a dependency index from the dependency word: det(the-3, dog-4)
	 * the-3 will return 2. This is because the numbering starts from 1 not 0
	 */
	public static int getDependencyValue (String nodeStr) {
		int divide = nodeStr.lastIndexOf('-');
		String num = nodeStr.substring(divide+1,nodeStr.length());
		return Integer.parseInt(num) -1;
	}
	
	/**
	 * Seperates a dependency value from the dependency word: "the-3"
	 * will return "the".
	 */
	public static String getDependencyKey (String depString) {
		int divide = depString.lastIndexOf('-');
		return depString.substring(0, divide);
	}
	
	/**
	 * Returns the index of the dependent member of a typed 
	 * dependency
	 */
	public static int getDepIndex (TypedDependency td) {
		return getDependencyValue(td.dep().label().toString());
	}
	
	/**
	 * Returns the index of the governing member of a typed 
	 * dependency
	 */
	public static int getGovIndex (TypedDependency td) {
		return getDependencyValue(td.gov().label().toString());
	}
	
	/**
	 * Given an index for a word in the sentence get the 
	 * most direct parent noun phrase or verb phrase. Returned
	 * as int[0] = word index for start of noun phrase
	 * int[1] = word index for end of noun phrase
	 */
	public int[] getParentNpVp (Tree root, Tree[] indexed_trees, int wordIndex) {
		int[] out = new int[2];
		Tree start = indexed_trees[wordIndex];
		Tree t = start;
		
		String label;
		do {
			t = t.parent(root);
			label = t.label().toString();
		} while (!label.equals(NounPhrase));
		
		List<Tree> leaves = t.getLeaves();		
		Tree left = leaves.get(0);
		int size = leaves.size();
		
		// Linear search through indexed word tree for matching leaf... Ugh but tree should be small
		for (int i = 0; i < indexed_trees.length; i++) {
			if (indexed_trees[i].equals(left)) {
				out[0] = i;
				break;
			}
		}
		
		out[1] = out[0] + size -1;
		
		return out;
	}
	
	public static void main(String[] args) {
		StanfordParser sp = new StanfordParser();
		String sent;
		
		InteractiveReader iReader = new InteractiveReader();
		while ((sent = iReader.getInput()) != null) {
			sp.parseSentenceTest(sent);
		}
	}
}

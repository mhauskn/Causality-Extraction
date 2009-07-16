package parser.Stanford;

import java.util.Collection;

import turk.writers.HumanToCRF;


import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import haus.io.InteractiveReader;
import haus.misc.Map;

/**
 * Allows easy interactive parsing of sentences with the Stanford parser
 */
public class InteractiveStanfordParser implements Map<String> {
	static StanfordParser sp = StanfordParser.getStanfordParser();
	
	public InteractiveStanfordParser () {
		System.out.println("Welcome to the interactive stanford parser. Type in a sentence.");
		InteractiveReader ir = new InteractiveReader(this);
		ir.run();
	}
	
	/**
	 * Gives an informative parse of a sentence displaying the 
	 * different types of available information.
	 */
	public static void parseSentenceTest (String[] toks) {
		//Tree parse = sp.getParseTree(sent);
		Tree parse = sp.getExpandedParseTree(toks);

	    treePrint(parse);
	}
	
	public static void treePrint (Tree parse) {
		TreePrint tp = new TreePrint("penn,typedDependenciesCollapsed");
	    tp.printTree(parse);
	}
	
	public static void pennPrint (Tree parse) {
		parse.pennPrint();
	    System.out.println();
	}
	
	public static void tdlPrint (Tree parse) {
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
	    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
	    Collection<TypedDependency> tdl = gs.typedDependenciesCollapsed(); // This is the good one
	    System.out.println(tdl);
	}
	
	public static String[] detagSentence (String[] toks) {
		String[] out = new String[toks.length];
		for (int i = 0; i < toks.length; i++)
			out[i] = HumanToCRF.removeTags(toks[i], HumanToCRF.OPEN_TAGS, HumanToCRF.CLOSE_TAGS);
		return out;
	}
	
	public void map(String arg0) {
		parseSentenceTest(detagSentence(arg0.split(" ")));
	}
	
	public static void main (String[] args) {
		new InteractiveStanfordParser();
	}
}

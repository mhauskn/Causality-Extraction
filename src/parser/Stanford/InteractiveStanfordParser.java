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
	StanfordParser sp = StanfordParser.getStanfordParser();
	
	public InteractiveStanfordParser () {
		System.out.println("Welcome to the interactive stanford parser. Type in a sentence.");
		InteractiveReader ir = new InteractiveReader(this);
		ir.run();
	}
	
	/**
	 * Gives an informative parse of a sentence displaying the 
	 * different types of available information.
	 */
	public void parseSentenceTest (String sentence) {
		System.out.println("Original Sentence: " + sentence);
		String[] sent = sentence.split(" ");
		//Tree parse = sp.getParseTree(sent);
		Tree parse = sp.getExpandedParseTree(sent);
		
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
	
	public void map(String arg0) {
		String detagged = "";
		for (String word : arg0.split(" "))
			detagged += HumanToCRF.removeTags(word, HumanToCRF.OPEN_TAGS, HumanToCRF.CLOSE_TAGS) + " ";
		parseSentenceTest(detagged);
	}
	
	public static void main (String[] args) {
		new InteractiveStanfordParser();
	}
}

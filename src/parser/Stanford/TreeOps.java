package parser.Stanford;

import haus.misc.Condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

/**
 * This class contains many common static operations performed on
 * Stanford Parse Trees.
 */
public class TreeOps {
	/**
	 * Punctuation is separated into different tokens to improve quality
	 * of Stanford's parse. This method will combine the punctuation
	 * producing a parse tree as if the punctuation were never separated.
	 */
	public static Tree combinePunc (Tree t) {
		Tree[] leaves = getLeaves(t);
		for (int i = 0; i < leaves.length; i++) {
			Tree leaf = leaves[i];
			String label = leaf.label().toString();
			if (label.length() == 1 && StanfordParser.sep_punc.contains(label)) {
				Tree exp = expandWhile(t, leaf, new stringMatcher(label));
				int childNum = getChildNum(t,exp);
				exp.parent(t).removeChild(childNum);
				Tree prev = leaves[i-1];
				prev.setValue(prev.label().toString()+label);
				return combinePunc(t);
			}
		}
		return t;
	}
	
	/**
	 * Returns the Tree t's child number with respect
	 * to T's parent. If t is the first child it will return 1
	 */
	public static int getChildNum (Tree root, Tree t) {
		Tree[] children = t.parent(root).children();
		for (int i = 0; i < children.length; i++) {
			if (children[i].equals(t))
				return i;
		}
		return -1;
	}
	
	/**
	 * Returns a list of POS tags corresponding to the words of the sentence
	 */
	public static String[] getPOSTags (Tree t) {
		ArrayList<String> tags = new ArrayList<String>();
		for (Tree leaf : t.getLeaves()) {
			Tree parent = leaf.parent(t);
			String pos  = parent.label().toString();
			tags.add(pos);
		}
		return haus.misc.Conversions.toStrArray(tags);
	}
	
	/**
	 * Returns the lowest level leaves in our tree root
	 */
	public static Tree[] getLeaves (Tree root) {
	    List<Tree> trees = root.getLeaves();
	    Tree[] out = new Tree[trees.size()];
	    out = trees.toArray(out);
	    return out;
	}
	
	/**
	 * Returns next sibling of tree t
	 */
	public static Tree getNextChild (Tree t, Tree root) {
		Tree parent = t.parent(root);
		Tree[] children = parent.children();
		for (int i = 0; i < children.length; i++) {
			if (children[i].equals(t) && i != children.length -1)
				return children[i+1];
		}
		return null;
	}
	
	/**
	 * Given an index for a word in the sentence get the 
	 * most direct parent noun phrase or verb phrase. Returned
	 * as int[0] = word index for start of noun phrase
	 * int[1] = word index for end of noun phrase
	 */
	public static int[] getParentNpVp (Tree root, Tree[] indexed_trees, int wordIndex) {
		int[] out = new int[2];
		Tree start = indexed_trees[wordIndex];
		Tree t = start;
		
		String label;
		do {
			t = t.parent(root);
			label = t.label().toString();
		} while (!label.equals(StanfordParser.NounPhrase));
		
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
	
	/**
	 * Expands from the given tree until the given condition is satisfied
	 */
	public static Tree expandUntil (Tree root, Tree subtree, Condition<Tree> cond) {
		Tree t = subtree;
		
		while (t != null && !cond.satisfied(t))
			t = t.parent(root);
		
		return t;
	}
	
	/**
	 * Expands the given tree until the condition is satisfied
	 */
	public static Tree expandWhile (Tree root, Tree subtree, Condition<Tree> cond) {
		Tree t = subtree;
		Tree out = subtree;
		
		while (t != null && cond.satisfied(t)) {
			out = t;
			t = t.parent(root);
		}
		
		return out;
	}
	
	/**
	 * Return true once regular expression is matched
	 */
	public static class regexpMatcher implements Condition<Tree> {
		String regexp;
		public regexpMatcher (String r) {
			regexp = r;
		}
		public boolean satisfied (Tree t) {
			return t.label().toString().matches(regexp);
		}
	}
	
	/**
	 * Returns true once the string is matched
	 */
	public static class stringMatcher implements Condition<Tree> {
		String s;
		public stringMatcher (String r) {
			s = r;
		}
		public boolean satisfied (Tree t) {
			return t.label().toString().equals(s);
		}
	}
	
	/**
	 * Returns an integer with the high and low bounds of the given subtree
	 * with respect to the full root tree.
	 * 
	 * If root = { "1","2","3","4"}
	 * and subtree = { "2","3" }
	 * 
	 * This method should return: 
	 * new int = { 1 , 2 }
	 */
	public static int[] getSubTreeBoundaries (Tree root, Tree subtree) {
		if (root == null || subtree == null)
			return null;
		int left_char_index = root.leftCharEdge(subtree);
		if (left_char_index < 0) return null;
		int size = subtree.getLeaves().size();
		Tree[] indexed_tree = getLeaves(root);
		int charcnt = 0;
		if (left_char_index == 0)
			return new int [] { 0, size-1 };
		for (int i = 0; i < indexed_tree.length; i++) {
			charcnt += indexed_tree[i].label().toString().length();
			if (left_char_index == charcnt)
				return new int [] { i + 1, i+size };
		}
		return null;
	}
	
	// Expand while VP and not including other root
	
	
	// Expand to largest NP/S not including other root
	
	
	// ---------------- Dependency Stuff ----------------------//
	
	/**
	 * Returns a collection of dependencies between words of the sentence
	 */
	public static Collection<TypedDependency> getDependencies (Tree parse) {
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
	    Collection<TypedDependency> deps = gs.typedDependenciesCollapsed();
	    return deps;
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
}

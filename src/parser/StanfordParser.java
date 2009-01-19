package parser;

import include.Include;
import io.InteractiveReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

public class StanfordParser {
	
	public static final String NP_REGEXP = "(NN|NNS|NP|NNP|NNPS)";
	public static final String VP_REGEXP = "(VP|VB|VBN|VBG|VBD)";
	public static final String V_REGEXP = "(VB|VBN|VBG|VBD)";
	public static final String DT_REGEXP = "DT";
	
	public static final String NounPhrase = "NP";
	public static final String VerbPhrase = "VP";
	
	public static final int MAX_SENT_LEN = 50;
	
	private String pcfgPath;
	private LexicalizedParser lp;
	
	private String sentence;
	
	private Tree parse;
	
	private Collection<TypedDependency> tdl;
	
	private TypedDependency[] tdArr;
	
	private ArrayList<String> causeDeps;
	private ArrayList<TypedDependency> deps;
    
    private boolean acSingle = false; 
    private boolean acDouble = false; 
    private boolean passSingle = false;
    
    private String[] out = new String[5];
    
    private Tree affTree;
    private Tree effTree;
    private Tree verbTree;
    
    private Tree affPhraseTree;
    private Tree effPhraseTree;
    private Tree verbPhraseTree;
    
    private boolean foundAffTree = false;
    private boolean foundEffTree = false;
    private boolean foundVerbTree = false;
    
    
	/**
	 * Constructor
	 * @param _pcfgPath
	 */
	public StanfordParser (String _pcfgPath)
	{
		pcfgPath = _pcfgPath;
	    lp = new LexicalizedParser(pcfgPath);
	    lp.setOptionFlags(new String[]{"-maxLength", "80", "-retainTmpSubcategories"});
	}
	
	public StanfordParser () {
		pcfgPath = include.Include.pcfgPath;
	    lp = new LexicalizedParser(pcfgPath);
	    lp.setOptionFlags(new String[]{"-maxLength", "80", "-retainTmpSubcategories"});
	}
	
	/**
	 * Parses the affector cause and effect from a causal sentence.
	 */
	public String[] parseSentence (String _sentence)
	{
		sentence = _sentence;
		out[0] = out[1] = out[2] = out[3] = out[4] = "";
		System.out.println(sentence);
		sentence = removeTrailingPunc();
		
		String[] sent = sentence.split(" ");
		
		if (sent.length > MAX_SENT_LEN)
			return null;
		
		parse = (Tree) lp.apply(Arrays.asList(sent));

	    TreebankLanguagePack tlp = new PennTreebankLanguagePack();
	    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
	    tdl = gs.typedDependenciesCollapsed();
	    
	    tdArr = new TypedDependency[tdl.size()];
	    
	    if (!findCauseDeps())
	    	return out;
	    
	    if(!findSentType())
	    	return out;
	    
	    //System.out.println(out[0] + " ---> " + out[1] + " ---> " + out[2]);
	    
	    findKeywords();
	    
	    findPhrases();
	    
	    if (affPhraseTree != null) 
	    	out[3] = affPhraseTree.toString();
	    else
	    	out[3] = "";
	    if (effPhraseTree != null)
	    	out[4] = effPhraseTree.toString();
	    else
	    	out[4] = "";
	    
	    //System.out.println(affPhraseTree.toString());
	    //System.out.println(effPhraseTree.toString());
	    
	    System.out.println(out[0] + "\t" + out[3] + "\t" + out[1] + "\t" + out[4] + "\t" + out[2]);
		
		return out;
	}
	
	public Tree getParseTree (String[] words) {
		Tree parse = (Tree) lp.apply(Arrays.asList(words));
		return parse;
	}
	
	public void parseSentenceTest (String sentence)
	{
		System.out.println("Original Sentence: " + sentence);
		String[] sent = sentence.split(" ");
		Tree parse = (Tree) lp.apply(Arrays.asList(sent));
		
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
		
		//System.out.println(tdl.toString());
	}
	
	/**
	 * Returns the dependencies between the words. Very Useful! -- Includes word numbers
	 */
	public Collection<TypedDependency> getDependencies (String[] tokens) {
		Tree parse = getParseTree(tokens);
		return getDependencies(parse);
	}
	
	public Collection<TypedDependency> getDependencies (Tree parse) {
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
	    return gs.typedDependenciesCollapsed();
	}
	
	public String[] getTokenTags (String[] tokens) {
		Tree parse = getParseTree(tokens);
		return getTokenTags(parse);
	}
	
	/**
	 * Traverses the tree getting the tokens for each of the labels.
	 * @param parse
	 * @return
	 */
	public String[] getTokenTags (Tree parse) {
		ArrayList<String> tags = new ArrayList<String>();
		LinkedList<Tree> queue = new LinkedList<Tree>();
		queue.add(parse);
		
		while (!queue.isEmpty()) {
			Tree t = queue.removeLast();
			if (t.isLeaf())
				tags.add(t.parent(parse).label().toString());
			for (Tree child : t.children())
				queue.addFirst(child);
		}
		String[] out = new String[tags.size()];
		out = tags.toArray(out);
		return out;
	}
	
	/**
	 * Returns an array of trees. Each array element is the tree
	 * for the word located at that index in the sentence.
	 */
	public Tree[] getWordIndexedTree (Tree root) {
		List<Tree> trees = root.getLeaves();
		Tree[] out = new Tree[trees.size()];
		out = trees.toArray(out);
		return out;
	}
	
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
	
	/**
	 * Finds All Dependencies containing the word Cause
	 */
	private boolean findCauseDeps ()
	{
		int i = 0;
		causeDeps = new ArrayList<String>();
		deps = new ArrayList<TypedDependency>();
	    // Find all TDs containing cause words
	    for (Iterator<TypedDependency> it = tdl.iterator(); it.hasNext();)	
	    {
	    	TypedDependency td = it.next();
	    	String gov = td.gov().toString();
	    	if (gov.matches(Include.STANFORD_GRAM_CAUSE_REGEXP))
	    	{
	    		causeDeps.add(td.reln().toString());
	    		deps.add(td);
	    	}
	    	tdArr[i++] = td;
	    }
	    System.out.println("Cause Dependencies:" + causeDeps.toString());
	    return true;
	}
	
	
	/**
	 * Guesses if we have an active or passive sentence based on deps
	 */
	private boolean findSentType ()
	{
		acSingle = false;
		acDouble = false;
		passSingle = false;
		
		if (causeDeps.contains("nsubj"))
	    {
	    	if (causeDeps.contains("dobj")) // Active 1-Clause
	    	{
	    		System.out.println("Active 1-Clause");
	    		acSingle = true;
	    	}
	    	
	    	if (causeDeps.contains("xcomp")) // Active 2-Clause
	    	{
	    		System.out.println("Active 2-Clause");
	    		acDouble = true;
	    	}
	    }
	    
	    if (causeDeps.contains("agent"))
	    {
	    	if (causeDeps.contains("nsubjpass")) // Passive 1-Clause
	    	{
	    		System.out.println("Passive 1-Clause");
	    		passSingle = true;
	    	}
	    }

	    if (acSingle && acDouble || acSingle && passSingle || acDouble && passSingle)
	    {
	    	System.out.println("Multiple rules satisfied! We have a messed up sentence!");
	    	return false;
	    }
	    
	    if (!acSingle && !acDouble && !passSingle)
	    {
	    	System.out.println("No Rules Satisfied! We have a bad sentence!");
	    	return false;
	    }
	    
	    if (acSingle)
	    {
	    	out = getRelns(deps, out, "nsubj", "dobj");
	    }
	    
	    if (acDouble)
	    {
	    	out = getRelns(deps, out, "nsubj", "xcomp");
	    }
	    
	    if (passSingle)
	    {
	    	out = getRelns(deps, out, "agent", "nsubjpass");
	    }
	    
	    return true;
	}
	
	/**
	 * Extracts the relations from the given sentence
	 */
	private String[] getRelns (ArrayList<TypedDependency> deps, String[] out, 
			String id1, String id2)
	{
		String affector = "";
	    String verb = "";
	    String effect = "";
	    String verbConf = "";
	    
		for (TypedDependency d : deps)
    	{
    		if (d.reln().toString().equals(id1))
    		{
    			affector = cleanString(d.dep().toString());
    			verb = cleanString(d.gov().toString());
    			out[0] = cleanPunc(affector);
    			out[1] = cleanPunc(verb);
    		}
    		
    		if (d.reln().toString().equals(id2))
    		{
    			effect = cleanString(d.dep().toString());
    			out[2] = cleanPunc(effect);
    			verbConf = cleanString(d.gov().toString());
    		}
    	}
		
    	if (!verbConf.equals(verb))
		{
			System.out.println("Verbs " + verb + " and " + verbConf + " dont match!!");
		}
    	
    	if (affector.length() == 0 || verb.length() == 0 || effect.length() == 0)
	    {
	    	System.out.println("Something went wrong... We have string of zero Length!");
	    }
    	
    	return out;
	}
	
	private static String cleanPunc (String messy)
	{
		String clean = messy.toLowerCase().trim();
		
		if (clean.indexOf(",") != -1)
		{
			clean = clean.replaceAll(",", "");
		}
		if (clean.indexOf("\"") != -1)
		{
			clean = clean.replaceAll("\"", "");
		}
		if (clean.indexOf(";") != -1)
		{
			clean = clean.replaceAll(";", "");
		}
		if (clean.indexOf(":") != -1)
		{
			clean = clean.replaceAll(":", "");
		}
		
		return clean;
	}
	
	private String cleanString (String messy)
	{
		int splitIndex = messy.lastIndexOf('-');
		return messy.substring(0,splitIndex);
	}
	
	private String removeTrailingPunc ()
	{
		String out = sentence.trim();
		
		while (Include.END_SENT_PUNC.indexOf(out.charAt(out.length()-1)) != -1)
		{
			out = out.substring(0, out.length()-1);
		}
		
		return out;
	}
	
	private void findKeywords ()
	{
		foundAffTree = foundEffTree = foundVerbTree = false;
		
		String affector = out[0];
		String verb = out[1];
		String effect = out[2];
		
		searchTree(affector, verb, effect, parse);
		
		if (!foundAffTree || !foundEffTree || !foundVerbTree)
		{
			System.out.println("Problem finding root affector, verb, or effect trees!!");
			return;
		}
	}
	
	private void searchTree (String affector, String verb, String effect, Tree t)
	{
		if (t.label().toString().equals(affector))
		{
			if (foundAffTree)
				System.out.println("Found two trees for affector " + affector + " This is bad!");
			affTree = t;
			foundAffTree = true;
		}
		
		if (t.label().toString().equals(effect))
		{
			if (foundEffTree)
				System.out.println("Found two trees for effect " + effect + " This is bad!");
			effTree = t;
			foundEffTree = true;
		}
		
		if (t.label().toString().equals(verb))
		{
			if (foundVerbTree)
				System.out.println("Found two trees for verb " + verb + " This is bad!");
			verbTree = t;
			foundVerbTree = true;
		}
		
		if (!foundAffTree || !foundEffTree || !foundVerbTree)
		{
			for (Tree t2 : t.children())
				searchTree(affector, verb, effect, t2);
		}
	}
	
	private void findPhrases ()
	{
		if (affTree != null) 
			affPhraseTree = getPhrase(affTree, NP_REGEXP);
		if (effTree != null)
			effPhraseTree = getPhrase(effTree, NP_REGEXP);
		
		
		if (acDouble)
		{
			verbPhraseTree = getPhrase(verbTree, VP_REGEXP);
			
			Tree test;
			if ((test = bfs(verbPhraseTree, "NP")) != null)
				effPhraseTree = test;
		}
	}
	
	/**
	 * Traverses as far up as is allowed by the regExp and returns that tree
	 */
	private Tree getPhrase (Tree t, String regExp)
	{
		Tree curr = t;
		Tree parent = t.parent(parse);
		while (parent != null && parent.label().toString().matches(regExp))
		{
			curr = parent;
			parent = parent.parent(parse);
		}
		
		return curr;
	}
	
	/**
	 * BreathFirstSearch on tree t for string matching regExp
	 */
	private Tree bfs (Tree root, String regExp)
	{
		if (root.label().toString().matches(regExp))
			return root;
		
		Vector<Tree> queue = new Vector<Tree>();
		queue.add(root);
		
		while (!queue.isEmpty())
		{
			Tree t = queue.remove(0);
			for (Tree child : t.children())
			{
				if (child.label().toString().matches(regExp))
					return child;
				queue.add(child);
			}
		}
		
		return null;
	}
	
	
	public String testParse (String sentence)
	{
		String[] sent = sentence.split(" ");
		Tree parse = (Tree) lp.apply(Arrays.asList(sent));
		
	    //parse.pennPrint();
	    //System.out.println();

	    TreebankLanguagePack tlp = new PennTreebankLanguagePack();
	    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
	    Collection<TypedDependency> tdl = gs.typedDependenciesCollapsed(); // This is the good one
	    
	    //System.out.println(tdl);
	    //System.out.println();

	    //TreePrint tp = new TreePrint("penn,typedDependenciesCollapsed");
	    //tp.printTree(parse);
		
		return tdl.toString();
	}
	
	public static void main(String[] args) {
		StanfordParser sp = new StanfordParser("stanford/englishPCFG.ser.gz");
		String sent = "The slack absorbs the pulling strain generated by an earthquake.";
		
		InteractiveReader iReader = new InteractiveReader();
		while ((sent = iReader.getInput()) != null) {
			sp.parseSentenceTest(sent);
		}
	}
}

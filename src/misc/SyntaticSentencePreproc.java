package misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;

import parser.Stanford.StanfordParser;
import parser.Stanford.TreeOps;
import stemmer.BasicStemmer;

/**
 * Preprocesses a sentence for the task of syntatic commonality pattern extraction. This 
 * generally consists of:
 * 
 * 1. Identifying known causes and effects and their surrounding nps
 * 2. Stemming all the words in the sentence
 * 3. Finding the syntatic dependencies
 * @author epn
 *
 */
public class SyntaticSentencePreproc {
	// Variables
	String known_cause, known_effect;
	ArrayList<String> sentences;
	String curr_sentence; String[] curr_sentence_tokens;
	int cause_start; int cause_end;
	int effect_start; int effect_end;
	String[] pos_tags;
	Tree tree;
	ArrayList<String> features;
	ArrayList<TypedDependency>[] indexed_deps;
	ArrayList<TypedDependency>[] reverse_indexed_deps;
	Hashtable<TypedDependency,Boolean> visited = new Hashtable<TypedDependency,Boolean>();
	Hashtable<String,Boolean> processed_sentences = new Hashtable<String,Boolean>();
	
	// Tools
	StanfordParser parser = new StanfordParser();
	BasicStemmer stemmer = new BasicStemmer();
	
	public ArrayList<String[]> findSynDeps 
		(String cause, String effect, ArrayList<String> _sentences) {
		String[] cause_parts = cause.split(" ");
		String[] effect_parts = effect.split(" ");
		known_cause = "";
		known_effect = "";
		for (String s : cause_parts)
			known_cause += stemmer.stem(procWord(s)) + " ";
		for (String s : effect_parts)
			known_effect += stemmer.stem(procWord(s)) + " ";
		known_cause = known_cause.substring(0, known_cause.length()-1);
		known_effect = known_effect.substring(0, known_effect.length()-1);
		sentences = _sentences;
		
		ArrayList<String[]> doubleArr = new ArrayList<String[]>();
		
		for (int i = 0; i < sentences.size(); i++) {
			curr_sentence = sentences.get(i);
			if (processed_sentences.containsKey(curr_sentence))
				continue;
			processed_sentences.put(curr_sentence, true);
			ArrayList<String> tags = processSentence();
			String[] tagsArr = new String[tags.size()];
			tags.toArray(tagsArr);
			if (tags != null)
				doubleArr.add(tagsArr);
		}
		
		return doubleArr;
	}
	
	/**
	 * Processes the current sentence
	 */
	ArrayList<String> processSentence () {
		features = new ArrayList<String>();
		curr_sentence_tokens = curr_sentence.split(" ");
		if (!screenSentence())
			return null;
		tree = parser.getParseTree(curr_sentence_tokens);
		pos_tags = TreeOps.getPOSTags(tree);
		try {
			checkDuplicates();
			findCauseEffect();
		} catch (Exception e) {
			return null;
		}
		createSyntaticFeatures();
		return features;
	}
	
	/**
	 * Screens out improper sentences
	 */
	boolean screenSentence () {
		if (curr_sentence_tokens.length >= StanfordParser.MAX_SENT_LEN)
			return false;
		return true;
	}
	
	/**
	 * Checks and handles the duplicate causes and effects
	 */
	void checkDuplicates () throws Exception {
		int causeIndex = curr_sentence.indexOf(known_cause);
		int lastCauseIndex = curr_sentence.lastIndexOf(known_cause);
		int effectIndex = curr_sentence.indexOf(known_effect);
		int lastEffectIndex = curr_sentence.lastIndexOf(known_effect);
		
		if (causeIndex == -1 || effectIndex == -1) {
			System.out.printf("Cause (%s) or effect (%s) not found in sentence: %s\n", 
					known_cause, known_effect, curr_sentence);
			throw new Exception();
		}
		
		if (causeIndex != lastCauseIndex) {
			int end = causeIndex + known_cause.length();
			String newTask = curr_sentence.substring(0, causeIndex) +
			curr_sentence.substring(end, curr_sentence.length());
			sentences.add(newTask);
		}
		
		if (effectIndex != lastEffectIndex) {
			int end = effectIndex + known_effect.length();
			String newTask = curr_sentence.substring(0, effectIndex) +
				curr_sentence.substring(end, curr_sentence.length());
			sentences.add(newTask);
		}
	}
	
	/**
	 * Finds the word index of cause and effect phrases.
	 */
	void findCauseEffect () throws Exception {
		String[] cause_split = known_cause.split(" ");
		cause_start = getPhraseStart(cause_split);
		cause_end = cause_start + cause_split.length -1;
		
		String[] effect_split = known_effect.split(" ");
		effect_start = getPhraseStart(effect_split);
		effect_end = effect_start + effect_split.length -1;
		
		Tree[] wordIndexedTree = TreeOps.getLeaves(tree);
		cause_start = TreeOps.getParentNpVp(tree, wordIndexedTree, cause_start)[0];
		cause_end = TreeOps.getParentNpVp(tree, wordIndexedTree, cause_end)[1];
		
		effect_start = TreeOps.getParentNpVp(tree, wordIndexedTree, effect_start)[0];
		effect_end = TreeOps.getParentNpVp(tree, wordIndexedTree, effect_end)[1];
	}
	
	/**
	 * Using the dependencies inside of the sentence as 
	 * well as the stems of each word, features are created 
	 * linking the other words in the sentence to the cause
	 * and effect phrases.
	 */
	void createSyntaticFeatures () {
		Collection<TypedDependency> deps = 
			TreeOps.getDependencies(tree);
		getIndexedDeps(deps);
		makeFeatures();
	}
	
	/**
	 * Finds the TypedDeps for each word of the form:
	 * det(The-1, hurricane-2). This will simplify the further process.
	 * @param deps
	 * @return
	 */
	@SuppressWarnings("unchecked")
	void getIndexedDeps (Collection<TypedDependency> deps) {
		indexed_deps = new ArrayList[curr_sentence_tokens.length];
		reverse_indexed_deps = new ArrayList[curr_sentence_tokens.length];
				
		for (TypedDependency td : deps) {
			int dep_index = TreeOps.getDepIndex(td);
			int gov_index = TreeOps.getGovIndex(td);
			if (indexed_deps[dep_index] == null) {
				ArrayList<TypedDependency> a = new ArrayList<TypedDependency>();
				a.add(td);
				indexed_deps[dep_index] = a;
			} else {
				indexed_deps[dep_index].add(td);
			}
			
			if (reverse_indexed_deps[gov_index] == null) {
				ArrayList<TypedDependency> a = new ArrayList<TypedDependency>();
				a.add(td);
				reverse_indexed_deps[gov_index] = a;
			} else {
				reverse_indexed_deps[gov_index].add(td);
			}
		}
	}
	
	/**
	 * Walks through our known causes and effects and works outward
	 * finding all other words related to these.
	 */
	void makeFeatures () {
		for (int i = cause_start; i <= cause_end; i++) {
			visited.clear();
			markedTraversal(i, include.Include.CAUSE_PHRASE);
		}
		
		for (int i = effect_start; i <= effect_end; i++) {
			visited.clear();
			markedTraversal(i, include.Include.EFFECT_PHRASE);
		}
	}
	
	/**
	 * Creates a actual single feature of the type
	 * absorb-dobj-EP
	 */
	void makeFeature (TypedDependency td, String phraseReln, boolean reversed) {
		int gov_index = TreeOps.getDependencyValue(td.gov().toString());
		int dep_index = TreeOps.getDependencyValue(td.dep().toString());
		
		if (inBasePhrase(gov_index) && inBasePhrase(dep_index))
			return;
		
		String connection = "_of_";
		String reln = td.reln().toString();
		String gov = TreeOps.getDependencyKey(td.gov().toString());
		if (reversed) {
			gov = TreeOps.getDependencyKey(td.dep().toString());
			gov_index = dep_index;
			connection = "_fo_";
		}
		String stemmed_gov = stemmer.stem(gov);
		stemmed_gov = procWord(stemmed_gov);
		String new_phrase_reln = "<" + reln + ">" + connection + phraseReln;
		
		String feature = stemmed_gov + "-" + new_phrase_reln;
		
		features.add(feature);
		
		markedTraversal (gov_index, new_phrase_reln);
	}
	
	void markedTraversal (int index, String relnPhrase) {
		if (indexed_deps[index] != null)
			for (TypedDependency new_dep : indexed_deps[index]) {
				if (visited.containsKey(new_dep))
					continue;
				visited.put(new_dep, true);
				makeFeature(new_dep, relnPhrase, false);
			}
		if (reverse_indexed_deps[index] != null)
			for (TypedDependency new_dep : reverse_indexed_deps[index]) {
				if (visited.containsKey(new_dep))
					continue;
				visited.put(new_dep, true);
				makeFeature(new_dep, relnPhrase, true);
			}
	}
	
	boolean inBasePhrase (int index) {
		if (index >= cause_start && index <= cause_end)
			return true;
		if (index >= effect_start && index <= effect_end)
			return true;
		return false;
	}
	
	/**
	 * Get the word number of the matched phrase.
	 * Fails if unable to match a cause or effect in the sentence.
	 */
	int getPhraseStart (String[] phrase_segs) throws Exception {
		for (int i = 0; i < curr_sentence_tokens.length; i++) {
			if (phrase_segs[0].equals(stemmer.stem(procWord(curr_sentence_tokens[i])))) {
				boolean matched = true;
				for (int j = 1; j < phrase_segs.length; j++) 
					if (!phrase_segs[j].equals(stemmer.stem(procWord(curr_sentence_tokens[i+j])))) 
						matched = false;
				if (matched)
					return i;
			}
		}
		System.out.println("No matching Phrase Found!");
		for (String s : phrase_segs)
			System.out.print(s + " ");
		System.out.println("\n" + curr_sentence);
		throw new Exception();
	}
	
	/**
	 * Processes a word for matching. Generally just removes
	 * punctuation which might throw off a match.
	 */
	String procWord (String word) {
		word = include.Include.removePunctuation(word);
		word = word.toLowerCase();
		return word;
	}
	
	public static void main (String[] args) {
		ArrayList<String> words = new ArrayList<String>();
		words.add("The slack absorbs the pulling strain generated by an earthquake.");
		SyntaticSentencePreproc ssp = new SyntaticSentencePreproc();
		ssp.findSynDeps("earthquake", "strain", words);
	}
}

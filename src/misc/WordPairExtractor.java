package misc;

import haus.io.DataWriter;
import haus.io.FileReader;
import haus.io.IO;
import haus.io.Pipe;

import java.util.ArrayList;

import parser.TrecParser;

import chunker.SentenceChunker;

/**
 * Scans through a corpus and extract sentences which 
 * contain our specified pairs of words. This is used for 
 * finding causally related word pairs.
 * @author epn
 *
 */
public class WordPairExtractor extends IO<String,String> {
	public static String in_file = "Trec";
	public static String out_file = UnsupervisedCauseExtractor.file_path + "wordPairs.txt";
	
	ArrayList<String[]> word_pairs;
	ArrayList<? extends Pipe<String>> outputs;
	
	/**
	 * Lazy man's constructor
	 */
	public WordPairExtractor () {
		FileReader reader = new FileReader(in_file);
		reader.setRecursive(true);
		reader.setFolderRegExp("ap");
		TrecParser parser = new TrecParser(reader);
		SentenceChunker sent_chunker = new SentenceChunker(parser);
		DataWriter writer = new DataWriter(out_file);
		in = sent_chunker;
		out = writer;
	}
	
	/**
	 * Working man's constructor
	 */
	public WordPairExtractor(Pipe<String> input, Pipe<String> output) {
		super(input, output);
	}
	
	public void extractWordPairs (ArrayList<String[]> pairs) {
		ArrayList<DataWriter> writers = new ArrayList<DataWriter>();
		for (String[] pair : pairs)
			writers.add(new DataWriter(genFileName(pair[0],pair[1])));
		extractWordPairs(pairs, writers);
		for (DataWriter writer : writers)
			writer.close();
	}
	
	/**
	 * Generates a word pair file name
	 */
	public static String genFileName (String cause, String effect) {
		return cause + "_" + effect + "_pair.txt";
	}
	
	/**
	 * Extracts the sentences containing any of the given 
	 * word pairs.
	 * @param pairs: String[0] = word1; String[1] = word2;
	 */
	public void extractWordPairs (ArrayList<String[]> pairs, 
			ArrayList<? extends Pipe<String>> pair_outputs) {
		word_pairs = pairs;
		outputs = pair_outputs;
		String line;
		while ((line = in.get()) != null)
			if (containsWordPair(line))
				out.add(line);
	}
	
	/**
	 * Checks if a given line contains any of our word 
	 * pairs.
	 */
	boolean containsWordPair (String line) {
		for (int i = 0; i < word_pairs.size(); i++) {
			String[] pair = word_pairs.get(i);
			String word1 = pair[0];
			String word2 = pair[1];
			if (containsWordPair(word1, word2, line)) {
				out = outputs.get(i);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks if a given line contains a given pair
	 * of words
	 */
	boolean containsWordPair (String word1, String word2, String line) {
		if (line.indexOf(word1) > 0 && line.indexOf(word2) > 0)
			return true;
		return false;
	}
	
	public static void main (String[] args) {
		ArrayList<String[]> pairs = new ArrayList<String[]>();
		pairs.add(new String[] {"earthquake","tsunami"});
		pairs.add(new String[] {"moon","tide"});
		pairs.add(new String[] {"smok","cancer"});
		WordPairExtractor.in_file = args[0];
		WordPairExtractor.out_file= args[1];
		WordPairExtractor extractor = new WordPairExtractor();
		extractor.extractWordPairs(pairs);
	}

	public void mapInput (String e) {}
}

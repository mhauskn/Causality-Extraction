package misc;

import java.util.ArrayList;
import haus.io.FileReader;

/**
 * High Level class to do Unsupervised Causal Extraction.
 * @author Administrator
 *
 */
public class UnsupervisedCauseExtractor {
	public static String file_path = "UnsupExt/";
	
	ArrayList<String> sentences;
	
	/**
	 * Step 1: Gather probable causal sentences.
	 */
	public void gatherCausalMaterial (String file_name) {
		sentences = FileReader.readFile(file_name);
	}
	
	/**
	 * Step 2: Extract syntactic similarities
	 */
	public void extractSyntaticSimilarities (String known_cause, String known_effect) {
		SyntaticSimilarityExtractor sim = new SyntaticSimilarityExtractor();
		sim.extractSyntaticSimilarities(sentences, known_cause, known_effect);
	}
	
	public static void main (String[] args) {
		//UnsupervisedCauseExtractor ext = new UnsupervisedCauseExtractor();
		ArrayList<String[]> pairs = new ArrayList<String[]>();
		pairs.add(new String[] {"earthquake","tsunami"});
		pairs.add(new String[] {"moon","tide"});
		pairs.add(new String[] {"smoke","cancer"});
		/*
		for (String[] pair : pairs) {
			ext.getInitialMaterial(file_path + WordPairExtractor.genFileName(pair[0], pair[1]));
			ext.processMaterial(pair[0],pair[1]);
		}*/
		
		
	}
}

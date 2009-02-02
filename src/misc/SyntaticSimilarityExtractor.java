package misc;

import haus.io.DataWriter;
import haus.io.FileReader;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Designed to extract syntactic similarities 
 * between different sentences.
 */
public class SyntaticSimilarityExtractor {
	public static String file_path = UnsupervisedCauseExtractor.file_path;
	public static String basic_out_file = "basic_features.txt";
	public static String out_file = "synFeatures.ascii";
	public static String mafia_file = "mfi.txt";
	public static String results_file = "results.txt";
	
	SyntaticSentencePreproc preproc;
	
	ArrayList<String> sentences;
	String known_cause;
	String known_effect;
	
	Hashtable<String,Integer> featureIndex;
	ArrayList<String> attrs;
	ArrayList<String[]> features = new ArrayList<String[]>();
	
	/**
	 * Extract syntatic similarities
	 */
	public void extractSyntaticSimilarities (ArrayList<String> _sentences,
			String _known_cause, String _known_effect) {
		sentences = _sentences;
		known_cause = _known_cause;
		known_effect = _known_effect;
		
		preprocessSentences();
		writeFeatures();
		writeMAFIAFeatures();
		// Use mafia
		decodeMafiaFeatures();
		// Decode Mafia results
		// return results
	}
	
	/**
	 * Uses our SyntaticSentencePreproc to do some pre-processing
	 * of the sentences. This will ready them for similarity 
	 * extraction.
	 */
	void preprocessSentences () {
		features.addAll(preproc.findSynDeps(known_cause, known_effect, sentences));
	}
	
	/**
	 * Writes the features to a simple file. This 
	 * can be read and used to decode features.
	 */
	void writeFeatures () {
		DataWriter writer = new DataWriter(file_path + basic_out_file);
		for (String[] arr : features) {
			String line = "";
			for (String s : arr)
				line += s + " ";
			writer.write(line + "\n");
		}
		writer.close();
	}
	
	/**
	 * Writes MAFIA ascii format. This is the a simple
	 * lines of integer indexes.
	 */
	void writeMAFIAFeatures () {
		indexFeaturesSimple();
		DataWriter writer = new DataWriter(file_path + out_file);
		for (String[] arr : features) {
			String line = "";
			for (String s : arr)
				line += featureIndex.get(s) + " ";
			writer.writeln(line);
		}
		writer.close();
	}
	
	/**
	 * Reads features into our synFeatures array from the basic 
	 * features file
	 */
	void readFeatures () {
		features = new ArrayList<String[]>();
		FileReader reader = new FileReader(file_path + basic_out_file);
		String line;
		while ((line = reader.getNextLine()) != null)
			features.add(line.split(" "));
	}
	
	/**
	 * Does a simple indexing of the attributes
	 */
	void indexFeaturesSimple () {
		attrs = new ArrayList<String>();
		featureIndex = new Hashtable<String,Integer>();
		int count = 0;
		for (String[] arr : features) {
			for (String s : arr) {
				if (!featureIndex.containsKey(s)) {
					featureIndex.put(s, count++);
					attrs.add(s);
				}
			}
		}
	}
	
	/**
	 * Decodes features from our Mafia output
	 */
	void decodeMafiaFeatures () {
		readFeatures();
		indexFeaturesSimple();
		
		FileReader reader = new FileReader(file_path + mafia_file);
		DataWriter writer = new DataWriter(file_path + results_file);
		String line;
		while ((line = reader.getNextLine()) != null) {
			String[] int_toks = line.split(" ");
			String out = "";
			for (int i = 0; i < int_toks.length-1; i++) {
				int num = Integer.parseInt(int_toks[i]);
				out += attrs.get(num) + " ";
			}
			out += int_toks[int_toks.length-1];
			writer.write(out + "\n");
		}
		writer.close();
	}
}

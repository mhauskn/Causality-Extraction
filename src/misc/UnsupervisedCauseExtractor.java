package misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import haus.io.DataWriter;
import haus.io.FileReader;

public class UnsupervisedCauseExtractor {
	public static String file_path = "UnsupExt/";
	public static String file_name = "wordPairs.txt";
	public static String basic_out_file = "basic_features.txt";
	public static String out_file = "synFeatures.ascii";
	public static String mafia_file = "mfi.txt";
	public static String results_file = "results.txt";
	
	SyntaticSentencePreproc preproc;
	ArrayList<String> sentences;
	
	ArrayList<ArrayList<String>> synFeatures;
	Hashtable<String,int[]> featureIndex;
	ArrayList<String> attrs;
	
	public UnsupervisedCauseExtractor () {
		preproc = new SyntaticSentencePreproc();
	}
	
	public void getInitialMaterial () {
		FileReader reader = new FileReader(file_path + file_name);
		sentences = new ArrayList<String>();
		String line;
		while ((line = reader.getNextLine()) != null) {
			sentences.add(line);
		}
	}
	
	public void processMaterial () {
		synFeatures = preproc.findSynDeps("moon", "tide", sentences);
	}
	
	/**
	 * Writes the features to a simple file
	 */
	public void writeFeatures () {
		DataWriter writer = new DataWriter(file_path + basic_out_file);
		for (ArrayList<String> arr : synFeatures) {
			String line = "";
			for (String s : arr)
				line += s + " ";
			writer.write(line + "\n");
		}
		writer.close();
	}
	
	/**
	 * Reads features into our synFeatures array from the basic 
	 * features file
	 */
	void readFeatures () {
		synFeatures = new ArrayList<ArrayList<String>>();
		FileReader reader = new FileReader(file_path + basic_out_file);
		String line;
		while ((line = reader.getNextLine()) != null) {
			ArrayList<String> toks = new ArrayList<String>(Arrays.asList(line.split(" ")));
			synFeatures.add(toks);
		}
	}
	
	/**
	 * Writes the features into sparse ARFF format for Weka to deal with
	 */
	public void writeARFFFeatures () {
		indexFeatures();
		SparseArffWriter writer = new SparseArffWriter(file_path + out_file);
		writer.writeTitle("MoonTide");
		ArrayList<String> nomVal = new ArrayList<String>();
		nomVal.add("x");
		for (String s : attrs)
			writer.addNomAttribute(s, nomVal);
		
		for (ArrayList<String> arr : synFeatures) {
			ArrayList<Integer> indexes = new ArrayList<Integer>();
			for (String s : arr) {
				int[] a = featureIndex.get(s);
				int index = a[0];
				int count = a[1];
				if (count <= 1)
					continue;
				if (!indexes.contains(index))
					indexes.add(index);
			}
			if (indexes.size() > 0)
				writer.addSparseData(indexes, "x");
		}
		writer.close();
	}
	
	/**
	 * Writes MAFIA ascii format. This is the a simple
	 * lines of integer indexes.
	 */
	public void writeMAFIAFeatures () {
		indexFeaturesSimple();
		DataWriter writer = new DataWriter(file_path + out_file);
		for (ArrayList<String> arr : synFeatures) {
			String line = "";
			for (String s : arr) {
				int[] a = featureIndex.get(s);
				int index = a[0];
				line += index + " ";
			}
			writer.write(line + "\n");
		}
		writer.close();
	}
		
	/**
	 * Finds each unique feature and puts in a 
	 * hashtable with the feature number. Also creates
	 * a count for the number of times this feature
	 * is seen.
	 */
	void indexFeatures () {
		attrs = new ArrayList<String>();
		featureIndex = new Hashtable<String,int[]>();
		int count = 0;
		for (ArrayList<String> arr : synFeatures) {
			for (String s : arr) {
				if (!featureIndex.containsKey(s)) {
					int[] a = new int[] { 0, 1};
					featureIndex.put(s, a);
				} else {
					int[] a = featureIndex.get(s);
					if (a[1] == 1) // We know this has threshold
						attrs.add(s);
					a[0] = count++;
					a[1]++;
					featureIndex.put(s, a);
				}
			}
		}
	}
	
	/**
	 * Does a simple indexing of the attributes
	 */
	void indexFeaturesSimple () {
		attrs = new ArrayList<String>();
		featureIndex = new Hashtable<String,int[]>();
		int count = 0;
		for (ArrayList<String> arr : synFeatures) {
			for (String s : arr) {
				if (!featureIndex.containsKey(s)) {
					int[] a = new int[] { count++, 1};
					featureIndex.put(s, a);
					attrs.add(s);
				} else {
					int[] a = featureIndex.get(s);
					a[1]++;
					featureIndex.put(s, a);
				}
			}
		}
	}
	
	/**
	 * Decodes features from our Mafia output
	 */
	public void decodeMafiaFeatures () {
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
	
	public static void main (String[] args) {
		UnsupervisedCauseExtractor ext = new UnsupervisedCauseExtractor();
		ext.getInitialMaterial();
		ext.processMaterial();
		ext.writeFeatures();
		ext.writeMAFIAFeatures();
		//ext.decodeMafiaFeatures();
	}
}

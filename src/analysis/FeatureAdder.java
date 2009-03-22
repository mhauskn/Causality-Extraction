package analysis;

import java.util.ArrayList;

import mallet.Include;

import haus.io.DataWriter;
import haus.io.FileReader;
import haus.io.IO;
import parser.StanfordParser;
import analysis.features.POSFeature;
import analysis.features.StemFeature;

/**
 * Adds features to a bare CRF input file to make this file ready for 
 * final processing by CRF.
 * 
 * This class does 3 things:
 * 1. Reads a sentence from our bare CRF file
 * 2. Adds all of the features to that sentence
 * 3. Writes sentence back to our new CRF file
 */
public class FeatureAdder extends IO<String,String> {
	public static final String file_in = "crf/crf_bare.txt";
	public static final String file_out = "crf/crf.txt";
	
	ArrayList<Feature> feat_generators = new ArrayList<Feature>();
	
	String[] toks;
	String[] feats;
	String[] labels;
	
	ArrayList<String> _toks = new ArrayList<String>();
	ArrayList<String> _labels = new ArrayList<String>();
	
	/**
	 * Adds another feature generator to our list of feature generators
	 */
	public void addFeatureGenerator (Feature f) {
		feat_generators.add(f);
	}
	
	/**
	 * Map function for our input. Gets one sentence and divides it 
	 * into different parts.
	 */
	public void map (String line) {
		_toks.add(Include.getToken(line));
		_labels.add(Include.getLabel(line));
		
		if (Include.hasSentDelim(line)) {
			toks = haus.misc.Conversions.toStrArray(_toks);
			labels = haus.misc.Conversions.toStrArray(_labels);
			_toks.clear();
			_labels.clear();
			createFeatures();
			writePopulatedCRF();
		}
	}
	
	/**
	 * Creates a set of features for our tokens and labels. This is done
	 * by calling each of our feature generators with the strings
	 * in the sentence
	 */
	void createFeatures () {
		feats = new String[toks.length];
		for (int i = 0; i < feats.length; i++) feats[i] = "";
		for (Feature gen : feat_generators)
			addFeature(feats, gen.getFeature(toks));
	}
	
	/**
	 * Writes a sentence of the new CRF file. To do this toks/feats/labels
	 * must be populated.
	 */
	void writePopulatedCRF () {
		for (int i = 0; i < toks.length; i++)
			out.add(toks[i] + feats[i] + labels[i]);
	}
	
	/**
	 * Adds new feature to the old feature
	 */
	void addFeature (String[] features, String[] newFeature) {
		for (int i = 0; i < features.length; i++) {
			features[i] += newFeature[i] + " ";
		}
	}
	
	public static void main (String[] args) {
		DataWriter d = new DataWriter(file_out);
		FeatureAdder f = new FeatureAdder();
		f.setInput(new FileReader(file_in));
		f.setOutput(d);
		
		f.addFeatureGenerator(new StemFeature());
		f.addFeatureGenerator(new POSFeature(new StanfordParser()));
		
		f.mapInput();
		d.close();
	}
}

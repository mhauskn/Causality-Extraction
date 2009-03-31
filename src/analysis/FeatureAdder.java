package analysis;

import java.util.ArrayList;

import mallet.Include;

import haus.io.Closer;
import haus.io.DataWriter;
import haus.io.FileReader;
import haus.io.IO;
import parser.Stanford.StanfordParser;
import analysis.features.POSFeature;
import analysis.features.StemFeature;
import analysis.postFeatures.RelnDep;
import analysis.postFeatures.RelnProc;

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
	public static final String file_test = "crf/test.txt";
	public static final String file_out = "crf/crf.txt";
	
	ArrayList<Feature> feat_generators = new ArrayList<Feature>();
	ArrayList<PostFeature> post_feat_generators = new ArrayList<PostFeature>();
	
	String[] toks;
	String[] feats;
	String[] labels;
	
	ArrayList<String> _toks = new ArrayList<String>();
	ArrayList<String> _feats = new ArrayList<String>();
	ArrayList<String> _labels = new ArrayList<String>();
	
	String last_tok;
	String last_feat;
	String last_label;
	
	/**
	 * Adds another feature generator to our list of feature generators
	 */
	public void addFeatureGenerator (Feature f) {
		feat_generators.add(f);
	}
	
	public void addPostFeatureGenerator (PostFeature f) {
		post_feat_generators.add(f);
	}
	
	/**
	 * Map function for our input. Gets one sentence and divides it 
	 * into different parts.
	 */
	public void map (String line) {
		_toks.add(Include.getToken(line));
		_feats.add(Include.getFeature(line) + " ");
		_labels.add(Include.getLabel(line));
		
		if (Include.hasSentDelim(line)) {
			last_tok = _toks.remove(_toks.size()-1);
			last_feat = _feats.remove(_feats.size()-1);
			last_label = _labels.remove(_labels.size()-1);
			
			toks = haus.misc.Conversions.toStrArray(_toks);
			feats = haus.misc.Conversions.toStrArray(_feats);
			labels = haus.misc.Conversions.toStrArray(_labels);
			createFeatures(haus.misc.Conversions.toStrArray(_feats));
			writePopulatedCRF();
			_toks.clear();
			_feats.clear();
			_labels.clear();
		}
	}
	
	/**
	 * Creates a set of features for our tokens and labels. This is done
	 * by calling each of our feature generators with the strings
	 * in the sentence. 
	 * 
	 * We need the original features to avoid confusing our post feature
	 * generators.
	 */
	void createFeatures (String[] original_features) {
		for (Feature gen : feat_generators)
			addFeature(feats, gen.getFeature(toks));
		for (PostFeature pfeat : post_feat_generators)
			addFeature(feats, pfeat.getFeature(toks, original_features));
	}
	
	/**
	 * Writes a sentence of the new CRF file. To do this toks/feats/labels
	 * must be populated.
	 */
	void writePopulatedCRF () {
		for (int i = 0; i < toks.length; i++)
			out.add(toks[i].trim() + " " + feats[i].trim() + " " + labels[i].trim());
		out.add(last_tok + " " + last_feat + " " + last_label);
	}
	
	/**
	 * Adds new feature to the old feature
	 */
	void addFeature (String[] features, String[] newFeature) {
		for (int i = 0; i < features.length; i++) {
			String trimmedFeat = feats[i].trim();
			features[i] = trimmedFeat + " " + newFeature[i].trim() + " ";
		}
	}
	
	public static void main (String[] args) {
		Closer c = new Closer();
		StanfordParser sp = new StanfordParser(c);
		
		FeatureAdder f = new FeatureAdder();
		f.setInput((1 == 2) ? new FileReader(file_in) : new FileReader(file_test));
		f.setOutput(new DataWriter(file_out,c));
		
		f.addFeatureGenerator(new StemFeature());
		f.addFeatureGenerator(new POSFeature(sp));
		f.addPostFeatureGenerator(new RelnProc());
		f.addPostFeatureGenerator(new RelnDep(sp));
		
		f.mapInput();
		c.close();
	}
}

package analysis.features;

import haus.io.DataWriter;
import haus.io.FileReader;

import java.util.ArrayList;

import analysis.Feature;

import mallet.Include;

/**
 * The Adjacent Feature is key to our CRF.
 * It creates as features all of the features of the
 * previous and next words in the sentence.
 * 
 * This is necessary for CRF to take into account pre
 * and post word/feature dependencies.
 *
 * We shall call them residual features.
 */
public class AdjacentFeature implements Feature {
	static int numAdjacent = 3;
	public static final String tok_iden = "res(";
	public static boolean include_adj_feats = true;
	static int featsPerLine = -1;
	
	/**
	 * Gets additional features from the tokens and features of the 
	 * previous and subsequent tuples. Sent should be of the form:
	 * 
	 * <tok> <feat1> ... <featn> 
	 * 
	 * Do not include class label!
	 */
	public String[] getFeature (String[] tokens) {
		int len = tokens.length -1;
		String[] out = new String[tokens.length];
		for (int i = 0; i < out.length; i++)
			out[i] = "";
		for (int i = 0; i <= len; i++) {
			for (int j = i-numAdjacent; j <= i+numAdjacent; j++) {
				if (i == j) continue;
				if (j < 0 || j >= out.length)
					out[i] += genNullFeatures(j-i, featsPerLine);
				else
					out[i] += genFeatures(j-i,tokens[j].split(" "));
			}
		}
		return out;
	}
	
	//------------------------Helper functions-----------------------
	
	static String genFeatures (int major, String[] segs) {
		String out = "";
		if (include_adj_feats)
			for (String s : segs)
				out += genFeature(major, s);
		else
			out += genFeature(major, segs[0]);
		return out;
	}
	
	/**
	 * Generates a number of null features.
	 */
	static String genNullFeatures (int major, int numFeats) {
		String[] feats = new String[numFeats];
		for (int i = 0; i < feats.length; i++)
			feats[i] = "Null";
		return genFeatures(major,feats);
	}
	
	/**
	 * Generates a feature at the given word location with the 
	 * given token.
	 * 
	 * Major is the row that the feature comes
	 */
	static String genFeature (int major, String token) {
		return tok_iden + major + "):" + token + " ";
	}
	
	//------------------------End Help Functions--------------------
	
	/**
	 * Post Processes a file already containing features. This is probably the 
	 * safest and easiest way to gain access to this feature.
	 */
	public static void postProcessFile (String in_file, String out_file) {
		AdjacentFeature adj = new AdjacentFeature();
		ArrayList<String> feats = new ArrayList<String>();
		ArrayList<String> labels = new ArrayList<String>();
		FileReader reader = new FileReader(in_file);
		DataWriter writer = new DataWriter(out_file);

		String line;
		while ((line = reader.getNextLine()) != null) {
			if (featsPerLine == -1)
				featsPerLine = line.split(" ").length-1;
			if (Include.hasSentDelim(line)) {
				String[] strs = adj.getFeature(haus.misc.Conversions.toStrArray(feats));
				for (int i = 0; i < strs.length; i++)
					writer.writeln(feats.get(i) + strs[i] + labels.get(i));
				String toWrite = line.substring(0, line.lastIndexOf(' ')) + " ";
				for (int i = -numAdjacent; i <= numAdjacent; i++) {
					if (i == 0) continue;
					toWrite += genNullFeatures(i,featsPerLine);
				}
				writer.writeln(toWrite + Include.NEITHER_TAG);
				feats.clear();
				labels.clear();
			} else {
				String[] segs = line.split(" ");
				String tuple = "";
				for (int i = 0; i < segs.length-1; i++)
					tuple += segs[i] + " ";
				feats.add(tuple);
				labels.add(segs[segs.length-1]);
			}
		}
		writer.close();
	}
	
	public static void main (String[] args) {
		//AdjacentFeature.postProcessFile("crf/bogusCRF.txt", "crf/bogusCRF2.txt");
		AdjacentFeature.postProcessFile("crf/crf.txt", "crf/crf_final.txt");
	}
}

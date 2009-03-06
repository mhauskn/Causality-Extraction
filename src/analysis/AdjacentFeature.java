package analysis;

import haus.io.DataWriter;
import haus.io.FileReader;

import java.util.ArrayList;

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
public class AdjacentFeature {
	public static final String tok_iden = "res(";
	
	/**
	 * Gets additional features from the tokens and features of the 
	 * previous and subsequent tuples. Sent should be of the form:
	 * 
	 * <tok> <feat1> ... <featn> 
	 * 
	 * Do not include class label!
	 */
	static String[] getSmallFeature (String[] sent, int num_adj) {
		int len = sent.length -1;
		String[] out = new String[sent.length];
		for (int i = 0; i < out.length; i++)
			out[i] = "";
		for (int i = 0; i <= len; i++) {
			for (int j = i-num_adj; j <= i+num_adj; j++) {
				if (i == j) continue;
				if (j < 0 || j >= out.length)
					out[i] += genFeature(j-i,"Null");
				else
					out[i] += genFeatures(j-i,sent[j].split(" "));
			}
		}
		return out;
	}
	
	static String[] getSmallFeature (ArrayList<String> sent, int num_adj) {
		return getSmallFeature(toStrArray(sent), num_adj);
	}
	
	/**
	 * Wrapper function which takes a string array with our 
	 * sent delim tokens included.
	 */
	public static String[] getFeature (String[] sent, int num_adj) {
		ArrayList<String> features = new ArrayList<String>();
		ArrayList<String> tmp = new ArrayList<String>();
		for (int i = 0; i < sent.length; i++) {
			if (Include.hasSentDelim(sent[i])) {
				for (String res : getSmallFeature(toStrArray(tmp), num_adj))
					features.add(res);
				tmp.clear();
				features.add(sent[i]);
			} else 
				tmp.add(sent[i]);
		}
		return toStrArray(features);
	}
	
	public static String[] getFeature (ArrayList<String> sent, int num_adj) {
		return getFeature(toStrArray(sent), num_adj);
	}
	
	public static String[] toStrArray (ArrayList<String> ar) {
		String[] out = new String[ar.size()];
		out = ar.toArray(out);
		return out;
	}
	
	// Helper functions
	static String genFeatures (int major, String[] segs) {
		String out = "";
		for (String s : segs)
			out += genFeature(major, s);
		return out;
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
	
	public static void postProcessFile (String in_file, String out_file) {
		ArrayList<String> feats = new ArrayList<String>();
		ArrayList<String> labels = new ArrayList<String>();
		FileReader reader = new FileReader(in_file);
		DataWriter writer = new DataWriter(out_file);

		String line;
		while ((line = reader.getNextLine()) != null) {
			if (Include.hasSentDelim(line)) {
				String[] strs = getSmallFeature(feats, 4);
				for (int i = 0; i < strs.length; i++)
					writer.writeln(feats.get(i) + strs[i] + labels.get(i));
				writer.writeln(line);
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
		AdjacentFeature.postProcessFile("crf/bogusCRF.txt", "crf/bogusCRF2.txt");
		//AdjacentFeature.postProcessFile("crf/crf.txt", "crf/crf2.txt");
		System.exit(1);
		
		String[] test = new String[] { "w0 f1","w1 f1","w1 f1","w0 f0","w0 f1" };
		for (String s : AdjacentFeature.getFeature(test,1))
			System.out.println(s);
	}
}

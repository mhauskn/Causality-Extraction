package misc;

import mallet.Include;
import haus.io.DataWriter;
import haus.io.FileReader;
import haus.io.IO;

/**
 * Converts our CRF file to an ARFF file to be processed by WEKA
 * There are several assumptions made to simplify this conversion:
 * 
 * 1. There are the same number of features on each line
 * 2. Each feature except label is of type String
 */
public class CRF2ARFF extends IO<String,String> {
	public static final String reln = "@RELATION ";
	public static final String attr = "@ATTRIBUTE ";
	public static final String data = "@DATA ";
	
	String relnName = "CCP_Identification";
	String[] featNames = new String[] { "Token", "Stem" };
	String[] classes = new String[] { Include.NEITHER_TAG, Include.RELN_BEGIN_TAG,
			Include.RELN_INTERMEDIATE_TAG, Include.RELN_END_TAG, Include.RELN_TAG };
	int num_adj = 0;
	boolean adj_includes_feats = true;
	
	/**
	 * Converts our CRF to an ARFF file
	 */
	public void convert () {
		writeHeader();
		mapInput();
	}
	
	/**
	 * Writes our ARFF header
	 */
	void writeHeader () {
		out.add(reln + relnName + "\n");
		for (String feat : featNames)
			out.add(attr + feat + " string");
		if (num_adj > 0) writeAdjFeats();
		String classLine = attr + "Class {";
		for (int i = 0; i < classes.length; i++) {
			classLine += classes[i];
			if (i != classes.length-1)
				classLine += ",";
		}
		out.add(classLine + "}\n");
		out.add(data);
	}
	
	/**
	 * Writes the attributes for our adjacent features
	 */
	void writeAdjFeats () {
		for (int i = -num_adj; i <= num_adj; i++) {
			if (i == 0) continue;
			out.add(attr + "AdjToken" + i + " string");
			if (adj_includes_feats) {
				for (int j = 1; j < featNames.length; j++)
					out.add(attr + "AdjFeat:" + featNames[j] + i + " string");
			}
		}
	}
	
	/**
	 * Converts a single line of data
	 */
	public void map (String arg0) {
		arg0 = arg0.replaceAll(",", "");
		arg0 = arg0.replaceAll("\\\\", "");
		String[] segs = arg0.split(" ");
		String ret = "";
		for (int i = 0; i < segs.length-1; i++) {
			ret += "\"" + segs[i] + "\",";
		}
		out.add(ret + segs[segs.length-1]);
		//out.add(arg0.replaceAll(" ", ","));
	}
	
	public static void main (String[] args) {
		DataWriter d = new DataWriter("crf/crf_final.arff");
		CRF2ARFF c = new CRF2ARFF();
		c.setInput(new FileReader("crf/crf.txt"));
		c.setOutput(d);
		c.convert();
		d.close();
	}
}

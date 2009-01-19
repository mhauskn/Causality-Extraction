package misc;

import java.util.ArrayList;
import java.util.Collections;

import haus.io.DataWriter;

public class SparseArffWriter {
	public static final String RELN = "@RELATION";
	public static final String ATTR = "@ATTRIBUTE";
	public static final String DATA = "@DATA";
	
	DataWriter writer;
	boolean wrote_data_header;
	
	public SparseArffWriter (String out_file) {
		writer = new DataWriter(out_file);
		wrote_data_header = false;
	}
	
	/**
	 * Writes the relation for the arff file
	 * @relation rel
	 * @param reln_name
	 */
	public void writeTitle (String reln_name) {
		writer.write(RELN + " " + reln_name + "\n");
	}
	
	/**
	 * Adds a nomial attribute of the form 
	 * @ATTRIBUTE atta {A}
	 */
	public void addNomAttribute (String attr_name, ArrayList<String> attrValues) {
		String to_write = ATTR + " " + attr_name + " {";
		for (String s : attrValues)
			to_write += s + ", ";
		to_write = to_write.subSequence(0, to_write.length()-2) + "}";
		writer.write(to_write + "\n");
	}
	
	/**
	 * Adds sparse data. Assumption is that all sparse data will use
	 * the same tag.
	 */
	public void addSparseData (ArrayList<Integer> indexes, String tag) {
		Collections.sort(indexes);
		if (!wrote_data_header) writeDataHeader();
		String to_write = "{";
		for (int i : indexes)
			to_write += i + " " + tag + ", ";
		to_write = to_write.subSequence(0, to_write.length()-2) + "}";
		writer.write(to_write + "\n");
	}
	
	public void close () {
		writer.close();
	}
	
	/**
	 * Writes our data header between the attributes 
	 * and the data
	 */
	void writeDataHeader () {
		wrote_data_header = true;
		writer.write(DATA + "\n");
	}
}

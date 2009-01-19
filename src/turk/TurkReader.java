package turk;

import haus.io.FileReader;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Generically Parse Turk Batch Files for Information.
 * @author epn
 *
 */
public class TurkReader {
	Hashtable<String,Integer> key; // Key into the indexes of each of the responses
	Hashtable<String,HitResult> hits; // Key: clusterKey,  Value: ResponseList
	static String clusterKey = "HITId"; // Clusters Tuples based on matches of this value
	
	class HitResult {
		String clusterKey;
		ArrayList<String[]> responses;
		public HitResult (String id, String[] response) {
			clusterKey = id;
			responses = new ArrayList<String[]> ();
			addResult(response);
		}
		public void addResult (String[] response) {
			responses.add(response);
		}
		public ArrayList<String[]> getResults () {
			return responses;
		}
	}
	
	public TurkReader () {
		key = new Hashtable<String,Integer>();
		hits = new Hashtable<String,HitResult>();
	}
	
	/**
	 * This class assists in the reading of a Turk 
	 * batch file. This is a trickier task than it seems 
	 * because of linebreaks
	 */
	public class TurkAssistedReader {
		FileReader reader = null;
		public String fullLine;
		
		public TurkAssistedReader (String fileName) {
			reader = new FileReader(fileName);
			String header = reader.getNextLine();
			processHeadings(parseCSV(header));
		}
		
		/**
		 * Gets the next logical Turk line..
		 * Not always equated with real lines in the file
		 * @return An array of CSV segments
		 */
		public String[] getNextLine () {
			String tuple = "";
			String line;
			while ((line = reader.getNextLine()) != null) {
				line = removeExtraQuotes(line);
				tuple += line;
				String[] segs = parseCSV(tuple);
				if (segs.length > (key.size() - 2)) {
					System.err.println("Found Seg Too Long: " + segs);
				}
				if (segs.length == (key.size() - 2)) {
					fullLine = tuple;
					return segs;
				}
			}
			return null;
		}
		
		/**
		 * Removes quotes that people put inside of the comment boxes..
		 * Otherwise these quotes throw off the CSV parsing.
		 */
		String removeExtraQuotes (String line) {
			line = line.replaceAll(" \"\"", " ''");
			line = line.replaceAll("\"\" ", "'' ");
			if (line.startsWith("\"\"") && !line.startsWith("\"\","))
				line = line.substring(2, line.length());
			if (line.endsWith("\"\"") && !line.endsWith(",\"\""))
				line = line.substring(0, line.length()-2);
			return line;
		}
	}
	
	/**
	 * Sets a new value for the data to be organized with.
	 * Default is HITId which means the data will be divided 
	 * into one HitResult for each different HITId.
	 */
	public void setOrganizeKey (String newKey) {
		clusterKey = newKey;
	}
	
	/**
	 * Will return all the Strings which are keys to the different
	 * clusters.
	 * @return
	 */
	public String[] getClusterKeys () {
		String[] keys = new String[hits.size()];
		Enumeration<String> e = hits.keys();
		int i = 0;
		while (e.hasMoreElements())
			keys[i++] = e.nextElement();
		return keys;
	}
	
	/**
	 * Given a clusterKey, this method returns all responses for 
	 * that cluster.
	 * @param cluster
	 * @return
	 */
	public ArrayList<String[]> getClusterResponses (String clusterID) {
		if (!hits.containsKey(clusterID))
			return null;
		return hits.get(clusterID).getResults();
	}
	
	/**
	 * Reads a given Turk Batch file and parses the information inside
	 */
	public void parseBatchFile (String batchFile) {
		TurkAssistedReader areader = new TurkAssistedReader(batchFile);
		String[] segs;
		while ((segs = areader.getNextLine()) != null)
			processTuple(segs);
	}
	
	/**
	 * Given a line of data from the batch file - "tuple"
	 * and given a column name ("category"), this method
	 * will retrieve the value of the given category from 
	 * the tuple.
	 * @param category - The column name ie HITID
	 * @param tuple - The row of data
	 * @return
	 */
	public String decode (String category, String[] tuple) {
		if (!key.containsKey(category))
			return null;
		int index = key.get(category);
		if (index < 0 || index >= tuple.length || tuple.length == 0) {
			System.err.println("Bad Index Tuple Pair: Index: " + index + " Tuple: " + tuple);
			return null;
		}
		return tuple[index];
	}
	
	//----------------PRIVATE METHODS-----------------//
	
	/**
	 * Fills our Key Hashtable with indexes for each heading
	 */
	void processHeadings (String[] headings) {
		for (int i = 0; i < headings.length; i++) {
			if (key.containsKey(headings[i])) {
				System.out.println("We have overlap on heading with name " + headings[i]);
			}
			String heading = headings[i];
			key.put(heading, i);
		}
	}
	
	/**
	 * Processes a given row of data
	 * @param tuple
	 */
	void processTuple (String[] tuple) {
		if (tuple.length == 0 || tuple.length != key.size() - 2) {
			System.err.println("Encountered Malformed Tuple: Length " + tuple.length + 
					" Expected Length: " + (key.size() - 2));
			return;
		}
		String major = decode(clusterKey, tuple);
		HitResult res;
		if (hits.containsKey(major)) {
			res = hits.get(major);
			res.addResult(tuple);
		} else {
			res = new HitResult(major, tuple);
		}
		hits.put(major,res);
	}

	/**
	 * Parses terms out of a CSV format.
	 * Returns a String[] containing all of the different items
	 */
	static String[] parseCSV (String tuple) {
		ArrayList<String> strs = new ArrayList<String>();
		boolean inPhrase = false;
		String seg = "";
		for (int i = 0; i < tuple.length(); i++) {
			if (tuple.charAt(i) == '"') {
				if (inPhrase) //We reached phrase end
					strs.add(seg);
				else
					seg = "";
				inPhrase = !inPhrase;
			} else if (inPhrase) {
				seg += tuple.charAt(i);
			}
		}
		String[] out = new String[strs.size()];
		strs.toArray(out);
		return out;
	}
}

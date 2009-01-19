package turk;

public class Include {
	public static final int MAX_SENT_LEN = 45; // Maximum Sentence Length for Turk Task
	public static final String LINE_DELIM = "\r\n"; // Endline delim for CSV Turk file
	public static final int SENT_PER_HIT = 10; // Sentences per hit
	
	/**
	 * Puts the given string array into CSV format
	 * @param segs
	 * @return
	 */
	public static String makeCSV (String[] segs) {
		String batch = "";
		for (int i = 0; i < segs.length; i++) {
			batch += "\"" + segs[i].replaceAll("\"", "''") + "\""; // These throw CSV Off
			if (i != segs.length-1)
				batch += ",";
		}
		return batch;
	}
}

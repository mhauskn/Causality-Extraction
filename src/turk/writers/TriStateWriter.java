package turk.writers;

import java.util.ArrayList;

/**
 * Designed to write tri-state tags. These tags are of the form CB CI CE.
 * This is useful for Converting Human Readable CRFs into CRF Readable 
 * files and vice versa. 
 * 
 */
public class TriStateWriter {
	String non = "";
	String standalone;
	String start;
	String mid;
	String end;
	ArrayList<String> labels;
	boolean active = false;
	boolean gotStart = false;
	boolean gotEnd = false;
	
	public TriStateWriter (String _standalone, String _start, String _mid, String _end) {
		standalone = _standalone;
		start = _start;
		mid = _mid;
		end = _end;
		labels = new ArrayList<String>();
	}
	
	/**
	 * Called when we encounter a stand alone word 
	 */
	public void addStandalone () {
		labels.add(standalone);
	}
	
	/**
	 * Called when an open parenthesis is encountered to tell about the start
	 * of a tag
	 */
	public void addOpening () {
		gotStart = true;
		active = true;
	}
	
	/**
	 * Called when a closing parenthesis is encountered to tell about the
	 * end of a given tag
	 */
	public void addClosing () {
		gotEnd = true;
		active = false;
	}
	
	/**
	 * Called when non-tagged word
	 */
	public void addWord () {
		if (gotStart && gotEnd)
			labels.add(standalone);
		else if (gotStart)
			labels.add(start);
		else if (gotEnd)
			labels.add(end);
		else if (active)
			labels.add(mid);
		else
			labels.add(non);
		
		gotStart = gotEnd = false;
	}
	
	/**
	 * Signifies the end of a given sentence.
	 */
	public String[] addSentEnd () {
		String[] out = new String[labels.size()];
		out = labels.toArray(out);
		labels.clear();
		return out;
	}
	
	/**
	 * Checks if our standalone tag matches one encountered in
	 * the wild.
	 */
	public boolean matchesTag (String tag) {
		return standalone.equals(tag);
	}
}

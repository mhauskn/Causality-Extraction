package turk.writers;

import java.util.ArrayList;

import haus.io.DataWriter;
import haus.io.FileReader;
import haus.io.IO;
import haus.misc.Map;
import mallet.Include;

/**
 * Converts the human readable CRF into the machine readable version
 * @author Administrator
 *
 */
public class HumanToCRF extends IO<String,String> implements Map<String> {
	public static final String OPEN_TAGS = "[(<{";
	public static final String CLOSE_TAGS = "])>}";
	
	ArrayList<TriStateWriter> writers = new ArrayList<TriStateWriter>();
	
	public HumanToCRF () {
		writers.add(new TriStateWriter(Include.RELN_TAG, 
				Include.RELN_BEGIN_TAG, 
				Include.RELN_INTERMEDIATE_TAG,
				Include.RELN_END_TAG));
	}
	
	/**
	 * Processes a given sentence
	 */
	public void map (String str) {
		String[] segs = str.split(" ");
		for (int i = 0; i < segs.length; i++) {
			String s = segs[i];
			if (hasOpeningTag(s)) {
				String reln = getAllMatchingTags(segs, i);
				for (int j = 0; j < reln.length(); j++) {
					String openReln = Character.toString(reln.charAt(j));
					for (TriStateWriter w : writers)
						if (w.matchesTag(openReln))
							w.addOpening();
				}
			}
			if (hasClosingTag(s)) {
				String closingTags = getClosingTags(s);
				for (int j = 0; j < closingTags.length(); j++) {
					String closedRelns = getAssociatedClosingTags(s, closingTags.charAt(j));
					for (int k = 0; k < closedRelns.length(); k++) {
						String closedReln = Character.toString(closedRelns.charAt(k));
						for (TriStateWriter w : writers)
							if (w.matchesTag(closedReln))
								w.addClosing();
					}
				}
			}
			for (TriStateWriter w : writers)
				w.addWord();
			segs[i] = removeTags(s);
		}
		String[] labels = new String[segs.length];
		for (int i = 0; i < labels.length; i++)
			labels[i] = ""; 
		for (TriStateWriter w : writers)
			combineArrays(labels, w.addSentEnd());
		for (int i = 0; i < labels.length; i++)
			if (labels[i].trim().length() == 0)
				labels[i] = Include.NEITHER_TAG;
		combineArrays(segs, labels);
		for (String s : segs)
			out.add(s);
		out.add(Include.SENT_DELIM);
	}
	
	/**
	 * Removes the tags from a given word.
	 */
	static String removeTags (String s) {
		int lastOpening = -1;
		for (int i = 0; i < OPEN_TAGS.length(); i++) {
			int best = s.lastIndexOf(OPEN_TAGS.charAt(i));
			if (best > lastOpening)
				lastOpening = best;
		}
		int firstClosing = s.length();
		for (int i = 0; i < s.length(); i++) {
			if (CLOSE_TAGS.indexOf(s.charAt(i)) != -1) {
				firstClosing = i;
				break;
			}
		}
		return s.substring(lastOpening+1,firstClosing);
	}
	
	/**
	 * Combines two arrays
	 */
	static void combineArrays (String[] master, String[] slave) {
		for (int i = 0; i < master.length; i++) {
			master[i] += " " + slave[i].trim();
		}
	}
	
	/**
	 * Given a string like [(the it will look for all 
	 * possible tag matches
	 */
	static String getAllMatchingTags (String[] segs, int num) {
		String openingTags = getOpeningTags(segs[num]);
		String closingTags = "";
		for (int i = 0; i < openingTags.length(); i++) {
			char a = openingTags.charAt(i);
			String ret = getMatchingTag(segs, num, a, getMatchingParen(a));
			for (int j = 0; j < ret.length(); j++)
				if (closingTags.indexOf(ret.charAt(j)) == -1)
					closingTags += ret.charAt(j);
		}
		return closingTags;
	}
	
	/**
	 * Looks at our word with opening tags and finds all associated 
	 * tags which close our parenthesis.
	 */
	static String getMatchingTag (String[] segs, int num, char openParens, char closeParens) {
		if (!hasOpeningTag(segs[num]))
			return null;
		if (getOpeningTags(segs[num]).indexOf(openParens) == -1)
			return null;
		
		String closingTags = "";
		if (getClosingTags(segs[num]).indexOf(closeParens) != -1)
			closingTags += getAssociatedClosingTags(segs[num], closeParens);
		for (int i = num + 1; i < segs.length; i++) {
			String word = segs[i];
			if (getOpeningTags(word).indexOf(openParens) != -1)
				break;
			if (getClosingTags(word).indexOf(closeParens) != -1)
				closingTags += getAssociatedClosingTags(word, closeParens);
		}
		return closingTags;
	}
	
	/**
	 * Returns the matching type of parenthesis
	 */
	static Character getMatchingParen (char paren) {
		int i = OPEN_TAGS.indexOf(paren);
		if (i != -1)
			return CLOSE_TAGS.charAt(i);
		int j = CLOSE_TAGS.indexOf(paren);
		if (j != -1)
			return OPEN_TAGS.charAt(j);
		return null;
	}
	
	/**
	 * True if the word has an opening tag associated
	 */
	static boolean hasOpeningTag (String s) {
		return OPEN_TAGS.indexOf(s.charAt(0)) != -1 ? true : false;
	}
	
	/**
	 * Returns the opening tags associated with a given word
	 */
	static String getOpeningTags (String s) {
		String tags = "";
		int i = 0;
		while (true) {
			int j = OPEN_TAGS.indexOf(s.charAt(i++));
			if (j == -1) 
				break;
			tags += OPEN_TAGS.charAt(j);
		}
		return tags;
	}
	
	/**
	 * Returns the closing tags associated with a given word
	 */
	static String getClosingTags (String s) {
		String tags = "";
		for (int i = 0; i < s.length(); i++){
			int j = CLOSE_TAGS.indexOf(s.charAt(i));
			if (j != -1)
				tags += CLOSE_TAGS.charAt(j);
		}
		return tags;
	}
	
	/**
	 * Returns the associated tags with a closing tag:
	 * 
	 * the]R then we return R
	 */
	static String getAssociatedClosingTags (String s, char closingTag) {
		String tags = "";
		for (int i = s.indexOf(closingTag)+1; i < s.length(); i++){
			char c = s.charAt(i);
			if (CLOSE_TAGS.indexOf(c) != -1)
				break;
			tags += c;
		}
		return tags;
	}
	
	/**
	 * True if the word has a closing tag associated
	 */
	static boolean hasClosingTag (String s) {
		for (int i = 0; i < CLOSE_TAGS.length(); i++)
			if (s.indexOf(CLOSE_TAGS.charAt(i)) != -1)
				return true;
		return false;
	}
	
	

	public static void main (String[] args) {
		String in_file = "turk/ReadableCRF.txt";
		String out_file = "turk/crfOut.txt";
		DataWriter writer = new DataWriter(out_file);
		
		HumanToCRF translator = new HumanToCRF();
		translator.setInput(new FileReader(in_file));
		translator.setOutput(writer);
		
		translator.mapInput();
		writer.close();
	}
}

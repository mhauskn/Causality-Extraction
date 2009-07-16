package turk.writers;

import java.util.ArrayList;

import haus.io.DataWriter;
import haus.io.FileReader;
import haus.io.IO;
import mallet.Include;

/**
 * Converts the human readable CRF into the machine readable version
 * @author Administrator
 *
 */
public class HumanToCRF extends IO<String,String> {
	public static final String OPEN_TAGS = "[(<{";
	public static final String CLOSE_TAGS = "])>}";
	
	public static final String[] OPEN = new String[] { "[", "(", "<", "{" };
	public static final String[] CLOSE = new String[] { "]", ")", ">", "}" };
	
	String reln = turk.Include.relnFeat;
	
	ArrayList<TriStateWriter> writers = new ArrayList<TriStateWriter>();
	
	public HumanToCRF () {
		TriStateWriter t = new TriStateWriter(Include.RELN_TAG,
				reln + Include.RELN_TAG, 
				reln + Include.RELN_BEGIN_TAG, 
				reln + Include.RELN_INTERMEDIATE_TAG,
				reln + Include.RELN_END_TAG);
		t.outputAsFeature(reln + "N");
		writers.add(t);
		writers.add(new TriStateWriter(Include.CAUSE_TAG,
				Include.CAUSE_TAG,
				Include.CAUSE_BEGIN_TAG, 
				Include.CAUSE_INTERMEDIATE_TAG,
				Include.CAUSE_END_TAG));
		writers.add(new TriStateWriter(Include.EFFECT_TAG, 
				Include.EFFECT_TAG,
				Include.EFFECT_BEGIN_TAG, 
				Include.EFFECT_INTERMEDIATE_TAG,
				Include.EFFECT_END_TAG));
	}
	
	/**
	 * Processes a given sentence:
	 * 1. Look for lowest )R tag
	 * 2. Create list of valid Phrasal tags [(
	 * 3. Get annotation for these valid tags
	 * 4. Remove annotation from sentence
	 * 5. Repeat until no more )R remains
	 */
	public void mapInput (String str) {
		String[] segs = str.split(" ");
		String[] clean = str.split(" ");
		for (int i = 0; i < clean.length; i++)
			clean[i] = removeTags(clean[i],OPEN_TAGS,CLOSE_TAGS);
		
		int tag_level = getClosingRelnTagNum(segs);
		while (tag_level != -1) {
			String valid_open_tags = OPEN_TAGS.substring(0, tag_level+1);
			String valid_close_tags = CLOSE_TAGS.substring(0, tag_level+1);
			extractTags(segs,valid_open_tags,valid_close_tags);
			writeSentenceOuput(clean);
			tag_level = getClosingRelnTagNum(segs);
		}
	}
	
	/**
	 * Writes the parse output
	 */
	void writeSentenceOuput (String[] segs) {
		String[] feats = new String[segs.length];
		String[] labels = new String[segs.length];
		for (int i = 0; i < labels.length; i++) {
			labels[i] = ""; 
			feats[i] = "";
		}
		for (TriStateWriter w : writers) {
			if (w.asFeature)
				combineArrays(feats, w.getResult());
			else
				combineArrays(labels, w.getResult());
		}
			
		for (int i = 0; i < labels.length; i++)
			if (labels[i].trim().length() == 0)
				labels[i] = " " + Include.NEITHER_TAG;
		
		for (int i = 0; i < segs.length; i++)
			out.add(segs[i] + feats[i] + labels[i]);
		
		String delim = Include.SENT_DELIM_REDUX + " ";
		for (TriStateWriter w : writers)
			if (w.asFeature)
				delim += w.non + " ";
		out.add(delim + Include.NEITHER_TAG);
	}
	
	/**
	 * Extracts valid tags from a sentence
	 * @param segs The sentence split into individual words
	 * @param valid_open_tags the list of valid opening tags
	 * @param valid_close_tags the list of valid closing tags
	 */
	void extractTags (String[] segs, String valid_open_tags, String valid_close_tags) {
		for (int i = 0; i < segs.length; i++) {
			String s = segs[i];
			if (hasTag(s,valid_open_tags)) {
				String reln = getMatchingTags(segs, i, valid_open_tags, valid_close_tags);
				for (int j = 0; j < reln.length(); j++) {
					String openReln = Character.toString(reln.charAt(j));
					for (TriStateWriter w : writers)
						if (w.matchesTag(openReln))
							w.addOpening();
				}
			}
			
			if (hasTag(s,valid_close_tags)) {
				String closingTags = getClosingTags(s);
				for (int j = 0; j < closingTags.length(); j++) {
					char close_tag = closingTags.charAt(j);
					if (valid_close_tags.indexOf(close_tag) == -1) continue;
					String closedRelns = getAssociatedRelation(s, close_tag);
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
			segs[i] = removeTags(s,valid_open_tags,valid_close_tags);
		}
	}
	
	/**
	 * Returns the number associated with the earliest
	 * closing relation in the sentence
	 */
	int getClosingRelnTagNum (String[] segs) {
		for (int i = 0; i < CLOSE_TAGS.length(); i++) {
			for (String word: segs) {
				String s = CLOSE[i] + Include.RELN_TAG;
				if (word.indexOf(s) >= 0)
					return i;
			}
		}
		return -1;
	}
	
	/**
	 * Strips a word of open/closing tags as well as the associated relations.
	 * @param word
	 * @param valid_open_tags
	 * @param valid_close_tags
	 * @return
	 */
	public static String removeTags (String word, String valid_open_tags, String valid_close_tags) {
		String out = "";
		boolean closing = false;
		for (int i = 0; i < word.length(); i++) {
			char c = word.charAt(i);
			if (valid_open_tags.indexOf(c) != -1) continue;
			if (CLOSE_TAGS.indexOf(c) != -1) {
				if (valid_close_tags.indexOf(c) != -1) { closing = true; continue; }
				else closing = false;
			}
			if (closing) continue;
			out += c;
		}
		return out;
	}

	/**
	 * Combines two arrays
	 */
	static void combineArrays (String[] master, String[] slave) {
		for (int i = 0; i < master.length; i++) {
			String out = master[i].trim();
			if (slave[i].trim().length() == 0) continue;
			master[i] = out + " " + slave[i].trim();
		}
	}
	
	/**
	 * Gets all valid matching close tags 
	 * @param segs The sentence divided into segments
	 * @param num the word number
	 * @param valid_open_tags the string of all valid opening tags
	 * @param valid_close_tags the string of all valid closing tags
	 * @return all the associated tags with the valid opening/closing tags
	 */
	String getMatchingTags (String[] segs, int num, String valid_open_tags, String valid_close_tags) {
		String matched = "";
		for (int i = 0; i < valid_open_tags.length(); i++) {
			char open_tag = valid_open_tags.charAt(i);
			char close_tag = valid_close_tags.charAt(i);
			matched += getMatchingTag(segs, num, open_tag, close_tag);
		}
		return matched;
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
			closingTags += getAssociatedRelation(segs[num], closeParens);
		for (int i = num + 1; i < segs.length; i++) {
			String word = segs[i];
			if (getOpeningTags(word).indexOf(openParens) != -1)
				break;
			if (getClosingTags(word).indexOf(closeParens) != -1)
				closingTags += getAssociatedRelation(word, closeParens);
		}
		return closingTags;
	}
	
	/**
	 * True if the word has an opening tag associated
	 */
	static boolean hasOpeningTag (String s) {
		return OPEN_TAGS.indexOf(s.charAt(0)) != -1 ? true : false;
	}
	
	/**
	 * Checks each char in our string for a match with the
	 * given tagset.
	 */
	static boolean hasTag (String s, String tagset) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (tagset.indexOf(c) != -1)
				return true;
		}
		return false;
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
	static String getAssociatedRelation (String s, char closingTag) {
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
		//String in_file = "turk/readableCRF.txt";
		String in_file = "propbank/100.txt";
		String out_file = "crf/100_bare.txt";
		DataWriter writer = new DataWriter(out_file);
		
		HumanToCRF translator = new HumanToCRF();
		translator.setInput(new FileReader(in_file));
		translator.setOutput(writer);
		
		translator.mapInput();
		writer.close();
	}
}

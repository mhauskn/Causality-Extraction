package turk;

import java.util.ArrayList;

import haus.io.DataWriter;

/**
 * Converts a human readable Turk File into a CRF reader file.
 * 
 * The human file has format:
 * The [storm]E caused the [flood.]E
 * 
 * To the CRF format:
 * The N
 * storm C
 * caused N
 * the N
 * flood. E
 *
 */
public class TurkHumanToCRF {
	public static final char OPEN_CHAR = '[';
	public static final char CLOSE_CHAR = ']';
	public static final char CAUSE_SHORTHAND = 'C';
	public static final char EFFECT_SHORTHAND = 'E';
	
	DataWriter writer = new DataWriter("turk/genCRF.txt");
	
	public void convert (String file_name) {
		for (String line : haus.io.FileReader.readFile(file_name))
			processLine(line);
	}
	
	public void processLine (String line) {
		boolean inCause = false;
		boolean inEffect = false;
		String[] segs = line.split(" ");
		ArrayList<String> reverse = new ArrayList<String>();
		for (int i = segs.length-1; i >= 0; i--) {
			String seg = segs[i];
			if (seg.charAt(0) == OPEN_CHAR && seg.charAt(seg.length()-2) == CLOSE_CHAR) {
				reverse.add(seg.substring(1, seg.length()-2) + " " + 
						getCRFLabel(seg.charAt(seg.length()-1),true,false,false));
			} else if (seg.charAt(0) == OPEN_CHAR) {
				String l = "";
				l += seg.substring(1) + " ";
				if (inCause)
					l += getCRFLabel(CAUSE_SHORTHAND,false,false,true);
				else if (inEffect)
					l += getCRFLabel(EFFECT_SHORTHAND,false,false,true);
				inCause = false; inEffect = false;
				reverse.add(l);
			} else if (seg.length() > 2 && seg.charAt(seg.length()-2) == CLOSE_CHAR) {
				if (seg.charAt(seg.length()-1) == CAUSE_SHORTHAND)
					inCause = true;
				else if (seg.charAt(seg.length()-1) == EFFECT_SHORTHAND)
					inEffect = true;
				reverse.add(seg.substring(0, seg.length()-2) + " " + 
						getCRFLabel(seg.charAt(seg.length()-1),false,true,false));
			} else if (inCause) {
				reverse.add(seg + " " + getCRFLabel(CAUSE_SHORTHAND,false,false,false));
			} else if (inEffect) {
				reverse.add(seg + " " + getCRFLabel(EFFECT_SHORTHAND, false,false,false));
			} else {
				reverse.add(seg + " " + getCRFLabel('N',false,false,false));
			}
		}
		for (int i = reverse.size() -1; i >= 0; i--)
			writer.writeln(reverse.get(i));
		writer.writeln(mallet.Include.SENT_DELIM);
	}
	
	/**
	 * Translates the shorthand label [storm]C into our CRF label
	 * such as CI.
	 */
	public String getCRFLabel (char shorthand_label, boolean singular, boolean end, boolean begin) {
		if (shorthand_label == CAUSE_SHORTHAND) {
			if (singular)
				return mallet.Include.CAUSE_TAG;
			if (begin)
				return mallet.Include.CAUSE_BEGIN_TAG;
			if (end)
				return mallet.Include.CAUSE_END_TAG;
			return mallet.Include.CAUSE_INTERMEDIATE_TAG;
		}
		
		if (shorthand_label == EFFECT_SHORTHAND) {
			if (singular)
				return mallet.Include.EFFECT_TAG;
			if (begin)
				return mallet.Include.EFFECT_BEGIN_TAG;
			if (end)
				return mallet.Include.EFFECT_END_TAG;
			return mallet.Include.EFFECT_INTERMEDIATE_TAG;
		}
		
		return mallet.Include.NEITHER_TAG;
	}
	
	public static void main (String[] args) {
		TurkHumanToCRF t = new TurkHumanToCRF();
		t.convert("turk/readableCRF.txt");
		t.writer.close();
	}
}

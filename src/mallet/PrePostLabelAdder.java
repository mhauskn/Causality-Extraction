package mallet;

import java.util.ArrayList;

/**
 * Adds pre-cause/effect labels (BC, BE), post-cause/effect (AC/AE)
 * and mid (M) tags to our crf file.
 * @author Administrator
 *
 */
public class PrePostLabelAdder {
	public static final String BEFORE_CAUSE = "BC";
	public static final String BEFORE_EFFECT = "BE";
	public static final String AFTER_CAUSE = "AC";
	public static final String AFTER_EFFECT = "AE";
	public static final String MIDDLE = "M";
	
	
	
	public static final String in_file = "crf/crf_bare.txt";
	public static final String out_file = "crf/crf_PrePostLabel.txt";
	public static final int span = 3;
	
	//These have bad scoping but this class is only meant to be used once
	ArrayList<String> toks = new ArrayList<String>();
	ArrayList<String> labels = new ArrayList<String>();
	ArrayList<String> features = new ArrayList<String>();
	
	ArrayList<String> new_labels = new ArrayList<String>();
	
	ArrayList<String> sent;

	void reTag () {
		CRFFormatter.readCRFFile(in_file, toks, features, labels);
		ArrayList<String> c = new ArrayList<String>();
		for (int i = 0 ; i < toks.size(); i++) {
			String tok = toks.get(i);
			String lab = labels.get(i);
			if (tok.equals(Include.SENT_DELIM_REDUX)) {
				reTagSentence(c);
				new_labels.add(Include.NEITHER_TAG);
				c.clear();
			} else 
				c.add(lab);
		}
		CRFFormatter.writeCRFFile(out_file, toks, features, new_labels);
	}
	
	void reTagSentence (ArrayList<String> sent_labels) {
		sent = sent_labels;
		boolean haveCause = false;
		boolean haveEffect = false;
		
		for (int i = 0; i < sent_labels.size(); i++) {
			String label = sent_labels.get(i);
			if (label.matches(Include.CAUSE_REGEXP)) {
				haveCause = true;
				new_labels.add(label);
				continue;
			}
			
			if (label.matches(Include.EFFECT_REGEXP)) {
				haveEffect = true;
				new_labels.add(label);
				continue;
			}
			
			int selected = 0;
			
			if (effectAhead(i) && !haveCause && !haveEffect) {
				selected++;
				new_labels.add(BEFORE_EFFECT);
			}
			
			if (causeAhead(i) && !haveEffect && !haveCause) {
				selected++;
				new_labels.add(BEFORE_CAUSE);
			}
			
			if (causeBehind(i) && !tagsAhead(i)) {
				selected++;
				new_labels.add(AFTER_CAUSE);
			}
			
			if (effectBehind(i) && !tagsAhead(i)) {
				selected++;
				new_labels.add(AFTER_EFFECT);
			}
			
			if ((haveCause || haveEffect) && tagsAhead(i)) {
				selected++;
				new_labels.add(MIDDLE);
			}
			
			if (selected == 0)
				new_labels.add(label);
			
			if (selected > 1) { 
				System.out.println("Accepted too many statements! We are not exclusive!");
				System.exit(1);
			}
		}
	}
	
	boolean effectAhead (int index) {
		for (int i = index;  i < index + span; i++) {
			if (i < 0 || i >= sent.size())
				continue;
			
			if (sent.get(i).matches(Include.CAUSE_REGEXP))
				return false;
			
			if (sent.get(i).matches(Include.EFFECT_REGEXP))
				return true;
		}
		return false;
	}
	
	boolean effectBehind (int index) {
		for (int i = index;  i > index - span; i--) {
			if (i < 0 || i >= sent.size())
				continue;
			
			if (sent.get(i).matches(Include.CAUSE_REGEXP))
				return false;
			
			if (sent.get(i).matches(Include.EFFECT_REGEXP))
				return true;
		}
		return false;
	}
	
	boolean causeAhead (int index) {
		for (int i = index;  i < index + span; i++) {
			if (i < 0 || i >= sent.size())
				continue;
			
			if (sent.get(i).matches(Include.EFFECT_REGEXP))
				return false;
			
			if (sent.get(i).matches(Include.CAUSE_REGEXP))
				return true;
		}
		return false;
	}
	
	boolean causeBehind (int index) {
		for (int i = index;  i > index - span; i--) {
			if (i < 0 || i >= sent.size())
				continue;
			
			if (sent.get(i).matches(Include.EFFECT_REGEXP))
				return false;
			
			if (sent.get(i).matches(Include.CAUSE_REGEXP))
				return true;
		}
		return false;
	}
	
	/**
	 * Checks if there are any more Cause or Effect tags ahead of the current 
	 * position in our sentence.
	 * @return
	 */
	boolean tagsAhead (int index) {
		for (int i = index; i < sent.size(); i++) {
			String tag = sent.get(i);
			if (tag.matches(Include.CAUSE_REGEXP) || tag.matches(Include.EFFECT_REGEXP))
				return true;
		}
		return false;
	}
	
	public static void main (String[] args) {
		PrePostLabelAdder a = new PrePostLabelAdder();
		a.reTag();
	}
}

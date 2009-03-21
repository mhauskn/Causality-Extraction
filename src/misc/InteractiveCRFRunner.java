package misc;

import java.util.ArrayList;

import stemmer.BasicStemmer;
import turk.turkCrfToHuman;

import analysis.features.NpVpAggFeature;
import analysis.features.StemFeature;

import haus.io.DataWriter;
import haus.io.FileReader;
import io.InteractiveReader;

public class InteractiveCRFRunner {
	public static final String RELN_TAG = "R";
	
	public static final String mallet_dir = mallet.Include.MALLET_DIR;
	public static final String data_out = mallet_dir + "/interactiveTrain";
	public static final String model_out = mallet_dir + "/interactiveModel";
	public static final String junk_out = mallet_dir + "/interactiveJunk";
	public static final String test_out = mallet_dir + "/interactiveTest";
	public static final String ans_out = mallet_dir + "/interactiveAnswer";
	
	public static final String pos_ans = "yes";
	public static final String neg_ans = "no";
	public static final String done_ans = "done";
	
	boolean arg0cause = false;
	
	public static final String ARG0_TAG = "[ARG0";
	public static final String ARG1_TAG = "[ARG1";
	public static final String REL_TAG = "[rel";
	
	InteractiveReader iReader = new InteractiveReader();
	deAnnotator deann = new deAnnotator();
	NpVpAggFeature agg = new NpVpAggFeature();
	BasicStemmer stem = new BasicStemmer();
	StemFeature stemFeat = new StemFeature();
	
	String stemmedRel = "";
	
	ArrayList<String[]> sentences = new ArrayList<String[]>();
	
	/**
	 * Takes propbank annotated sentence and extracts the cause/effect/normal sentence
	 * HORRIBLE CODE!
	 */
	class deAnnotator {
		String arg0 = "";
		String arg1 = "";
		String rel = "";
		ArrayList<String> tokens;
		ArrayList<String> labels;
		ArrayList<Integer> relIndex;
		String sentence = "";
		
		boolean inarg0;
		boolean inarg1;
		boolean inRel;
		boolean fresharg0;
		boolean fresharg1;
		boolean arg0end;
		boolean arg1end;
		
		void init () {
			inarg0 = false; inarg1 = false; inRel = false;
			fresharg0 = false; fresharg1 = false;
			tokens = new ArrayList<String>();
			labels = new ArrayList<String>();
			relIndex = new ArrayList<Integer>();
			sentence = "";
		}
		
		public void deAnnotate (String sent) {
			init();
			String[] words = sent.split(" ");
			for (int i = 0; i < words.length; i++) {
				String word = words[i];
				if (word.equals(ARG0_TAG)) {
					inarg0 = true;
					fresharg0 = true;
				} else if (word.equals(ARG1_TAG)) {
					inarg1 = true;
					fresharg1 = true;
				} else if (word.equals(REL_TAG)) {
					inRel = true;
					deFreshify();
				} else if (word.charAt(word.length()-1) == ']') {
					gotEnd(word);
					deFreshify();
				} else if (word.charAt(0) == '[') {
					continue;
				} else {
					tokens.add(word);
					labels.add(getLab());
					deFreshify();
				}
			}
		}
		
		void deFreshify () {
			fresharg0 = false; fresharg1 = false;
		}
		
		/**
		 * 3Bits of entropy... UGH!
		 */
		String getLab () {
			/*if (inarg0) {
				if (fresharg0) {
					if (arg0cause)
						return mallet.Include.CAUSE_BEGIN_TAG;
					else
						return mallet.Include.EFFECT_BEGIN_TAG;
				} else {
					if (arg0cause)
						return mallet.Include.CAUSE_INTERMEDIATE_TAG;
					else
						return mallet.Include.EFFECT_INTERMEDIATE_TAG;
				}
			} else if (inarg1) {
				if (fresharg1) {
					if (arg0cause)
						return mallet.Include.EFFECT_BEGIN_TAG;
					else
						return mallet.Include.CAUSE_BEGIN_TAG;
				} else {
					if (arg0cause)
						return mallet.Include.EFFECT_INTERMEDIATE_TAG;
					else
						return mallet.Include.CAUSE_INTERMEDIATE_TAG;
				}
			} else*/ if (inRel) {
				return RELN_TAG;
			} else 
				return mallet.Include.NEITHER_TAG;
		}
		
		void gotEnd (String word) {
			word = word.substring(0, word.length()-1);
			tokens.add(word);
			/*if (inarg0) {
				if (arg0cause) {
					if (fresharg0)
						labels.add(mallet.Include.CAUSE_TAG);
					else
						labels.add(mallet.Include.CAUSE_END_TAG);
				} else {
					if (fresharg1)
						labels.add(mallet.Include.EFFECT_TAG);
					else
						labels.add(mallet.Include.EFFECT_END_TAG);
				}
					
			} else if (inarg1) {
				if (arg0cause)
					labels.add(mallet.Include.EFFECT_END_TAG);
				else
					labels.add(mallet.Include.CAUSE_END_TAG);
			} else*/ if (inRel) {
				relIndex.add(tokens.size()-1);
				stemmedRel += stem.stem(word);
				labels.add(RELN_TAG);
				//labels.add(mallet.Include.NEITHER_TAG);
			} else {
				labels.add(mallet.Include.NEITHER_TAG);
			}
			inarg0 = false; inarg1 = false; inRel = false;
		}
		
		void print () {
			for (int i = 0; i < tokens.size(); i++) {
				System.out.println(tokens.get(i) + " " + labels.get(i));
			}
			for (int i = 0; i < relIndex.size(); i++) {
				System.out.println("Rel:" + relIndex.get(i));
			}
		}
	}
	
	public void askQuestions () {
		System.out.println("Assume rel0/rel1 is cause/effect. Is rel0 cause? (yes/no):");
		String resp;
		resp = iReader.getInput();
		if (resp.equals(pos_ans)) {
			arg0cause = true;
			System.out.println("arg0 cause confirmed.");
		} else if (resp.equals(neg_ans)) {
			System.out.println("arg0 effect confirmed.");
		} else {
			System.out.println("Bad response.");
			askQuestions();
		}
	}
	
	void getData () {
		System.out.println("Give me some data please! Type '" + done_ans + "' when finished.");
		String data;
		while (!(data = iReader.getInput()).equals(done_ans)) {
			processSent(data);
		}
	}
	
	void processSent (String data) {
		deann.deAnnotate(data);
		//deann.print();
		
		String[] tokensArr = new String[deann.tokens.size()];
		tokensArr = deann.tokens.toArray(tokensArr);
		agg.getFeature(tokensArr);
		String[] feature = agg.normalizeFeatureTokens(deann.relIndex.isEmpty() ? 0 : deann.relIndex.get(0));
		String[] stems = stemFeat.getFeature(tokensArr);
		
		String[] out = new String[tokensArr.length];
		for (int i = 0; i < tokensArr.length; i++) {
			out[i] = tokensArr[i] + " " + feature[i] + " " + stems[i] + deann.labels.get(i);
		}
		sentences.add(out);
	}
	
	void writeTrainingFile () {
		DataWriter writer = new DataWriter(data_out);
		for (int i = 0; i < sentences.size(); i++) {
			String[] sent = sentences.get(i);
			for (int j = 0; j < sent.length; j++) {
				writer.write(sent[j] + "\n");
			}
			writer.write(mallet.Include.SENT_DELIM + "\n");
		}
		writer.close();
	}
	
	void interactiveEvaluation () {
		System.out.println("Ready to tag some sentences! Type '" + done_ans + "' when finished.");
		String data;
		
		while (!(data = iReader.getInput()).equals(done_ans)) {
			String[] tokarr = data.split(" ");
			int relIndex = 0;
			for (int i = 0; i < tokarr.length; i++) {
				String stemmed = stem.stem(tokarr[i]);
				if (stemmed == null) continue;
				if (stemmedRel.indexOf(stemmed) != -1)
					relIndex = i;
			}
			agg.getFeature(tokarr);
			String[] feature = agg.normalizeFeatureTokens(relIndex);
			String[] stems = stemFeat.getFeature(tokarr);
			DataWriter writer = new DataWriter(test_out);
			for (int i = 0; i < tokarr.length; i++)
				writer.write(tokarr[i] + " " + feature[i] + " " + stems[i] + "\n");
			writer.write(mallet.Include.SENT_DELIM_REDUX + "\n");
			writer.close();
			mallet.CRFRunner.evaluateCRF(ans_out, model_out, test_out);
			turkCrfToHuman tcrf = new turkCrfToHuman();
			tcrf.setFormat(turkCrfToHuman.CRF_OUTPUT_FORMAT);
			tcrf.setTokenInput(tokarr);
			tcrf.setInfile(ans_out);
			tcrf.setOutfile(junk_out);
			tcrf.translate();
			FileReader reader = new FileReader(junk_out);
			String line;
			while ((line = reader.getNextLine()) != null) {
				System.out.println(line);
			}
		}
	}
		
	void run () {
		askQuestions();
		getData();
		writeTrainingFile();
		mallet.CRFRunner.trainCRF(junk_out, model_out, data_out);
		sentences.clear();
		interactiveEvaluation();
	}
	
	public static void main (String[] args) {
		InteractiveCRFRunner icrf = new InteractiveCRFRunner();
		icrf.run();
	}
}

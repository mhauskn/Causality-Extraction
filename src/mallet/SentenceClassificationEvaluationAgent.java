package mallet;

import java.util.ArrayList;


import evaluation.EvaluationAgent;

/**
 * Evaluates a sentence to see if it is correctly classified as causal
 * or non-causal. This is based on whether a cue phrase is found 
 * inside of the sentence.
 * Base unit is a String[]
 * String[0] = CRF's answer sentence
 * String[1] = Accepted answer Sentence
 * @author epn
 *
 */
class SentenceClassificationEvaluationAgent extends EvaluationAgent<String[],String[]> {
	ArrayList<String> answers = null;
	ArrayList<String> given = null;

	@Override
	public boolean exhausted() {
		if (answers.size() == 0 && given.size() == 0)
			return true;
		return false;
	}

	@Override
	public String[] getNextUnit() {
		String[] out = new String[2];
		out[0] = given.remove(0);
		out[1] = answers.remove(0);
		return out;
	}

	@Override
	public boolean isRelevant(String[] unit) {
		String accepted_answer = unit[1];
		return checkSent(accepted_answer, true);
	}

	@Override
	public boolean isRetrieved(String[] unit) {
		String given_answer = unit[0];
		return checkSent(given_answer, false);
	}
	
	boolean checkSent (String toCheck, boolean isAnswer) {
		return CRFRunner.sentenceContains(toCheck, isAnswer, Include.RELN_TAG);
		//return (CRFRunner.sentenceContains(toCheck, isAnswer, Include.CAUSE_TAG)
		//		|| CRFRunner.sentenceContains(toCheck, isAnswer, Include.EFFECT_TAG));
	}
	
	/**
	 * Reads our data from a strings containing the data files
	 * Answer file is first, Output file is second.
	 */
	public void processData  (String[] data) {
		answers = Include.readSentDelimFile(data[0]);
		given = Include.readSentDelimFile(data[1]);
		if (answers.size() != given.size()) {
			System.err.println("Unequal Answer Sizes! in given data and answer files");
			System.exit(1);
		}
	}
}

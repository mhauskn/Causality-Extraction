package mallet;

import java.util.ArrayList;

import evaluation.EvaluationAgent;

/**
 * Evaluates a sentence to see if it is correctly classified as causal
 * or non-causal.
 * Base unit is a String[]
 * String[0] = CRF's answer sentence
 * String[1] = Accepted answer Sentence
 * @author epn
 *
 */
class SentenceClassificationEvaluationAgent extends EvaluationAgent<String[]> {
	ArrayList<String> answers = null;
	ArrayList<String> given = null;
	
	public void getData (String ans_file, String output_file) throws Exception {
		answers = CRFRunner.readDataFromFile(ans_file);
		given = CRFRunner.readDataFromFile(output_file);
		if (answers.size() != given.size()) {
			System.err.println("Unequal Answer Sizes!");
			throw new Exception();
		}
	}

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
		if (CRFRunner.hasCauseQuestion(accepted_answer, true) || 
				CRFRunner.hasEffectQuestion(accepted_answer, true))
			return true;	
		return false;
	}

	@Override
	public boolean isRetrieved(String[] unit) {
		String given_answer = unit[0];
		if (CRFRunner.hasCauseQuestion(given_answer, false) || 
				CRFRunner.hasEffectQuestion(given_answer, false))
			return true;
		return false;
	}
}

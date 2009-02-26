package mallet;

public class CuePhraseTokenClassificationEvaluationAgent extends TokenClassificationEvaluationAgent {
	@Override
	public boolean isRelevant(String[] unit) {
		String accepted_answer = unit[1];
		if (CRFRunner.isCuePhrase(CRFRunner.getAnswer(accepted_answer)))
			return true;	
		return false;
	}

	@Override
	public boolean isRetrieved(String[] unit) {
		String given_answer = unit[0];
		if (CRFRunner.isCuePhrase(given_answer))
			return true;
		return false;
	}
}

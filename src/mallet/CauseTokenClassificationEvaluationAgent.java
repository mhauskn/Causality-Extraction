package mallet;

/**
 * Preforms token matching for Cause Tokens
 * @author epn
 *
 */
public class CauseTokenClassificationEvaluationAgent extends TokenClassificationEvaluationAgent {
	@Override
	public boolean isRelevant(String[] unit) {
		String accepted_answer = unit[1];
		if (CRFRunner.isCause(CRFRunner.getAnswer(accepted_answer)))
			return true;	
		return false;
	}

	@Override
	public boolean isRetrieved(String[] unit) {
		String given_answer = unit[0];
		if (CRFRunner.isCause(given_answer))
			return true;
		return false;
	}
}

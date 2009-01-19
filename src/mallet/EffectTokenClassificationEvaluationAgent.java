package mallet;

/**
 * Preforms token matching for Effect Tokens
 * @author epn
 *
 */
public class EffectTokenClassificationEvaluationAgent extends TokenClassificationEvaluationAgent {
	@Override
	public boolean isRelevant(String[] unit) {
		String accepted_answer = unit[1];
		if (CRFRunner.isEffect(CRFRunner.getAnswer(accepted_answer)))
			return true;	
		return false;
	}

	@Override
	public boolean isRetrieved(String[] unit) {
		String given_answer = unit[0];
		if (CRFRunner.isEffect(given_answer))
			return true;
		return false;
	}
}

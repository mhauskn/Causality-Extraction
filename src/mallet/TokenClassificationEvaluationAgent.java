package mallet;

/**
 * The TokenClassificationEvaluationAgent gets its data from test
 * and answer files and evaluates the system's score on a token by
 * token basis
 * String[0] = given answer
 * String[1] = accepted answer
 * @author epn
 *
 */
public abstract class TokenClassificationEvaluationAgent extends SentenceClassificationEvaluationAgent {
	String[] answer_tokens = new String[0];
	String[] given_tokens = new String[0];
	int index = 0;
	
	@Override
	public String[] getNextUnit() {
		if (index == answer_tokens.length) {
			String nextAnswer = answers.remove(0);
			String nextGivenAnswer = given.remove(0);
			answer_tokens = nextAnswer.split("\n");
			given_tokens = nextGivenAnswer.split("\n");
			index = 0;
		}
		String[] out = new String[2];
		out[0] = given_tokens[index];
		out[1] = answer_tokens[index];
		index++;
		return out;
	}
}

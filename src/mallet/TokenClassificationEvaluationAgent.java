package mallet;

import haus.io.FileReader;


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
	
	/**
	 * Reads our data from a strings containing the data files
	 * Answer file is first, Output file is second.
	 */
	public void processData  (String[] data) {
		answers = FileReader.readFile(data[0]);
		given = FileReader.readFile(data[1]);
		if (answers.size() != given.size()) {
			System.err.println("Unequal Answer Sizes! in given data and answer files");
			System.exit(1);
		}
	}
}

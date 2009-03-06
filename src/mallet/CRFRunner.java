package mallet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import haus.io.DataWriter;

import evaluation.CrossValidator;
import evaluation.CrossEvaluable;
import evaluation.EvalTracker;
import evaluation.EvaluationAgent;

/**
 * Runs our CRF implementation in a sequential manner using 
 * cross validation.
 *
 */
public class CRFRunner extends CrossEvaluable<String> {
	public static final int NUM_VALIDATIONS = 10;
	
	String data_file = "crf/bogusCRF2.txt";
	String out_file = "mallet_out/";
	
	ArrayList<String> data = null;
	ArrayList<Long> train_times = null;
	
	CrossValidator<String> validator = null;
	
	String[] agentDataSource;
	
	int numCorrectTokens = 0;
	int numTokens = 0;
	
	int iteration = 0;
	
	public CRFRunner () {
		data = Include.readSentDelimFile(data_file);
		train_times = new ArrayList<Long>();
		Collections.shuffle(data);
		validator = new CrossValidator<String>(NUM_VALIDATIONS, data, this);
		
		addTracker(new EvalTracker(new SentenceClassificationEvaluationAgent(), "Causal Sentence Tracker",this));
		//addTracker(new EvalTracker(new CauseTokenClassificationEvaluationAgent(), "Cause Token Tracker",this));
		//addTracker(new EvalTracker(new EffectTokenClassificationEvaluationAgent(), "Effect Token Tracker",this));
		addTracker(new EvalTracker(new CuePhraseTokenClassificationEvaluationAgent(), "Cue Phrase Token Tracker",this));
	}
	
	public static void main (String[] args) {
		//CRFRunner.help();
		//System.exit(1);
		CRFRunner c = new CRFRunner();
		
		System.out.println("Data size: " + c.data.size());
		
		c.run();
		
		System.out.println("CRF Training Times:");
		for (Long l : c.train_times)
			System.out.print(l + ", ");
		System.out.println("\n");
		
		System.out.println(c.getEvaluationResults());
	}
	
	static Double getAvg (ArrayList<Double> list) {
		double sum = 0.0;
		for (Double d: list)
			sum += d;
		return sum/list.size();
	}
	
	/**
	 * Starts the Evaluation
	 */
	public void run () {
		validator.crossValidate();
	}
	
	/**
	 * Handles the data given by the Cross Validator.
	 * In general we will want to train CRF, evaluate CRF, and prepare
	 * for our evaluation
	 */
	public void handleData (List<String> trainData, List<String> testData) {
		String answer_file = out_file + "Answers_" + iteration;
		String train_file = out_file + "Train_" + iteration;
		String test_file = out_file + "Test_" + iteration;
		String model_file = out_file + "causeModel_" + iteration;
		String output_file = out_file + "Output_" + iteration;

		DataWriter.writeFile(answer_file, testData); // Create Norm Answer file
		DataWriter.writeFile(train_file, trainData); // Create Norm Train file
		DataWriter.writeFile(test_file, createTestQuestions(testData)); // Create Normal Test File
		
		long time_start = System.currentTimeMillis();
		trainCRF(output_file+"Bad", model_file, train_file); // Train normal CRF
		train_times.add(System.currentTimeMillis() - time_start);
		
		evaluateCRF(output_file, model_file, test_file); // Evaluate Normal CRF
		
		agentDataSource = new String[] {answer_file, output_file};
		
		iteration++;
	}
	
	/**
	 * Give the Evaluation Trackers the Answer and output
	 * files so that they can analyze the data
	 */
	@SuppressWarnings("unchecked")
	public void provideData (EvaluationAgent a) {
		a.addData(agentDataSource);
	}
	
	/**
	 * Breaks a given question down into its constituant tuples
	 */
	String[] getQuestionTokens (String question) {
		return question.split("\n");
	}
	
	/**
	 * Parses the answer file along with the CRF's output file, looking for questions the CRF
	 * labeled causal. It takes these questions and outputs them to a test for CRF2 as well
	 * as their answers to an answer file for CRF2.
	 * @param testFile- test for CRF2
	 * @param ansFile- answers for CRF2's test
	 * @param fullOutFile - CRF's output file
	 * @param fullAnsFile - CRF's test's answer file
	 */
	void writeSymFile (String testFile, String ansFile, String fullOutFile, String fullAnsFile) {
		ArrayList<String> answers = Include.readSentDelimFile(fullAnsFile);
		ArrayList<String> output = Include.readSentDelimFile(fullOutFile);
		
		ArrayList<String> pos_ans = new ArrayList<String>();
		ArrayList<String> pos_test = new ArrayList<String>();
		
		for (int i = 0; i < answers.size(); i++) {
			String ans = answers.get(i);
			String given = output.get(i);
			if (sentenceContains(given, false, Include.CAUSE_TAG) || 
					sentenceContains(given, false,Include.EFFECT_TAG)) {
				pos_ans.add(ans);
				pos_test.add(getTest(ans));
			}
		}
		
		DataWriter.writeFile(testFile, pos_test);
		DataWriter.writeFile(ansFile, pos_ans);
	}
	
	/**
	 * Searches through a question looking for either a cause or effect token. 
	 * If it finds one, it returns the question as causal.
	 * @param data: A series of questions
	 * @return: A set of causal questions
	 */
	ArrayList<String> getPosExamples (List<String> data) {
		ArrayList<String> out = new ArrayList<String>();
		for (String question : data) {
			String[] tuples = question.split("\n");
			for (String tuple : tuples) {
				String ans = getAnswer(tuple);
				if (isCause(ans) || isEffect(ans)) {
					out.add(question);
					break;
				}
			}
		}
		return out;
	}
	
	/**
	 * Returns true if our sentence contains a word with the given key.
	 * This is used to check if sentence contains cause/effect/cue phrase.
	 */
	static boolean sentenceContains (String sent, boolean isAnswerBased, String key) {
		String[] segs = sent.split("\n");
		for (String token : segs) {
			if (isAnswerBased)
				token = getAnswer(token);
			if (token.startsWith(key))
				return true;
		}
		return false;
	}
	
	/**
	 * Returns if the the two tokens have agreeing classifications
	 */
	boolean tokenClassAgree (String tok1, String tok2) {
		char tok1c = getTokenClass(tok1).charAt(0);
		char tok2c = getTokenClass(tok2).charAt(0);
		
		if (tok1c == tok2c)
			return true;
		return false;
	}
	
	/**
	 * Given Tuple-- Does this tuple has effect Classification
	 * @param causeKey
	 * @return
	 */
	static boolean isEffect (String key) {
		return key.startsWith(Include.EFFECT_TAG);
	}
	
	/**
	 * Given Tuple -- Does this tuple have a Cause classification
	 * @param causeKey
	 * @return
	 */
	static boolean isCause (String key) {
		return key.startsWith(Include.CAUSE_TAG);
	}
	
	/**
	 * Given Tuple -- Does this tuple have a Cue Phrase Classification
	 * @param key
	 * @return
	 */
	static boolean isCuePhrase (String key) {
		return key.startsWith(Include.RELN_TAG);
	}
	
	/**
	 * Returns the classification associated with the Token:
	 * As found at the end of the token
	 * @param token
	 * @return
	 */
	String getTokenClass (String token) {
		token = token.trim();
		int i = token.lastIndexOf(" ");
		return token.substring(i, token.length());
	}
	
	/**
	 * Returns the class for a tuple of the form <word> <features> <class>
	 */
	static String getAnswer (String tuple) {
		return tuple.substring(tuple.lastIndexOf(" ")+1, tuple.length());
	}
	
	/**
	 * Trains the Simple Tagger CRF Model
	 * @param out_file- Used to output garbage outfile-- ignore
	 * @param model_file- File to output trained model to 
	 * @param train_file- File with training data to train upon
	 */
	public static void trainCRF (String out_file, String model_file, String train_file) {
		try {
			String[] args = {"--train","true","--out-file", out_file+"Bad",
					"--model-file",model_file, train_file};
			SimpleTagger.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Evaluates a trained CRF on a given Test File, output results
	 * @param output_file- The file with ultimate results
	 * @param model_file- The trained model file for CRF to use
	 * @param test_file- The testing file for CRF to be evaluated on 
	 */
	public static void evaluateCRF (String output_file, String model_file, String test_file) {
		try {
			String[] args2 = {"--include-input","false","--train","false","--out-file",output_file,
					"--model-file",model_file, test_file};
			SimpleTagger.main(args2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void help () {
		try {
			String[] args2 = {"--help"};
			SimpleTagger.main(args2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static ArrayList<String> createTestQuestions (List<String> testData) {
		ArrayList<String> testQuestions = new ArrayList<String>();
		for (String problem: testData) {
			testQuestions.add(getTest(problem));
		}
		return testQuestions;
	}
	
	/**
	 * Removes the trailing class label from a given tuple
	 */
	public static String getTest (String item) {
		String out = "";
		String[] tuples = item.split("\n");
		for (int i = 0; i < tuples.length; i++) {
			String tuple = tuples[i];
			int index = tuple.lastIndexOf(" ");
			if (index <= 0) {
				System.out.println("Encountered Erroneous Tuple: " + item);
				continue;
			}
			out += tuple.substring(0, index) + "\n";
		}
		return out;
	}
}

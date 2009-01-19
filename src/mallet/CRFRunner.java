package mallet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import haus.io.DataWriter;

import evaluation.CrossValidator;
import evaluation.CrossEvaluable;
import evaluation.EvalTracker;

public class CRFRunner extends CrossEvaluable<String> {
	public static final String SENT_DELIM = "_END_OF_SENTENCE_";
	public static final int NUM_VALIDATIONS = 10;
	
	String data_file = "crf/crf.txt";
	String out_file = "mallet_out/";
	
	ArrayList<String> data = null;
	ArrayList<Double> grades = null;
	ArrayList<Long> train_times = null;
	
	CrossValidator<String> validator = null;
	
	SentenceClassificationEvaluationAgent CausalSentenceAgent = null;
	CauseTokenClassificationEvaluationAgent CauseTokenAgent = null;
	EffectTokenClassificationEvaluationAgent EffectTokenAgent = null;
	
	int numCorrectTokens = 0;
	int numTokens = 0;
	
	int iteration = 0;
	
	public CRFRunner () {
		grades = new ArrayList<Double>();
		data = readDataFromFile(data_file);
		train_times = new ArrayList<Long>();
		Collections.shuffle(data);
		validator = new CrossValidator<String>(NUM_VALIDATIONS, data, this);
		
		CausalSentenceAgent = new SentenceClassificationEvaluationAgent();
		CauseTokenAgent = new CauseTokenClassificationEvaluationAgent();
		EffectTokenAgent = new EffectTokenClassificationEvaluationAgent();
		
		addTracker(new EvalTracker(CausalSentenceAgent, "Causal Sentence Tracker"));
		addTracker(new EvalTracker(CauseTokenAgent, "Cause Token Tracker"));
		addTracker(new EvalTracker(EffectTokenAgent, "Effect Token Tracker"));
	}
	
	public static void main (String[] args) {
		CRFRunner c = new CRFRunner();
		
		System.out.println("Data size: " + c.data.size());
		
		c.run();
		
		System.out.println("CRF Training Times:");
		for (Long l : c.train_times)
			System.out.print(l + ", ");
		System.out.println("");
		
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
		//String pos_answer_file = out_file + "Pos_Answers_" + iteration;
		String train_file = out_file + "Train_" + iteration;
		//String pos_train_file = out_file + "Pos_Train_" + iteration;
		String test_file = out_file + "Test_" + iteration;
		//String pos_test_file = out_file + "Pos_Test_" + iteration;
		String model_file = out_file + "causeModel_" + iteration;
		//String pos_model_file = out_file + "Pos_causeModel_" + iteration;
		String output_file = out_file + "Output_" + iteration;
		//String pos_output_file = out_file + "Pos_Output_" + iteration;
		//String grade_file = out_file + "Grade_" + iteration;

		writeFile(answer_file, testData); // Create Norm Answer file
		writeFile(train_file, trainData); // Create Norm Train file
		//writeFile(pos_train_file, getPosExamples(trainData)); // Create Postive Example Train file
		writeFile(test_file, createTestQuestions(testData)); // Create Normal Test File
		
		long time_start = System.currentTimeMillis();
		trainCRF(output_file+"Bad", model_file, train_file); // Train normal CRF
		train_times.add(System.currentTimeMillis() - time_start);
		
		evaluateCRF(output_file, model_file, test_file); // Evaluate Normal CRF
		
		// Write a test/answer file for CRF 2 from the positive data CRF 1 labeled
		//writeSymFile(pos_test_file, pos_answer_file, output_file, answer_file); 
		//trainCRF(output_file+"Bad", pos_model_file, pos_train_file); // Train POS CRF
		//evaluateCRF(pos_output_file, pos_model_file, pos_test_file); // Evaluate POS CRF
		
		//gradeCRF(grade_file, answer_file, output_file);
		try {
			CausalSentenceAgent.getData(answer_file, output_file);
			CauseTokenAgent.getData(answer_file, output_file);
			EffectTokenAgent.getData(answer_file, output_file);
			//CauseTokenAgent.getData(pos_answer_file, pos_output_file);
			//EffectTokenAgent.getData(pos_answer_file, pos_output_file);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		iteration++;
	}
	
	
	/**
	 * Determine how well CRF2 has preformed on the cause/effect token labeling task.
	 * @param answer_file
	 * @param test_file
	 * @return
	 * @throws Exception
	 
	void gradeTokenCausality (String pos_answer_file, String pos_test_file) {
		ArrayList<String> answers = readDataFromFile(pos_answer_file);
		ArrayList<String> given = readDataFromFile(pos_test_file);
		
		for (int q = 0; q < answers.size(); q++) {
			String ans = answers.get(q);
			String giv = given.get(q);
			checkTokenCausality(ans, giv);
		}
	}*/
	
	/**
	 * Checks each token in the answer to see how well the classification agrees
	 * with the given answer
	 * @param answer
	 * @param given_answer
	 
	void checkTokenCausality (String answer, String given_answer) {
		String[] answer_segs = getQuestionTokens(answer);
		String[] given_segs = getQuestionTokens(given_answer);
		
		for (int t = 0; t < answer_segs.length; t++) {
			String ansTok = answer_segs[t];
			String givTok = given_segs[t];
			
			boolean relevant = false;
			boolean retrieved = false;
			
			if (isEffect(ansTok)) {
				EffectTokenTracker.addRelevant();
				relevant = true;
			}
			if (isEffect(givTok)) {
				EffectTokenTracker.addRetrieved();
				retrieved = true;
			}
			if (relevant && retrieved) {
				
			}
			if ()
				
			
			if (tokenClassAgree(ansTok, givTok)) {
				if (isEffect || is)
			}
		}
	}*/
	
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
		ArrayList<String> answers = readDataFromFile(fullAnsFile);
		ArrayList<String> output = readDataFromFile(fullOutFile);
		
		ArrayList<String> pos_ans = new ArrayList<String>();
		ArrayList<String> pos_test = new ArrayList<String>();
		
		for (int i = 0; i < answers.size(); i++) {
			String ans = answers.get(i);
			String given = output.get(i);
			if (hasCauseQuestion(given, false) || hasEffectQuestion(given, false)) {
				pos_ans.add(ans);
				pos_test.add(getTest(ans));
			}
		}
		
		writeFile(testFile, pos_test);
		writeFile(ansFile, pos_ans);
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
	 * Returns true if the string Question has a token with a Cause Label
	 * @param isAnswerBased: If so look at end of sentence for class.
	 * Otherwise look at beginning of sentence for class.
	 */
	static boolean hasCauseQuestion (String question, boolean isAnswerBased) {
		String[] segs = question.split("\n");
		for (String token : segs) {
			if (isAnswerBased)
				token = getAnswer(token);
			if (isCause(token))
				return true;
		}
		return false;
	}
	
	static boolean hasEffectQuestion (String question, boolean isAnswerBased) {
		String[] segs = question.split("\n");
		for (String token : segs) {
			if (isAnswerBased)
				token = getAnswer(token);
			if (isEffect(token))
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
	static boolean isEffect (String causeKey) {
		if (causeKey.startsWith("E"))
			return true;
		return false;
	}
	
	/**
	 * Given Tuple -- Does this tuple have a Cause classification
	 * @param causeKey
	 * @return
	 */
	static boolean isCause (String causeKey) {
		if (causeKey.startsWith("C"))
			return true;
		return false;
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
			String[] args2 = {"--train","false","--out-file",output_file,
					"--model-file",model_file, test_file};
			SimpleTagger.main(args2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	ArrayList<String> createTestQuestions (List<String> testData) {
		ArrayList<String> testQuestions = new ArrayList<String>();
		for (String problem: testData) {
			testQuestions.add(getTest(problem));
		}
		return testQuestions;
	}
	
	void writeFile (String fileName, List<String> data) {
		DataWriter writer = new DataWriter(fileName);
		for (String answer: data)
			writer.write(answer);
		writer.close();
	}
	
	/**
	 * Checks for a new sentence
	 */
	private static boolean isNewSent (String line)  {
		if (line.contains(SENT_DELIM))
			return true;
		return false;
	}
	
	/**
	 * Reads the DataFile and return an arraylist in which each string 
	 * is a sentences with features
	 */
	public static ArrayList<String> readDataFromFile (String dataFile) {
		ArrayList<String> out = new ArrayList<String>();
		try {
			BufferedReader input =  new BufferedReader(new FileReader(dataFile));
			try {
				String line = null;
				String block = "";
				while ((line = input.readLine()) != null){
					line = line.trim();
					block += line + "\n";
					if (isNewSent(line)) {
						out.add(block);
						block = "";
					}
				}
			}
			finally {
				input.close();
			}
		}
		catch (IOException ex){
			ex.printStackTrace();
		}
		return out;
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

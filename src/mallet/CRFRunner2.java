package mallet;

import haus.io.DataWriter;

import java.util.List;

import evaluation.CrossEvaluable;
import evaluation.EvalTracker;
import evaluation.EvaluationAgent;

/**
 * This class only runs the CRF after the data is divided by
 * the CRF data divider.
 *
 */
public class CRFRunner2 extends CrossEvaluable<String>{
	String answer_file = "Answers";
	String train_file = "Train";
	String test_file = "Test";
	String model_file = "Model";
	String output_file = "Output";
	String grade_file = "Grade";
	
	public CRFRunner2 () {
		addTracker(new EvalTracker(new SentenceClassificationEvaluationAgent(), "Causal Sentence Tracker",this));
		//addTracker(new EvalTracker(new CauseTokenClassificationEvaluationAgent(), "Cause Token Tracker",this));
		//addTracker(new EvalTracker(new EffectTokenClassificationEvaluationAgent(), "Effect Token Tracker",this));
		addTracker(new EvalTracker(new CuePhraseTokenClassificationEvaluationAgent(), "Cue Phrase Token Tracker",this));
		
		CRFRunner.trainCRF(output_file+"Bad", model_file, train_file); // Train normal CRF
		
		CRFRunner.evaluateCRF(output_file, model_file, test_file); // Evaluate Normal CRF
		evaluate();
		DataWriter writer = new DataWriter(grade_file);
		writer.write(getEvaluationResults());
		writer.close();
	}
	
	public void handleData(List<String> trainData, List<String> testData) {}

	@SuppressWarnings("unchecked")
	public void provideData(EvaluationAgent a) {
		a.addData(new String[] {answer_file, output_file});
	}
	
	public static void main (String[] args) {
		@SuppressWarnings("unused")
		CRFRunner2 runner = new CRFRunner2();
	}
}

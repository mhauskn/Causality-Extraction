package mallet;

import haus.io.DataWriter;

import java.util.ArrayList;
import java.util.List;

import evaluation.CrossEvaluable;
import evaluation.CrossValidator;
import evaluation.EvaluationAgent;


/**
 * This class simply divides our CRF Data into 
 * multiple sections for later processing.
 */
public class CRFDataDivider extends CrossEvaluable<String> {
	String data_file = "crf/crf.txt";
	public static final int NUM_VALIDATIONS = 10;
	String out_file = "parallel/";
	int iteration = 0;
	
	public CRFDataDivider () {
		ArrayList<String> data = Include.readSentDelimFile(data_file);
		CrossValidator<String> validator = new CrossValidator<String>(NUM_VALIDATIONS, data, this);
		validator.crossValidate();
	}
	
	@Override
	public void handleData(List<String> trainData, List<String> testData) {
		String answer_file = out_file + iteration + "/Answers";
		String train_file = out_file + iteration + "/Train";
		String test_file = out_file + iteration + "/Test";

		DataWriter.writeFile(answer_file, testData); // Create Norm Answer file
		DataWriter.writeFile(train_file, trainData); // Create Norm Train file
		//writeSampledTrainFile(trainData,train_file); // Creates a 50/50 Distribution train file
		DataWriter.writeFile(test_file, CRFRunner.createTestQuestions(testData)); // Create Normal Test File
		iteration++;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void provideData(EvaluationAgent a) {}
	
	public static void main (String[] args) {
		@SuppressWarnings("unused")
		CRFDataDivider d = new CRFDataDivider();
	}
	
	/**
	 * Writes a training file for our cue phrase 
	 * task which is sampled at roughly 50% pos
	 * and negative examples.
	 */
	public void writeSampledTrainFile (List<String> trainData, String file_name) {
		DataWriter w = new DataWriter(file_name);
		boolean reset = true;
		ArrayList<String> non = new ArrayList<String>();
		for (int i = 0; i < trainData.size(); i++) {
			String sent = trainData.get(i);
			String[] segs = sent.split("\n");
			for (int j = 0; j < segs.length; j++) {
				String line = segs[j];
				if (CRFRunner.getAnswer(line).equals(Include.NEITHER_TAG)) {
					reset = true;
					non.add(line);
				} else { // Pos example
					if (reset)
						w.writeln(non.remove(((int) (Math.random() * non.size()))));
					w.writeln(line);
					reset = false;
				}
			}
		}
		w.writeln(Include.SENT_DELIM);
		w.close();
	}
}

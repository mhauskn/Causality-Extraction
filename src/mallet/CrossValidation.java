package mallet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;


public class CrossValidation {
	public static final String SENT_DELIM = "_END_OF_SENTENCE_ N";
	public static final int NUM_VALIDATIONS = 10;
	public static String outfile = "mallet_out/";
	public static String data_file = "allcrfWorking.txt";
	static ArrayList<Double> grades = null;
	static ArrayList<Double> classification_type1_grades = null;
	static ArrayList<Double> classification_type2_grades = null;
	
	public static void main (String[] args) {
		ArrayList<String> data = getData();
		grades = new ArrayList<Double>();
		classification_type1_grades = new ArrayList<Double>();
		classification_type2_grades = new ArrayList<Double>();
		System.out.println("Data size: " + data.size());
		
		try {
			double index = 0.0;
			double increment = data.size() / (double) NUM_VALIDATIONS;
			for (int i = 0; i < NUM_VALIDATIONS; i++) {
				int end = (int) Math.round(index + increment);
				crossValidate(i, (int) Math.round(index), end, data);
				index += increment;
			}
			System.out.println("Overall Average: " + getAvg(grades));
			System.out.println("Classification Type 1 Average: " + getAvg(classification_type1_grades));
			System.out.println("Classification Type 2 Average: " + getAvg(classification_type2_grades));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Double getAvg (ArrayList<Double> list) {
		double sum = 0.0;
		for (Double d: list)
			sum += d;
		return sum/list.size();
	}
	
	public static void crossValidate (int iter, int start, int end, ArrayList<String> data) 
		throws Exception {
		System.out.printf("Iter %d start %d end %d\n", iter, start, end);
		
		String answer_file = outfile + "Answers_" + iter;
		String train_file = outfile + "Train_" + iter;
		String test_file = outfile + "Test_" + iter;
		String model_file = outfile + "causeModel_" + iter;
		String output_file = outfile + "Output_" + iter;
		String grade_file = outfile + "Grade_" + iter;
		
		FileWriter fstream = null;
		BufferedWriter out = null;
		
		fstream = new FileWriter(answer_file);
		out = new BufferedWriter(fstream);
		for (int i = start; i < end; i++)
			out.write(data.get(i));
		out.close();
		
		fstream = new FileWriter(train_file);
		out = new BufferedWriter(fstream);
		for (int i = 0; i < start; i++)
			out.write(data.get(i));
		for (int i = end; i < data.size(); i++)
			out.write(data.get(i));
		out.close();
		
		fstream = new FileWriter(test_file);
		out = new BufferedWriter(fstream);
		for (int i = start; i < end; i++)
			out.write(getTest(data.get(i)));
		out.close();
		
		String[] args = {"--train","true","--out-file", outfile+"Bad",
				"--model-file",model_file, train_file};
		SimpleTagger.main(args);
		
		String[] args2 = {"--train","false","--out-file",output_file,
				"--model-file",model_file, test_file};
		SimpleTagger.main(args2);
		
		fstream = new FileWriter(grade_file);
		out = new BufferedWriter(fstream);
		out.write(grade(answer_file, output_file));
		out.close();
	}
	
	public static String grade (String answer_file, String test_file) throws Exception {
		StringBuilder out = new StringBuilder();
		
		BufferedReader aReader =  new BufferedReader(new FileReader(answer_file));
		BufferedReader tReader =  new BufferedReader(new FileReader(test_file));
		
		int questionNum = 0;
		boolean correct = true;
		boolean crf_has_cause = false;
		boolean crf_has_effect = false;
		boolean ans_has_cause = false;
		boolean ans_has_effect = false;
		int numCorrect = 0; int numWrong = 0;
		int correctlyClassified1 = 0; int incorrectlyClassified1 = 0; //Strict: Counts badly formed results
		int correctlyClassified2 = 0; int incorrectlyClassified2 = 0; //Easy: Discards badly formed resutls
		out.append("Question " + questionNum + ": ");
		
		ArrayList<String> resp = new ArrayList<String>();
		ArrayList<String> good = new ArrayList<String>();
		
		while (true) {
			String aline = aReader.readLine();
			String tline = tReader.readLine();
			if (aline == null && tline == null)
				break;
			aline = aline.trim(); tline = tline.trim();
			
			String correctAns = aline.substring(aline.lastIndexOf(" ")+1, aline.length());
			String givenAns = tline.substring(0, tline.indexOf(" "));
			
			good.add(correctAns + " " + aline); resp.add(givenAns);
			
			if (correctAns.startsWith("C"))
				ans_has_cause = true;
			if (correctAns.startsWith("E"))
				ans_has_effect = true;
			if (givenAns.startsWith("C"))
				crf_has_cause = true;
			if (givenAns.startsWith("E"))
				crf_has_effect = true;
			
			if (!correctAns.equals(givenAns)) {
				correct = false;
			}
				
			if (isNewSent(aline)) {
				questionNum++;
				if (correct) {
					out.append("Correct!\n");
					for (int i = 0; i < good.size(); i++) {
						out.append("\t" + good.get(i) + "\n");
					}
					numCorrect++;
				} else {
					out.append("Incorrect.\nGiven   Correct\n");
					for (int i = 0; i < good.size(); i++) {
						out.append(resp.get(i));
						for (int j = resp.get(i).length(); j < 8; j++)
							out.append(".");
						out.append(good.get(i) + "\n");
					}
					numWrong++;
				}
				if (ans_has_cause && ans_has_effect) {
					if (crf_has_cause && crf_has_effect) {
						correctlyClassified1++;
						correctlyClassified2++;
					} else if (crf_has_cause || crf_has_effect) {
						correctlyClassified2++;
						incorrectlyClassified1++;
					} else {
						incorrectlyClassified1++;
						incorrectlyClassified2++;
					}
				} else {
					if (ans_has_cause || ans_has_effect) {
						System.out.println("Encountered Malformed Sentence! Lacks both cause and effect!");
						System.exit(1);
					}
					if (crf_has_cause && crf_has_effect) {
						incorrectlyClassified1++;
						incorrectlyClassified2++;
					} else if (crf_has_cause || crf_has_effect) {
						correctlyClassified2++;
						incorrectlyClassified1++;
					} else {
						correctlyClassified1++;
						correctlyClassified2++;
					}
				}
				
				correct = true;
				crf_has_cause = false;
				crf_has_effect = false;
				ans_has_cause = false;
				ans_has_effect = false;
				resp.clear(); good.clear();
				out.append("\nQuestion " + questionNum + ": ");
			}
		}
		double grade = numCorrect / (double) (numCorrect + numWrong);
		out.append("Num Correct: " + numCorrect + " Num Incorrect: " + numWrong + 
				" Grade: " + grade + "\n");
		double classification_grade1 = correctlyClassified1 / (double) (correctlyClassified1 + incorrectlyClassified1);
		out.append("Num Correctly Classified (Strict: Counting Malformed): " + classification_grade1 + "\n");
		double classification_grade2 = correctlyClassified2 / (double) (correctlyClassified2 + incorrectlyClassified2);
		out.append("Num Correctly Classified (Easy: Discarding Malformed): " + classification_grade2 + "\n");
		grades.add(grade);
		classification_type1_grades.add(classification_grade1);
		classification_type2_grades.add(classification_grade2);
		aReader.close();
		tReader.close();
		return out.toString();
	}
	
	/**
	 * Checks for a new sentence
	 */
	private static boolean isNewSent (String line)  {
		if (line.equals(SENT_DELIM))
			return true;
		return false;
	}
	
	/**
	 * Reads the DataFile and return an arraylist in which each string 
	 * is a sentences with features
	 */
	public static ArrayList<String> getData () {
		ArrayList<String> out = new ArrayList<String>();
		try {
			BufferedReader input =  new BufferedReader(new FileReader(data_file));
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
		Collections.shuffle(out);
		return out;
	}
	
	public static String getTest (String item) {
		String out = "";
		String[] tuples = item.split("\n");
		for (int i = 0; i < tuples.length; i++) {
			String tuple = tuples[i];
			int index = tuple.lastIndexOf(" ");
			out += tuple.substring(0, index) + "\n";
		}
		return out;
	}
}

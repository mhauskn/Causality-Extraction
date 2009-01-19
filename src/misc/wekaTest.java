package misc;

import weka.core.converters.CSVLoader;

public class wekaTest {
	public static void main (String[] args) {
		for (String s : args)
			System.out.println(s);
		String in = UnsupervisedCauseExtractor.file_path + UnsupervisedCauseExtractor.file_name;
		String out = UnsupervisedCauseExtractor.file_path + "data.arff";
		String[] argsnew = new String[2];
		argsnew[0] = in;
		argsnew[1] = out;
		//CSVLoader loader = new CSVLoader();
		CSVLoader.main(argsnew);
	}
}

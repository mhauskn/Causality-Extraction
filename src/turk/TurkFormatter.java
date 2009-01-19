package turk;

import java.io.IOException;

import haus.io.FileReader;
import haus.io.DataWriter;

/**
 * Given a list of sentence as input, the TurkFormatter will create a Turk
 * task with each sentence in a csv format.
 * @author epn
 *
 */
public class TurkFormatter {
	String delim = ","; 
	int sentPerLine = 10; //Num Sentences Per Line
	String batch = "";
	int batchCnt = 0;
	int written = 0;
	DataWriter dw = null;
	
	public TurkFormatter (DataWriter _dw) {
		dw = _dw;
		dw.write("sentence0, sentence1, sentence2, sentence3, sentence4, sentence5, sentence6, sentence7, sentence8, sentence9" + Include.LINE_DELIM);
	}
	
	/**
	 * Formats Sentence into the given turk format
	 * @param sentence
	 * @throws IOException
	 */
	public void format (String sentence) throws IOException {
		sentence = sentence.trim();
		int splitlen = sentence.split(" ").length;
		if (splitlen > Include.MAX_SENT_LEN || splitlen <= 1) {
			System.out.println("Ruled out sentence of length " + splitlen + " " + sentence);
			return;
		}
		sentence = sentence.replaceAll("\"", "");
		if (batchCnt >= sentPerLine) {
			flush();
		}
		batch += "\"" + sentence + "\"" + delim;
		batchCnt++;
	}
	
	public void flush () throws IOException {
		dw.write(batch + Include.LINE_DELIM);
		batch = "";
		batchCnt = 0;
		written++;
	}
	
	public static void main (String[] args) throws IOException {
		DataWriter dw = new DataWriter("turk/newTurkTask.csv");
		TurkFormatter tf = new TurkFormatter(dw);
		FileReader r = new FileReader("turk/ReduxResponseGood.txt");
		String line;
		while ((line = r.getNextLine()) != null) {
			tf.format(line);
		}
		tf.flush();
		dw.close();
	}
}

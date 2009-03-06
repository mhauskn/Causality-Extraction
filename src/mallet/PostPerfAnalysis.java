package mallet;

import haus.io.DataWriter;
import haus.io.FileReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;

/**
 * Analyzes our Answer and Output files to determine where CRF 
 * excelled and where it needs more help.
 */
public class PostPerfAnalysis {
	Hashtable<String,WordStat> stats = new Hashtable<String,WordStat>();
	/**
	 * Statistics about each individual word
	 */
	class WordStat implements Comparable<WordStat> {
		String myWord;
		String prevWords;
		public int numRelevant = 0;
		public int numCorrectlyRetrieved = 0;
		public int numIncorrectlyRetrieved = 0;
		public int numOccurences = 0; //TODO: This is tricky
		
		public WordStat (String _myWord, String _prevWord) {
			myWord = _myWord;
			prevWords = _prevWord;
		}
		
		int sum () {
			return numRelevant + numCorrectlyRetrieved + numIncorrectlyRetrieved;
		}
		
		public int compareTo(WordStat arg0) {
			return sum() < arg0.sum() ? 1 : -1;
		}
		
		public String toString () {
			StringBuilder b = new StringBuilder();
			b.append(prevWords + myWord);
			b.append(" " + numCorrectlyRetrieved + "/" + numRelevant);
			b.append(" incorrectly_retrieved: " + numIncorrectlyRetrieved);
			return b.toString();
		}
	}
	
	/**
	 * Symmetrically parses the answers and the output files.
	 * Checks to make sure that the given answers match or dismatch the 
	 * ones outputted by CRF.
	 */
	public void doSymParse (ArrayList<String> answers, ArrayList<String> outputs) {
		String ans_prev = "";
		String out_prev = "";
		for (int i = 0; i < answers.size(); i++) {
			String[] answer = answers.get(i).split(" ");
			String[] output = outputs.get(i).split(" ");
			String token = answer[0];
			String ans_label = answer[answer.length-1];
			String out_label = output[0];
			String ans_key = ans_prev + token;
			String out_key = out_prev + token;
			boolean ans_pos = ans_label.startsWith(Include.RELN_TAG);
			boolean out_pos = out_label.startsWith(Include.RELN_TAG);
			
			if (ans_pos && !stats.containsKey(ans_key))
				stats.put(ans_key, new WordStat(token,ans_prev));
			if (out_pos && !stats.containsKey(out_key))
				stats.put(out_key, new WordStat(token,ans_prev));
			
			if (ans_label.equals(out_label) && ans_pos) {
				WordStat w = stats.get(ans_key);
				w.numCorrectlyRetrieved++;
				w.numRelevant++;
			} else if (ans_pos) {
				WordStat w = stats.get(ans_key);
				w.numRelevant++;
			} else if (out_pos) {
				WordStat w = stats.get(out_key);
				w.numIncorrectlyRetrieved++;
			}
			
			if (ans_pos && !ans_key.equals(Include.RELN_TAG))
				ans_prev += token + " ";
			else
				ans_prev = "";
			if (out_pos && !out_key.equals(Include.RELN_TAG))
				out_prev += token + " ";
			else
				out_prev = "";
		}
	}
	
	void writeResults (String out_file) {
		ArrayList<WordStat> list = new ArrayList<WordStat>(stats.values());
		Collections.sort(list);
		DataWriter.writeObjects(out_file, list);
	}
	
	public static void main (String[] args) {
		
		PostPerfAnalysis p = new PostPerfAnalysis();
		String base_path = args[0] + "/";
		String out_file = args[1];
		for (int i = 0; i < 10; i++)
			p.doSymParse(FileReader.readFile(base_path + i + "/Answers"), 
					FileReader.readFile(base_path + i + "/Output"));
		p.writeResults(out_file);
		/*
		p.doSymParse(FileReader.readFile("temp/Answers"), 
					FileReader.readFile("temp/Output"));
		p.writeResults("temp/results.txt");*/
	}
}

package mallet;

import haus.io.DataWriter;
import haus.io.FileReader;

import java.util.ArrayList;

import evaluation.EvalTracker;

/**
 * Aggregates our individual grade files into a larger grade file
 *
 */
public class gradeAgg {
	String[] trackers = new String[] { "Causal Sentence Tracker:", 
			"Cue Phrase Token Tracker:" };
	String[] stats = new String[] { "Relevant:", "Retrieved:", "Rel&Ret:", "Total_Seen:" };
	
	int[][] table;
	
	int majorInd = 0;
	
	public gradeAgg () {
		table = new int[trackers.length][stats.length];
	}
	
	void parseFile (ArrayList<String> sentences) {
		for (String s : sentences) {
			parseTracker(s);
			parseStats(s);
		}
	}
	
	void parseTracker (String s) {
		for (int i = 0; i < trackers.length; i++)
			if (s.indexOf(trackers[i]) != -1)
				majorInd = i;
	}
	
	void parseStats (String s) {
		for (int i = 0; i < stats.length; i++) {
			int statStart = s.indexOf(stats[i]);
			if (statStart != -1)
				table[majorInd][i] += getFollowingNum(s, statStart);
		}
	}
	
	int getFollowingNum (String s, int statStart) {
		int start = s.indexOf(' ', statStart) + 1;
		String out = "";
		char c = s.charAt(start++);
		while (Character.isDigit(c)) {
			out += c;
			if (start == s.length())
				break;
			c = s.charAt(start++);
		}
		return Integer.parseInt(out);
	}
	
	ArrayList<String> GetScores () {
		ArrayList<String> out = new ArrayList<String>();
		EvalTracker t = new EvalTracker(null, "", null);
		for (int i = 0; i < trackers.length; i++) {
			t.trackerName = trackers[i];
			t.relevant = table[i][0];
			t.retrieved = table[i][1];
			t.retrieved_relevant = table[i][2];
			t.total_visited = table[i][3];
			out.add(t.getScoreBattery() + "\n");
		}
		return out;
	}
	
	public static void main (String[] args) {
		gradeAgg agg = new gradeAgg();
		agg.parseFile(FileReader.readFile(args[0]));
		DataWriter.writeFile(args[1], agg.GetScores());
	}
}

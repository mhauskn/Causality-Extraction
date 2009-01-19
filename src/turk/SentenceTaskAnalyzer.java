package turk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import haus.io.FileReader;
import haus.io.DataWriter;

/**
 * Deals with the Analysis of the task of labeling sentence turk data
 * as causal or non-causal.
 * This is Task 1.
 * @author epn
 *
 */
public class SentenceTaskAnalyzer {
	TurkReader reader;
	ArrayList<Integer[]> knownCausal;
	Hashtable<String,Boolean> knownPos;
	int found = 0;
	int index = 0;
	int skipped = 0;
	Hashtable<String,Boolean> blacklist;
	
	String workerid_ind = "WorkerId";
	String hitid_ind = "HITId";

	
	public SentenceTaskAnalyzer () throws IOException {
		knownCausal = new ArrayList<Integer[]>();
		knownPos = new Hashtable<String,Boolean>();
		blacklist = new Hashtable<String,Boolean>();
		//parseKnownCausal();
		parseKnownPos();
		
		DataWriter dw = new DataWriter("turk/Non-CausalSentences.txt");
		reader = new TurkReader();
		reader.parseBatchFile("turk/reduxResult.csv");
		Enumeration<String> e = reader.hits.keys();
		while (e.hasMoreElements()) {
			String id = e.nextElement();
			ArrayList<String[]> responses = reader.hits.get(id).getResults();
			checkQuality(responses);
		}
		e = reader.hits.keys();
		while (e.hasMoreElements()) {
			String id = e.nextElement();
			ArrayList<String[]> responses = reader.hits.get(id).getResults();
			dw.write(handleElement(responses, index++));
		}
		System.out.println("Sentences found: " + found + " and " + skipped + " were blacklisted.");
		dw.write("Sentences found: " + found);
		dw.close();
	}
	
	void checkQuality (ArrayList<String[]> responses) {
		for (int i = 0; i < responses.size(); i++) {
			int strikes = 0;
			int numYes = 0;
			int numNo = 0;
			int numUnsure = 0;
			String[] resp = responses.get(i);
			String id = reader.decode(workerid_ind, resp);
			String hitid = reader.decode(hitid_ind, resp);
			for (int j = 0; j < 10; j++) {
				String sentence = "Input.sentence" + j;
				String question = "Answer.Causal_Option_Box" + j;
				String sent = reader.decode(sentence, resp);
				String ans = reader.decode(question, resp);
				if (ans.equals("Yes"))
					numYes++;
				else if (ans.equals("No"))
					numNo++;
				else if (ans.equals("Unsure"))
					numUnsure++;
				if (knownPos.containsKey(sent)) {
					if (!ans.equals("Yes"))
						strikes++;
				}
			}
			if (strikes > 1) {
				blacklist.put(hash(hitid, id), true);
				System.out.println("ID " + id + " Has been blacklisted on hit " + hitid);
			}
			if (numYes == 10 || numNo == 10 || numUnsure == 10) {
				System.out.println("ID " + id + " Has selected Uniform Answers hitid: " + hitid);
			}
			//if (numNo == 0 || numYes == 0) {
			//	System.out.println("No No's or Yesses from " + id + " On hit " + hitid);
			//}
		}
	}
	
	String hash (String hitid, String workerid) {
		return hitid + "_" + workerid;
	}
	
	void parseKnownPos() {
		FileReader cr = new FileReader("turk/pos.txt");
		String line;
		while ((line = cr.getNextLine()) != null) {
			line = line.trim();
			if (line.length() == 0) 
				continue;
			if (knownPos.containsKey(line)) {
				System.out.println("Hash Collision or duplication sentence!");
			}
			knownPos.put(line, true);
		}
	}
	
	/**
	 * Reads our list of known causal sentences so that we can identify them
	 */
	void parseKnownCausal () {
		FileReader cr = new FileReader("Turk/taskIndex.txt");
		String line;
		while ((line = cr.getNextLine()) != null) {
			String[] segs = line.split(",");
			Integer[] ind = new Integer[2];
			ind[0] = Integer.parseInt(segs[0].trim());
			ind[1] = Integer.parseInt(segs[1].trim());
			knownCausal.add(ind);
		}
	}
	
	public String handleElement (ArrayList<String[]> responses, int index) {
		String out = "";
		for (int i = 0; i < 10; i++) {
			String sentence = "Input.sentence" + i;
			String question = "Answer.Causal_Option_Box" + i;
			String sent = "";
			int numYes = 0;
			int numNo = 0;
			int numUnsure = 0;
			int numBlacklisted = 0;
			for (int j = 0; j < responses.size(); j++) {
				String[] resp = responses.get(j);
				sent = reader.decode(sentence, resp);
				if (blacklisted(resp)) {
					skipped++;
					numBlacklisted++;
					continue;
				}
				
				String ans = reader.decode(question, resp);
				if (ans.equals("Yes"))
					numYes++;
				else if (ans.equals("No"))
					numNo++;
				else if (ans.equals("Unsure"))
					numUnsure++;
			}
			if (knownPos.containsKey(sent)) {
				numYes=0;
			}
			//if (numYes > 4) {
			//if (numNo + numUnsure == 0) {
			if (!maj(numYes, numNo + numUnsure) && !knownPos.containsKey(sent)) { // Detect clean negatives
			//if (numYes > 3) {
				out += sent + "\n";
				//out += sent + " Voted Causal: (Yes-" + numYes + ") (No-" + numNo + ") (Unsure-" +
				//numUnsure + ") (Blacklisted-" + numBlacklisted + ")\n";
				//System.out.printf("%s Voted Causal: Yes: %d No: %d Unsure: %d Blacklisted: %d\n",
				//		sent, numYes, numNo, numUnsure, numBlacklisted);
				found++;
			}
		}
		return out;
	}
	
	boolean maj (int numFor, int numAgainst) {
		if (numFor > numAgainst)
			return true;
		return false;
	}
	
	boolean blacklisted (String[] resp) {
		String hitid = reader.decode(hitid_ind, resp);
		String workerid = reader.decode(workerid_ind, resp);
		if (blacklist.containsKey(hash(hitid, workerid)))
			return true;
		return false;
	}
	
	public static void main (String[] args) {
		try {
			@SuppressWarnings("unused")
			SentenceTaskAnalyzer crr = new SentenceTaskAnalyzer();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

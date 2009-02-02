package probability;

import include.Include;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import haus.io.DataWriter;

/**
 * Attempts to find prior probabilities between nouns in a corpus.
 *
 */
public class PriorProbGen2 {
	public static String TAG_SPLIT = "_";
	
	Hashtable<String,Integer> refs;
	int total;
	DecimalFormat df;
	
	public PriorProbGen2 () {
		refs = new Hashtable<String,Integer>();
		total = 0;
		df = new DecimalFormat("#.###");
	}
	
	public void processNouns (String nouns, String delim) {
		String[] na = nouns.split(delim);
		ArrayList<String> nn = new ArrayList<String>();
		for (String noun : na)
			nn.add(noun);
		processNouns(nn);
	}
	
	public void processTaggedSent (String sentence) {
		String[] words = sentence.split(" ");
		ArrayList<String> nouns = new ArrayList<String>();
		for (String word : words) {
			String[] parts = word.split(TAG_SPLIT);
			if (parts.length > 2) {
				//System.out.println("Invalid Tagged Word Encountered: " + word);
				continue;
			}
			if (parts[1].matches(Include.NOUN_REGEXP)) {
				String candidateNoun = parts[0].toLowerCase();
				if (!nouns.contains(candidateNoun) && candidateNoun.length() > 1)
					nouns.add(parts[0].toLowerCase());
			}
		}
		processNouns(nouns);
	}
	
	public void processNouns (ArrayList<String> nouns) {
		total++;
		for (int i = 0; i < nouns.size(); i++) {
			String noun = nouns.get(i);
			increment(noun);
			for (int j = i + 1; j < nouns.size(); j++) {
				String secNoun = nouns.get(j);
				increment(hashNouns(noun,secNoun));
			}
		}
	}
	
	private void increment (String noun) {
		if (!refs.containsKey(noun))
			refs.put(noun, 1);
		else
			refs.put(noun, refs.get(noun)+1);
	}
	
	private String hashNouns (String noun1, String noun2) {
		int res = noun1.compareTo(noun2);
		if (res == 0)
			return noun1;
		else if (res < 0)
			return noun1 + TAG_SPLIT + noun2;
		else if (res > 0)
			return noun2 + TAG_SPLIT + noun1;
		else
			System.out.println("Problem In hash function... Shouldnt have reached this code.");
		return null;
	}
	
	private boolean singular (String elem) {
		if (elem.indexOf(TAG_SPLIT) != -1)
			return false;
		return true;
	}
	
	private int getIntersect (String n1, String n2) {
		if (!refs.containsKey(hashNouns(n1,n2)))
			return 0;
		else
			return refs.get(hashNouns(n1,n2));
	}
	
	private double getProbability (String effect, String cause) {
		if (effect.equals(cause) || !refs.containsKey(effect) || !refs.containsKey(cause))
			return 0.0;
		
		double numCause = (double) refs.get(cause);
		double numEffect = (double) refs.get(effect);
		double numIntersec = (double) getIntersect(effect, cause);
		
		double priorProb = numIntersec/numCause - (numEffect-numIntersec)/((double)total-numCause);
		return priorProb;
	}
	
	private String getProbabilityStr (String n1, String n2) {
		if (n1.equals(n2) || !refs.containsKey(n1) || !refs.containsKey(n2))
			return "0.0";
		
		double numCause = (double) refs.get(n1);
		double numEffect = (double) refs.get(n2);
		double numIntersec = (double) getIntersect(n1, n2);
		
		String priorProb = df.format(numIntersec) + "/" + df.format(numCause) + " - (" + 
				df.format(numEffect) + " - " + df.format(numIntersec) + ")/(" + total + "-" + 
				df.format(numCause) + ")";
		return priorProb;
	}
	
	private String getExtraStr (String n1, String n2) {
		if (n1.equals(n2) || !refs.containsKey(n1) || !refs.containsKey(n2))
			return "0.0";
		
		double numCause = (double) refs.get(n1);
		double numEffect = (double) refs.get(n2);
		double numIntersec = (double) getIntersect(n1, n2);
		
		String priorProb = "\n";
		priorProb += "Intersect(" + n1 + "," + n2 + ") = " + numIntersec + "\n";
		priorProb += "#(" + n1 + ") = " + numCause + "\n";
		priorProb += "#(" + n2 + ") = " + numEffect + "\n";
		priorProb += "#(Universe) = " + total + "\n";
		return priorProb;
	}
	
	public void writeProbs (double minProb, int minIntersect, DataWriter dw) throws IOException {
		ArrayList<String> tmp = new ArrayList<String>();
		Enumeration<String> e = refs.keys();
		while (e.hasMoreElements()) {
			String elem = e.nextElement();
			if (singular(elem))
				tmp.add(elem);
		}
		
		for (int i = 0; i < tmp.size(); i++) {
			for (int j = i+1; j < tmp.size(); j++) {
				String n1 = tmp.get(i);
				String n2 = tmp.get(j);
				if (getIntersect(n1,n2) < minIntersect)
					continue;
				double p1 = getProbability(n1,n2);
				double p2 = getProbability(n2,n1);
				if (p1 < minProb && p2 < minProb)
					continue;
				String prob1 = df.format(p1);
				String prob2 = df.format(p2);
				String out = "deltaP[" + n2 + "|" + n1 + "] = " + getProbabilityStr(n1,n2) + " = " + prob2 +
					getExtraStr (n2, n1) + 
					"   \ndeltaP[" + n1 + "|" + n2 + "] = " + getProbabilityStr(n2,n1) + " = " + prob1 +
					getExtraStr (n1, n2) + "\n";
				dw.write(out + "\n");
				System.out.println(out);
			}
		}
	}
	
	/**
	 * Sorted by size of intersect
	 */
	public void writeProbs2 (double minProb, int minIntersect, DataWriter dw) 
		throws IOException {
		ArrayList<String> tmp = new ArrayList<String>();
		Enumeration<String> e = refs.keys();
		while (e.hasMoreElements()) {
			String elem = e.nextElement();
			if (singular(elem))
				tmp.add(elem);
		}
		Hashtable<Integer,StringBuilder> results = new Hashtable<Integer,StringBuilder>();
		
		for (int i = 0; i < tmp.size(); i++) {
			for (int j = i+1; j < tmp.size(); j++) {
				String n1 = tmp.get(i);
				String n2 = tmp.get(j);
				int intersect = getIntersect(n1,n2);
				if (intersect < minIntersect)
					continue;
				double p1 = getProbability(n1,n2);
				double p2 = getProbability(n2,n1);
				if (p1 < minProb && p2 < minProb)
					continue;
				String prob1 = df.format(p1);
				String prob2 = df.format(p2);
				String out = "deltaP[" + n2 + "|" + n1 + "] = " + getProbabilityStr(n1,n2) + 
				" = " + prob2 + getExtraStr (n2, n1) + "   \ndeltaP[" + n1 + "|" + n2 + 
				"] = " + getProbabilityStr(n2,n1) + " = " + prob1 + getExtraStr (n1, n2) + 
				"\n";
				if (results.containsKey(intersect))
					results.put(intersect, results.get(intersect).append(out));
				else {
					StringBuilder sb = new StringBuilder();
					sb.append(out);
					results.put(intersect, sb);
				}
			}
		}
		
		Integer[] cnts = new Integer[results.size()];
		Enumeration<Integer> e2 = results.keys();
		int i = 0;
		while (e2.hasMoreElements()) {
			cnts[i++] = e2.nextElement();
		}
		haus.util.sort.QuickSort.quicksort(cnts);
		for (i = cnts.length-1; i >= 0; i--) {
			int intersect = cnts[i];
			dw.write(results.get(intersect).toString() + "\n");
			System.out.println(results.get(intersect).toString());
		}
	}
	
	/**
	 * Sorted by ratio of intersect
	 */
	public void writeProbs3 (double minRatio, int minIntersect, DataWriter dw) 
		throws IOException {
	long t1 = System.currentTimeMillis();
	ArrayList<String> tmp = new ArrayList<String>();
	Enumeration<String> e = refs.keys();
	while (e.hasMoreElements()) {
		String elem = e.nextElement();
		if (singular(elem) && refs.get(elem) >= minIntersect)
			tmp.add(elem);
	}
	System.out.println("Read all strings out from Hashtable: " + (System.currentTimeMillis() - t1));
	t1 = System.currentTimeMillis();
	
	Hashtable<Double,StringBuilder> results = new Hashtable<Double,StringBuilder>();
	
	for (int i = 0; i < tmp.size(); i++) {
		for (int j = i+1; j < tmp.size(); j++) {
			String n1 = tmp.get(i);
			String n2 = tmp.get(j);
			int intersect = getIntersect(n1,n2);
			if (intersect < minIntersect)
				continue;
			double p1 = getProbability(n1,n2);
			double p2 = getProbability(n2,n1);
			double ratio = p1/p2;
			if (ratio < minRatio)
				continue;
			if (p1 < p2) ratio = p2/p1;
			String prob1 = df.format(p1);
			String prob2 = df.format(p2);
			String out = "deltaP[" + n2 + "|" + n1 + "] = " + getProbabilityStr(n1,n2) + 
			" = " + prob2 + getExtraStr (n2, n1) + "   deltaP[" + n1 + "|" + n2 + 
			"] = " + getProbabilityStr(n2,n1) + " = " + prob1 + getExtraStr (n1, n2) + 
			"Ratio: " + ratio + "\n\n";
			if (results.containsKey(ratio))
				results.put(ratio, results.get(ratio).append(out));
			else {
				StringBuilder sb = new StringBuilder();
				sb.append(out);
				results.put(ratio, sb);
			}
		}
	}
	
	System.out.println("Got All Pairs of Probabilities: " + (System.currentTimeMillis() - t1));
	t1 = System.currentTimeMillis();
	
	Double[] cnts = new Double[results.size()];
	Enumeration<Double> e2 = results.keys();
	int i = 0;
	while (e2.hasMoreElements()) {
		cnts[i++] = e2.nextElement();
	}
	haus.util.sort.QuickSort.quicksort(cnts);
	
	System.out.println("Finished Enumeration and Quicksort: " + (System.currentTimeMillis() - t1));
	t1 = System.currentTimeMillis();
	
	for (i = cnts.length-1; i >= 0; i--) {
		Double intersect = cnts[i];
		dw.write(results.get(intersect).toString());
	}
}
}

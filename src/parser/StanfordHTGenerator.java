package parser;

import haus.io.FileReader;

import java.util.ArrayList;
import java.util.Hashtable;

import mallet.Include;

import edu.stanford.nlp.trees.Tree;

/**
 * Generates a hashtable from our sentences and stores for Stanford parser
 */
public class StanfordHTGenerator {
	public static void main (String[] args) {
		//Hashtable<String[],Tree> ht = (Hashtable<String[], Tree>) haus.io.Serializer.deserialize("stanford/sentence_lookup");
		int count = 0;
		Hashtable<String[],Tree> ht = new Hashtable<String[],Tree>();
		StanfordParser sp = new StanfordParser();
		
		ArrayList<String> lines = FileReader.readFile("crf/crf_bare.txt");
		ArrayList<String> curr = new ArrayList<String>();
		for (String line : lines) {
			curr.add(Include.getToken(line));
			if (Include.hasSentDelim(line)) {
				String[] key = haus.misc.Conversions.toStrArray(curr);
				Tree t = sp.getParseTree(key);
				ht.put(key, t);
				curr.clear();
				System.out.println(count++);
			}
		}
		haus.io.Serializer.serialize(ht, "stanford/sentence_lookup");
	}
}

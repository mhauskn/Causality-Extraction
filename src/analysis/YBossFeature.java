package analysis;

import include.Include;
import haus.io.Serializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import yboss.YBoss;

/**
 * YBoss feature uses YahooBoss to find probable causes/effects inside of 
 * the sentence. It does this based on either nouns or noun phrases in the sentence.
 * For this reason it needs access to POS data or noun phrasing data. 
 */
public class YBossFeature {
	public static final String CAUSE_HIGH_PROB = "HighProbCause";
	public static final String EFFECT_HIGH_PROB = "HighProbEffect";
	public static final String LOW_PROB = "YBossLowProb";
	
	public static final int HIGH_THRESHOLD = 10;
	
	public static final String ht_out = "crf/bossHT";
	
	Hashtable<String,Long> storage = new Hashtable<String,Long>();
	Serializer<Hashtable<String,Long>> ser = new Serializer<Hashtable<String,Long>>();
	YBoss boss = new YBoss(storage);
	
	ArrayList<String> nounPhrases;
	ArrayList<Integer> indexStart;
	
	String[] tokens;
	String[] feature;
	
	phraseTracker pTracker;
	
	int missCount = 0;
	int missThreshold = 10;
	int sleepNum = 5000;
	
	/**
	 * Loads a Hashtable from a file
	 */
	public void deSerializeHT (String fileName) {
		storage = ser.deserialize(fileName);
		boss.setStorage(storage);
	}
	
	public void deSerializeHT () {
		storage = ser.deserialize(ht_out);
		boss.setStorage(storage);
	}
	
	/**
	 * Saves our hashtable
	 */
	public void serializeHT (String fileName) {
		ser.serialize(storage, fileName);
	}
	
	public void serializeHT () {
		ser.serialize(storage, ht_out);
	}
	
	/**
	 * Keeps track of our noun phrases as they grow
	 * and break
	 */
	class phraseTracker {
		String np = "";
				
		void add (int index) {
			np += tokens[index] + " ";
			indexStart.add(index);
		}
		
		void breakPhrase (int index) {
			if (np.length() <= 0)
				return;
			nounPhrases.add(np.substring(0, np.length() -1));
			np = "";
		}
	}
	
	/**
	 * Initializer for many of the fields
	 */
	void init (String[] _tokens) {
		tokens = _tokens;
		nounPhrases = new ArrayList<String>();
		indexStart = new ArrayList<Integer>();
		feature = new String[tokens.length];
		pTracker = new phraseTracker();
		for (int i = 0; i < feature.length; i++) 
			feature[i] = LOW_PROB;
	}
	
	/**
	 * Looks at POS tags for words and queries YBoss based on this data
	 */
	public String[] getPOSFeature (String[] _tokens, String[] pos_tags) {
		init(_tokens);
		for (int tagI = 0; tagI < pos_tags.length; tagI++) {
			String posTag = pos_tags[tagI];
			if (posTag.matches(Include.NOUN_REGEXP) || posTag.matches(Include.EXTENDED_NOUN_REGEXP))
				pTracker.add(tagI);
			else
				pTracker.breakPhrase(tagI);
		}
		pTracker.breakPhrase(pos_tags.length -1);
		queryNounPhrases();
		return feature;
	}
	
	/**
	 * Looks at stanford NP phrase data and queries Yboss based on this data.
	 * The queries are based on NPs rather than single nouns.
	 */
	public String[] getFeature (String[] _tokens, String[] np_tags) {
		init(_tokens);
		for (int i = 0; i < np_tags.length; i++) {
			String tag = np_tags[i];
			if (tag == null)
				continue;
			if (tag.equals(StanfordPhraseFinder.NP_START)) {
				pTracker.breakPhrase(i);
				pTracker.add(i);
			} else if (tag.equals(StanfordPhraseFinder.NP_MID)) {
				pTracker.add(i);
			} else if (tag.equals(StanfordPhraseFinder.NP_END)) {
				pTracker.add(i);
			} else {
				pTracker.breakPhrase(i);
			}
		}
		queryNounPhrases();
		return feature;
	}
	
	/**
	 * Runs through noun phrases and queries them via YBoss
	 */
	void queryNounPhrases () {
		long[] countI = new long[nounPhrases.size()];
		for (int i = 0; i < nounPhrases.size(); i++) {
			String np = nounPhrases.get(i);
			for (int j = 0; j < nounPhrases.size(); j++) {
				if (i == j) continue;
				String np2 = nounPhrases.get(j);
				missCount = 0;
				long hits = queryBoss(np,np2);
				if (hits < HIGH_THRESHOLD)
					continue;
				if (hits > countI[i]) {
					countI[i] = hits;
					int cstart = indexStart.get(i);
					int csize = np.split(" ").length;
					for (int s = cstart; s < cstart + csize; s++) 
						feature[s] = CAUSE_HIGH_PROB;
				} 
				if (hits > countI[j]) {
					countI[j] = hits;
					int estart = indexStart.get(j);
					int esize = np2.split(" ").length;
					for (int s = estart; s < estart + esize; s++) 
						feature[s] = EFFECT_HIGH_PROB;
				}
			}
		}
	}
	
	/**
	 * Queries boss.. Importantly when we get error code 503: sever 
	 * Unavailable then we will re-try up until a limit. If we fail
	 * we will save HT to keep our work.
	 */
	Long queryBoss (String np, String np2) {
		long hits;
		try {
			hits = boss.getHits(np, np2);
			return hits;
		} catch (IOException e) {
			if (missCount >= missThreshold) {
				serializeHT();
				System.exit(0);
			}
			e.printStackTrace();
			missCount++;
			try {
				Thread.sleep(sleepNum);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			return queryBoss(np,np2);
		}
	}
}

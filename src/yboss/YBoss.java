package yboss;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Hashtable;

public class YBoss {
	public static final String SAMPLE_QUERY = "http://boss.yahooapis.com/ysearch/web/v1/moon+cause+tide?appid=Dm6KYxTV34G2F_C4ipmra27.s2y9X9NEzIIBPDlIJ_.i4y9sK4hOQZG.LXAioePD0hXXWL4EQwa_&format=xml&start=0&count=0";
	public static final String BOSS_ID = "Dm6KYxTV34G2F_C4ipmra27.s2y9X9NEzIIBPDlIJ_.i4y9sK4hOQZG.LXAioePD0hXXWL4EQwa_";
	public static final String IN_TITLE = "intitle%3A";
	public static final String DIVIDER = "cause";
	
	boolean use_title = true;
	boolean elim_pairs = false;
	
	Dictionary<String,Long> data = null;
	
	public static final long universe = Long.MAX_VALUE;
	
	static long queryCount = 0;
	
	public YBoss (Dictionary<String,Long> _dict) {
		data = _dict;
	}
	
	/**
	 * Allows YBoss to change the default storage type
	 */
	public void setStorage (Dictionary<String,Long> storage) {
		data = storage;
	}
	
	/**
	 * Determines whether we search for the terms in the title of the page 
	 * or in the body.
	 */
	public void setTitleUsage (boolean new_usage) {
		use_title = new_usage;
	}
	
	/**
	 * If true this will eliminate frequently co-occuring word pairs such as 
	 * soviet and union
	 */
	public void setElimPairs (boolean _elim_pairs) {
		elim_pairs = _elim_pairs;
	}
	
	public static double getProb (double cause, double effect, double intersect) {
		return intersect/cause - (effect-intersect)/((double)universe-cause);
	}
	
	/**
	 * Queries via Yahoo boss for the number of hits 
	 * np1 causes np2
	 */
	public long getHits (String np1, String np2) throws IOException {
		String query = formulateQuery(np1,np2);
		Long hits;
		if ((hits = data.get(query)) != null)
			return hits;
		hits = getQueryNumHits(query);
		data.put(query, hits);
		return hits;
	}
	
	//-------PRIVATE METHODS---------//
	
	/**
	 * Creates a query for yahoo boss generally moon+causes+tide.
	 * Possible more complicated if in title desired.
	 */
	private String formulateQuery (String np1, String np2) {
		String query = "";
		String formattedNp1 = handleEscapeChars(np1);
		String formattedNp2 = handleEscapeChars(np2);
		String[] segs1 = formattedNp1.split("%20");
		String[] segs2 = formattedNp2.split("%20");
		for (int i = 0; i < segs1.length; i++) {
			if (use_title)
				query += IN_TITLE;
			query += segs1[i] + "+";
		}
		if (use_title) 
			query += IN_TITLE;
		query += DIVIDER + "+";
		for (int i = 0; i < segs2.length; i++) {
			if (use_title)
				query += IN_TITLE;
			query += segs2[i];
			if (i != segs2.length -1)
				query += "+";
		}
		if (elim_pairs) {
			query += "+-" + IN_TITLE + "\"" + formattedNp1 + "+" + formattedNp2 + "\"";
			query += "+-" + IN_TITLE + "\"" + formattedNp2 + "+" + formattedNp1 + "\"";
		}
		return query;
	}
	
	String handleEscapeChars (String np) {
		np = np.replaceAll("%", "%25");
		np = np.replaceAll(" ", "%20");
		np = np.replaceAll("\\$", "%24");
		np = np.replaceAll("&", "%26");
		np = np.replaceAll("`", "%60");
		np = np.replaceAll("'", "%27");
		np = np.replaceAll(":", "%3A");
		np = np.replaceAll("<", "%3C");
		np = np.replaceAll(">", "%3E");
		np = np.replaceAll("\\[", "%5B");
		np = np.replaceAll("]", "%5D");
		np = np.replaceAll("\\{", "%7B");
		np = np.replaceAll("}", "%7D");
		np = np.replaceAll("#", "%23");
		np = np.replaceAll("@", "%40");
		np = np.replaceAll("/", "%2F");
		np = np.replaceAll(";", "%3B");
		np = np.replaceAll("=", "%3D");
		np = np.replaceAll("\\?", "%3F");
		np = np.replaceAll("\\\\", "%5C");
		np = np.replaceAll("\\^", "%5E");
		np = np.replaceAll("\\|", "%7C");
		np = np.replaceAll("~", "%7E");
		return np;
	}
	
	/**
	 * Sends a query to boss and parses the number of pages returned
	 * @param query
	 * @return
	 * @throws IOException
	 */
	private long getQueryNumHits (String query) throws IOException {
		queryCount++;
		String res = queryBoss(query);
		return parseNumHits(res);
	}
	
	/**
	 * Parses the number of pages returned given a boss query result
	 */
	Long parseNumHits (String queryResult) {
		int start = queryResult.indexOf("totalhits=\"") + 11;
		if (start == -1) {
			System.err.println("Error in Yahoo boss query results: " + queryResult);
			return null;
		}
		int end = queryResult.indexOf("\"", start);
		String guess = queryResult.substring(start, end);
		return Long.parseLong(guess);
	}
	
	/**
	 * Sends a query to Yahoo Boss and returns the document boss retrieves for us
	 */
	private static String queryBoss (String query) throws IOException {
		URL bossURL = new URL("http://boss.yahooapis.com/ysearch/web/v1/" + query + 
				"?appid=" + BOSS_ID + "&format=xml&start=0&count=0");
		URLConnection urlc = bossURL.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
		String inputLine;
		String doc = "";
		while ((inputLine = in.readLine()) != null) 
            doc += inputLine;
        in.close();
        return doc;
	}
	
	public static void main(String[] args) {
		try {
			YBoss yb = new YBoss(new Hashtable<String,Long>());
			yb.setTitleUsage(true);
			System.out.println(yb.getHits("moon","tide"));
			System.out.println(yb.getHits("tide","moon"));
			
			System.out.println(yb.getHits("soviet","union"));
			System.out.println(yb.getHits("union","soviet"));
			System.out.println("Made " + queryCount + " queries");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}

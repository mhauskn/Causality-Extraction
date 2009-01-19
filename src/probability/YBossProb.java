package probability;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import mysql.Jdbc;

public class YBossProb {
	public static final String BOSS_ID = "Dm6KYxTV34G2F_C4ipmra27.s2y9X9NEzIIBPDlIJ_.i4y9sK4hOQZG.LXAioePD0hXXWL4EQwa_";
	public static final String HOSTNAME = "jdbc:mysql://localhost:3306";
	public static final String USERNAME = "epn";
	public static final String PASS = "";
	public static final String DBNAME = "yboss";
	public static final String TABLENAME = "data";
	public static final String COLNAME = "hits";
	
	public static final long universe = Long.MAX_VALUE;
	
	Jdbc db = null;
	static long queryCount = 0;
	
	public YBossProb () throws Exception {
		db = new Jdbc(HOSTNAME, USERNAME, PASS);
		db.selectDB(DBNAME);
		queryCount = 0;
	}
	
	public double getProbability (String n1, String n2) throws IOException {
		long h1 = getHits(n1);
		long h2 = getHits(n2);
		long intersect = getHits(n1,n2);
		return getProb((double)h1,(double)h2,(double)intersect);
	}
	
	public static double getProb (double cause, double effect, double intersect) {
		return intersect/cause - (effect-intersect)/((double)universe-cause);
	}
	
	public long getHits (String n1, String n2) throws IOException {
		n1 = n1.toLowerCase();
		n2 = n2.toLowerCase();
		String combo1 = n1 + "+" + n2;
		String combo2 = n2 + "+" + n1;
		long res1 = getHits(combo1);
		long res2 = getHits(combo2);
		return (res1 + res2) / 2;
	}
	
	public long getHits (String noun) throws IOException {
		noun = noun.toLowerCase();
		long hits = 0;
		//long hits = db.getHits(noun,TABLENAME,COLNAME);
		if (hits != -1)
			return hits;
		hits = getQueryNumHits(noun);
		//db.addHits(noun, hits);
		return hits;
	}
	
	private long getQueryNumHits (String query) throws IOException {
		queryCount++;
		String res = queryBoss(query);
		int start = res.indexOf("totalhits=\"") + 11;
		if (start == -1) {
			System.out.println("Error in Yahoo boss query results for query " + query);
			throw new IOException();
		}
		int end = res.indexOf("\"", start);
		String guess = res.substring(start, end);
		return Long.parseLong(guess);
	}
	
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
			YBossProb yb = new YBossProb();
			System.out.println(yb.getProbability("moon","tide"));
			System.out.println(yb.getProbability("tide","moon"));
			
			System.out.println(yb.getProbability("soviet","union"));
			System.out.println(yb.getProbability("union","soviet"));
			System.out.println("Made " + queryCount + " queries");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}

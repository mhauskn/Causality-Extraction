package mallet;

import java.util.ArrayList;

import haus.io.DataWriter;
import haus.io.FileReader;
import parser.StanfordParser;
import pos.PosTagger;
import analysis.AdjacentFeature;
import analysis.ChunkFeature;
import analysis.HypernymFeature;
import analysis.StanfordPhraseFinder;
import analysis.StemFeature;
import analysis.YBossFeature;
import chunker.AbstractPhraseChunker;


/**
 * Formats input in a manner readable to our crf training program.
 * Generally input is expected to come from a turk formatter.
 * We just add the features to the labels.
 */
public class CRFFormatter {
	public static final String file_in = "crf/crf_bare.txt";
	public static final String file_out = "crf/crf.txt";
	
	//PosTagger posFeat = 		new PosTagger(include.Include.hmmFile);
	//ChunkFeature chunkFeat = 		new ChunkFeature(new AbstractPhraseChunker(include.Include.hmmFile));
	//HypernymFeature hf = 	new HypernymFeature();
	//StanfordPhraseFinder stanFeat = new StanfordPhraseFinder(new StanfordParser(include.Include.pcfgPath));
	StemFeature stemFeat = new StemFeature();
	
	DataWriter writer = new DataWriter(file_out);
	
	/**
	 * Simplified version
	 */
	public void writeData (ArrayList<String> tokens, ArrayList<String> labels) {
		String[] tokensArr = new String[tokens.size()];
		tokensArr = tokens.toArray(tokensArr);
		String[] labelsArr = new String[labels.size()];
		labelsArr = labels.toArray(labelsArr);
		writeData(tokensArr,labelsArr);
	}
	
	/**
	 * Given the tokens in a sentence and the token classes (labels) this method will
	 * add the features and then write the end of sentence delim
	 */
	public void writeData (String[] tokens, String[] labels) {
		String[] features = new String[tokens.length];
		for (int i = 0; i < features.length; i++) features[i] = "";
		//String[] chunk_tags = chunkFeat.getFeature(tokens);
		//String[] pos_tags = posFeat.getTags(tokens);
		//String[] stanford_tags = stanFeat.getFeature(tokens);
		addFeature(features, stemFeat.getFeature(tokens));
		
		//addFeature(features, chunk_tags);
		//addFeature(features, pos_tags);
		//addFeature(features, stanford_tags);
		//addFeature(features, boss_tags);
		
		for (int i = 0; i < tokens.length; i++) {
			if (features[i] == null)
				features[i] = "";
			writer.write(tokens[i] + " " + features[i] + labels[i] + "\n");
		}
	}
	
	/**
	 * Adds new feature to the old feature
	 */
	public static void addFeature (String[] features, String[] newFeature) {
		for (int i = 0; i < features.length; i++) {
			features[i] += newFeature[i] + " ";
		}
	}
	
	public void close () {
		writer.close();
		//bossFeat.serializeHT();
	}
	
	public static void combineFeatures (String feat1, String feat2, String out) {
		FileReader r1 = new FileReader(feat1);
		FileReader r2 = new FileReader(feat2);
		DataWriter wri = new DataWriter(out);
		String l1 = r1.getNextLine(), l2 = r2.getNextLine();
		int count = 0;
		while (l1 != null && l2 != null) {
			count++;
			int toki = l1.indexOf(' ');
			int labi = l1.lastIndexOf(' ');
			int toki2 = l2.indexOf(' ');
			int labi2 = l2.lastIndexOf(' ');
			if (toki != toki2) {
				System.out.println("Problem in mathcing of strings." + l1 + " " + l2 + " " + count);
				System.exit(0);
			}
			String tok = l1.substring(0, toki);
			String label = l1.substring(labi, l1.length());
			String mid = l1.substring(toki, labi);
			String mid2 = l2.substring(toki2, labi2);
			wri.write(tok + mid + mid2 + label + "\n");
			l1 = r1.getNextLine();
			l2 = r2.getNextLine();
		}
		if ((l1 != null && l2 == null) || (l2 != null && l1 == null)) {
			System.out.println("Wrong number of lines in files");
			System.exit(1);
		}
		wri.close();
	}
	
	/**
	 * Reads a CRF formatted file scanning for the tokens, features,
	 * and labels.
	 * 
	 * Format should be like 
	 * token feat1....featn label
	 * 
	 * The data is returned inside of the arraylists passed.
	 */
	public static void readCRFFile (String file_name, 
			ArrayList<String> toks, ArrayList<String> features,
			ArrayList<String> labels) {
		FileReader reader = new FileReader(file_name);
		String line;
		while ((line = reader.getNextLine()) != null) {
			String[] segs = line.split(" ");
			toks.add(segs[0]);
			String feat = "";
			for (int i = 1; i < segs.length-1; i++)
				feat += segs[i];
			features.add(feat);
			labels.add(segs[segs.length-1]);
		}
	}
	
	/**
	 * Writes a CRF readable file given the tokens, features and labels
	 * which are needed. Spaces are added between these three classes
	 * so don't add them on your own!
	 * 
	 * @param file_name The name of the file to be written
	 * @param toks The tokens given to the CRF
	 * @param features the features
	 * @param labels and the class labels used by CRF
	 */
	public static void writeCRFFile (String file_name,
			ArrayList<String> toks, ArrayList<String> features,
			ArrayList<String> labels) {
		DataWriter writer = new DataWriter(file_name);
		for (int i = 0; i < toks.size(); i++)
			writer.writeln(toks.get(i) + " " + features.get(i) + " " + labels.get(i));
		writer.close();
	}
	
	public static void main (String[] args) {
		CRFFormatter formatter = new CRFFormatter();
		FileReader reader = new FileReader(file_in);
		ArrayList<String> words = new ArrayList<String>();
		ArrayList<String> labels = new ArrayList<String>();
		String line;
		while ((line = reader.getNextLine()) != null) {
			String[] segs = line.split(" ");
			words.add(segs[0]);
			labels.add(segs[1]);
			if (line.equals(Include.SENT_DELIM)) {
				formatter.writeData(words,labels);
				words.clear();
				labels.clear();
			}
		}
		formatter.close();
	}
}

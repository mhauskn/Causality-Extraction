package wordnet;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import edu.mit.jwi.morph.SimpleStemmer;

public class JWI {
	static final String WN_PATH = "/usr/local/WordNet-3.0/dict";
	IDictionary dict = null;
	SimpleStemmer ss = null;
	
	public JWI () {
		initDic();
		ss = new SimpleStemmer();
	}
	
	private void initDic () {
		try {
			URL url = new URL("file", null, WN_PATH);
			dict = new Dictionary(url);
			dict.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void getSynonyms (IDictionary dict, String word) {
		IIndexWord idxWord = dict.getIndexWord(word, POS.NOUN);
		IWordID wordID = idxWord.getWordIDs().get(0);
		IWord iWord = dict.getWord(wordID);
		ISynset synset = iWord.getSynset();
		
		for (IWord w: synset.getWords())
			System.out.println(w.getLemma());
	}
	
	public ArrayList<String> getNounHypernyms (String desiredWord) {
		IIndexWord idxWord = dict.getIndexWord(desiredWord, POS.NOUN);
		if (idxWord != null)
			return getHypernyms(idxWord);
		
		List<String> strs = ss.findStems(desiredWord);
		for (String s : strs) {
			idxWord = dict.getIndexWord(s, POS.NOUN);
			if (idxWord != null)
				return getHypernyms(idxWord);
		}
		return null;
	}
	
	public ArrayList<String> getVerbHypernyms (String desiredWord) {
		IIndexWord idxWord = dict.getIndexWord(desiredWord, POS.VERB);
		if (idxWord != null)
			return getHypernyms(idxWord);
		
		List<String> strs = ss.findStems(desiredWord);
		for (String s : strs) {
			idxWord = dict.getIndexWord(s, POS.VERB);
			if (idxWord != null)
				return getHypernyms(idxWord);
		}
		return null;
	}
	
	private ArrayList<String> getHypernyms (IIndexWord idxWord) {
		if (idxWord == null)
			return null;
		IWordID wordID = idxWord.getWordIDs().get(0); // 1st meaning
		IWord word = dict.getWord(wordID);
		return getHypernyms(word);
	}
	
	private ArrayList<String> getHypernyms (IWord iWord) {
		ArrayList<String> out = new ArrayList<String>();
		IWord word = iWord;
		ISynset synset = word.getSynset();
		List<ISynsetID> hypernyms = synset.getRelatedSynsets(Pointer.HYPERNYM);
		List<IWord> words;
		for(ISynsetID sid : hypernyms){
			words = dict.getSynset(sid).getWords();
			IWord fw = null;
			for(Iterator<IWord> i = words.iterator(); i.hasNext();){
				IWord iw = i.next();
				if (fw == null)
					fw = iw;
				out.add(iw.getLemma());
			}
			if (!fw.getLemma().equals("restrain") && !fw.getLemma().equals("inhibit"))
				out.addAll(getHypernyms(fw));
		}
		return out;
	}
	
	public void testDictionary (IDictionary dict) {
		IIndexWord idxWord = dict.getIndexWord("dog", POS.NOUN);
		IWordID wordID = idxWord.getWordIDs().get(0);
		IWord word = dict.getWord(wordID);
		System.out.println("ID = " + wordID);
		System.out.println("Lemma = " + word.getLemma());
		System.out.println("Gloss = " + word.getSynset().getGloss());
	}
}

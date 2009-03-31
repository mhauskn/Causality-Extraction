package analysis.postFeatures;

import java.util.ArrayList;

import edu.stanford.nlp.trees.Tree;
import parser.Stanford.StanfordParser;
import parser.Stanford.TreeOps;
import analysis.PostFeature;
import analysis.postFeatures.RelnTypes.BiPhrasalReln;
import analysis.postFeatures.RelnTypes.TypeReln;
import analysis.postFeatures.RelnTypes.VerbBasedReln;

/**
 * Finds syntactic dependencies between our Relation (Cue Phrase) and 
 * the rest of the sentence.
 */
public class RelnDep implements PostFeature {
	public static final String NP_OR_S = "(NP|S|WHNP)";
	
	public static final String neg_conn = "no_verbal_reln";
	
	StanfordParser sp;
	
	ArrayList<TypeReln> relns = new ArrayList<TypeReln>();
	
	Tree t;
	Tree[] leaves;
	
	String[] toks;
	String[] feats;
	
	public RelnDep (StanfordParser parser) {
		sp = parser;
		relns.add(new BiPhrasalReln());
		relns.add(new VerbBasedReln());
	}
	
	/**
	 * 1. Determine if Cue Phrase is a verb
	 */
	public String[] getFeature(String[] tokens, String[] features) {
		toks = tokens;
		feats = features;
		
		t = sp.getParseTree(tokens);
		leaves = TreeOps.getWordIndexedTree(t);
		
		double best_conf = -1.0;
		TypeReln best_reln = null;
		for (TypeReln reln : relns) {
			double conf = reln.computeConfidence(toks, feats, t, leaves, sp);
			if (conf > best_conf) {
				best_conf = conf;
				best_reln = reln;
			}
		}
		return best_reln.getResults();
	}
	
	/**
	 * Oh Shit! Abandon Ship! Print the sentence so we can an 
	 * in-depth exam of why this happened.
	 */
	void abandon () {
		for (String s : toks)
			System.out.print(s + " ");
		System.out.println("");
		//System.exit(1);
	}
}

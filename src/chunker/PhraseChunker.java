package chunker;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.ChunkFactory;
import com.aliasi.chunk.Chunking;
import com.aliasi.chunk.ChunkingImpl;

import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;

import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;

import com.aliasi.util.AbstractExternalizable;
import com.aliasi.util.FastCache;
import com.aliasi.util.Strings;



import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Expects to chunk 1 sentence at a time.
 */
public class PhraseChunker implements Chunker {

    // Determiners & Numerals
    // ABN, ABX, AP, AP$, AT, CD, CD$, DT, DT$, DTI, DTS, DTX, OD

    // Adjectives
    // JJ, JJ$, JJR, JJS, JJT

    // Nouns
    // NN, NN$, NNS, NNS$, NP, NP$, NPS, NPS$

    // Adverbs
    // RB, RB$, RBR, RBT, RN (not RP, the particle adverb)

    // Pronoun
    // PN, PN$, PP$, PP$$, PPL, PPLS, PPO, PPS, PPSS

    // Verbs
    // VB, VBD, VBG, VBN, VBZ

    // Auxiliaries
    // MD, BE, BED, BEDZ, BEG, BEM, BEN, BER, BEZ

    // Adverbs
    // RB, RB$, RBR, RBT, RN (not RP, the particle adverb)


    // Punctuation
    // ', ``, '', ., (, ), *, --, :, ,

    static final Set<String> DETERMINER_TAGS = new HashSet<String>();
    static final Set<String> ADJECTIVE_TAGS = new HashSet<String>();
    static final Set<String> NOUN_TAGS = new HashSet<String>();
    static final Set<String> PRONOUN_TAGS = new HashSet<String>();

    static final Set<String> ADVERB_TAGS = new HashSet<String>();

    static final Set<String> VERB_TAGS = new HashSet<String>();
    static final Set<String> AUXILIARY_VERB_TAGS = new HashSet<String>();

    static final Set<String> PUNCTUATION_TAGS = new HashSet<String>();

    static final Set<String> START_VERB_TAGS = new HashSet<String>();
    static final Set<String> CONTINUE_VERB_TAGS = new HashSet<String>();

    static final Set<String> START_NOUN_TAGS = new HashSet<String>();
    static final Set<String> CONTINUE_NOUN_TAGS = new HashSet<String>();

    static {
        DETERMINER_TAGS.add("abn");
        DETERMINER_TAGS.add("abx");
        DETERMINER_TAGS.add("ap");
        DETERMINER_TAGS.add("ap$");
        DETERMINER_TAGS.add("at");
        DETERMINER_TAGS.add("cd");
        DETERMINER_TAGS.add("cd$");
        DETERMINER_TAGS.add("dt");
        DETERMINER_TAGS.add("dt$");
        DETERMINER_TAGS.add("dti");
        DETERMINER_TAGS.add("dts");
        DETERMINER_TAGS.add("dtx");
        DETERMINER_TAGS.add("od");

        ADJECTIVE_TAGS.add("jj");
        ADJECTIVE_TAGS.add("jj$");
        ADJECTIVE_TAGS.add("jjr");
        ADJECTIVE_TAGS.add("jjs");
        ADJECTIVE_TAGS.add("jjt");

        NOUN_TAGS.add("nn");
        NOUN_TAGS.add("nn$");
        NOUN_TAGS.add("nns");
        NOUN_TAGS.add("nns$");
        NOUN_TAGS.add("np");
        NOUN_TAGS.add("np$");
        NOUN_TAGS.add("nps");
        NOUN_TAGS.add("nps$");
        NOUN_TAGS.add("nr");
        NOUN_TAGS.add("nr$");
        NOUN_TAGS.add("nrs");

        PRONOUN_TAGS.add("pn");
        PRONOUN_TAGS.add("pn$");
        PRONOUN_TAGS.add("pp$");
        PRONOUN_TAGS.add("pp$$");
        PRONOUN_TAGS.add("ppl");
        PRONOUN_TAGS.add("ppls");
        PRONOUN_TAGS.add("ppo");
        PRONOUN_TAGS.add("pps");
        PRONOUN_TAGS.add("ppss");

        VERB_TAGS.add("vb");
        VERB_TAGS.add("vbd");
        VERB_TAGS.add("vbg");
        VERB_TAGS.add("vbn");
        VERB_TAGS.add("vbz");

        AUXILIARY_VERB_TAGS.add("to");
        AUXILIARY_VERB_TAGS.add("md");
        AUXILIARY_VERB_TAGS.add("be");
        AUXILIARY_VERB_TAGS.add("bed");
        AUXILIARY_VERB_TAGS.add("bedz");
        AUXILIARY_VERB_TAGS.add("beg");
        AUXILIARY_VERB_TAGS.add("bem");
        AUXILIARY_VERB_TAGS.add("ben");
        AUXILIARY_VERB_TAGS.add("ber");
        AUXILIARY_VERB_TAGS.add("bez");
        AUXILIARY_VERB_TAGS.add("*");  // negation

        ADVERB_TAGS.add("rb");
        ADVERB_TAGS.add("rb$");
        ADVERB_TAGS.add("rbr");
        ADVERB_TAGS.add("rbt");
        ADVERB_TAGS.add("rn");

        PUNCTUATION_TAGS.add("'");
        // PUNCTUATION_TAGS.add("``");
        // PUNCTUATION_TAGS.add("''");
        PUNCTUATION_TAGS.add(".");
        // PUNCTUATION_TAGS.add(","); // miss comma-separated phrases
        // PUNCTUATION_TAGS.add("(");
        // PUNCTUATION_TAGS.add(")");
        PUNCTUATION_TAGS.add("*");
        // PUNCTUATION_TAGS.add("--");
        // PUNCTUATION_TAGS.add(":");
    }

    static {

        START_NOUN_TAGS.addAll(DETERMINER_TAGS);
        START_NOUN_TAGS.addAll(ADJECTIVE_TAGS);
        START_NOUN_TAGS.addAll(NOUN_TAGS);
        START_NOUN_TAGS.addAll(PRONOUN_TAGS);

        CONTINUE_NOUN_TAGS.addAll(START_NOUN_TAGS);
        CONTINUE_NOUN_TAGS.addAll(ADVERB_TAGS);
        CONTINUE_NOUN_TAGS.addAll(PUNCTUATION_TAGS);

        START_VERB_TAGS.addAll(VERB_TAGS);
        START_VERB_TAGS.addAll(AUXILIARY_VERB_TAGS);
        START_VERB_TAGS.addAll(ADVERB_TAGS);

        CONTINUE_VERB_TAGS.addAll(START_VERB_TAGS);
        CONTINUE_VERB_TAGS.addAll(PUNCTUATION_TAGS);
    }

    private final HmmDecoder mPosTagger;
    private final TokenizerFactory mTokenizerFactory;

    public PhraseChunker(HmmDecoder posTagger,
                         TokenizerFactory tokenizerFactory) {
        mPosTagger = posTagger;
        mTokenizerFactory = tokenizerFactory;
    }

    public Chunking chunk(CharSequence cSeq) {
        char[] cs = Strings.toCharArray(cSeq);
        return chunk(cs,0,cs.length);
    }

    public Chunking chunk(char[] cs, int start, int end) {

        // tokenize
        List<String> tokenList = new ArrayList<String>();
        List<String> whiteList = new ArrayList<String>();
        Tokenizer tokenizer = mTokenizerFactory.tokenizer(cs,start,end-start);
        tokenizer.tokenize(tokenList,whiteList);
        String[] tokens
            = tokenList.<String>toArray(new String[tokenList.size()]);
        String[] whites
            = whiteList.<String>toArray(new String[whiteList.size()]);

        // part-of-speech tag
        String[] tags = mPosTagger.firstBest(tokens);

        ChunkingImpl chunking = new ChunkingImpl(cs,start,end);
        int startChunk = 0;
        for (int i = 0; i < tags.length; ) {
            startChunk += whites[i].length();
            if (START_NOUN_TAGS.contains(tags[i])) {
                int endChunk = startChunk + tokens[i].length();
                ++i;
                while (i < tokens.length && CONTINUE_NOUN_TAGS.contains(tags[i])) {
                    endChunk += whites[i].length() + tokens[i].length();
                    ++i;
                }
                int trimmedEndChunk = endChunk;
                for (int k = i;
                     PUNCTUATION_TAGS.contains(tags[--k]);
                     trimmedEndChunk -= (whites[k].length() + tokens[k].length())) ;
                if (startChunk == trimmedEndChunk) continue;
                Chunk chunk
                    = ChunkFactory.createChunk(startChunk,trimmedEndChunk,"noun");
                chunking.add(chunk);
                startChunk = endChunk;
            } else if (START_VERB_TAGS.contains(tags[i])) {
                int endChunk = startChunk + tokens[i].length();
                ++i;
                while (i < tokens.length && CONTINUE_VERB_TAGS.contains(tags[i])) {
                    endChunk += whites[i].length() + tokens[i].length();
                    ++i;
                }
                int trimmedEndChunk = endChunk;
                //for (int k = i; 
               // 	 PUNCTUATION_TAGS.contains(tags[--k]);
                //     endChunk -= (whites[k].length() + tokens[k].length())) ;
                
               // for (int k = i; k >= 0 && PUNCTUATION_TAGS.contains(tags[k]); k--)
               // 	endChunk -= (whites[k].length() + tokens[k].length());
                
                for (int k = i; k >= 0; k--)
                {
                	if (k >= tags.length) k = tags.length -1;
                	if (!PUNCTUATION_TAGS.contains(tags[k]))
                		break;
                	
                	endChunk -= (whites[k].length() + tokens[k].length());
                }

                if (startChunk == trimmedEndChunk) continue;
                Chunk chunk = ChunkFactory.createChunk(startChunk,trimmedEndChunk,"verb");
                chunking.add(chunk);
                startChunk = endChunk;
            } else {
                startChunk += tokens[i].length();
                ++i;
            }
        }
        return chunking;
    }

    public static void main(String[] args) {

        // parse input params
        File hmmFile = new File(args[0]);
        int cacheSize = Integer.parseInt(args[1]);
        FastCache<String,double[]> cache = new FastCache<String,double[]>(cacheSize);

        // read HMM for pos tagging
        HiddenMarkovModel posHmm;
        try {
            posHmm
                = (HiddenMarkovModel)
                AbstractExternalizable.readObject(hmmFile);
        } catch (IOException e) {
            System.out.println("Exception reading model=" + e);
            e.printStackTrace(System.out);
            return;
        } catch (ClassNotFoundException e) {
            System.out.println("Exception reading model=" + e);
            e.printStackTrace(System.out);
            return;
        }

        // construct chunker
        HmmDecoder posTagger  = new HmmDecoder(posHmm,null,cache);
        TokenizerFactory tokenizerFactory = new IndoEuropeanTokenizerFactory();
        PhraseChunker chunker = new PhraseChunker(posTagger,tokenizerFactory);

        // apply chunker
        for (int i = 2; i < args.length; ++i) {
            Chunking chunking = chunker.chunk(args[i]);
            CharSequence cs = chunking.charSequence();
            System.out.println("\n" + cs);
            for (Chunk chunk : chunking.chunkSet()) {
                String type = chunk.type();
                int start = chunk.start();
                int end = chunk.end();
                CharSequence text = cs.subSequence(start,end);
                System.out.println("  " + type + "(" + start + "," + end + ") " + text);
            }
        }

    }

}
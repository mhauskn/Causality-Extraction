package detector;

import chunker.AbstractPhraseChunker;
import chunker.SentenceChunker;
import parser.TrecParser;
import haus.io.DataWriter;
import haus.io.FileReader;

import com.aliasi.chunk.Chunking;

import java.util.ArrayList;


public class ChunkCauseExtractor 
{
	public static final String DELIM = " --> ";
	
	private static String dataPath = ".";
	private static boolean recursiveSearch = true;
	private static String fileTypeFilter = "";
	private static String hmmFile = "";
	private static String outFile = "out.txt";
	private static ArrayList<String> ignoreExtensions = new ArrayList<String>();
		
	
	public static void main (String[] args)
	{
		if (!handleArgs(args))
		{
			usage();
			System.exit(1);
		}

		FileReader dr = new FileReader(dataPath);
		dr.setRecursive(recursiveSearch);
		dr.setFilterExtension(fileTypeFilter);
		for (String s : ignoreExtensions)
			dr.addIgnoreExtensions(s);
		TrecParser t = new TrecParser (dr);
		AbstractPhraseChunker ac = new AbstractPhraseChunker(hmmFile);
		SentenceChunker sc = new SentenceChunker(t);
		DataWriter dw = new DataWriter(outFile);
				
		long startTime = System.currentTimeMillis();
		String sentence;
		
		while ((sentence = sc.getNextSentence()) != null)
		{
			if (CausalRelationDetector.containsCausalWord(sentence))
			{
				Chunking ck = ac.chunkSentence(sentence);
				if (CausalRelationDetector.containsCausalRelation(ck))
				{		
					dw.write(sentence.trim() + "\n");
					//String[] triple = CausalRelationDetector.getCausalTriple(ck);
					//System.out.println(triple[0]+" DELIM "+triple[1]+" DELIM "+triple[2]+"\n");
					//dw.write(triple[0]+DELIM+triple[1]+DELIM+triple[2]+"\n");
				}
			}
		}
		
		dw.close();
		long taken = System.currentTimeMillis() - startTime;
		System.out.println("Finished: " + taken + "ms");
		System.out.println("Read " + sc.getSentenceCount() + " sentences.");
		System.out.println("Identified " + CausalRelationDetector.getInteresting() +
				" sentences containg the word cause and accepted " + 
				CausalRelationDetector.getAccepted() + " of them.");
		System.out.println("Output written to file " + outFile);
    }
	
	private static void usage ()
	{
		String usage = "";
		
		usage += "Usage: BasicExtractor -data <datapath> -hmm <hmmPath>\n";
		usage += "\n";
		usage += "Optional Generic Args:\n";
		usage += "	-out <outFileName>: File to write output to\n";
		usage += "\n";
		usage += "Optional Corpus Args:\n";
		usage += "	-recursive: Recursively search data Path for files\n";
		usage += "	-norecursive: Do not recursively search data Path for files\n";
		usage += "	-filterFileType <extension>: Read only files with extension <extension>\n";
		usage += "	-ignore <extension>: Ignore all files with extension <extension>\n";
		usage += "\n";
		usage += "\n";
		System.out.println(usage);
	}
	
	private static boolean handleArgs (String[] args)
    {
		int i = 0;
        String arg;
        boolean haveData = false;
        boolean haveHmm = false;
        
        
        while (i < args.length && args[i].startsWith("-")) {
            arg = args[i++];

	
            if (arg.equals("-data")) {
                if (i < args.length) {
                    dataPath = args[i++];
                    haveData = true;
                    continue;
                } else
                    System.err.println("-data requires a filename");
            }
            
            else if (arg.equals("-hmm")) {
                if (i < args.length) {
                    hmmFile = args[i++];
                    haveHmm = true;
                    continue;
                } else
                    System.err.println("-hmm requires a filename");
            }
            
            else if (arg.equals("-out")) {
                if (i < args.length) {
                    outFile = args[i++];
                    continue;
                } else
                    System.err.println("-out requires a filename");
            }
            
            else if (arg.equals("-ignore")) {
            	while (i+1 < args.length && !args[i+1].startsWith("-"))
            	{
            		ignoreExtensions.add(args[i++]);
            	}
                if (i < args.length) {
                    ignoreExtensions.add(args[i++]);
                	continue;
            	} else
                    System.err.println("-ignore requires a extension");
            }
            
            else if (arg.equals("-recursive")) {
                System.out.println("Doing Recursive Explore");
                recursiveSearch = true;
                continue;
            }
            
            else if (arg.equals("-filterFileType")) {
                if (i < args.length) {
                    fileTypeFilter = args[i++];
                    continue;
                } else
                    System.err.println("-filterFileType requires an extension");
            }
            
            else 
            {
            	System.err.println("Invalid Option " + arg + " specified.");
            	usage();
            	System.exit(1);
            }
        }
        return (haveData && haveHmm);
    }
}

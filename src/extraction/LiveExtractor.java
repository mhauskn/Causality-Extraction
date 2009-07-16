package extraction;

import java.util.ArrayList;

import parser.Stanford.InteractiveStanfordParser;

import haus.io.IO;
import haus.io.InteractiveReader;
import haus.io.Pipe;
import haus.io.classes.IOQueue;
import haus.misc.Map;
import turk.writers.CRFToHuman;
import turk.writers.HumanToCRF;

/**
 * Performs Extraction of CP/EP when give CCPs
 * in real time.
 */
public class LiveExtractor implements Map<String> {
	@SuppressWarnings("unchecked")
	ArrayList<IO> nodes = new ArrayList<IO>();
	
	IOQueue<String> in = new IOQueue<String>();
	IOQueue<String> out = new IOQueue<String>();
	
	public LiveExtractor () {
		@SuppressWarnings("unchecked")
		ArrayList<Pipe> pipes = new ArrayList<Pipe>();
		
		CRFToHuman crf2h = new CRFToHuman();
		crf2h.setInputFormat(new CpEpExtractor.OutputFormatter());
		
		pipes.add(in);
		nodes.add(new HumanToCRF());
		nodes.add(new CpEpExtractor());
		nodes.add(crf2h);
		for (int i = 0; i < nodes.size()-1; i++)
			pipes.add(new IOQueue<String>());
		pipes.add(out);
		
		IO.cascadeInput(nodes, pipes);
		
		InteractiveReader ir = new InteractiveReader(this);
		ir.run();
	}

	public void map (String arg0) {
		InteractiveStanfordParser.parseSentenceTest(InteractiveStanfordParser.detagSentence(arg0.split(" ")));
		in.add(arg0);
		for (IO<String,String> node : nodes)
			node.run();
		print();
	}
	
	void print () {
		String line;
		while ((line = out.get()) != null)
			System.out.println(line);
	}
	
	public static void main (String[] args) {
		new LiveExtractor();
	}
}

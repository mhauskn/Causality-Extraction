package stream;

import haus.io.Pipe;

import java.util.ArrayList;

import stream.Templates.CRFDataStreamTemplate;

public abstract class CRFDataStream implements CRFDataStreamTemplate {

	public String[] getFeatureArray(String line) {
		return getFeatures(line).split(" ");
	}

	/**
	 * Parses a block of CRF format separating it into constituent
	 * parts.
	 */
	public Container parseBlock(Pipe<String> in) {
		Container c = new Container();
		ArrayList<String> toks = new ArrayList<String>();
		ArrayList<String> feats = new ArrayList<String>();
		ArrayList<String> labels = new ArrayList<String>();
		ArrayList<String> output = new ArrayList<String>();
		
		String line;
		while ((line = in.get()) != null) {
			toks.add(getToken(line));
			feats.add(getFeatures(line));
			labels.add(getLabel(line));
			output.add(getOutput(line));
			
			if (containsDelim(line))
				break;
		}
		c.toks = haus.misc.Conversions.toStrArray(toks);
		c.feats = haus.misc.Conversions.toStrArray(feats);
		c.labels = haus.misc.Conversions.toStrArray(labels);
		c.output = haus.misc.Conversions.toStrArray(output);
		return c;
	}
	
	public static class Container {
		public String[] toks, feats, labels, output;
	}
	
	public boolean containsLabel (String label, String labelClass) {
		return (label.startsWith(labelClass));
	}
}

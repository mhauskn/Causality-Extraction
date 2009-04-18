package turk.writers;

import stream.CRFDataStream.Container;
import stream.Templates.CRFDataStreamTemplate;
import stream.Templates.HumanDataStreamTemplate;
import haus.io.formatted.FormattedIO;

/**
 * Converts CRF Format:
 * 
 * C the
 * C dog
 * E increased
 * E the 
 * E walk.
 * 
 * to human format:
 * 
 * [the dog]C [increased the walk.]E
 * 
 */
public class CRFToHuman extends FormattedIO<String,String,CRFDataStreamTemplate,HumanDataStreamTemplate> {
	String[] toks;
	String[] output;
	
	public void run () {
		Container c = context.parseBlock(in);
		toks = c.toks.clone();
		output = c.output.clone();
		
		for (String labelClass : context.getLabelClasses())
			tagBoundaries(labelClass);
		
		String outStr = "";
		for (String tok : toks)
			outStr += tok + " ";
		out.add(outStr);
	}
	
	void tagBoundaries (String labelClass) {
		boolean active = false;
		for (int i = 0; i < toks.length; i++) {
			String out = output[i];
			if (context.containsLabel(out, labelClass)) {
				if (!active) {
					toks[i] = "[" + toks[i];
					active = true;
				}
			} else {
				if (active)
					toks[i-1] = toks[i-1] + "]" + labelClass;
				active = false;
			}
		}
		if (active)
			toks[toks.length-1] = toks[toks.length-1] + "]" + labelClass;
	}

	// Required Methods
	
	public void mapInput (String _in) {}
	
	public HumanDataStreamTemplate provideOutputFormat() {
		return null;
	}
}

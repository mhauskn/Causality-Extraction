package turk;

import haus.io.DataWriter;
import haus.io.FileReader;

/**
 * Converts a file meant for input into a CRF into a human readable
 * version.
 * 
 * File format for CRF:
 * <tok> <feats> <label>
 * 
 * File format for human readable:
 * The [storm]C caused the [flood.]E
 *
 */
public class turkCrfToHuman {
	public static final String in_file = "turk/crfOut.txt";
	public static final String out_file = "turk/readableCRF.txt";
	
	public static final String CRF_INPUT_FORMAT = "CRFINPUTFORM";
	public static final String CRF_OUTPUT_FORMAT = "CRFOUTPUTFORM";
	
	FileReader reader = new FileReader(in_file);
	DataWriter writer = new DataWriter(out_file);
	
	String formatType = CRF_INPUT_FORMAT;
	
	String[] tokens;
	
	/**
	 * Changes the format we work with.
	 * 
	 * This is because when we test CRF it returns files of the form:
	 * <label> <features>
	 * 
	 * Rather than the input format:
	 * <token> <features> <label>
	 * 
	 */
	public void setFormat (String format) {
		if (!format.equals(CRF_INPUT_FORMAT) && !format.equals(CRF_OUTPUT_FORMAT)) {
			System.out.println("Invalid Format Specified");
			return;
		}
		formatType = format;
	}
	
	public void setInfile (String infile) {
		reader.setFile(infile);
	}
	
	public void setOutfile (String outfile) {
		writer.changeFile(outfile);
	}
	
	public void setTokenInput (String[] _tokens) {
		tokens = _tokens;
	}
	
	public void translate () {
		String line; 
		int linenum = 0;
		String out = "";
		while ((line = reader.getNextLine()) != null) {
			if (line.indexOf(mallet.Include.SENT_DELIM_REDUX) != -1) {
				writer.write(out + "\n");
				out = "";
				continue;
			}
			String[] segs = line.split(" ");
			String label = getLabel(segs);
			String token = getToken(segs);
			if (formatType.equals(CRF_OUTPUT_FORMAT))
				token = tokens[linenum++];
			if (label.equals(mallet.Include.CAUSE_BEGIN_TAG) || label.equals(mallet.Include.EFFECT_BEGIN_TAG)) {
				out += "[" + token + " ";
			} else if (label.equals(mallet.Include.CAUSE_END_TAG)) {
				out += token + "]C ";
			} else if (label.equals(mallet.Include.EFFECT_END_TAG)) {
				out += token + "]E ";
			} else if (label.equals(mallet.Include.CAUSE_TAG)) {
				out += "[" + token + "]C ";
			} else if (label.equals(mallet.Include.EFFECT_TAG)) {
				out += "[" + token + "]E ";
			} else {
				out += token + " ";
			}
		}
		writer.close();
	}
	
	String getLabel (String[] segs) {
		if (formatType.equals(CRF_INPUT_FORMAT)) {
			return segs[segs.length-1];
		} else if (formatType.equals(CRF_OUTPUT_FORMAT)) {
			return segs[0];
		}
		return null;
	}
	
	String getToken (String[] segs) {
		if (formatType.equals(CRF_INPUT_FORMAT)) {
			return segs[0];
		} else if (formatType.equals(CRF_OUTPUT_FORMAT)) {
			return segs[segs.length-1];
		}
		return null;
	}
	
	public static void main (String[] args) {
		turkCrfToHuman t = new turkCrfToHuman();
		t.translate();
	}
}

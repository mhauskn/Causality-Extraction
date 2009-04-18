package stream.Templates;

import stream.CRFDataStream.Container;

import haus.io.Pipe;

/**
 * Provides a set of methods useful for dealing with data
 * formatted in the CRF format: Generally:
 * 
 * token feat1 ... featn label
 *
 */
public interface CRFDataStreamTemplate {
	String getToken (String line);
	String getFeatures (String line);
	String getLabel (String line);
	String getOutput (String line);
	String[] getFeatureArray (String line);
	boolean containsDelim (String line);
	
	/**
	 * Parses a block of input text separating it
	 * into separate arrays for toks, feats, etc.
	 */
	Container parseBlock (Pipe<String> in);
	
	/**
	 * Returns the classes of possible labels: e.g.
	 * C for Cause, E for Effect, R for Reln
	 */
	String[] getLabelClasses ();
	
	/**
	 * Checks if a desired output or label contains
	 * the desired label class:
	 * CE contains C
	 * @param label CE, CI, CB, etc
	 * @param labelClass C,R,E, etc
	 */
	boolean containsLabel (String label, String labelClass);
}

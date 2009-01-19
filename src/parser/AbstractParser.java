package parser;

/**
 * Designed to give a well-formed block of text.
 * @author epn
 *
 */
public interface AbstractParser 
{
	/**
	 * Returns the next block of text in the current document
	 * @return
	 */
	public String getTextBlock ();
}

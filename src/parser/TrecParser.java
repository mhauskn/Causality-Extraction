package parser;

import haus.io.FileReader;

/**
 * Takes a standard TREC document and returns the text between 
 * <TEXT> and </TEXT> tags
 * @author epn
 *
 */
public class TrecParser implements AbstractParser
{
	/**
	 * Opening text tag
	 */
	public static final String TEXT_START_TAG = "<TEXT>";
	
	/**
	 * Closing text tag
	 */
	public static final String TEXT_END_TAG = "</TEXT>";
	
	/**
	 * The source of the text to parse
	 */
	private FileReader reader;	
	
	/**
	 * Creates a new TrecParser
	 * @param ar The abstract document reader need to read in text
	 */
	public TrecParser (FileReader ar)
	{
		reader = ar;
	}
	
	/**
	 * Returns the next block of text in the current document
	 */
	public String getTextBlock ()
	{
		String currentText = "";
		String line = "";
		
		while (!line.equals(TEXT_START_TAG))
		{
			line = reader.getNextLine();
			if (line == null)
				return null;
		}	
		
		line = reader.getNextLine();
		
		while (!line.equals(TEXT_END_TAG))
		{
			if (line.startsWith("<")) {}// we have a tag
				// do Nothing
			else
			{
				currentText += line + " ";
			}
			line = reader.getNextLine();
		}
		
		return currentText;
	}
}

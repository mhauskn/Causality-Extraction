package parser;

import haus.io.FileReader;

public class GenericParser implements AbstractParser {
	FileReader reader;
	
	public GenericParser (FileReader ar)
	{
		reader = ar;
	}
	
	/**
	 * Returns the next block of text in the current document
	 */
	public String getTextBlock ()
	{
		StringBuilder currentText = new StringBuilder();
		
		for (int i = 0; i < 100; i++) {
			String line;
			while ((line = reader.getNextLine()) != null)
				currentText.append(line);
		}
		
		String out = currentText.toString();
		if (out.length() == 0)
			return null;
		return out;
	}
}

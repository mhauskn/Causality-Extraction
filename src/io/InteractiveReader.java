package io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Interactively reads from user input
 * @author epn
 *
 */
public class InteractiveReader {
	InputStreamReader converter = null;
	BufferedReader iReader = null;
	String line = "";
	String end_dialogue = "quit";
	
	public InteractiveReader () {
		converter = new InputStreamReader(System.in);
		iReader = new BufferedReader(converter);
		System.out.println("Type 'quit' to quit.");
	}
	
	public String getInput () {
		System.out.print(" > ");
		try {
			line = iReader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		if (line.equals(end_dialogue))
			return null;
		return line;
	}
}

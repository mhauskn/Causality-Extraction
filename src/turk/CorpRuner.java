package turk;

import haus.io.FileReader;
import haus.io.IO;

/**
 * Throw-away class designed to help me look through different aspects of the corpus
 * @author Administrator
 *
 */
public class CorpRuner extends IO<String,String>{
	String end = "]R";
	int cnt = 0;
	public void map(String e) {
		cnt++;
		if (e.indexOf(end) != e.lastIndexOf(end))
			System.out.println(cnt);
	}

	public static void main (String[] args) {
		CorpRuner c =new CorpRuner();
		c.setInput(new FileReader(turk.Include.READABLE_CRF));
		c.mapInput();
	}
}

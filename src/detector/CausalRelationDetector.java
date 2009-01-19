package detector;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;

/**
 * Designed to detect causal relations via basic text matching schemes
 * @author epn
 *
 */
public class CausalRelationDetector 
{
	public static final String CAUSE_REGEXP = ".*( cause | caused ).*";
	public static final String CAUSE_REGEXP2 = ".* +(cause|caused) +.*";
	public static final String CAUSED_BY_REGEXP = ".* +(is|was|were|been) (cause|caused) +.*";
	
	private static long interesting = 0;
	private static long accepted = 0;
	
	/**
	 * True if the given chunking contains a causal verb phrase
	 * @param c
	 * @return
	 */
	public static boolean containsCausalRelation (Chunking c)
	{
        CharSequence cs = c.charSequence();
        for (Chunk chunk : c.chunkSet()) {
            String type = chunk.type();
            if (!type.equals("verb"))
            	continue;
            int start = chunk.start();
            int end = chunk.end();
            String text = cs.subSequence(start,end).toString();
            text = " " + text + " ";
            if (text.toString().matches(CAUSE_REGEXP2))
            {
            	accepted++;
            	return true;
            }
        }
		return false;
	}
	
	/**
	 * True if the sentence contains a word which might indicate a causal
	 * relation.
	 * @param s
	 * @return
	 */
	public static boolean containsCausalWord (String s)
	{
		if (s.matches(CAUSE_REGEXP))
		{
			interesting++;
			return true;
		}
		return false;
	}
	
	public static String[] getCausalTriple (Chunking c)
	{
		String agent = "";
		String connective = "";
		String effect = "";
		boolean connectiveIdentified = false;
		boolean effectIdentified = false;
		boolean reverse = false;
		
		CharSequence cs = c.charSequence();
        for (Chunk chunk : c.chunkSet()) {
            String type = chunk.type();
            int start = chunk.start();
            int end = chunk.end();
            String text = " " + cs.subSequence(start,end).toString() + " ";
            if (type.equals("verb") && text.matches(CAUSE_REGEXP2))
            {
            	connectiveIdentified = true;
            	connective = text;
            	
            	if (text.matches(CAUSED_BY_REGEXP))
            	{
            		reverse = true;
            		if (text.matches(".* +cause +.*")) connective = "cause";
            		if (text.matches(".* +caused +.*")) connective = "caused";
            	}
            }
            
            if (type.equals("noun") && !connectiveIdentified)
            	agent = text;
            
            if (type.equals("noun") && connectiveIdentified && !effectIdentified)
            {
            	effect = text;
            	effectIdentified = true;
            }
        }
		String[] triple = {agent, connective, effect};
		if (reverse) { triple[0] = effect; triple[2] = agent; }
		return triple;
	}
	
	public static long getInteresting ()
	{
		return interesting;
	}
	
	public static long getAccepted ()
	{
		return accepted;
	}
}

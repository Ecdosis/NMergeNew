package edu.luc.nmerge.graph;
import java.util.BitSet;
/**
 * We need to store information about which versions each 
 * previous character in the graph belongs to. This is 
 * because previous characters (as used in the 
 * MatchThreadDirect.isMaximal() routine)
 * @author desmond
 *
 */
public class PrevChar 
{
	/** the previous char to some starting-point 
	 * of a match in the graph */
	char previous;
	/** set of version that this char belongs 
	 * to (the versions of its arc) */
	BitSet versions;
	/**
	 * Vanilla constructor
	 * @param versions the versions of the arc the prevchar 
	 * belongs to
	 * @param previous the previous byte
	 */
	PrevChar( BitSet versions, char previous )
	{
		this.versions = versions;
		this.previous = previous;
	}
}

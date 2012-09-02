package edu.luc.nmerge.mvd;
import java.util.BitSet;
/**
 * Store Node in when constructing variants
 * @author desmond
 */
public class CompactNode 
{
	BitSet incoming,outgoing;
    /** index of the arc leading into the node or -1 for start node */
	int index;
	CompactNode( int index )
	{
		this.incoming = new BitSet();
		this.outgoing = new BitSet();
		this.index = index;
	}
	/**
	 * Get the set of versions leaving this node
	 * @return a BitSet
	 */
	BitSet getOutgoing()
	{
		return outgoing;
	}
	/**
	 * Get the set of versions entering this node
	 * @return a BitSet
	 */
	BitSet getIncoming()
	{
		return incoming;
	}
	/**
	 * Find the set of versions this node lacks as incoming
	 * @return the difference outgoing - incoming
	 */
	BitSet getWantsIncoming()
	{
		BitSet difference = new BitSet();
		difference.or( outgoing );
		difference.andNot( incoming );
		return difference;
	}
	/**
	 * Compute the difference between incoming, outgoing
	 * @return the difference 
	 */
	BitSet getWantsOutgoing()
	{
		BitSet difference = new BitSet();
		difference.or( incoming );
		difference.andNot( outgoing );
		return difference;
	}
	/**
	 * Get the index into the pairs vector: the index 
	 * at which this node starts
	 * @return an int
	 */
	int getIndex()
	{
		return index;
	}
	/**
	 * Add as incoming a set of versions
	 * @param versions the versions to add
	 */
	void addIncoming( Pair arc )
	{
		incoming.or( arc.versions );
	}
	/**
	 * Add as outgoing a pair
	 * @param arc the pair from the MVD to add as outgoing
	 */
	void addOutgoing( Pair arc )
	{
		outgoing.or( arc.versions );
	}
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("incoming: "+incoming.toString()+"\n");
		sb.append("outgoing: "+outgoing.toString()+"\n");
		sb.append("index: "+index);
		return sb.toString();
	}
}
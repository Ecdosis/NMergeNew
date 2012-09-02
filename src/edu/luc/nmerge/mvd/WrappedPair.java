package edu.luc.nmerge.mvd;

public class WrappedPair 
{
	Pair pair;
	CompactNode defaultNode;
	WrappedPair( Pair pair )
	{
		this.pair = pair;
	}
	CompactNode getDefaultNode()
	{
		return defaultNode;
	}
	Pair getPair()
	{
		return this.pair;
	}
	void setDefaultNode( CompactNode cn )
	{
		this.defaultNode = cn;
	}
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append( "pair: "+pair.toString()+"\n" );
		String defaultNodeString = (defaultNode==null)?"null":defaultNode.toString();
		sb.append( "defaultNode: "+defaultNodeString );
		return sb.toString();
	}
}

/*
 *  NMerge is Copyright 2009 Desmond Schmidt
 * 
 *  This file is part of NMerge. NMerge is a Java library for merging 
 *  multiple versions into multi-version documents (MVDs), and for 
 *  reading, searching and comparing them.
 *
 *  NMerge is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  NMerge is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.luc.nmerge.graph;
import edu.luc.nmerge.exception.*;
import java.util.BitSet;
import java.util.ListIterator;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.HashSet;
import edu.luc.nmerge.mvd.diff.Diff;

/**
 * Simple representation of a variant graph, or subgraph thereof
 * @author Desmond Schmidt 24/10/08
 */
public class Graph 
{
	/** the special start and end nodes that define the graph */
	Node start,end;
	/** distance of the (sub)graph from the start of the new version */
	int position;
	/** subset of versions applicable to this subgraph */
	BitSet constraint;
	/** maximum length of this graph */
	int maxLen;
	/** total length of all bytes stored in all arcs */
	int totalLen;
	static int MIN_OVERLAP_LEN = 10;

	/**
	 * Basic constructor
	 */
	public Graph()
	{
		start = new Node();
		end = new Node();
		this.constraint = new BitSet();
		maxLen = -1;
	}
	/**
	 * This constructor makes a sub-graph out of part of a larger graph
	 * @param start the node which will function as start and 
	 * may have incoming arcs that will be ignored
	 * @param end the node which will function as end and 
	 * which may have outgoing arcs that will be ignored
	 * @param constraint graph only covers these versions and ignores all others
	 * @param position the position from the start of the new version
	 */
	public Graph( Node start, Node end, BitSet constraint, int position )
	{
		this.start = start;
		this.end = end;
		this.position = position;
		this.constraint = new BitSet();
		this.constraint.or( constraint );
		this.maxLen = maxLength();
	}
	/**
	 * Split an arc at the desired location in the given version
	 * @param n the node to start looking from
	 * @param version the version to follow
	 * @param pos the initial position of n in the version
	 * @param offset the offset we are seeking to split at
     * @param atStart if true we are seeking a split for the start node
	 * @return the node in the middle or at the start of the found arc
	 */
	Node splitArcAt( Node n, short version, int pos, int offset,
        boolean atStart ) throws Exception
	{
		Arc a = null;
		Node splitNode = null;
		Node orig = n;
		while ( n != end )
		{
			a = n.pickOutgoingArc( version );
			if ( pos+a.dataLen() < offset || (!atStart && pos+a.dataLen()
				== offset) )
			{
                pos += a.dataLen();
                n = a.to;
            }
            else 
                break;
		}
		if ( a != null )
		{
            if ( offset == pos )
			{
				if ( atStart )
					splitNode = a.from;
				else if ( n != orig )
					splitNode = n;
				else
					splitNode = n.split();
			}
			else if ( a.dataLen()+pos == offset )
				splitNode = a.to;
			else
			{
				Arc[] pair = a.split( offset-pos );
				splitNode = pair[0].to;
			}
		}
        else
            throw new MVDException("could not find arc at offset "
                +offset+" in version "+version );
		return splitNode;
	}
	/**
	 * Create a subgraph by traversing this graph to the position indicated
	 * @param d the diff to form the basis of a subgraph
     * @param version the version to follow
     * @param pos the position in the version we are seeking
     * @param n the node to start looking from
	 * @return the subgraph
	 */
	public Graph getMiniGraph( Diff d, short version, int pos, Node n )
        throws Exception
	{
		Node startNode = splitArcAt( n, version, pos, d.oldOff(), true );
		Node endNode = splitArcAt( startNode, version, 
			d.oldOff(), d.oldOff()+d.oldLen(), false );
		BitSet nc = new BitSet();
		nc.or( startNode.getVersions() );
		nc.and( endNode.getVersions() );
		return new Graph( startNode, endNode, nc, d.oldOff() );
	}
	/**
	 * Work out if there are any paths from start to end that share exactly 
	 * the same text. This is unique because the shared versions must include 
	 * the given version, and by the definition of a variant graph there is 
	 * only one path for each version form start to end.
	 * @param version the shared version set MUST contain this version
	 * @return a set of versions that share the same text
	 */
	public BitSet getSharedVersions( short version )
	{
		Node n = start;
		BitSet bs = new BitSet();
		assert start.getOutgoingSet().nextSetBit(version)==version;
		bs.or( start.pickOutgoingArc(version).versions );
		while ( n != end )
		{
			Arc a = n.pickOutgoingArc( version );
			bs.and( a.versions );
			n = a.to;
		}
		return bs;
	}
	/**
	 * Add an unaligned arc to the graph, attached to the start and end only
	 * @param data the data of the single version it will hold
	 * @param version the ID of that version
	 * @param position the position of the arc
	 * @return the special, unaligned arc
	 */
	public SpecialArc addSpecialArc( char[] data, int version, int position )
		throws MVDException
	{
		BitSet bs = new BitSet();
		bs.set( version );
		return addSpecialArc( data, bs, position );
	}
	/**
	 * Add an unaligned arc to the graph, attached to the start and end only
	 * @param data the data of the single version it will hold
	 * @param bs the versions of the arc
	 * @param position the position of the arc
	 * @return the special, unaligned arc
	 */
	public SpecialArc addSpecialArc( char[] data, BitSet bs, int position )
		throws MVDException
	{
		SpecialArc a = new SpecialArc( bs, data, position );
		start.addOutgoing( a );
		end.addIncoming( a );
		// ensure this is clear
		this.constraint.andNot( bs );
		// not part of the constraint set yet
		return a;
	}
	/**
	 * Get the data of the specified version
	 * @param version the id of the version to read
	 * @return the version's data as a char array
	 */
	char[] getVersion( int version )
	{
		Node temp = start;
		int len=0;
		while ( temp != null && temp != end )
		{
			Arc a = temp.pickOutgoingArc( version );
			len += a.dataLen();
			temp = a.to;
		}
		char[] versionData = new char[len];
		temp = start;
		int j = 0;
		while ( temp != null && temp != end )
		{
			Arc a = temp.pickOutgoingArc( version );
			char[] data = a.getData();
			for ( int i=0;i<data.length;i++ )
				versionData[j++] = data[i];
			temp = a.to;
		}
		return versionData;
	}
	/**
	 * Find the maximum length of this graph in bytes by following 
	 * each path and finding the length of the longest one
	 * @return the maximum length of the graph
	 */
	private int maxLength()
	{
		HashMap <Integer,Integer> lengths = new HashMap<Integer,Integer>();
		SimpleQueue<Node> queue = new SimpleQueue<Node>();
		HashSet<Node> printed = new HashSet<Node>();
		queue.add( start );
		while ( !queue.isEmpty() )
		{
			Node node = queue.poll();
			ListIterator<Arc> iter = node.outgoingArcs( this );
			while ( iter.hasNext() )
			{
				Arc a = iter.next();
				char[] data = a.getData();
				// calculate total length
				totalLen += data.length;
				BitSet bs = a.versions;
				for ( int i=bs.nextSetBit(0);i>=0;i=bs.nextSetBit(i+1) )
				{
					if ( lengths.containsKey(i) )
					{
						Integer value = lengths.get( i );
						lengths.put( i, value.intValue()+data.length );
					}
					else
						lengths.put( i, data.length );
				}
				a.to.printArc( a );
				printed.add( a.to );
				if ( a.to != end && a.to.allPrintedIncoming(constraint) )
				{
					queue.add( a.to );
					a.to.reset();
				}
			}
		}
		// clear printed
		Iterator<Node> iter2 = printed.iterator();
		while ( iter2.hasNext() )
		{
			Node n = iter2.next();
			n.reset();
		}
		/// Find the maximum length version
		Integer max = new Integer( 0 );
		Set<Integer> keys = lengths.keySet();
		Iterator<Integer> iter = keys.iterator();
		while ( iter.hasNext() )
		{
			Integer key = iter.next();
			Integer value = lengths.get( key );
			if ( value.intValue() > max.intValue() )
				max = value;
		}
		return max.intValue();
	}
	/**
	 * What is the maximum length of this graph?
	 * @return the length 
	 */
	int length()
	{
		if ( maxLen == -1 )
			maxLen = maxLength();
		return maxLen;
	}
	/**
	 * Get the total length of all bytes in the graph
	 * @return the number of bytes in the graph
	 */
	int totalLen()
	{
		if ( maxLen == - 1 )
			maxLen = maxLength();
		return totalLen;
	}
	/**
	 * Add a version to the constraint set, i.e. adopt it. Also turn 
	 * all special arcs into ordinary arcs.
	 * @param version the version to adopt
	 */
	public void adopt( int version ) throws Exception
	{
		constraint.set( version );
		Node temp = start;
		while ( temp != end )
		{
			Arc a = temp.pickOutgoingArc( version );
			assert a != null: "Couldn't find outgoing arc for version "+version;
			if ( a instanceof SpecialArc )
			{
				constraint.or( a.versions );
				Arc b = new Arc( a.versions, a.data );
				temp.replaceOutgoing( a, b );
				a.to.replaceIncoming( a, b );
				temp = b.to;
			} 
			else
				temp = a.to;
			assert temp != null;
		}
	}
	/**
	 * Verify a graph by breadth-first traversal, using exceptions.
	 */
	public void verify() throws MVDException
	{
		SimpleQueue<Node> queue = new SimpleQueue<Node>();
		HashSet<Node> printed = new HashSet<Node>();
		start.verify();
		queue.add( start );
		while ( !queue.isEmpty() )
		{
			Node node = queue.poll();
			node.verify();
			ListIterator<Arc> iter = node.outgoingArcs(this);
			while ( iter.hasNext() )
			{
				Arc a = iter.next();
				a.verify();
				a.to.printArc( a );
				printed.add( a.to );
				if ( a.to != end && a.to.allPrintedIncoming(constraint) )
				{
					queue.add( a.to );
				}
			}
		}	
		Iterator<Node> iter2 = printed.iterator();
		while ( iter2.hasNext() )
		{
			Node n = iter2.next();
			n.reset();
		}
		end.verify();
	}
	/**
	 * Print out a graph using breadth-first traversal. Also verify.
	 * @param message a message to display first
	 */
	public void printAndVerify( String message )
	{
		System.out.println("-----------------------");
		System.out.println( message );
		System.out.println( toString() );
	}
	/** 
	 * Classic override of toString method, but check structure 
	 * of graph also. Works for subgraphs too.
	 * @return a String representation of this Graph
	 */
	public String toString()
	{
		int totalOutdegree = 0;
		int totalIndegree = 0;
		int totalNodes = 0;
		int maxIndegree = 0;
		int maxOutdegree = 0;
		StringBuilder sb = new StringBuilder();
		HashSet<Node> printed = new HashSet<Node>();
		try
		{
			SimpleQueue<Node> queue = new SimpleQueue<Node>();
			queue.add( start );
			while ( !queue.isEmpty() )
			{
				Node node = queue.poll();
				if ( node.indegree() > maxIndegree )
					maxIndegree = node.indegree();
				if ( node.outdegree() > maxOutdegree )
					maxOutdegree = node.outdegree();
				totalIndegree += node.indegree();
				totalOutdegree += node.outdegree();
				totalNodes++;
				node.verify();
				ListIterator<Arc> iter = node.outgoingArcs(this);
				while ( iter.hasNext() )
				{
					Arc a = iter.next();
					sb.append( a.toString() );
					sb.append( "\n" );
					a.to.printArc( a );
					printed.add( a.to );
					if ( a.to != end && a.to.allPrintedIncoming(constraint) )
					{
						queue.add( a.to );
					}
				}
			}
			Iterator<Node> iter2 = printed.iterator();
			while ( iter2.hasNext() )
			{
				Node n = iter2.next();
				n.reset();
			}
		}
		catch ( Exception e )
		{
			System.out.println(sb.toString());
		}
		float averageOutdegree = (float)totalOutdegree/(float)totalNodes;
		float averageIndegree = (float)totalIndegree/(float)totalNodes;
		sb.append("\naverageOutdegree=" );
		sb.append( averageOutdegree );
		sb.append("\naverageIndegree=" );
		sb.append( averageIndegree );
		sb.append("\nmaxOutdegree=" );
		sb.append( maxOutdegree );
		sb.append("\nmaxIndegree=" );
		sb.append( maxIndegree );
		return sb.toString(); 
	}
	/**
	 * Clear all the printed arcs. Report any that are already 
	 * printed but shouldn't be. 
	 */
	void clearPrinted()
	{
		HashMap<Integer,Node> hash = new HashMap<Integer,Node>(1500);
		SimpleQueue<Node> queue = new SimpleQueue<Node>();
		queue.add(start);
		while ( !queue.isEmpty() )
		{
			Node node = queue.poll();
			ListIterator<Arc> iter = node.outgoingArcs();
			while ( iter.hasNext() )
			{
				Arc a = iter.next();
				if ( !hash.containsKey(a.to.nodeId) )
				{
					queue.add( node );
					node.reset();
					hash.put( a.to.nodeId, a.to );
				}
			}
		}
	}
	// extra routines for nmerge
	/**
	 * Get the start node (read only)
	 * @return a node
	 */
	public Node getStart()
	{
		return start;
	}
	// extra routines for nmerge
	/**
	 * Get the end node (read only)
	 * @return a node
	 */
	public Node getEnd()
	{
		return end;
	}
	/**
	 * Remove a set of versions from the graph
	 * @param bs the set of versions to remove
	 */
	public void removeVersions( BitSet bs )
	{
		for ( int i=bs.nextSetBit(0);i>=0;i=bs.nextSetBit(i+1))
		{
			removeVersion( i );
		}
	}
	/**
	 * Remove the text of a version from the graph. 
	 * @param version the version to remove
	 */
	public void removeVersion( int version )
	{
		Node n = start;
        while ( n != end )
		{
			Arc a = n.pickOutgoingArc( version );
			// get next n before a.to becomes null
			n = a.to;
			if ( a.versions.cardinality()==1 )
			{
				a.from.removeOutgoing( a );
				a.to.removeIncoming( a );
				// is it a child arc?
				if ( a.parent != null )
					a.parent.removeChild( a );
				else if ( a.children != null )
					a.passOnData();
			}
			else
			{
				a.versions.clear( version );
				a.to.removeIncomingVersion( version );
				a.from.removeOutgoingVersion( version );
			}
            // not needed once testing is complete
			// infeasible when merging minigraphs
			/*try
			{
				n.verify();
			}
			catch ( Exception e )
			{
				System.out.println(e);
			}	*/
		}
		this.constraint.clear( version );
	}
}

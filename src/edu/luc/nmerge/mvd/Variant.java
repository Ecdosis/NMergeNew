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
package edu.luc.nmerge.mvd;
import java.util.BitSet;
import edu.luc.nmerge.exception.MVDException;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
/**
 * Represent a variant computed from a range in a base version
 * @author Desmond Schmidt 1/6/09
 */
public class Variant implements Comparable<Variant>
{
	/** more than one version for a variant is possible */
	BitSet versions;
	/** the start-index where it occurs within pairs */
	int startIndex;
	/** end index within pairs (used for isWithin) */
	int endIndex;
	/** the start-offset within start node */
	int startOffset;
	/** the length of the variant's real data in chars */
	int length;
	/** the mvd it is associated with */
	MVD mvd;
	/** the actual data of this variant */
	char[] data;
	/**
	 * Construct a variant 
	 * @param startOffset initial offset within startIndex
	 * @param startIndex the index within mvd of the first node
	 * @param length the length of the variant
	 * @param versions the set of versions over the variant
	 * @param mvd the mvd it came from
	 * @throws MVDException
	 */
	public Variant( int startOffset, int startIndex, int endIndex, 
		int length, BitSet versions, MVD mvd )
	{
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.mvd = mvd;
		this.versions = versions;
		this.length = length;
		this.startOffset = startOffset;
	}
	/**
	 * Get the version set
	 * @return a BitSet
	 */
	public BitSet getVersions()
	{
		return versions;
	}
	/**
	 * Create a String representing the header part of a chunk
	 * @return the header as a String including the trailing ':'
	 */
	protected String createHeader()
	{
		StringBuffer sb = new StringBuffer();
		sb.append( '[' );
		for ( int i=versions.nextSetBit(1); i>=1; 
			i=versions.nextSetBit(i+1) ) 
		{ 
			if ( sb.length()>1 )
				sb.append(',');
			sb.append( mvd.getVersionShortName(i) );
		} 
		sb.append(':');
		return sb.toString();
	}
	/**
	 * Convert to a string
	 */
	public String toString()
	{
		String header = createHeader();
		StringBuilder sb = new StringBuilder();
		try
		{
			sb.append( new String(data) );
		}
		catch ( Exception e )
		{
			sb.append( new String(data) );
		}
		String dataStr = sb.toString();
		return header+dataStr+"]";
	}
	/**
	 * Convert this variant to a char array
	 * @return a char array
	 */
	public char[] getChars()
	{
		String str = this.toString();
        char[] chars = new char[str.length()];
        str.getChars(0,chars.length,chars,0);
        return chars;
	}
	/**
	 * Test for equality. Versions don't matter. What we want is to find 
	 * out if two variants have the same text.
	 * @param other the other variant to compare with this one
	 * @return true if they are the same
	 */
	public boolean equals( Object other )
	{
		Variant otherV = (Variant)other;
		return this.versions.equals(otherV.versions) 
			&& this.startIndex == otherV.startIndex
			&& this.endIndex == otherV.endIndex
			&& this.startOffset == otherV.startOffset
			&& this.mvd == otherV.mvd
			&& this.equalsContent(otherV);
	}
	/**
	 * Are two variants equal in content but differ only in versions?
	 * @param other the other variant to compare with
	 * @return true if they are 'equal'
	 */
	public boolean equalsContent( Variant other )
	{
		if ( this.mvd != other.mvd
			|| this.data.length != other.data.length )
			return false;
		else 
		{
			for ( int i=0;i<data.length;i++ )
			{
				if ( this.data[i] != other.data[i] )
					return false;
			}
			return true;
		}
	}
	/**
	 * Generate a hash of the content of this Variant. It should be almost 
	 * unique. It will be used to collect together and wipe out any variants 
	 * generated during the getApparatus method that are identical.
	 */
	public int hashCode()
	{
		final int MOD_ADLER = 65521;
		int a = 1;
		int b = 0;
		String nodeStr = Integer.toString(startIndex);
		String offsetStr = Integer.toString(startOffset);
		String vStr = versions.toString();
		int hDataLen = data.length+nodeStr.length()
			+offsetStr.length()+vStr.length();
		char[] hashData = new char[hDataLen];
		int j = 0;
		for ( int i=0;i<nodeStr.length();i++ )
			hashData[j++] = nodeStr.charAt(i);
		for ( int i=0;i<offsetStr.length();i++ )
			hashData[j++] = offsetStr.charAt(i);
		for ( int i=0;i<vStr.length();i++ )
			hashData[j++] = vStr.charAt(i);
		for ( int i=0;i<data.length;i++ )
			hashData[j++] = data[i];
		for ( int i=0;i<hashData.length;++i )
		{
			a = (a+hashData[i])%MOD_ADLER;
			b = (a+b)%MOD_ADLER;
		}
		return (b<<16)|a;
	}
	/** 
	 * Generate content by following the paths of the variant
	 * in the MVD.
	 */
	void findContent()
	{
		if ( data == null )
        {
            StringBuilder sb = new StringBuilder();
            int iNode = startIndex;
            Pair p = mvd.pairs.get( iNode );
            int i = startOffset;
            int totalLen = 0;
            while ( p.length()==0 || totalLen<this.length )
            {
                if ( p.length()==0||i==p.length() )
                {
                    iNode = mvd.next(iNode+1,(short)
                        versions.nextSetBit(0));
                    p = mvd.pairs.get( iNode );
                    i = 0;
                }
                else
                {
                    sb.append( p.getChars()[i++] );
                    totalLen++;
                }
            }
            data = new char[sb.length()];
            sb.getChars(0, data.length, data, 0);
        }
	}
	/**
	 * Merge two variants equal in content
	 * @param other the other variant to merge with this one.
	 */
	public void merge( Variant other )
	{
		this.versions.or( other.versions ); 
	}
    /**
     * Split this variant so that the set of specified versions are 
     * contained in a new variant, and the rest in the old variant.
     * @param bs a set of versions
     * @return the new variant representing those versions
     */
    Variant split( BitSet bs )
    {
        Variant v = new Variant( this.startOffset, this.startIndex, 
            this.endIndex, this.length, this.versions, this.mvd );
        v.versions.and( bs );
        this.versions.andNot( bs );
        return v;
    }
	/**
	 * Is this variant entirely contained within another variant? 
	 * We just check if we are within the bounds of the other variant. 
	 * No need to compare the text of the two variants - the versions 
	 * must be the same, so within the bounds means that the same text 
	 * will occur.
	 * @param other the other variant to compare it to
	 * @return true if we are within other, false otherwise
	 */
	public boolean isWithin( Variant other )
	{
		// these tests will mostly fail
		// so we can avoid the main computation
		if ( length < other.length 
			&& startIndex >= other.startIndex 
			&& endIndex <= other.endIndex
			&& this.versions.equals(other.versions) )
		{
			// another quick test to shortcut the computation
			if ( startIndex == other.startIndex 
				&& (startOffset<other.startOffset
					||(startOffset-other.startOffset)+length>other.length) )
				return false;
			else 
			{
				// OK, we have some work to do ...
				// find the start of this variant in other
				int offset = other.startOffset;
				int index = other.startIndex;
				Pair p = mvd.pairs.get( index );
				int i = 0;
				short followV = (short) versions.nextSetBit(1);
				while ( i < other.length )
				{
					if ( offset==p.length() )
					{
						index = mvd.next( index+1, followV );
						p = mvd.pairs.get( index );
						offset = 0;
					}
					else
					{
						offset++;
						i++;
					}
					// found start?
					if ( index == startIndex && offset == startOffset )
						return other.length-i>=length;
				}
			}
		}
		return false;
	}
	/**
	 * Compare two Variants. Try to short-circuit the  
	 * comparison to reduce computation.
	 * @param other the variant to compare ourselves to
	 */
	public int compareTo( Variant other )
	{
		if ( this.startIndex < other.startIndex )
			return -1;
		else if ( this.startIndex > other.startIndex )
			return 1;
		else if ( this.startOffset < other.startOffset )
			return -1;
		else if ( this.startOffset > other.startOffset )
			return 1;
		else if ( this.length < other.length )
			return -1;
		else if ( this.length > other.length )
			return 1;
		else
		{
			String thisV = versions.toString();
			String thatV = other.versions.toString();
			int res = thisV.compareTo( thatV );
			if ( res != 0 )
				return res;
			else
			{
				String thisD = new String( data );
				String thatD = new String( other.data );
			    return thisD.compareTo( thatD );
			}
		}
	}
}

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
package edu.luc.nmerge.mvd.table;
import edu.luc.nmerge.mvd.Version;
import edu.luc.nmerge.mvd.Group;
import java.util.BitSet;
import java.util.ArrayList;

/**
 * General utilities for table view
 * @author desmond
 */
public class Utils 
{
    static String[] layerNames = {"add0","base","del1","rdg1","rdg2"}; 
    /**
     * Check if the given shortName is a layer
     * @param shortName the shortName of a version
     * @return true if it is a recognised layer name
     */
    private static boolean isLayerName( String shortName )
    {
        int top = 0;
        int bot = layerNames.length-1;
        while ( top <= bot )
        {
            int mid = (top+bot)/2;
            int res = shortName.compareTo(layerNames[mid]);
            if ( res == 0 )
                return true;
            else if ( res < 0 )
                bot = mid-1;
            else
                top = mid+1;
        }
        return false;
    }
    /**
     * Using a sigla array convert a bitset into a string of 
     * space-delimited short names
     * @param groups the MVD's list of groups
     * @param bs the set of versions
     * @return a String
     */
    static String bitSetToString( ArrayList<Version> sigla, 
        ArrayList<Group> groups, BitSet bs )
    {
        try
        {
            StringBuilder sb = new StringBuilder();
            for ( int i = bs.nextSetBit(0); i>= 0; i = bs.nextSetBit(i+1))
            {
                Version v = sigla.get(i-1);
                String siglum = v.shortName;
                if ( isLayerName(siglum) )
                {
                    Group g = groups.get(v.group-1);
                    siglum = g.name+"/"+v.shortName;
                }
                if ( sb.length()>0 )
                    sb.append( " " );
                sb.append( siglum );
            }
            return sb.toString();
        }
        catch ( Exception e )
        {
            System.out.println("error");
            return "";
        }
    }
    /**
     * Adjust two bitsets so that they do not overlap
     * @param bs1 the first bitset
     * @param bs2 the second bitset
     */
    public static void ensureExclusivity( BitSet bs1, BitSet bs2 )
    {
        if ( bs1.cardinality()>bs2.cardinality() )
        {
            bs1.andNot( bs2 );
            bs2.andNot( bs1 );
        }
        else
        {
            bs2.andNot( bs1 );
            bs1.andNot( bs2 );
        }
    }
}

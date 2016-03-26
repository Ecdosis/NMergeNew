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
import java.util.Random;

/**
 * Generate a random Id
 * @author desmond
 */

public class RandomId
{
    private static final String symbols = "0123456789abcdefghijklmnopqrstuvwxyz";

    private static final Random random = new Random();
    /** 
    * Create a random Id of the required length and reasonably high uniqueness
    * @param length 
    */
    public static String getId( int length )
    {
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<length;i++ )
        {
            sb.append( symbols.charAt(random.nextInt(36)));
        }
        return sb.toString();
    }
}
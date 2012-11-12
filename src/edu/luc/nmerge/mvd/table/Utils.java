/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.luc.nmerge.mvd.table;
import edu.luc.nmerge.mvd.Version;
import java.util.BitSet;
import java.util.ArrayList;

/**
 *
 * @author desmond
 */
public class Utils 
{
    /**
     * Using a sigla array convert a bitset into a string of 
     * space-delimited short names
     * @param bs the set of versions
     * @return a String
     */
    static String bitSetToString( ArrayList<Version> sigla, BitSet bs )
    {
        try
        {
            StringBuilder sb = new StringBuilder();
            for ( int i = bs.nextSetBit(0); i>= 0; i = bs.nextSetBit(i+1))
            {
                String siglum = sigla.get(i-1).shortName;
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
}

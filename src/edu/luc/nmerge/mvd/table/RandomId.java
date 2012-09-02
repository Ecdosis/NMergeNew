/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
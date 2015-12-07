/**
 * Record moves made within the matrix and print them out. We use a
 * 2-D array of characters and just fill them in. This object is only
 * instantiated if both strings are less than 100 characters long.
 * Used only for debug.
 * Author: desmond
 *
 * Created on 6 March 2011, 1:38 PM
 */

package edu.luc.nmerge.mvd.diff;

/**
 *
 * @author desmond
 */
public class Tracker
{
    int x;
    int y;
    char[] xAxis;
    char[] yAxis;
    char[][] m;
    /**
     * Create a tracker
     * @param x its x-dimension
     * @param y its y-dimension
     */
    Tracker( char[] xAxis, char[] yAxis )
    {
        this.x = xAxis.length;
        this.y = yAxis.length;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.m = new char[y+1][];
        for ( int i=0;i<y+1;i++ )
        {
            this.m[i] = new char[x+1];
            for ( int j=0;j<x;j++ )
                this.m[i][j] = '-';
        }
    }
    /**
     * Record a move in the matrix
     * @param fromX the move's original x-position
     * @param fromY the move's original y-position
     * @param toX the move's final x-position
     * @param toY the move's final y-position
     */
    void track( int fromX, int fromY, int toX, int toY )
    {
        int deltaX = toX-fromX;
        int deltaY = toY - fromY;
        if ( deltaX > 0 && deltaY == 0 )
            for ( int i=fromX;i<=toX;i++ )
                this.m[fromY][i] = 'x';
        else if ( deltaX == 0 && deltaY > 0 )
            for ( int i=fromY;i<=toY;i++ )
                this.m[i][fromX] = 'x';
        else if ( deltaX > 0 && deltaY > 0 )
            for ( int i=fromY,j=fromX;i<=toY&&j<=toX;i++,j++ )
                this.m[i][j] = 'x';
    }
    /**
     * Print the formatted tracker data to standard out
     */
    void print()
    {
        System.out.println("  "+xAxis);
        for ( int i=0;i<y;i++ )
        {
            System.out.print(yAxis[i]+" ");
            for ( int j=0;j<x;j++ )
                System.out.print(m[i][j]);
            System.out.print("\n");
        }
    }
}

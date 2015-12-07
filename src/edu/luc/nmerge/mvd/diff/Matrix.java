/**
 * Represent the matrix of string1 versus string 2 economically as a linked
 * list of active diagonals and manage the alignment process.
 */

package edu.luc.nmerge.mvd.diff;

public class Matrix
{
        char[] A;
        char[] B;
        int goalIndex;
        int d;
        Diagonal best;
        Diagonal diagonals;
        Tracker t;
        boolean reachedEnd;
    /**
     * Set up the matrix
     * @param str1 the first string
     * @param str2 the second string
     */
    protected Matrix( char[] str1, char[] str2 )
    {
        this.A = str1;
		// B is the old or base version
        this.B = str2;
        if ( A.length > 100 || B.length > 100 )
            this.t = null;
        else
            this.t = new Tracker( this.B, this.A );
        this.diagonals = null;
        this.goalIndex = B.length-A.length;
        this.best = null;
        this.reachedEnd = false;
    }
    /**
     * Convenience method to call compute basic diffs
     * @param newtext the new text not yet committed to the MVD
     * @param base the original version already in the MVD
     * @return an array of Diffs contained only changed diffs
     */
    public static Diff[] computeBasicDiffs( char[] newtext, char[] base )
    {
        Matrix m = new Matrix( newtext, base );
        m.compute();
        return m.getDiffs( true );
    }
    /**
     * Convenience method to call compute detailed diffs
     * @param newtext the new text not yet committed to the MVD
     * @param base the original version already in the MVD
     * @return an array of Diffs containing inserts dels and exchanges
     */
    public static Diff[] computeDetailedDiffs( char[] newtext, char[] base )
    {
        Matrix m = new Matrix( newtext, base );
        m.compute();
        return m.getDiffs( false );
    }
    /**
     * Compute the matrix by finding the optimal path from source to sink.
     * We cannot compute each diagonal in sequence because they interfere
     * with their neighbours. So we alternatively compute the odd and even
     * diagonals. Once both odd and even diagonals have been done we increment
     * d (the edit distance).
     */
    void compute()
    {
        diagonals = new Diagonal( 0, this, 0 );
        diagonals.p = new Path( 0, 0, PathOperation.nothing, 0 );
        Diagonal current = diagonals;
        best = diagonals;
        d = 1;
        int run = 0;
        int N = (A.length>B.length)?A.length:B.length;
        while ( d < N )
        {
            while ( current != null && !current.atEnd() )
            {
                if ( (run % 2) == Math.abs(current.index % 2) )
                    current.seek();
                current = current.left;
            }
            if ( current != null && current.atEnd() )
                break;
            if ( run % 2 == 1 )
                d++;
            run++;
            current = diagonals;
        }
    }
    /**
     * Get the basic or detailed diffs after a run of compute
     * @param basic if true get only changed ranges, else show inserts dels etc
     * @return and array of Diffs
     */
    Diff[] getDiffs( boolean basic )
    {
        Diff[] diffs = new Diff[0];
        /*if ( t != null )
            t.print();*/
        if ( best.p != null )
            diffs = best.p.print( B.length, A.length, basic );
        return diffs;
    }
    /**
     * Check if we can move to the requested position.
     * If the minimum cost of the new diagonal is greater than the maximum
     * cost of the best diagonal then it is not viable. Also if we go outside
     * the square it's not valid either.
     * @param index the index of the diagonal requested
     * @param x its x value at the moment
     * @return true if it was OK, false otherwise
     */
    boolean check( int index, int x )
    {
        return valid(index,x) && within(index,x);
    }
    /**
     * Is this move within the matrix bounds? We can go one outside the square.
     * @param index the diagonal's index
     * @param x the x-value
     * @return true if it is the square
     */
    boolean within( int index, int x )
    {
        return x <= B.length && x-index <= A.length;
    }
    /**
     * Is this move on a valid diagonal?
     * @param index the diagonal index
     * @param x the x-value along it
     * @return true if it is valid, false otherwise
     */
    boolean valid( int index, int x )
    {
        int minCost = Math.abs(index-goalIndex);
        int maxCost = Math.abs(best.index-goalIndex)+(B.length-best.x);
        return minCost <= maxCost;
    }
    /**
     * Set the best value for the best diagonal
     * @param index the index of the diagonal
     * @param x its new x value that MAY be the best
     */
    void setBest( Diagonal dia )
    {
        if ( best == null )
            best = dia;
        else
        {
            int oldDist = (this.B.length-best.x) + Math.abs(best.index-goalIndex);
            int dist = (this.B.length-dia.x) + Math.abs(dia.index-goalIndex);
            if ( dist < oldDist )
                best = dia;
        }
        reachedEnd = best.atEnd();
    }
    /**
     * Record the moves in the matrix for debugging
     * @param fromX the original x-coordinate
     * @param fromY the original y-coordinate
     * @param toX the final x-coordinate
     * @param toY the final Y-coordinate
     */
    void track( int fromX, int fromY, int toX, int toY )
    {
        if ( t != null )
            t.track( fromX, fromY, toX, toY );
    }
}

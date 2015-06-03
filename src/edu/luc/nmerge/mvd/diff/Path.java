/**
 * Represent a path component. If the type changes e.g. from deleted
 * to inserted, we start a new path and join it to the previous one.
 * When we print it out we convert to Diff format.
 * Author: desmond
 *
 * Created on 9 March 2011, 7:43 AM
 */

package edu.luc.nmerge.mvd.diff;
import java.util.ArrayList;
import java.awt.Point;

/**
 *
 * @author desmond
 */
public class Path
{
    Path prev;
    Path next;
    PathOperation kind;
    int len;
    int index;
    int x;
    /**
     * Construct a path component whenever we make a move that is different
     * from the previous move. Otherwise we will extend the existing move.
     * @param index the path's original index
     * @param x the path's original x-position
     * @param operation the edit operation that got us here
     * @param len the length of the operation
     */
    Path( int index, int x, PathOperation kind, int len )
    {
        this.index = index;
        this.x = x;
        this.kind = kind;
        this.len = len;
        this.prev = null;
    }
    /**
     * The Diff type has changed. Print out the old one IF it is pending
     * @param old the old location in the edit graph
     * @param p the current path component
     * @param diffs a list to store the new diff in
     * @param oldKind the kind of the pending Diff
     * @return
     */
    Point printOld( Point old, Path p, ArrayList<Diff> diffs,
		DiffKind oldKind )
    {
        if ( p.x>old.x || (p.x-p.index)>old.y )
        {
            diffs.add( new Diff( old.x, old.y, p.x-old.x,
            (p.x-p.index)-old.y,oldKind) );
        }
        old.x = p.x;
        old.y = p.x-p.index;
        return old;
    }
    /**
     * Print the path in the form of Diffs.
     * @param xLen the length of the original version
     * @param yLen the length of the new version
     * @param basic if true return only changed Diffs
     * @return an array of diffs in A compared to the base version B
     */
    Diff[] print( int xLen, int yLen, boolean basic )
    {
        Path p = this;
        ArrayList<Diff> diffs = new ArrayList<Diff>();
		// reverse the path
        Path start = null;
        while ( p != null )
        {
            Path oldp = p;
            p = p.prev;
            if ( p != null )
                p.next = oldp;
            else
                start = oldp;
        }
        // now build the diff array
        p = start;
        Point old = new Point( 0, 0 );
        DiffKind oldKind = DiffKind.CHANGED;
        while ( p != null )
        {
            switch ( p.kind )
            {
                case nothing:
                    break;
                case insertion:
                    if ( !basic )
                    {
                        if ( oldKind != DiffKind.INSERTED )
                            old = printOld( old, p, diffs, oldKind );
                        oldKind = DiffKind.INSERTED;
                    }
                    break;
                case deletion:
                    if ( !basic )
                    {
                        if ( oldKind != DiffKind.DELETED )
                            old = printOld( old, p, diffs, oldKind );
                        oldKind = DiffKind.DELETED;
                    }
                    break;
                case exchange:
                    if ( !basic )
                    {
                        if ( oldKind != DiffKind.EXCHANGED )
                            old = printOld( old, p, diffs, oldKind );
                        oldKind = DiffKind.EXCHANGED;
                    }
                    break;
                case matched:
                    printOld( old, p, diffs, oldKind );
                    old.x = p.x+p.len;
                    old.y = (p.x-p.index)+p.len;
                break;
            }
            p = p.next;
        }
		p = new Path( xLen-yLen, xLen, PathOperation.nothing, 0 );
        printOld( old, p, diffs, oldKind );
        Diff[] array = new Diff[diffs.size()];
        return diffs.toArray( array );
    }
}

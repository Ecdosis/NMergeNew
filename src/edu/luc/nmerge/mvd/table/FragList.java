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
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * A list of concatenated fragments within a Section. Represented by a 
 * table cell in HTML. A FragList does not have a set of versions, but the 
 * versions shared by all fragments in the list can be computed on the fly 
 * by the getShared method. Individually Atoms in the list may go outside 
 * this narrow band.
 */
public class FragList
{
    /** atoms of which we are composed (can include tables) */
    ArrayList<Atom> fragments;
    /** a reverse-order list of edit operations or null */
    Step editScript;
    /** this fraglist is the base version of a nested row */
    boolean isBase;
    /** is this fraglist a possibly empty merged one? */
    boolean merged;
    /** Is this fraglist part of a nested table? */
    boolean nested;
    /** id for entire cell */
    int id;
    /**
     * Create an empty frag list 
     */
    FragList()
    {
        fragments = new ArrayList<Atom>();
    }
    /**
     * Create a fraglist
     * @param merged if true then it is merged
     */
    FragList( boolean merged )
    {
        this.merged = merged;
        fragments = new ArrayList<Atom>();
    }
    /**
     * Make this a nested fraglist
     * @param value true if this fraglist is part of a nested table
     */
    void setNested( boolean value )
    {
        this.nested = value;
    }
    /**
     * Set the id in preparation for printing
     * @param id the id of the current cell
     */
    void setID( int id )
    {
        this.id = id;
    }
    /**
     * Add a fragment to the list
     * @param kind the kind of fragment: aligned, merged etc
     * @param frag the text of the fragment
     * @param versions the set of versions sharing this fragment
     */
    void add( FragKind kind, String frag, BitSet versions )
    {
        Fragment f = new Fragment( kind, frag, versions );
        add( f );   
    }
    /**
     * Add a prepared atom to the list
     * @param a an atom (table or fragment)
     */
    void add( Atom a )
    {
        if ( a.versions.isEmpty() )
            System.out.println("empty!");
        fragments.add( a );
    }
    /**
     * Add a fragment to the start of the list
     * @param kind the kind of fragment: aligned, merged etc
     * @param frag the text of the fragment
     * @param versions  the set of versions sharing this fragment
     */
    void prepend( FragKind kind, String frag, BitSet versions )
    {
        Fragment f = new Fragment( kind, frag, versions );
        prepend( f );   
    }
    /** 
     * Ditto but with a prepared Atom
     * @param a the existing Atom
     */
    void prepend( Atom a )
    {
        fragments.add( 0, a );
    }
    /**
     * Set whether or not this fraglist contains the base version
     * @param isBase 
     */
    void setBase( boolean isBase )
    {
        this.isBase = isBase;
    }
    /**
     * Compute the set of versions shared by all Atoms in the list
     * @return a BitSet
     */
    BitSet getShared()
    {
        BitSet bs = new BitSet();
        for ( int i=0;i<fragments.size();i++ )
        {
            Atom a = fragments.get( i );
            if ( bs.cardinality()==0 ) 
                bs.or( a.versions );
            else
                bs.and( a.versions );
        }
        return bs;
    }
    @Override
    public Object clone()
    {
        FragList copy = new FragList();
        for ( int i=0;i<fragments.size();i++ )
            copy.add( fragments.get(i) );
        copy.editScript = editScript;
        copy.isBase = this.isBase;
        copy.merged = this.merged;
        copy.nested = this.nested;
        return copy;
    }
    void printList( ArrayList<Version> sigla, ArrayList<Group> groups )
    {
        for ( int j=0;j<fragments.size();j++ )
        {
            Atom a = fragments.get( j );
            if ( a instanceof Fragment )
            {
                Fragment f = (Fragment)a;
                System.out.println(Utils.bitSetToString(sigla,groups,f.versions)
                    +" \""+f.contents+"\"" );
            }
        }
    }
    /**
     * Convert the fraglist to formatted HTML 
     */
    public String toJSONString()
    {
        TableCell tc = toTableCell();
        return tc.toJSONString();
    }
    private TableCell toTableCell()
    {
        TableCell tc = new TableCell("td", null);
        String className = null;
        if ( isBase && nested )
            className = "base";
        for ( int i=0;i<fragments.size();i++ )
        {
            Atom a = fragments.get(i);
            if ( className == null && a instanceof Fragment )
                className = ((Fragment)a).getClassName();
            if ( a instanceof Fragment )
                tc.add( ((Fragment)a).contents, className );
            else
                tc.add( a.toString() );
            // reset
            if ( className != null && !className.equals("base") )
                className = null;
        }
        tc.setID( id );
        return tc;
    }
    /**
     * Convert the fraglist to formatted HTML 
     */
    @Override
    public String toString()
    {
        TableCell tc = toTableCell();
        return tc.toString();
    }
    /**
     * Is the textual content of the two fraglists identical? Two fraglists 
     * are also identical if one or the other is base and the other is 
     * aligned with it.
     * @param obj the other frag list
     * @return true if the text is the same else false
     */
    public boolean equals( Object obj )
    {
        if ( obj instanceof FragList )
        {
            FragList fl = (FragList)obj;
            if ( (fl.isBase && merged) || (fl.merged && isBase) )
                return true;
            else if ( fl.fragments.size()==fragments.size() )
            {
                for ( int i=0;i<fragments.size();i++ )
                {
                    Atom f1 = fragments.get(i);
                    Atom f2 = fl.fragments.get(i);
                    if ( f1!=null&&f2!=null )
                    {
                        if ( !f1.equals(f2) )
                            return false;
                    }
                    else if ( f1==null&&f2==null )
                        continue;
                    else
                        return false;
                }
                return true;
            }
        }
        return false;
    }
    /**
     * For convenience this is a method
     * @param a the first of two to compare
     * @param b the second
     * @return the greater of a and b
     */
    int max( int a, int b )
    {
        return (a>b)?a:b;
    }
    /**
     * We can only be finished if both x and y are 1 past the maximum
     * @param x the x coordinate
     * @param y the y coordinate
     * @param fl the other frag list
     * @return true if it is finished
     */
    boolean finished( int x, int y, FragList fl )
    {
        return x==fl.fragments.size()&&y==fragments.size();
    }
    /**
     * Calculate similarity between 2 fraglists using diagonalisation
     * @param fl the other fragment list to compare to
     * @return its similarity as a percentage
     */
    float similarity( FragList fl )
    {
        // first cell may match
        int x = snake(0,0,fl);
        int y = x;
        int D = 0;
        boolean ended = finished( x, y, fl );
        if ( !ended )
        {
            HashSet<Integer> updated = new HashSet<Integer>();
            HashMap<Integer,Step> V = new HashMap<Integer,Step>();
            if ( x > 0 )
                V.put( 0, new Step(null,FragKind.merged,x) );
            else
                V.put( 0, new Step(null,FragKind.empty,x) );
            while ( !ended )
            {
                Set<Integer> keys = V.keySet();
                Integer[] diagonals = new Integer[keys.size()];
                updated.clear();
                keys.toArray( diagonals );
                for ( int i=0;i<diagonals.length;i++ )
                {
                    int k = diagonals[i].intValue();
                    Step kParent = V.get(k);
                    if ( !updated.contains(k) )
                    {
                        x = V.get(k).x;
                        y = x-k;
                        // exchange
                        if ( x < fl.fragments.size() && y < fragments.size() )
                        {
                            int newx = snake(x+1,y+1,fl);
                            int newy = newx-k;
                            ended=finished(newx,newy,fl);
                            updated.add( k );
                            Step s = V.get(k);
                            if ( s != null )
                                s = s.update(FragKind.exchanged,newx);
                            else
                                s = new Step(null,FragKind.exchanged,newx);
                            V.put( k, s );
                        }
                        // insert
                        if ( !ended && x < fl.fragments.size() )
                        {
                            int newx = snake( x+1, y, fl );
                            ended=finished(newx,newx-(k+1),fl);
                            Step s = V.get(k+1);
                            if ( s == null || s.x < newx )
                            {
                                s = kParent.update(FragKind.inserted,newx);
                                updated.add( k+1 );
                            }
                            V.put( k+1, s );
                        }
                        // delete
                        if ( !ended && y < fragments.size() )
                        {
                            int newx = snake(x,y+1,fl);
                            ended=finished(newx,newx-(k-1),fl);
                            Step s = V.get(k-1);
                            if ( s == null || s.x < newx )
                            {
                                s = kParent.update(FragKind.deleted,newx);
                                updated.add( k-1 );
                            }
                            V.put( k-1, s );
                        }
                        if ( ended )
                            break;
                    }
                }
                D++;
            }
            editScript = V.get( fl.fragments.size()-fragments.size() );
        }
        else
            editScript = new Step(null, FragKind.merged, x );
        return computeD( fl );
    }
    /**
     * How far can we extend x by matching fragments?
     * @param x the current x value
     * @param y the current y value
     * @param fl the other fraglist we are comparing to
     * @return a new x value never greater than fl.fragments.size()
     */
    int snake( int x, int y, FragList fl )
    {
        while ( x<fl.fragments.size()&&y<fragments.size() )
        {
            if ( fragments.get(y).equals(fl.fragments.get(x)) )
            {
                x++;
                y++;
            }
            else
                break;
        }
        return x;
    }
    /**
     * Is this fraglist empty?
     * @return true if it is
     */
    boolean isEmpty()
    {
        return fragments.isEmpty();
    }
    /**
     * Create a table if one does not already exist
     * @param table the input table to check if it exists
     * @param versions the sigla needed by the table
     * @param base the base version of the new table
     * @param constraint versions to which this table must be constrained
     * @return the new table or the old one
     */
    Table updateTable( Table table, ArrayList<Version> versions, short base, 
        BitSet constraint )
    {
        if ( table == null )
        {
            table = new Table( constraint, versions, base, table.depth+1 );
        }
        return table;         
    }
    /**
     * The base of a sub-table is the first version of the union of the 
     * two shared version sets
     * @param a the shared versions from the first list
     * @param b the shared versions from the second list
     * @return the base version
     */
    short computeBase( BitSet a, BitSet b )
    {
        a.or( b );
        return (short)a.nextSetBit(0);
    }
    /**
     * Compute the char by char diff between two fraglists from the editscript
     * @param fl the first fraglist
     * @return the difference
     */
    float computeD( FragList fl )
    {
        Stack<Step> stack = new Stack<Step>();
        int x,y;
        Table table = null;
        // reverse editScript using a stack
        Step temp = editScript;
        while ( temp != null )
        {
            stack.push( temp);
            temp = temp.parent;
        }
        // x indexes into fl.fragments, y into fragments
        x = y = 0;
        Atom a,b;
        int len1,len2;
        float total = 0;
        float diff = 0;
        float overall;
        while ( !stack.isEmpty() )
        {
            Step s = stack.pop();
            switch ( s.kind )
            {
                case inserted:
                    a = fl.fragments.get(x++);
                    len1 = a.length();
                    total += len1;
                    diff += len1;
                    break;
                case deleted:
                    a = fragments.get(y++);
                    len2 = a.length();
                    total += len2;
                    diff += len2;
                    break;
                case exchanged:
                    a = fl.fragments.get(x++);
                    b = fragments.get(y++);
                    b.kind = FragKind.deleted;
                    len1 = a.length();
                    len2 = b.length();
                    total += (len1>len2)?len1:len2;
                    diff += (len1>len2)?len1:len2;
                    break;
                case merged:
                    while ( x < s.x )
                    {
                        Atom f1 = fragments.get(y++);
                        x++;
                        total += f1.length();
                    }
                    break;
                case empty:
                    break;
            }
        }
        overall = (total-diff)/total;
        //System.out.println("overall="+overall+": "+this.getContents()
        //    +" fl.contents="+fl.getContents());
        return overall;
    }
    /**
     * Merge two fraglists for real by comparing them and creating an embedded 
     * table in the middle
     * @param fl the other fraglist
     * @param sigla the descriptions of the versions
     * @param constraint nested tables must belong to these versions only
     */
    private void mergeByAtom( FragList fl, ArrayList<Version> sigla, 
        BitSet constraint ) throws Exception
    {
        Stack<Step> stack = new Stack<Step>();
        int x,y;
        Table table = null;
        // reverse editScript using a stack
        Step temp = editScript;
        while ( temp != null )
        {
            stack.push( temp);
            temp = temp.parent;
        }
        // x indexes into fl.fragments, y into fragments
        x = y = 0;
        Atom a,b;
        BitSet bs1,bs2;
        ArrayList<Atom> mergedFrags = new ArrayList<Atom>();
        while ( !stack.isEmpty() )
        {
            Step s = stack.pop();
            switch ( s.kind )
            {
                case inserted:
                    bs1 = fl.getShared();
                    bs1.and( constraint );
                    bs2 = getShared();
                    bs2.and( constraint );
                    Utils.ensureExclusivity( bs1, bs2 );
                    a = fl.fragments.get(x++);
                    // don't create a nested table with a blank row
                    if ( a instanceof Table )
                    {
                        Row r = ((Table)a).getEmptyRow();
                        if ( r != null )
                        {
                            r.addVersions( bs2 );
                            table = (Table)a;
                            break;
                        }
                    }
                    // fall through to here if no blank row
                    table = updateTable( table, sigla, 
                        (short)bs1.nextSetBit(0), constraint );
                    a.versions.and(bs1);
                    a.kind = FragKind.inserted;
                    b = new Fragment(FragKind.deleted,"", bs2);
                    table.assignToRow( a );
                    table.assignToRow( b );
//                    if ( table.rows.size()!=2 )
//                       System.out.println("Table size != 2");
                    break;
                case deleted:
                    bs1 = getShared();
                    bs1.and( constraint );
                    bs2 = fl.getShared();
                    bs2.and( constraint );
                    Utils.ensureExclusivity( bs1, bs2 );
                    a = fragments.get(y++);
                    // don't create a nested table with a blank row
                    if ( a instanceof Table )
                    {
                        Row r = ((Table)a).getEmptyRow();
                        if ( r != null )
                        {
                            r.addVersions( bs2 );
                            table = (Table)a;
                            break;
                        }
                    }
                    // fall through to here if no blank row
                    table = updateTable( table, sigla, 
                        (short)bs1.nextSetBit(0), constraint );
                    a.versions.and(bs1);
                    a.kind = FragKind.deleted;
                    b = new Fragment(FragKind.inserted,"", bs2);
                    table.assignToRow( a );
                    table.assignToRow( b );
//                    if ( b.versions.isEmpty() )
//                    {
//                        System.out.println("empty!");
//                    }
//                    if ( table.rows.size()!=2 )
//                       System.out.println("Table size != 2");
                    break;
                case exchanged:
                    a = fl.fragments.get(x++);
                    a.kind = FragKind.inserted;
                    b = fragments.get(y++);
                    b.kind = FragKind.deleted;
                    bs1 = fl.getShared();
                    bs1.or( getShared() );
                    bs1.and( constraint );
                    table = updateTable( table, sigla, 
                        (short)bs1.nextSetBit(0), bs1 );
                    table.assignToRow( a );
                    table.assignToRow( b );
//                    if ( a.versions.intersects(b.versions) )
//                        System.out.println("versions conflict");
//                    if ( table.rows.size()!=2 )
//                       System.out.println("Table size != 2");
                    break;
                case merged:
                    if ( table != null )
                    {
                        mergedFrags.add(table);
                        table = null;
                    }
                    while ( x < s.x )
                    {
                        Atom f1 = fragments.get(y++);
                        Atom f2 = fl.fragments.get(x++);
                        mergedFrags.add(f1.merge(f2));
                    }
                    break;
                case empty:
                    break;
            }
        }
        if ( table != null )
            mergedFrags.add(table);
        this.fragments = mergedFrags;
    }
    /**
     * Merge two fraglists by just putting them into separate rows of a table
     * @param fl the other fraglist
     * @param sigla the descriptions of the versions
     * @param groups the groups those versions belong to
     * @param constraint nested tables must belong to these versions only
     */
    private void mergeAsTable( FragList fl, ArrayList<Version> sigla, 
        ArrayList<Group> groups, BitSet constraint ) throws Exception
    {
        BitSet bs1 = getShared();
        bs1.and( constraint );
        BitSet bs2 = fl.getShared();
        bs2.and( constraint );
        Utils.ensureExclusivity( bs1, bs2 );
        short base = (short)bs1.nextSetBit(0);
        Table table = new Table( constraint, sigla, base, 0 );
        table.setNested();
        // construct rows
        Row r = new Row( bs1, sigla, groups, base );
        r.setNested( true );
        FragList copy1 = (FragList)this.clone();
        FragList copy2 = (FragList)fl.clone();
        if ( bs1.nextSetBit(base)==base )
            copy1.setBase( true );
        else
            copy2.setBase( true );
        r.add( copy1 );
        Row s = new Row( bs2, sigla, groups, base );
        s.setNested( true );
        s.add( copy2 );
        table.addRow( r );
        table.addRow( s );
        if ( table.rows.size() != 2 )
            System.out.println("merged table does not contain two rows!");
        this.fragments.clear();
        this.fragments.add( table );
        if ( r.versions==null||r.versions.isEmpty() )
            System.out.println("row versions are empty");
        if ( s.versions==null||s.versions.isEmpty() )
            System.out.println("row versions are empty");
    }
    /**
     * Merge one fraglist with another
     * @param fl the other fraglist
     * @param sigla the descriptions of the versions
     * @param groups the groups those versions belong to
     * @param constraint nested tables must belong to these versions only
     */
    void merge( FragList fl, ArrayList<Version> sigla, ArrayList<Group> groups, 
        BitSet constraint ) throws Exception
    {
        if ( !fl.isEmpty() && isEmpty() )
            this.fragments = fl.fragments;
        else if ( !fl.isEmpty() && !isEmpty() )
        {
            // ensure that the edit script is fresh
            float sim = similarity( fl );
            if ( sim >= Atom.THRESHOLD_SIM )
                mergeByAtom( fl, sigla, constraint);
            else
                mergeAsTable( fl, sigla, groups, constraint );
        }
    }
    /**
     * Get the text up to the next space or the end of the list
     * @return a String bounded on the right by a space
     */
    String getToNextBreakPoint( String value )
    {
        int spacePos = indexOfBreakPoint( value );
        if ( spacePos == -1 )
            return value;
        else
        return value.substring(0,spacePos);
    }
    int indexOfBreakPoint( String src )
    {
        for ( int i=0;i<src.length();i++ )
            if ( !Character.isLetter( src.charAt(i) ) )
                return i;
        return -1;
    }
    int lastIndexOfBreakPoint( String src )
    {
        for ( int i=src.length()-1;i>=0;i-- )
            if ( !Character.isLetter( src.charAt(i) ) )
                return i+1;
        return -1;
    }
    /**
     * Get the last token bounded by a space or the whole list if not
     * @return a String
     */
    private String getToPrevBreakPoint( String value )
    {
        int spacePos = lastIndexOfBreakPoint(value);
        if ( spacePos == -1 )
            return value;
        else
            return value.substring(spacePos);
    }
    /**
     * Make sure we don't expand the same text twice into the adjacent 
     * merged cell
     * @param token the token to be extended to
     * @param mergedCell the full contents of the original merged cell
     * @param cell the cell to test for doubling up
     * @return true if doubling up would occur
     */
    boolean doubledUp( String token, String mergedCell, FragList cell )
    {
        if ( token.equals(mergedCell) )
        {
            if ( cell.fragments.size()==1 )
            {
                Atom a = cell.fragments.get(0);
                if ( a instanceof Fragment )
                {
                    Fragment f = (Fragment)a;
                    if ( f.contents!=null&&f.contents.equals(token) )
                        return true;
                }
            }
        }
        return false;
    }
    /**
     * Extend the merged cell fl on our left from our cell
     * @param prev a merged cell to our left
     * @param mergedCell the merged cell's full contents
     * @param base the base version of the merged cell
     */
    void extendLeft( FragList prev, String mergedCell, short base )
    {
        // find the next space in the merged cell
        String token = getToPrevBreakPoint( mergedCell );
        if ( !doubledUp(token,mergedCell,prev) && token.length() > 0 
            && fragments.size()>0 )
        {
            Atom first = this.fragments.get(0);
            if ( !first.startsWithBreakPoint() )
            {
                Fragment f2 = new Fragment( FragKind.aligned, 
                    token, getShared() );
                f2.setStyle( "right" );
                // add the extended fraglist to the merged section
                if ( !prev.isEmpty() )
                {
                    for ( int i=0;i<prev.fragments.size();i++ )
                    {
                        Atom a = prev.fragments.get(i);
                        if ( a instanceof Fragment )
                        {
                            Fragment g = (Fragment) a;
                            if ( g.style==null )
                                return;
                        }
                    }
                    prev.add( f2 );
                }
                else
                    prev.add( f2 );
            }
        }
    }
    /**
     * Extend the merged s on our right using our section
     * @param next the next cell which should be merged
     * @param mergedCell the merged cell contents to our right
     * @param base the base version of the merged section
     */
    void extendRight( FragList next, String mergedCell, short base )
    {
        // find the next space in the merged cell
        String token = getToNextBreakPoint( mergedCell );
        if ( token!=null&&token.equals(".") )
            System.out.println(".");
        if ( !doubledUp(token,mergedCell,next) && token.length() > 0 
            && fragments.size()>0 )
        {
            Atom last = fragments.get(fragments.size()-1);
            if ( !last.endsWithBreakPoint() )
            {
                Fragment f2 = new Fragment( FragKind.aligned, 
                    token, getShared() );
                f2.setStyle( "left" );
                // add the extended fraglist to the merged section
                if ( !next.isEmpty() )
                {
                    for ( int i=0;i<next.fragments.size();i++ )
                    {
                        Atom a = next.fragments.get(i);
                        if ( a instanceof Fragment )
                        {
                            Fragment g = (Fragment) a;
                            if ( g.style==null )
                                return;
                        }
                    }
                    next.fragments.add( 0, f2 );
                }
                else
                    next.add( f2 );
            }
        }
    }
    /**
     * For testing
     * @param args unused
     */
    public static void main( String[] args )
    {
        try
        {
            FragList fl1 = new FragList();
            FragList fl2 = new FragList();
            BitSet bs = new BitSet();
            ArrayList<Version> vt = new ArrayList<Version>();
            ArrayList<Group> groups = new ArrayList<Group>();
            fl1.add( FragKind.aligned, "banana", bs );
            fl2.add( FragKind.inserted, "banana", bs );
            fl1.add( FragKind.aligned, "apple", bs );
            fl2.add( FragKind.inserted, "pear", bs );
            fl1.add( FragKind.aligned, "mango", bs );
            fl2.add( FragKind.inserted, "mango", bs );
            fl1.add( FragKind.aligned, "grape", bs );
            fl2.add( FragKind.inserted, "grape", bs );
            fl1.add( FragKind.aligned, "orange", bs );
            fl2.add( FragKind.inserted, "lemon", bs );
            fl1.add( FragKind.aligned, "mandarin", bs );
            fl2.add( FragKind.inserted, "cumquat", bs );
            float percent = fl1.similarity(fl2);
            fl1.merge( fl2, vt, groups, bs );
        }
        catch ( Exception e )
        {
            e.printStackTrace( System.out );
        }
    }
    /**
     * Get the contents of this fraglist
     * @return a String
     */
    String getContents()
    { 
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<fragments.size();i++ )
            sb.append(fragments.get(i).getContents());
        return sb.toString();
    }
}

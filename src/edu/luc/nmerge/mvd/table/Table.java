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
import java.util.Set;
import java.util.Iterator;
/**
 * Represent the table as a set of rows
 * @author desmond
 */
public class Table extends Atom
{
    ArrayList<Row> rows;
    ArrayList<Version> sigla;
    ArrayList<Group> groups;
    short base;
    int id;
    String tableId;
    /** depth of nesting */
    int depth;
    /**
     * Create a table    
     * @param bs the set of versions encompassed by this table
     * @param sigla the sigla (version descriptions)
     * @param base the base version
     * @param depth the depth of nesting
     */
    Table( BitSet bs, ArrayList<Version> sigla, short base, int depth )
    {
        rows = new ArrayList<Row>();
        this.sigla = sigla;
        this.versions = bs;
        this.base = base;
        this.depth = depth;
    }
    /**
     * Do we already have an empty row?
     * @return the empty row if any
     */
    public Row getEmptyRow()
    {
        for ( int i=0;i<rows.size();i++ )
        {
            Row r = rows.get( i );
            if ( r.isEmpty() )
                return r;
        }
        return null;
    }
    /**
     * Get the overall length of this table in characters
     * @return its length in characters
     */
    public int length()
    {
        int total = 0;
        for ( int i=0;i<rows.size();i++ )
        {
            Row r = rows.get( i );
            for ( int j=0;j<r.cells.size();j++ )
            {
                FragList fl = r.cells.get( j );
                for ( int k=0;k<fl.fragments.size();k++ )
                {
                    Atom a = fl.fragments.get( k );
                    total += a.length();
                }
            }
        }
        return total;
    }
    boolean nested()
    {
        return depth > 0;
    }
    void setNested()
    {
        depth++;
    }
    /**
     * Set the id for printing
     * @param id the first cell id of the base row
     */
    void setID( int id )
    {
        this.id = id;
    }
    /**
     * Set the entire table's id
     * @param tableId the id as a String
     */
    void setTableId( String tableId )
    {
        this.tableId = tableId;
    }
    /**
     * Is one bitset the subset or equal to the second?
     * @param a the first is it subset of b
     * @param b the second superset or equal
     * @return true if a is a subset of b
     */
    boolean isSubSet( BitSet a, BitSet b )
    {
        for (int i = a.nextSetBit(0); i >= 0; i = a.nextSetBit(i+1)) 
        {
            if ( b.nextSetBit(i)!=i )
                return false;
        }
        return true;
    }
    /**
     * Add a row to the table
     * @param row the new row
     */
    void addRow( Row row )
    {
        rows.add( row );
        this.versions.or( row.versions );
    }
    /**
     * Try to find an existing row that can take this atom
     * @param a the atom to add
     * @return true if it was placed, else false
     */
    private boolean addToExisting( Atom a )
    {
        for ( int i=0;i<rows.size();i++ )
        {
            Row r = rows.get(i);
            if ( a.versions.intersects(r.versions) )
            {
                FragList tail = r.cells.get(r.cells.size()-1);
                tail.add( a );
                return true;
            }
        }
        return false;
    }
    /**
     * Assign an atom (fragment or another table) to a new row
     * @param a the atom
     * @throws an exception if the row versions are not mutually exclusive
     */
    void assignToRow( Atom a ) throws Exception
    {
        if ( !addToExisting(a) )
        {
            Row r = new Row( sigla, groups, base );
            // this will always be a nested row
            r.setNested( this.nested() );
            FragList fl = new FragList();
            fl.setBase( a.versions.nextSetBit(base)==base );
            fl.add( a );
            r.add( fl );
            r.versions.or( a.versions );
            // contraint it to the versions of the table
            r.versions.and( this.versions );
            addRow( r );
            //System.out.println("Created row "+Utils.bitSetToString(sigla,
            //    r.versions));
            if ( r.versions==null || r.versions.isEmpty() )
                System.out.println("row versions are empty");
        }
    }
    /**
     * Merge an Atom into this table
     * @param other the other Atom (fragment or table)
     * @return the merged object (this)
     * @throws Exception 
     */
    Atom merge( Atom other ) throws Exception
    {
        if ( other instanceof Fragment )
        {
            // if the other is a fragment then add it as a new row
            Fragment f = (Fragment)other;
            if ( !contains(f) )
            {
                Row r = new Row( sigla, groups, base );
                r.setNested( this.nested() );
                r.add( other );
                r.versions.or(f.versions);
                addRow( r );
            }
        }
        else 
        {
            // else if it is another table merge its rows with ours
            // versions must not intersect or throw an exception
            Table u = (Table) other;
            for ( int i=0;i<u.rows.size();i++ )
            {
                Row r = u.rows.get(i);
                addRow( r );
            }
        }
        return this;
    }
    /**
     * Conversion to a string (HTML only)
     * @return a HTML fragment
     */
    @Override
    public String toString()
    {
        boolean set = false;
        StringBuilder sb = new StringBuilder();
        int baseRow = -1;
        sb.append("<table");
        if ( this.depth > 0 )
        {
            String localId = RandomId.getId( 32 );
            sb.append(" id=\"");
            sb.append( localId );
            sb.append( "\" class=\"inline\"");
        }
        else if ( tableId != null && tableId.length()>0 )
            sb.append( " id=\""+tableId+"\"");
        sb.append(">");
        // print out the rows
        // ensure base version is at bottom
        for ( int i=0;i<rows.size();i++ )
        {
            Row r = rows.get(i);
            if ( r.versions.nextSetBit(base)!=base )
                sb.append( r.toString() );
            else if ( !set )
            {
                baseRow = i;
                set = true;
            }
            else
                System.out.println("base already set!");
        }
        if ( baseRow == -1 )
            System.out.println("base row not found!");
        else
        {
            Row br = rows.get(baseRow);
            br.setID( id );
            sb.append( br.toString() );
        }
        sb.append("</table>");
        return sb.toString();
    }
    /**
     * Convert the table to a JSON representation
     * @param options include these in the json output
     * @return a String being a JSON document
     */
    public String toJSONString( HashMap<String,String> options )
    {
        boolean set = false;
        StringBuilder sb = new StringBuilder();
        int baseRow = -1;
        sb.append("{");
        // put table contents here
        if ( this.depth > 0 )
        {
            String localId = RandomId.getId( 32 );
            sb.append("\"id\":\"");
            sb.append( localId );
            sb.append( "\"," );
            sb.append("\"class\":\"inline\", ");
        }
        if ( options != null )
        {
            sb.append("\"options\": {");
            Set<String> keys = options.keySet();
            Iterator<String> iter = keys.iterator();
            while ( iter.hasNext() )
            {
                String key = iter.next();
                sb.append("\"");
                sb.append(key);
                sb.append("\": \"");
                sb.append(options.get(key));
                sb.append("\"");
                if ( iter.hasNext() )
                    sb.append(", ");
            }
            sb.append("}, ");
        }
        sb.append( "\"rows\":[" );
        // print out the rows
        // ensure base version is at bottom
        for ( int i=0;i<rows.size();i++ )
        {
            Row r = rows.get(i);
            if ( r.versions.nextSetBit(base)!=base )
            {
                sb.append( r.toJSONString() );
                sb.append(",");
            }
            else if ( !set )
            {
                baseRow = i;
                set = true;
            }
            else
                System.out.println("base already set!");
        }
        if ( baseRow == -1 )
            System.out.println("base row not found!");
        else
        {
            Row br = rows.get(baseRow);
            br.setID( id );
            sb.append( br.toJSONString() );
        }
        sb.append("]}");
        return sb.toString();
    }
    /**
     * Get the designated row (used when building a table)
     * @param i the index into the rows table
     * @return null if not there else the row
     */
    Row getRow( int i )
    {
        if ( i >= rows.size() )
            return null;
        else
            return rows.get(i);
    }
    /**
     * Compact a table by merging rows of high similarity
     * @throws Exception 
     */
    public void compact() throws Exception
    {
        ArrayList<Row> delenda = new ArrayList<Row>();
        for ( int i=0;i<rows.size();i++ )
        {
            for ( int j=0;j<rows.size();j++ )
            {
                if ( i != j )
                {
                    Row r = rows.get( i );
                    Row s = rows.get( j );
                    if ( !delenda.contains(r) && !delenda.contains(s) )
                    {
                        if ( r.merge(s) )
                        {
                            delenda.add( s );
                        }
                    }
                }
            }
        }
        for ( int i=0;i<delenda.size();i++ )
        {
            rows.remove( delenda.get(i) );
        }
    }
    /**
     * Is the given fragment already in a row if this table?
     * @param f the fragment to test
     * @return true if it is there already else false
     */
    boolean contains( Fragment f )
    {
        for ( int i=0;i<rows.size();i++ )
        {
            Row r = rows.get(i);
            if ( r.cells.size()==1 )
            {
                FragList fl = r.cells.get( 0 );
                if ( fl.fragments.size()==1 )
                {
                    Atom a = fl.fragments.get(0);
                    if ( a instanceof Fragment )
                    {
                        Fragment g = (Fragment)a;
                        if ( f.versions.equals(g.versions) 
                            && f.contents.equals(g.contents) )
                            return true;
                    }
                }
            }
        }
        return false;
    }
    /**
     * A hard test of equality. Mostly answers false
     * @param other another table or atom
     * @return false or true if the two tables cover the same set of versions 
     */
    @Override
    public boolean equals( Object other )
    {
        if ( other instanceof Table )
        {
            Table t = (Table)other;
            if ( t.versions.equals(versions)&&t.rows.size()==rows.size() )
            {
                for ( int i=0;i<rows.size();i++ )
                {
                    Row r1 = rows.get( i );
                    Row r2 = t.rows.get( i );
                    if ( r1.equals(r2) )
                        return false;
                }
                return true;
            }
        }
        else if ( other instanceof Fragment )
        {
            // the fragment might already be IN the table
            return this.contains( (Fragment)other );
        }
        return false;
    }
    /**
     * Required by equals
     * @return a hash of this object's contents
     */
    @Override
    public int hashCode() 
    {
        int hash = 7;
        hash = 97 * hash + (this.rows != null ? this.rows.hashCode() : 0);
        hash = 97 * hash + this.base;
        return hash;
    }
    /**
     * Get an array of the contents of the base version's merged cells, 
     * with blanks for the other cells.
     * @return an array of String the same length as the base row
     * @throws Exception if an embedded table occurs in a merged cell
     */
    private String[] getMergedCells() throws Exception
    {
        String[] array = null;
        Row r = null;
        for ( int i=0;i<rows.size();i++ )
        {
            r = rows.get(i);
            if ( r.versions.nextSetBit(base)==base )
                break;
            else
                r = null;
        }
        if ( r != null )
        {
            array = new String[r.cells.size()];
            for ( int i=0;i<r.cells.size();i++ )
            {
                FragList fl = r.cells.get(i);
                if ( fl.merged  )
                {
                    StringBuilder sb = new StringBuilder();
                    for ( int j=0;j<fl.fragments.size();j++ )
                    {
                        Atom a = fl.fragments.get(j);
                        if ( a instanceof Fragment )
                            sb.append( ((Fragment)a).contents );
                        else 
                            throw new Exception("embedded table in merged cell");
                    }
                    array[i] = sb.toString();
                }
                else
                    array[i] = null;
            }
        }
        return array;
    }
    /**
     * Extend fragments to the next space in adjacent merged sections
     */
    void extendToWholeWords() throws Exception
    {
        String[] mergedCells = getMergedCells();
        for ( int i=0;i<rows.size();i++ )
        {
            Row r = rows.get(i);
            for ( int j=0;j<r.cells.size();j++ )
            {
                FragList fl = r.cells.get(j);
                if ( !fl.merged )
                {
                    // try to extend left
                    if ( j>0&&r.cells.get(j-1).merged )
                    {
                        FragList fl2 = r.cells.get(j-1);
                        fl.extendLeft( fl2, mergedCells[j-1], base );
                    }
                    // try to extend right
                    if ( j<r.cells.size()-1 && mergedCells[j+1] != null )
                    {
                        FragList fl2 = r.cells.get(j+1);
                        fl.extendRight( fl2, mergedCells[j+1], base );
                    }
                }
            }
        }
    }
    /**
     * Test if the contents of the base version start with a break point 
     * @return true if it does
     */
    boolean startsWithBreakPoint()
    {
        for ( int i=0;i<rows.size();i++ )
        {
            Row r = rows.get(i);
            if ( r.cells.size()>0 && r.versions.nextSetBit(base)==base )
            {
                FragList fl = r.cells.get(0);
                if ( fl.fragments.size()>0 )
                {
                    Atom a = fl.fragments.get(0);
                    return a.startsWithBreakPoint();
                }
            }
        }
        return false;
    }
    /**
     * Test if the contents of the base version ends with a break point 
     * @return true if it does
     */
    boolean endsWithBreakPoint()
    {
        for ( int i=0;i<rows.size();i++ )
        {
            Row r = rows.get(i);
            if ( r.cells.size()>0 && r.versions.nextSetBit(base)==base )
            {
                FragList fl = r.cells.get(r.cells.size()-1);
                if ( fl.fragments.size()>0 )
                {
                    Atom a = fl.fragments.get(fl.fragments.size()-1);
                    return a.startsWithBreakPoint();
                }
            }
        }
        return false;
    }
    /**
     * Debug: check that the row versions are exclusive
     * @return true if it was OK, false if it was not
     */
    boolean checkRowVersions()
    {
        BitSet bs = new BitSet();
        for ( int i=0;i<rows.size();i++ )
        {
            Row r = rows.get(i);
            if ( bs.intersects(r.versions) )
            {
                System.out.println("Overlapping version in row "
                    +Utils.bitSetToString(sigla,groups,r.versions));
                return false;
            }
            bs.or( r.versions );
        }
        return true;
    }
    /**
     * Get the contents of this thing
     * @return the text
     */
    public String getContents()
    {
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<rows.size();i++ )
        {
            Row r = rows.get(i);
            sb.append( r.getContents() );
        }
        return sb.toString();
    }
    
}
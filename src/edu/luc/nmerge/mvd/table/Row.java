/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.luc.nmerge.mvd.table;
import edu.luc.nmerge.mvd.Version;
import java.util.ArrayList;
import java.util.BitSet;
/**
 * Rearrange data into a HTML tabular form: one row per version
 */
class Row
{
    ArrayList<FragList> cells;
    /** description of versions */
    ArrayList<Version> sigla;
    /** Set of versions represented by all fraglists in this row (ANDed) */
    BitSet versions;
    /** the base version which we might not contain */
    short base;
    /** true if this row is part of a nested table */
    boolean nested;
    /**
     * id of first table cell
     */
    int id;
    Row( BitSet versions, ArrayList<Version> sigla, short base )
    {
        this.versions = (BitSet)versions.clone();
        this.sigla = sigla;
        this.base = base;
        cells = new ArrayList<FragList>();
    }
    Row( ArrayList<Version> sigla, short base )
    {
        this.versions = new BitSet();
        this.base = base;
        this.sigla = sigla;
        cells = new ArrayList<FragList>();
    }
    /**
     * Set the nested flag for this row 
     * @param value true if this row is part of a nested table
     */
    void setNested( boolean value )
    {
        this.nested = value;
    }
    /**
     * Set the ID for printing
     * @param id the first ID of the first base cell
     */
    void setID( int id )
    {
        this.id = id;
    }
    /**
     * Add an atom to the end of the list
     * @param a the atom to add
     */
    void add( Atom a )
    {
        FragList fl;
        if ( cells.isEmpty() )
        {
            fl = new FragList();
            fl.setNested( this.nested );
            fl.add( a );
            add( fl );
        }
        else
        {
            fl = cells.get(cells.size()-1);
            fl.add( a );
            this.versions.and(fl.getShared());
        }
    }
    /**
     * Add a fraglist to the row. 
     * @param fl the fraglist to add
     */
    void add( FragList fl )
    {
        if ( fl == null )
            fl = new FragList();
        cells.add( fl );
        fl.setNested( this.nested );
    }
    void printRow()
    {
        for ( int i=0;i<cells.size();i++ )
        {
            FragList fl = cells.get(i);
            fl.printList( sigla );
        }
    }
    /**
     * Compute the similarity between 2 rows
     * @param r the other row
     * @return the fraction of fragments shared between the 2
     */
    float similarity( Row r )
    {
        float sim = 0.0f;
        for ( int i=0;i<cells.size();i++ )
        {
            FragList fl1 = cells.get(i);
            FragList fl2 = r.cells.get(i);
            if ( fl1.merged&&fl2.merged )
                sim += 1.0f/cells.size();
            else if ( fl1.equals(fl2) )
                sim += 1.0f/cells.size();
            else
                sim += fl1.similarity(fl2)/cells.size();
        }
        return sim;
    }
    /**
     * Try to merge another row with this one
     * @param r the row to merge with it
     * @return true if a merge took place, else false
     */
    boolean merge( Row r ) throws Exception
    {
        float sim = similarity( r );
        if ( sim >= Atom.THRESHOLD_SIM )
        {
            //System.out.println("Merging "+Utils.bitSetToString(sigla,versions)
            //    +" with "+Utils.bitSetToString(sigla,r.versions)+" sim="+sim);
            BitSet constraint = new BitSet();
            constraint.or( this.versions );
            constraint.or( r.versions );
            for ( int i=0;i<cells.size();i++ )
            {
                FragList fl1 = cells.get(i);
                FragList fl2 = r.cells.get(i);
                fl1.merge(fl2,sigla,constraint);
            }
            this.versions.or( r.versions );
            return true;
        }
        else
            return false;
    }
    /**
     * Does this row NOT represent the base version?
     * @return true if it doesn't else false
     */
    boolean isHidden()
    {
        return nested&&versions.nextSetBit(base)!=base;
    }
    /**
     * Convert to HTML
     * @return a String
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<tr");
        //boolean rowWithNesting = hasNestedTable();
        if ( isHidden() )
            sb.append(" class=\"hidden\"" );
        //else if ( rowWithNesting )
        //    sb.append(" class=\"nested\"" );
        if ( nested )
            sb.append("><td class=\"siglumhidden\">");
        else
            sb.append("><td class=\"siglum\">");
        // write siglum
        String versionsString = Utils.bitSetToString(sigla,versions);
        sb.append( versionsString );
        sb.append("</td>");
        for ( int i=0;i<cells.size();i++ )
        {
            FragList fl = cells.get(i);
            if ( fl.merged && fl.isBase )
                fl.setID( id++ );
            sb.append( fl.toString() );
        }
        sb.append("</tr>");
        return sb.toString();
    }
    /**
     * A stringent test of equality
     * @param obj another object to compare ourselves to
     */
    public boolean equals( Object obj )
    {
        if ( obj instanceof Row )
        {
            Row r = (Row)obj;
            if ( r.versions.equals(versions) && r.cells.size()==cells.size() )
            {
                for ( int i=0;i<cells.size();i++ )
                {
                    FragList fl1 = cells.get(i);
                    FragList fl2 = r.cells.get(i);
                    if ( !fl1.equals(fl2) )
                        return false;
                }
                return true;
            }
        }
        return false;
    }
    /**
     * Debug: get contents of row
     * @return a String
     */
    String getContents()
    {
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<cells.size();i++ )
            sb.append( cells.get(i).getContents() );
        return sb.toString();
    }
}

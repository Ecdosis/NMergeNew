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
import java.util.*;
import edu.luc.nmerge.mvd.Version;
import edu.luc.nmerge.mvd.Group;

/**
 * Represent a table of variants for a range in the base text. 
 * <p>The method is to assign pairs from the MVD to sections where either
 * all versions share the same text or not. So we alternate merged,not-merged,
 * merged etc. In each not-merged section we maintain separate lists for 
 * each version.</p>
 * <p>To convert this into a table (toTable) we go through the sections,  
 * by tracking which sets of versions share the same text throughout 
 * (computeRows). In the worst case this will be all the versions one by one, 
 * but usually there are some versions that share the same text throughout. 
 * Optionally we may copy the merged text into the other rows 
 * (Options.HIDE_MERGED=false), but by default we omit it. Also optionally 
 * we may merge rows that are nearly the same (Options.COMPACT=true). This 
 * produces small sections that are initially collapsed as sub-tables but may 
 * be expanded by clicking on them. The other text of a compacted row is just
 * the same and the versions are those of the two rows together.</p>
 * @author desmond 3/6/2012
 */
public class TableView 
{
    ArrayList<Section> sections;
    ArrayList<Version> sigla;
    ArrayList<Group> groups;
    EnumMap<Options,Object> options;
    boolean hideMerged;
    Section current;
    int firstID;
    String tableId;
    /** all the versions we are considering */
    BitSet all;
    short base;
    /**
     * Construct a table view
     * @param sigla an array of Versions from the MVD
     * @param base the designated base version
     * @param options a set of key-value pair options
     */
    public TableView( ArrayList<Version> sigla, ArrayList<Group> groups, 
        short base, BitSet all, EnumMap<Options,Object> options )
    {
        sections = new ArrayList<Section>();
        this.sigla = sigla;
        this.groups = groups;
        this.base = base;
        this.options = options;
        Integer id = (Integer)options.get(Options.FIRST_MERGEID);
        this.firstID = id.intValue();
        this.tableId = (String)options.get(Options.TABLE_ID);
        this.all = all;
        this.hideMerged = getBooleanOption(Options.HIDE_MERGED);
    }
    public int[] getSectionStats()
    {
        int[] offsets = new int[sections.size()];
        for ( int i=0;i<offsets.length;i++ )
            offsets[i] = sections.get(i).getOffset();
        return offsets;
    }
    /**
     * Add a fragment and decide whether or not to create a new section
     * @param kind the fragment kind
     * @param versions the versions of the fragment
     * @param offset the starting point in base
     * @param frag the text of the fragment
     */
    public void addFragment( FragKind kind, int offset, BitSet versions, String frag )
    {
        BitSet missing=null;
        if ( kind == FragKind.almost )
        {
            missing = (BitSet)current.getVersions().clone();
            missing.xor(versions);
        }
        if ( sections.isEmpty() || SectionState.state(kind) != current.state )
        {
            sections.add( new Section() );
            current = sections.get(sections.size()-1 );
        }
        if ( kind == FragKind.merged && hideMerged )
            current.addFrag( kind, base, offset, versions, frag );
        else if ( kind == FragKind.almost )
        {
            current.addAlmostSet( offset, versions, missing, frag );
        }
        else
            current.addFragSet( kind, offset, versions, frag );
    }
    /**
     * Get the versions of the current segment
     * @return a bitset
     */
    public BitSet getCurrentVersions()
    {
        return (current==null)?this.all:current.getVersions();
    }
    /**
     * Clear the current version which has now ended
     * @param v a version id starting at 1
     */
    public void clearCurrent( short v )
    {
        if ( current != null )
            current.versions.clear(v);
    }
    /**
     * Debug routine
     * @param barray the array of version IDs to dump
     */
    private void dumpBArray( BitSet[] barray )
    {
        for ( int i=0;i<barray.length;i++ )
        {
            StringBuilder sb = new StringBuilder();
            for (int j = barray[i].nextSetBit(1); j>= 0; 
                j = barray[i].nextSetBit(j+1))
            {
                if ( j <= sigla.size()&&j>0 )
                {
                    Version v = sigla.get( j-1 );
                    sb.append( v.shortName );
                    sb.append( " " );
                }
            }
            System.out.println( sb.toString() );
        }
    }
    /**
     * Adjust versions in the bitsets based on the current shared versions
     * @param sets the sets representing each row
     * @param shared the versions shared by all fragments in the fraglist
     * @param key the version key of the set to adjust
     */
    private void adjustSet( HashMap<Short,BitSet> sets, BitSet shared, 
        short key )
    {
        BitSet set = sets.get( key );
        if ( !set.equals(shared) )
        {
            BitSet intersection = (BitSet)set.clone();
            intersection.and( shared );
            sets.put( key, intersection );
            // 
            BitSet diff = (BitSet)set.clone();
            diff.andNot( shared );
            for ( int j=diff.nextSetBit(0);j>=1;
                j=diff.nextSetBit(j+1) )
            {
                BitSet current = sets.get( (short)j );
                current.and( diff );
                sets.put( (short)j, current );
            }
        }
    }
    /**
     * Work out the rows that are needed. Sets must be mutually exclusive.
     * @return a compact array of different version-sets
     */
    BitSet[] computeRows()
    {
        HashMap<Short,BitSet> sets = new HashMap<Short,BitSet>();
        for (int i=all.nextSetBit(0);i>=0;i=all.nextSetBit(i+1)) 
        {
            sets.put( (short)i, (BitSet)all.clone() );
        }
        for ( int i=0;i<sections.size();i++ )
        {
            Section s = sections.get(i);
            if ( s.state==SectionState.merged && getBooleanOption(Options.HIDE_MERGED) )
            {
                for (int j=all.nextSetBit(0);j>=0;j=all.nextSetBit(j+1)) 
                    adjustSet( sets, all, (short)j );
            }
            else
            {
                Set<Short> keys = s.lists.keySet();
                Iterator<Short> iter = keys.iterator();
                while ( iter.hasNext() )
                {
                    short key = iter.next();
                    FragList fl = s.lists.get( key );
                    BitSet bs = fl.getShared();
                    adjustSet( sets, bs, key );
                }
            }
        }
        Set<Short> bsKeys = sets.keySet();
        HashSet<BitSet> map = new HashSet<BitSet>();
        Iterator<Short> iter2 = bsKeys.iterator();
        while ( iter2.hasNext() )
        {
            Short v = iter2.next();
            map.add( sets.get(v) );
        }
        BitSet[] barray = new BitSet[map.size()];
        map.toArray(barray);
        return barray;
    }
    /**
     * DEBUG: Check mutual exclusivity of the sets of versions
     * @param barray the array of bitsets to test
     * @throws Exception 
     */
    void checkExclusivity( BitSet[] barray ) throws Exception
    {
        for ( int i=0;i<barray.length;i++ )
        {
            for ( int j=0;j<barray.length;j++ )
            {
                if ( i != j )
                {
                    BitSet bs = new BitSet();
                    bs.or( barray[i] );
                    bs.and(barray[j]);
                    if ( bs.cardinality()!=0 )
                        throw new Exception("bitsets not mutually exclusive");
                }
            }
        }
    }
    /**
     * Get the simple boolean value of an option
     * @param key the option-key
     * @return true or false
     */
    boolean getBooleanOption( Options key )
    {
        Boolean b = (Boolean)options.get( key );
        if ( b != null )
            return b.booleanValue();
        else
            return false;
    }
    /**
     * Convert a TableView object to an array of Rows
     * @return a table of Rows
     */
    private Table toTable() throws Exception
    {
        BitSet[] rowSets = computeRows();
        //dumpBArray( rowSets );
        checkExclusivity( rowSets );
        // get the options which are all acted on here
        boolean extend = getBooleanOption( Options.WHOLE_WORDS );
        boolean compact = getBooleanOption( Options.COMPACT );
        boolean hideMerged = getBooleanOption( Options.HIDE_MERGED );
        BitSet bs = new BitSet();
        for ( int i=0;i<rowSets.length;i++ )
        {
            bs.or( rowSets[i] );
        }
        Table table = new Table( bs, sigla, base, 0 );
        table.setID( this.firstID );
        table.setTableId( tableId );
        for ( int i=0;i<sections.size();i++ )
        {
            Section s = sections.get( i );
            for ( int j=0;j<rowSets.length;j++ )
            {
                int v = rowSets[j].nextSetBit(0);
                Row r = table.getRow( j );
                if ( r == null )
                {
                    r = new Row( rowSets[j], sigla, groups, base );
                    table.addRow( r );
                }
                FragList fl = s.lists.get((short)v);
                if ( s.state == SectionState.merged )
                {
                    if ( fl == null )
                        fl = new FragList( true );
                    else
                        fl.merged = true;
                }
                if ( rowSets[j].nextSetBit(base)==base )
                {
                    if ( fl==null )
                        fl = new FragList();
                    fl.setBase( true );
                }
                r.add( fl );
            }
        }
        //table.checkRowVersions();
        if ( compact )
            table.compact();
        //table.checkRowVersions();
        if ( extend && hideMerged )
            table.extendToWholeWords();
        return table;
    }
    /**
     * Convert to HTML the hard way
     * @return a HTML fragment as a table
     */
    @Override
    public String toString()
    {
        String str = null;
        try
        {
            str = toTable().toString();
        }
        catch ( Exception e )
        {
            e.printStackTrace(System.out);
        }
        return str;
    }
    /**
     * Convert to HTML the hard way
     * @param options include these in JSON output
     * @return a HTML fragment as a table
     */
    public String toJSONString( HashMap<String,String> options )
    {
        String str = null;
        try
        {
            str = toTable().toJSONString(options);
        }
        catch ( Exception e )
        {
            e.printStackTrace(System.out);
        }
        return str;
    }
}
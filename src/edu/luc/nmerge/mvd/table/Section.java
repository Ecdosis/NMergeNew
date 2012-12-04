/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.luc.nmerge.mvd.table;
import java.util.*;
/**
 * A section within a table
 */
public class Section
{
    HashMap<Short,FragList> lists;
    boolean merged;
    Section()
    {
        lists = new HashMap<Short,FragList>();
    }
    BitSet getVersions()
    {
        BitSet bs = new BitSet();
        Set<Short> keys = lists.keySet();
        Iterator<Short> iter = keys.iterator();
        while ( iter.hasNext() )
        {
            bs.set( iter.next().shortValue() );
        }
        return bs;
    }
    /**
     * Add a fragment belonging to just one version
     * @param kind the fragment kind
     * @param base the version of the fragment
     * @param bs the set of versions sharing this frag
     * @param frag the actual fragment
     */
    void addFrag( FragKind kind, short base, BitSet bs, String frag )
    {
        FragList fl;
        if ( kind==FragKind.merged )
            this.merged = true;
        if ( !lists.containsKey(base) )
        {
            fl = new FragList();
            lists.put(base,fl);
        }
        fl = lists.get(base);
        fl.add( kind, frag, bs );
    }
    /**
     * Add a frag that belongs to a set of versions
     * @param kind the fragment kind
     * @param bs the set of versions it belongs to
     * @param frag the text of the fragment
     */
    void addFragSet( FragKind kind, BitSet bs, String frag )
    {
        if ( kind == FragKind.merged )
            this.merged = true;
        for (int i = bs.nextSetBit(1); i>= 0; 
                i = bs.nextSetBit(i+1))
        {
            if ( lists.containsKey((short)i) )
            {
                FragList fl = lists.get( (short)i );
                fl.add( kind, frag, bs );
            }
            else
            {
                FragList fl = new FragList();
                fl.add( kind, frag, bs );
                lists.put( (short)i, fl );
            }
        }
    }
}
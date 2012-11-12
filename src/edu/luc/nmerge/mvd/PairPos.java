/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.luc.nmerge.mvd;

/**
 * Represent a position in a pair
 * @author desmond
 */
public class PairPos 
{
    /** index of the pair */
    int index;
    /** position within the pair */
    int position;
    /** version it is anchored to */
    short base;
    public PairPos( int index, int position, short base )
    {
        this.index = index;
        this.position = position;
        this.base = base;
    }
    int getPosition()
    {
        return position;
    }
    int getIndex()
    {
        return index;
    }
    short getBase()
    {
        return base;
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.luc.nmerge.mvd.table;
import java.util.BitSet;
/**
 * An Atom is either a Table or a Fragment.
 * @author desmond
 */
public abstract class Atom 
{
    FragKind kind;
    BitSet versions;
    static float THRESHOLD_SIM = 0.8f;
    abstract Atom merge( Atom other ) throws Exception;
    abstract boolean startsWithBreakPoint();
    abstract boolean endsWithBreakPoint();
    abstract String getContents();
    abstract int length();
}

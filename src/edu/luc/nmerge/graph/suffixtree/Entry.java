/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.luc.nmerge.graph.suffixtree;

class Entry
{
    String file;
    int size;
    long time;
    long space;
    Entry next;
    public Entry( String file, int size, long time, long space )
    {
        this.file = file;
        this.size = size;
        this.time = time;
        this.space = space;
    }
}
    
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.luc.nmerge.mvd.table;

/**
 *
 * @author desmond
 */
public enum SectionState {
    merged,
    almost,
    disjoint;
    public static SectionState state(FragKind kind )
    {
        switch ( kind )
        {
            case merged:
                return SectionState.merged;
            case almost:
                return SectionState.almost;
            default: case inserted: case aligned:
                return disjoint;
        }
    }
}

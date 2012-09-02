/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.luc.nmerge.mvd.diff;

/**
 *
 * Types of DIff. There are no matched
 */
public enum DiffKind
{
    /** standard inserted */
    INSERTED,
    /** standard deleted */
    DELETED,
    /** can be converted into insert/delete pairs */
    EXCHANGED,
    /** simple changed type not the same */
    CHANGED;
}

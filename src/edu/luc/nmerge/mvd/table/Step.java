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
import java.util.Stack;
/**
 * Component of an edit script going backwards from the end in a FragList 
 * @author desmond
 */
public class Step 
{
    Step parent;
    FragKind kind;
    int x;
    Step( Step parent, FragKind kind, int x )
    {
        this.parent = parent;
        this.kind = kind;
        this.x = x;
    }
    /**
     * Update an existing Step
     * @param kind the kind of step
     * @param x the new x-coordinate of the step
     * @return a new step or the old one updated
     */
    Step update( FragKind kind, int x )
    {
        Step s,t;
        switch ( kind )
        {
            case exchanged: case inserted:
                s = new Step( this, kind, this.x+1 );
                if ( x-this.x>1 )
                {
                    t = new Step( s, FragKind.merged, x );
                    return t;
                }
                else
                    return s;
            case deleted:
                s = new Step( this, kind, this.x );
                if ( x>this.x )
                {
                    t = new Step( s, FragKind.merged, x );
                    return t;
                }
                else
                    return s;
            default:
                return this;
        }
    }
    /**
     * Print out the steps in the correct order
     * @return a string
     */
    public String toString()
    {
        Stack<Step> stack = new Stack<Step>();
        Step temp = this;
        while ( temp != null )
        {
            stack.push( temp );
            temp = temp.parent;
        }
        StringBuilder sb = new StringBuilder();
        while ( !stack.isEmpty() )
        {
            Step s = stack.pop();
            if ( s.kind != FragKind.empty )
                sb.append("x="+s.x+" kind="+s.kind.toString()+"\n" );
        }
        return sb.toString();
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.luc.nmerge.mvd.table;
import java.util.BitSet;
/**
 * A fragment of text within a section
 */
class Fragment extends Atom
{
    String contents;
    String style;
    /**
     * A Fragment is an atom with a String for content. It shares a set of 
     * versions in case its text is the same as in those other versions.
     * @param kind the insert/delete/exchange/merge status
     * @param contents the string contents of the fragment
     * @param versions the set of versions this text is shared by
     */
    Fragment( FragKind kind, String contents, BitSet versions )
    {
        this.kind = kind;
        this.contents = contents;
        this.versions = versions;
    }
    /**
     * Get the length of this fragment
     * @return its length in characters
     */
    public int length()
    {
        return contents.length();
    }
    /**
     * Are we an empty fragment?
     * @return true or false
     */
    public boolean isEmpty()
    {
        return contents == null || contents.length()==0;
    }
    /**
     * Set a special style that will be written out as a span class
     * @param style the name of the span's class
     */
    void setStyle( String style )
    {
        this.style = style;
    }
    /**
     * In here we could replace troublesome characters with benign forms
     * @param value the value to escape
     * @return the escaped string
     */
    private String escape( String value )
    {
        return value;
    }
    /**
     * Get the contents of this thing
     * @return the text
     */
    public String getContents()
    {
        return contents;
    }
    /**
     * Two fragments are equals only if their contents are
     * @param f the other fragment or some other atom
     * @return true if the contents match else false
     */
    public boolean equals( Object f )
    {
        return f instanceof Fragment && contents.equals(((Fragment)f).contents);
    }
    /**
     * Convert to HTML (no other conversion makes sense)
     * @return a HTML fragment
     */
    public String getClassName()
    {
        if ( kind == FragKind.aligned || kind == FragKind.merged )
            return style;
        else if ( style != null )
            return "inserted-"+style;
        else 
            return "inserted";
    }
    /**
     * We can't convert the versions without a sigla list
     * @return a String being the fragments raw contents
     */
    public String toString()
    {
        return contents;
    }
    /**
     * Merge this fragment with another fragment or a table
     * @param other the other fragment or table
     * @return the merged object (usually this one)
     */
    Atom merge( Atom other ) throws Exception
    {
        if ( other instanceof Fragment )
        {
            // the versions of a fragment are sacred
            return this;
        }
        else // we are a fragment and other is not
            return other.merge( this );
    }
    /**
     * test if the contents start with a break point such as a space
     * @return true if it does
     */
    @Override
    boolean startsWithBreakPoint()
    {
        if ( contents.length()>0 )
        {
            char token = contents.charAt(0);
            if ( !Character.isLetter(token) )
                return true;
        }
        return false;
    }
    @Override
    boolean endsWithBreakPoint()
    {
        if ( contents.length()>0 )
        {
            char token = contents.charAt(contents.length()-1);
            if ( !Character.isLetter(token) )
                return true;
        }
        return false;
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.luc.nmerge.mvd.table;

/**
 * Format the text contents of a cell
 * @author desmond
 */
public class TableCell 
{
    String tagName;
    String className;
    String currentTextClass;
    StringBuilder sb;
    /** optional id of cell contents if not 0 */
    int id;
    
    TableCell( String tagName, String className )
    {
        this.tagName= tagName;
        this.className = className;
        this.sb = new StringBuilder();
    }
    /**
     * This is set during table writing only
     * @param id the id for the cell contents
     */
    void setID( int id )
    {
        this.id = id;
    }
    /**
     * Convert contents to a simple string
     * @return a string
     */
    @Override
    public String toString()
    {
        int len = sb.length();
        if ( len>0 )
        {
            if ( sb.charAt(0)==' ' )
                sb.replace(0,1,"&nbsp;");
            if ( sb.charAt(len-1)==' ' )
                sb.replace(len-1,len,"&nbsp;");
        }
        // start cell tag
        int pos = 0;
        sb.insert( pos++, "<" );
        sb.insert( pos, tagName );
        pos += tagName.length();
        if ( className !=null && className.length()>0 )
        {
            sb.insert( pos, " class=\"" );
            pos += 8;
            sb.insert( pos, className );
            pos += className.length();
            sb.insert( pos, "\"" );
            pos++;
        }
        if ( id != 0 )
        {
            sb.insert( pos, " id=\"t" );
            pos += 6;
            String idStr = Integer.toString(id);
            sb.insert( pos, idStr );
            pos += idStr.length();
            sb.insert( pos, "\"" );
            pos++;
        }
        sb.insert( pos, ">" );
        if ( currentTextClass != null )
            sb.append("</span>" );
        sb.append( "</" );
        sb.append( tagName );
        sb.append( ">" );
        return sb.toString();
    }
    /**
     * Add a piece of class-qualified text
     * @param value the actual text
     * @param textClassName the className of the text or null
     */
    void add( String value, String textClassName )
    {
        if ( textClassName != null && (currentTextClass == null 
            || !currentTextClass.equals(textClassName)) )
        {
            if ( currentTextClass != null )
                sb.append("</span>" );
            sb.append( "<span class=\"" );
            sb.append( textClassName );
            sb.append( "\">" );
        }
        sb.append( value );
        currentTextClass = textClassName;
    }
    /**
     * Add a simple text value
     * @param value the value to add
     */
    void add( String value )
    {
        // finish off previous text class
        if ( currentTextClass != null )
        {
            sb.append("</span>");
            currentTextClass = null;
        }
        sb.append( value );
    }
}

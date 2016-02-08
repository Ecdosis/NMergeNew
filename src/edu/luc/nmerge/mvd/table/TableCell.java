/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.luc.nmerge.mvd.table;
import java.util.ArrayList;
/**
 * Format the text contents of a cell
 * @author desmond
 */
public class TableCell 
{
    String tagName;
    String className;
    String currentTextClass;
    ArrayList<TextSegment> segments;
    /** optional id of cell contents if not 0 */
    int id;
    
    TableCell( String tagName, String className )
    {
        this.tagName= tagName;
        this.segments = new ArrayList<TextSegment>();
        this.segments.add( new TextSegment(className,"") );
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
     * Convert contents to a JSON string
     * @return a string
     */
    public String toJSONString()
    {
        StringBuilder json = new StringBuilder();
        json.append( "{" );
        if ( className !=null && className.length()>0 )
        {
            json.append( "\"class\":\"" );
            json.append( className );
            json.append( "\"," );
        }
        if ( id != 0 )
        {
            json.append( "\"id\":\"t" );
            String idStr = Integer.toString(id);
            json.append( idStr );
            json.append("\",");
        }
        json.append( "\"segments\":[" );
        for ( int i=0;i<segments.size();i++ )
        {
            TextSegment s = segments.get(i);
            if ( s.length()> 0 )
            {
                json.append( s.toJSONString() );
                if ( i<segments.size()-1 )
                    json.append(", ");
            }
        }
        json.append( "]}" );
        return json.toString();
    }
    /**
     * Convert contents to a HTML string
     * @return a string
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        // start cell tag
        sb.append( "<" );
        sb.append( tagName );
        if ( className !=null && className.length()>0 )
        {
            sb.append( " class=\"" );
            sb.append( className );
            sb.append( "\"" );
        }
        if ( id != 0 )
        {
            sb.append( " id=\"t" );
            String idStr = Integer.toString(id);
            sb.append( idStr );
            sb.append( "\"" );
        }
        sb.append( ">" );
        for ( int i=0;i<segments.size();i++ )
            sb.append( segments.get(i).toString() );
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
        TextSegment ts = segments.get(segments.size()-1);
        if ( ts.className != textClassName )
        {
            TextSegment ts2 = new TextSegment( textClassName, value );
            segments.add( ts2 );
        }
        else
            ts.add( value );
    }
    /**
     * Add a simple text value
     * @param value the value to add
     */
    void add( String value )
    {
        // finish off previous text class
        TextSegment ts = segments.get(segments.size()-1);
        if ( ts.className != null )
        {
            TextSegment ts2 = new TextSegment(null, value );
            segments.add( ts2 );
        }
        else
            ts.add( value );
    }
}

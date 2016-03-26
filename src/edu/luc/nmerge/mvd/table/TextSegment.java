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

/**
 * A segment of text formatted in one way
 * @author desmond
 */
public class TextSegment 
{
    String className;
    StringBuilder sb;
    public TextSegment( String className, String text )
    {
        this.className = className;
        this.sb = new StringBuilder();
        this.sb.append( text );
    }
    public void add( String text )
    {
        this.sb.append( text );
    }
    public String getClassName()
    {
        return className;
    }
    /**
     * prepare some text for use in a JSON document string
     * @param text the unclean text
     * @return the cleaned text
     */
    private String cleanText( String text )
    {
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<text.length();i++ )
        {
            char token = text.charAt(i);
            if ( token == '\n' )
                sb.append(" ");
            else if ( token == '\t' )
                sb.append(" ");
            else if ( token == '"' )
                sb.append("\\\"");
            else
                sb.append(token);
        }
        return sb.toString();
    }
    public int length()
    {
        return sb.length();
    }
    public String toJSONString()
    {
        StringBuilder json = new StringBuilder();
        if ( sb.length() > 0 )
        {
            int len = sb.length();
            if ( sb.charAt(0)==' ' )
                sb.replace(0,1,"&nbsp;");
            if ( sb.charAt(len-1)==' ' )
                sb.replace(len-1,len,"&nbsp;");
        }
        // can't concatenate segments in JSON because no mixed content
        json.append("{ ");
        if ( this.className != null )
        {
            json.append("\"class\": \"");
            json.append( className );
            json.append("\", ");
        }
        json.append("\"text\": \"");
        json.append( cleanText(sb.toString()) );
        json.append( "\"");
        json.append(" }");
        return json.toString();
    }
    public String toString()
    {
        StringBuilder html = new StringBuilder();
        if ( sb.length() > 0 )
        {
            int len = sb.length();
            if ( sb.charAt(0)==' ' )
                sb.replace(0,1,"&nbsp;");
            if ( sb.charAt(len-1)==' ' )
                sb.replace(len-1,len,"&nbsp;");
        }
        html.append("{ ");
        if ( this.className != null )
        {
            html.append("<span \"class\"=\"");
            html.append( className );
            html.append("\">");
        }
        html.append( sb.toString());
        if ( this.className != null )
            html.append("</span>");
        return html.toString();
    }
}

package edu.luc.nmerge.mvd;

import java.io.FileOutputStream;
import java.io.IOException;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Write out XML prettily. This should really be supplied by the XML 
 * serialiser classes but it isn't.
 * @author Desmond Schmidt 9/5/09
 */
public class XMLPretty 
{
	/**
	 * Write the XML declaration at the start
	 * @param fos the file output stream to write to
	 * @param encoding the encoding to use
	 */
	public static void writeXMLDeclaration( FileOutputStream fos, 
		String encoding ) throws IOException
	{
		fos.write( '<' );
		fos.write( "?xml".getBytes(encoding) );
		writeAttribute( fos, "version", "1.0", encoding );
		writeAttribute( fos, "encoding", encoding, encoding );
		fos.write( '?' );
		fos.write( '>' );
		writeLineEnd( fos, encoding, 0 );
	}
	/**
	 * Write out a single node recursively
	 * @param node the node to write out
	 * @param fos the output stream to write to
	 * @param encoding desired encoding of the output
	 * @param pretty if true add new lines before element
	 * @param depth indent level
	 * @throws IOException if there was an I/O error
	 */
	public static void writeNode( Node node, FileOutputStream fos, 
		String encoding, boolean pretty, int depth ) throws IOException
	{
		if ( node.hasChildNodes() )
		{
			writeStartTag( node, fos, encoding );
			writeNodeContent( node, fos, encoding, pretty, depth );
			writeEndTag( node, fos, encoding );
		}
		else
			writeEmptyTag( node, fos, encoding );
	}
	/**
	 * Write a single attribute to the output
	 * @param fos the output stream to write to
	 * @param name the name of the attribute
	 * @param value its value
	 * @param encoding the desired encoding
	 * @throws IOException if there was an I/O error
	 */
	private static void writeAttribute( FileOutputStream fos, String name, 
		String value, String encoding ) throws IOException
	{
		// insert a space before ALL attributes
		fos.write( ' ' );
		fos.write( name.getBytes(encoding) );
		fos.write( '=' );
		fos.write( '"' );
		fos.write( value.getBytes(encoding) );
		fos.write( '"' );
	}
	/**
	 * Enforce DOS line endings because url-encoded forms return them.
	 * @param fos the output stream to write to
	 * @param encoding the encoding for the new line and spacing
	 * @param depth the indent level if pretty printing (otherwise 0)
	 */
	private static void writeLineEnd( FileOutputStream fos, String encoding, 
		int depth ) throws IOException
	{
		fos.write( '\r' );
		fos.write( '\n' );
		for ( int i=0;i<depth;i++ )
			fos.write( "    ".getBytes(encoding) );
	}
	/**
	 * Write out a node that has no content
	 * @param node the node to write
	 * @param fos the output stream to write to
	 * @param encoding the encoding for the element name and attributes
	 * @throws IOException if there was an I/O error
	 */
	private static void writeEmptyTag( Node node, FileOutputStream fos, 
		String encoding ) throws IOException
	{
		fos.write( '<' );
		fos.write( node.getNodeName().getBytes(encoding) );
		writeAttributes( node, fos, encoding );
		fos.write( '/' );
		fos.write( '>' );
	}
	/**
	 * Write out a single start tag
	 * @param node the node to write out
	 * @param fos the output stream to write to
	 * @param encoding the desired encoding of the output
	 * @throws IOException if there was an I/O error
	 */
	private static void writeStartTag( Node node, FileOutputStream fos, 
		String encoding ) throws IOException
	{
		fos.write( '<' );
		String nodeName = node.getNodeName();
		fos.write( nodeName.getBytes(encoding) );
		writeAttributes( node, fos, encoding );
		fos.write( '>' );
	}
	/**
	 * Write out attributes of a node
	 * @param node the node whose attributes need writing out
	 * @param fos the output stream to write to
	 * @param encoding the encoding to use
	 * @throws IOException if there was an I/O error
	 */
	private static void writeAttributes( Node node, FileOutputStream fos, 
		String encoding ) throws IOException
	{
		NamedNodeMap attrs = node.getAttributes();
		for ( int i=0;i<attrs.getLength();i++ )
		{
			Node attr = attrs.item( i );
			writeAttribute( fos, attr.getNodeName(), attr.getNodeValue(), 
				encoding );
		}
	}
	/**
	 * Write out a single end tag
	 * @param node the node to write out
	 * @param fos the output stream to write to
	 * @param encoding the desired encoding of the output
	 */
	private static void writeEndTag( Node node, FileOutputStream fos, 
		String encoding ) throws IOException
	{
		fos.write( '<' );
		fos.write( '/' );
		fos.write( node.getNodeName().getBytes(encoding) );
		fos.write( '>' );
	}
	/**
	 * Write out the content of a Node verbatim (hooray we can do this!)
	 * @param node the node to write out
	 * @param fos the output stream to write to
	 * @param encoding the desired encoding (stick to this!)
	 * @param pretty if true add newlines before elements
	 * @param level the indent level
	 * @throws IOException if there was an I/O error
	 */
	private static void writeNodeContent( Node node, FileOutputStream fos, 
		String encoding, boolean pretty, int level ) throws IOException
	{
		boolean containedOnlyElements = true;
		NodeList children = node.getChildNodes();
		for ( int i=0;i<children.getLength();i++ )
		{
			Node child = children.item( i );
			switch ( child.getNodeType() )
			{
				case Node.ELEMENT_NODE:
					if ( pretty )
						writeLineEnd( fos, encoding, level );
					writeNode( child, fos, encoding, pretty, level+1 );
					break;
				case Node.TEXT_NODE:
					String nodeContent = child.getTextContent();
					nodeContent = escapeContent( nodeContent );
					fos.write( nodeContent.getBytes(encoding) );
					containedOnlyElements = false;
					break;
			}
		}
		if ( containedOnlyElements && pretty )
			writeLineEnd( fos, encoding, level-1 );
	}
	/**
	 * In order to get XML inside XML we must escape angle brackets and 
	 * ampersands as a minimum.
	 * @param raw the raw unescaped string
	 * @return the escaped string
	 */
	private static String escapeContent( String raw )
	{
		char[] chars = raw.toCharArray();
		StringBuffer sb = new StringBuffer( 
			Math.round((float)chars.length * 1.15f) );
		for ( int i=0;i<chars.length;i++ )
		{
			if ( chars[i] == '<' )
				sb.append( "&lt;" );
			else if ( chars[i] == '>' )
				sb.append( "&gt;" );
			else if ( chars[i] == '&' )
				sb.append( "&amp;" );
			else
				sb.append( chars[i] );
		}
		return sb.toString();
	}
}

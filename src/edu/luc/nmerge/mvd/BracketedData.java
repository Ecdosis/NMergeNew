package edu.luc.nmerge.mvd;

import java.io.ByteArrayOutputStream;

/**
 * Represent an object output by the MvdTool as data 
 * enclosed in [..] and using a ':' to separate the 
 * header (after the '[') from the data parts
 * @author Desmond Schmidt 2/6/09
 */
public abstract class BracketedData 
{
	/** the data of the chunk */
	protected char[] realData;
	/** the escaped data of the chunk */
	protected char[] escapedData;
	/** length of data parsed to produce this object */
	protected int srcLen;
	/** encoding of the data */
	protected String encoding;
	/**
	 * Create a BracketedData object
	 * @param encoding the encoding of the data to be parsed
	 */
	public BracketedData( String encoding )
	{
		this.encoding = encoding;
	}
	/**
	 * Create a BracketedData object using the data
	 * @param encoding the encoding of the data to be parsed
	 * @param data the original data
	 */
	public BracketedData( String encoding, char[] data )
	{
		this.encoding = encoding;
		this.realData = data;
		this.escapedData = escapeData( data );
	}
	/**
	 * Read the body of the chunk.
	 * @param chunkData the data to read from, with escaped ]s
	 * @param pos the start offset in the data
	 * @return the number of bytes consumed
	 */
	protected int readData( char[] chunkData, int pos )
	{
		int state = 0;
		int start = pos;
		StringBuilder sb = new StringBuilder();
        while ( pos < chunkData.length && state != -1 )
		{
			switch ( state )
			{
				case 0:	// reading text
					if ( chunkData[pos] == '\\' )
						state = 1;
					else if ( chunkData[pos] == ']' )
						state = -1;
					else
						sb.append( chunkData[pos] );
					break;
				case 1:	// reading backslash
					if ( chunkData[pos] == '\\' )
						sb.append( '\\' );
					else if ( chunkData[pos] == ']' )
						sb.append( ']' );
					state = 0;
					break;					
			}
			pos++;
		}
        realData = new char[sb.length()];
		sb.getChars(0,realData.length,realData,0);
		escapedData = escapeData( realData );
		return pos - start;
	}
	/**
	 * Ensure that any ']'s in the data are escaped so we can use them 
	 * to terminate the chunk when parsing it
	 * @param chars the array to be escaped
	 * @return the same array of bytes but ']' replaced with '\]' and 
	 * '\' replaced by '\\'
	 */
	protected char[] escapeData( char[] chars )
	{
		StringBuilder sb= new StringBuilder();
		for ( int i=0;i<chars.length;i++ )
		{
			if ( chars[i] == '\\' )
			{
				sb.append( '\\' );
				sb.append( '\\' );
			}
			else if ( chars[i] == ']' )
			{
				sb.append( '\\' );
				sb.append( ']' );
			}
			else
				sb.append( chars[i] );
		}
        char[] array = new char[sb.length()];
        sb.getChars(0,array.length,array,0);
		return array;
	}
	/**
	 * Add some data to the chunk. 
	 * @param chars the new chars to add to data
	 */
	public void addData( char[] chars )
	{
		if ( chars != null && chars.length > 0 )
		{
			char[] newData = new char[realData.length+chars.length];
			for ( int i=0;i<realData.length;i++ )
				newData[i] = realData[i];
			for ( int j=realData.length,i=0;i<chars.length;i++,j++ )
				newData[j] = chars[i];
			realData = newData;
			escapedData = escapeData( realData );
		}
	}
	/**
	 * Create the header which is assumed to be convertable 
	 * into a string
	 * @return the header as a String
	 */
	protected abstract String createHeader();
	/**
	 * Write out the chunk without converting its bytes to characters
	 * @return a char array
	 */
	public char[] getChars()
	{
		String header = createHeader();
        StringBuilder sb = new StringBuilder(header);
        sb.append(escapedData);
        sb.append("]");
        char[] totalChars = new char[sb.length()];
        sb.getChars(0,totalChars.length,totalChars,0);
		return totalChars;
	}
	/**
	 * Get the length of the source data
	 * @return the number of bytes parsed to produce this variant
	 */
	public int getSrcLen()
	{
		return srcLen;
	}
	/**
	 * Get the read unescaped data
	 * @return a byte array of raw data
	 */
	public char[] getData()
	{
		return realData;
	}
}
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
	protected byte[] realData;
	/** the escaped data of the chunk */
	protected byte[] escapedData;
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
	public BracketedData( String encoding, byte[] data )
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
	protected int readData( byte[] chunkData, int pos )
	{
		int state = 0;
		int start = pos;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
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
						bos.write( chunkData[pos] );
					break;
				case 1:	// reading backslash
					if ( chunkData[pos] == '\\' )
						bos.write( '\\' );
					else if ( chunkData[pos] == ']' )
						bos.write( ']' );
					state = 0;
					break;					
			}
			pos++;
		}
		realData = bos.toByteArray();
		escapedData = escapeData( realData );
		return pos - start;
	}
	/**
	 * Ensure that any ']'s in the data are escaped so we can use them 
	 * to terminate the chunk when parsing it
	 * @param bytes the array to be escaped
	 * @return the same array of bytes but ']' replaced with '\]' and 
	 * '\' replaced by '\\'
	 */
	protected byte[] escapeData( byte[] bytes )
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream( 
			bytes.length+5 );
		for ( int i=0;i<bytes.length;i++ )
		{
			if ( bytes[i] == '\\' )
			{
				bos.write( '\\' );
				bos.write( '\\' );
			}
			else if ( bytes[i] == ']' )
			{
				bos.write( '\\' );
				bos.write( ']' );
			}
			else
				bos.write( bytes[i] );
		}
		return bos.toByteArray();
	}
	/**
	 * Add some data to the chunk. 
	 * @param bytes the new bytes to add to data
	 */
	public void addData( byte[] bytes )
	{
		if ( bytes != null && bytes.length > 0 )
		{
			byte[] newData = new byte[realData.length+bytes.length];
			for ( int i=0;i<realData.length;i++ )
				newData[i] = realData[i];
			for ( int j=realData.length,i=0;i<bytes.length;i++,j++ )
				newData[j] = bytes[i];
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
	 * @return a byte array
	 */
	public byte[] getBytes()
	{
		String header = createHeader();
		byte[] headerBytes = header.getBytes();
		byte[] totalBytes = new byte[headerBytes.length+escapedData.length+1];
		int j=0;
		for ( int i=0;i<headerBytes.length;i++ )
			totalBytes[j++] = headerBytes[i];
		for ( int i=0;i<escapedData.length;i++ )
			totalBytes[j++] = escapedData[i];
		totalBytes[j] = ']';
		return totalBytes;
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
	public byte[] getData()
	{
		return realData;
	}
}
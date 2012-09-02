package edu.luc.nmerge.mvd;

import java.io.FileWriter;
import java.io.File;
/**
 * Log errors to a /tmp file
 * OK, this is quick and dirty
 */
public class MVDError 
{
	public static void log( String message )
	{
		try
		{
			File tmpDir = new File( System.getProperty("java.io.tmpdir"));
			File tmpFile = new File( tmpDir,"mvderror.log" );
			FileWriter fw = new FileWriter(tmpFile,true);
			fw.write( message+"\n" );
			fw.close();
		}
		catch ( Exception e )
		{
		}
	}
}

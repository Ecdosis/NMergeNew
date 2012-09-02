package edu.luc.qut.nmerge;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class Utilities {
	/**
	 * Load a properties file describing the database properties for connecting to
	 * @param dbConn the name of the properties file (in the class path)
	 * @return a loaded resource bundle
	 */
	public static Properties loadDBProperties( String dbConn )
	{
		if ( dbConn == null )
			return null;
		else
		{
			String wd = System.getProperty("user.dir");
			File wdDir = new File( wd );
			Properties props = new Properties();
	        try
	        {
	        	FileInputStream fis = new FileInputStream(wdDir
	        		+File.separator+dbConn+".properties");
		        props.load(fis);    
		        fis.close();
		        return props;
	        }
	        catch ( Exception e )
	        {
	        	System.out.println(e.getMessage());
	        	return null;
	        }
		}
	}
}

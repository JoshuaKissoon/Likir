package unito.likir.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads the values of the properties from the file config.properties
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class PropFinder
{	
	private static Properties prop = null;
	
	//State persistence
	public static final String NODE_PERSISTENCE_PATH = "nodePersistencePath"; //directory in which node states are saved
	
	/*SETTINGS PATH. MODIFY THIS STRING TO CHANGE SETTINGS LOCATION*/
	/*=============================== Modify here ===========================================*/
	private static String propertyFilePath = "Settings"+File.separator+"config.properties";
	/*=======================================================================================*/
	
	/**
	 * Loads the value corresponding to propertyName from the file config.properties
	 * @param propertyName the name of the property
	 */
	public static String get(String propertyName)
	{
		if (prop == null || prop.getProperty(propertyName) == null)
		{
			try
			{
				loadProperties(propertyFilePath);
			}
			catch (IOException e)
			{
				try
				{
					propertyFilePath = ".." + File.separator + propertyFilePath;
					loadProperties(propertyFilePath);
				}
				catch (FileNotFoundException ex)
				{
					System.err.println("Property file not found");
					e.printStackTrace();
					System.exit(0);
				}
				catch (IOException ex)
				{
					System.err.println("Error while reading property file");
					e.printStackTrace();
					System.exit(0);
				}
			}
		}
		return prop.getProperty(propertyName);
	}
	
	private static void loadProperties(String path) throws FileNotFoundException, IOException
	{
		prop = new Properties();
		File file = new File(path);
		InputStream fis = new FileInputStream(file);
		prop.load(fis);
	}
}

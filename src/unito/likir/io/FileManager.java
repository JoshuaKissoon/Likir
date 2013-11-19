package unito.likir.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import unito.likir.NodeId;

/**
 * FileManager class
 * @author Aiello luca Maria
 * @version 0.1
 */

public class FileManager
{	
	public FileManager()
	{
	}
	
	/**
	 * SHA-1 hash (160 bit)
	 * @param file
	 * @return
	 */
	public synchronized NodeId hash(File file) throws IOException
	{		
		try
        {         
			//hash function initialization
            MessageDigest md = MessageDigest.getInstance("SHA");
            //file name is converted to a byte array
            String toLowerCaseName = file.getName().toLowerCase();
            //System.out.println("FileManager.FileHash = " + toLowerCaseName);
            byte[] buffer = toLowerCaseName.getBytes();
            //hashing
            md.update(buffer);
            //hash finalization (padding etc...)
            buffer = md.digest();
            //build hash code in Kademlia format
            return NodeId.createWithBytes(buffer);
        }
        catch (NoSuchAlgorithmException e)
        {
            System.err.println("Non existent algorithm");
        }
        return null;
	}
	
	public synchronized NodeId hash(String string) throws IOException
	{		
		try
        {         
			//hash function initialization
            MessageDigest md = MessageDigest.getInstance("SHA");
            String toLowerCaseString = string.toLowerCase();
            //System.out.println("FileManager.StringHash = " + toLowerCaseString);
            byte[] buffer = toLowerCaseString.getBytes();
            //hashing
            md.update(buffer);
            //hash finalization (padding etc...)
            buffer = md.digest();
            //build hash code in Kademlia format
            return NodeId.createWithBytes(buffer);
        }
        catch (NoSuchAlgorithmException e)
        {
            System.err.println("Non existent algorithm");
        }
        return null;
	}
	
	public synchronized byte[] toBytes(File file) throws IOException
	{
		if (file.length() >= Integer.MAX_VALUE)
			throw new IllegalArgumentException("Too large file size");

		FileInputStream in = new FileInputStream(file);
		
		byte[] bytes = new byte[(int)file.length()];
		in.read(bytes);
		return bytes;
	}
}

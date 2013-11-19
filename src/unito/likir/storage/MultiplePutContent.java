package unito.likir.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class MultiplePutContent implements Serializable
{
	private static final long serialVersionUID = -2720143568003858264L;
	
	private byte[][] contents;
	private String[] types;
	private long[] ttls;
	int size = 0;
	
	public MultiplePutContent(int n)
	{
		this.contents = new byte[n][];
		this.types = new String[n];
		this.ttls = new long[n];
	}
	
	public MultiplePutContent(byte[][] content, String[] type, long[] ttl)
	{
		this.contents = content;
		this.types = type;
		this.ttls = ttl;
	}
	
	public void putContent(byte[] content, String type, long ttl)
	{
		if (size < contents.length)
		{
			contents[size] = content;
			types[size] = type;
			ttls[size] = ttl;
			size++;
		}
		else
		{
			throw new IllegalArgumentException("The structure is full");
		}
	}
	
	public int getSize()
	{
		return size;
	}
	
	public byte[] toBytes()
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try
		{
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(this);
		}
		catch(IOException ioe)
		{
			System.err.println("Error in MultiplePutContent serialization!");
			return null;
		}
		return baos.toByteArray();
	}
	
	public static MultiplePutContent createFromBytes(byte[] bytes) throws IllegalArgumentException
	{
		MultiplePutContent object = null;
		try
		{
			object = (MultiplePutContent) new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
		}
		catch(IOException ioe)
		{
			throw new IllegalArgumentException("I/O error while converting to MultiplePutObject instance");
		}
		catch(ClassNotFoundException cnfe)
		{
			throw new IllegalArgumentException("Error while converting to MultiplePutObject instance");
		}
		catch(ClassCastException cce)
		{
			throw new IllegalArgumentException("Error while converting to MultiplePutObject instance");
		}
		return object;
	}
	
	/*GETTERS and SETTERS*/

	public byte[][] getContent()
	{
		return contents;
	}

	public void setContent(byte[][] content)
	{
		this.contents = content;
	}

	public String[] getType()
	{
		return types;
	}

	public void setType(String[] type)
	{
		this.types = type;
	}

	public long[] getTtl()
	{
		return ttls;
	}

	public void setTtl(long[] ttl)
	{
		this.ttls = ttl;
	}
}

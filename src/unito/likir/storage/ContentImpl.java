package unito.likir.storage;

/**
 * A DHT content implementation
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class ContentImpl implements Content
{
	private static final long serialVersionUID = 8233945168573357136L;
	
	private byte[] value; //the content payload
	private String type; // the content type
	
	/**
	 * Create a new content
	 * @param value the raw data 
	 * @param type the data type
	 */
	public ContentImpl(byte[] value, String type)
	{
		if (type == null || type.equals(""))
			throw new IllegalArgumentException("Content type must be != null");
		this.value = value;
		this.type = type;
	}
	
	/**
	 * Returns the content payload
	 * @return the content payload
	 */
	public byte[] getValue()
	{
		return value;
	}
	
	/**
	 * Returns the content type
	 * @return the content type
	 */	
	public String getType()
	{
		return type;
	}
	
	/**
	 * Returns the content size (in bytes)
	 * @return the content size
	 */
	public int size()
	{
		return value.length;
	}
	
	/**
	 * Return a string representation of the content
	 * @return a string representation of the content
	 */
    public String toString()
    {
        String result = "Content - type = " + type + ", size = " + size();
        return result;
    }
}

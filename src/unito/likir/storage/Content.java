package unito.likir.storage;

import java.io.Serializable;

/** 
 * A DHT content interface
 * @author Luca Maria Aiello
 * @version 0.1
 */

public interface Content extends Serializable
{   
    /**
     * Returns the data (the raw bytes) of the content
     */
    public byte[] getValue();
    
    /**
     * Returns the type of the content data.
     */
    public String getType();
    
    /**
     * Returns the size of the data payload in byte
     */
    public int size();
}
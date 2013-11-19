package unito.likir.storage;

import java.io.Serializable;

import unito.likir.NodeId;
import unito.likir.security.Credentials;

/**
 * A StorageEntry instance models a generic Likir content.
 * Every StorageEntry is composed by a lookup key, a Content and Credentials which bound the
 * identity of the Content owner to the Content itself. 
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class StorageEntry implements Serializable, Comparable<StorageEntry>
{
	private static final long serialVersionUID = 8862122105739833992L;
	
	private NodeId key;
	private Content content;
	private Credentials credentials;
	private transient long lastRepublishTime;
	
	/**
	 * Create a new storage entry. This constructor must be used only if right Credentials
	 * for the content are available; the class EntryFactory provides methods to create
	 * a StorageEntry bounded with its right Credentials starting from a lookup key and 
	 * a raw byte array.
	 * @param key the content lookup key
	 * @param content the Content
	 * @param credentials the Credentials bound to the content
	 */
	public StorageEntry(NodeId key, Content content, Credentials credentials)
	{
		this.key = key;
		this.content = content;
		this.credentials = credentials;
		this.lastRepublishTime = System.currentTimeMillis();
	}
	
	/**
	 * Returns the content lookup key
	 * @return the lookup key
	 */
	public NodeId getKey()
	{
		return key;
	}
	
	/**
	 * Returns the owner of the Content of this StorageEntry
	 * @return the userId of the owner
	 */
	public String getOwnerId()
	{
		return credentials.getOwnerId();
	}
	
	/**
	 * Returns the Content of this StorageEntry
	 * @return the Content
	 */
	public Content getContent()
	{
		return content;
	}
	
	/**
	 * Returns the Credentials of this StorageEntry
	 * @return
	 */
	public Credentials getCredentials()
	{
		return credentials;
	}
	
	/**
	 * Returns the submission time of the Content
	 * @return the submission time
	 */
	public long getSubmissionTime()
	{
		return credentials.getTimeStamp();
	}
	
	/**
	 * Returns the Time To Live of the content in this StorageEntry 
	 * @return
	 */
	public long getTtl()
	{
		return credentials.getTTL();
	}
	
	/**
	 * Returns the time of last republishing. This value is significant only when the StorageEntry
	 * is kept in a Storage
	 * @return the last republish time
	 */
	public long getLastRepublishTime()
	{
		return lastRepublishTime;
	}
	
	/**
	 * Sets the last republish time to now
	 * @return the last republish time
	 */
	public long refreshRepublishTime()
	{
		return lastRepublishTime = System.currentTimeMillis();
	}
	
	/**
	 * True if the Content of this StorageEntry is expired (iff getSubmissionTime() + getTtl() > currentTime)
	 * @return whether or not the content is expired
	 */
	public boolean isExpired()
	{
		return credentials.isExpired();
	}
	
	/**
	 * Compares StorageEntries on their lookup key
	 * @return the result of the comparison
	 */
	public int compareTo(StorageEntry e)
	{
		return key.compareTo(e.getKey());
	}
	
	/**
	 * String representation of this StorageEntry
	 * @return a string representation of this StorageEntry 
	 */
	public String toString()
	{
        StringBuilder buffer = new StringBuilder();
        buffer.append("Entry: ").append(" - ");
        
        buffer.append("Key: ").append(key).append(" - Type: ").append(getContent().getType()).append(" - Owner: ").append(getOwnerId()).append(" - TS: ").append(getSubmissionTime()).append(" - TTL: ").append(getTtl());

        return buffer.toString();
	}
}

package unito.likir.storage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.io.Serializable;

import unito.likir.NodeId;

/**
 * A Node Storage is a data structure where DHT contents are kept.
 * When a node receives a valid store request it must save the related StorageEntry in its Storage
 * Provides methods to store and retrieve the StorageEntries 
 * @author Luca Maria Aiello
 * @version 0.1
 */

public interface Storage extends Serializable
{
	/**
	 * Checks if the storage is empty
	 * @return Whether or not this storage is empty
	 */
	public boolean isEmpty();
	
    /**
     * Adds the given entry in this storage
     * @param entry the content to be stored
     * @return Whether or not the given content was added
     */
    public boolean store(StorageEntry entry);
    
    /**
     * Gets a view of the of the entries corresponding to the couple <key,ownerId>
     * If the parameter recent is set it returns only the first element of the list corresponding
     * to the couple <key,ownerId>
     * @param key the entry key
     * @param type the content type (can be null)
     * @param ownerId the owner id (can be null)
     * @param recent if set retrieves only the most recent values 
     * @return the required storage entry, null if there is no matching result
     */
    public Collection<StorageEntry> get(NodeId key, String type, String ownerId, boolean recent);

    /**
     * TODO
     */
    public HashMap<String,Integer> getCount(NodeId key, String type, String ownerId, boolean recent);
    
    /**
	 * TODO: Suitable for UDP packet length
     */
    public Collection<StorageEntry> getLimited(NodeId key, String type, String ownerId, boolean recent);
    
    /**
     * Removes the key-bucket corresponding to key
     * @param key the entry key
     * @return a Map view of the removed lists
     */
    public HashMap<String,HashMap<String,List<StorageEntry>>> remove(NodeId key);
    
    /**
     * Removes the type-bucket corresponding to <key,type>
     * @param key the entry key
     * @param type the type of the content 
     * @return the removed list, null if there is no value stored for <key,type>
     */
    public Map<String,List<StorageEntry>> remove(NodeId key, String type);
    
    /**
     * Removes the user-bucket corresponding to <key,type,ownerId>
     * @param key the entry key
     * @param type the type of the content 
     * @param ownerId the owner of the content 
     * @return the removed list, null if there is no value stored for <key,type,ownerId>
     */
    public List<StorageEntry> remove(NodeId key, String type, String ownerId);
    
    /**
     * Removes the specified StorageEntry, if contained in this storage
     * @param e the StorageEntry to be erased
     * @return true if the entry was deleted, false if it wasn't in the storage
     */
    public boolean remove(StorageEntry e);
   
    /**
     * Returns whether or not exists a key-bucket corresponding to key
     * @param key the key
     * the check result
     */
    public boolean contains(NodeId key);
    
    /**
     * Returns whether or not exists a type-bucket corresponding to <key,type>
     * @param key the key
     * @param type the type
     * the check result
     */
    public boolean contains(NodeId key, String type);
    
    /**
     * Returns whether or not exists a user-bucket corresponding to <key,type,ownerId>
     * @param key the key
     * @param type the type
     * @param ownerId the owner id
     * the check result
     */
    public boolean contains(NodeId key, String type, String ownerId);
    
    /**
     * Returns all stored keys
     * @return a set of keys
     */
    public Set<NodeId> keySet();
    
    /**
     * Returns a collection view of all the StorageEntry in the storage
     * @return a collection containing every storage entry
     */
    public Collection<StorageEntry> values();
    
    /**
     * Returns the number of keys in this Storage
     * @return the number of keys in the Storage
     */
    public int getKeyBucketCount();
    
    /**
     * Returns the number of typeBuckets in this Storage
     * @return typeBuckets counter
     */
    public int getTypeBucketCount();
    
    /**
     * Returns the number of userBuckets in this Storage
     * @return userBuckets counter
     */
    public int getUserBucketCount();
    
    /**
     * Return the total number of StorageEntry object contained in this Storage
     * @return the StorageEntry counter
     */
    public int getEntryCount();
    
    /**
     * Clears the Storage
     */
    public void clear();
    
}
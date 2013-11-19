package unito.likir.routing;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import unito.likir.*;

/**
 * Kademlia K-Bucket interface
 * @author Luca Maria Aiello
 * @version 0.1
 */
public interface Bucket extends Serializable
{
    /**
     * Returns the Bucket NodeId
     */
    public NodeId getBucketId();

    /**
     * Returns the depth of the Bucket in the Tree
     * @return the route table
     */
    public int getDepth();

    /**
     * Returns the time stamp when this Bucket was refreshed last time
     * @return the time stamp (in milliseconds)
     */
    public long getLastRefresh();
    
    /**
     * Set the refresh time of the Bucket to 'now'
     */
    public void touch();
    
    /**
     * Returns the Contact (in the bucket or in it's cache) that has the provided NodeId
     * @param nodeId the NodeId
     * @return the contact corresponding to the specified NodeId, null if it is not contained in this bucket
     */
    public Contact get(NodeId nodeId);
    
    /**
     * Returns the Contact that has the provided NodeId
     * @param nodeId the NodeId
     * @return the contact corresponding to the specified NodeId, null if it is not contained in this bucket
     */
    public Contact getBucketContact(NodeId nodeId);
    
    /**
     * Returns the cached Contact that has the provided NodeId
     * @param nodeId the NodeId
     * @return the contact corresponding to the specified NodeId, null if it is not contained in this bucket's cache
     */
    public Contact getCachedContact(NodeId nodeId);
    
    /**
     * Returns the best matching Contact for the provided NodeId
     * @param nodeId the NodeId
     * @return the best matching contact
     */
    public Contact select(NodeId nodeId);
    
    /**
     * Returns the 'count' best matching Contacts for the provided NodeId
     * @param nodeId a NodeId
     * @param count the number of desired entries
     */
    public Collection<Contact> select(NodeId nodeId, int count);
    
    /**
     * Adds the specified Contact in this Bucket if it is not full
     * Returns true if the contact has been added, false if the Bucket is full 
     * @param node the new contact
     * @return the result of the operation
     */
    public boolean addBucketContact(Contact node);

    /**
     * Add the specified Contact to the replacement cache of this bucket
     * @param node the new contact
     */
    public void addCachedContact(Contact node);

    /**
     * Updates the Contact in this bucket 
     */
    //TODO: eliminare
    //public Contact updateContact(Contact node);

    /**
     * Removes the Contact that has the provided NodeId in this Bucket or in it's cache
     * @return the Contact that has been removed, null if there isn't a contact that had that NodeId
     */
    public Contact remove(NodeId nodeId);
    
    /**
     * Removes the Contact that has the provided NodeId from the Bucket
     * @return the Contact that has been removed, null if there isn't a contact that had that NodeId
     */
    public Contact removeBucketContact(NodeId nodeId);

    /**
     * Removes the Contact that has the provided NodeId from the Cache
     * @return the Contact that has been removed, null if there isn't a contact that had that NodeId
     */
    public Contact removeCachedContact(NodeId nodeId);

    /**
     * Returns whether or not this Bucket contains a Contact with the specified NodeId
     * @return the result of the check
     */
    public boolean containsBucketContact(NodeId nodeId);

    /**
     * Returns whether or not this Bucket's cache contains a Contact with the specified NodeId
     * @return the result of the check
     */
    public boolean containsCachedContact(NodeId nodeId);

    /**
     * Returns whether or not this Bucket is full
     * @return the result of the check
     */
    public boolean isBucketFull();

    /**
     * Returns whether or not this Bucket's cache is full
     * @return the result of the check
     */
    public boolean isCacheFull();

    /**
     * Returns whether or not this Bucket is too deep in the Tree
     */
    public boolean isTooDeep();

    /**
     * Returns all bucket Contacts as List
     * @return a Collection containing all the bucket Contacts
     */
    public Collection<Contact> getAllBucketContacts();

    /**
     * Returns all cached Contacts as List
     * @return a Collection containing all the cached Contacts
     */
    public Collection<Contact> getAllCachedContacts();

    /**
     * Returns the least recently seen active Contact
     * @return the Contact
     */
    public Contact getLeastRecentlySeenBucketContact();

    /**
     * Returns the most recently seen active Contact
     * @return the Contact
     */
    public Contact getMostRecentlySeenBucketContact();

    /**
     * Returns the least recently seen cached Contact
     * @return the Contact
     */
    public Contact getLeastRecentlySeenCachedContact();

    /**
     * Returns the most recently seen cached Contact
     * @return the Contact
     */
    public Contact getMostRecentlySeenCachedContact();
    
    /**
     * Removes the UNKNOWN and DEAD Contacts in this bucket and replaces
     * empty entries in the Bucket with the cache entries
     */
    public void purge();

    /**
     * Splits the Bucket into two buckets with empty cache
     * @return a List containing the two new buckets, null if this Bucket is empty
     */
    public List<Bucket> split();

    /**
     * Returns the number of Contacts in the Bucket plus the number of cached contacts
     * @return the total number of Contacts
     */
    public int size();
    
    /**
     * Returns the number of Contacts in the Bucket
     * @return the number of Contacts
     */
    public int getBucketSize();
    
    /**
     * Returns the number of cached Contacts in the Bucket
     * @return the number of cached Contacts
     */
    public int getCacheSize();
    
    /**
     * Returns the maximum number of active Contacts the Bucket can hold (aka k)
     * @return the parameter k
     */
    public int getMaxBucketSize();
    
    /**
     * Returns the maximum number of active Contacts the Bucket's cache can hold
     * @return maximum number of entries
     */
    public int getMaxCacheSize();

    /**
     * Clears the Bucket
     */
    public void clear();
    
    /**
     * Set to time the last refresh of the bucket 
     * @param time the time of bucket refresh
     */
    public void setLastRefresh(long time);
    
    /**
     * Returns whether or not this Bucket needs to be refreshed
     */
    public boolean isRefreshRequired();
    
    /**
     * Returns true if the given Contact is the local Node
     */
    //public boolean isLocalNode(Contact node);

}
package unito.likir.routing;

import java.util.Collection;

import unito.likir.NodeId;

/**
 * The Kademlia route table
 * @author Luca Maria Aiello
 * @version 0.1
 */

public interface RouteTable
{
    /**
     * Selection mode for the RouteTable's select() operation
     */
    public static enum SelectMode
    {
        /**
         * Selects all Contacts
         */
        ALL,
        
        /**
         * Selects only alive Contacts
         */
        ALIVE,
        
        /**
         * Selects only alive and the local Contacts
         */
        ALIVE_WITH_LOCAL;
    }
    
    /**
     * Rebuild mode for the RouteTable's purge() operation
     */
    public static enum PurgeMode
    {
        /**
         * Drops all Contacts in the replacement cache
         */
        DROP_CACHE,
        
        /**
         * Deletes all non-alive Contacts from the active
         * RouteTable and fill up the new slots with alive
         * Contacts from the replacement cache
         */
        PURGE_CONTACTS,
        
        /**
         * Merges all Buckets by essentially rebuilding the
         * entire RouteTable
         */
        MERGE_BUCKETS,
        
        /**
         * Marks all Contacts as unknown
         */
        STATE_TO_UNKNOWN,
    }
    public Contact getLocalNode();
    
	public boolean isLocalNode(Contact contact);
	
    public Bucket getBucket(NodeId nodeId);
    
    public Collection<Bucket> getAllBuckets();
    
    public Contact getContact(NodeId nodeId);
    
    public Collection<Contact> getAllBucketContacts();
    
    public Collection<Contact> getAllCachedContacts();
    
    /**
     * Returns 'count' number of Contacts that are nearest (XOR distance)
     * to the given NodeId.
     */
    public Collection<Contact> select(NodeId nodeId, int count);
    
    public Collection<Contact> select(final NodeId nodeId, final int count, final SelectMode mode);
    
    /**
     * Returns NodeIds to be refreshed
     * @return
     */
    public Collection<NodeId> getRefreshIDs(boolean bootstrap);
    
    public void resetRefreshTimes();

	/**
	 * COMMENTA
	 * @param contact
	 */
	public void add(Contact contact);
	
    /**
     * This method tries to replace an existing Contact in the given
     * Bucket with the given Contact or tries to add the given Contact
     * to the Bucket's replacement Cache. There are certain conditions
     * in which cases we replace Contacts and if it's not possible we're
     * trying to add the Contact to the replacement cache.
     */
    public void replaceContactInBucket(Bucket bucket, Contact node);
    
    /**
     * 
     */
    public void fill(Collection<Contact> contacts);
    
    /**
     * Removes the given Contact from the RouteTable
     * @return the removed contact
     */
    public Contact remove(Contact node);
    
    /**
     * Removes the Contact with the given NodeId from the RouteTable
     * @return the removed contact
     */
    public Contact remove(NodeId nodeId);
    
    /**
     * This method splits the given Bucket into two new Buckets.
     * There are three conditions for splitting:
     * 1. Bucket contains the local Node
     * 2. New node part of the smallest subtree to the local node
     * 3. current_depth mod symbol_size != 0 //SPIEGA MEGLIO
     */
    public boolean split(Bucket bucket);
    
	/**
	 * Handles a failed probing for NodeID
	 */
	public void handleFailure(NodeId nodeId);
	
    public void clear();
    
    public int size();
}

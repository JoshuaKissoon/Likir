package unito.likir.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.io.IOException;
import java.io.Serializable;

import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.settings.PropFinder;
import unito.likir.settings.Settings;
import unito.likir.util.PatriciaTrie;
import unito.likir.util.Trie.Cursor;

/**
 * A simple RouteTable implementation
 * Adapted from:
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 * @author Luca Maria Aiello
 */

public class RouteTableImpl implements RouteTable, Serializable
{
	private static final long serialVersionUID = 4365589086316081729L;
	
	private Node controlNode;
	private Contact localNode;
	private PatriciaTrie<NodeId,Bucket> bucketTrie;
    private Bucket smallestSubtreeBucket;
    private int consecutiveFailures;
	
    private final int MAX_ACCEPT_NODE_FAILURES;
    //private final int BUCKET_REFRESH_PERIOD;
    
	/**
	 * 
	 * @param nodeId
	 */
	public RouteTableImpl(Node controlNode)
	{
		this.controlNode = controlNode;
		this.localNode = new ContactImpl(controlNode.getNodeId(), controlNode.getAddress());
		this.smallestSubtreeBucket = null;
		bucketTrie = new PatriciaTrie<NodeId,Bucket>(NodeId.KEY_ANALYZER);
		this.MAX_ACCEPT_NODE_FAILURES = Integer.parseInt(PropFinder.get(Settings.MAX_ACCEPT_NODE_FAILURES));
		//this.BUCKET_REFRESH_PERIOD = Integer.parseInt(PropFinder.get(Settings.BUCKET_REFRESH_PERIOD));
		this.consecutiveFailures = 0;
		init();
	}
	
	public void init()
	{
		Bucket bucket = new BucketNode(this, NodeId.MINIMUM, 0);
		bucketTrie.put(NodeId.MINIMUM, bucket);
		
        addContactToBucket(bucket, localNode);
        //consecutiveFailures = 0;
	}
	
	public Contact getLocalNode()
	{
		return localNode;
	}
	
	public boolean isLocalNode(Contact contact)
	{
		return contact.getNodeId().equals(localNode.getNodeId());
	}
	
    public synchronized Bucket getBucket(NodeId nodeId)
    {
        return bucketTrie.select(nodeId);
    }
    
    public synchronized Collection<Bucket> getAllBuckets()
    {
        return Collections.unmodifiableCollection(bucketTrie.values());
    }
    
    private void touchBucket(Bucket bucket)
    {
        bucket.touch();
    }
    
    public synchronized Contact getContact(NodeId nodeId)
    {
        return bucketTrie.select(nodeId).get(nodeId);
    }
    
    public synchronized Collection<Contact> getAllBucketContacts()
    {
        List<Contact> nodes = new ArrayList<Contact>();
        for (Bucket bucket : bucketTrie.values())
        {
            nodes.addAll(bucket.getAllBucketContacts());
        }
        return nodes;
    }
    
    public synchronized Collection<Contact> getAllCachedContacts() 
    {
        List<Contact> nodes = new ArrayList<Contact>();
        for (Bucket bucket : bucketTrie.values())
        {
            nodes.addAll(bucket.getAllCachedContacts());
        }
        return nodes;
    }
    
    /**
     * Returns 'count' number of Contacts that are nearest (XOR distance)
     * to the given NodeId.
     */
    public synchronized Collection<Contact> select(NodeId nodeId, int count)
    {
        return select(nodeId, count, SelectMode.ALIVE);
    }
    
    public synchronized Collection<Contact> select(final NodeId nodeId, final int count, final SelectMode mode)
    {
        if (count == 0)
            return Collections.emptyList();
        
        final int maxNodeFailures = MAX_ACCEPT_NODE_FAILURES;
        
        final List<Contact> nodes = new ArrayList<Contact>(count);//output list
        
        //iterates over the Trie, starting from the bucket whose id is the nearest to nodeId
        bucketTrie.select(nodeId, new Cursor<NodeId, Bucket>()
        {
            public SelectStatus select(Entry<? extends NodeId, ? extends Bucket> entry)
            {
                Bucket bucket = entry.getValue();
                
                Collection<Contact> list = null;
                
                //select all bucket's contacts
                list = bucket.select(nodeId, bucket.getBucketSize());
                
                for(Contact node : list)
                {
                    // Exit the loop if done
                    if (nodes.size() >= count)
                    {
                        return SelectStatus.EXIT;
                    }
                    
                    // Ignore all non-alive Contacts if only active Contacts are requested.
                    // Ignore also the local contact here (see LocalContact.isAlive)
                    // because a node will always have himself in the routing table
                    if (mode == SelectMode.ALIVE && (!node.isAlive() || isLocalNode(node)))
                    {
                        continue;
                    }
                    
                    if (mode == SelectMode.ALIVE_WITH_LOCAL && !node.isAlive() && !isLocalNode(node))
                    {
                        continue;
                    }
                    
                    if (node.isDead())
                    {
                    	//adds the dead node to the output list only  if it's failure factor is less than
                    	//a random value in [0,1]
                    	
                    	//failure factor
                        float fact = (maxNodeFailures - node.getFailures()) / (float)Math.max(1, maxNodeFailures);
                        
                        if (Math.random() >= fact)
                        {
                            continue;
                        }
                    }
                    
                    nodes.add(node);//adds the contact to the output list
                }
                
                return SelectStatus.CONTINUE;
            }
        });
        
        return nodes;
    }
    
    /**
     * This method adds the given Contact to the given Bucket.
     */
    protected synchronized void addContactToBucket(Bucket bucket, Contact contact)
    {
        bucket.addBucketContact(contact);
        bucket.touch();
    }
	
    /**
     * Adds the given Contact to the Bucket's replacement Cache
     */
    protected synchronized void addContactToBucketCache(Bucket bucket, Contact contact)
    {
        // If the cache is full the least recently seen node will be evicted!
        bucket.addCachedContact(contact);
    }
    
	/**
	 * COMMENTA
	 * @param contact
	 */
	public synchronized void add(Contact contact)
	{
		consecutiveFailures = 0;
		
		if (contact.getNodeId().equals(localNode.getNodeId()))
		{
			// The local node cannot be added to the route table
			//throw new IllegalArgumentException("Cannot add the local node to the route table");
			return;
		}
		NodeId nodeId = contact.getNodeId();
        Bucket bucket = bucketTrie.select(nodeId);//selects the bucket whose id is the closest to contact's id

        if (!bucket.containsBucketContact(contact.getNodeId()))
        {
        	if (!bucket.isBucketFull())
        	{
        		addContactToBucket(bucket, contact);
                //addContactToCache(bucket, contact);
        	}
        	else if (split(bucket))
        	{
        		add(contact); // re-try to add the contact after the bucket splitting
        	}
        	else
        	{
        		//try to replace an old bucket contact, or add it to bucket's cache 
        		replaceContactInBucket(bucket, contact); 
        	}
        }
        else
        {
        	bucket.get(contact.getNodeId()).refresh();
        }
        
        bucket.touch();
	}
	
    /**
     * This method tries to replace an existing Contact in the given
     * Bucket with the given Contact or tries to add the given Contact
     * to the Bucket's replacement Cache. There are certain conditions
     * in which cases we replace Contacts and if it's not possible we're
     * trying to add the Contact to the replacement cache.
     */
    public synchronized void replaceContactInBucket(Bucket bucket, Contact node)
    {
        if (node.isAlive())
        {
            Contact leastRecentlySeen = bucket.getLeastRecentlySeenBucketContact();
            
            //The LRS node can be replaced if it is not the local node, AND it is DEAD
            if (!isLocalNode(leastRecentlySeen) && (leastRecentlySeen.isDead()))
            {
                //replace the node in the bucket
                bucket.removeBucketContact(leastRecentlySeen.getNodeId());//remove the LRS node from the bucket
                bucket.addBucketContact(node);//add the new node to the bucket
                touchBucket(bucket);
                
                return;
            }
        }
        addContactToBucketCache(bucket, node);
        try
        {
        	controlNode.ping(bucket.getLeastRecentlySeenBucketContact());
        }
        catch(IOException ioe)
        {
        	//log
        	System.err.println("Error in ping");
        }
    }
    
    public synchronized void fill(Collection<Contact> contacts)
    {
    	for (Contact c : contacts)
    		add(c);
    }
    
    public synchronized Contact remove(Contact node)
    {
        return remove(node.getNodeId());
    }
    
    public synchronized Contact remove(NodeId nodeId)
    {
        return bucketTrie.select(nodeId).remove(nodeId);
    }
    
    public synchronized boolean split(Bucket bucket)
    {
        boolean containsLocalNode = bucket.containsBucketContact(getLocalNode().getNodeId());
        if ((containsLocalNode || bucket.equals(smallestSubtreeBucket)) && !bucket.isTooDeep())
        {  
            List<Bucket> buckets = bucket.split();
            
            Bucket left = buckets.get(0);
            Bucket right = buckets.get(1);
            
            if (containsLocalNode)
            {
                if (left.containsBucketContact(getLocalNode().getNodeId()))
                    smallestSubtreeBucket = right;
                else if (right.containsBucketContact(getLocalNode().getNodeId()))
                    smallestSubtreeBucket = left;
                else
                    throw new IllegalStateException("Neither left nor right Bucket contains the local Node");
            }
            
            // The left one replaces the current bucket in the Trie!
            bucketTrie.put(left.getBucketId(), left);
            bucketTrie.put(right.getBucketId(), right);
           
            //the bucket is split
            return true;
        }
        return false;
    }
    
    public synchronized Collection<NodeId> getRefreshIDs(boolean bootstrap)
    {
    	Collection<NodeId> toRefresh = new ArrayList<NodeId>();
    	Collection<Bucket> buckets = getAllBuckets();
    	for (Bucket b : buckets)
    	{
    		if (b.isRefreshRequired())
    		{
    			NodeId id = NodeId.createWithPrefix(b.getBucketId(), b.getDepth());
    			toRefresh.add(id);
    		}
    	}
    	return toRefresh;
    }
    
    public void resetRefreshTimes()
    {
    	Collection<Bucket> buckets = getAllBuckets();
    	for (Bucket b : buckets)
    		b.setLastRefresh(0);
    }
    
    public synchronized void clear()
    {
        bucketTrie.clear();
        init();
    }
    
    public synchronized int size() 
    {
        return getAllBucketContacts().size() + getAllCachedContacts().size();
    }
    
    /*private synchronized void mergeBuckets()
    {
        // Get the active Contacts
        Collection<Contact> activeNodes = getBucketContacts();
        activeNodes = ContactUtils.sortAliveToFailed(activeNodes);
        
        // Get the cached Contacts
        Collection<Contact> cachedNodes = getCachedContacts();
        cachedNodes = ContactUtils.sort(cachedNodes);
        
        // We count on the fact that getActiveContacts() and 
        // getCachedContacts() return copies!
        clear();
        
        // Remove the local Node from the List. Shouldn't fail as 
        // activeNodes is a copy!
        boolean removed = activeNodes.remove(localNode);
        assert (removed);
        
        // Re-add the active Contacts
        for (Contact node : activeNodes) 
            add(node);
        
        // Re-add the cached Contacts
        for (Contact node : cachedNodes)
            add(node);
    }*/
    
    /**
     * 
     */
    public synchronized void handleFailure(NodeId nodeId)
    {
        if (nodeId == null)
        {
            return;
        }
        
        // This should never happen
        if(nodeId.equals(localNode.getNodeId()))
        {
            //throw new IllegalArgumentException("Can't handle a failure for local node");
        	return;
        }
        
        Bucket bucket = bucketTrie.select(nodeId);
        Contact node = bucket.get(nodeId);
        if (node == null)
        {
            // None of the contacts in the route table has the specified nodeId
            return;
        }
        
        // Ignore failure if we start getting to many disconnections in a row
        /*if (consecutiveFailures >= RoutingSettings.MAX_CONSECUTIVE_FAILURES)
        {
            if (LOG.isTraceEnabled())
            {
                LOG.trace("Ignoring node failure as it appears that we are disconnected");
            }
            return;
        }*/
        
        node.handleFailure();

        if (node.isDead())
        {
            if (bucket.containsBucketContact(nodeId)) // the contact is in the bucket
            {
                // The node has failed too many times and have to be replaced
                // with the MRS node from the cache. If the cache is empty
                // the dead node will remain in the bucket and will be removed
                // with the next "replaceContactInBucket"
            	bucket.removeBucketContact(nodeId);
                if (bucket.getCacheSize() > 0)
                {
                    Contact mrs = null;
                    while((mrs = bucket.getMostRecentlySeenCachedContact()) != null)
                    {
                        bucket.removeCachedContact(mrs.getNodeId());

                        //bucket.removeBucketContact(nodeId);
                        //assert (bucket.isBucketFull() == false);
                           
                        bucket.addBucketContact(mrs);
                        break;
                    }
                }
            }
            else // the contact is in the cache
            {
                // On first glance this might look like as if it is
                // not necessary since we're never contacting cached
                // Contacts but that's not absolutely true. FIND_NODE
                // lookups may return Contacts that are in our cache
                // and if they don't respond we want to remove them...
            	
                bucket.removeCachedContact(nodeId);
            }
        }
        
        //TODO:gestione fallimenti provvisoria! modifica
        if (++consecutiveFailures == 20)
        {
        	System.err.println(controlNode.getUserId() + " DISCONNECTING (too many falures)");
        	controlNode.exit(true);
        }
    }
    
    public synchronized String toString()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Local: ").append(getLocalNode()).append("\n");
        
        int alive = 0;
        int dead = 0;
        int unknown = 0;
        
        for(Bucket bucket : getAllBuckets())
        {
            buffer.append(bucket).append("\n");
            
            for (Contact node : bucket.getAllBucketContacts())
            {
                if (node.isAlive())
                {
                    alive++;
                }
                else if (node.isDead())
                {
                    dead++;
                }
                else
                {
                    unknown++;
                }
            }
            
            for (Contact node : bucket.getAllCachedContacts())
            {
                if (node.isAlive())
                {
                    alive++;
                }
                else if (node.isDead())
                {
                    dead++;
                }
                else
                {
                    unknown++;
                }
            }
        }
        
        buffer.append("Total Buckets: ").append(bucketTrie.size()).append("\n");
        buffer.append("Total Active Contacts: ").append(getAllBucketContacts().size()).append("\n");
        buffer.append("Total Cached Contacts: ").append(getAllCachedContacts().size()).append("\n");
        buffer.append("Total Alive Contacts: ").append(alive).append("\n");
        buffer.append("Total Dead Contacts: ").append(dead).append("\n");
        buffer.append("Total Unknown Contacts: ").append(unknown);
        return buffer.toString();
    }
    
}

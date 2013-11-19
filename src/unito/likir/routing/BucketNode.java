package unito.likir.routing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import unito.likir.NodeId;
import unito.likir.settings.PropFinder;
import unito.likir.settings.Settings;
import unito.likir.util.PatriciaTrie;
import unito.likir.util.Trie;
import unito.likir.util.TrieUtils;
import unito.likir.util.Trie.Cursor;

/**
 * Kademlia K-Bucket implementation
 * @author Luca Maria Aiello
 * @version 0.1
 */
public class BucketNode implements Bucket
{
	private static final long serialVersionUID = 8779723607407382745L;
	
	private final NodeId bucketId;
	private final RouteTableImpl routeTable;
	private Trie<NodeId,Contact> contacts;
	private Trie<NodeId,Contact> cache;
	private long lastRefresh;
	private final int depth;
	
	private final int K;
	private final int B;
	private final int BUCKET_REFRESH_PERIOD;
	private final int CACHE_SIZE;
	
	/**
	 * COMMENTA
	 * @param routeTable
	 * @param bucketId
	 * @param depth
	 */
	public BucketNode(RouteTableImpl routeTable, NodeId bucketId, int depth)
	{
		this.bucketId = bucketId;
		this.routeTable = routeTable;
		this.contacts = new PatriciaTrie<NodeId,Contact>(NodeId.KEY_ANALYZER);
		this.cache = new PatriciaTrie<NodeId,Contact>(NodeId.KEY_ANALYZER);
		this.depth = depth;
		this.lastRefresh = 0;
		
		this.K = Integer.parseInt(PropFinder.get(Settings.K));
		this.B = Integer.parseInt(PropFinder.get(Settings.B));
		this.BUCKET_REFRESH_PERIOD = Integer.parseInt(PropFinder.get(Settings.BUCKET_REFRESH_PERIOD));
		this.CACHE_SIZE = Integer.parseInt(PropFinder.get(Settings.CACHE_SIZE));
	}

    public NodeId getBucketId()
    {
    	return bucketId;
    }

    public int getDepth()
    {
    	return depth;
    }

	public RouteTableImpl getRouteTable()
	{
		return routeTable;
	}
	
    public long getLastRefresh()
    {
    	return lastRefresh;
    }
    
    public void setLastRefresh(long time)
    {
    	this.lastRefresh = time;
    }

    public void touch()
    {
    	this.lastRefresh = System.currentTimeMillis();
    }
    
    public boolean isTooDeep()
    {
        return depth / B != 0;
    }
    
    /**
     * 
     * @param node
     * @return
     */
    public boolean isLocalNode(Contact contact)
    {
        return routeTable.isLocalNode(contact);
    }
    
    public Contact get(NodeId nodeId)
    {
    	Contact res = getBucketContact(nodeId);
    	if (res == null)
    		return getCachedContact(nodeId);
    	else
    		return res;
    }
    
    public Contact getBucketContact(NodeId nodeId)
    {
    	return contacts.get(nodeId);
    }
    
    public Contact getCachedContact(NodeId nodeId)
    {
    	return cache.get(nodeId);
    }
    
    public Contact select(NodeId nodeId)
    {
    	return contacts.select(nodeId);
    }

    public Collection<Contact> select(NodeId nodeId, int count)
    {
    	return TrieUtils.select(contacts, nodeId, count);
    }
    
    public boolean addBucketContact(Contact node)
    {
        if(!isBucketFull())
        {
        	contacts.put(node.getNodeId(), node);
        	//Gestire il caso in cui il risultato della put sia != NULL??
        	if(node.isAlive())
                touch();
        	return true;
        }
        else
        	return false;
    }

    public void addCachedContact(Contact node) //RAFFINARE LA TECNICA DI REPLACEMENT?
    {
    	if (isCacheFull())
    	{
    		Contact lrs = getLeastRecentlySeenCachedContact();
    		cache.remove(lrs.getNodeId());
    	}
    	cache.put(node.getNodeId(), node);
    }
    
    public Contact remove(NodeId nodeId)
    {
    	Contact c = removeBucketContact(nodeId);
    	if (c == null)
    		return removeCachedContact(nodeId);
    	else
    		return c;
    }
    
    public Contact removeBucketContact(NodeId nodeId)
    {
    	return contacts.remove(nodeId);
    }

    public Contact removeCachedContact(NodeId nodeId)
    {
    	return cache.remove(nodeId);
    }

    public boolean containsBucketContact(NodeId nodeId)
    {
    	return contacts.containsKey(nodeId);
    }

    public boolean containsCachedContact(NodeId nodeId)
    {
    	return cache.containsKey(nodeId);
    }

    public boolean isBucketFull()
    {
    	return contacts.size() == K;
    }

    public boolean isCacheFull()
    {
    	return cache.size() == CACHE_SIZE;
    }

    public Collection<Contact> getAllBucketContacts()
    {
    	return contacts.values();
    }

    public Collection<Contact> getAllCachedContacts()
    {
    	return cache.values();
    }

    public Contact getLeastRecentlySeenBucketContact()
    {
        final Contact[] leastRecentlySeen = new Contact[]{ null };
        contacts.traverse(
        		new Cursor<NodeId, Contact>()
        		{
        			public SelectStatus select(Map.Entry<? extends NodeId, ? extends Contact> entry)
        			{
        				Contact node = entry.getValue();
        				Contact lrs = leastRecentlySeen[0];
        				if (lrs == null || node.getLastContact() < lrs.getLastContact())
        					leastRecentlySeen[0] = node;
        				
        				return SelectStatus.CONTINUE;
        			}
        		}
        		);
        return leastRecentlySeen[0];
    }

    public Contact getMostRecentlySeenBucketContact()
    {
        final Contact[] mostRecentlySeen = new Contact[]{ null };
        contacts.traverse(
        		new Cursor<NodeId, Contact>()
        		{
        			public SelectStatus select(Map.Entry<? extends NodeId, ? extends Contact> entry)
        			{
        				Contact node = entry.getValue();
        				Contact mrs = mostRecentlySeen[0];
        				if (mrs == null || node.getLastContact() > mrs.getLastContact())
        					mostRecentlySeen[0] = node;

        				return SelectStatus.CONTINUE;
        			}
        		}
        		);
        return mostRecentlySeen[0];
    }

    public Contact getLeastRecentlySeenCachedContact()
    {
        if (getAllCachedContacts().isEmpty())
            return null;
        
        return getAllCachedContacts().iterator().next();
    }

    public Contact getMostRecentlySeenCachedContact()
    {
        Contact node = null;
        for(Contact n : getAllCachedContacts())
            node = n;
        
        return node;
    }
    
    public void purge()
    {
    	//removes DEAD contacts from the bucket
        for (Iterator<Contact> it = contacts.values().iterator(); it.hasNext();)
        {
            Contact node = it.next();
            if(!node.isAlive() && !isLocalNode(node))
                it.remove();
        }
        
        //replaces empty entries in the bucket with cache entries
        if(!isBucketFull() && !cache.isEmpty())
        {
            // The cache Map is in LRS order. Add the Contacts to a List and 
            // iterate it backwards so that we get the elements in MRS order.
            List<Contact> cont = new ArrayList<Contact>(getAllCachedContacts());
            for(int i = cont.size()-1; i>=0 && !isBucketFull(); i--)
            {
                Contact node = cont.get(i);
                
                if(node.isAlive())
                	contacts.put(node.getNodeId(), node);
                
                cache.remove(node.getNodeId());
            }
        }
    }

    public List<Bucket> split()
    {
        //assert (getAllCachedContacts().isEmpty() == true);
    	
        if (contacts.size() == 0)
        	return null;
        //creates two buckets
        //the first's id is the same id of the original bucket
        //the latter's id is the same id of the original bucket with the "depth"th bit set to 1
        
        Bucket left = new BucketNode(routeTable, bucketId, depth+1);
        Bucket right = new BucketNode(routeTable, bucketId.set(depth), depth+1);
        
        for (Contact node : getAllBucketContacts())
        {
        	//Contacts are split on the base of the 'depth'th bit of their NodeId
            NodeId nodeId = node.getNodeId();
            if (!nodeId.isBitSet(depth))
            	left.addBucketContact(node);
            else
            	right.addBucketContact(node);

        }

        return Arrays.asList(left, right);
    }
    
    public int size()
    {
    	return getBucketSize() + getCacheSize();
    }
    
    public int getBucketSize()
    {
    	return contacts.size();
    }
    
    public int getCacheSize()
    {
    	return cache.size();
    }
    
    public int getMaxBucketSize()
    {
    	return K;
    }
    
    public int getMaxCacheSize()
    {
    	return CACHE_SIZE;
    }

    public void clear()
    {
    	contacts.clear();
        cache.clear();
    }
    
    public boolean isRefreshRequired()
    {
        if ((System.currentTimeMillis() - getLastRefresh()) >= BUCKET_REFRESH_PERIOD)
            return true;
        else
        	return false;
    }
    
    /**
     * Returns the string representation of this object
     * @return a string representing this Bucket
     */
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append("BucketId = ").append(getBucketId()).append(" --- ");
        buffer.append("(depth=").append(getDepth())
            .append(", active=").append(getBucketSize())
            .append(", cache=").append(getCacheSize()).append(")\n");
        
        Iterator<Contact> it = getAllBucketContacts().iterator();
        for(int i = 0; it.hasNext(); i++)
        {
            buffer.append(" ").append(i).append(": ").append(it.next()).append("\n");
        }
        
        if (!getAllCachedContacts().isEmpty())
        {
            buffer.append("---cache--\n");
            it = getAllCachedContacts().iterator();
            for(int i = 0; it.hasNext(); i++)
            {
                buffer.append(" ").append(i).append(": ").append(it.next()).append("\n");
            }
        }
        
        return buffer.toString();
    }
}

package unito.likir.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.LinkedList;

import unito.likir.NodeId;
import unito.likir.settings.PropFinder;
import unito.likir.settings.Settings;

/**
 * A simple implementation of a Storage
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class StorageImpl implements Storage
{
	private static final long serialVersionUID = 6810336673059288882L;
	
	private final int MAX_CONTENT_SIZE;
	
	private HashMap<NodeId,HashMap<String,HashMap<String,List<StorageEntry>>>> store;

	/**
	 * Create a new StorageImpl instance
	 */
	public StorageImpl()
	{
		this.MAX_CONTENT_SIZE = Integer.parseInt(PropFinder.get(Settings.MAX_CONTENT_SIZE));
		store = new HashMap<NodeId,HashMap<String,HashMap<String,List<StorageEntry>>>>();//(StorageSettings.STORE_INITIAL_SIZE);
	}
	
	public boolean isEmpty()
	{
		return store.isEmpty();
	}

    public synchronized boolean store(StorageEntry entry)
    {
    	NodeId key = entry.getKey();
    	String type = entry.getContent().getType();
    	String ownerId = entry.getOwnerId();

    	HashMap<String,HashMap<String, List<StorageEntry>>> keyBucket = store.get(key);
    	if (keyBucket != null)// there is a key-bucket corresponding to key
    	{
    		HashMap<String, List<StorageEntry>> typeBucket = keyBucket.get(type);
    		if (typeBucket != null)//there is a type-bucket corresponding to type
    		{
    			List<StorageEntry> entryList = typeBucket.get(ownerId);
    			if (entryList != null) // there is a user-bucket corresponding to ownerId
        		{
        			for (StorageEntry e : entryList)
        			{
        				if (e.getSubmissionTime() == entry.getSubmissionTime())
        				{
        					e.refreshRepublishTime();
        					return true;
        				}
        			}
        			entryList.add(0,entry); // adds the entry to the existing user-list
        		}
        		else // there is no user-bucket corresponding to ownerId
        		{
        			// creates a new user-list containing the new entry
        			ArrayList<StorageEntry> userList = new ArrayList<StorageEntry>();
        			userList.add(0,entry);
        			// adds the user-list to the existing user-bucket
        			typeBucket.put(ownerId,userList);
        		}
    		}
    		else //there is no type-bucket corresponding to type
    		{
    			// creates a new user-list containing the new entry
    			ArrayList<StorageEntry> userList = new ArrayList<StorageEntry>();
    			userList.add(0,entry);
    			// creates a new type-bucket
    			HashMap<String,List<StorageEntry>> newTypeBucket = new HashMap<String,List<StorageEntry>>();
    			newTypeBucket.put(ownerId, userList);
    			// add the type bucket to the existing key bucket
    			keyBucket.put(type, newTypeBucket);
    		}
    	}
    	else // there is no key-bucket corresponding to key
    	{
    		// creates a new user-list containing the new entry
    		ArrayList<StorageEntry> userList = new ArrayList<StorageEntry>();
			userList.add(0,entry);
			// creates a new type-bucket containing the user-list
			HashMap<String,List<StorageEntry>> newTypeBucket = new HashMap<String,List<StorageEntry>>();
			newTypeBucket.put(ownerId, userList);
			//Creates a new key-bucket containing the type-bucket
			HashMap<String,HashMap<String,List<StorageEntry>>> newKeyBucket = new HashMap<String,HashMap<String,List<StorageEntry>>>();
			newKeyBucket.put(type, newTypeBucket);
			// adds the user-bucket to the store
			store.put(key,newKeyBucket);
    	}
    	
    	return true; //TODO: esaminare i casi in cui restituisce false
    }
    
    /*public synchronized int getCount(NodeId key, String type, String ownerId, boolean recent)
    {
    	List<StorageEntry> list = get(key, type, ownerId, recent);
    	if (list == null)
    		return 0;
    	else
    		return list.size();
    }*/
    
    public synchronized HashMap<String, Integer> getCount(NodeId key, String type, String ownerId, boolean recent)
    {
    	HashMap<String, Integer> result = new HashMap<String, Integer>();
    	
    	HashMap<String,HashMap<String,List<StorageEntry>>> keyBucket = store.get(key);
    	if (keyBucket != null)
    	{
    		if (type != null && ownerId != null)
    		{
    			HashMap<String,List<StorageEntry>> typeBucket = keyBucket.get(type);
    			if (typeBucket != null)
    			{
    				List<StorageEntry> userBucket = typeBucket.get(ownerId);
    				if (userBucket != null)
    				{
	    				if (recent)
	    				{
	    					result.put(ownerId, 1);
	    				}
	    				else
	    				{
	    					result.put(ownerId, userBucket.size());
	    				}
    				}
    				else
    					return null;
    			}
    			else
    			{
    				return null;
    			}
    		}
    		else if (type != null && ownerId == null)
    		{
    			HashMap<String,List<StorageEntry>> typeBucket = keyBucket.get(type);
    			if (typeBucket != null)
    			{
    				if (recent)
    				{
    					for (String owner : typeBucket.keySet())
    						result.put(owner,1);
    				}
    				else
    				{
    					for (String owner : typeBucket.keySet())
    						result.put(owner,typeBucket.get(owner).size());
    				}
    			}
    			else
    			{
    				return null;
    			}
    		}
    		else if (type == null && ownerId != null)
    		{
    			if (recent)
    			{
    				for (String typeName : keyBucket.keySet())
    				{
    					HashMap<String,List<StorageEntry>> typeBucket = keyBucket.get(typeName);
    					if (typeBucket != null)
    					{
	    					List<StorageEntry> ownerList = typeBucket.get(ownerId);
	    					if (ownerList != null)
	    						result.put(typeName, 1);
    					}
    					else 
    						return null;
    				}
    			}
    			else
    			{
    				for (String typeName : keyBucket.keySet())
    				{
    					HashMap<String,List<StorageEntry>> typeBucket = keyBucket.get(typeName);
    					if (typeBucket != null)
    					{
	    					List<StorageEntry> ownerList = typeBucket.get(ownerId);
	    					if (ownerList != null)
	    						result.put(typeName, ownerList.size());
    					}
    					else
    						return null;
    				}
    			}
    		}
    		else
    		{
    			if (recent)
    			{
    				for (String typeName : keyBucket.keySet())
    				{
    					int c = keyBucket.get(typeName).size();
    					result.put(typeName, c);
    				}
    			}
    			else
    			{
    				for (String typeName : keyBucket.keySet())
    				{
    					HashMap<String,List<StorageEntry>> typeBucket = keyBucket.get(typeName);
    					int c = 0;
    					if (typeBucket != null)
    					{
    						for (List<StorageEntry> l : typeBucket.values())
    							c+=l.size();
    						result.put(typeName,c);
    					}	
    					else
    						return null;
    				}
    			}

    		}
    		return result;
    	}
    	else
    		return null;
    }
    
    public synchronized List<StorageEntry> get(NodeId key, String type, String ownerId, boolean recent)
    {
    	List<StorageEntry> result = new ArrayList<StorageEntry>();
    	HashMap<String,HashMap<String,List<StorageEntry>>> keyBucket = store.get(key);
    	if (keyBucket != null)
    	{
    		if (type != null && ownerId != null)
    		{
    			HashMap<String,List<StorageEntry>> typeBucket = keyBucket.get(type);
    			if (typeBucket != null)
    			{
    				List<StorageEntry> userBucket = typeBucket.get(ownerId);
    				if (recent)
    				{
        				result.add(userBucket.get(0));
    				}
    				else
    				{
    					result = userBucket;
    				}
    			}
    			else
    			{
    				return null;
    			}
    		}
    		else if (type != null && ownerId == null)
    		{
    			HashMap<String,List<StorageEntry>> typeBucket = keyBucket.get(type);
    			if (typeBucket != null)
    			{
    				if (recent)
    				{
    					for (List<StorageEntry> list : typeBucket.values())
    						result.add(list.get(0));
    				}
    				else
    				{
    					for (List<StorageEntry> list : typeBucket.values())
    						result.addAll(list);
    				}
    			}
    			else
    			{
    				return null;
    			}
    		}
    		else if (type == null && ownerId != null)
    		{
    			if (recent)
    			{
    				for (HashMap<String,List<StorageEntry>> t : keyBucket.values()) //for all the type-buckets
    				{
    					System.out.println(">>>>> " + t.size());
    					System.out.println(">>>>> " + t);
    					System.out.println(">>>>> " + t.get(ownerId));
    					System.out.println(">>>>> " + t.get(ownerId).get(0));
    					StorageEntry e = t.get(ownerId).get(0);
    					result.add(e);
    				}
    			}
    			else
    			{
    				for (HashMap<String,List<StorageEntry>> t : keyBucket.values()) //for all the type-buckets
    				{
    					result.addAll(t.get(ownerId));
    				}
    			}
    		}
    		else
    		{
    			if (recent)
    			{
    				for (HashMap<String,List<StorageEntry>> t : keyBucket.values()) //for all the type-buckets
    					for (List<StorageEntry> list : t.values())
    						result.add(list.get(0));
    			}
    			else
    			{
    				for (HashMap<String,List<StorageEntry>> t : keyBucket.values()) //for all the type-buckets
    					for (List<StorageEntry> list : t.values())
    						result.addAll(list);
    			}

    		}
    		return result;
    	}
    	else
    		return null;
    }
    
    public synchronized List<StorageEntry> getLimited(NodeId key, String type, String ownerId, boolean recent)
    {
    	List<StorageEntry> result = new ArrayList<StorageEntry>();
    	result = get(key,type,ownerId,recent);
    	if (result != null)
    	{
    		reduce(result);
    	}
    	return result;
    }
    
    private synchronized void reduce(List<StorageEntry> list)
    {
    	int size =  MAX_CONTENT_SIZE+1;
    	
    	try
    	{
	    	while (size > MAX_CONTENT_SIZE)
	    	{
	    		byte[] cont = serialize(list);
	    		size = cont.length;
	    		StorageEntry[] arr = list.toArray(new StorageEntry[0]);
	    		for (StorageEntry e : arr)
	        	{
	    			if (size <= MAX_CONTENT_SIZE)
	        			break;
	    			else
	    			{
	    				list.remove(e);
	    				size -= e.getContent().size();
	    			}
	        	}
	    	}
	    }
    	catch(IOException ioe)
    	{
    		list = new ArrayList<StorageEntry>();
    	}
    }
    
	private synchronized byte[] serialize(Object msg) throws IOException
	{
		//compose the UDP packet to be sent
		byte[] data;
		
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    ObjectOutputStream oos = new ObjectOutputStream(bos);
	    oos.writeObject(msg);
	    oos.flush();
	    data = bos.toByteArray();
	    oos.close();
	    bos.close();
	    
	    return data;
	}
    
    public synchronized HashMap<String,HashMap<String,List<StorageEntry>>> remove(NodeId key)
    {
    	return store.remove(key);
    }

    public synchronized HashMap<String,List<StorageEntry>> remove(NodeId key, String type)
    {
    	HashMap<String,HashMap<String,List<StorageEntry>>> keyBucket = store.get(key);
    	if (keyBucket != null)
    		return keyBucket.remove(type);
    	else
    		return null;
    }
    
    public synchronized List<StorageEntry> remove(NodeId key, String type, String owner)
    {
    	HashMap<String,HashMap<String,List<StorageEntry>>> keyBucket = store.get(key);
    	if (keyBucket != null)
    	{
    		HashMap<String,List<StorageEntry>> typeBucket = keyBucket.get(type);
    		return typeBucket.remove(owner);
    	}
    	else
    		return null;
    }
    
    public synchronized boolean remove(StorageEntry e)
    {
    	Collection<StorageEntry> list = get(e.getKey(),e.getContent().getType(),e.getOwnerId(),false);
    	return list.remove(e);
    }
    
    public synchronized boolean contains(NodeId key)
    {
    	return store.containsKey(key);
    }
    
    public synchronized boolean contains(NodeId key, String type)
    {
    	HashMap<String,HashMap<String,List<StorageEntry>>> keyBucket = store.get(key);
    	if (keyBucket != null)
    		return (keyBucket.get(type) != null);
    	else
    		return false;
    }
    
    public synchronized boolean contains(NodeId key, String type, String userId)
    {
    	HashMap<String,HashMap<String,List<StorageEntry>>> keyBucket = store.get(key);
    	if (keyBucket != null)
    	{
    		HashMap<String,List<StorageEntry>> typeBucket = keyBucket.get(type);
    		if (typeBucket != null)
    			return (typeBucket.get(userId) != null);
    		else
    			return false;
    	}
    	else
    		return false;
    }

    public synchronized Set<NodeId> keySet()
    {
    	return store.keySet();
    }

    public synchronized Collection<StorageEntry> values()
    {
    	Collection <StorageEntry> entries = new LinkedList<StorageEntry>(); // result collection

    	Collection<HashMap<String,HashMap<String,List<StorageEntry>>>> keyBuckets = store.values(); //collection of key-bucket

    	for (HashMap<String,HashMap<String,List<StorageEntry>>> kBucket : keyBuckets)//for every key-bucket
    	{
    		Collection<HashMap<String,List<StorageEntry>>> typeBuckets = kBucket.values();
    		for (HashMap<String,List<StorageEntry>> tBucket : typeBuckets) //for every type-bucket
    		{
    			Collection<List<StorageEntry>> userBuckets = tBucket.values();
    			for (List<StorageEntry> uBucket : userBuckets) //for every user-bucket
    				entries.addAll(uBucket);
    		}
    	}

    	return entries;
    }

    public synchronized int getKeyBucketCount()
    {
    	return store.size();
    }
    
    public synchronized int getUserBucketCount()
    {
    	int users = 0;
    	
    	Collection<HashMap<String,HashMap<String,List<StorageEntry>>>> keyBuckets = store.values();
    	for (HashMap<String,HashMap<String,List<StorageEntry>>> kBucket : keyBuckets)
    	{
    		Collection<HashMap<String,List<StorageEntry>>> typeBuckets = kBucket.values();
    		for (HashMap<String,List<StorageEntry>> tBucket : typeBuckets)
    			users += tBucket.size();
    	}
    	return users;
    }

    public synchronized int getTypeBucketCount()
    {
    	int types = 0;

    	Collection<HashMap<String,HashMap<String,List<StorageEntry>>>> keyBuckets = store.values();
    	for (HashMap<String,HashMap<String,List<StorageEntry>>> bucket : keyBuckets)
    		types += bucket.size();
    	
    	return types;
    }

    public synchronized int getEntryCount()
    {
    	return values().size();
    }

    public synchronized void clear()
    {
    	store.clear();
    }

    public synchronized String toString()
    {
    	StringBuilder buffer = new StringBuilder();
        buffer.append("STORAGE: \n").append("KeyBuckets:").append(getKeyBucketCount()).append(", TypeBuckets: ").append(getTypeBucketCount()).append(", UserBuckets:").append(getUserBucketCount()).append("\n");

    	Collection<HashMap<String,HashMap<String,List<StorageEntry>>>> keyBuckets = store.values(); //collection of key-bucket
    	for (HashMap<String,HashMap<String,List<StorageEntry>>> kBucket : keyBuckets)//for every key-bucket
    	{
    		buffer.append("Key-Bucket: \n");
    		Collection<HashMap<String,List<StorageEntry>>> typeBuckets = kBucket.values();
    		for (HashMap<String,List<StorageEntry>> tBucket : typeBuckets) //for every type-bucket
    		{
    			buffer.append("    Type-Bucket: \n");
    			Collection<List<StorageEntry>> userBuckets = tBucket.values();
    			for (List<StorageEntry> uBucket : userBuckets) //for every user-bucket
    			{
    				buffer.append("        User-Bucket: \n");
    				for ( StorageEntry e : uBucket) // for every entry in the list
        				buffer.append("            "+e+"\n");
    			}
    		}
    	}
    	return buffer.toString();
    }
}
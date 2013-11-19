package unito.likir.io;

import java.util.List;
import java.util.LinkedList;
import java.util.Collection;
import java.util.concurrent.Future;
import java.io.IOException;

import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.messages.dht.RPCMessage;
import unito.likir.messages.dht.StoreResponse;
import unito.likir.routing.Contact;
import unito.likir.settings.PropFinder;
import unito.likir.settings.Settings;
import unito.likir.storage.MultiplePutContent;
import unito.likir.storage.StorageEntry;

/**
 * IterativeStoreManager class
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class PutManager extends ObservableFutureTask<Integer>
{
	private Node node;
	
	private NodeId key;
	private String[] types = new String[1];
	private byte[][] objects = new byte[1][];
	private long[] ttls = new long[1];
	private boolean sign = true;
	
	private final int TIME_OUT;
	
	
	private int steps;
	
	public PutManager(Node node, NodeId key, String type, byte[] object, long ttl)
	{
		this.node = node;
		this.key = key;
		this.types[0] = type;
		this.objects[0] = object;
		this.ttls[0] = ttl;
		this.TIME_OUT = Integer.parseInt(PropFinder.get(Settings.TIME_OUT));
		
		this.steps = 0;
	}
	
	public PutManager(Node node, NodeId key, String[] types, byte[][] objects, long[] ttls, boolean sign)
	{
		this.node = node;
		this.key = key;
		this.types = types;
		this.objects = objects;
		this.ttls = ttls;
		this.sign = sign;
		this.TIME_OUT = Integer.parseInt(PropFinder.get(Settings.TIME_OUT));
		
		this.steps = 0;
	}
	
	public void run()
	{
		int storeCounter = 0;
		
		Collection<Contact> nearestToKey = null;
		try
		{
			LookupManager manager = (LookupManager) node.lookup(key);
			nearestToKey =  manager.get();
			steps = manager.getSteps();
		}
		catch (InterruptedException ie)
		{
			set(storeCounter);//TODO: gestisci meglio
		}
		/*catch (ExecutionException ee)
		{
			set(storeCounter);//TODO: gestisci meglio
		}*/
		
		if (nearestToKey == null)
		{
			set(0);
			return;
		}
		
		if (sign)
		{
			List<Future<RPCMessage>> replies = new LinkedList<Future<RPCMessage>>();
			
			StorageEntry[] entries = new StorageEntry[objects.length];
			
			for (int i=0; i<objects.length; i++)
			{
				entries[i] = node.getEntryFactory().buildStorageEntry(key, objects[i], types[i], ttls[i]);
			}
	
			for (Contact c : nearestToKey)
			{
				try
				{
					replies.add(node.store(c, entries));
				}
				catch (IOException ioe)
				{
					System.err.println("ERROR in iterative store");
					ioe.printStackTrace();//log
				}
			}
			steps++;
			
			RPCMessage msg;
			StoreResponse resp;
			for (Future<RPCMessage> futureMsg : replies)
			{
				try
				{
					msg = futureMsg.get(TIME_OUT, Settings.DEFAULT_TIME_UNIT);
					resp = (StoreResponse)msg.getRPC();
					if (resp.getStoreResult())
						storeCounter++;
				}
				catch(Exception e)
				{
					System.err.println("Iterative store: a contact failed to respond");
					//e.printStackTrace();//log
				}
			}
			
			set(storeCounter);
		}
		else
		{
			List<Future<RPCMessage>> replies = new LinkedList<Future<RPCMessage>>();
			
			MultiplePutContent multipleContents = new MultiplePutContent(objects, types, ttls);
			StorageEntry entry = node.getEntryFactory().buildStorageEntry(key, multipleContents.toBytes(), "", 0);
	
			for (Contact c : nearestToKey)
			{
				StoreManager task = new StoreManager(node, c, entry, sign);
				node.getExecutor().execute(task);
				replies.add(task);
			}
			steps++;
			
			RPCMessage msg;
			StoreResponse resp;
			for (Future<RPCMessage> futureMsg : replies)
			{
				try
				{
					msg = futureMsg.get(TIME_OUT, Settings.DEFAULT_TIME_UNIT);
					resp = (StoreResponse)msg.getRPC();
					if (resp.getStoreResult())
						storeCounter++;
				}
				catch(Exception e)
				{
					System.err.println("Iterative store: a contact failed to respond");
					//e.printStackTrace();//log
				}
			}
			
			set(storeCounter);
		}
	}
	
	public int getSteps()
	{
		return steps;
	}
}

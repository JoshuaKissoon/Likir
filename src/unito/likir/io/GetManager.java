package unito.likir.io;

import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.Future;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.Comparator;

import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.messages.dht.FindValueResponse;
import unito.likir.messages.dht.RPCMessage;
import unito.likir.routing.Contact;
import unito.likir.settings.PropFinder;
import unito.likir.settings.Settings;
import unito.likir.storage.StorageEntry;
import unito.likir.util.Couple;

/**
 * IterativeFindValueManager class
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class GetManager extends ObservableFutureTask<Collection<StorageEntry>>
{
	private Node node;
	private NodeId key; //the lookup key
	private String type;
	private String owner;
	private boolean recent;
	private int contentNumber;
	private TreeSet<Contact> shortList; //list of contacts improved at each step
	private TreeSet<Contact> markedList; //subset of shortList containing the probed contacts
	private List<StorageEntry> contentList;
	private final int K; //replication factor
	private final int ALPHA; //parallelism factor
	private final int TIME_OUT;
	
	private int steps;
	
	//The comparator used to sort the elements in the TreeSet instances
	//which stores the contacts retrieved from the FIND_NODE responses
	private LookupComparator comparator;
	
	//A list of Future FIND_NODE responses received at each step,
	//associated to the contact to which the FIND_NODE request id sent
	private Collection<Couple<Contact,Future<RPCMessage>>> replies;
	
	public GetManager(Node node, NodeId key, String type, String owner, boolean recent, int contentNumber)
	{
		this.node = node;
		this.type = type;
		this.key = key;
		this.owner = owner;
		this.recent = recent;
		this.contentNumber = contentNumber;
		this.comparator = new LookupComparator(key);
		this.shortList = new TreeSet<Contact>(comparator);
		this.markedList = new TreeSet<Contact>(comparator);
		this.contentList = new LinkedList<StorageEntry>();
		this.replies = new LinkedList<Couple<Contact,Future<RPCMessage>>>();
		this.K = Integer.parseInt(PropFinder.get(Settings.K));
		this.ALPHA = Integer.parseInt(PropFinder.get(Settings.ALPHA));
		this.TIME_OUT = Integer.parseInt(PropFinder.get(Settings.TIME_OUT));
		
		this.steps = 0;
	}
	
	public void run()
	{
		//The K contacts nearest to the lookup key are added to the shortList
		Collection<Contact> nearestContacts = node.getRouteTable().select(key, K);
		shortList.addAll(nearestContacts);
		
		if (shortList.isEmpty())
		{
			set(contentList);
			return;
		}
		
		//References to the node closest to target key (at the previous and at the current step)
		Contact oldClosestNode = null;
		Contact closestNode = shortList.first();
		
		//Checks if the content is stored at this node
		Collection<StorageEntry> localEntries;
		
		localEntries = node.getStorage().get(key, type, owner, recent);
		
		if (localEntries != null)
			contentList.addAll(localEntries);
		
		while (!(oldClosestNode == closestNode) && contentList.size() < contentNumber)
		{
			doQueryRound(ALPHA,shortList.size());
			steps++;
			
			oldClosestNode = closestNode;
			try
			{
				closestNode = shortList.first();
				//If the closest node is not changed, query each of the k 
				//closest nodes that it has not already queried.
				if (oldClosestNode == closestNode && contentList.size() < contentNumber)
				{
					doQueryRound(K,K);
					int oldSize = 0;
				    int size = markedList.size();
				    
					while(size < K && size != oldSize && contentList.size() < contentNumber)
					{
						doQueryRound(K - size, K);
						oldSize = size;
						size = markedList.size();
					}
				}
			}
			catch(NoSuchElementException nsee)
			{
				set(contentList);
			}
		}
		
		Collection<StorageEntry> cleanedList = node.getSecurityAgent().clean(contentList);
		
		set(cleanedList);
	}
	
	/*
	 * Scan the shortList for probing
	 * n : max number of contacts to be probed (>0)
	 * range : max index of the list (>0)
	 */
	private void doQueryRound(int n, int range)
	{
		Future<RPCMessage> futureReply;
		int i=0;
		int rangeCounter=1;
		Iterator<Contact> it = shortList.iterator(); 
		Contact c;
		
		while (it.hasNext() && rangeCounter <= range && i < n)
		{
			c = it.next();
			
			//if c is unprobed and it is not the local node then query it
			if (!markedList.contains(c) && !(node.getNodeId().equals(c.getNodeId())))
			{
				try
				{
					futureReply = node.findValue(c, key, type, owner, recent);
					replies.add(new Couple<Contact,Future<RPCMessage>>(c,futureReply));
					i++;
				}
				catch (Exception e)
				{
					//If something went wrong, discard the node from  the list
					shortList.remove(c);
				}
			}
			rangeCounter++;
		}
		
		FindValueResponse reply;
		Contact contact;
		for (Couple<Contact,Future<RPCMessage>> replyCouple : replies)
		{
			contact = replyCouple.first();
			futureReply = replyCouple.second();
			try
			{
				//waits the FIND_NODE responses for a short time
				reply = (FindValueResponse)(futureReply.get(TIME_OUT, Settings.DEFAULT_TIME_UNIT).getRPC());
				
				if (reply.getContacts() != null)
				{
					shortList.addAll(reply.getContacts());
					//if (!markedList.contains(contact))
				}
				if (reply.getValues() != null)
				{
					contentList.addAll(reply.getValues());
				}
				markedList.add(contact);
			}
			catch (Exception e)
			{
				//contacts that fail to respond quickly are removed from consideration
				shortList.remove(contact);
				System.err.println("IterativeFindVaue" + contact + " failed to respond. Removedfrom shortlist.");
			}
		}
		replies.clear();
	}
	
	private class LookupComparator implements Comparator<Contact>
	{
		private NodeId key;
		
		public LookupComparator(NodeId key)
		{
			this.key = key;
		}
		
		public NodeId getKey()
		{
			return key;
		}
		
		public int compare(Contact c1, Contact c2)
		{
			NodeId xorDistanceC1 = c1.getNodeId().xor(key);
			NodeId xorDistanceC2 = c2.getNodeId().xor(key);
			return xorDistanceC1.compareTo(xorDistanceC2);
		}
		
		public boolean equals(Object comparator)
		{
			try
			{
				LookupComparator castedComparator = (LookupComparator)comparator;
				return key.equals(castedComparator.getKey());
			}
			catch (Exception e)
			{
				return false;
			}
		}
	}
	
	public int getSteps()
	{
		return steps;
	}
	
	/* DEBUG METHOD*/
	/*private void printShortList()
	{
		System.out.println("\n+++ ShortList +++");
		for (Contact cc : shortList)
		{
			System.out.print(cc);
			if (markedList.contains(cc))
				System.out.println("  #");
			else
				System.out.println("");
		}
		System.out.println("\n+++++++++++++++++\n");
	}*/

}

package unito.likir.io;

import java.util.LinkedList;
import java.util.concurrent.Future;
import java.util.Collection;
//import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Comparator;

import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.messages.dht.FindNodeResponse;
import unito.likir.messages.dht.RPCMessage;
import unito.likir.routing.Contact;
import unito.likir.settings.PropFinder;
import unito.likir.settings.Settings;
import unito.likir.util.Couple;

/**
 * LookupManager class
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class LookupManager extends ObservableFutureTask<Collection<Contact>>
{
	private Node node;
	private NodeId key; //the lookup key
	private SortedSet<Contact> shortList; //list of contacts improved at each step
	private SortedSet<Contact> markedList; //subset of shortList containing the probed contacts
	private SortedSet<Contact> failedNodes; //nodes that had been probed during this lookup but they had not responded 
	
	//The comparator used to sort the elements in the TreeSet instances
	//which stores the contacts retrieved from the FIND_NODE responses
	private LookupComparator comparator;
	
	//A list of Future FIND_NODE responses received at each step,
	//associated to the contact to which the FIND_NODE request id sent
	private Collection<Couple<Contact,Future<RPCMessage>>> replies;
	
	private final int K; //replication factor
	private final int ALPHA; //parallelism factor
	private final int TIME_OUT;
	
	private int steps;
	
	public LookupManager(Node node, NodeId key)
	{
		super();
		this.node = node;
		this.key = key;
		this.comparator = new LookupComparator(key);
		
		this.shortList = new TreeSet<Contact>(comparator);
		this.markedList = new TreeSet<Contact>(comparator);
		this.failedNodes = new TreeSet<Contact>(comparator);
		
		this.replies = new LinkedList<Couple<Contact,Future<RPCMessage>>>();
		
		this.K = Integer.parseInt(PropFinder.get(Settings.K));
		this.ALPHA = Integer.parseInt(PropFinder.get(Settings.ALPHA));
		this.TIME_OUT = Integer.parseInt(PropFinder.get(Settings.TIME_OUT));
		
		this.steps = 0;
	}
	
	public SortedSet<Contact> getShortList()
	{
		return shortList;
	}
	
	public SortedSet<Contact> getMarkedList()
	{
		return markedList;
	}
	
	public void run()
	{
		//The K contacts nearest to the lookup key are added to the shortList
		Collection<Contact> nearestContacts = node.getRouteTable().select(key, K);
		shortList.addAll(nearestContacts);

		if (shortList.isEmpty())
		{
			set(new TreeSet<Contact>());
			return;
		}
		
		//References to the node closest to target key (at the previous and at the current step)
		Contact oldClosestNode = null;
		Contact closestNode = shortList.first();
		
		//printShortList();
		
		while (!(oldClosestNode == closestNode))
		{
			doQueryRound(ALPHA,shortList.size());
			steps++;
			//printShortList();
			
			oldClosestNode = closestNode;
			try
			{
				closestNode = shortList.first();
			
				//If the closest node is not changed, query each of the k 
				//closest nodes that it has not already queried.
				if (oldClosestNode == closestNode)
				{
					doQueryRound(K,K);
					int oldSize = 0;
				    int size = markedList.size();
				    
				    //printShortList();
				    
					while(size < K && size != oldSize)
					{
						doQueryRound(K - size, K);
						oldSize = size;
						size = markedList.size();
					}
				}
			}
			catch(NoSuchElementException nsee)
			{
				System.err.println("No more contacts to probe!");
			}
		}
		
		//Build the response collection
		Collection<Contact> result = new TreeSet<Contact>(comparator);
		int i=0;
		Contact c = null;
		while (i<K && markedList.size()>0)
		{
			c = markedList.first();
			markedList.remove(c);
			result.add(c);
			i++;
		}
		
		set(result);
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
					futureReply = node.findNode(c,key);
					replies.add(new Couple<Contact,Future<RPCMessage>>(c,futureReply));
					i++;
				}
				catch (Exception e)
				{
					//If something went wrong, discard the node from  the list
					failedNodes.add(c);
					shortList.remove(c);
					System.err.println(e);
				}
			}
			rangeCounter++;
		}
		
		FindNodeResponse reply;
		Contact contact;
		for (Couple<Contact,Future<RPCMessage>> replyCouple : replies)
		{
			contact = replyCouple.first();
			futureReply = replyCouple.second();
			try
			{
				//waits the FIND_NODE responses for a short time
				reply = (FindNodeResponse)(futureReply.get(TIME_OUT, Settings.DEFAULT_TIME_UNIT).getRPC());
				
				//removes the contact of failed nodes already probed
				Collection<Contact> newContacts = reply.getContacts();
				newContacts.removeAll(failedNodes);
				
				shortList.addAll(newContacts);
				//if (!markedList.contains(contact))
				markedList.add(contact);
			}
			catch (Exception e)
			{
				//contacts that fail to respond quickly are removed from consideration
				failedNodes.add(contact);
				shortList.remove(contact);
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

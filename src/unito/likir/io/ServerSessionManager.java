package unito.likir.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Collection;
import java.util.HashMap;

import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.exceptions.TaskException;
import unito.likir.messages.dht.DHTMessage;
import unito.likir.messages.dht.FindRequest;
import unito.likir.messages.dht.FindValueRequest;
import unito.likir.messages.dht.Nonce;
import unito.likir.messages.dht.RPCMessage;
import unito.likir.messages.dht.RPCMessageFactory;
import unito.likir.messages.dht.StoreRequest;
import unito.likir.messages.dht.RPC.OpCode;
import unito.likir.routing.Contact;
import unito.likir.routing.ContactImpl;
import unito.likir.routing.RouteTable.SelectMode;
import unito.likir.settings.PropFinder;
import unito.likir.settings.Settings;
import unito.likir.storage.MultiplePutContent;
import unito.likir.storage.StorageEntry;
//import unito.likir.test.Logger;

public class ServerSessionManager implements SessionManager, Runnable
{
	protected Node node;
	protected MessageDispatcher messageDispatcher;
	protected RPCMessage receivedRPC;
	protected RPCMessage sentRPC;
	protected Nonce sentNonce;
	protected Nonce receivedNonce;
	protected final int TIME_OUT;
	protected long sid; //session ID
	protected SocketAddress client;
	
	public ServerSessionManager(Node node, SocketAddress client, Nonce receivedNonce)
	{
		this.node = node;
		this.client = client;
		this.sid = receivedNonce.getSid();
		this.receivedNonce = receivedNonce;
		this.messageDispatcher = node.getMessageDispatcher();
		this.TIME_OUT = Integer.parseInt(PropFinder.get(Settings.TIME_OUT));
	}
	
	public RPCMessage getReceivedRPC()
	{
		return receivedRPC;
	}

	public void setReceivedRPC(RPCMessage receivedRPC)
	{
		this.receivedRPC = receivedRPC;
	}

	public RPCMessage getSentRPC()
	{
		return sentRPC;
	}

	public void setSentRPC(RPCMessage sentRPC) 
	{
		this.sentRPC = sentRPC;
	}

	public Nonce getSentNonce()
	{
		return sentNonce;
	}
	
	public void setSentNonce(Nonce sentNonce)
	{
		this.sentNonce = sentNonce;
	}

	public Nonce getReceivedNonce()
	{
		return receivedNonce;
	}

	public void setReceivedNonce(Nonce receivedNonce)
	{
		this.receivedNonce = receivedNonce;
	}
	
	public void handle(DHTMessage received)
	{
		//System.out.println(node.getUserId() + " - ServerManager : Handling \n" + received);
		DHTMessage.OpCode opcode = received.getMsgOpCode();
		
		//message has a valid opcode
		if (opcode.isRPCMessageRequest())
		{
			//System.out.println(node.getUserId() + " - ServerManager : Opcode OK");
			RPCMessage receivedMessage = (RPCMessage) received;
			
			//sender is not blacklisted
			if(!node.getBlacklist().contains(receivedMessage.getAuthNodeId().getContent().getUser()))
			{
				//System.out.println(node.getUserId() + " - ServerManager : Blacklist OK");
				//security checks are passed
				if (node.getSecurityAgent().check(receivedMessage,receivedNonce,sentNonce))
				{
					//System.out.println(node.getUserId() + " - ServerManager : Security check OK");
					//System.out.println(node.getUserId() + " - ServerManager : setting result: \n" + receivedMessage);
					synchronized(this)
					{
						receivedRPC = receivedMessage; //set the received RPC
						this.notifyAll(); //wakeup
					}
					//System.out.println(node.getUserId() + " - ServerManager : HandlED \n" + receivedMessage);
					//refresh the route table entries
					refreshRouteTable(receivedRPC);
				}
				else
				{
					System.err.println(node.getUserId() + " - ServerManager : message check failed! \n " + received);
				}
			}
			else
			{
				////System.err.println(node.getUserId() + " - ServerManager : The received message was sent by a blacklisted node");
				//node.getRouteTable().remove(receivedMessage.getAuthNodeId().getContent().getNodeId());
			}
		}
		else
		{
			////System.err.println(node.getUserId() + " - ServerManager: Invalid message for this session: \n" + received);
		}
	}
	
	private void refreshRouteTable(RPCMessage message)
	{
		NodeId senderNodeId = message.getAuthNodeId().getContent().getNodeId();
		Contact contact = new ContactImpl(senderNodeId, client);
		node.getRouteTable().add(contact);
	}
	
	public void run()
	{
		sendNonce();
		if (receiveRPC())
			sendRPC();
	}
	
	private void sendNonce()
	{
		byte[] payload = null; //payload of UDP packet
		DatagramPacket packet = null; //UDP packet
		
		//generates a new nonce
		sentNonce = new Nonce(node.getNodeId(),sid,false);
		
		//Builds the UDP packet
		try
		{
			payload = serialize(sentNonce);
			packet = new DatagramPacket(payload,payload.length,client);
		}
		catch (SocketException se)
		{
			se.printStackTrace();
			TaskException e = new TaskException("ServerManager : Error in UDP packet constuction");
			throw e;
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
			TaskException e = new TaskException("ServerManager : Error in Nonce serialization");
			throw e;
		}
		
		//Sends the Nonce
		messageDispatcher.addSessionHandler(sid,this);
		try
		{
			//System.out.println(node.getUserId() + " sending Nonce response : sid " + sid + " - to: " + packet.getAddress() +","+ packet.getPort());
			messageDispatcher.send(packet);
		}
		catch (IOException ioe)
		{
			TaskException e = new TaskException("ServerManager : Error in sending UDP packet");
			messageDispatcher.removeSessionHandler(sid);//unregister from the message dispatcher
			throw e;
		}
	}
	
	private boolean receiveRPC()
	{
		try
		{
			synchronized(this)
			{
				if (receivedRPC == null)
				{
					//System.out.println(node.getUserId() + " - ServerManager :  waiting for RPC in session " + sid);
					this.wait(TIME_OUT);
				}
			}
			if (receivedRPC == null)
			{
				////System.err.println(node.getUserId() + " - ServerManager : incoming RPC time out in session " + sid);
							
				messageDispatcher.removeSessionHandler(sid);//unregister from the message dispatcher
				return false;
			}
			else
			{
				return true;
			}
		}
		catch(InterruptedException ie)
		{
			Object x = messageDispatcher.removeSessionHandler(sid); //unregister from the message dispatcher
			System.err.println(node.getUserId() + " interrupted while waiting for response - removed " + x);
			return false;
		}
	}
	
	private void sendRPC()
	{
		OpCode code = receivedRPC.getRPCOpcode();
		
		if (code.equals(OpCode.PING_REQUEST))
		{
			handlePingRequest(receivedRPC);
		}
		else if (code.equals(OpCode.FIND_NODE_REQUEST))
		{
			handleFindNodeRequest(receivedRPC);
		}
		else if (code.equals(OpCode.FIND_VALUE_REQUEST))
		{
			handleFindValueRequest(receivedRPC);
		}
		else if (code.equals(OpCode.STORE_REQUEST))
		{
			handleStoreRequest(receivedRPC);
		}
		else
		{
			System.err.println("ServerManager : Unknown message type");
		}
		messageDispatcher.removeSessionHandler(sid);//unregister from the message dispatcher
	}
	
	private byte[] serialize(Serializable msg) throws IOException
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

	private void handlePingRequest(RPCMessage received)
	{
		RPCMessageFactory messageFactory = node.getMessageFactory();
		NodeId addressee = received.getAuthNodeId().getContent().getNodeId();
		byte[] nonce = receivedNonce.getNonce();
		RPCMessage response = messageFactory.createPingResponse(addressee,sid, nonce);
		try
		{
			byte[] payload = serialize(response);
			DatagramPacket packet = new DatagramPacket(payload,payload.length,client);
			//System.out.println(node.getUserId() + " sending Ping RPC message response : sid " + sid);
			
			node.getMessageDispatcher().send(packet);
		}
		catch (IOException ioe)
		{
			System.err.println("ERROR in handlePingRequest!");
			ioe.printStackTrace();//log
		}
	}

	private void handleFindNodeRequest(RPCMessage received)
	{
		FindRequest reqRPC = (FindRequest)received.getRPC();

		Collection<Contact> contacts = node.getRouteTable().select(reqRPC.getLookupId(), Integer.parseInt(PropFinder.get(Settings.K)), SelectMode.ALIVE_WITH_LOCAL);
		RPCMessageFactory messageFactory = node.getMessageFactory();
		NodeId addressee = received.getAuthNodeId().getContent().getNodeId();
		byte[] nonce = receivedNonce.getNonce();
		RPCMessage response = messageFactory.createFindNodeResponse(addressee,sid,nonce,contacts);
		try
		{
			byte[] payload = serialize(response);
			DatagramPacket packet = new DatagramPacket(payload,payload.length,client);
			//System.out.println(node + " - SEND RESPONSE: \n"+ response);
			//System.out.println("----------------------------------------------");
			//System.out.println(node.getUserId() + " sending FN RPC message response : sid " + sid + " - to: " + packet.getAddress() +","+ packet.getPort());
			
			node.getMessageDispatcher().send(packet);
		}
		catch (IOException ioe)
		{
			System.err.println("ERROR in handleFindNodeRequest!");
			ioe.printStackTrace();//log
		}
	}

	private void handleFindValueRequest(RPCMessage received)
	{
		try
		{
			FindValueRequest reqRPC = (FindValueRequest)received.getRPC();
			NodeId key = reqRPC.getLookupId();
			String type = reqRPC.getType();
			String ownerId = reqRPC.getOwner();
			boolean recent = reqRPC.getRecent();
			boolean countersOnly = reqRPC.getCountersOnly();
			
			Collection<Contact> contacts = null;
			Collection<StorageEntry> entries = null;
			HashMap<String, Integer> counters = null;

			if (countersOnly)
			{
				counters = node.getStorage().getCount(key, type, ownerId, recent);
			}
			else
			{
				entries = node.getStorage().getLimited(key, type, ownerId, recent);
			}
			
			if (entries == null && counters == null)
			{
				contacts = node.getRouteTable().select(key, Integer.parseInt(PropFinder.get(Settings.K)));
			}
			
			RPCMessageFactory messageFactory = node.getMessageFactory();
			NodeId addressee = received.getAuthNodeId().getContent().getNodeId();
			byte[] nonce = receivedNonce.getNonce();
			RPCMessage response = messageFactory.createFindValueResponse(addressee, sid, nonce, contacts, entries, counters);
			try
			{
				byte[] payload = serialize(response);
				DatagramPacket packet = new DatagramPacket(payload,payload.length,client);
				//System.out.println(node + " - SEND RESPONSE: \n"+ response);
				//System.out.println("----------------------------------------------");
				//System.out.println(node.getUserId() + " sending FV RPC message response : sid " + sid);
				
				node.getMessageDispatcher().send(packet);
			}
			catch (IOException ioe)
			{
				System.err.println(node.getNodeId()+" I/O ERROR in ServerSessionManager");
				ioe.printStackTrace();
				//gestisci eccezione
			}
		}
		catch(Exception e)
		{
			System.err.println(node +" CRITICAL ERROR in ServerSessionManager");
			e.printStackTrace();
		}
	}

	private void handleStoreRequest(RPCMessage received)
	{
		StoreRequest reqRPC = (StoreRequest)received.getRPC();
		StorageEntry[] entries = reqRPC.getValues();
		boolean signed = reqRPC.isSigned();
		boolean storeResult;

		if (signed)
		{
			storeResult = true;
			for (StorageEntry entry : entries)
			{
				if (! node.getStorage().store(entry))
					storeResult = false;
			}
		}
		else
		{
			storeResult = true;
			byte[][] contents;
			String[] types;
			long[] ttls;
			long ts;
			NodeId key;
			String owner;
			for (StorageEntry entry : entries)
			{
				MultiplePutContent mpc = MultiplePutContent.createFromBytes(entry.getContent().getValue());
				key = entry.getKey();
				owner = entry.getOwnerId();
				contents = mpc.getContent();
				types = mpc.getType();
				ttls = mpc.getTtl();
				ts = entry.getSubmissionTime();
				
				if (contents.length != types.length || contents.length != ttls.length || types.length != ttls.length)
					storeResult = false;
				else
				{
					for (int i=0; i< contents.length; i++)
					{
						StorageEntry se =node.getEntryFactory().buildUnsignedStorageEntry(key, contents[i], types[i], ts, ttls[i], owner);
						if (! node.getStorage().store(se))
							storeResult = false;
					}
				}
			}
		}
		
		RPCMessageFactory messageFactory = node.getMessageFactory();
		NodeId addressee = received.getAuthNodeId().getContent().getNodeId();
		byte[] nonce = receivedNonce.getNonce();

		RPCMessage response = messageFactory.createStoreResponse(addressee, sid, nonce, storeResult);
		try
		{
			byte[] payload = serialize(response);
			DatagramPacket packet = new DatagramPacket(payload,payload.length,client);
			//System.out.println(node + " - SEND RESPONSE: \n"+ response);
			//System.out.println("----------------------------------------------");
			//System.out.println(node.getUserId() + " sending Store RPC message response : sid " + sid);
			node.getMessageDispatcher().send(packet);
		}
		catch (IOException ioe)
		{
			System.err.println("ERROR in handleStoreRequest!");
			ioe.printStackTrace();//log
			//gestisci eccezione
		}
	}
	
	public String toString()
	{
		String result = "Server Session " + sid + " with " + client + "\n";
		result += "Rec  Nonce : " + receivedNonce + "\n";
		result += "Sent Nonce : " + sentNonce + "\n";
		result += "Rec  RPC   : " + receivedRPC + "\n";
		result += "Sent RPC   : " + sentRPC;
		return result;
	}
}
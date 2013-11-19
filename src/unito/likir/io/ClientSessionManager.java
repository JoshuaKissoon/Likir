package unito.likir.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.SocketException;

import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.exceptions.TaskException;
import unito.likir.messages.dht.DHTMessage;
import unito.likir.messages.dht.Nonce;
import unito.likir.messages.dht.RPCMessage;
import unito.likir.routing.Contact;
import unito.likir.routing.ContactImpl;
import unito.likir.security.MTRandom;
import unito.likir.settings.PropFinder;
import unito.likir.settings.Settings;
//import unito.likir.test.Logger;

/**
 * Manager class
 * @author Luca Maria Aiello
 * @version 0.1
 */
public abstract class ClientSessionManager extends ObservableFutureTask<RPCMessage> implements SessionManager
{
	private static MTRandom rand = new MTRandom();
	protected Node node;
	protected MessageDispatcher messageDispatcher;
	protected RPCMessage receivedRPC;
	protected RPCMessage sentRPC;
	protected Nonce sentNonce;
	protected Nonce receivedNonce;
	protected final int TIME_OUT;
	protected long sid; //session ID
	
	protected Contact addressee; 
	protected SocketAddress addresseeSocket;
	protected NodeId addresseeId;
	
	public ClientSessionManager(Node node, Contact addressee)
	{
		this.node = node;
		this.messageDispatcher = node.getMessageDispatcher();
		this.addressee = addressee;
		this.addresseeId = addressee.getNodeId();
		this.sid = rand.nextLong(); //random session ID
		this.TIME_OUT = Integer.parseInt(PropFinder.get(Settings.TIME_OUT));
	}
	
	public ClientSessionManager(Node node, NodeId addresseeId)
	{
		this.node = node;
		this.messageDispatcher = node.getMessageDispatcher();
		this.addressee = null;
		this.addresseeId = addresseeId;
		this.sid = rand.nextLong(); //random session ID
		this.TIME_OUT = Integer.parseInt(PropFinder.get(Settings.TIME_OUT));
	}
	
	/**
	 * 
	 * @param nonce
	 * @return
	 */
	public abstract RPCMessage buildMessage(NodeId addresseeId, long sid, byte[] nonce);
	
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
		//System.out.println(node.getUserId() + " - ClientManager : Handling \n" + received);
		DHTMessage.OpCode opcode = received.getMsgOpCode();
		
		if (opcode.isNonceResponse()) //message has a valid opcode
		{
			//System.out.println(node.getUserId() + " -  ClientManager : Handling Nonce response");
			synchronized(this)
			{
				receivedNonce = (Nonce)received; //set the received Nonce
				this.notifyAll(); //wakeup
			}
			//System.out.println(node.getUserId() + " -  ClientManager : HandlED Nonce response " + sid);
		}
		else if (opcode.isRPCMessageResponse() && receivedNonce != null) //message has a valid opcode
		{
			RPCMessage receivedMessage = (RPCMessage) received;
			//System.out.println(node.getUserId() + " -  ClientManager : Handling RPC response");
			//sender is not blacklisted
			if(!node.getBlacklist().contains(receivedMessage.getAuthNodeId().getContent().getUser()))
			{
				//security checks are passed
				if (node.getSecurityAgent().check(receivedMessage,receivedNonce,sentNonce))
				{
					synchronized(this)
					{
						receivedRPC = receivedMessage; //set the received RPC
						this.notifyAll(); //wakeup
					}
					//System.out.println(node.getUserId() + " -  ClientManager : HandlED RPC response " + sid);
					//refresh the route table entries
					refreshRouteTable(receivedRPC);
				}
				else
				{
					System.err.println(node.getUserId() + " - ClientManager : message check failed! \n " + received);
				}
			}
			else
			{
				////System.err.println(node.getUserId() + " - ClientManager : The received message was sent by a blacklisted node");
				node.getRouteTable().remove(addressee.getNodeId());
			}
		}
		else
		{
			////System.err.println(node.getUserId() + " - ClientManager: Invalid message for this session: \n" + received);
		}
	}
	
	public void run() throws TaskException
	{
		findAddresseeSocket();
		sendNonce();
		if (receiveNonce())
		{
			sendRPC();
			receiveRPC();
		}
	}
	
	//Retrieves the addressee IP from the route table
	private void findAddresseeSocket()
	{
		if (addressee != null)
		{
			addresseeSocket = addressee.getAddress();
		}
		else
		{
			addressee =  node.getRouteTable().getContact(addresseeId);
			if (addressee != null)
			{
				addresseeSocket = addressee.getAddress();
			}
			else
			{
				TaskException e = new TaskException("ClientManager : The node "+addresseeId+" is unknown by the route table");
				notifyFailure(e);
				throw e;
			}
		}
	}
	
	private void sendNonce()
	{
		byte[] payload = null; //payload of UDP packet
		DatagramPacket packet = null; //UDP packet
		
		//generates a new nonce
		sentNonce = new Nonce(node.getNodeId(),sid,true);
		
		//Builds the UDP packet
		try
		{
			payload = serialize(sentNonce);
			//System.out.println("ClientManager : PAYLOAD LENGTH = " + payload.length);
			packet = new DatagramPacket(payload,payload.length,addresseeSocket);
		}
		catch (SocketException se)
		{
			TaskException e = new TaskException("ClientManager : Error in UDP packet construction");
			notifyFailure(e);
			throw e;
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
			TaskException e = new TaskException("ClientManager : Error in Nonce serialization");
			notifyFailure(e);
			throw e;
		}
		
		//Sends the Nonce
		messageDispatcher.addSessionHandler(sid,this);
		try
		{ 
			//System.out.println(node + " - SEND NONCE: \n"+ sentNonce);
			//System.out.println("----------------------------------------------");
			//System.out.println(node.getUserId() + " sending Nonce request : sid " + sid + " - to: " + packet.getAddress() +","+ packet.getPort());
			//Logger.log(node.getUserId() + " sending Nonce request : sid " + sid + " - to: " + packet.getAddress() +","+ packet.getPort());
			
			messageDispatcher.send(packet);
		}
		catch (IOException ioe)
		{
			TaskException e = new TaskException("ClientManager : Error in sending UDP packet");
			notifyFailure(e);
			messageDispatcher.removeSessionHandler(sid);//unregister from the message dispatcher
			throw e;
		}
	}
	
	private boolean receiveNonce()
	{
		try
		{
			synchronized(this)
			{
				if (receivedNonce == null)
				{
					//System.out.println(node.getUserId() + " - ClientManager :  waiting for NONCE in session " + sid);
					this.wait(TIME_OUT);
				}
			}
			if (receivedNonce == null)
			{
				////System.err.println(node.getUserId() + " - ClientManager : incoming nonce time out! to " + addresseeSocket);
				
				messageDispatcher.removeSessionHandler(sid);//unregister from the message dispatcher
				node.getRouteTable().handleFailure(addressee.getNodeId());
				notifyFailure(new TaskException("Time out!"));
				return false;
			}
			else
			{
				//System.out.println(node.getUserId() + " -  ClientManager : Received nonce in session " + sid);
				return true;
			}
		}
		catch(InterruptedException ie)
		{
			////ie.printStackTrace();
			messageDispatcher.removeSessionHandler(sid);//unregister from the message dispatcher
			return false;
		}
	}
	
	private void sendRPC()
	{
		//builds the RPC
		RPCMessage msg = buildMessage(addresseeId, sid, receivedNonce.getNonce());
		
		//Builds the UDP packet
		byte[] payload = null; //payload of UDP packet
		DatagramPacket packet = null; //UDP packet
		try
		{
			payload = serialize(msg);
			packet = new DatagramPacket(payload,payload.length,addresseeSocket);
		}
		catch (SocketException se)
		{
			TaskException e = new TaskException("ClientManager : Error in UDP packet constuction");
			notifyFailure(e);
			messageDispatcher.removeSessionHandler(sid);
			throw e;
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
			TaskException e = new TaskException("ClientManager : Error in RPC serialization");
			notifyFailure(e);
			messageDispatcher.removeSessionHandler(sid);
			throw e;
		}
		
		//Sends the RPC
		try
		{
			//System.out.println(node + " - SEND REQUEST: \n"+ msg);
			//System.out.println("----------------------------------------------");
			sentRPC = msg;
			//System.out.println(node.getUserId() + " sending RPC request : sid " + sid + " - to: " + packet.getAddress() +","+ packet.getPort());
			
			messageDispatcher.send(packet);
		}
		catch (IOException ioe)
		{
			TaskException e = new TaskException( node.getUserId() + " - ClientManager : Error in sending UDP packet to " + msg.getAuthenticator().getContent().getId() + ", " + packet.getPort());
			notifyFailure(e);
			messageDispatcher.removeSessionHandler(sid);//unregister from the message dispatcher
			throw e;
		}
	}
	
	private void receiveRPC()
	{
		try
		{
			synchronized(this)
			{
				if (receivedRPC == null)
				{
					//System.out.println(node.getUserId() + " - ClientManager :  waiting for RPC in session " + sid);
					this.wait(TIME_OUT);
				}
			}
			if (receivedRPC == null)
			{
				String errorMsg = "";
				errorMsg += node.getUserId() + " - ClientManager : incoming RPC time out! to " + sentRPC.getAuthenticator().getContent().getId() + ", " + addresseeSocket +"\n";
				errorMsg += node.getUserId() + " -     Addressee : " + addresseeSocket + "\n";
				errorMsg += node.getUserId() + " -     " + sentRPC;
				////System.err.println(errorMsg);
				
				messageDispatcher.removeSessionHandler(sid);//unregister from the message dispatcher
				node.getRouteTable().handleFailure(addressee.getNodeId());
				notifyFailure(new TaskException("Time out!"));
			}
			else
			{
				messageDispatcher.removeSessionHandler(sid);//unregister from the message dispatcher
				//sets the result of the ObservableFuture
				set(receivedRPC);
			}
		}
		catch(InterruptedException ie)
		{
			messageDispatcher.removeSessionHandler(sid);//unregister from the message dispatcher
			ie.printStackTrace();
		}	
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
	
	private void refreshRouteTable(RPCMessage message)
	{
		NodeId senderNodeId = message.getAuthNodeId().getContent().getNodeId();
		Contact contact = new ContactImpl(senderNodeId, addresseeSocket);
		node.getRouteTable().add(contact);
	}
	
	public String toString()
	{
		String result = "Client Session " + sid + " with " + addresseeId + " , " + addresseeSocket + "\n";
		result += "Rec  Nonce : " + receivedNonce + "\n";
		result += "Sent Nonce : " + sentNonce + "\n";
		result += "Rec  RPC   : " + receivedRPC + "\n";
		result += "Sent RPC   : " + sentRPC;
		return result;
	}
}
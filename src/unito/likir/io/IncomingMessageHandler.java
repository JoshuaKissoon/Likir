package unito.likir.io;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.io.ByteArrayInputStream;
import java.lang.ClassNotFoundException;

import unito.likir.Node;
import unito.likir.messages.dht.DHTMessage;
import unito.likir.messages.dht.Nonce;
//import unito.likir.test.Logger;

/**
 * IncomingMessageHandler class
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class IncomingMessageHandler implements Runnable
{
	Node node;
	MessageDispatcher messageDispatcher;
	DatagramPacket packet;
	SocketAddress senderAddress;

	public IncomingMessageHandler(Node node, DatagramPacket packet)
	{
		this.node = node;
		this.messageDispatcher = node.getMessageDispatcher();
		this.packet = packet;
		this.senderAddress = packet.getSocketAddress();
	}

	public void run()
	{
		//deserialize data and check the validity of message
		DHTMessage received = null;
		try
		{
			received = deserialize(packet.getData());
		}
		catch(IOException ioe)
		{
			System.err.println("Error in data deserialization");
			return;
		}
		catch(ClassNotFoundException cnfe)
		{
			System.err.println("Error in data deserialization");
			return;
		}
		
		long sid = received.getSid();
		DHTMessage.OpCode opcode = received.getMsgOpCode();
		
		SessionManager m = messageDispatcher.getSessionHandler(sid);
		//boolean managerOK = m != null;
		//System.out.println(node.getUserId() + " incoming message : sid " + sid + " - Opcode : " +  opcode + " - from: " + packet.getAddress() +","+ packet.getPort() + " - manager:" + managerOK);
		
		if (m != null)
		{
			m.handle(received);
		}
		else
		{
			if (opcode.isNonceRequest())
			{
				//System.out.println(node.getUserId() + " New session! " + received.getSid());
				ServerSessionManager manager = new ServerSessionManager(node,packet.getSocketAddress(),(Nonce)received);
				node.getExecutor().submit(manager);
			}
			else
			{
				////System.err.println(node.getUserId() + " : Received message is invalid: no session for: "+
				////		"\n     " + received);
			}
		}
		
		/*if (m != null)
		{
			try
			{
				if (opcode.isNonce())
				{
					synchronized(m)//acquires lock on rh to notify the handler waiting on it
					{
						m.setReceivedNonce((Nonce)received);
						m.notifyAll();
					}
				}
				else if (opcode.isRPCMessage())
				{
					synchronized(m)//acquires lock on rh to notify the handler waiting on it
					{
						m.setReceivedRPC((RPCMessage)received);
						m.notifyAll();
					}
				}
				else
				{
					System.err.println(node.getUserId() + " incoming message : sid " + sid + " - Received message is corrupted: invalid message opcode");
					//Logger.log(node.getUserId() + " incoming message : sid " + sid + "Received message is corrupted: invalid message opcode1");
					return;
				}
			}
			catch(ClassCastException cce)
			{
				System.err.println(node.getUserId() + " incoming message : sid " + sid + " - Received message is corrupted: invalid message opcode");
				//Logger.log(node.getUserId() + " incoming message : sid " + sid + "Received message is corrupted: invalid message opcode2");
				return;
			}
		}
		else
		{
			try
			{
				if (opcode.isNonce())
				{
					Nonce n = (Nonce)received;
					if (opcode.isRequest())
					{
						ServerSessionManager manager = new ServerSessionManager(node,packet.getSocketAddress(),n);
						node.getExecutor().submit(manager); //TODO: mettere .execute??
					}
					else
					{
						System.err.println("Received message is corrupted: invalid nonce type");
						//Logger.log(node.getUserId() + " incoming message : sid " + sid + "Received message is corrupted: invalid nonce type");
						return;
					}
				}
				else if (opcode.isRPCMessage())
				{
					System.err.println(node.getUserId() + " : Received message is invalid: no session for RPC"+
							"\n     "+(RPCMessage)received);
					//Logger.log(node.getUserId() + " : Received message is invalid: no session for RPC"+"\n     "+(RPCMessage)received);
				}
				else
				{
					System.err.println(node.getUserId() + " incoming message : sid " + sid + "Received message is corrupted: invalid message opcode");
					//Logger.log(node.getUserId() + " incoming message : sid " + sid + "Received message is corrupted: invalid message opcode3");
					return;
				}
			}
			catch(ClassCastException cce)
			{
				System.err.println(node.getUserId() + " incoming message : sid " + sid + "Received message is corrupted: invalid message opcode");
				//Logger.log(node.getUserId() + " incoming message : sid " + sid + "Received message is corrupted: invalid message opcode4");
				return;
			}
		}*/
	}

	private DHTMessage deserialize(byte[] rawData) throws IOException, ClassNotFoundException
	{
		DHTMessage message = null;
		ByteArrayInputStream bais = new ByteArrayInputStream(rawData);
		ObjectInputStream ois = new ObjectInputStream(bais);
		message = (DHTMessage)(ois.readObject());
		bais.close();
		ois.close();
		if (message == null) throw new IOException("IncomingMessageHandler - Message deserialization failed");

		return message;
	}
}
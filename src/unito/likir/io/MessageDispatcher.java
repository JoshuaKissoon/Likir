package unito.likir.io;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import unito.likir.Node;

/**
 * MessageDispatcher class
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class MessageDispatcher implements Runnable
{
	Node node;
	InetSocketAddress address;
	DatagramSocket socket;
	Map<Long,SessionManager> sessionHandlerPool;
	
	public MessageDispatcher(Node node, InetSocketAddress address) throws SocketException
	{
		this.address = address;
		this.node = node;
		Map<Long,SessionManager> map = new HashMap<Long,SessionManager>();
		this.sessionHandlerPool = Collections.synchronizedMap(map);
		this.socket = new DatagramSocket(this.address);
	}
	
	public InetSocketAddress getLocalAddress()
	{
		return address;
	}
	
	//TODO: METODO TEST!!! ELIMINA!
	public synchronized Map<Long,SessionManager> getSessionHandlerPool()
	{
		return sessionHandlerPool;
	}
	
	//TODO: METODO TEST!!! ELIMINA!
	public synchronized String printResponseHandlerPool()
	{
		String result = node.getUserId() + " - Handler Tree\n";
		Collection<Long> keys = sessionHandlerPool.keySet();
		for (Long key : keys)
			result += " * " + key + " : \n" + sessionHandlerPool.get(key) + "\n";
		return result;
	}
	
	public synchronized void addSessionHandler(long key, SessionManager r)
	{
		sessionHandlerPool.put(key, r);
	}
	
	public synchronized SessionManager getSessionHandler(long key)
	{
		return sessionHandlerPool.get(key);
	}
	
	public synchronized SessionManager removeSessionHandler(long key)
	{
		return sessionHandlerPool.remove(key);
	}
	
	public synchronized void send(DatagramPacket packet) throws IOException
	{
		socket.send(packet);
	}
	
	public void close()
	{
		socket.close();
	}
	
	public void run()
	{
		if (socket == null)
		{
			System.err.println("MessageDispatcher: socket is null!");
			System.exit(0);
		}	
		try
		{
			while (true)
			{
				//initialize the object to receive the next incoming UDP datagram
				byte[] buf = new byte[64000];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				//wait for a message to come
				//synchronized(socket)
				//{
					socket.receive(packet);
					//System.out.println(node.getUserId() +" received a packet!");
				//}
				//run a handler for the UDP datagram 
				IncomingMessageHandler handler = new IncomingMessageHandler(node, packet);
				node.getExecutor().submit(handler); //TODO: potrebbe essere .execute(handler)??
			}
		}
		catch (IOException e)
		{
			System.out.println("MessageDispatcher interrupted!");
		}
    }
	
	public String toString()
	{
		String result = node.getUserId() + " - Handler Tree\n";
		Collection<Long> keys = sessionHandlerPool.keySet();
		for (Long key : keys)
			result += " * " + key + " : \n" + sessionHandlerPool.get(key) + "\n";
		return result;
	}
}
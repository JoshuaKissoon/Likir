package unito.likir.io;

import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.messages.dht.RPCMessage;
import unito.likir.routing.Contact;

/**
 * PingManager class
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class PingManager extends ClientSessionManager
{
	NodeId addresseeId;
	Contact addressee;
	
	public PingManager(Node node, NodeId addresseeId)
	{
		super(node, addresseeId);
	}
	
	public PingManager(Node node, Contact addressee)
	{
		super(node, addressee);
	}
	
	public RPCMessage buildMessage(NodeId addresseeId, long sid, byte[] nonce)
	{
		if (addressee != null)
		{
			addresseeId = addressee.getNodeId();
		}
		RPCMessage ping = node.getMessageFactory().createPingRequest(addresseeId, sid, nonce);
		return ping;
	}
}
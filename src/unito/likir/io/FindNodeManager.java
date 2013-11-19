package unito.likir.io;

import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.messages.dht.RPCMessage;
import unito.likir.routing.Contact;

/**
 * FindNodeManager class
 * @author Aiello Luca Maria
 * @version 0.1
 */

public class FindNodeManager extends ClientSessionManager
{
	private NodeId key;
	
	public FindNodeManager(Node node, Contact addressee, NodeId key)
	{
		super(node, addressee);
		this.key = key;
	}
	
	public FindNodeManager(Node node, NodeId addresseeId, NodeId key)
	{
		super(node, addresseeId);
		this.key = key;
	}
	
	public RPCMessage buildMessage(NodeId addresseeId, long sid, byte[] nonce)
	{
		if (addressee != null)
		{
			addresseeId = addressee.getNodeId();
		}
		RPCMessage findNode = super.node.getMessageFactory().createFindNodeRequest(addresseeId, sid, nonce, key);
		return findNode;
	}
}
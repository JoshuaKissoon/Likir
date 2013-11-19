package unito.likir.io;

import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.messages.dht.RPCMessage;
import unito.likir.routing.Contact;

/**
 * FindValueManager class
 * @author Aiello Luca Maria
 * @version 0.1
 */

public class FindValueManager extends ClientSessionManager
{
	private NodeId key;
	private String type;
	private String owner;
	private boolean recent;
	private boolean countersOnly;
	
	public FindValueManager(Node node, Contact addressee, NodeId key, String type, String owner, boolean recent)
	{
		super(node, addressee);
		this.key = key;
		this.type = type;
		this.owner = owner;
		this.recent = recent;
		this.countersOnly = false;
	}

	public FindValueManager(Node node, NodeId addresseeId, NodeId key, String type, String owner, boolean recent)
	{
		super(node, addresseeId);
		this.key = key;
		this.type = type;
		this.owner = owner;
		this.recent = recent;
		this.countersOnly = false;
	}
	
	public FindValueManager(Node node, Contact addressee, NodeId key, String type, String owner, boolean recent, boolean countersOnly)
	{
		super(node, addressee);
		this.key = key;
		this.type = type;
		this.owner = owner;
		this.recent = recent;
		this.countersOnly = countersOnly;
	}

	public FindValueManager(Node node, NodeId addresseeId, NodeId key, String type, String owner, boolean recent, boolean countersOnly)
	{
		super(node, addresseeId);
		this.key = key;
		this.type = type;
		this.owner = owner;
		this.recent = recent;
		this.countersOnly = countersOnly;
	}
	
	public RPCMessage buildMessage(NodeId addresseeId, long sid, byte[] nonce)
	{
		if (addressee != null)
		{
			addresseeId = addressee.getNodeId();
		}
		RPCMessage findValue = super.node.getMessageFactory().createFindValueRequest(addresseeId, sid, nonce, key, type, owner, recent, countersOnly);
		return findValue;
	}
}
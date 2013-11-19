package unito.likir.io;

import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.messages.dht.RPCMessage;
import unito.likir.routing.Contact;
import unito.likir.storage.StorageEntry;

/**
 * StoreManager class
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class StoreManager extends ClientSessionManager
{
	private NodeId key;
	private byte[] content;
	private String type;
	long ttl;
	boolean sign = true;
	
	StorageEntry[] entries;
	
	public StoreManager(Node node, NodeId addresseeId, NodeId key, byte[] content, String type, long ttl)
	{
		super(node, addresseeId);
		this.key = key;
		this.content = content;
		this.type = type;
		this.ttl = ttl;
		this.entries = null;
	}
	
	public StoreManager(Node node, Contact addressee, NodeId key, byte[] content, String type, long ttl)
	{
		super(node, addressee);
		this.key = key;
		this.content = content;
		this.type = type;
		this.ttl = ttl;
		this.entries = null;
	}
	
	public StoreManager(Node node, Contact addressee, StorageEntry entry)
	{
		super(node, addressee);
		this.key = null;
		this.content = null;
		this.ttl = 0;
		this.entries = new StorageEntry[1];
		this.entries[0] = entry;
	}
	
	public StoreManager(Node node, Contact addressee, StorageEntry entry, boolean sign)
	{
		super(node, addressee);
		this.key = null;
		this.content = null;
		this.ttl = 0;
		this.entries = new StorageEntry[1];
		this.entries[0] = entry;
		this.sign = sign;
	}
	
	public StoreManager(Node node, Contact addressee, StorageEntry[] entries)
	{
		super(node, addressee);
		this.key = null;
		this.content = null;
		this.ttl = 0;
		this.entries = entries;
	}
	
	public RPCMessage buildMessage(NodeId addresseeId, long sid, byte[] nonce)
	{
		if (entries == null)
		{
			entries = new StorageEntry[1];
			entries[0] = node.getEntryFactory().buildStorageEntry(key, content, type, ttl);
		}
		if (addressee != null)
		{
			addresseeId = addressee.getNodeId();
		}
		RPCMessage store = super.node.getMessageFactory().createStoreRequest(addresseeId, sid, nonce, entries, sign);
		return store;
	}
}

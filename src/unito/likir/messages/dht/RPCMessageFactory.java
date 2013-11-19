package unito.likir.messages.dht;

import java.security.PublicKey;
import java.util.Collection;
import java.util.HashMap;

import unito.likir.NodeId;
import unito.likir.messages.ca.CAKeyRequest;
import unito.likir.messages.ca.InitRequest;
import unito.likir.routing.Contact;
import unito.likir.storage.StorageEntry;

public interface RPCMessageFactory
{
	public void setStartTime(long millis);
	
    /**
     * Creates and returns a PingRequest Message
     * 
     * @param src The contact information of the issuing Node
     * @param dst The destination address to where the request will be send
     */
	public RPCMessage createPingRequest(NodeId addressee, long sid, byte[] nonce);

	public RPCMessage createPingResponse(NodeId addressee, long sid, byte[] nonce);

	public RPCMessage createFindNodeRequest(NodeId addressee, long sid, byte[] nonce, NodeId key);

	public RPCMessage createFindNodeResponse(NodeId addressee, long sid, byte[] nonce, Collection<Contact> nodes);

	public RPCMessage createFindValueRequest(NodeId addressee, long sid, byte[] nonce, NodeId key, String type, String owner, boolean recent);
    
	public RPCMessage createFindValueRequest(NodeId addressee, long sid, byte[] nonce, NodeId key, String type, String owner, boolean recent, boolean countersOnly);
	
	public RPCMessage createFindValueResponse(NodeId addressee, long sid, byte[] nonce, Collection<Contact> nodes, Collection<StorageEntry> values, HashMap<String, Integer> counters);

	public RPCMessage createStoreRequest(NodeId addressee, long sid, byte[] nonce, StorageEntry[] values, boolean sign);

	public RPCMessage createStoreResponse(NodeId addressee, long sid, byte[] nonce, boolean storeResult);
    
    public InitRequest createInitRequest(PublicKey publicKey, NodeId nodeId);
    
    public CAKeyRequest createCAKeyRequest(String userId);
}
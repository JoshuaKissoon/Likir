package unito.likir.messages.dht;

import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.messages.ca.*;
import unito.likir.routing.Contact;
import unito.likir.security.AuthNodeId;
import unito.likir.security.Authenticator;
import unito.likir.security.MTRandom;
import unito.likir.storage.StorageEntry;

public class RPCMessageFactoryImpl implements RPCMessageFactory
{
	private Node node;
	private Random randomGenerator;
	private long startTime; // time stamp of node startup
	
	public RPCMessageFactoryImpl(Node node)
	{
		this.node = node;
		this.randomGenerator = new MTRandom();
		this.startTime = 0;
	}
	
	public void setStartTime(long millis)
	{
		startTime = millis;
	}
	
	public long createMessageId()
	{
		return randomGenerator.nextLong();
	}
	
    public RPCMessage createPingRequest(NodeId addressee, long sid, byte[] nonce)
    {
    	return createRPCMessage(addressee, nonce, new PingRequestImpl(sid));
    }

    public RPCMessage createPingResponse(NodeId addressee, long sid, byte[] nonce)
    {
    	return createRPCMessage(addressee, nonce, new PingResponseImpl(sid, System.currentTimeMillis()-startTime));
    }

    public RPCMessage createFindNodeRequest(NodeId addressee, long sid, byte[] nonce, NodeId key)
    {
    	return createRPCMessage(addressee, nonce, new FindNodeRequestImpl(sid,key));
    }

    public RPCMessage createFindNodeResponse(NodeId addressee, long sid, byte[] nonce, Collection<Contact> nodes)
    {
    	return createRPCMessage(addressee, nonce, new FindNodeResponseImpl(sid, nodes));
    }

    public RPCMessage createFindValueRequest(NodeId addressee, long sid, byte[] nonce, NodeId key, String type, String owner, boolean recent)
    {
    	if ("".equals(type))
    		type = null;
    	return createRPCMessage(addressee, nonce, new FindValueRequestImpl(sid, key, type, owner, recent, false));
    }
    
    public RPCMessage createFindValueRequest(NodeId addressee, long sid, byte[] nonce, NodeId key, String type, String owner, boolean recent, boolean countersOnly)
    {
    	if ("".equals(type))
    		type = null;
    	return createRPCMessage(addressee, nonce, new FindValueRequestImpl(sid, key, type, owner, recent, countersOnly));
    }
    
    public RPCMessage createFindValueResponse(NodeId addressee, long sid, byte[] nonce, Collection<Contact> nodes, Collection<StorageEntry> values, HashMap<String,Integer> counters)
    {
    	return createRPCMessage(addressee, nonce, new FindValueResponseImpl(sid, nodes, values, counters));
    }

    public RPCMessage createStoreRequest(NodeId addressee, long sid, byte[] nonce, StorageEntry[] value, boolean sign)
    {
    	return createRPCMessage(addressee, nonce, new StoreRequestImpl(sid,value,sign));
    }

    public RPCMessage createStoreResponse(NodeId addressee, long sid, byte[] nonce, boolean storeResult)
    {
    	return createRPCMessage(addressee, nonce, new StoreResponseImpl(sid, storeResult));
    }

    private RPCMessage createRPCMessage(NodeId addressee, byte[] nonce, RPC rpc)
    {
    	//TODO: rivedi eccezione
    	AuthNodeId myId = node.getSecurityAgent().getAuthNodeId();
    	try
    	{
    		Authenticator auth = node.getSecurityAgent().buildAuthenticator(addressee, nonce, rpc);
    		return new RPCMessageImpl(myId, auth, rpc);
    	}
    	catch (SignatureException se)
    	{
    		se.printStackTrace();
    	}
    	return null;
    }
    
    public InitRequest createInitRequest(PublicKey publicKey, NodeId nodeId)
    {
    	return new InitRequestImpl(node.getUserId(), createMessageId(), node.getMessageDispatcher().getLocalAddress(), publicKey, nodeId);
    }
    
    public CAKeyRequest createCAKeyRequest(String userId)
    {
    	return new CAKeyRequestImpl(node.getUserId(), createMessageId());
    }
}
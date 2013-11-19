package unito.likir.messages.dht;

import unito.likir.security.AuthNodeId;
import unito.likir.security.Authenticator;

public interface RPCMessage extends DHTMessage
{
	public AuthNodeId getAuthNodeId();
	
	public Authenticator getAuthenticator();
	
	public RPC getRPC();
	
	public RPC.OpCode getRPCOpcode();
}

package unito.likir.io;

import unito.likir.messages.dht.DHTMessage;
import unito.likir.messages.dht.Nonce;
import unito.likir.messages.dht.RPCMessage;

public interface SessionManager
{
	public Nonce getSentNonce();
	
	public Nonce getReceivedNonce();
	
	public RPCMessage getSentRPC();
	
	public RPCMessage getReceivedRPC();
	
	
	public void setSentNonce(Nonce sentNonce);

	public void setReceivedNonce(Nonce receivedNonce);

	public void setSentRPC(RPCMessage sentRPC); 
	
	public void setReceivedRPC(RPCMessage receivedRPC);

	
	public void handle(DHTMessage received);
}

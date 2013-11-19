package unito.likir.messages.dht;

import unito.likir.security.AuthNodeId;
import unito.likir.security.Authenticator;

public class RPCMessageImpl implements RPCMessage
{
	private static final long serialVersionUID = -1221871915443581640L;
	
	private AuthNodeId authNodeId;
	private Authenticator authenticator;
	private RPC rpc;
	private OpCode opcode;
	
	public RPCMessageImpl(AuthNodeId authNodeId, Authenticator authenticator, RPC rpc)
	{
		if (authNodeId == null)
			throw new NullPointerException("The authNodeId is null!");
		if (authenticator == null)
			throw new NullPointerException("The authenticator is null!");
		if (rpc == null)
			throw new NullPointerException("The rpc is null!");
		
		this.authNodeId = authNodeId;
		this.authenticator = authenticator;
		this.rpc = rpc;
		
		RPC.OpCode rpcOpcode = rpc.getRPCOpCode();
		if (rpcOpcode.isRequest())
			this.opcode = DHTMessage.OpCode.RPC_REQ;
		else
			this.opcode = DHTMessage.OpCode.RPC_RES;
	}
	
	public AuthNodeId getAuthNodeId()
	{
		return authNodeId;
	}
	
	public Authenticator getAuthenticator()
	{
		return authenticator;
	}
	
	public RPC getRPC()
	{
		return rpc;
	}
	
	public long getSid()
	{
		return rpc.getMessageId();
	}
	
	public OpCode getMsgOpCode()
	{
		return opcode;
	}
	
	public RPC.OpCode getRPCOpcode()
	{
		return rpc.getRPCOpCode();
	}
	
	public String toString()
	{
		StringBuilder buffer = new StringBuilder();
        buffer.append("Message: \n");
        buffer.append("  Sender: ").append(authNodeId).append("\n");
        buffer.append("  Receiver: ").append(authenticator).append("\n");
        buffer.append("  RPC: ").append(rpc);
        return buffer.toString();
	}


}

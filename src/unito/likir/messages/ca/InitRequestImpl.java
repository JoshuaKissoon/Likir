package unito.likir.messages.ca;

import java.net.SocketAddress;
import java.security.PublicKey;

import unito.likir.NodeId;

public class InitRequestImpl extends AbstractCAMessage implements InitRequest
{
	private static final long serialVersionUID = 7824225873038719844L;
	
	private PublicKey publicKey;
	private SocketAddress udpAddress;
	private NodeId nodeId;
	
	public InitRequestImpl(String userId, long messageId, SocketAddress udpAddress, PublicKey publicKey, NodeId nodeId)
	{
		super(userId, OpCode.INITIALIZATION_REQUEST, messageId);
		this.udpAddress = udpAddress;
		this.publicKey = publicKey;
		this.nodeId = nodeId;
	}
	
	public PublicKey getPublicKey()
	{
		return publicKey;
	}
	
	public SocketAddress getUDPAddress()
	{
		return udpAddress;
	}
	
	public NodeId getNodeId()
	{
		return nodeId;
	}
}

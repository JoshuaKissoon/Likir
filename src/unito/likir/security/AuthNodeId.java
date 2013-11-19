package unito.likir.security;

import java.io.Serializable;
import java.security.PublicKey;

import unito.likir.NodeId;

/**
 * The authenticated Likir NodeId. The Likir Certification Service produces AuthNodeId
 * following to an appropriate request. The AuthNodeId is a crypto token that binds the
 * identity of the human user (the userId) to a NodeId and to the Node's public key.
 * Every Likir RPCMessage must include the sender's AuthNodeId 
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class AuthNodeId implements Serializable
{
	private static final long serialVersionUID = 8820799624091876909L;
	
	private final AuthNodeIdContent content;
	private byte[] signature;
	
	/**
	 * Create a new AuthNodeId
	 * @param nodeId the NodeId
	 * @param key the node's public key
	 * @param userId the human user id
	 * @param expireTime the AuthNodeId expire time
	 */
	public AuthNodeId(NodeId nodeId, PublicKey key, String userId, long expireTime)
	{
		this.content = new AuthNodeIdContent(nodeId, key, userId, expireTime);
		this.signature = null;
	}
	
	/**
	 * Return the content of this AuthNodeId (data without signature)
	 * @return the content
	 */
	public AuthNodeIdContent getContent()
	{
		return content;
	}
	
	/**
	 * Return the signature of this AuthNodeId 
	 * @return the signature
	 */
	public byte[] getSignature()
	{
		return signature;
	}
	
	/**
	 * Set the Signature
	 * @param signature
	 */
	public void setSignature(byte[] signature)
	{
		this.signature = signature;
	}
	
	/**
	 * Return whether or not this AuthNodeId is signed
	 * @return
	 */
	public boolean isSigned()
	{
		return signature != null;
	}
	
	public String toString()
	{
		return "nodeId= "+content.getNodeId()+"- userId="+content.getUser();
	}
}
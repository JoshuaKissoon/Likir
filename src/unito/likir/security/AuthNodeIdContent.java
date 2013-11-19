package unito.likir.security;

import java.io.Serializable;
import java.security.PublicKey;

import unito.likir.NodeId;

/**
 * The content of an AuthNodeId
 * @author Luca Maria Aiello
 * @version 0.1
 */
public class AuthNodeIdContent implements Serializable
{
	private static final long serialVersionUID = -8109593472461129626L;
	
	private final NodeId nodeId;
	private final PublicKey key;
	private final String userId;
	private final long expireTime;
	
	/**
	 * Create a new AuthNodeIdcontent
	 * @param nodeId the NodeId
	 * @param key the node's public key
	 * @param userId the human user id
	 * @param expireTime the AuthNodeId expire time
	 */
	public AuthNodeIdContent(NodeId nodeId, PublicKey key, String userId, long expireTime)
	{
		this.nodeId = nodeId;
		this.key = key;
		this.userId = userId;
		this.expireTime = expireTime;
	}
	
	/**
	 * Return the addressee NodeId
	 * @return the addressee NodeId
	 */
	public NodeId getNodeId()
	{
		return nodeId;
	}
	
	/**
	 * Return the PublicKey
	 * @return the public key
	 */
	public PublicKey getKey()
	{
		return key;
	}
	
	/**
	 * Return the username
	 * @return the username
	 */
	public String getUser()
	{
		return userId;
	}
	
	/**
	 * Return the expire time
	 * @return the expire time
	 */
	public long getExpireTime()
	{
		return expireTime;
	}
}
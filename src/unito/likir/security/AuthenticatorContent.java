package unito.likir.security;

import java.io.Serializable;

import unito.likir.NodeId;

/**
 * The content of an Authenticator
 * @author Luca Maria Aiello
 * @version 0.1
 */
public class AuthenticatorContent implements Serializable 
{
	private static final long serialVersionUID = -434608647226936811L;
	
	private NodeId id;
	private byte[] nonce;
	private byte[] rpcHash;

	/**
	 * Create a new AuthenticatorContent
	 * @param id the addressee NodeId
	 * @param nonce the nonce received previously during the Node session
	 * @param rpcHash the SHA-1 hash of the RPCMessage to be sent with this Authenticator
	 */
	public AuthenticatorContent(NodeId id, byte[] nonce, byte[] rpcHash)
	{
		this.id = id;
		this.nonce = nonce;
		this.rpcHash = rpcHash;
	}

	/**
	 * Return the addressee NodeId
	 * @return the addressee NodeId
	 */
	public NodeId getId()
	{
		return id;
	}

	/**
	 * Set the addressee NodeId
	 * @param the addressee NodeId
	 */
	public void setId(NodeId id)
	{
		this.id = id;
	}

	/**
	 * Return the nonce
	 * @return the nonce
	 */
	public byte[] getNonce()
	{
		return nonce;
	}

	/**
	 * Set the nonce
	 * @param nonce
	 */
	public void setNonce(byte[] nonce)
	{
		this.nonce = nonce;
	}

	/**
	 * Return the RPCMessage hash
	 * @return the SHA-1 hash
	 */
	public byte[] getRpcHash()
	{
		return rpcHash;
	}

	/**
	 * Set the RPCMessage hash
	 * @param rpcHash the SHA-1 hash
	 */
	public void setRpcHash(byte[] rpcHash)
	{
		this.rpcHash = rpcHash;
	}
	
}

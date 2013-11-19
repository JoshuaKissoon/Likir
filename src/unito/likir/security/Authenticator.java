package unito.likir.security;

import java.io.Serializable;

import unito.likir.NodeId;
import unito.likir.util.ArrayUtils;

/**
 * The Authenticator is a crypto token that Nodes must send together with an RPC message
 * during a session, according to the Likir protocol.
 * It contains the addressee NodeId, the nonce previously received during the same session
 * and the SHA-1 hash of the RPCMessage; this data must be signed with the local Node private key.
 * To create appropriate Authenticator for a specific node, use the NodeSecurityAgent class.
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class Authenticator implements Serializable
{
	private static final long serialVersionUID = -94519055234062832L;
	
	private AuthenticatorContent content;
	private byte[] signature;
	
	/**
	 * Create a new Authenticator instance, with a null signature
	 * @param id the addressee NodeId
	 * @param nonce the nonce received previously during the Node session
	 * @param rpcHash the SHA-1 hash of the RPCMessage to be sent with this Authenticator
	 */
	public Authenticator(NodeId id, byte[] nonce, byte[] rpcHash)
	{
		this.content = new AuthenticatorContent(id, nonce, rpcHash);
		this.signature = null;
	}
	
	/**
	 * Return the content (data without signature) of this Authenticator
	 * @return the Authenticator content
	 */
	public AuthenticatorContent getContent()
	{
		return content;
	}
	
	/**
	 * Return the signature of this Authenticator
	 * @return the Authenticator signature
	 */
	public byte[] getSignature()
	{
		return signature;
	}
	
	/**
	 * Set the signature of this Authenticator
	 * @param signature the signature
	 */
	public void setSignature(byte[] signature)
	{
		this.signature = signature;
	}
	
	/**
	 * Return whether or not this Authenticator is signed
	 * @return whether or not this Authenticator is signed
	 */
	public boolean isSigned()
	{
		return signature != null;
	}
	
	/**
	 * Return a String representation of this Authenticator
	 * @return the String representation
	 */
	public String toString()
	{
		return "Authenticator: addressee="+content.getId()+ 
		" - nonce = "+ArrayUtils.toHexString(content.getNonce())+
		" - H = "+ArrayUtils.toHexString(content.getRpcHash());
	}
}
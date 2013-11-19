package unito.likir.security;

import java.io.Serializable;
import java.security.PublicKey;

/**
 * CredentialsImpl class
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class Credentials implements Serializable
{
	private static final long serialVersionUID = -848423796474991531L;
	
	private CredentialsContent content;
	private byte[] signature;
	
	public Credentials(String ownerId, PublicKey key, long timeStamp, long ttl, byte[] objectHash)
	{
		this.content = new CredentialsContent(ownerId, key, timeStamp, ttl, objectHash);
		this.signature = null;
	}
	
	public Credentials(String ownerId,PublicKey key,  byte[] objectHash)
	{
		this.content = new CredentialsContent(ownerId, key, objectHash);
		this.signature = null;
	}
	
    /**
     * Makes a deep copy of this Credentials
     */
	/*public Object clone() throws CloneNotSupportedException
	{
		Credentials cloned = (Credentials) super.clone();
		byte[] clonedSign = signature.clone();
		CredentialsContent clonedCont = (CredentialsContent) content.clone();	
		cloned.setSignature(clonedSign);
		cloned.setContent(clonedCont);
		return cloned;
	}*/
		
	public CredentialsContent getContent()
	{
		return content;
	}
	
	public String getOwnerId()
	{
		return getContent().getOwnerId();
	}
	
	public PublicKey getPublicKey()
	{
		return getContent().getPublicKey();
	}

	public long getTimeStamp()
	{
		return getContent().getTimeStamp();
	}

	public long getTTL()
	{
		return getContent().getTTL();
	}

	public byte[] getObjectHash()
	{
		return getContent().getObjectHash();
	}
	
	public byte[] getSignature()
	{
		return signature;
	}
	
	public void setSignature(byte[] signature)
	{
		this.signature = signature;
	}
	
	public void setContent(CredentialsContent content)
	{
		this.content = content;
	}
	
	public boolean isSigned()
	{
		return signature != null;
	}
	
	public boolean isExpired()
	{
		return getContent().getTimeStamp() + getContent().getTTL() < System.currentTimeMillis();
	}
	
	public String toString()
	{
		return "Credential: owner="+getContent().getOwnerId() +" - timeStamp="+getContent().getTimeStamp() +" - TTL="+getContent().getTTL();
	}
}

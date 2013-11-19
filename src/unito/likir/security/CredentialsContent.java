package unito.likir.security;

import java.io.Serializable;
import java.security.PublicKey;

import unito.likir.settings.PropFinder;
import unito.likir.settings.Settings;

public class CredentialsContent implements Serializable, Cloneable
{
	private static final long serialVersionUID = -321321321321321321L;
	private final String ownerId;
	private final PublicKey key;
	private final long timeStamp;
	private final long ttl;
	private final byte[] objectHash; 
	
	public CredentialsContent(String ownerId, PublicKey key, long timeStamp, long ttl, byte[] objectHash)
	{
		this.ownerId = ownerId;
		this.key = key;
		this.timeStamp = timeStamp;
		this.ttl = ttl;
		this.objectHash = objectHash;
	}

	public CredentialsContent(String ownerId, PublicKey key, byte[] objectHash)
	{
		this.ownerId = ownerId;
		this.key = key;
		this.timeStamp = System.currentTimeMillis();
		this.ttl = Integer.parseInt(PropFinder.get(Settings.DEFAULT_TTL));
		this.objectHash = objectHash;
	}
	
	public String getOwnerId()
	{
		return ownerId;
	}
	
	public PublicKey getPublicKey()
	{
		return key;
	}

	public long getTimeStamp()
	{
		return timeStamp;
	}

	public long getTTL()
	{
		return ttl;
	}

	public byte[] getObjectHash()
	{
		return objectHash;
	}
}

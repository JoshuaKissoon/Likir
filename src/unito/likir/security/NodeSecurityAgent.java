package unito.likir.security;

import java.io.IOException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;

import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.messages.dht.Nonce;
import unito.likir.messages.dht.RPC;
import unito.likir.messages.dht.RPCMessage;
import unito.likir.storage.StorageEntry;

/**
 * SecurityAgent class
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class NodeSecurityAgent extends SecurityAgent
{
	private static final long serialVersionUID = 6541239839850189728L;
	
	private Node localNode;
	private Random randomGenerator;
	private AuthNodeId localAuthNodeId;
	private PublicKey CAPublicKey;
	
	public NodeSecurityAgent(Node localNode)
	{
		super();
		this.localAuthNodeId = null;
		this.localNode = localNode;
		this.randomGenerator = new MTRandom();
	}
	
	public Node getNode()
	{
		return localNode;
	}
	
	public Random getRandomGenerator()
	{
		return randomGenerator;
	}
	
	public AuthNodeId getAuthNodeId()
	{
		return localAuthNodeId;
	}
	
	public PublicKey getCAPublicKey()
	{
		return CAPublicKey;
	}
	
	public void setAuthNodeId(AuthNodeId authNodeId)
	{
		this.localAuthNodeId = authNodeId;
	}
	
	public void setCAPublicKey(PublicKey key)
	{
		this.CAPublicKey = key;
	}
	
	public synchronized Authenticator buildAuthenticator(NodeId id, byte[] nonce, RPC rpc) throws SignatureException
	{
		byte[] rpcHash = null;
		try
		{
			rpcHash = hash(toBytes(rpc));
		}
		catch (IOException e) //it should never happen
		{
			System.err.println("Error in authenticator construction");
			e.printStackTrace();
		}
		Authenticator auth = new Authenticator(id,nonce,rpcHash);
		byte[] signature = sign(auth.getContent());
		auth.setSignature(signature);
		return auth;
	}
	
	public synchronized Credentials buildCredentials(byte[] object, long ttl) throws SignatureException
	{
		byte[] objectHash = hash(object);
		Credentials credentials = new Credentials(localNode.getUserId(), localNode.getSecurityAgent().getPublicKey(), System.currentTimeMillis(), ttl, objectHash);
		byte[] signature = sign(credentials.getContent());
		credentials.setSignature(signature);
		return credentials;
	}
	
	public synchronized boolean check(RPCMessage message, Nonce receivedNonce, Nonce sentNonce)
	{
		AuthNodeId authNodeId = message.getAuthNodeId();
		Authenticator auth = message.getAuthenticator();
		
		if (auth.getContent().getId().equals(sentNonce.getSender()))
		{
			if (Arrays.equals(auth.getContent().getNonce(),sentNonce.getNonce()))
			{
				if (auth.getContent().getId().equals(localNode.getNodeId()))
				{
					if (authNodeId.getContent().getExpireTime() > System.currentTimeMillis())
					{
						if (verifyHash(message.getRPC(), auth.getContent().getRpcHash()))
						{
							try
							{
								if (verifySignature(auth.getContent(), auth.getSignature(), authNodeId.getContent().getKey()))
								{
									if (verifySignature(authNodeId.getContent(), authNodeId.getSignature(), CAPublicKey))
									{
										return true;
									}
									else
										System.err.println("Msg check failed: Invalid AuthNodeId signature");
								}
								else
									System.err.println("Msg check failed: Invalid authenticator signature");
							}
							catch(SignatureException se)
							{
								return false;
							}
						}
						else
							System.err.println("Msg check failed: Invalid hash");
					}
					else
						System.err.println("Msg check failed: Sender AuthId is expired");
				}
				else
					System.err.println("Msg check failed: Authenticator ID is not the local one");
			}
			else
				System.err.println("Msg check failed: Different nonces");
		}
		else
		System.err.println("Msg check failed: Authenticator ID is not that of the sender");
		return false;
	}
	
	public synchronized boolean check(StorageEntry se) throws SignatureException
	{
		return verifySignature(se.getCredentials().getContent(), se.getCredentials().getSignature(), se.getCredentials().getContent().getPublicKey());
	}
	
	public synchronized Collection<StorageEntry> clean(Collection<StorageEntry> entries)
	{
		Collection<StorageEntry> cleaned = new HashSet<StorageEntry>();
		for (StorageEntry se : entries)
		{
			try
			{
				if (check(se))
					cleaned.add(se);
			}
			catch(SignatureException e)
			{
				System.err.println("Can't verify Credentials signature");
			}
		}
		return cleaned;
	}
}
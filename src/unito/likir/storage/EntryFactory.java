package unito.likir.storage;

import java.security.SignatureException;

import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.security.Credentials;
import unito.likir.settings.PropFinder;
import unito.likir.settings.Settings;

/**
 * A StorageEntry Factory: builds StorageEntries from raw data provided by the user.
 * The Credentials of the produced StorageEntries are related to the current instance
 * of the local Node, specified as a constructor parameter. An EntryFactory is always
 * associated to a specific Node instance.
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class EntryFactory
{
	private Node localNode;
	
	private final String DEFAULT_CONTENT_TYPE;
	
	/**
	 * Create a new EntryFactory instance 
	 * @param localNode the Node which this EntryFactory refers to
	 */
	public EntryFactory(Node localNode)
	{
		this.localNode = localNode;
		this.DEFAULT_CONTENT_TYPE = PropFinder.get(Settings.DEFAULT_CONTENT_TYPE);
	}
	
	/**
	 * Create a new StorageEntry with default type and TTL
	 * @param key the lookup key for the content
	 * @param object the raw content
	 * @return the StorageEntry
	 */
	public StorageEntry	buildStorageEntry(NodeId key, byte[] object)
	{
		return buildStorageEntry(key, object, DEFAULT_CONTENT_TYPE);
	}
	
	/**
	 * Create a new StorageEntry with default type
	 * @param key the lookup key for the content
	 * @param object the raw content
	 * @param ttl the time to live of the content (in milliseconds)
	 * @return the StorageEntry
	 */
	public StorageEntry	buildStorageEntry(NodeId key, byte[] object, long ttl)
	{
		return buildStorageEntry(key, object, DEFAULT_CONTENT_TYPE, ttl);
	}
	
	/**
	 * Create a new StorageEntry with default TTL
	 * @param key the lookup key for the content
	 * @param object the raw content
	 * @param type the content type
	 * @return
	 */
	public StorageEntry	buildStorageEntry(NodeId key, byte[] object, String type)
	{
		if (type == null || type.equals(""))
			type = DEFAULT_CONTENT_TYPE;
		return buildStorageEntry(key, object,type,Integer.parseInt(PropFinder.get(Settings.DEFAULT_TTL)));
	}
	
	/**
	 * Create a new StorageEntry
	 * @param key the lookup key for the content
	 * @param object the raw content
	 * @param type the content type
	 * @param ttl the time to live of the content (in milliseconds)
	 * @return
	 */
	public StorageEntry	buildStorageEntry(NodeId key, byte[] object, String type, long ttl)
	{
		if (type == null || type.equals(""))
			type = DEFAULT_CONTENT_TYPE;
		
		Content content = new ContentImpl(object, type);
		
		Credentials credentials;
		try
		{
			credentials = localNode.getSecurityAgent().buildCredentials(object, ttl);
			return new StorageEntry(key,content,credentials);
		}
		catch (SignatureException e)
		{
			// TODO GESTIRE MEGLIO
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * TODO
	 */
	public StorageEntry	buildUnsignedStorageEntry(NodeId key, byte[] object, String type, long timestamp, long ttl, String owner)
	{
		if (type == null || type.equals(""))
			type = DEFAULT_CONTENT_TYPE;
		
		Content content = new ContentImpl(object, type);
		Credentials credentials = new Credentials(owner, null, timestamp, ttl, null);
		
		return new StorageEntry(key,content,credentials);
	}
}
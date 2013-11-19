package unito.likir.security;

import java.io.Serializable;
import java.util.Collection;

import unito.likir.routing.Contact;
import unito.likir.util.ListUtils;

public class BootstrapList implements Serializable
{
	private static final long serialVersionUID = -8832239063365985515L;
	
	private BootstrapListContent content;
	private byte[] signature;
	
	/**
	 * Create a new bootstrap list
	 * @param contacts the set of contacts
	 * @param randomize if set, contacts will be shuffled
	 */
	public BootstrapList(Collection<Contact> contacts, boolean randomize)
	{
		if (randomize)
		{
			ListUtils.shuffle(contacts);
		}
		this.content = new BootstrapListContent(contacts);
		this.signature = null;
		
	}
	
	public BootstrapListContent getContent()
	{
		return content;
	}
	
	public Collection<Contact> getContacts()
	{
		return content.getContacts();
	}
	
	public byte[] getSignature()
	{
		return signature;
	}
	
	public void setSignature(byte[] signature)
	{
		this.signature = signature;
	}
}

package unito.likir.messages.dht;

import java.util.Collection;

import unito.likir.routing.Contact;

public abstract class AbstractFindResponse extends AbstractRPC implements FindResponse
{
	private static final long serialVersionUID = -3506716097155379819L;
	
	protected Collection<Contact> contacts;
		
	public AbstractFindResponse(long messageId, OpCode opCode, Collection<Contact> contacts)
	{
		super(messageId, opCode);
		this.contacts = contacts;
	}
		
	public Collection<Contact> getContacts()
	{
		return contacts;
	}
}

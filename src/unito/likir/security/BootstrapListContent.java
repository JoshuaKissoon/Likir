package unito.likir.security;

import java.io.Serializable;
import java.util.Collection;

import unito.likir.routing.Contact;

public class BootstrapListContent implements Serializable
{
	private static final long serialVersionUID = -990420283043587868L;
	
	private final Collection<Contact> contacts;
	
	public BootstrapListContent(Collection<Contact> contacts)
	{
		this.contacts = contacts; 
	}
	
	public Collection<Contact> getContacts()
	{
		return contacts;
	}
}

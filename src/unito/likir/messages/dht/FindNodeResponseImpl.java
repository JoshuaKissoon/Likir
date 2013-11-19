package unito.likir.messages.dht;

import java.util.Collection;

import unito.likir.routing.Contact;

public class FindNodeResponseImpl extends AbstractFindResponse implements FindNodeResponse
{
	private static final long serialVersionUID = -5333333333333333L;
	
	public FindNodeResponseImpl(long messageId, Collection<Contact> contacts)
	{
		super(messageId, RPC.OpCode.FIND_NODE_RESPONSE, contacts);
	}
	
	public String toString()
	{
		return "FindNodeResponse: "+ super.toString() +" - nodes="+ contacts;
	}
}
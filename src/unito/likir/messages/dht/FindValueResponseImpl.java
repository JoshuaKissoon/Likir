package unito.likir.messages.dht;

import java.util.Collection;
import java.util.HashMap;

import unito.likir.routing.Contact;
import unito.likir.storage.StorageEntry;

public class FindValueResponseImpl extends AbstractFindResponse implements FindValueResponse
{
	private static final long serialVersionUID = -5297861096651405315L;

	private Collection<StorageEntry> values;
	
	private HashMap<String,Integer> counters;
	
	/**
	 * TODO
	 * @param messageId
	 * @param contacts
	 * @param values
	 */
	public FindValueResponseImpl(long messageId, Collection<Contact> contacts, Collection<StorageEntry> values, HashMap<String,Integer> counters)
	{
		super(messageId, RPC.OpCode.FIND_VALUE_RESPONSE, contacts);
		this.values = values;
		this.counters = counters;
	}
	
	public Collection<StorageEntry> getValues()
	{
		return values;
	}
	
	public HashMap<String,Integer> getCounters()
	{
		return counters;
	}
	
	public String toString()
	{
		return "FindValueResponse: contacts="+ contacts +" - values="+ values + " - counters="+ counters;
	}
}
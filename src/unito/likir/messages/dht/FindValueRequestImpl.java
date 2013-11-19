package unito.likir.messages.dht;

import unito.likir.NodeId;

public class FindValueRequestImpl extends AbstractFindRequest implements FindValueRequest
{
	private static final long serialVersionUID = -4039628197442262174L;
	
	private String type;
	private String owner;
	private boolean recent;
	private boolean countersOnly;
	
	public FindValueRequestImpl(long messageId, NodeId key, String type, String owner, boolean recent, boolean countersOnly)
	{
		super(messageId, RPC.OpCode.FIND_VALUE_REQUEST, key);
		this.type = type;
		this.owner = owner;
		this.recent = recent;
		this.countersOnly = countersOnly;
	}
	
	public String getType()
	{
		return type;
	}
	
	public String getOwner()
	{
		return owner;
	}
	
	public boolean getRecent()
	{
		return recent;
	}
	
	public boolean getCountersOnly()
	{
		return countersOnly;
	}
	
	public String toString()
	{
		return "FindValueRequest: key="+ lookupId +", type= " + type + "  owner= " + owner + " - recent= " + recent;
	}
}
package unito.likir.messages.dht;

import unito.likir.NodeId;

public abstract class AbstractFindRequest extends AbstractRPC implements FindRequest
{
	private static final long serialVersionUID = -7256544814360243479L;
	
	protected NodeId lookupId;
	
	public AbstractFindRequest(long messageId, OpCode opCode, NodeId lookupId)
	{
		super(messageId, opCode);
		this.lookupId = lookupId;
	}
	
	public NodeId getLookupId()
	{
		return lookupId;
	}
}

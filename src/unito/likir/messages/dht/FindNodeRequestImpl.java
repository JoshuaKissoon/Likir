package unito.likir.messages.dht;

import unito.likir.NodeId;

public class FindNodeRequestImpl extends AbstractFindRequest implements FindNodeRequest
{
	private static final long serialVersionUID = -5222222222222222L;
	
	public FindNodeRequestImpl(long messageId, NodeId key)
	{
		super(messageId, RPC.OpCode.FIND_NODE_REQUEST, key);
	}

	public String toString()
	{
        return "FindNodeRequest: "+ super.toString() +" - key = "+ lookupId;
	}
}

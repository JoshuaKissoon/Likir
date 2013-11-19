package unito.likir.messages.dht;

import unito.likir.NodeId;

public interface FindRequest extends RequestRPC
{
	public NodeId getLookupId();
}

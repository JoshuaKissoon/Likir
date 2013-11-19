package unito.likir.messages.dht;

import unito.likir.storage.StorageEntry;

public class StoreRequestImpl extends AbstractRPC implements StoreRequest
{
	private static final long serialVersionUID = 1630595857165824041L;
	
	private StorageEntry[] values;
	private boolean sign = true;
	
	public StoreRequestImpl(long messageId, StorageEntry[] values)
	{
		super(messageId, RPC.OpCode.STORE_REQUEST);
		this.values = values;
	}
	
	public StoreRequestImpl(long messageId, StorageEntry[] values, boolean sign)
	{
		super(messageId, RPC.OpCode.STORE_REQUEST);
		this.values = values;
		this.sign = sign;
	}
	
	public boolean isSigned()
	{
		return sign;
	}
	
	public StorageEntry[] getValues()
	{
		return values;
	}
	
	public String toString()
	{
		return "StoreRequest: "+super.toString()+" - value="+ values;
	}
}
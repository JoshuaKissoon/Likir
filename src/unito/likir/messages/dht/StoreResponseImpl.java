package unito.likir.messages.dht;

public class StoreResponseImpl extends AbstractRPC implements StoreResponse
{
	private static final long serialVersionUID = -5522222222222222L;
	
	private boolean storeResult;
	
	public StoreResponseImpl(long messageId, boolean storeResult)
	{
		super(messageId, RPC.OpCode.STORE_RESPONSE);
		this.storeResult = storeResult;
	}
	
	public boolean getStoreResult()
	{
		return storeResult;
	}
	
	public String toString()
	{
		return  "StoreResponse: "+super.toString()+" - result= " + storeResult;
	}
}
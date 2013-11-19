package unito.likir.messages.dht;

public abstract class AbstractRPC implements RPC
{
	private static final long serialVersionUID = 9067230408452354539L;
	
	protected final OpCode opCode;
	protected final long messageId;
	
	public AbstractRPC(long messageId, OpCode opCode)
	{
		if (opCode == null)
			throw new NullPointerException("The opCode is null!");
		
		this.messageId = messageId;
		this.opCode = opCode;
	}
	
    public long getMessageId()
    {
    	return messageId;
    }
    
    public OpCode getRPCOpCode()
    {
    	return opCode;
    }
    
    public String toString()
    {
    	return "msgId=" + messageId;
    }
}

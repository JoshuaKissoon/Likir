package unito.likir.messages.dht;

public class PingResponseImpl extends AbstractRPC implements PingResponse
{
	private static final long serialVersionUID = -5777777777777777L;
	
	private long upTime;
	
	public PingResponseImpl(long messageId, long upTime)
	{
		super(messageId, RPC.OpCode.PING_RESPONSE);
		this.upTime = upTime;
	}
    
    public long getUpTime()
    {
    	return upTime;
    }
    
	public String toString()
	{
		return "PingResponse: "+super.toString()+" - upTime= "+upTime;
	}
}

package unito.likir.messages.dht;

public class PingRequestImpl extends AbstractRPC implements PingRequest
{
	private static final long serialVersionUID = -5666666666666666L;
	
	public PingRequestImpl(long messageId)
	{
		super(messageId, RPC.OpCode.PING_REQUEST);
	}
	
	public String toString()
	{
		return "PingRequest - "+ super.toString();
	}
}
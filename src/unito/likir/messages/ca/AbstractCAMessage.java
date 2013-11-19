package unito.likir.messages.ca;

public abstract class AbstractCAMessage implements CAMessage
{
	private static final long serialVersionUID = -98384163782552111L;
	
	private String userId;
	private OpCode opCode;
	private long messageId;
	
	public AbstractCAMessage(String userId, OpCode opCode, long messageId)
	{
		this.userId = userId;
		this.opCode = opCode;
		this.messageId = messageId;
	}
	
	public String getUserId()
	{
		return userId;
	}
	
	public OpCode getOpCode()
	{
		return opCode;
	}
	
	public long getMessageId()
	{
		return messageId;
	}
	
	public String toString()
	{
		StringBuilder buffer = new StringBuilder();
        buffer.append("AC Message: \n");
        return buffer.toString();
	}
}

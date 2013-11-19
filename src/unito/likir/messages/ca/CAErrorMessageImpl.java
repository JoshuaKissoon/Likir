package unito.likir.messages.ca;

public class CAErrorMessageImpl extends AbstractCAMessage implements CAErrorMessage
{
	private static final long serialVersionUID = 8366889989904016660L;
	
	private String errorMessage;
	
	public CAErrorMessageImpl(String userId, long messageId, String errorMessage)
	{
		super(userId, OpCode.ERROR, messageId);
		this.errorMessage = errorMessage;
	}
	
	public String getErrorMessage()
	{
		return errorMessage;
	}
}

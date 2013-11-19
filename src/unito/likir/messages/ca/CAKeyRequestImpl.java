package unito.likir.messages.ca;

public class CAKeyRequestImpl extends AbstractCAMessage implements CAKeyRequest
{
	private static final long serialVersionUID = -5584245647760218423L;

	public CAKeyRequestImpl(String userId, long messageId)
	{
		super(userId, OpCode.CA_KEY_REQUEST, messageId);
	}
}

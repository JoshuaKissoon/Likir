package unito.likir.messages.ca;

import java.security.PublicKey;

public class CAKeyResponseImpl extends AbstractCAMessage implements CAKeyResponse
{
	private static final long serialVersionUID = 3469863021846205134L;
	
	private PublicKey CAkey;
	
	public CAKeyResponseImpl(String userId, long messageId, PublicKey CAkey)
	{
		super(userId, OpCode.CA_KEY_RESPONSE, messageId);
		this.CAkey = CAkey;
	}
	
	public PublicKey getCAKey()
	{
		return CAkey;
	}
}

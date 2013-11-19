package unito.likir.messages.ca;

import java.security.PublicKey;

public interface CAKeyResponse extends CAMessage
{
	public PublicKey getCAKey();
}

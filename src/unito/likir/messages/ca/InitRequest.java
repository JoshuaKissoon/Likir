package unito.likir.messages.ca;

import java.net.SocketAddress;
import java.security.PublicKey;

public interface InitRequest extends CAMessage
{
	public PublicKey getPublicKey();
	
	public SocketAddress getUDPAddress();
}

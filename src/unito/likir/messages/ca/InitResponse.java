package unito.likir.messages.ca;

import unito.likir.security.AuthNodeId;
import unito.likir.security.BootstrapList;

public interface InitResponse extends CAMessage
{
	public AuthNodeId getAuthNodeId();
	
	public BootstrapList getBootstrapList();
}

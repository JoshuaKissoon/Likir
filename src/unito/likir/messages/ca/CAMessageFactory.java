package unito.likir.messages.ca;

import java.security.PublicKey;

import unito.likir.security.AuthNodeId;
import unito.likir.security.BootstrapList;

public interface CAMessageFactory
{
	public InitResponse createInitResponse(String userId, long messageId, AuthNodeId authNodeId, BootstrapList bootstrapList);
	
	public CAKeyResponse createCAKeyResponse(String userId, long messageId, PublicKey key);
	
	public CAErrorMessage createErrorMessage(String userId, long messageId, String errorMessage);
}

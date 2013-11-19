package unito.likir.messages.ca;

import java.security.PublicKey;

import unito.likir.security.AuthNodeId;
import unito.likir.security.BootstrapList;

public class CAMessageFactoryImpl implements CAMessageFactory
{
    public InitResponse createInitResponse(String userId, long messageId, AuthNodeId authNodeId, BootstrapList bootstrapList)
    {
    	return new InitResponseImpl(userId, messageId, authNodeId, bootstrapList);
    }
    
    public CAKeyResponse createCAKeyResponse(String userId, long messageId, PublicKey key)
    {
    	return new CAKeyResponseImpl(userId, messageId, key);
    }
    
    public CAErrorMessage createErrorMessage(String userId, long messageId, String errorMessage)
    {
    	return new CAErrorMessageImpl(userId, messageId, errorMessage);
    }
}
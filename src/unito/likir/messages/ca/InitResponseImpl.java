package unito.likir.messages.ca;

import unito.likir.routing.Contact;
import unito.likir.security.AuthNodeId;
import unito.likir.security.BootstrapList;

public class InitResponseImpl extends AbstractCAMessage implements InitResponse
{
	private static final long serialVersionUID = -5714778365768681537L;
	
	private AuthNodeId authNodeId;
	private BootstrapList bootstrapList;
	
	public InitResponseImpl(String userId, long messageId, AuthNodeId authNodeId, BootstrapList bootstrapList)
	{
		super(userId, OpCode.INITIALIZATION_RESPONSE, messageId);
		this.authNodeId = authNodeId;
		this.bootstrapList = bootstrapList;
	}
	
	public AuthNodeId getAuthNodeId()
	{
		return authNodeId;
	}
	
	public BootstrapList getBootstrapList()
	{
		return bootstrapList;
	}
	
	public String toString()
	{
		StringBuilder buffer = new StringBuilder();
        buffer.append("InitResponse : ");
        buffer.append("\n      " + authNodeId);
        buffer.append("\n      BootstrapList : "); 
        for (Contact c : bootstrapList.getContacts())
        	buffer.append("\n         "+c);
        return buffer.toString();
	}
	
}

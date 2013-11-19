package unito.likir.messages.dht;

public interface FindValueRequest extends FindRequest
{
	public String getType();
	
	public String getOwner();
	
	public boolean getRecent();
	
	public boolean getCountersOnly();
}

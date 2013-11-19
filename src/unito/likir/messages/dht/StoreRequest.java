package unito.likir.messages.dht;

import unito.likir.storage.StorageEntry;

public interface StoreRequest extends RequestRPC
{
	public StorageEntry[] getValues();
	
	public boolean isSigned();
}

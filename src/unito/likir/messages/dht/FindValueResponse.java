package unito.likir.messages.dht;

import java.util.Collection;
import java.util.HashMap;

import unito.likir.routing.Contact;
import unito.likir.storage.StorageEntry;

public interface FindValueResponse extends FindResponse
{
    /**
     * Returns a Collection of DHTValueEntity(s)
     */
    public Collection<StorageEntry> getValues();
    
    public Collection<Contact> getContacts();
    
    public HashMap<String,Integer> getCounters();
}

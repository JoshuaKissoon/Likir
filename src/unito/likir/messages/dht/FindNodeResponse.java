package unito.likir.messages.dht;

import java.util.Collection;

import unito.likir.routing.Contact;

public interface FindNodeResponse extends FindResponse
{
    /**
     * Returns the k-closest (or less) Node's to the id we were looking for
     */
    public Collection<Contact> getContacts();
}

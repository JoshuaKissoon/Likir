package unito.likir.routing;

import java.net.SocketAddress;
import java.io.Serializable;

import unito.likir.*;

/**
 * A Contact is a reference to a Node in the DHT. The Contact interface encapsulates 
 * all required informations to contact the remote Node or to keep track of its current
 * State in the RouteTable.
 * @author Luca Maria Aiello
 * @version 0.1
 */

public interface Contact extends Serializable
{
    /**
     * Various states a Contact may have
     */
    public static enum State
    {
        // A Contact is alive either if we got a 
        // response to a request or we received a 
        // request from the Contact
        ALIVE, 
        
        // A Contact is dead if it fails to respond
        // a certain number of times to our requests
        DEAD,
    }
    
	/**
	 * Returns the NodeId of the contact
	 * @return the NodeId
	 */
	public NodeId getNodeId();
	
	/**
	 * Returns the network address o the contact
	 * @return the network address
	 */
	public SocketAddress getAddress();
	
    /**
     * Returns whether or not this Contact is ALIVE
     * @return true if the contact is ALIVE
     */
    public boolean isAlive();
    
    /**
     * Returns whether or not this Contact is in DEAD or SHUTDOWN state
     * @return true if the contact is DEAD
     */
    public boolean isDead();
    
    /**
     * Set this contact state to ALIVE
    */
    public void setAliveState();
    
    /**
     * Set this contact state to DEAD
    */
    public void setDeadState();
    
    /**
     * Sets the time of the last successful contact
     * @param the time of the last successful contact
    */
   public void setLastContact(long timeStamp);
   
   /**
    * Returns the time of the last successful exchange with this node
    * @return the time of the last successful contact
    */
   public long getLastContact();
   
   /**
    * Returns the number of failures have occurred since
    * the last successful contact
    * @return the number of failures
    */
   public int getFailures();
   
   /**
    * Increments the failure counter, sets the last dead or alive time
    * and so on
    */
   public void handleFailure();
	
   public void refresh();
}

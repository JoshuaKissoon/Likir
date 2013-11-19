package unito.likir.routing;

import java.net.SocketAddress;
import java.lang.Comparable;

import unito.likir.NodeId;
import unito.likir.settings.PropFinder;
import unito.likir.settings.Settings;

/**
 * ContactImpl class
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class ContactImpl implements Contact, Comparable<ContactImpl>
{
	private static final long serialVersionUID = -2111111111111111111L;
	
	private final NodeId nodeId;
    private SocketAddress address;
    private State state;
    private transient int failureCounter;
    private transient long lastContact; //serve?
    private final int MAX_ACCEPT_NODE_FAILURES;
    
    /**
     * Constructs a RemoteContact from the specified parameters
     * @param nodeId contact NodeId
     * @param address contact SocketAddress
     * @param state contact state
     * @param failures number of failures
     */
    public ContactImpl(NodeId nodeId, SocketAddress address, State state, int failures)
    {
    	this.nodeId = nodeId;
    	this.address = address;
    	this.state = state;
    	this.failureCounter = failures;
    	this.MAX_ACCEPT_NODE_FAILURES = Integer.parseInt(PropFinder.get(Settings.MAX_ACCEPT_NODE_FAILURES));
    }
    
    /**
     * Constructs a RemoteContact from the specified parameters.
     * Initializes the contact state to ALIVE and the failure counter to zero
     * @param nodeId contact NodeId
     * @param address contact SocketAddress
     */
    public ContactImpl(NodeId nodeId, SocketAddress address)
    {
    	this.nodeId = nodeId;
    	this.address = address;
    	this.state = State.ALIVE;
    	this.failureCounter = 0;
    	this.MAX_ACCEPT_NODE_FAILURES = Integer.parseInt(PropFinder.get(Settings.MAX_ACCEPT_NODE_FAILURES));
    }
    
	/**
	 * Returns the NodeId of the contact
	 * @return the NodeId
	 */
	public NodeId getNodeId()
	{
		return this.nodeId;
	}
	
	/**
	 * Returns the network address o the contact
	 * @return the network address
	 */
	public SocketAddress getAddress()
	{
		return this.address;
	}
	
    /**
     * Returns whether or not this Contact is ALIVE
     * @return true if the contact is ALIVE
     */
    public boolean isAlive()
    {
    	return State.ALIVE.equals(state);
    }
    
    /**
     * Returns whether or not this Contact is in DEAD or SHUTDOWN state
     * @return true if the contact is DEAD
     */
    public boolean isDead()
    {
    	return State.DEAD.equals(state);
    }
    
    /**
     * Set this contact state to ALIVE
    */
    public void setAliveState()
    {
    	state = State.ALIVE;
    }
    
    /**
     * Set this contact state to DEAD
    */
    public void setDeadState()
    {
    	state = State.DEAD;
    }
    
    /**
     * Sets the time of the last successful contact
     * @param the time of the last successful contact
    */
   public void setLastContact(long timeStamp)
   {
	   this.lastContact = timeStamp;
   }
   
   /**
    * Returns the time of the last successful exchange with this node
    * @return the time of the last successful contact
    */
   public long getLastContact()
   {
	   return lastContact;
   }
   
   /**
    * Returns the number of failures have occurred since
    * the last successful contact
    * @return the number of failures
    */
   public int getFailures()
   {
	   return failureCounter;
   }
   
   /**
    * Increments the failure counter, sets the last dead or alive time
    * and so on
    */
   public void handleFailure()
   {
	   failureCounter++;
	   if (failureCounter >= MAX_ACCEPT_NODE_FAILURES)
		   state = State.DEAD;
   }
   
   public boolean equals(Object obj)
   {
	   try
	   {
		   ContactImpl castedObject = (ContactImpl) obj;
		   if (this.nodeId.equals(castedObject.getNodeId()))
			   return true;
		   else
			   return false;
	   }
	   catch(Exception e)
	   {
		   return false;
	   }
   }
   
   public int compareTo(ContactImpl c)
   {
	   return this.nodeId.compareTo(c.getNodeId());
   }
   
   public void refresh()
   {
	   this.lastContact = System.currentTimeMillis();
	   this.failureCounter = 0;
	   this.state = State.ALIVE;
   }
   
   /**
    * Returns a String representation of this RemoteContact
    * @return a String representation of the object
    */
   public String toString()
   {
	   StringBuilder buffer = new StringBuilder();
       buffer.append("Contact")
             .append(" (Id=").append(this.nodeId)
             .append(", Address=").append(this.address)
             .append(", State=").append(this.state)
             .append(", Failures=").append(this.failureCounter)
             .append(")");
       
       return buffer.toString();
   }

}
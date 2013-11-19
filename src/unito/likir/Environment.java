package unito.likir;

import java.util.Collection;

/**
 * The Environment host references to a set of Nodes. It easily allows creation and initialization of
 * a small Likir network and offers handles to each registered Node. 
 * @author Luca Maria Aiello
 * @version 0.1
 */

public interface Environment
{
	/**
	 * Registers a new Node with specified userId to a random port. Node must be initialized
	 * in order to get his NodeId. Node must be bootstrapped to join the network.
	 * If the port is unavailable the Node will be registered to another system designated port
	 * @param userId the unique id of the Node owner
	 * @return the registered Node
	 * @throws IllegalArgumentException if userId used by another Node in this environment 
	 */
	public Node registerNode(String userId);
	
	/**
	 * Registers a new Node with specified userId to the specified port. Node must be initialized
	 * in order to get his NodeId. Node must be bootstrapped to join the network.
	 * If the port is unavailable the Node will be registered to another system designated port
	 * @param userId the unique id of the Node owner
	 * @param the UDP port this Node will use
	 * @return the registered Node
	 * @throws IllegalArgumentException if userId used by another Node in this environment 
	 */
	public Node registerNode(String userId, int port);
	
	/**
	 * Registers nodeNumber Nodes whose NodeIds has the same prefix userIdPrefix. UserId suffixes
	 * are integer values starting from 0. The i-th node is registered to the UDP port intialPort+i.
	 * Each Node must be initialized in order to get his NodeId.
	 * Each Node must be bootstrapped to join the network. 
	 * @param nodeNumber the number of nodes to be registered
	 * @param userIdPrefix the common userId prefix
	 * @param initialPort 
	 */
	public void registerNodes(int nodeNumber, String userIdPrefix, int initialPort);
	
	/**
	 * Returns a handle to the Node corresponding to userId, null if this node is not
	 * registered to this Environment
	 * @param userId the userId
	 * @return the Node corresponding to userId
	 */
	public Node selectNode(String userId);
	
	/**
	 * Returns a reference to the Node corresponding to nodeId, null if this node is not
	 * registered to this Environment
	 * @param nodeId the nodeId
	 * @return the Node corresponding to nodeId
	 */
	public Node selectNode(NodeId nodeId);
	
	/**
	 * Returns the number of nodes registered to this Environment
	 * @return the number of nodes
	 */
	public int size();
	
	/**
	 * Returns a Collection view of all the registered Nodes
	 * @return the collection view
	 */
	public Collection<Node> getAllNodes();
	
	/**
	 * Removes the Node bounded to nodeId from the environment.
	 * The Node is NOT removed from the network and continues to be alive.
	 * @param userId the userId
	 */
	public void unregisterNode(String userId);
	
	/**
	 * RIVEDI!!!
	 */
	public void loadNetwork();
	
	/**
	 * Initializes all the registered Nodes
	 */
	public void initAll();
	
	/**
	 * Startups all the registered Nodes
	 */
	public void startupAll();
	
	/**
	 * Bootstraps all the registered Nodes
	 */
	public void bootstrapAll();
	
	/**
	 * Kills all the registered Nodes
	 * @param save true to save Nodes state
	 */
	public void shutDownAll(boolean save);
}
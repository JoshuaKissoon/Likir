package unito.likir;

/**
 * An application that lies over the Likir DHT.
 * Every application which has to use the Node primitives can extend this class
 * to acquire methods for register and retrieve its Likir Node
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class Application
{
	private Node node;
	private Environment env;
	
	/**
	 * Creates a new Application
	 */
	public Application()
	{
		this.node = null;
		this.env = null;
	}
	
	/**
	 * Creates a new Application upon the given Environment
	 * @param env the Environment
	 */
	public Application(Environment env)
	{
		this.node = null;
		this.env = env;
	}
	
	/**
	 * Returns the Node this Application is registered to, null if this Application
	 * wasn't registered to any Node
	 * @return the Node
	 */
	public Node getNode()
	{
		return node;
	}
	
	/**
	 * Returns the Environment upon which this Application was created, null
	 * if the Application was created without specifying an Environment 
	 * @return the Environment
	 */
	public Environment getEnvironment()
	{
		return env;
	}
	
	/**
	 * Register this Application on a newly generated Node.
	 * The returned node is NOT initialized. Must call the init() procedure on the return value of 
	 * this method to initialize it
	 * @param userId the userID
	 * @param port the Node's UDP port
	 * @return the registered node
	 * @throws IllegalArgumentException if an Environment was specified and userId is not unique in that Environment
	 */
	public Node registerNode(String userId, int port) throws IllegalArgumentException
	{
		if (env != null)
		{
			node = env.registerNode(userId,port);
		}
		else
		{	
			node = new Node(userId, port);
		}
		return node;
	}
	
	/**
	 * Register this Application on a newly generated Node.
	 * The returned node is NOT initialized. Must call the init() procedure on the return value of 
	 * this method to initialize it
	 * @param userId the userID
	 * @return the registered node
	 * @throws IllegalArgumentException if an Environment was specified and userId is not unique in that Environment
	 */
	public Node registerNode(String userId) throws IllegalArgumentException
	{
		if (env != null)
		{
			node = env.registerNode(userId);
		}
		else
		{	
			node = new Node(userId);
		}
		return node;
	}
	
	/**
	 * Register this Application on the given Node
	 * @param node the Node
	 */
	public void registerNode(Node node)
	{
		this.node = node;
	}
	
	/**
	 * Unregister the Application from its Node
	 * @param save
	 */
	public void unregister()
	{
		node = null;
	}
}

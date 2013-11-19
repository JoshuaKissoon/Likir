package unito.likir.ca;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.messages.ca.CAMessageFactory;
import unito.likir.messages.ca.CAMessageFactoryImpl;
import unito.likir.routing.Contact;
import unito.likir.routing.ContactImpl;
import unito.likir.security.AuthNodeId;
import unito.likir.security.MTRandom;
import unito.likir.settings.PropFinder;
import unito.likir.settings.Settings;

/**
 * A Central Authority implementation of the Certification Service
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class CA extends Thread implements Serializable
{
	private static final long serialVersionUID = 8899915854998325975L;
	
	private boolean isDone;
	
	private Map<String, AuthNodeId> registeredNodesData;
	private List<Contact> contactList;
	private transient MTRandom randomGen;
	private transient CAMessageFactory messageFactory;
	private CASecurityAgent securityAgent;
	private Node node;
	private transient Prober prober;
	
	private transient ServerSocket serverSocket;
    private transient Socket clientSocket;
	
	private transient ExecutorService executor;
	private transient ScheduledExecutorService scheduledExecutor;
	
	private final String CA_ID;
	private final String CA_PERSISTENCE_PATH;
	private final int DEFAULT_CA_PORT;
	private final long PROBING_PERIOD;
	private final int CONTACT_LIST_MIN_SIZE;
	
	public CA() throws IOException
	{
		CA_ID = PropFinder.get(Settings.CA_ID);
		CA_PERSISTENCE_PATH = PropFinder.get(Settings.CA_PERSISTENCE_PATH);
		DEFAULT_CA_PORT = Integer.parseInt(PropFinder.get(Settings.DEFAULT_CA_PORT));
		PROBING_PERIOD = Integer.parseInt(PropFinder.get(Settings.PROBING_PERIOD));
		CONTACT_LIST_MIN_SIZE = Integer.parseInt(PropFinder.get(Settings.CONTACT_LIST_MIN_SIZE));
		
		this.isDone = false;
		
		this.registeredNodesData = Collections.synchronizedMap(new HashMap<String, AuthNodeId>());
		this.randomGen = new MTRandom(666);
		this.messageFactory = new CAMessageFactoryImpl();
		this.executor = Executors.newCachedThreadPool();
		this.scheduledExecutor = Executors.newScheduledThreadPool(1);
		this.securityAgent = new CASecurityAgent();
		this.contactList = Collections.synchronizedList(new LinkedList<Contact>());
		
		serverSocket = new ServerSocket(DEFAULT_CA_PORT);
        System.out.println ("CA: running on : " + serverSocket);
        
        node = new Node(CA_ID, 9000);
        prober = new Prober(this);
	}
	
	private void loadState() throws IOException
	{
		try
		{
			String path = CA_PERSISTENCE_PATH + File.separator + CA_ID;
			File nodeFile = new File(path);
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(nodeFile));
			CA ca = (CA) ois.readObject();
			if (ca.getUserId() != null && ca.getSecurityAgent() != null
					&& ca.getRegisteredNodeData() != null
					&& ca.getNode() != null
					&& ca.getContactList() != null)
			{
				this.securityAgent.setKeyPair(ca.getSecurityAgent().getKeyPair());
				this.registeredNodesData = ca.getRegisteredNodeData();
				//this.node.loadState(ca.getNode());
				this.contactList = ca.getContactList();
			}
			else
			{
				throw new IOException("Can't load CA data");
			}	
		}
		catch (ClassNotFoundException cnfe)
		{
			throw new IOException("Corrupted file, can't load CA state");
		}
	}
	
	public boolean isDone()
	{
		return isDone;
	}
	
	public void done()
	{
		isDone = true;
	}
	
	public String getUserId()
	{
		return CA_ID;
	}
	
	public Node getNode()
	{
		return node;
	}
	
	public CAMessageFactory getMessageFactory()
	{
		return messageFactory;
	}
	
	public CASecurityAgent getSecurityAgent()
	{
		return securityAgent;
	}
	
	public ExecutorService getExecutor()
	{
		return executor;
	}
	
	public Map<String, AuthNodeId> getRegisteredNodeData()
	{
		return registeredNodesData;
	}
	
	public List<Contact> getContactList()
	{
		return contactList;
	}
	
	public void addContact(NodeId userNodeId, SocketAddress clientAddress)
	{
		//TODO: pensa anche ad un modo per rimuovere le entry
		if (contactList.size() < CONTACT_LIST_MIN_SIZE)
		{
			Contact contact = new ContactImpl(userNodeId, clientAddress);
			contactList.add(0, contact);
		}
	}
	
	public synchronized NodeId genNodeId()
	{
		byte[] rawBytes = new byte[20];
		randomGen.nextBytes(rawBytes);
		return new NodeId(rawBytes);
	}
	
	public void startup() throws Exception //modifica
	{
		startup(DEFAULT_CA_PORT);
	}
	
	public void startup(int port) throws Exception //modifica
	{
		try
		{
			loadState();
			System.out.println("State loaded successfully");
			start();
		}
		catch(Exception e) //can't load AC state -e.g. first boot-
		{
			System.out.println("-- Can't load AC state --");
			//generates a new keyPair
			System.out.println("Generating new CA key pair...");
			securityAgent.initKeyPair();
			start();
			System.out.println("Initializing new Node...");
			node.init();
		}
		System.out.println("Public key:");
		System.out.println(securityAgent.getPublicKey());
		System.out.println("Starting prober...");
		scheduledExecutor.scheduleWithFixedDelay(prober, PROBING_PERIOD, PROBING_PERIOD, Settings.DEFAULT_TIME_UNIT);
		System.out.println("CA RUNNING");
		System.out.println(this);
	}
	
	public void shutdown(boolean save)
	{
		System.out.println("SHUTDOWN");
		executor.shutdown();
		scheduledExecutor.shutdown();
		isDone();
		Thread.currentThread().interrupt();
		
		if (save)
		{
			try
			{	
				saveState();
			}	
			catch(IOException ioe)
			{
				System.err.println("Can't save CA state");
				ioe.printStackTrace();
			}
		}
	}
	
	private void saveState() throws IOException
	{
		String path = CA_PERSISTENCE_PATH;
		File nodeDir = new File(path);
		if (!nodeDir.exists())
			nodeDir.mkdir();
		path = path+"\\" + CA_ID;
		File nodeFile = new File(path);
		nodeFile.createNewFile();
		
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(nodeFile));
		oos.writeObject(this);
	}
	
	public void run()
    {
		while (!isDone())
        {
			try
			{
				action();
            }
			catch (InterruptedException e)
	        {
	            System.out.println("AC service interrupted");
	            done();
	        }
			catch (Exception e)
	        {
	            System.out.println("ERROR in the running AC instance");
	            done();
	            e.printStackTrace();
	        }
        }
        
    }
	
	public void action() throws InterruptedException, IOException
	{
		//wait for incoming connections
        clientSocket = serverSocket.accept();
        
        //create a thread to manage the connection
        CAInst inst = new CAInst(this, clientSocket);
        executor.execute(inst);
	}
	
	public String toString()
	{
		StringBuilder buffer = new StringBuilder();
        buffer.append("Central Authority: \n");
        buffer.append("  Node: ");
        buffer.append(node + "\n");
        buffer.append("  UserId list: \n");
        for (String uid : registeredNodesData.keySet())
        {
        	buffer.append("    " + uid + "\n");
        }
        buffer.append("  Contact list: \n");
        for (Contact c : contactList)
        {
        	buffer.append("    " + c + "\n");
        }
        return buffer.toString();
	}
    
    /*public static void main(String[] args)
    {
    	try
        {
            CA ca = new CA();
            ca.startup();
            
            InputStreamReader inReader = new InputStreamReader(System.in);
    		BufferedReader bufReader = new BufferedReader(inReader);
            bufReader.readLine();
            ca.interrupt();
            ca.shutdown(false);
        }
        catch(Exception e)
        {
            System.out.println("ERROR in CA!");
            e.printStackTrace();
        }
    }*/
}

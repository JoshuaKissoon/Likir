package unito.likir;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import unito.likir.io.FileManager;
import unito.likir.io.FindNodeManager;
import unito.likir.io.FindValueManager;
import unito.likir.io.GetManager;
import unito.likir.io.PutManager;
import unito.likir.io.LookupManager;
import unito.likir.io.MessageDispatcher;
import unito.likir.io.NodeInitializator;
import unito.likir.io.ObservableFuture;
import unito.likir.io.PingManager;
import unito.likir.io.StoreManager;
import unito.likir.io.UnsignedGetManager;
import unito.likir.messages.dht.RPCMessage;
import unito.likir.messages.dht.RPCMessageFactory;
import unito.likir.messages.dht.RPCMessageFactoryImpl;
import unito.likir.routing.Contact;
import unito.likir.routing.RouteTable;
import unito.likir.routing.RouteTableImpl;
import unito.likir.routing.RouteTableRefresher;
import unito.likir.security.AuthNodeId;
import unito.likir.security.MTRandom;
import unito.likir.security.NodeSecurityAgent;
import unito.likir.settings.PropFinder;
import unito.likir.settings.Settings;
import unito.likir.storage.EntryFactory;
import unito.likir.storage.Storage;
import unito.likir.storage.StorageCleaner;
import unito.likir.storage.StorageEntry;
import unito.likir.storage.StorageImpl;

/**
 * A Likir Node. Maintains references to the Storage and to the Route Table,
 * provides a message dispatcher, executors for the manager threads and methods
 * to send all the Kademlia RPCs and perform the iterative lookup and store
 * procedures
 *
 * @author Aiello Luca Maria
 * @version 0.11b
 */
public class Node implements Serializable
{

    private static final long serialVersionUID = -6009969378177843425L;

    private final int MIN_PORT = 1024;
    private final int MAX_PORT = 65535;

    //node state
    private NodeId nodeId;
    private InetSocketAddress localAddress;
    private String userId;
    private RouteTable routeTable;
    private Storage storage;
    private transient long startTime;
    private boolean alive;
    private Set<String> blacklist;

    //inner managers
    private transient NodeInitializator initializator;
    private transient ExecutorService executor;
    private transient ScheduledExecutorService innerExecutor;
    private transient StorageCleaner storageCleaner;
    private transient RouteTableRefresher routeTableRefresher;
    private NodeSecurityAgent securityAgent;
    private transient EntryFactory entryFactory;

    //IO managers
    private transient MessageDispatcher messageDispatcher;
    private transient RPCMessageFactory messageFactory;
    private transient FileManager fileManager;

    //Settings parameters
    private final String NODE_PERSISTENCE_PATH;
    private final String CS_KEY_PATH;
    private final int MAX_CONTENT_SIZE = 64000;

    //------------ Constructor and initialization -------------
    /**
     * Creates a new node registered to a random UDP port
     *
     * @param userId the userId
     */
    public Node(String userId)
    {
        this(userId, 0);
    }

    /**
     * Creates a new Node registered to the specified UDP port If port is
     * unavailable it assigns a system selected port to this Node.
     *
     * @param userId the userId
     * @param port the port
     */
    public Node(String userId, int port)
    {
        if (userId == null)
        {
            throw new IllegalArgumentException("UserId cannot be null");
        }

        this.NODE_PERSISTENCE_PATH = PropFinder.get(Settings.NODE_PERSISTENCE_PATH);
        this.CS_KEY_PATH = PropFinder.get(Settings.CS_KEY_PATH);

        if (port < MIN_PORT || port > MAX_PORT)
        {
            MTRandom rand = new MTRandom();
            port = rand.getUniform(MIN_PORT, MAX_PORT);
        }
        this.userId = userId.toLowerCase();
        this.startTime = 0;

        localAddress = null;
        int i = 0;
        while (messageDispatcher == null)
        {
            try
            {
                localAddress = new InetSocketAddress(InetAddress.getLocalHost(), port + i);
            }
            catch (UnknownHostException e)
            {
                localAddress = new InetSocketAddress("localhost", port + i);
            }
            try
            {
                this.messageDispatcher = new MessageDispatcher(this, localAddress);

            }
            catch (SocketException se)
            {
                i++; //increments the port value
            }
        } //TODO: gestisci il caso in cui non si riesca proprio ad ottenere un indirizzo

        this.storage = new StorageImpl();
        this.initializator = new NodeInitializator(this);
        this.executor = Executors.newCachedThreadPool();
        this.innerExecutor = Executors.newScheduledThreadPool(2);
        this.securityAgent = new NodeSecurityAgent(this);
        this.messageFactory = new RPCMessageFactoryImpl(this);
        this.storageCleaner = new StorageCleaner(this);
        this.routeTableRefresher = new RouteTableRefresher(this);
        this.entryFactory = new EntryFactory(this);
        this.fileManager = new FileManager();
        this.blacklist = Collections.synchronizedSet(new TreeSet<String>());

        this.alive = true;
    }

    /**
     * Creates a new Node loading its data from file
     *
     * @param f the data file
     * @throws IOException if an error occurs while loading data from file
     */
    public Node(File f) throws IOException
    {
        this.NODE_PERSISTENCE_PATH = PropFinder.get(Settings.NODE_PERSISTENCE_PATH);
        this.CS_KEY_PATH = PropFinder.get(Settings.CS_KEY_PATH);

        this.startTime = 0;

        this.storage = new StorageImpl();
        this.initializator = new NodeInitializator(this);
        this.executor = Executors.newCachedThreadPool();
        this.innerExecutor = Executors.newScheduledThreadPool(2);
        this.securityAgent = new NodeSecurityAgent(this);
        this.messageFactory = new RPCMessageFactoryImpl(this);
        this.storageCleaner = new StorageCleaner(this);
        this.routeTableRefresher = new RouteTableRefresher(this);
        this.entryFactory = new EntryFactory(this);
        this.fileManager = new FileManager();
        this.blacklist = Collections.synchronizedSet(new TreeSet<String>());

        localAddress = null;
        int port = 0;
        try
        {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
            Node n = (Node) ois.readObject();
            if (n.getNodeId() != null
                    && n.getStorage() != null
                    && n.getSecurityAgent() != null
                    && n.getRouteTable() != null)
            {
                this.nodeId = n.getNodeId();
                this.userId = n.getUserId();

                this.securityAgent.setKeyPair(n.getSecurityAgent().getKeyPair());
                this.securityAgent.setAuthNodeId(n.getSecurityAgent().getAuthNodeId());
                this.securityAgent.setCAPublicKey(n.getSecurityAgent().getCAPublicKey());
                //this.securityAgent = n.getSecurityAgent();

                this.routeTable = new RouteTableImpl(this);
                this.storage = new StorageImpl();
                this.storage = n.getStorage();
                this.routeTable.fill(n.getRouteTable().getAllBucketContacts());
                port = n.getAddress().getPort();
            }
            else
            {
                throw new IOException("Can't load Node data from file " + f.getName());
            }
        }
        catch (ClassNotFoundException cnfe)
        {
            throw new IOException("Corrupted data: can't load node from file " + f.getName());
        }

        boolean addressOk = false;
        int inc = 0;
        while (!addressOk)
        {
            try
            {
                localAddress = new InetSocketAddress(InetAddress.getLocalHost(), port + inc);
            }
            catch (UnknownHostException e)
            {
                localAddress = new InetSocketAddress("localhost", port + inc);
            }
            try
            {
                this.messageDispatcher = new MessageDispatcher(this, localAddress);
                addressOk = true;
            }
            catch (SocketException se)
            {
                System.err.println("Requested port (" + port + ") is not available for node " + nodeId);
                inc++;
            }
        }

        this.alive = true;
    }

    /**
     * Creates a new Node loading its data from file
     *
     * @param f the data file
     * @throws IOException if an error occurs while loading data from file
     */
    public Node(String userId, int port, boolean xxx) throws IOException
    {
        this.NODE_PERSISTENCE_PATH = PropFinder.get(Settings.NODE_PERSISTENCE_PATH);
        this.CS_KEY_PATH = PropFinder.get(Settings.CS_KEY_PATH);

        this.startTime = 0;

        this.userId = userId;
        this.storage = new StorageImpl();
        this.initializator = new NodeInitializator(this);
        this.executor = Executors.newCachedThreadPool();
        this.innerExecutor = Executors.newScheduledThreadPool(2);
        this.securityAgent = new NodeSecurityAgent(this);
        this.messageFactory = new RPCMessageFactoryImpl(this);
        this.storageCleaner = new StorageCleaner(this);
        this.routeTableRefresher = new RouteTableRefresher(this);
        this.entryFactory = new EntryFactory(this);
        this.fileManager = new FileManager();
        this.blacklist = Collections.synchronizedSet(new TreeSet<String>());
        this.routeTable = new RouteTableImpl(this);
        this.storage = new StorageImpl();

        localAddress = null;
        try
        {
            ObjectInputStream ois;
            File keyFile = new File(NODE_PERSISTENCE_PATH + File.separator + userId + File.separator + ".keys");
            File authIdFile = new File(NODE_PERSISTENCE_PATH + File.separator + userId + File.separator + ".id");
            File stateFile = new File(NODE_PERSISTENCE_PATH + File.separator + userId + File.separator + ".state");
            File csKeyFile = new File(CS_KEY_PATH + File.separator + "cs.key");

            //Load CS public key
            ois = new ObjectInputStream(new FileInputStream(csKeyFile));
            PublicKey cspk = (PublicKey) ois.readObject();
            this.securityAgent.setCAPublicKey(cspk);

            //Load RSA key pair
            ois = new ObjectInputStream(new FileInputStream(keyFile));
            KeyPair kp = (KeyPair) ois.readObject();
            this.securityAgent.setKeyPair(kp);

            //Load AuthNodeId
            ois = new ObjectInputStream(new FileInputStream(authIdFile));
            AuthNodeId id = (AuthNodeId) ois.readObject();
            this.securityAgent.setAuthNodeId(id);
            this.nodeId = id.getContent().getNodeId();

            //Load node state
            try
            {
                ois = new ObjectInputStream(new FileInputStream(stateFile));
                Node n = (Node) ois.readObject();
                if (n.getStorage() != null && n.getRouteTable() != null)
                {
                    this.storage = n.getStorage();
                    this.routeTable.fill(n.getRouteTable().getAllBucketContacts());
                    port = n.getAddress().getPort();
                }
                else
                {
                    System.err.println("Can't load node state");
                }
            }
            catch (IOException ioe)
            {
                System.err.println("Can't load node state");
            }
        }
        catch (ClassNotFoundException cnfe)
        {
            throw new IOException("Corrupted data: can't load node from file");
        }

        boolean addressOk = false;
        int inc = 0;
        while (!addressOk)
        {
            try
            {
                localAddress = new InetSocketAddress(InetAddress.getLocalHost(), port + inc);
            }
            catch (UnknownHostException e)
            {
                localAddress = new InetSocketAddress("localhost", port + inc);
            }
            try
            {
                this.messageDispatcher = new MessageDispatcher(this, localAddress);
                addressOk = true;
            }
            catch (SocketException se)
            {
                System.err.println("Requested port (" + port + ") is not available for node " + nodeId);
                inc++;
            }
        }

        this.alive = true;
    }

    public void init() throws IOException
    {
        startup();
        bootstrap();
    }

    /**
     * Startup procedure is the first step to start a Likir node Initialize node
     * managers. If this node has not yet an AuthId, send a proper request to
     * the CA
     *
     * @throws IOException if startup process fails in contacting the CA
     */
    public void startup() throws IOException
    {
        System.out.println(userId + ": Startup Begin...");
        startTime = System.currentTimeMillis();
        executor.execute(messageDispatcher);
        messageFactory.setStartTime(startTime);
        if (securityAgent.getAuthNodeId() == null)
        {
            initializator.startup();
        }

        if (securityAgent.getKeyPair() == null)
        {
            throw new IOException("Unable to retrieve KeyPair!");
        }

        if (routeTable == null)
        {
            this.routeTable = new RouteTableImpl(this);
        }

        //storageCleaner.start();
        //routeTableRefresher.start();
        System.out.println(userId + ": Startup End...");
    }

    /**
     * Startup procedure is the first step to start a Likir node Initialize node
     * managers. If this node has not yet an AuthId, send a proper request to
     * the CA
     *
     * @throws IOException if startup process fails in contacting the CA
     */
    public void startup2() throws IOException
    {
        System.out.println(userId + ": Startup Begin...");
        startTime = System.currentTimeMillis();
        executor.execute(messageDispatcher);
        messageFactory.setStartTime(startTime);

        if (routeTable.size() < 3)
        {
            initializator.startup2();
        }

        storageCleaner.start();
        routeTableRefresher.start();

        System.out.println(userId + ": Startup End...");
    }

    /**
     * Kademlia bootstrap procedure. This is the second step to start a Likir
     * node and it must be executed AFTER the startup procedure
     *
     * @return true if bootstrap was successful
     * @throws IOException if an error occurs during the bootstrap phase
     */
    public boolean bootstrap() throws IOException
    {
        return initializator.bootstrap();
    }

    //------------ Get and set methods ---------------
    /**
     * Whether or not this Node is connected to the Likir network
     *
     * @return true if this Node is alive
     */
    public boolean isAlive()
    {
        return alive;
    }

    /**
     * Returns the NodeId of this Node
     *
     * @return the NodeId
     */
    public NodeId getNodeId()
    {
        return nodeId;
    }

    /**
     * Sets the NodeId of this Node
     *
     * @param nodeId the NodeId
     */
    public void setNodeId(NodeId nodeId)
    {
        this.nodeId = nodeId;
    }

    /**
     * Returns the SocketAddress of this Node
     *
     * @return the SocketAddress
     */
    public InetSocketAddress getAddress()
    {
        return this.localAddress;
    }

    /**
     * Returns the userId of this node
     *
     * @return theuserId
     */
    public String getUserId()
    {
        return userId;
    }

    /**
     * Returns the route table of this node
     *
     * @return the route table
     */
    public RouteTable getRouteTable()
    {
        return routeTable;
    }

    /**
     * Returns the storage of this node
     *
     * @return the storage
     */
    public Storage getStorage()
    {
        return storage;
    }

    /**
     * Returns the time (in milliseconds) of the node initialization
     *
     * @return time of node initialization
     */
    public long getStartTime()
    {
        return startTime;
    }

    /**
     * Returns the message dispatcher of this node
     *
     * @return the message dispatcher
     */
    public MessageDispatcher getMessageDispatcher()
    {
        return messageDispatcher;
    }

    /**
     * Returns the message factory of this node
     *
     * @return the message factory
     */
    public RPCMessageFactory getMessageFactory()
    {
        return messageFactory;
    }

    /**
     * Returns the object which manages the execution of I/O managers
     *
     * @return the thread executor
     */
    public ExecutorService getExecutor()
    {
        return executor;
    }

    /**
     * Returns the object which manages the periodical execution of inner
     * managers
     *
     * @return the thread executor
     */
    public ScheduledExecutorService getInnerExecutor()
    {
        return innerExecutor;
    }

    /**
     * Returns the security agent of this Node
     *
     * @return the security agent
     */
    public NodeSecurityAgent getSecurityAgent()
    {
        return securityAgent;
    }

    /**
     * Sets the Security Agent of this Node
     *
     * @param securityAgent the security agent
     */
    public void setSecurityAgent(NodeSecurityAgent securityAgent)
    {
        this.securityAgent = securityAgent;
    }

    /**
     * Returns the storage cleaner of this Node
     *
     * @return the storage cleaner
     */
    public StorageCleaner getStorageCleaner()
    {
        return storageCleaner;
    }

    /**
     * Returns the entry factory of this Node
     *
     * @return the entry factory
     */
    public EntryFactory getEntryFactory()
    {
        return entryFactory;
    }

    /**
     * Returns the blacklist of this Node. The blacklist contains the userIds of
     * all the Nodes that was marked as 'untrustworthy' by the user through
     * blacklist(userId) method
     *
     * @return
     */
    public Set<String> getBlacklist()
    {
        return blacklist;
    }

    /**
     * Adds the specified userId to this Node blacklist. The blacklist contains
     * the userIds of all the Nodes that are considered 'untrustworthy'. No
     * message sent from a blacklisted user will be accepted
     *
     * @param user the userId to be blacklisted
     */
    public void blacklist(String user)
    {
        blacklist.add(user);
    }

    /* ------------------- Immediate operations --------------------- */
    /**
     * Sends an asynchronous PING message to the specified NodeId If the
     * specified addressee is unknown by the route table, this method will
     * return an IOException
     *
     * @param addressee the NodeId of the addressee
     * @return the future response message
     * @throws IOException if addressee is unknown or if there is an I/O problem
     * in sending message
     */
    public ObservableFuture<RPCMessage> ping(NodeId addressee) throws IOException
    {
        PingManager task = new PingManager(this, addressee);
        executor.execute(task);
        return task;
    }

    /**
     * Sends an asynchronous PING message to the specified Contact
     *
     * @param addressee the Contact of the addressee
     * @return the future response message
     * @throws IOException if there is an I/O problem in sending message
     */
    public ObservableFuture<RPCMessage> ping(Contact addressee) throws IOException
    {
        PingManager task = new PingManager(this, addressee);
        executor.execute(task);
        return task;
    }

    /**
     * Sends an asynchronous FIND_NODE message to the specified NodeId If the
     * specified addressee is unknown by the route table, this method will
     * return an IOException
     *
     * @param addressee the NodeId of the addressee
     * @param key the lookup key
     * @return the future response message
     * @throws IOException if addressee is unknown or if there is an I/O problem
     * in sending message
     */
    public ObservableFuture<RPCMessage> findNode(NodeId addressee, NodeId key) throws IOException
    {
        FindNodeManager task = new FindNodeManager(this, addressee, key);
        executor.execute(task);
        return task;
    }

    /**
     * Sends an asynchronous FIND_NODE message to the specified Contact
     *
     * @param addressee the Contact of the addressee
     * @param key the lookup key
     * @return the future response message
     * @throws IOException if there is an I/O problem in sending message
     */
    public ObservableFuture<RPCMessage> findNode(Contact addressee, NodeId key) throws IOException
    {
        FindNodeManager task = new FindNodeManager(this, addressee, key);
        executor.execute(task);
        return task;
    }

    /**
     * Sends an asynchronous FIND_VALUE message to the specified NodeId The
     * "owner" parameter is a String which instructs the receiving node to
     * include in the message response only the contents originally submitted by
     * "owner". The "recent" parameter instructs the receiving node whether or
     * not include only the most recent versions of the contents saved in its
     * storage. If the specified addressee is unknown by the route table, this
     * method will return an IOException.
     *
     * @param addressee the NodeId of the addressee
     * @param key the search key
     * @param owner index-side filtering parameter
     * @param recent index-side filtering parameter
     * @return the future response message
     * @throws IOException if addressee is unknown or if there is an I/O problem
     * in sending message
     */
    public ObservableFuture<RPCMessage> findValue(NodeId addressee, NodeId key, String type, String owner, boolean recent) throws IOException
    {
        if (owner != null)
        {
            owner = owner.toLowerCase();
        }
        FindValueManager task = new FindValueManager(this, addressee, key, type, owner, recent);
        executor.execute(task);
        return task;
    }

    /**
     * Sends an asynchronous FIND_VALUE message to the specified Contact The
     * "owner" parameter is a String which instructs the receiving node to
     * include in the message response only the contents originally submitted by
     * "owner". The "recent" parameter instructs the receiving node whether or
     * not include only the most recent versions of the contents saved in its
     * storage.
     *
     * @param addressee the Contact of the addressee
     * @param key the search key
     * @param type the content type (or the identifier of the application)
     * @param owner index-side filtering parameter
     * @param recent index-side filtering parameter
     * @param sizeonly TODO
     * @return the future response message
     * @throws IOException if there is an I/O problem in sending message
     */
    public ObservableFuture<RPCMessage> findValue(Contact addressee, NodeId key, String type, String owner, boolean recent) throws IOException
    {
        if (owner != null)
        {
            owner = owner.toLowerCase();
        }
        FindValueManager task = new FindValueManager(this, addressee, key, type, owner, recent);
        executor.execute(task);
        return task;
    }

    public ObservableFuture<RPCMessage> findValue(Contact addressee, NodeId key, String type, String owner, boolean recent, boolean countersOnly) throws IOException
    {
        if (owner != null)
        {
            owner = owner.toLowerCase();
        }
        FindValueManager task = new FindValueManager(this, addressee, key, type, owner, recent, countersOnly);
        executor.execute(task);
        return task;
    }

    /**
     * Sends an asynchronous STORE message to the specified NodeId. Builds a new
     * StorageEntry containing credentials related to this Node and to the
     * specified content; then sends a store message to the specified replica
     * node. The invoker must specify the key which tags the content and a Time
     * To Live: after the expiration of the TTL the entry could be erased by the
     * replica node If the specified addressee is unknown by the route table,
     * this method will return an IOException.
     *
     * @param addressee the NodeId of the addressee
     * @param key the key associated to content
     * @param content the content to be stored
     * @param type the content type (or the identifier of the application)
     * @param ttl the time to live of the content (milliseconds)
     * @return the future response message
     * @throws IOException if addressee is unknown or if there is an I/O problem
     * in sending message
     * @throws IllegalArgumentException if the content size exceeds the maximum
     * size allowed (64K)
     */
    public ObservableFuture<RPCMessage> store(NodeId addressee, NodeId key, byte[] content, String type, long ttl) throws IOException
    {
        if (content.length > MAX_CONTENT_SIZE)
        {
            throw new IllegalArgumentException("Content exceeds the maximum size allowed");
        }
        StoreManager task = new StoreManager(this, addressee, key, content, type, ttl);
        executor.execute(task);
        return task;
    }

    /**
     * Sends an asynchronous STORE message to the specified Contact. Builds a
     * new StorageEntry containing credentials related to this Node and to the
     * specified content; then sends a store message to the specified replica
     * node. The invoker must specify the key which tags the content and a Time
     * To Live: after the expiration of the TTL the entry could be erased by the
     * replica node
     *
     * @param addressee the Contact of the addressee
     * @param key the key associated to content
     * @param content the content to be stored
     * @param type the content type (or the identifier of the application)
     * @param ttl the time to live of the content (milliseconds)
     * @return the future response message
     * @throws IOException if there is an I/O problem in sending message
     * @throws IllegalArgumentException if the content size exceeds the maximum
     * size allowed (64K)
     */
    public ObservableFuture<RPCMessage> store(Contact addressee, NodeId key, byte[] content, String type, long ttl) throws IOException
    {
        if (content.length > MAX_CONTENT_SIZE)
        {
            throw new IllegalArgumentException("Content exceeds the maximum size allowed");
        }
        StoreManager task = new StoreManager(this, addressee, key, content, type, ttl);
        executor.execute(task);
        return task;
    }

    /**
     * Sends an asynchronous STORE message to the specified Contact. Use the
     * specified StorageEntry to compose the store message; used by replica
     * nodes to spread the stored contents across the DHT
     *
     * @param addressee the Contact of the addressee
     * @param entry the storage entry
     * @return the future response message
     * @throws IOException if there is an I/O problem in sending message
     * @throws IllegalArgumentException if the storage entry exceeds the maximum
     * size allowed (64K)
     */
    public ObservableFuture<RPCMessage> store(Contact addressee, StorageEntry entry) throws IOException
    {
        if (entry.getContent().size() > MAX_CONTENT_SIZE)
        {
            throw new IllegalArgumentException("Content exceeds the maximum size allowed");
        }
        StoreManager task = new StoreManager(this, addressee, entry);
        executor.execute(task);
        return task;
    }

    /**
     * TODO
     *
     * @param addressee
     * @param entries
     * @return
     * @throws IOException
     */
    public ObservableFuture<RPCMessage> store(Contact addressee, StorageEntry[] entries) throws IOException
    {
        int totalSize = 0;
        for (StorageEntry e : entries)
        {
            totalSize += e.getContent().size();
            if (totalSize > MAX_CONTENT_SIZE)
            {
                throw new IllegalArgumentException("Content exceeds the maximum size allowed");
            }
        }

        StoreManager task = new StoreManager(this, addressee, entries);
        executor.execute(task);
        return task;
    }

    /* ------------------- Iterative operations --------------------- */
    /**
     * Starts the Kademlia lookup procedure for the specified key using the
     * FIND_NODE RPC Returns the K Contacts found nearest to key
     *
     * @param key the lookup key
     * @returns a Future Collection of the found Contacts
     */
    public synchronized ObservableFuture<Collection<Contact>> lookup(NodeId key)
    {
        ObservableFuture<Collection<Contact>> task = new LookupManager(this, key);
        executor.execute(task);
        return task;
    }

    /**
     * Starts the Kademlia lookup procedure for the specified key using the
     * FIND_VALUE RPC. Returns the values, tagged with key, saved in the
     * storages reached by the FIND_VALUES messages. The "owner" parameter is a
     * String which instructs the receiving node to include in the message
     * response only the contents originally submitted by "owner". The "recent"
     * parameter instructs the receiving node whether or not include only the
     * most recent versions of the contents saved in its storage. The
     * contentNumber parameter specifies the maximum number of entries required
     * to be returned by this method (e.g. if this parameter is set to 1 the
     * lookup procedure will be stopped when a round of FIND_VALUES retrieves
     * the first value).
     *
     * @param key the search key
     * @param type the content type (or the identifier of the application)
     * @param recent index-side filtering parameter
     * @return a Future Collection of the found entries
     */
    public synchronized ObservableFuture<Collection<StorageEntry>> get(NodeId key, String type, String owner, boolean recent, int contentNumber)
    {
        if (owner != null)
        {
            owner = owner.toLowerCase();
        }
        ObservableFuture<Collection<StorageEntry>> task = new GetManager(this, key, type, owner, recent, contentNumber);
        executor.execute(task);
        return task;
    }

    /**
     * Starts the Kademlia lookup procedure for the specified key using the
     * FIND_VALUE RPC. The keys specified in the FIND_VALUE RPCs are equal to
     * the SHA-1 hash of the provided keyWord Returns the values, tagged with
     * key, saved in the storages reached by the FIND_VALUES messages. The
     * "owner" parameter is a String which instructs the receiving node to
     * include in the message response only the contents originally submitted by
     * "owner". The "recent" parameter instructs the receiving node whether or
     * not include only the most recent versions of the contents saved in its
     * storage. The contentNumber parameter specifies the maximum number of
     * entries required to be returned by this method (e.g. if this parameter is
     * set to 1 the lookup procedure will be stopped when a round of FIND_VALUES
     * retrieves the first value).
     *
     * @param keyWord a string from which the message Id will be calculated
     * @param type the content type (or the identifier of the application)
     * @param recent index-side filtering parameter
     * @return a Future Collection of the found entries throws IOException if
     * there is an problem in calculating the Kademlia ID from the provided
     * keyWord
     */
    public synchronized ObservableFuture<Collection<StorageEntry>> get(String keyWord, String type, String owner, boolean recent, int contentNumber) throws IOException
    {
        NodeId key = fileManager.hash(keyWord);
        if (owner != null)
        {
            owner = owner.toLowerCase();
        }
        ObservableFuture<Collection<StorageEntry>> task = new GetManager(this, key, type, owner, recent, contentNumber);
        executor.execute(task);
        return task;
    }

    /**
     * Starts the Kademlia lookup procedure for the specified key using the
     * FIND_VALUE RPC. The keys specified in the FIND_VALUE RPCs are equal to
     * the SHA-1 hash of the provided keyWord Returns a multiset of pair
     * (String,Integer) with a meaning which depends on the filtering parameters
     * in input: (Keyword) -> (type,#contents of that type) (Keyword,type) ->
     * (owner,#contents of the specified type belonging to owner)
     * (Keyword,owner)-> (type,#contents of type 'type', belonging to the
     * specified owner) (Keyword,type,owner) -> (owner,#contents of specified
     * type belonging to the specified owner) If parameter 'recent' is set only
     * the most recent resources are taken into account. The contentNumber
     * parameter specifies the maximum number of entries required to be returned
     * by this method (e.g. if this parameter is set to 1 the lookup procedure
     * will be stopped when a round of FIND_VALUES retrieves the first value).
     *
     * @param keyWord a string from which the message Id will be calculated
     * @param type the content type (or the identifier of the application)
     * @param recent index-side filtering parameter
     * @return a Future Collection of the found entries throws IOException if
     * there is an problem in calculating the Kademlia ID from the provided
     * keyWord
     */
    public synchronized ObservableFuture<LinkedList<HashMap<String, Integer>>> getCounters(String keyWord, String type, String owner, boolean recent, int contentNumber) throws IOException
    {
        NodeId key = fileManager.hash(keyWord);
        if (owner != null)
        {
            owner = owner.toLowerCase();
        }
        ObservableFuture<LinkedList<HashMap<String, Integer>>> task = new UnsignedGetManager(this, key, type, owner, recent, contentNumber);
        executor.execute(task);
        return task;
    }

    /**
     * Starts the Kademlia lookup procedure for the specified key using the
     * FIND_NODE RPC; then builds a new StorageEntry containing credentials
     * related to this Node and to the specified content and sends a STORE
     * message to each Node retrieved in the lookup procedure. The invoker must
     * specify the key which tags the content and a Time To Live: after the
     * expiration of the TTL the entry could be erased by the replica node.
     * Returns the number of successful store RPCs
     *
     * @param key the key associated to content
     * @param type the content type (or the identifier of the application)
     * @param content the content to be stored
     * @param ttl the time to live of the content (milliseconds)
     * @return the future result
     * @throws IllegalArgumentException if the content size exceeds the maximum
     * size allowed (64000 Bytes)
     */
    public synchronized ObservableFuture<Integer> put(NodeId key, byte[] content, String type, long ttl)
    {
        if (content.length > MAX_CONTENT_SIZE)
        {
            throw new IllegalArgumentException("Content exceeds the maximum size allowed");
        }
        ObservableFuture<Integer> task = new PutManager(this, key, type, content, ttl);
        executor.execute(task);
        return task;
    }

    /**
     * Starts the Kademlia lookup procedure for the specified key using the
     * FIND_NODE RPC; then builds a new StorageEntry containing credentials
     * related to this Node and to the specified file and sends a STORE message
     * to each Node retrieved in the lookup procedure. The keys specified in the
     * STORE RPCs are equal to the SHA-1 hash of the provided keyWord The
     * invoker must specify a Time To Live: after the expiration of the TTL the
     * entry could be erased by the replica node. Returns the number of
     * successful store RPCs If an error occurs while hashing the keyword it
     * returns an IOException
     *
     * @param keyWord a string from which the store key will be calculated
     * @param type the content type (or the identifier of the application)
     * @param content the content to be stored
     * @param ttl the time to live of the content (milliseconds)
     * @return the future result
     * @throws IOException if there is an problem in calculating the Kademlia ID
     * from the provided keyWord
     * @throws IllegalArgumentException if the content size exceeds the maximum
     * size allowed (64000 Bytes)
     */
    public synchronized ObservableFuture<Integer> put(String keyWord, byte[] content, String type, long ttl) throws IOException
    {
        if (content.length > MAX_CONTENT_SIZE)
        {
            throw new IllegalArgumentException("Content exceeds the maximum size allowed");
        }
        NodeId key = fileManager.hash(keyWord);
        ObservableFuture<Integer> task = new PutManager(this, key, type, content, ttl);
        executor.execute(task);
        return task;
    }

    /**
     * Starts the Kademlia lookup procedure for the specified key using the
     * FIND_NODE RPC; then stores the contents specified in input to the
     * retrieved nodes. Here, multiple contents can be specified. Each content
     * is signed accordingly to the Likir protocol.
     *
     * @param keyWord a string from which the store key will be calculated
     * @param contents the contents to be stored
     * @param type the contents type (or the identifier of the application)
     * @param ttl the time to live of the contents (milliseconds)
     * @return the future result
     * @throws IOException if there is an problem in calculating the Kademlia ID
     * from the provided keyWord
     * @throws IllegalArgumentException if the content size exceeds the maximum
     * size allowed (64000 Bytes)
     */
    public synchronized ObservableFuture<Integer> put(String keyWord, byte[][] contents, String[] types, long[] ttls) throws IOException
    {
        int totalLength = 0;
        for (byte[] content : contents)
        {
            totalLength += content.length;
            if (totalLength > MAX_CONTENT_SIZE)
            {
                throw new IllegalArgumentException("Content exceeds the maximum size allowed");
            }
        }
        NodeId key = fileManager.hash(keyWord);
        ObservableFuture<Integer> task = new PutManager(this, key, types, contents, ttls, true);
        executor.execute(task);
        return task;
    }

    /**
     * Starts the Kademlia lookup procedure for the specified key using the
     * FIND_NODE RPC; then builds a new StorageEntry containing credentials
     * related to this Node and to the specified file and sends a STORE message
     * to each Node retrieved in the lookup procedure. The keys specified in the
     * STORE RPCs are equal to the SHA-1 hash of the filename. The invoker must
     * specify a Time To Live: after the expiration of the TTL the entry could
     * be erased by the replica node. Returns the number of successful store
     * RPCs If an error occurs while hashing the filename it returns an
     * IOException
     *
     * @param file the content to be stored
     * @param type the content type (or the identifier of the application)
     * @param ttl the time to live of the content (milliseconds)
     * @return the future result
     * @throws IOException if there is an problem in calculating the Kademlia ID
     * from the provided file
     * @throws IllegalArgumentException if the file size exceeds the maximum
     * size allowed (64000 Bytes)
     */
    public synchronized ObservableFuture<Integer> put(File file, String type, long ttl) throws IOException
    {
        if (file.length() > MAX_CONTENT_SIZE)
        {
            throw new IllegalArgumentException("Content exceeds the maximum size allowed");
        }
        NodeId key = fileManager.hash(file);
        byte[] content = fileManager.toBytes(file);
        ObservableFuture<Integer> task = new PutManager(this, key, type, content, ttl);
        executor.execute(task);
        return task;
    }

    /**
     * Starts the Kademlia lookup procedure for the specified key using the
     * FIND_NODE RPC; then stores the contents specified in input to the
     * retrieved nodes. Here, multiple contents can be specified. The contents
     * are packaged in a StorageEntry but they are not signed individually. When
     * the StorageEntry is received by the index nodes it is unpacked and the
     * unsigned contents are stored. Since they are not signed, the stored
     * contents cannot be retrieved (they fail Likir security test), but they
     * can be useful when the getCounter primitive is in use.
     *
     * @param keyWord a string from which the store key will be calculated
     * @param contents the contents to be stored
     * @param type the contents type (or the identifier of the application)
     * @param ttl the time to live of the contents (milliseconds)
     * @return the future result
     * @throws IOException if there is an problem in calculating the Kademlia ID
     * from the provided keyWord
     * @throws IllegalArgumentException if the content size exceeds the maximum
     * size allowed (64000 Bytes)
     */
    public synchronized ObservableFuture<Integer> unsignedPut(String keyWord, byte[][] contents, String[] types, long[] ttls) throws IOException
    {
        int totalLength = 0;
        for (byte[] content : contents)
        {
            totalLength += content.length;
            if (totalLength > MAX_CONTENT_SIZE)
            {
                throw new IllegalArgumentException("Content exceeds the maximum size allowed");
            }
        }
        NodeId key = fileManager.hash(keyWord);
        ObservableFuture<Integer> task = new PutManager(this, key, types, contents, ttls, false);
        executor.execute(task);
        return task;
    }

    /**
     * TEST METHOD Triggers the storage cleaner process
     */
    //TODO: erase
    public synchronized void forceStorageManteinace()
    {
        executor.submit(storageCleaner);
    }

    /**
     * Cause this Node shutdown
     *
     * @param save if set the state of the node will be saved
     */
    public void exit(boolean save)
    {
        if (save)
        {
            try
            {
                saveState();
            }
            catch (IOException ioe)
            {
                System.err.println("Can't save Node state");
                ioe.printStackTrace();
            }
        }
        messageDispatcher.close();
        executor.shutdownNow();
        innerExecutor.shutdownNow();
        alive = false;
    }

    private void saveState() throws IOException
    {
        String path = NODE_PERSISTENCE_PATH;
        File nodeDir = new File(path);
        if (!nodeDir.exists())
        {
            nodeDir.mkdir();
        }
        path = path + File.separator + userId + File.separator + ".state";
        File nodeFile = new File(path);
        nodeFile.createNewFile();

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(nodeFile));
        oos.writeObject(this);
    }

    /**
     * Returns a String representation of this Node
     *
     * @return a string representation of this object
     */
    public String toString()
    {
        return "UserId=" + userId + " - NodeId=" + nodeId + " - Address=" + this.messageDispatcher.getLocalAddress();
    }

}

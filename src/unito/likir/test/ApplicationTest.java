package unito.likir.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import unito.likir.Application;
import unito.likir.Environment;
import unito.likir.EnvironmentImpl;
import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.io.FutureObserver;
import unito.likir.io.ObservableFuture;
import unito.likir.storage.StorageEntry;

/**
 * Tutorial about: - how to create a small Likir test network - how to register
 * an application to a Kademlia node - how to call node's primitives in blocking
 * or non-blocking mode - how to save and resume a network
 *
 * @author Luca Maria Aiello
 * @version 0.1
 */
public class ApplicationTest
{

    public static InputStreamReader inReader = new InputStreamReader(System.in);
    public static BufferedReader bufReader = new BufferedReader(inReader);

    public static void main(String... args)
    {
        int nodeNumber = 3; //number of nodes in the network
        int initialPort = 8000; //initial UDP port
        String userNamePrefix = "TestUser"; //userId common prefix

        /*
         * PART I : Network initialization
         */
        //Create the environment and initialize all the node
        EnvironmentImpl env = new EnvironmentImpl();
        env.registerNodes(nodeNumber, userNamePrefix, initialPort); //Nodes are registered to the Environment
        System.out.println("Registered Nodes: " + env.getAllNodes());

        System.out.println("******************* Starting Nodes *******************************");
        env.startupAll(); //CA is contacted by every Node

        //  !!! The CA could be shut down, old nodes don't need to contact it anymore !!!
        System.out.println("******************* Bootstrapping Nodes *******************************");
        env.bootstrapAll(); //Kademlia bootstrap procedure is completed by every Node 

        //Build a new application upon the environment.
        //The use of Application class is not strictly necessary. It is just used to provide 
        //a useful handle to a particular node, in order to differentiate it from the other
        //nodes of the network.
        Application appl = new Application(env);
        //register the application to a new node (the new node will be added to the environment)
        Node n = appl.registerNode("ApplicationNode", 10000);
        System.out.println("Registered Nodes: " + env.getAllNodes());
        //initialize the new node
        try
        {
            n.init(); //the new node is initialized (startup + bootstrap)
        }
        catch (IOException ioe)
        {
            System.err.println("Error in node initialization");
            System.exit(0);
        }
        //Method getNode() retrieves the new Node
        n = appl.getNode();

        //Print the Nodes of the environment
        System.out.println(env);

        /*
         * PART II : RPCs
         */
        //----Blocking RPC call
        try
        {
            System.out.println("Node \"" + n.getUserId() + "\" stores a content with the key " + NodeId.MAXIMUM);
            //if get() is called on the result of an RPC, the process is blocked for a network I/O request 
            int replica = n.put(NodeId.MAXIMUM, new byte[6000], "ApplType", 3600000).get();
            System.out.println("CONTENT STORED AT " + replica + " REPLICAS");

            //print the Storages: K nodes are keeping the content in their stores
            System.out.println("STORAGES");
            for (Node i : env.getAllNodes())
            {
                System.out.println(i.getStorage());
            }

            //Select another node to perform a findValue operation
            n = env.selectNode(userNamePrefix + "7");
            System.out.println("Node \"" + n.getUserId() + "\" searches for a content with the key " + NodeId.MAXIMUM + "(blocking mode)");
            Collection<StorageEntry> results = n.get(NodeId.MAXIMUM, null, null, false, nodeNumber).get();

            if (results.size() == 0)
            {
                System.out.println("NO CONTENT FOUND!");
            }
            else
            {
                System.out.println("CONTENTS FOUND:");
                for (StorageEntry e : results) //print the found values
                {
                    System.out.println("--> " + e);
                }
            }
        }
        catch (InterruptedException ie)
        {
            System.err.println("Interrupted");
        }
        catch (ExecutionException e)
        {
            System.err.println("ExecutionException");
        }

        //----Non-blocking RPC call
        System.out.println("Node \"" + n.getUserId() + "\" searches for a content with the key " + NodeId.MAXIMUM + "(blocking mode)");
        //Retrieves the result of a findValue via observer.
        //The definition of the observer class is application-dependent. Here a simple stub is used.
        FutureObserver<Collection<StorageEntry>> observer = new FindNodeObserverStub();
        ObservableFuture<Collection<StorageEntry>> result = n.get(NodeId.MAXIMUM, null, null, false, 10);
        result.addObserver(observer);

        /*
         * PART III : Shutdown & restart
         */
        //requires confirm to go on
        System.out.println("Press a button to CONTINUE (you could also try to shutdown the CA...)");
        try
        {
            bufReader.readLine();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        //unregisters the Application from its node
        //(it simply put NULL in appl.node, the node remains active)
        appl.unregister();

        //shutdown every Node registered to the Environment, saving their states to disk
        env.shutDownAll(true);

        //reload all the saved nodes in the environment
        env.loadNetwork();
        env.startupAll();
        env.bootstrapAll();

        //retrieves the application node by its name
        n = env.selectNode("ApplicationNode");

        //print the Storages: stored contents are maintained
        for (Node x : env.getAllNodes())
        {
            System.out.println(x.getStorage());
        }

        //requires confirm to shutdown environment
        System.out.println("Press a button to EXIT");
        try
        {
            bufReader.readLine();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        //shutdown without save
        env.shutDownAll(true);
    }
}

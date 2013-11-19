package unito.likir;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import unito.likir.settings.PropFinder;
import unito.likir.settings.Settings;

/**
 * A simple Environment implementation
 *
 * @author Aiello Luca Maria
 * @version 0.1
 */
public class EnvironmentImpl implements Environment
{

    private ArrayList<Node> registeredNodes;
    private final String NODE_PERSISTENCE_PATH;

    /**
     * Builds a new Environment
     */
    public EnvironmentImpl()
    {
        registeredNodes = new ArrayList<Node>();
        this.NODE_PERSISTENCE_PATH = PropFinder.get(Settings.NODE_PERSISTENCE_PATH);
    }

    public List<Node> getAllNodes()
    {
        return registeredNodes;
    }

    public int size()
    {
        return registeredNodes.size();
    }

    public void registerNode(Node newNode)
    {
        if (newNode == null)
        {
            throw new NullPointerException();
        }

        registeredNodes.add(newNode);
    }

    public Node registerNode(String userId)
    {
        //Checks if the specified userId is already used
        if (selectNode(userId) != null)
        {
            throw new IllegalArgumentException("The specified userId is already registered");
        }

        Node n = new Node(userId);
        registeredNodes.add(n);
        return n;
    }

    public Node registerNode(String userId, int port)
    {
        //Checks if the specified userId is already used
        if (selectNode(userId) != null)
        {
            throw new IllegalArgumentException("The specified userId is already registered");
        }

        Node n = new Node(userId, port);
        registeredNodes.add(n);
        return n;
    }

    public void registerNodes(int nodeNumber, String userIdPrefix, int initialPort)
    {
        for (int i = 0; i < nodeNumber; i++)
        {
            registerNode(userIdPrefix + i, initialPort + i);
        }
    }

    public Node selectNode(int index)
    {
        return registeredNodes.get(index);
    }

    public Node selectNode(String userId)
    {
        String id = userId.toLowerCase();
        for (Node n : registeredNodes)
        {
            if ((n.getUserId().toLowerCase()).equals(id))
            {
                return n;
            }
        }
        return null;
    }

    public Node selectNode(NodeId nodeId)
    {
        for (Node n : registeredNodes)
        {
            if (n.getNodeId().equals(nodeId))
            {
                return n;
            }
        }
        return null;
    }

    public void unregisterNode(String userId)
    {
        registeredNodes.remove(selectNode(userId));
    }

    public void loadNetwork()
    {
        String path = NODE_PERSISTENCE_PATH;
        File dir = new File(path);
        String[] fileNames = dir.list();

        Node n = null;
        int nodeCounter = 0;
        for (int i = 0; i < fileNames.length; i++)
        {
            try
            {
                File nodeFile = new File(NODE_PERSISTENCE_PATH + File.separator + fileNames[i]);
                n = new Node(nodeFile);

                registerNode(n);
                nodeCounter++;
            }
            catch (Exception e)
            {
                System.err.println(e);
            }
        }
    }

    public void initAll()
    {
        for (Node n : registeredNodes)
        {
            try
            {
                n.init();
            }
            catch (IOException ioe)
            {
                registeredNodes.remove(n);
                System.err.println("Error while initializing " + n.getUserId() + " - node has been removed");
            }
        }
    }

    public void startupAll()
    {
        Iterator<Node> it = registeredNodes.iterator();
        while (it.hasNext())
        {
            Node n = it.next();
            try
            {
                n.startup();
            }
            catch (IOException ioe)
            {
                //it.remove();
                System.err.println("Error while startupping " + n.getUserId() + " - node has been removed");
            }
        }
    }

    public void bootstrapAll()
    {
        Iterator<Node> it = registeredNodes.iterator();
        while (it.hasNext())
        {
            Node n = it.next();
            try
            {
                n.bootstrap();
            }
            catch (IOException ioe)
            {
                //it.remove();
                System.err.println("Error while bootstrapping " + n.getUserId() + " - node has been removed");
            }
        }
    }

    public void shutDownAll(boolean save)
    {
        for (Node n : registeredNodes)
        {
            n.exit(save);
        }
        registeredNodes.clear();
    }

    public String toString()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append("ENVIRONMENT - Registered Nodes\n");

        int i = 0;
        for (Node n : registeredNodes)
        {
            i++;
            buffer.append(i + "- " + n + "\n");
        }
        return buffer.toString();
    }
}

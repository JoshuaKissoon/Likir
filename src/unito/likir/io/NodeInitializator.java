package unito.likir.io;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Collection;
import java.util.LinkedList;

import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.messages.ca.CAErrorMessage;
import unito.likir.messages.ca.CAKeyResponse;
import unito.likir.messages.ca.CAMessage;
import unito.likir.messages.ca.InitResponse;
import unito.likir.messages.dht.RPCMessageFactory;
import unito.likir.routing.Contact;
import unito.likir.security.AuthNodeId;
import unito.likir.security.BootstrapList;
import unito.likir.security.NodeSecurityAgent;
import unito.likir.settings.PropFinder;
import unito.likir.settings.Settings;

public class NodeInitializator
{

    private Node node;
    private Collection<Contact> bootstrapList;
    private final int CA_PORT;
    private final String CA_IP;

    public NodeInitializator(Node node)
    {
        this.CA_IP = PropFinder.get(Settings.DEFAULT_CA_IP);
        this.CA_PORT = Integer.parseInt(PropFinder.get(Settings.DEFAULT_CA_PORT));
        this.node = node;
        this.bootstrapList = new LinkedList<Contact>();
    }

    /**
     *
     */
    public boolean startup() throws IOException
    {
        String userId = node.getUserId();
        RPCMessageFactory messageFactory = node.getMessageFactory();
        NodeSecurityAgent securityAgent = node.getSecurityAgent();
        StringBuilder buffer = new StringBuilder();

        buffer.append("\nStarting initialization protocol");
        try
        {
            //create the node key pair
            buffer.append("\n   " + userId + ": Creating a new Key pair...");
            securityAgent.initKeyPair();
            buffer.append("\n   " + userId + ": Public Key = " + securityAgent.getPublicKey());

            //create a connection to the CA
            buffer.append("\n   " + userId + ": connecting to CA...");
            System.out.println(CA_IP);
            System.out.println(CA_PORT);
            Socket socket = new Socket(CA_IP, CA_PORT);
            //socket.setSoTimeout(timeout);
            buffer.append("\n   " + userId + ": connected to CA!");
            CAMessage request;
            CAMessage response;
            ObjectOutputStream objOut;
            ObjectInputStream objIn;

            // create a request for the CA publicKey and send it to the CA
            buffer.append("\n   " + userId + ": creating key request");
            request = messageFactory.createCAKeyRequest(node.getUserId());
            objOut = new ObjectOutputStream(socket.getOutputStream());
            objOut.writeObject(request);

            //waits for a reply
            buffer.append("\n   " + userId + ": waiting for key response");
            objIn = new ObjectInputStream(socket.getInputStream());
            response = (CAMessage) objIn.readObject();
            if (response.getOpCode().equals(unito.likir.messages.ca.CAMessage.OpCode.ERROR))
            {
                throw new IOException();
            }
            else if (response.getOpCode().equals(unito.likir.messages.ca.CAMessage.OpCode.CA_KEY_RESPONSE))
            {
                CAKeyResponse resp = (CAKeyResponse) response;
                buffer.append("\n   " + userId + ": CA Key achieved!");
                securityAgent.setCAPublicKey(resp.getCAKey());
            }
            else
            {
                throw new IOException();
            }

            //create a connection with CA
            buffer.append("\n   " + userId + ": connecting to CA...");
            socket = new Socket(CA_IP, CA_PORT);

            // create an initialization request and send it to the CA
            buffer.append("\n   " + userId + ": create init request");
            request = messageFactory.createInitRequest(securityAgent.getPublicKey(), null);
            objOut = new ObjectOutputStream(socket.getOutputStream());
            objOut.writeObject(request);

            //waits for a reply
            buffer.append("\n   " + userId + ": waiting for init response");
            objIn = new ObjectInputStream(socket.getInputStream());
            response = (CAMessage) objIn.readObject();
            Collection<Contact> bootList = null;
            if (response.getOpCode().equals(unito.likir.messages.ca.CAMessage.OpCode.ERROR))
            {
                CAErrorMessage err = (CAErrorMessage) response;
                throw new IOException(err.getErrorMessage());
            }
            else if (response.getOpCode().equals(unito.likir.messages.ca.CAMessage.OpCode.INITIALIZATION_RESPONSE))
            {
                buffer.append("\n      " + response);
                InitResponse resp = (InitResponse) response;
                BootstrapList bl = resp.getBootstrapList();
                AuthNodeId ani = resp.getAuthNodeId();
                PublicKey CAPubKey = node.getSecurityAgent().getCAPublicKey();

                //verifies the validity of received information
                try
                {
                    boolean bootListValid = node.getSecurityAgent().verifySignature(bl.getContent(), bl.getSignature(), CAPubKey);
                    boolean authIdValid = node.getSecurityAgent().verifySignature(ani.getContent(), ani.getSignature(), CAPubKey);
                    if (!bootListValid || !authIdValid)
                    {
                        buffer.append("\n   Corrupted infos from CA!!!");
                        System.out.println(buffer.toString());
                        return false;
                    }
                    buffer.append("\n   AuthId and bootstrapList signatures verified!");
                }
                catch (SignatureException se)
                {
                    buffer.append("\n   Can't verify signatures of CA!!!");
                    System.out.println(buffer.toString());
                    return false;
                }

                bootList = resp.getBootstrapList().getContacts();
                securityAgent.setAuthNodeId(resp.getAuthNodeId());

                node.setNodeId(resp.getAuthNodeId().getContent().getNodeId());
            }
            else
            {
                throw new IOException();
            }
            objOut.close();
            objIn.close();
            buffer.append("\n   " + userId + ": initialization successful");
            buffer.append("\n   " + userId + ": AuthNodeId -> " + securityAgent.getAuthNodeId());
            buffer.append("\n" + userId + ":End initialization");
            System.out.println(buffer.toString());

            if (bootList != null)
            {
                this.bootstrapList = bootList;
            }

            return false;

        }
        catch (Exception cnfe)
        {
            cnfe.printStackTrace();
            throw new IOException();
        }
    }

    /**
     *
     */
    public boolean startup2() throws IOException
    {
        String userId = node.getUserId();
        RPCMessageFactory messageFactory = node.getMessageFactory();
        NodeSecurityAgent securityAgent = node.getSecurityAgent();
        StringBuilder buffer = new StringBuilder();

        buffer.append("\nStarting initialization protocol");
        try
        {
            Socket socket;
            CAMessage request;
            CAMessage response;
            ObjectOutputStream objOut;
            ObjectInputStream objIn;

            //create a connection to the CA
			/*buffer.append("\n   "+userId+": connecting to CA...");
             socket = new Socket (CA_IP, CA_PORT);
             buffer.append("\n   "+userId+": connected to CA!");
			
             // create a request for the CA publicKey and send it to the CA
             buffer.append("\n   "+userId+": creating key request");
             request = messageFactory.createCAKeyRequest(node.getUserId());
             objOut = new ObjectOutputStream(socket.getOutputStream());
             objOut.writeObject(request);
            
             //waits for a reply
             buffer.append("\n   "+userId+": waiting for key response");
             objIn = new ObjectInputStream(socket.getInputStream());
             response = (CAMessage)objIn.readObject();
             if (response.getOpCode().equals(unito.likir.messages.ca.CAMessage.OpCode.ERROR))
             {
             throw new IOException();
             }
             else if (response.getOpCode().equals(unito.likir.messages.ca.CAMessage.OpCode.CA_KEY_RESPONSE))
             {
             CAKeyResponse resp = (CAKeyResponse) response;
             buffer.append("\n   "+userId+": CA Key achieved!");
             securityAgent.setCAPublicKey(resp.getCAKey());
             }
             else
             {
             throw new IOException();
             }*/
            //create a connection with CA
            buffer.append("\n   " + userId + ": connecting to CA...");
            socket = new Socket(CA_IP, CA_PORT);

            // create an initialization request and send it to the CA
            buffer.append("\n   " + userId + ": create init request");
            request = messageFactory.createInitRequest(securityAgent.getPublicKey(), node.getNodeId());
            objOut = new ObjectOutputStream(socket.getOutputStream());
            objOut.writeObject(request);

            //waits for a reply
            buffer.append("\n   " + userId + ": waiting for init response");
            objIn = new ObjectInputStream(socket.getInputStream());
            response = (CAMessage) objIn.readObject();
            Collection<Contact> bootList = null;
            if (response.getOpCode().equals(unito.likir.messages.ca.CAMessage.OpCode.ERROR))
            {
                CAErrorMessage err = (CAErrorMessage) response;
                throw new IOException(err.getErrorMessage());
            }
            else if (response.getOpCode().equals(unito.likir.messages.ca.CAMessage.OpCode.INITIALIZATION_RESPONSE))
            {
                buffer.append("\n      " + response);
                InitResponse resp = (InitResponse) response;
                /*BootstrapList bl = resp.getBootstrapList();
                 AuthNodeId ani = resp.getAuthNodeId();
                 PublicKey CAPubKey = node.getSecurityAgent().getCAPublicKey();
            	
                 //verifies the validity of received information
                 try
                 {
                 boolean bootListValid = node.getSecurityAgent().verifySignature(bl.getContent(), bl.getSignature(), CAPubKey);
                 boolean authIdValid = node.getSecurityAgent().verifySignature(ani.getContent(), ani.getSignature(), CAPubKey);
                 if (!bootListValid || !authIdValid)
                 {
                 buffer.append("\n   Corrupted infos from CA!!!");
                 System.out.println(buffer.toString());
                 return false;
                 }
                 buffer.append("\n   AuthId and bootstrapList signatures verified!");
                 }
                 catch(SignatureException se)
                 {
                 buffer.append("\n   Can't verify signatures of CA!!!");
                 System.out.println(buffer.toString());
                 return false;
                 }*/

                bootList = resp.getBootstrapList().getContacts();
                //securityAgent.setAuthNodeId(resp.getAuthNodeId());

                //node.setNodeId(resp.getAuthNodeId().getContent().getNodeId());
            }
            else
            {
                throw new IOException();
            }
            objOut.close();
            objIn.close();
            buffer.append("\n   " + userId + ": initialization successful");
            buffer.append("\n   " + userId + ": AuthNodeId -> " + securityAgent.getAuthNodeId());
            buffer.append("\n" + userId + ":End initialization");
            System.out.println(buffer.toString());

            if (bootList != null)
            {
                this.bootstrapList = bootList;
            }

            return false;

        }
        catch (ClassNotFoundException cnfe)
        {
            throw new IOException();
        }
    }

    public boolean bootstrap()
    {
        String userId = node.getUserId();
        boolean bootstrapped = false;
        System.out.println(userId + " - Bootstrapping...");

        if (bootstrapList.isEmpty()) //CA wasn't contacted
        {
            //bootstrap list is get from formerly known contacts 
            bootstrapList = node.getRouteTable().getAllBucketContacts();
            //node.getRouteTable().clear(); //the route table will be filled with new contacts
        }

        //System.out.println("OOO " + node.getRouteTable().size());
        //for (Contact c : bootstrapList)
        //	System.out.println("----" + c);
        //while (!bootstrapList.isEmpty() && !bootstrapped)
        for (Contact c : bootstrapList)
        {
            node.getRouteTable().add(c);
            //System.out.println(">>> " + node.getRouteTable());
            try
            {
                Collection<Contact> contacts = node.lookup(node.getNodeId()).get();
                if (contacts != null && contacts.size() != 0)
                {
                    bootstrapped = true;
                    break;
                }
                else
                {
                    node.getRouteTable().remove(c);
                }
            }
            catch (Exception e)
            {
                System.out.println(userId + " -  1st Bootstrap phase attempt failed!");
                //e.printStackTrace();
            }
            finally
            {
                node.getRouteTable().remove(c);
            }
        }

        if (bootstrapped)
        {
            System.out.println(userId + " - End 1st phase Bootstrap");
            try
            {
                node.getRouteTable().resetRefreshTimes();
                Collection<NodeId> toRefresh = node.getRouteTable().getRefreshIDs(true);
                for (NodeId id : toRefresh)
                {
                    node.lookup(id).get();
                }

                System.out.println(userId + " - End 2nd phase Bootstrap");
            }
            catch (Exception e)
            {
                System.out.println(userId + " - Bootstrap failed!");
                return false;
            }

            return true;
        }
        else
        {
            System.out.println(userId + " -  Bootstrap Failed!");
            return false;
        }

    }

    public boolean bootstrap2()
    {
        String userId = node.getUserId();
        boolean bootstrapped = false;
        System.out.println(userId + ": Bootstrapping...");

        for (Contact c : bootstrapList)
        {
            node.getRouteTable().add(c);
        }

        //while (!bootstrapList.isEmpty() && !bootstrapped)
        for (Contact c : bootstrapList)
        {
            try
            {
                if (node.lookup(node.getNodeId()).get() != null)
                {
                    bootstrapped = true;
                    break;
                }
            }
            catch (Exception e)
            {
                System.out.println(userId + ": 1st Bootstrap phase attempt failed!");
                //e.printStackTrace();
            }
            finally
            {
                node.getRouteTable().remove(c);
            }
        }

        if (bootstrapped)
        {
            System.out.println(userId + ": End 1st phase Bootstrap");
        }
        else
        {
            System.out.println(userId + ": Bootstrap Failed!");
            return false;
        }

        try
        {
            node.getRouteTable().resetRefreshTimes();
            Collection<NodeId> toRefresh = node.getRouteTable().getRefreshIDs(true);
            for (NodeId id : toRefresh)
            {
                node.lookup(id);
            }

            System.out.println(userId + ": End 2nd phase Bootstrap");
        }
        catch (Exception e)
        {
            System.out.println(userId + ": Bootstrap failed!");
            return false;
        }

        return true;
    }
}

package unito.likir.ca;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.LinkedList;
import java.util.List;

import unito.likir.NodeId;
import unito.likir.messages.ca.CAKeyRequest;
import unito.likir.messages.ca.CAMessage;
import unito.likir.messages.ca.InitRequest;
import unito.likir.messages.ca.CAMessage.OpCode;
import unito.likir.routing.Contact;
import unito.likir.security.AuthNodeId;
import unito.likir.security.AuthNodeIdContent;
import unito.likir.security.BootstrapList;
import unito.likir.security.BootstrapListContent;
import unito.likir.settings.PropFinder;
import unito.likir.settings.Settings;

public class CAInst implements Runnable
{
	private CA ca;
	private Socket clientSocket;
	private final int BOOT_LIST_SIZE;
	
	public CAInst(CA ca, Socket clientSocket)
	{
		BOOT_LIST_SIZE = Integer.parseInt(PropFinder.get(Settings.BOOT_LIST_SIZE));
		this.ca = ca;
		this.clientSocket = clientSocket;
	}
	
	public void run()
	{
        try
        {    
            // reader for incoming messages initialization
            ObjectInputStream objIn = new ObjectInputStream(clientSocket.getInputStream());
            // wait for a request
            CAMessage received = (CAMessage)objIn.readObject();
            
            OpCode code = received.getOpCode();
            
            if (code.equals(OpCode.INITIALIZATION_REQUEST))
			{
				handleInitRequest((InitRequest)received);
				objIn.close();
			}
			/*else if (code.equals(OpCode.BOOTLIST_REQUEST))
			{
				//handleBootlistRequest((BootlistRequest)received);
				System.err.println("Operation unsupported");
			}*/
			else if (code.equals(OpCode.CA_KEY_REQUEST))
			{
				handleCAKeyRequest((CAKeyRequest)received);
				objIn.close();
			}
			else
			{
				System.err.println("Unknown message type");
				//System.exit(0);
				return;
			};
        }
        catch (ClassCastException cce)
        {
        	System.err.println("CAThread: received a corrupted message");
        	//TODO: send error message response
        }
        catch (Exception e)
        {
            System.err.println("CAThread: Error!");
            e.printStackTrace();
          //TODO: send error message response
        }		
	}
	
	/**
	 * 
	 * @param msg
	 * @throws IOException if fails in sending the response message
	 */
	private void handleInitRequest(InitRequest msg) throws IOException
	{   
        BootstrapList bootstrapList = null;
		CAMessage response = null;
		String userId = msg.getUserId();
		
		//stub method, it should implement the various id verification policies
		verifyIdentity(userId);
		
		//build the bootstrap list
		List<Contact> bootList = new LinkedList<Contact>();
		synchronized (ca.getContactList()) //TODO: eliminare???
		{
			int bootListSize = Math.min(ca.getContactList().size(), BOOT_LIST_SIZE);
			for (int i=0; i<bootListSize; i++)
			{
				bootList.add(ca.getContactList().get(i));
			}
			bootstrapList = new BootstrapList(bootList, true);
			try
			{
				//sign bootstrap list
				BootstrapListContent cont = bootstrapList.getContent();
				byte[] signature = ca.getSecurityAgent().sign(cont);
				bootstrapList.setSignature(signature);
			}
			catch (SignatureException e)
			{
				response = ca.getMessageFactory().createErrorMessage(ca.getUserId(), msg.getMessageId(), "Can't sign bootstrap list. Retry later");
				return;
			}
		}
		
		AuthNodeId authId = ca.getRegisteredNodeData().get(userId);
		if (authId != null //there is an existent authNodeId...
			&& authId.getContent().getExpireTime() > System.currentTimeMillis()) //...and it is still valid
		{	
			if (msg.getPublicKey().equals(authId.getContent().getKey()))
			{
				response = ca.getMessageFactory().createInitResponse(ca.getUserId(), msg.getMessageId(), authId, bootstrapList);
			}
			else
			{
				response = ca.getMessageFactory().createErrorMessage(ca.getUserId(), msg.getMessageId(), "Error. Key mismatch");
			}
		}
		else
		{
			try
			{
				// generate a new AuthNodeId
				PublicKey userPublicKey = msg.getPublicKey();

				NodeId userNodeId = ca.genNodeId();
					
				long expireTime = System.currentTimeMillis() + Long.parseLong(PropFinder.get(Settings.AUTH_NODE_ID_DURATION));
				AuthNodeId userAuthNodeId = new AuthNodeId(userNodeId,userPublicKey,userId,expireTime);
				
				try
				{
					//sign authNodeId
					AuthNodeIdContent cont = userAuthNodeId.getContent();
					byte[] signature = ca.getSecurityAgent().sign(cont);
					userAuthNodeId.setSignature(signature);
				}
				catch (SignatureException e)
				{
					response = ca.getMessageFactory().createErrorMessage(ca.getUserId(), msg.getMessageId(), "Can't sign authNodeId. Retry later");
					return;
				}
				// store the generated AuthNodeId
				ca.getRegisteredNodeData().put(userId, userAuthNodeId);
			
				// build a response  message
				response = ca.getMessageFactory().createInitResponse(ca.getUserId(), msg.getMessageId(), userAuthNodeId, bootstrapList);
				
				// add the contact to the CA contact list
				if (msg.getUDPAddress() != null)
				{
					ca.addContact(userNodeId, msg.getUDPAddress());
				}
			}
			catch (Exception e)
			{
				response = ca.getMessageFactory().createErrorMessage(ca.getUserId(), msg.getMessageId(), "Error in request processing");
			}
		}
		//send message
		send(response);
	}
	
	/**
	 * 
	 * @param msg
	 * @throws IOException if fails in sending the response message
	 */
	/*private void handleInitRequest2(InitRequest msg) throws IOException
	{   
        BootstrapList bootstrapList = null;
		CAMessage response = null;
		
		//build the bootstrap list
		List<Contact> bootList = new LinkedList<Contact>();
		synchronized (ca.getContactList()) //TODO: eliminare???
		{
			int bootListSize = Math.min(ca.getContactList().size(), BOOT_LIST_SIZE);
			for (int i=0; i<bootListSize; i++)
			{
				bootList.add(ca.getContactList().get(i));
			}
			bootstrapList = new BootstrapList(bootList, true);
			try
			{
				//sign bootstrap list
				BootstrapListContent cont = bootstrapList.getContent();
				byte[] signature = ca.getSecurityAgent().sign(cont);
				bootstrapList.setSignature(signature);
			}
			catch (SignatureException e)
			{
				response = ca.getMessageFactory().createErrorMessage(ca.getUserId(), msg.getMessageId(), "Can't sign bootstrap list. Retry later");
				return;
			}
		}
		
		// build a response  message
		response = ca.getMessageFactory().createInitResponse(ca.getUserId(), msg.getMessageId(), null, bootstrapList);

		//send message
		send(response);
	}*/
	
	private void handleCAKeyRequest (CAKeyRequest msg) throws IOException
	{
		CAMessage response = null;
		String userId = msg.getUserId();
		
		//stub method, it should implement the various id verification policies
		verifyIdentity(userId);
		
		response = ca.getMessageFactory().createCAKeyResponse(ca.getUserId(), msg.getMessageId(), ca.getSecurityAgent().getPublicKey());

		send(response);
	}
	
	private void send(CAMessage msg) throws IOException
	{
		ObjectOutputStream objOut = null;
		try
		{
			objOut = new ObjectOutputStream(clientSocket.getOutputStream());
			objOut.writeObject(msg);
			objOut.close();
		}
		catch (IOException ioe)
		{
			throw new IOException("Unable to send message");
		}
		finally
		{
			if (objOut != null) objOut.close();
		}
	}
	
	/*
	 * It should also verify that the provided userId is granted to be UNIQUE
	 */
	private void verifyIdentity(String userId)
	{
		// do nothing
	}
}

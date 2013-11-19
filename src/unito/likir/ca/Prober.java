package unito.likir.ca;

import java.util.Collection;

import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.io.LookupManager;
import unito.likir.routing.Contact;
import unito.likir.security.MTRandom;
import unito.likir.settings.PropFinder;
import unito.likir.settings.Settings;

public class Prober implements Runnable
{
	CA ca;
	Node node;
	MTRandom randomGen;
	private long TIME_OUT;
	
	public Prober(CA ca)
	{
		this.ca = ca;
		this.node = ca.getNode();
		this.randomGen = new MTRandom(1000);
		TIME_OUT = Integer.parseInt(PropFinder.get(Settings.TIME_OUT));
	}
	
	public void run()
	{
		System.out.println("CA : start probing...");
		byte[] rawId = new byte[20];
		randomGen.nextBytes(rawId);
		NodeId probeId = NodeId.createWithBytes(rawId);
		LookupManager lookupManager = new LookupManager(node, probeId);
		ca.getExecutor().execute(lookupManager);
		try
		{
			lookupManager.get(TIME_OUT, Settings.DEFAULT_TIME_UNIT);
		}
		catch(Exception e)
		{
			System.err.println("CA : error! probing interrupted!");
		}
		
		Collection<Contact> probed = lookupManager.getMarkedList();
		ca.getContactList().addAll(probed);

		System.out.println("CA : stop probing - added " +probed.size() + " contacts");
	}
}

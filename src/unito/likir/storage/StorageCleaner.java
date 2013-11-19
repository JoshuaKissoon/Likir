package unito.likir.storage;

import java.util.Collection;
import java.util.concurrent.ScheduledFuture;
import java.io.IOException;

import unito.likir.Node;
import unito.likir.routing.Contact;
import unito.likir.settings.PropFinder;
import unito.likir.settings.Settings;

/**
 * Performs the Storage maintenance erasing StorageEntries whose TTL is expired
 * and spreading in the DHT the valid StorageEntries by performing store RPCs.
 * The StorageCleaner is executed periodically for a good DHT functioning.
 * A StorageCleaner instance is always related to the local Node instance.
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class StorageCleaner implements Runnable
{
	private Node node;
	private ScheduledFuture<?> future; //TODO:rivedi il <?>
	private final int K;
	private final int STORE_CLEANER_PERIOD;
	private final int CONTENT_REPUBLISH_PERIOD;
	
	/**
	 * Create a new StorageCleaner
	 * @param node the related Node
	 */
	public StorageCleaner(Node node)
	{
		this.node = node;
		this.K = Integer.parseInt(PropFinder.get(Settings.K));
		this.STORE_CLEANER_PERIOD = Integer.parseInt(PropFinder.get(Settings.STORE_CLEANER_PERIOD));
		this.CONTENT_REPUBLISH_PERIOD = Integer.parseInt(PropFinder.get(Settings.CONTENT_REPUBLISH_PERIOD));
	}
	
	/**
	 * Returns the interval between two subsequent StorageCleaner executions
	 * @return the interval (in milliseconds)
	 */
	public int getInterval()
	{
		return STORE_CLEANER_PERIOD;
	}
    
    /**
     * Starts the <code>StorageCleaner</code>.
     */
    public synchronized void start()
    {
        if (future == null)
        {
            long delay = STORE_CLEANER_PERIOD;
            long initialDelay = delay;
            
            future = node.getInnerExecutor().scheduleWithFixedDelay(this, initialDelay, delay, Settings.DEFAULT_TIME_UNIT);
        }
    }
    
    /**
     * Stops the <code>StorageCleaner</code>.
     */
    public synchronized void stop()
    {
        if (future != null) 
        {
            future.cancel(true);
            future = null;
        }
    }
    
    /*
     * Removes all expired <code>StorageEntry</code> from the <code>Storage</code>.
     */
    private void storageMaintenance()
    {
        Storage storage = node.getStorage();
        long currentTime = System.currentTimeMillis();
        
        synchronized (storage)
        {
            for (StorageEntry entry : storage.values())
            {
                if (entry.isExpired())
                    storage.remove(entry);
                else if (currentTime - entry.getLastRepublishTime() > CONTENT_REPUBLISH_PERIOD)
                {
                	Collection<Contact> nearestNodes =  node.getRouteTable().select(node.getNodeId(), K);
                	for (Contact c : nearestNodes)
                	{
                		try
                		{
                			node.store(c, entry);
                		}
                		catch(IOException ioe)
                		{
                			System.err.println("StorageCleaner : REPLICA STORE FAILED!");
                		}
                	}
                }
            }
        }
    }
    
    /**
     * Performs the storage maintenance
     */
    public void run()
    {
    	storageMaintenance();
    }
}

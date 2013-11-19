package unito.likir.routing;

import java.util.Collection;
import java.util.concurrent.ScheduledFuture;

import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.settings.PropFinder;
import unito.likir.settings.Settings;

/**
 * StorageCleaner class
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class RouteTableRefresher  implements Runnable
{
	private Node node;
	private ScheduledFuture<?> future; //TODO:rivedi il <?>
	//private final int BUCKET_REFRESH_PERIOD;
	private final int BUCKET_REFRESHER_INTERVAL;
	
	public RouteTableRefresher(Node node)
	{
		this.node = node;
		//this.BUCKET_REFRESH_PERIOD = Integer.parseInt(PropFinder.get(Settings.BUCKET_REFRESH_PERIOD));
		this.BUCKET_REFRESHER_INTERVAL = Integer.parseInt(PropFinder.get(Settings.BUCKET_REFRESHER_INTERVAL));
	}
    
    /**
     * Starts the <code>RouteTableRefresher</code>.
     */
    public synchronized void start()
    {
        if (future == null)
        {
            long delay = BUCKET_REFRESHER_INTERVAL;
            long initialDelay = delay;
            
            future = node.getInnerExecutor().scheduleWithFixedDelay(this, initialDelay, delay, Settings.DEFAULT_TIME_UNIT);
        }
    }
    
    /**
     * Stops the <code>RouteTableRefresher</code>.
     */
    public synchronized void stop()
    {
        if (future != null) 
        {
            future.cancel(true);
            future = null;
        }
    }
    
    /**
     * Refreshes buckets
     */
    private void bucketsRefresh()
    {
    	Collection<NodeId> toRefresh = node.getRouteTable().getRefreshIDs(true);
		for (NodeId id : toRefresh)
		{
			try
			{
				node.lookup(id).get();
			}
			catch(Exception ee)
			{
				System.err.println(node.getUserId() + " - failed while refreshing bucket for " + id);
			}
		}
    }
    
    public void run()
    {
    	bucketsRefresh();
    }
}
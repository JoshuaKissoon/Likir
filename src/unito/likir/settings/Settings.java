package unito.likir.settings;

import java.util.concurrent.TimeUnit;

/**
 * Contains the names of the properties in the file config.properties
 * @author Luca Maria Aiello
 * @version 0.1
 */

public class Settings
{
	//CA
	public static final String CA_ID = "caId"; // CA string userId
	public static final String DEFAULT_CA_IP = "caIP"; // default CA address
	public static final String DEFAULT_CA_PORT = "caPort"; // default CA TCP port
	public static final String CA_PERSISTENCE_PATH = "acPersistencePath"; // directory in which CA state is saved
	public static final String AUTH_NODE_ID_DURATION = "authNodeIdDuration"; //time validity of authNodeId
	public static final String PROBING_PERIOD = "probingPeriod"; //delay between two CA probes
	public static final String CONTACT_LIST_MIN_SIZE = "contactListMinSize";
	public static final String BOOT_LIST_SIZE = "bootListSize";
	
	//Kademlia
	public static final String ALPHA = "alpha"; //parallelism factor
	public static final String K = "k"; //replication factor
	public static final String B = "b"; //key length
	
	//IO
	public static final String MAX_CONTENT_SIZE = "maxContentSize"; //the max payload length in byte
	public static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS; 
	public static final String TIME_OUT = "timeOut"; //the default message timeout
	
	//Routing
	public static final String CACHE_SIZE = "cacheSize"; //the route table cache size
	public static final String BUCKET_REFRESH_PERIOD = "bucketRefreshPeriod"; //the route table bucket refresh period
	public static final String MAX_ACCEPT_NODE_FAILURES = "maxAcceptNodeFailures"; //number of failures before a bucket entry is replaced
	public static final String BUCKET_REFRESHER_INTERVAL = "bucketRefresherInterval"; //interval between two runs of the bucket refresher
	
	//Storage
	public static final String CONTENT_REPUBLISH_PERIOD = "contentRepublishPeriod";
	public static final String DEFAULT_TTL = "defaultTTL"; //content TTL
	public static final String MAX_TTL = "maxTTL"; //max content TTL value
	public static final String STORE_INITIAL_SIZE = "storeInitialSize"; //initial storage hash table size
	public static final String STORE_CLEANER_PERIOD = "databaseCleanerPeriod"; //the storage cleaner period
	public static final String DEFAULT_CONTENT_TYPE = "defaultContentType"; //the default content type
	
	//State persistence
	public static final String NODE_PERSISTENCE_PATH = "nodePersistencePath"; //directory in which node states are saved
	public static final String CS_KEY_PATH = "csKeyPath"; //directory in which CS public key is saved

	//TEST
	/*public static final String SERVER_NAME = "serverName"; // Server hostname or IP address
	public static final String SERVER_PORT = "serverPort"; // Server port
	public static final String CLIENT_PORT = "clientPort"; // client port
	public static final String ITERATION_NUMBER = "iterationsNumber"; // number of RPC sent between client and server
	public static final String KAD_SERVER_PORT = "kadServerPort"; // Server port
	public static final String KAD_CLIENT_PORT = "kadClientPort"; // client port*/
}
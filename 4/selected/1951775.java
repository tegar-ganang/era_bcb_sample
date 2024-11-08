package net.sf.ehcache.plugins.clustersupport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.blocks.NotificationBus;

public class JavaGroupsBroadcastingManager implements BroadcastingManager, NotificationBus.Consumer {

    private static final Log log = LogFactory.getLog(JavaGroupsBroadcastingManager.class);

    private static final String BUS_NAME = "EHCacheBus";

    private static final String CHANNEL_PROPERTIES = "cache.cluster.properties";

    private static final String MULTICAST_IP_PROPERTY = "cache.cluster.multicast.ip";

    /**
	 * The first half of the default channel properties. They default channel
	 * properties are:
	 * 
	 * <pre>
	 *      UDP(mcast_addr=*.*.*.*;mcast_port=45566;ip_ttl=32;\
	 *      mcast_send_buf_size=150000;mcast_recv_buf_size=80000):\
	 *      PING(timeout=2000;num_initial_members=3):\
	 *      MERGE2(min_interval=5000;max_interval=10000):\
	 *      FD_SOCK:VERIFY_SUSPECT(timeout=1500):\
	 *      pbcast.NAKACK(gc_lag=50;retransmit_timeout=300,600,1200,2400,4800;max_xmit_size=8192):\
	 *      UNICAST(timeout=300,600,1200,2400):\
	 *      pbcast.STABLE(desired_avg_gossip=20000):\
	 *      FRAG(frag_size=8096;down_thread=false;up_thread=false):\
	 *      pbcast.GMS(join_timeout=5000;join_retry_timeout=2000;shun=false;print_local_addr=true)
	 * </pre>
	 * 
	 * Where <code>*.*.*.*</code> is the specified multicast IP, which
	 * defaults to <code>239.252.1.1</code>.
	 */
    private static final String DEFAULT_CHANNEL_PROPERTIES_PRE = "UDP(mcast_addr=";

    /**
	 * The second half of the default channel properties. They default channel
	 * properties are:
	 * 
	 * <pre>
	 *      UDP(mcast_addr=*.*.*.*;mcast_port=45566;ip_ttl=32;\
	 *      mcast_send_buf_size=150000;mcast_recv_buf_size=80000):\
	 *      PING(timeout=2000;num_initial_members=3):\
	 *      MERGE2(min_interval=5000;max_interval=10000):\
	 *      FD_SOCK:VERIFY_SUSPECT(timeout=1500):\
	 *      pbcast.NAKACK(gc_lag=50;retransmit_timeout=300,600,1200,2400,4800;max_xmit_size=8192):\
	 *      UNICAST(timeout=300,600,1200,2400):\
	 *      pbcast.STABLE(desired_avg_gossip=20000):\
	 *      FRAG(frag_size=8096;down_thread=false;up_thread=false):\
	 *      pbcast.GMS(join_timeout=5000;join_retry_timeout=2000;shun=false;print_local_addr=true)
	 * </pre>
	 * 
	 * Where <code>*.*.*.*</code> is the specified multicast IP, which
	 * defaults to <code>239.252.1.1</code>.
	 */
    private static final String DEFAULT_CHANNEL_PROPERTIES_POST = ";mcast_port=45566;ip_ttl=32;mcast_send_buf_size=150000;mcast_recv_buf_size=80000):" + "PING(timeout=2000;num_initial_members=3):MERGE2(min_interval=5000;max_interval=10000):FD_SOCK:VERIFY_SUSPECT(timeout=1500):" + "pbcast.NAKACK(gc_lag=50;retransmit_timeout=300,600,1200,2400,4800;max_xmit_size=8192):UNICAST(timeout=300,600,1200,2400):pbcast.STABLE(desired_avg_gossip=20000):" + "FRAG(frag_size=8096;down_thread=false;up_thread=false):pbcast.GMS(join_timeout=5000;join_retry_timeout=2000;shun=false;print_local_addr=true)";

    private static final String DEFAULT_MULTICAST_IP = "239.252.1.1";

    private NotificationBus bus;

    public void start(String configuration) {
        try {
            String properties = getProperties(configuration);
            bus = new NotificationBus(BUS_NAME, properties);
            bus.start();
            bus.getChannel().setOpt(Channel.LOCAL, new Boolean(false));
            bus.setConsumer(this);
            log.info("JavaGroups clustering support started successfully");
        } catch (Exception e) {
            throw new ClusterCacheException("Initialization failed: ", e);
        }
    }

    private String getProperties(String configuration) throws IOException {
        InputStream is = null;
        try {
            String properties = null;
            String multicastIP = null;
            if (configuration != null) {
                Properties props = new Properties();
                is = new ByteArrayInputStream(configuration.getBytes());
                props.load(is);
                properties = props.getProperty(CHANNEL_PROPERTIES);
                multicastIP = props.getProperty(MULTICAST_IP_PROPERTY);
            } else {
                if (log.isWarnEnabled()) log.warn("clusterConfiguration is missing using the defaults");
            }
            if ((properties == null) && (multicastIP == null)) multicastIP = DEFAULT_MULTICAST_IP;
            if (properties == null) properties = DEFAULT_CHANNEL_PROPERTIES_PRE + multicastIP.trim() + DEFAULT_CHANNEL_PROPERTIES_POST; else properties = properties.trim();
            if (log.isInfoEnabled()) log.info("Starting a new JavaGroups broadcasting listener with properties=" + properties);
            return properties;
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                log.error("Could not close.", e);
            }
        }
    }

    /**
	 * Uses JavaGroups to broadcast the supplied notification message across the
	 * cluster.
	 * 
	 * @param message
	 *            The cluster nofication message to broadcast.
	 */
    public void sendNotification(ClusterNotification message) {
        if (log.isDebugEnabled()) log.debug("sendNotification : " + message);
        bus.sendNotification(message);
    }

    /**
	 * Handles incoming notification messages from JavaGroups. This method
	 * should never be called directly.
	 * 
	 * @param serializable
	 *            The incoming message object. This must be a
	 *            {@link ClusterNotification}.
	 */
    public void handleNotification(Serializable serializable) {
        if (log.isDebugEnabled()) log.debug("handleNotification (" + serializable + ") was received.");
        try {
            if (!(serializable instanceof ClusterNotification)) {
                log.error("An unknown cluster notification message received (class=" + serializable.getClass().getName() + "). Notification ignored.");
                return;
            }
            ClusterNotification clusterNotification = (ClusterNotification) serializable;
            Cache cache = CacheManager.getInstance().getCache(clusterNotification.getCacheName());
            if (cache == null) {
                log.warn("A cluster notification (" + clusterNotification + ") was received, but no cache is registered on this machine. Notification ignored.");
                return;
            }
            handleClusterNotification(clusterNotification);
        } catch (IllegalStateException e) {
            throw new ClusterCacheException(e);
        } catch (CacheException e) {
            throw new ClusterCacheException(e);
        }
    }

    private void handleClusterNotification(ClusterNotification message) {
        try {
            Cache cache = CacheManager.getInstance().getCache(message.getCacheName());
            if (log.isDebugEnabled()) log.debug("cluster notification type:" + message.getType() + "for key:" + message.getKey() + " from " + cache.getName() + "arrivied");
            switch(message.getType()) {
                case ClusterNotification.ELEMENT_PUT:
                    cache.removeQuiet(message.getKey());
                    if (log.isDebugEnabled()) log.debug(message.getKey() + " was removed without publish from " + cache.getName());
                    break;
                case ClusterNotification.ELEMENT_UPDATE:
                    cache.removeQuiet(message.getKey());
                    if (log.isDebugEnabled()) log.debug(message.getKey() + " was removed without publish from " + cache.getName());
                    break;
                case ClusterNotification.ELEMENT_REMOVED:
                    cache.removeQuiet(message.getKey());
                    if (log.isDebugEnabled()) log.debug(message.getKey() + " was removed without publish from " + cache.getName());
                    break;
                default:
                    log.warn("The cluster notification (" + message + ") is of an unknown type. Notification ignored.");
            }
        } catch (CacheException e) {
            throw new ClusterCacheException(e);
        }
    }

    /**
	 * We are not using the caching, so we just return something that identifies
	 * us. This method should never be called directly.
	 */
    public Serializable getCache() {
        return "BroadcastingManager: " + bus.getLocalAddress();
    }

    /**
	 * A callback that is fired when a new member joins the cluster. This method
	 * should never be called directly.
	 * 
	 * @param address
	 *            The address of the member who just joined.
	 */
    public void memberJoined(Address address) {
        if (log.isInfoEnabled()) {
            log.info("A new member at address '" + address + "' has joined the cluster");
        }
    }

    /**
	 * A callback that is fired when an existing member leaves the cluster. This
	 * method should never be called directly.
	 * 
	 * @param address
	 *            The address of the member who left.
	 */
    public void memberLeft(Address address) {
        if (log.isInfoEnabled()) {
            log.info("Member at address '" + address + "' left the cluster");
        }
    }

    public synchronized void stop() {
        if (log.isInfoEnabled()) {
            log.info("JavaGroups shutting down...");
        }
        if (bus != null) {
            bus.stop();
            bus = null;
        } else {
            log.warn("Notification bus wasn't started or stoped before was invoked before!");
        }
        if (log.isInfoEnabled()) {
            log.info("JavaGroups shutdown complete.");
        }
    }
}

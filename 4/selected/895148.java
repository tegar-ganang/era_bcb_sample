package net.sf.ehcache.plugins.clustersupport;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.ObjectExistsException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.Message;
import org.jgroups.blocks.NotificationBus;

public class JavaGroupsEHCacheTest extends TestCase implements NotificationBus.Consumer {

    private static final Log log = LogFactory.getLog(JavaGroupsEHCacheTest.class);

    private static final String BUS_NAME = "EHCacheBus";

    private static final String DEFAULT_CHANNEL_PROPERTIES_PRE = "UDP(mcast_addr=";

    private static final String DEFAULT_CHANNEL_PROPERTIES_POST = ";mcast_port=45566;ip_ttl=32;mcast_send_buf_size=150000;mcast_recv_buf_size=80000):" + "PING(timeout=2000;num_initial_members=3):MERGE2(min_interval=5000;max_interval=10000):FD_SOCK:VERIFY_SUSPECT(timeout=1500):" + "pbcast.NAKACK(gc_lag=50;retransmit_timeout=300,600,1200,2400,4800;max_xmit_size=8192):UNICAST(timeout=300,600,1200,2400):pbcast.STABLE(desired_avg_gossip=20000):" + "FRAG(frag_size=8096;down_thread=false;up_thread=false):pbcast.GMS(join_timeout=5000;join_retry_timeout=2000;shun=false;print_local_addr=true)";

    private static final String DEFAULT_MULTICAST_IP = "239.252.1.1";

    private static NotificationBus bus;

    private static Set notifications = new HashSet();

    protected void setUp() throws Exception {
        super.setUp();
        if (bus != null) return;
        String properties = null;
        String multicastIP = null;
        if ((properties == null) && (multicastIP == null)) multicastIP = DEFAULT_MULTICAST_IP;
        if (properties == null) properties = DEFAULT_CHANNEL_PROPERTIES_PRE + multicastIP.trim() + DEFAULT_CHANNEL_PROPERTIES_POST; else properties = properties.trim();
        if (log.isInfoEnabled()) log.info("Starting a new JavaGroups broadcasting listener with properties=" + properties);
        bus = new NotificationBus(BUS_NAME, properties);
        bus.start();
        bus.getChannel().setOpt(Channel.LOCAL, new Boolean(false));
        bus.setConsumer(this);
        CacheManager.create(Thread.currentThread().getContextClassLoader().getResource("/ehcache.xml"));
        CacheManager.getInstance().addCache("cacheTest");
        log.info("JavaGroups clustering support started successfully");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        if (log.isInfoEnabled()) log.info("test finished");
    }

    public void xtestSendPutMessege() throws IllegalStateException, ObjectExistsException, CacheException {
        notifications.add("sendPut");
        Cache cache = CacheManager.getInstance().getCache("cacheTest");
        if (log.isInfoEnabled()) log.info("start the testSendPutMessege");
        cache.put(new Element("sendPut", "value1"));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertFalse(notifications.contains("sendPut"));
    }

    public void testRecievePutMessege() throws IllegalStateException, ObjectExistsException, CacheException {
        Cache cache = CacheManager.getInstance().getCache("cacheTest");
        cache.put(new Element("recievePut", "value1"));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (log.isInfoEnabled()) log.info("start the testRecievePutMessege");
        bus.sendNotification(new ClusterNotification(ClusterNotification.ELEMENT_PUT, "recievePut", "cacheTest"));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertNull(cache.get("recievePut"));
    }

    public void testSendRemoveMessege() throws IllegalStateException, ObjectExistsException, CacheException {
        Cache cache = CacheManager.getInstance().getCache("cacheTest");
        cache.put(new Element("sendRemove", "value1"));
        notifications.add("sendRemove");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (log.isInfoEnabled()) log.info("start the testSendRemoveMessege");
        cache.remove("sendRemove");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertFalse(notifications.contains("sendRemove"));
    }

    public void testRecieveRemoveMessege() throws IllegalStateException, ObjectExistsException, CacheException {
        Cache cache = CacheManager.getInstance().getCache("cacheTest");
        cache.put(new Element("recieveRemove", "value1"));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (log.isInfoEnabled()) log.info("start the testRecieveRemoveMessege");
        bus.sendNotification(new ClusterNotification(ClusterNotification.ELEMENT_REMOVED, "recieveRemove", "cacheTest"));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertNull(cache.get("recieveRemove"));
    }

    public void testSendUpdateMessege() throws IllegalStateException, ObjectExistsException, CacheException {
        notifications.add("sendUpdate");
        Cache cache = CacheManager.getInstance().getCache("cacheTest");
        if (log.isInfoEnabled()) log.info("start the testSendPutMessege");
        cache.put(new Element("sendUpdate", "value1"));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertFalse(notifications.contains("sendUpdate"));
    }

    public void testRecieveUpdateMessege() throws IllegalStateException, ObjectExistsException, CacheException {
        Cache cache = CacheManager.getInstance().getCache("cacheTest");
        cache.put(new Element("recieveUpdate", "value1"));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (log.isInfoEnabled()) log.info("start the testRecieveUpdateMessege");
        bus.sendNotification(new ClusterNotification(ClusterNotification.ELEMENT_UPDATE, "recieveUpdate", "cacheTest"));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertNull(cache.get("recieveUpdate"));
    }

    public void handleNotification(Serializable serializable) {
        if (log.isDebugEnabled()) log.debug("handleNotification (" + serializable + ") was received.");
        notifications.remove(((ClusterNotification) (serializable)).getKey());
    }

    public Serializable getCache() {
        return "JavaGroupsEHCacheTest: " + bus.getLocalAddress();
    }

    public void memberJoined(Address address) {
        if (log.isInfoEnabled()) {
            log.info("A new member at address '" + address + "' has joined the cluster");
        }
    }

    public void memberLeft(Address address) {
        if (log.isInfoEnabled()) {
            log.info("Member at address '" + address + "' left the cluster");
        }
    }
}

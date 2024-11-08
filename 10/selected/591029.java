package org.light.portal.distribute.impl;

import static org.light.portal.util.Constants._BACKEND_HOST;
import static org.light.portal.util.Constants._CACHE_HOST;
import static org.light.portal.util.Constants._DATABASE_HOST;
import static org.light.portal.util.Constants._FILE_STORAGE_SECOND_LEVEL_ENABLED;
import static org.light.portal.util.Constants._FRONT_HOST;
import static org.light.portal.util.Constants._REPLICATION_BUS_HOST;
import static org.light.portal.util.Constants._REPLICATION_BUS_HOSTS;
import static org.light.portal.util.Constants._REPLICATION_ENABLED;
import static org.light.portal.util.Constants._REPLICATION_HOSTS;
import static org.light.portal.util.Constants._REPLICATION_HOSTS_FOLDER;
import static org.light.portal.util.Constants._REPLICATION_HOSTS_BUS_FILE;
import static org.light.portal.util.Constants._REPLICATION_HOSTS_FILE;
import static org.light.portal.util.Constants._REPLICATION_PUBLISHER_MAINTAIN_INTERVAL;
import static org.light.portal.util.Constants._REPLICATION_PUBLISHER_THREAD_LIVF_TIME;
import static org.light.portal.util.Constants._REPLICATION_PUBLISHER_THREAD_MAX;
import static org.light.portal.util.Constants._REPLICATION_PUBLISHER_THREAD_MIN;
import static org.light.portal.util.Constants._REPLICATION_SERVER_NAME;
import static org.light.portal.util.Constants._REPLICATION_SERVER_PORT;
import static org.light.portal.util.Constants._SEARCH_HOST;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.engine.SessionFactoryImplementor;
import org.light.portal.common.model.Fileable;
import org.light.portal.core.model.Entity;
import org.light.portal.distribute.Event;
import org.light.portal.distribute.Message;
import org.light.portal.distribute.ReplicationInterface;
import org.light.portal.distribute.ReplicationPublisher;
import org.light.portal.file.FileServiceUtil;
import org.light.portal.logger.Logger;
import org.light.portal.logger.LoggerFactory;
import org.light.portal.organization.model.Organization;

/**
 * 
 * @author Jianmin Liu
 **/
public class RMIReplicationPublisherImpl extends ReplicationAbstractImpl implements ReplicationPublisher {

    public RMIReplicationPublisherImpl() {
    }

    public void init() {
        if (_REPLICATION_ENABLED) {
            queue = new LinkedBlockingQueue<Message>();
            publisher = new Worker();
        }
    }

    public void connect(String server) {
        new Connector(server);
    }

    public void syncDone(String server) {
        syncDoneCounter.addAndGet(1);
    }

    public boolean isReady() {
        return (publisher == null) || (syncDoneCounter.get() >= publisher.registries.size());
    }

    public void process(Message message) {
        if (!_REPLICATION_ENABLED || queue == null) return;
        try {
            if (message.getBody() instanceof Fileable) {
                Organization org = this.getUserService().getOrgById(message.getOrgId());
                if (_FILE_STORAGE_SECOND_LEVEL_ENABLED) {
                    Fileable file = (Fileable) message.getBody();
                    file.setFile(null);
                }
            }
            queue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    class Worker implements Runnable {

        private Set<Registry> registries = new HashSet<Registry>();

        private Map<Registry, String> registryMap = new HashMap<Registry, String>();

        private Map<String, Long> serverMap = new HashMap<String, Long>();

        private Set<String> offlineServers;

        private String[] servers;

        private int totalHosts;

        private String local;

        private String thisServer;

        public Worker() {
            new Thread(this).start();
        }

        public void run() {
            init();
            while (true) {
                try {
                    Message message = queue.take();
                    this.execute(message);
                } catch (Throwable e) {
                    logger.error(e.getMessage());
                }
            }
        }

        private void init() {
            try {
                local = (InetAddress.getLocalHost()).getHostAddress();
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
            thisServer = local + ":" + _REPLICATION_SERVER_PORT;
            String hosts = (_REPLICATION_BUS_HOST) ? getBusHost() : getHosts();
            if (hosts != null && !hosts.isEmpty()) {
                servers = hosts.split(";");
                totalHosts = servers.length;
            }
            connect();
            int min = _REPLICATION_PUBLISHER_THREAD_MIN;
            int max = _REPLICATION_PUBLISHER_THREAD_MAX;
            if (max == 0) max = totalHosts;
            if (max <= min) max = min;
            tpe = new ThreadPoolExecutor(min, max, _REPLICATION_PUBLISHER_THREAD_LIVF_TIME, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
            new Maintainer();
        }

        private String getBusHost() {
            String hosts = _REPLICATION_HOSTS;
            if (_FILE_STORAGE_SECOND_LEVEL_ENABLED) {
                try {
                    String registerHosts = FileServiceUtil.getContent(_REPLICATION_HOSTS_FOLDER, _REPLICATION_HOSTS_FILE);
                    if (registerHosts != null) {
                        hosts = registerHosts;
                    } else {
                        hosts = "";
                    }
                    String busHosts = FileServiceUtil.getContent(_REPLICATION_HOSTS_FOLDER, _REPLICATION_HOSTS_BUS_FILE);
                    if (busHosts != null) {
                        if (busHosts.indexOf(thisServer) < 0) {
                            String updatedHosts = hosts + ";" + thisServer;
                            FileServiceUtil.putContent(_REPLICATION_HOSTS_FOLDER, _REPLICATION_HOSTS_BUS_FILE, updatedHosts);
                        }
                    } else {
                        FileServiceUtil.putContent(_REPLICATION_HOSTS_FOLDER, _REPLICATION_HOSTS_BUS_FILE, thisServer);
                    }
                    if (_FRONT_HOST || _SEARCH_HOST || _CACHE_HOST || _DATABASE_HOST || _BACKEND_HOST) {
                        if (registerHosts != null) {
                            if (registerHosts.indexOf(thisServer) < 0) {
                                String updatedHosts = registerHosts + ";" + thisServer;
                                FileServiceUtil.putContent(_REPLICATION_HOSTS_FOLDER, _REPLICATION_HOSTS_FILE, updatedHosts);
                            }
                        } else {
                            FileServiceUtil.putContent(_REPLICATION_HOSTS_FOLDER, _REPLICATION_HOSTS_FILE, thisServer);
                        }
                    }
                } catch (Exception e) {
                }
            }
            return hosts;
        }

        private String getHosts() {
            String hosts = _REPLICATION_BUS_HOSTS;
            if (_FILE_STORAGE_SECOND_LEVEL_ENABLED) {
                try {
                    String busHosts = FileServiceUtil.getContent(_REPLICATION_HOSTS_FOLDER, _REPLICATION_HOSTS_BUS_FILE);
                    if (busHosts != null) {
                        hosts = busHosts;
                    } else {
                        hosts = "";
                    }
                    String registerHosts = FileServiceUtil.getContent(_REPLICATION_HOSTS_FOLDER, _REPLICATION_HOSTS_FILE);
                    if (registerHosts != null) {
                        if (registerHosts.indexOf(thisServer) < 0) {
                            String updatedHosts = registerHosts + ";" + thisServer;
                            FileServiceUtil.putContent(_REPLICATION_HOSTS_FOLDER, _REPLICATION_HOSTS_FILE, updatedHosts);
                        }
                    } else {
                        FileServiceUtil.putContent(_REPLICATION_HOSTS_FOLDER, _REPLICATION_HOSTS_FILE, thisServer);
                    }
                } catch (Exception e) {
                }
            }
            return hosts;
        }

        private void connect() {
            if (registries == null || registries.size() < totalHosts) {
                lock.lock();
                if (registries == null || registries.size() < totalHosts) {
                    if (servers != null) {
                        for (String server : servers) {
                            connect(server);
                        }
                    }
                }
                lock.unlock();
            }
        }

        private void connect(String server) {
            if (!serverMap.containsKey(server)) {
                lock.lock();
                String[] pair = server.split(":");
                String host = pair[0];
                int port = _REPLICATION_SERVER_PORT;
                if (pair.length > 1) port = Integer.parseInt(pair[1]); else server = host + ":" + port;
                if (!serverMap.containsKey(server)) {
                    if (!host.equals(local) || port != _REPLICATION_SERVER_PORT) {
                        try {
                            Registry registry = LocateRegistry.getRegistry(host, port);
                            ReplicationInterface listener = (ReplicationInterface) (registry.lookup(_REPLICATION_SERVER_NAME));
                            String thisServer = local + ":" + _REPLICATION_SERVER_PORT;
                            listener.process(new Message(Event.CONNECT, thisServer));
                            listener = null;
                            if (registries == null) registries = new HashSet<Registry>();
                            registries.add(registry);
                            registryMap.put(registry, server);
                            serverMap.put(server, 0L);
                            if (offlineServers != null) offlineServers.remove(server);
                            logger.info(String.format("connected with replication server %s", server));
                        } catch (Exception e) {
                            logger.error(String.format("connect server %s failed: ", server, e.getMessage()));
                            if (offlineServers == null) offlineServers = new HashSet<String>();
                            offlineServers.add(server);
                        }
                    }
                }
                lock.unlock();
            }
        }

        private void execute(Message message) {
            if (message.getFromHost() == null) message.setFromHost(thisServer);
            if (_REPLICATION_BUS_HOST) publishToRestHosts(message); else publishToBus(message);
        }

        private void publishToBus(Message message) {
            lock.lock();
            if (registries != null && registries.size() > 0) {
                boolean published = false;
                if (registries.size() == 1) {
                    published = publishToBus(message, registries.iterator().next());
                } else {
                    int trial = 0;
                    while (!published && trial < 3) {
                        int index = new Random().nextInt(registries.size());
                        int count = 0;
                        Iterator<Registry> iterator = registries.iterator();
                        while (iterator.hasNext()) {
                            if (index == count) {
                                published = publishToBus(message, iterator.next());
                                break;
                            }
                            count++;
                        }
                    }
                    trial++;
                }
                if (!published) {
                    saveMessage(null, message);
                }
            }
            lock.unlock();
        }

        private boolean publishToBus(Message message, Registry registry) {
            boolean published = false;
            try {
                ReplicationInterface listener = (ReplicationInterface) (registry.lookup(_REPLICATION_SERVER_NAME));
                logger.info(String.format("host %s pulish replication message to bus host %s: %s", thisServer, registryMap.get(registry), message.toString()));
                listener.process(message);
                listener = null;
                published = true;
            } catch (Exception e) {
            }
            return published;
        }

        private void publishToRestHosts(Message message) {
            if (message.getTargetHost() != null) {
                Set<Entry<Registry, String>> map = registryMap.entrySet();
                for (Entry<Registry, String> entry : map) {
                    if (message.getTargetHost().equals(entry.getValue())) {
                        publish(message, entry.getKey());
                        break;
                    }
                }
            } else {
                lock.lock();
                if (registries != null && registries.size() > 0) {
                    Set<Entry<Registry, String>> map = registryMap.entrySet();
                    for (Entry<Registry, String> entry : map) {
                        if (!entry.getValue().equals(message.getFromHost())) {
                            publish(message, entry.getKey());
                        }
                    }
                }
                if (offlineServers != null && offlineServers.size() > 0) {
                    for (String server : offlineServers) {
                        if (!server.equals(message.getFromHost())) saveMessage(server, message);
                    }
                }
                lock.unlock();
            }
        }

        private void publish(final Message message, final Registry registry) {
            tpe.execute(new Thread() {

                public void run() {
                    publishing(message, registry);
                }
            });
        }

        private void publishing(Message message, Registry registry) {
            try {
                ReplicationInterface listener = (ReplicationInterface) (registry.lookup(_REPLICATION_SERVER_NAME));
                logger.info(String.format("host %s pulish replication message to host %s: %s", thisServer, registryMap.get(registry), message.toString()));
                listener.process(message);
                listener = null;
                String server = registryMap.get(registry);
                serverMap.put(server, 0L);
            } catch (Exception e) {
                String server = registryMap.get(registry);
                long failedCount = serverMap.get(server) + 1;
                if (failedCount <= 5) {
                    serverMap.put(server, failedCount);
                    saveMessage(server, message);
                } else {
                    registries.remove(registry);
                    serverMap.remove(server);
                    offlineServers.add(server);
                }
            }
        }

        private void saveMessage(String server, Message message) {
            if (message.getEvent() == Event.ENTITY_UPDATE || message.getEvent() == Event.ENTITY_DELETE) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oout = new ObjectOutputStream(baos);
                    oout.writeObject(message);
                    oout.close();
                    saveMessage(server, message, baos.toByteArray());
                    logger.info(String.format("host %s save replication message for host %s: %s", thisServer, server, message.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error(String.format("host %s save replication message for host %s failed: %s[Exception: %s]", thisServer, server, message.toString(), e.getMessage()));
                }
            }
        }

        private void saveMessage(String server, Message message, byte[] bytes) throws Exception {
            ConnectionProvider cp = null;
            Connection conn = null;
            PreparedStatement ps = null;
            try {
                SessionFactoryImplementor impl = (SessionFactoryImplementor) getPortalDao().getSessionFactory();
                cp = impl.getConnectionProvider();
                conn = cp.getConnection();
                conn.setAutoCommit(false);
                long orgId = 0;
                String className = "";
                long classId = 0;
                if (message.getBody() instanceof Entity) {
                    Entity entity = (Entity) message.getBody();
                    orgId = entity.getOrgId();
                    className = entity.getClass().getName();
                    classId = entity.getId();
                }
                ps = conn.prepareStatement("insert into light_replication_message (orgId,server,event,className,classId,message,createDate) values(?,?,?,?,?,?,?);");
                ps.setLong(1, orgId);
                ps.setString(2, server);
                ps.setString(3, message.getEvent().toString());
                ps.setString(4, className);
                ps.setLong(5, classId);
                ps.setBytes(6, bytes);
                ps.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
                ps.executeUpdate();
                conn.commit();
                ps.close();
                conn.close();
            } catch (Exception e) {
                conn.rollback();
                ps.close();
                conn.close();
                e.printStackTrace();
                throw new Exception(e);
            }
        }

        class Maintainer implements Runnable {

            public Maintainer() {
                new Thread(this).start();
            }

            public void run() {
                while (true) {
                    try {
                        Thread.sleep(_REPLICATION_PUBLISHER_MAINTAIN_INTERVAL);
                        connect();
                        maintain();
                    } catch (Throwable e) {
                        logger.error(e.getMessage());
                    }
                }
            }

            private void maintain() {
                logger.info(String.format("maintaining replicaiton publisher..."));
                logger.info(String.format("host %s retrieve saved messages", thisServer));
                if (registries != null) {
                    for (Registry registry : registries) {
                        String server = registryMap.get(registry);
                        processMessages(server);
                    }
                }
            }
        }
    }

    class Connector implements Runnable {

        private String server;

        public Connector(String server) {
            this.server = server;
            new Thread(this).start();
        }

        public void run() {
            while (!isReady()) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
            publisher.connect(server);
            processMessages(server);
            process(new Message(Event.SYNC_DONE, server));
        }
    }

    private ThreadPoolExecutor tpe;

    private BlockingQueue<Message> queue;

    private Worker publisher;

    private AtomicInteger syncDoneCounter = new AtomicInteger(0);

    private Lock lock = new ReentrantLock();

    private Logger logger = LoggerFactory.getLogger(this.getClass());
}

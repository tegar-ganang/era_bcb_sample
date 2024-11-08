package org.nexopenframework.services.ha.jgroups;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.Version;
import org.jgroups.blocks.NotificationBus;
import org.jgroups.blocks.NotificationBus.Consumer;
import org.jgroups.persistence.CannotPersistException;
import org.nexopenframework.core.ManagedComponent;
import org.nexopenframework.core.config.ComponentModuleConfig;
import org.nexopenframework.jmx.support.MBeanServerLocator;
import org.nexopenframework.services.Service;
import org.nexopenframework.services.ServiceException;
import org.nexopenframework.services.ServiceRuntimeException;
import org.nexopenframework.services.ha.AttributeChangeNotification;
import org.nexopenframework.services.ha.HANotification;
import org.nexopenframework.services.ha.HANotificationBroadcaster;
import org.nexopenframework.services.ha.OperationInvokedNotification;
import org.nexopenframework.services.ha.ServiceState;
import org.nexopenframework.services.ha.ServiceStateRegistry;
import org.nexopenframework.services.ha.StateChangeNotification;
import org.nexopenframework.services.ha.management.DistributedUpdatableMBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

/**
 * <p>NexTReT Open Framework</p>
 * 
 * <p>Service cluster implementation based in JGroups framework.</p>
 * 
 * <p>Note : it must be compatible with older versions of JGroups available 
 *    in App servers like JBoss 4.0.4.GA or higher</p>
 * 
 * @author <a href="mailto:fme@nextret.net">Francesc Xavier Magdaleno</a>
 * @author <a href="mailto:idm@nextret.net">Ivan Dario Due�as</a>
 * @author <a href="mailto:tlj@nextret.net">Toni Lopez</a>
 * @author <a href="mailto:mbc@nextret.net">Marc Baiges Camprub�</a>
 * @see DistributedUpdatableMBean
 * @see NotificationBus
 * @see NotificationBus.Consumer
 * @version 1.0.2
 * @since 1.0
 */
public class JGroupsServiceCluster implements JGroupsServiceClusterMBean, Consumer, HANotificationBroadcaster, ManagedComponent {

    private static boolean isJmxPresent = true;

    static {
        try {
            Class.forName("org.jgroups.jmx.JmxConfigurator");
        } catch (ClassNotFoundException e) {
            isJmxPresent = false;
        }
    }

    /**default JMX name*/
    public static final String DEFAULT_MBEAN_NAME = "openfrwk.core:service=cluster";

    /**JGroups prefix*/
    private static final String DEFAULT_CHANNEL_PROPERTIES_PRE = "UDP(mcast_addr=";

    /**JGroups default IP multicast*/
    private static final String DEFAULT_MULTICAST_IP = "231.12.21.132";

    /**JGroups post*/
    private static final String DEFAULT_CHANNEL_PROPERTIES_POST = ";mcast_port=44566;ip_ttl=64;" + "mcast_send_buf_size=150000;mcast_recv_buf_size=80000):" + "PING(timeout=2000;num_initial_members=3):" + "MERGE2(min_interval=5000;max_interval=10000):" + "FD:" + "VERIFY_SUSPECT(timeout=1500):" + "pbcast.NAKACK(gc_lag=50;retransmit_timeout=300,600,1200,2400,4800;max_xmit_size=8192):" + "UNICAST(timeout=600,1200,2400):" + "pbcast.STABLE(desired_avg_gossip=20000):" + "FRAG(down_thread=false;up_thread=false;frag_size=8192):" + "pbcast.GMS(join_timeout=5000;join_retry_timeout=2000;" + "shun=false;print_local_addr=true):" + "pbcast.STATE_TRANSFER";

    /**The JMX object name*/
    private ObjectName objName;

    /**logging service*/
    protected Log logger = LogFactory.getLog(this.getClass());

    /**The JGroups notification Bus*/
    private NotificationBus notificationBus;

    /**states of services <ObjectName,ServiceState>*/
    private ServiceStateRegistry registry;

    /** The JChannel name */
    private String clusterName = "openfrwkCluster";

    /**JGroups properties*/
    private String jgProperties;

    /**Multicast IP address*/
    private String multicastIP;

    /**timeout*/
    private long timeout = 1500;

    /**our extended persistence manager which has a lifecycle support*/
    private PersistenceManager persistenceManager;

    /**current state of this MBean*/
    private SynchronizedInt state = new SynchronizedInt(0);

    /**
	 * This flag tells to this service not to start till we explicitly
	 * say in a JMX console changing this variable to false and invoking start
	 */
    private SynchronizedBoolean localMode = new SynchronizedBoolean(false);

    /**
	 * <p>The JMX ObjectName for this MBean</p>
	 * 
	 * @see org.nexopenframework.services.ha.ServiceClusterMBean#getObjectName()
	 */
    public ObjectName getObjectName() {
        if (objName == null) {
            try {
                String strObjName = this._getObjectName();
                this.objName = ObjectName.getInstance(strObjName);
            } catch (MalformedObjectNameException e) {
                throw new ServiceRuntimeException(e);
            }
        }
        return objName;
    }

    public void setObjectName(ObjectName objName) {
        this.objName = objName;
    }

    public String getVersion() {
        return new StringBuffer(Version.version).append("(").append(Version.cvs).append(")").toString();
    }

    /**
	 * 
	 * @see org.nexopenframework.services.ha.ServiceClusterMBean#getClusterName()
	 */
    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
	 * @see org.nexopenframework.services.ha.ServiceClusterMBean#getCurrentNode()
	 */
    public String getCurrentNode() {
        Address address = this.notificationBus.getLocalAddress();
        return address.toString();
    }

    public boolean isCoordinator() {
        return this.notificationBus.isCoordinator();
    }

    public String getClusterProperties() {
        return jgProperties;
    }

    public void setClusterProperties(String clusterProperties) {
        this.jgProperties = clusterProperties;
    }

    /**
	 * <p>retrieves The Mulitcast IP</p>
	 * 
	 * @see org.nexopenframework.services.ha.jgroups.JGroupsServiceClusterMBean#getMulticastIP()
	 */
    public String getMulticastIP() {
        return multicastIP;
    }

    public void setMulticastIP(String multicastIP) {
        this.multicastIP = multicastIP;
    }

    /**
	 * @param persistenceManager
	 */
    public void setPersistenceManager(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    /**
	 * <p>We manifest here, the local mode. If we put true we do not start this service
	 * and we explicitly invoke in a JMX console. For false value, we start this service.
	 * Default value it is false, nevertheless in configuration provided by <code>NexOpen Framework</code>
	 * we use as default value true (in case not found the property <code>openfrwk.local.mode</code> 
	 * in a properties file called <code>cluster.properties</code> at the root of the classpath)</p>
	 * 
	 * @param localMode
	 */
    public void setLocalMode(boolean localMode) {
        this.localMode.set(localMode);
    }

    public boolean isLocalMode() {
        return this.localMode.get();
    }

    /**
	 * 
	 * @see org.nexopenframework.services.ha.ServiceClusterMBean#getHANotificationBroadcaster()
	 */
    public HANotificationBroadcaster getHANotificationBroadcaster() {
        return this;
    }

    /**
	 * 
	 * @see org.nexopenframework.services.ha.ServiceClusterMBean#getNodes()
	 */
    public List getNodes() {
        List menbers = (List) this.notificationBus.getMembership().clone();
        List nodes = new ArrayList(menbers.size());
        Iterator it_menbers = menbers.iterator();
        while (it_menbers.hasNext()) {
            Address address = (Address) it_menbers.next();
            nodes.add(address.toString());
        }
        return nodes;
    }

    /**
	 * @see org.jgroups.blocks.NotificationBus.Consumer#getCache()
	 */
    public Serializable getCache() {
        return (Serializable) this.registry.getStates();
    }

    /**
	 * @see org.jgroups.blocks.NotificationBus.Consumer#handleNotification(java.io.Serializable)
	 */
    public final void handleNotification(Serializable notification) {
        if (logger.isInfoEnabled()) {
            logger.info("Received notification :: " + notification);
        }
        if (notification instanceof StateChangeNotification) {
            handleStateChangeNotification(notification);
        } else if (notification instanceof AttributeChangeNotification) {
            handleAttributeChangeNotification(notification);
        } else if (notification instanceof OperationInvokedNotification) {
            handleOperationInvokedNotification(notification);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("[Address :: " + getCurrentNode() + "] Received notification not contempled :: " + notification);
            }
        }
    }

    /**
	 * @see org.jgroups.blocks.NotificationBus.Consumer#memberJoined(org.jgroups.Address)
	 */
    public void memberJoined(Address address) {
        if (logger.isInfoEnabled()) {
            logger.info("[Address :: " + getCurrentNode() + "] Member joined :: " + address);
        }
    }

    /**
	 * 
	 * @see org.jgroups.blocks.NotificationBus.Consumer#memberLeft(org.jgroups.Address)
	 */
    public void memberLeft(Address address) {
        if (logger.isInfoEnabled()) {
            logger.info("[Address :: " + getCurrentNode() + "] Member left :: " + address);
        }
    }

    public void create() {
        this.state.set(Service.SERVICE_CREATED);
    }

    public void destroy() {
        this.state.set(Service.SERVICE_DESTROYED);
    }

    public String getName() {
        return "Service Cluster JGroups";
    }

    public int getCurrentState() {
        return this.state.get();
    }

    /**
	 * <p>start the JGroups channel if we are not in local mode. In local mode we do not start this service
	 * till we explicitly invoke</p>
	 * 
	 * @see org.nexopenframework.services.ServiceLifecycle#start()
	 * @see NotificationBus#setConsumer(Consumer)
	 * @see Channel#setOpt(int, Object)
	 * @see #setLocalMode(boolean)
	 */
    public void start() throws ServiceException {
        if (isLocalMode()) {
            if (logger.isInfoEnabled()) {
                logger.info("[INFO] We are in local mode, we do not start this service. Current service state " + this.state.get());
                logger.info("[INFO] In local mode, we do not create the JGroups channel.");
                logger.info("[INFO] If you plan to use this service in a cluster environment (integration or production), " + "please go to your JMX console and change to false the local mode [setLocalMode(false)] " + "and invoke start method.");
            }
            return;
        }
        if (this.jgProperties != null && this.jgProperties.startsWith(DEFAULT_CHANNEL_PROPERTIES_PRE)) {
            int indexPre = this.jgProperties.indexOf(DEFAULT_CHANNEL_PROPERTIES_PRE);
            int indexPost = this.jgProperties.indexOf(DEFAULT_CHANNEL_PROPERTIES_POST);
            if (indexPost > -1) {
                this.multicastIP = this.jgProperties.substring(indexPre, indexPost);
            }
        }
        if (this.multicastIP != null && this.jgProperties == null) {
            StringBuffer sb = new StringBuffer(DEFAULT_CHANNEL_PROPERTIES_PRE);
            sb.append(this.multicastIP.trim()).append(DEFAULT_CHANNEL_PROPERTIES_POST);
            this.jgProperties = sb.toString();
        }
        if (this.jgProperties == null) {
            StringBuffer sb = new StringBuffer(DEFAULT_CHANNEL_PROPERTIES_PRE);
            sb.append(DEFAULT_MULTICAST_IP).append(DEFAULT_CHANNEL_PROPERTIES_POST);
            this.jgProperties = sb.toString();
        }
        try {
            this.notificationBus = new NotificationBus(this.clusterName, this.jgProperties);
            this.notificationBus.setConsumer(this);
            this.notificationBus.getChannel().setOpt(Channel.LOCAL, Boolean.TRUE);
            this.notificationBus.start();
            if (this.persistenceManager != null) {
                this.persistenceManager.create();
                this.persistenceManager.start();
            }
            this.registry = new ServiceStateRegistry();
            if (this.notificationBus.isCoordinator()) {
                if (this.persistenceManager != null) {
                    Map states = this.persistenceManager.retrieveAll();
                    notifyServiceStates(states);
                } else {
                    if (logger.isInfoEnabled()) {
                        logger.info("JGroups Persistence Manager not available. " + "No synchronization with persistence storage available");
                    }
                }
            }
            Serializable cache = this.notificationBus.getCacheFromCoordinator(timeout, 1);
            if (cache instanceof Map) {
                Map states = (Map) cache;
                notifyServiceStates(states);
            }
        } catch (InstanceNotFoundException e) {
            if (logger.isInfoEnabled()) {
                logger.info("JGroups NotificationBus exception at start :: JMX Instance not found", e);
            }
        } catch (Exception e) {
            if (logger.isInfoEnabled()) {
                logger.info("JGroups NotificationBus exception at start of this service", e);
            }
            throw new ServiceException("Problems statring the Notification Bus", e);
        }
        if (isJmxPresent) {
            registerChannel();
        }
        this.state.set(Service.SERVICE_STARTED);
    }

    public void stop() {
        if (this.persistenceManager != null) {
            this.persistenceManager.stop();
            this.persistenceManager.destroy();
        }
        if (this.notificationBus != null) {
            try {
                MBeanServer server = MBeanServerLocator.locateMBeanServer();
                Channel channel = this.notificationBus.getChannel();
                ObjectName channelName = this.getObjectName(channel);
                server.unregisterMBean(channelName);
                this.unregisterProtocols();
            } catch (JMException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("JMX Exception in stop, just printing", e);
                }
            } finally {
                this.notificationBus.stop();
                this.notificationBus = null;
            }
        }
        this.state.set(Service.SERVICE_STOPPED);
    }

    public boolean isStarted() {
        return this.state.get() == Service.SERVICE_STARTED;
    }

    /**
	 * 
	 * @see org.nexopenframework.services.ha.HANotificationBroadcaster#sendNotification(org.nexopenframework.services.ha.HANotification)
	 */
    public void sendNotification(HANotification notification) {
        {
            Assert.notNull(notification, "HANotification MUST be different from null");
        }
        if (!isStarted()) {
            return;
        }
        try {
            if (this.isOpen()) {
                this.notificationBus.sendNotification(notification);
            } else {
                if (logger.isWarnEnabled()) {
                    logger.warn("Channel closed detected. Not sending notification [" + notification + "]");
                }
            }
        } catch (SecurityException e) {
            if (logger.isInfoEnabled()) {
                logger.info("No notification send. Security exception arised", e);
            }
        } catch (RuntimeException e) {
            if (logger.isInfoEnabled()) {
                logger.info("Runtime Exception :: " + e.getMessage(), e);
            }
        }
    }

    /**
	 * @param notification
	 */
    protected void handleAttributeChangeNotification(Serializable notification) {
        try {
            AttributeChangeNotification _notification = (AttributeChangeNotification) notification;
            ObjectName service = _notification.getService();
            String propertyName = _notification.getProperty();
            Serializable value = _notification.getValue();
            if (logger.isInfoEnabled()) {
                logger.info("[Address :: " + getCurrentNode() + "] Received Attribute change notification");
            }
            updateDistributedAttribute(service, propertyName, value);
            this.registry.addProperty(service, propertyName, value);
            boolean persistent = _notification.isPersistent();
            if (persistent && this.notificationBus.isCoordinator()) {
                String key = new StringBuffer("").append(service).append("#").append(propertyName).toString();
                if (this.persistenceManager != null) {
                    this.persistenceManager.save(key, value);
                } else {
                    if (logger.isWarnEnabled()) {
                        logger.warn("[Address :: " + getCurrentNode() + "] Property :: " + propertyName + " marked as persistable but JGroups persistence manager is null");
                    }
                }
            }
        } catch (InstanceNotFoundException e) {
            throw new ServiceRuntimeException("Instance NOT found", e);
        } catch (MBeanException e) {
            throw new ServiceRuntimeException("MBean Exception", e);
        } catch (ReflectionException e) {
            throw new ServiceRuntimeException("Reflection exception", e);
        } catch (RuntimeException e) {
            throw new ServiceRuntimeException(e);
        } catch (CannotPersistException e) {
            throw new ServiceRuntimeException("Persistent problems achieved", e);
        }
    }

    /**
	 * <p></p>
	 * 
	 * @see DistributedUpdatableMBean#updateDistributedOperation(String, Serializable[], String[])
	 * @param notification
	 */
    protected void handleOperationInvokedNotification(Serializable notification) {
        try {
            OperationInvokedNotification _notification = (OperationInvokedNotification) notification;
            MBeanServer server = MBeanServerLocator.locateMBeanServer();
            ObjectName objName = _notification.getService();
            String operationName = _notification.getOperationName();
            Serializable[] values = _notification.getValues();
            String[] arguments = _notification.getArguments();
            server.invoke(objName, "updateDistributedOperation", new Object[] { operationName, values, arguments }, new String[] { "java.lang.String", java.io.Serializable[].class.getName(), java.lang.String[].class.getName() });
        } catch (InstanceNotFoundException e) {
            throw new ServiceRuntimeException("Instance NOT found", e);
        } catch (MBeanException e) {
            throw new ServiceRuntimeException("MBean Exception", e);
        } catch (ReflectionException e) {
            throw new ServiceRuntimeException("Reflection exception", e);
        }
    }

    /**
	 * @param notification
	 */
    protected void handleStateChangeNotification(Serializable notification) {
        try {
            if (logger.isInfoEnabled()) {
                logger.info("[Address :: " + getCurrentNode() + "] Received State change notification");
            }
            StateChangeNotification _notification = (StateChangeNotification) notification;
            ObjectName service = _notification.getService();
            int newState = _notification.getNewState();
            updateDistributedState(service, newState);
            this.registry.addState(service, newState);
        } catch (InstanceNotFoundException e) {
            throw new ServiceRuntimeException("Instance NOT found", e);
        } catch (MBeanException e) {
            throw new ServiceRuntimeException("MBean Exception", e);
        } catch (ReflectionException e) {
            throw new ServiceRuntimeException("Reflection exception", e);
        } catch (RuntimeException e) {
            throw new ServiceRuntimeException(e);
        }
    }

    /**
	 * <p>asserts if the channel is opened. this method is in order to avoid
	 * sending notifications to a closed channel</p>
	 * 
	 * @return boolean refering if the channel is open or not
	 * @see Channel#isOpen()
	 */
    protected boolean isOpen() {
        Channel channel = this.notificationBus.getChannel();
        boolean open = channel.isOpen();
        return open;
    }

    /**
	 * <p>Retrieve a default object name for this MBean</p>
	 * 
	 * @see #DEFAULT_MBEAN_NAME
	 * @return a default object name for this MBean
	 */
    private String _getObjectName() {
        ComponentModuleConfig smc = ComponentModuleConfig.getInstance();
        if (smc != null) {
            StringBuffer sb = new StringBuffer(DEFAULT_MBEAN_NAME);
            sb.append(",module=").append(smc.getName());
            return sb.toString();
        }
        return DEFAULT_MBEAN_NAME;
    }

    /**
	 * @param states
	 * @throws InstanceNotFoundException
	 * @throws MBeanException
	 * @throws ReflectionException
	 */
    private void notifyServiceStates(Map states) throws InstanceNotFoundException, MBeanException, ReflectionException {
        this.registry.putAllStates(states);
        Iterator it_states = states.keySet().iterator();
        while (it_states.hasNext()) {
            ObjectName service = (ObjectName) it_states.next();
            if (logger.isInfoEnabled()) {
                logger.info("Updating state and properties related to Service :: " + service);
            }
            ServiceState serviceState = (ServiceState) states.get(service);
            if (serviceState.getStatus() != Service.SERVICE_UNREGISTERED) {
                this.updateDistributedState(service, serviceState.getStatus());
            }
            Map properties = serviceState.getProperties();
            Iterator it_properties = properties.keySet().iterator();
            while (it_properties.hasNext()) {
                String propertyName = (String) it_properties.next();
                Serializable value = (Serializable) properties.get(propertyName);
                this.updateDistributedAttribute(service, propertyName, value);
            }
            if (logger.isInfoEnabled()) {
                logger.info("Updated Service :: " + service);
            }
        }
    }

    /**
	 * @param service
	 * @param newState
	 * @throws InstanceNotFoundException
	 * @throws MBeanException
	 * @throws ReflectionException
	 */
    private void updateDistributedState(ObjectName service, int newState) throws InstanceNotFoundException, MBeanException, ReflectionException {
        MBeanServer server = MBeanServerLocator.locateMBeanServer();
        server.invoke(service, "updateDistributedState", new Object[] { new Integer(newState) }, new String[] { "java.lang.Integer" });
    }

    /**
	 * @param service
	 * @param propertyName
	 * @param value
	 * @throws InstanceNotFoundException
	 * @throws MBeanException
	 * @throws ReflectionException
	 */
    private void updateDistributedAttribute(ObjectName service, String propertyName, Serializable value) throws InstanceNotFoundException, MBeanException, ReflectionException {
        MBeanServer server = MBeanServerLocator.locateMBeanServer();
        server.invoke(service, "updateDistributedAttribute", new Object[] { propertyName, value }, new String[] { "java.lang.String", "java.io.Serializable" });
    }

    /**
	 * <p>Register of {@link JChannel} and protocols in a JMX {@link MBeanServer}</p>
	 */
    private void registerChannel() {
        try {
            JChannel channel = (JChannel) this.notificationBus.getChannel();
            ObjectName channelName = this.getObjectName(channel);
            MBeanServer server = MBeanServerLocator.locateMBeanServer();
            Class jmxConfigurator = ClassUtils.forName("org.jgroups.jmx.JmxConfigurator");
            Class[] parameterTypes = new Class[] { JChannel.class, MBeanServer.class, String.class, boolean.class };
            Method registerChannel = jmxConfigurator.getMethod("registerChannel", parameterTypes);
            Object[] values = new Object[] { channel, server, channelName.getCanonicalName(), Boolean.TRUE };
            registerChannel.invoke(null, values);
        } catch (Exception e) {
            if (e instanceof InvocationTargetException) {
                InvocationTargetException ite = (InvocationTargetException) e;
                Throwable cause = ite.getTargetException();
                if (cause instanceof JMException) {
                    if (logger.isInfoEnabled()) {
                        logger.info("JMX exception at start of this service registering the JChannel :: " + cause.getClass().getName());
                        logger.info("Message of this exception :: " + cause.getMessage());
                    }
                } else {
                    throw new ServiceRuntimeException(cause);
                }
            } else {
                throw new ServiceRuntimeException(e);
            }
        }
    }

    private void unregisterProtocols() {
    }

    /**
	 * <p>Create a {@link ObjectName} for this JGroups channel</p>
	 * 
	 * @param channel the jgroups channel
	 * @return a suitable JMX object name for this JGroups channel
	 * @throws JMException
	 */
    private ObjectName getObjectName(Channel channel) throws JMException {
        ComponentModuleConfig smc = ComponentModuleConfig.getInstance();
        if (smc != null) {
            Hashtable ht = new Hashtable(2);
            ht.put("channel", channel.getChannelName());
            ht.put("module", smc.getName());
            ObjectName objName = ObjectName.getInstance("openfrwk.core", ht);
            return objName;
        }
        return ObjectName.getInstance("openfrwk.core", "channel", channel.getChannelName());
    }
}

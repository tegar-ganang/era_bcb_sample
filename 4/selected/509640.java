package com.sun.sgs.test.impl.profile;

import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.NameNotBoundException;
import com.sun.sgs.auth.Identity;
import com.sun.sgs.impl.auth.IdentityImpl;
import com.sun.sgs.impl.profile.ProfileCollectorImpl;
import com.sun.sgs.kernel.ComponentRegistry;
import com.sun.sgs.kernel.NodeType;
import com.sun.sgs.kernel.TransactionScheduler;
import com.sun.sgs.management.ChannelServiceMXBean;
import com.sun.sgs.management.ClientSessionServiceMXBean;
import com.sun.sgs.management.ConfigMXBean;
import com.sun.sgs.management.DataServiceMXBean;
import com.sun.sgs.management.DataStoreStatsMXBean;
import com.sun.sgs.management.NodeInfo;
import com.sun.sgs.management.NodeMappingServiceMXBean;
import com.sun.sgs.management.NodesMXBean;
import com.sun.sgs.management.ProfileControllerMXBean;
import com.sun.sgs.management.TaskServiceMXBean;
import com.sun.sgs.management.WatchdogServiceMXBean;
import com.sun.sgs.profile.ProfileCollector;
import com.sun.sgs.profile.ProfileCollector.ProfileLevel;
import com.sun.sgs.profile.ProfileConsumer;
import com.sun.sgs.service.NodeMappingService;
import com.sun.sgs.test.util.DummyManagedObject;
import com.sun.sgs.test.util.SgsTestNode;
import com.sun.sgs.test.util.TestAbstractKernelRunnable;
import com.sun.sgs.tools.test.FilteredNameRunner;
import java.lang.management.ManagementFactory;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigInteger;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for management beans.
 */
@RunWith(FilteredNameRunner.class)
public class TestMBeans {

    private static final String APP_NAME = "TestMBeans";

    /** A test server node */
    private SgsTestNode serverNode;

    /** The profile collector associated with the test server node */
    private ProfileCollector profileCollector;

    /** The system registry */
    private ComponentRegistry systemRegistry;

    /** The transaction scheduler. */
    private TransactionScheduler txnScheduler;

    /** The owner for tasks I initiate. */
    private Identity taskOwner;

    /** JMX connection server */
    private static JMXConnectorServer cs;

    /** JMX connector */
    private static JMXConnector cc;

    /** MBean server connection */
    private static MBeanServerConnection mbsc;

    /** Any additional nodes, only used for selected tests */
    private SgsTestNode additionalNodes[];

    /** Test setup. */
    @BeforeClass
    public static void first() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");
        cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
        cs.start();
        cc = JMXConnectorFactory.connect(cs.getAddress());
        mbsc = cc.getMBeanServerConnection();
        ObjectName query = new ObjectName("com.sun.sgs*:*");
        Set<ObjectName> names = mbsc.queryNames(query, null);
        for (ObjectName name : names) {
            System.out.println("BEFORECLASS unregistering : " + name);
            mbsc.unregisterMBean(name);
        }
    }

    @Before
    public void setUp() throws Exception {
        Properties props = SgsTestNode.getDefaultProperties(APP_NAME, null, null);
        props.setProperty(ProfileCollectorImpl.CREATE_MBEAN_SERVER_PROPERTY, "false");
        serverNode = new SgsTestNode(APP_NAME, null, props);
        profileCollector = getCollector(serverNode);
        systemRegistry = serverNode.getSystemRegistry();
        txnScheduler = systemRegistry.getComponent(TransactionScheduler.class);
        taskOwner = serverNode.getProxy().getCurrentOwner();
    }

    /** Shut down the nodes and shut down JMX. */
    @After
    public void tearDown() throws Exception {
        if (additionalNodes != null) {
            for (SgsTestNode node : additionalNodes) {
                if (node != null) {
                    node.shutdown(false);
                }
            }
            additionalNodes = null;
        }
        serverNode.shutdown(true);
    }

    @AfterClass
    public static void last() throws Exception {
        if (cc != null) {
            cc.close();
        }
        cs.stop();
    }

    /** 
     * Add additional nodes.  We only do this as required by the tests. 
     *
     * @param props properties for node creation, or {@code null} if default
     *     properties should be used
     * @parm num the number of nodes to add
     */
    private void addNodes(Properties props, int num) throws Exception {
        additionalNodes = new SgsTestNode[num];
        for (int i = 0; i < num; i++) {
            SgsTestNode node = new SgsTestNode(serverNode, null, props);
            additionalNodes[i] = node;
        }
    }

    /** Returns the profile collector for a given node */
    private ProfileCollector getCollector(SgsTestNode node) throws Exception {
        return node.getSystemRegistry().getComponent(ProfileCollector.class);
    }

    @Test
    public void testRegisterMBean() throws Exception {
        SimpleTestMBean bean1 = new SimpleTest();
        String beanName = "com.sun.sgs:type=Test";
        profileCollector.registerMBean(bean1, beanName);
        SimpleTestMBean bean2 = (SimpleTestMBean) profileCollector.getRegisteredMBean(beanName);
        bean1.setSomething(55);
        assertEquals(bean1.getSomething(), bean2.getSomething());
        SimpleTestMBean proxy = JMX.newMBeanProxy(mbsc, new ObjectName(beanName), SimpleTestMBean.class);
        proxy.clearSomething();
        assertEquals(0, bean1.getSomething());
        assertEquals(0, bean2.getSomething());
        assertEquals(0, proxy.getSomething());
    }

    @Test
    public void testRegisterMXBean() throws Exception {
        TestMXBean bean1 = new TestMXImpl();
        String beanName = "com.sun.sgs:type=Test";
        profileCollector.registerMBean(bean1, beanName);
        TestMXBean bean2 = (TestMXBean) profileCollector.getRegisteredMBean(beanName);
        bean1.setSomething(55);
        assertEquals(bean1.getSomething(), bean2.getSomething());
        TestMXBean proxy = JMX.newMXBeanProxy(mbsc, new ObjectName(beanName), TestMXBean.class);
        proxy.clearSomething();
        assertEquals(0, bean1.getSomething());
        assertEquals(0, bean2.getSomething());
        assertEquals(0, proxy.getSomething());
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterNullBean() throws Exception {
        profileCollector.registerMBean(null, "com.sun.sgs:type=TEST");
    }

    @Test(expected = JMException.class)
    public void testRegisterBadBean() throws Exception {
        profileCollector.registerMBean(new String("bad"), "com.sun.sgs:type=Bad");
    }

    @Test(expected = NullPointerException.class)
    public void testGetRegisteredMBeanNull() {
        Object o = profileCollector.getRegisteredMBean(null);
    }

    @Test
    public void testGetRegisteredMBeanNotThere() {
        Object o = profileCollector.getRegisteredMBean("notFound");
        assertNull(o);
    }

    @Test
    public void testMBeanShutdown() throws Exception {
        TestMXBean bean1 = new TestMXImpl();
        TestMXBean bean2 = new TestMXImpl();
        String beanName = "com.sun.sgs:type=Test";
        String otherName = "com.sun.sgs:type=AnotherName";
        profileCollector.registerMBean(bean1, beanName);
        profileCollector.registerMBean(bean2, otherName);
        TestMXBean proxy1 = JMX.newMXBeanProxy(mbsc, new ObjectName(beanName), TestMXBean.class);
        TestMXBean proxy2 = JMX.newMXBeanProxy(mbsc, new ObjectName(otherName), TestMXBean.class);
        proxy1.setSomething(55);
        proxy2.setSomething(56);
        assertEquals(55, bean1.getSomething());
        assertEquals(56, proxy2.getSomething());
        profileCollector.shutdown();
        Object o = profileCollector.getRegisteredMBean(beanName);
        assertNull(o);
        o = profileCollector.getRegisteredMBean(otherName);
        assertNull(o);
        proxy1 = JMX.newMXBeanProxy(mbsc, new ObjectName(beanName), TestMXBean.class);
        try {
            int value = proxy1.getSomething();
        } catch (UndeclaredThrowableException e) {
            Throwable cause = e.getCause();
            System.out.println(e.getCause());
            assertEquals(InstanceNotFoundException.class, cause.getClass());
        }
        proxy2 = JMX.newMXBeanProxy(mbsc, new ObjectName(otherName), TestMXBean.class);
        try {
            int value = proxy2.getSomething();
        } catch (UndeclaredThrowableException e) {
            Throwable cause = e.getCause();
            System.out.println(e.getCause());
            assertEquals(InstanceNotFoundException.class, cause.getClass());
        }
    }

    @Test
    public void testNodesMXBean() throws Exception {
        ObjectName name = new ObjectName(NodesMXBean.MXBEAN_NAME);
        NodesMXBean bean = (NodesMXBean) profileCollector.getRegisteredMBean(NodesMXBean.MXBEAN_NAME);
        assertNotNull(bean);
        CompositeData[] nodesData = (CompositeData[]) mbsc.getAttribute(name, "Nodes");
        assertEquals(1, nodesData.length);
        NodesMXBean proxy = JMX.newMXBeanProxy(mbsc, name, NodesMXBean.class, true);
        NodeInfo[] nodes = proxy.getNodes();
        for (NodeInfo n : nodes) {
            System.out.println("found node: " + n + n.getId());
            assertTrue(n.isLive());
        }
        assertEquals(1, bean.getNodes().length);
        assertEquals(1, nodes.length);
        addNodes(null, 2);
        nodes = proxy.getNodes();
        assertEquals(3, nodes.length);
        for (NodeInfo n : nodes) {
            System.out.println("found node: " + n + n.getId());
            assertTrue(n.isLive());
        }
        NotificationEmitter notifyProxy = (NotificationEmitter) proxy;
        TestJMXNotificationListener listener = new TestJMXNotificationListener();
        notifyProxy.addNotificationListener(listener, null, null);
        SgsTestNode node3 = null;
        try {
            node3 = new SgsTestNode(serverNode, null, null);
            AtomicLong count = listener.notificationMap.get(NodesMXBean.NODE_STARTED_NOTIFICATION);
            assertNotNull(count);
            assertEquals(1, count.longValue());
            assertNull(listener.notificationMap.get(NodesMXBean.NODE_FAILED_NOTIFICATION));
        } finally {
            if (node3 != null) {
                node3.shutdown(false);
            }
            int renewTime = Integer.valueOf(serverNode.getServiceProperties().getProperty("com.sun.sgs.impl.service.watchdog.server.renew.interval"));
            Thread.sleep(renewTime * 2);
            AtomicLong count = listener.notificationMap.get(NodesMXBean.NODE_FAILED_NOTIFICATION);
            assertNotNull(count);
            assertEquals(1, count.longValue());
        }
        notifyProxy.removeNotificationListener(listener);
    }

    @Test
    public void testConfigMXBean() throws Exception {
        ObjectName name = new ObjectName(ConfigMXBean.MXBEAN_NAME);
        ConfigMXBean bean = (ConfigMXBean) profileCollector.getRegisteredMBean(ConfigMXBean.MXBEAN_NAME);
        assertNotNull(bean);
        String appListener = (String) mbsc.getAttribute(name, "AppListener");
        String appName = (String) mbsc.getAttribute(name, "AppName");
        String hostName = (String) mbsc.getAttribute(name, "HostName");
        String appRoot = (String) mbsc.getAttribute(name, "AppRoot");
        int jmxPort = (Integer) mbsc.getAttribute(name, "JmxPort");
        NodeType type = NodeType.valueOf((String) mbsc.getAttribute(name, "NodeType"));
        String serverHost = (String) mbsc.getAttribute(name, "ServerHostName");
        long timeout = (Long) mbsc.getAttribute(name, "StandardTxnTimeout");
        String desc = (String) mbsc.getAttribute(name, "ProtocolDescriptor");
        System.out.println("This node's data:");
        System.out.println("  node type: " + type);
        System.out.println("  app listener: " + appListener);
        System.out.println("  app name: " + appName);
        System.out.println("  app root: " + appRoot);
        System.out.println("  txn timeout:" + timeout);
        System.out.println("  host name: " + hostName);
        System.out.println("  jmx port: " + jmxPort);
        System.out.println("  server host:" + serverHost);
        System.out.println("  protocol descriptor:" + desc);
        ConfigMXBean proxy = JMX.newMXBeanProxy(mbsc, name, ConfigMXBean.class);
        assertEquals(appListener, proxy.getAppListener());
        assertEquals(appName, proxy.getAppName());
        assertEquals(hostName, proxy.getHostName());
        assertEquals(appRoot, proxy.getAppRoot());
        assertEquals(jmxPort, proxy.getJmxPort());
        assertEquals(type, proxy.getNodeType());
        assertEquals(serverHost, proxy.getServerHostName());
        assertEquals(timeout, proxy.getStandardTxnTimeout());
        assertEquals(desc, proxy.getProtocolDescriptor());
        assertEquals(appListener, bean.getAppListener());
        assertEquals(appName, bean.getAppName());
        assertEquals(hostName, bean.getHostName());
        assertEquals(appRoot, bean.getAppRoot());
        assertEquals(jmxPort, bean.getJmxPort());
        assertEquals(type, bean.getNodeType());
        assertEquals(serverHost, bean.getServerHostName());
        assertEquals(timeout, bean.getStandardTxnTimeout());
        assertEquals(desc, bean.getProtocolDescriptor());
    }

    @Test
    public void testDataStoreStatsMXBean() throws Exception {
        ProfileConsumer cons = getCollector(serverNode).getConsumer(ProfileCollectorImpl.CORE_CONSUMER_PREFIX + "DataStore");
        cons.setProfileLevel(ProfileLevel.MAX);
        ObjectName name = new ObjectName(DataStoreStatsMXBean.MXBEAN_NAME);
        DataStoreStatsMXBean bean = (DataStoreStatsMXBean) profileCollector.getRegisteredMBean(DataStoreStatsMXBean.MXBEAN_NAME);
        assertNotNull(bean);
        long createObject = (Long) mbsc.getAttribute(name, "CreateObjectCalls");
        long getBinding = (Long) mbsc.getAttribute(name, "GetBindingCalls");
        long getClassId = (Long) mbsc.getAttribute(name, "GetClassIdCalls");
        long getClassInfo = (Long) mbsc.getAttribute(name, "GetClassInfoCalls");
        long getObject = (Long) mbsc.getAttribute(name, "GetObjectCalls");
        long getObjectForUpdateCalls = (Long) mbsc.getAttribute(name, "GetObjectForUpdateCalls");
        long markForUpdate = (Long) mbsc.getAttribute(name, "MarkForUpdateCalls");
        long nextBoundName = (Long) mbsc.getAttribute(name, "NextBoundNameCalls");
        long nextObjectId = (Long) mbsc.getAttribute(name, "NextObjectIdCalls");
        long removeBinding = (Long) mbsc.getAttribute(name, "RemoveBindingCalls");
        long removeObject = (Long) mbsc.getAttribute(name, "RemoveObjectCalls");
        long setBinding = (Long) mbsc.getAttribute(name, "SetBindingCalls");
        long setObject = (Long) mbsc.getAttribute(name, "SetObjectCalls");
        long setObjects = (Long) mbsc.getAttribute(name, "SetObjectsCalls");
        double avgRead = (Double) mbsc.getAttribute(name, "AvgReadBytesSample");
        long minRead = (Long) mbsc.getAttribute(name, "MinReadBytesSample");
        long maxRead = (Long) mbsc.getAttribute(name, "MaxReadBytesSample");
        long readBytes = (Long) mbsc.getAttribute(name, "ReadBytesCount");
        long readObjs = (Long) mbsc.getAttribute(name, "ReadObjectsCount");
        double avgWritten = (Double) mbsc.getAttribute(name, "AvgWrittenBytesSample");
        long minWritten = (Long) mbsc.getAttribute(name, "MinWrittenBytesSample");
        long maxWritten = (Long) mbsc.getAttribute(name, "MaxWrittenBytesSample");
        long writtenBytes = (Long) mbsc.getAttribute(name, "WrittenBytesCount");
        long writtenObjs = (Long) mbsc.getAttribute(name, "WrittenObjectsCount");
        DataStoreStatsMXBean proxy = JMX.newMXBeanProxy(mbsc, name, DataStoreStatsMXBean.class);
        assertTrue(createObject <= proxy.getCreateObjectCalls());
        assertTrue(getBinding <= proxy.getGetBindingCalls());
        assertTrue(getClassId <= proxy.getGetClassIdCalls());
        assertTrue(getClassInfo <= proxy.getGetClassInfoCalls());
        assertTrue(getObject <= proxy.getGetObjectCalls());
        assertTrue(getObjectForUpdateCalls <= proxy.getGetObjectForUpdateCalls());
        assertTrue(markForUpdate <= proxy.getMarkForUpdateCalls());
        assertTrue(nextBoundName <= proxy.getNextBoundNameCalls());
        assertTrue(nextObjectId <= proxy.getNextObjectIdCalls());
        assertTrue(removeBinding <= proxy.getRemoveBindingCalls());
        assertTrue(removeObject <= proxy.getRemoveObjectCalls());
        assertTrue(setBinding <= proxy.getSetBindingCalls());
        assertTrue(setObject <= proxy.getSetObjectCalls());
        assertTrue(setObjects <= proxy.getSetObjectsCalls());
        txnScheduler.runTask(new TestAbstractKernelRunnable() {

            public void run() {
                ManagedObject dummy = new DummyManagedObject();
                serverNode.getDataService().setBinding("dummy", dummy);
            }
        }, taskOwner);
        assertTrue(createObject < proxy.getCreateObjectCalls());
        assertTrue(writtenBytes < proxy.getWrittenBytesCount());
        assertTrue(writtenObjs < proxy.getWrittenObjectsCount());
        assertTrue(createObject < bean.getCreateObjectCalls());
        assertTrue(writtenBytes < bean.getWrittenBytesCount());
        assertTrue(writtenObjs < bean.getWrittenObjectsCount());
    }

    @Test
    public void testDataServiceMXBean() throws Exception {
        ProfileConsumer cons = getCollector(serverNode).getConsumer(ProfileCollectorImpl.CORE_CONSUMER_PREFIX + "DataService");
        cons.setProfileLevel(ProfileLevel.MAX);
        ObjectName name = new ObjectName(DataServiceMXBean.MXBEAN_NAME);
        DataServiceMXBean bean = (DataServiceMXBean) profileCollector.getRegisteredMBean(DataServiceMXBean.MXBEAN_NAME);
        assertNotNull(bean);
        long createRef = (Long) mbsc.getAttribute(name, "CreateReferenceCalls");
        long createRefForId = (Long) mbsc.getAttribute(name, "CreateReferenceForIdCalls");
        long getBinding = (Long) mbsc.getAttribute(name, "GetBindingCalls");
        long getLocalNodeId = (Long) mbsc.getAttribute(name, "GetLocalNodeIdCalls");
        long getServiceBinding = (Long) mbsc.getAttribute(name, "GetServiceBindingCalls");
        long markForUpdate = (Long) mbsc.getAttribute(name, "MarkForUpdateCalls");
        long nextBoundName = (Long) mbsc.getAttribute(name, "NextBoundNameCalls");
        long nextObjectId = (Long) mbsc.getAttribute(name, "NextObjectIdCalls");
        long nextServiceBoundName = (Long) mbsc.getAttribute(name, "NextServiceBoundNameCalls");
        long removeBinding = (Long) mbsc.getAttribute(name, "RemoveBindingCalls");
        long removeObject = (Long) mbsc.getAttribute(name, "RemoveObjectCalls");
        long removeServiceBinding = (Long) mbsc.getAttribute(name, "RemoveServiceBindingCalls");
        long setBinding = (Long) mbsc.getAttribute(name, "SetBindingCalls");
        long setServiceBinding = (Long) mbsc.getAttribute(name, "SetServiceBindingCalls");
        DataServiceMXBean proxy = JMX.newMXBeanProxy(mbsc, name, DataServiceMXBean.class);
        assertTrue(createRef <= proxy.getCreateReferenceCalls());
        assertTrue(createRefForId <= proxy.getCreateReferenceForIdCalls());
        assertTrue(getBinding <= proxy.getGetBindingCalls());
        assertTrue(getLocalNodeId <= proxy.getGetLocalNodeIdCalls());
        assertTrue(getServiceBinding <= proxy.getGetServiceBindingCalls());
        assertTrue(markForUpdate <= proxy.getMarkForUpdateCalls());
        assertTrue(nextBoundName <= proxy.getNextBoundNameCalls());
        assertTrue(nextObjectId <= proxy.getNextObjectIdCalls());
        assertTrue(nextServiceBoundName <= proxy.getNextServiceBoundNameCalls());
        assertTrue(removeBinding <= proxy.getRemoveBindingCalls());
        assertTrue(removeObject <= proxy.getRemoveObjectCalls());
        assertTrue(removeServiceBinding <= proxy.getRemoveServiceBindingCalls());
        assertTrue(setBinding <= proxy.getSetBindingCalls());
        assertTrue(setServiceBinding <= proxy.getSetServiceBindingCalls());
        assertTrue(createRef <= bean.getCreateReferenceCalls());
        assertTrue(createRefForId <= bean.getCreateReferenceForIdCalls());
        assertTrue(getBinding <= bean.getGetBindingCalls());
        assertTrue(getLocalNodeId <= bean.getGetLocalNodeIdCalls());
        assertTrue(getServiceBinding <= bean.getGetServiceBindingCalls());
        assertTrue(markForUpdate <= bean.getMarkForUpdateCalls());
        assertTrue(nextBoundName <= bean.getNextBoundNameCalls());
        assertTrue(nextObjectId <= bean.getNextObjectIdCalls());
        assertTrue(nextServiceBoundName <= bean.getNextServiceBoundNameCalls());
        assertTrue(removeBinding <= bean.getRemoveBindingCalls());
        assertTrue(removeObject <= bean.getRemoveObjectCalls());
        assertTrue(removeServiceBinding <= bean.getRemoveServiceBindingCalls());
        assertTrue(setBinding <= bean.getSetBindingCalls());
        assertTrue(setServiceBinding <= proxy.getSetServiceBindingCalls());
        txnScheduler.runTask(new TestAbstractKernelRunnable() {

            public void run() {
                ManagedObject dummy = new DummyManagedObject();
                serverNode.getDataService().setBinding("dummy", dummy);
            }
        }, taskOwner);
        assertTrue(setBinding < proxy.getSetBindingCalls());
        assertTrue(setBinding < bean.getSetBindingCalls());
    }

    @Test
    public void testWatchdogServiceMXBean() throws Exception {
        ProfileConsumer cons = getCollector(serverNode).getConsumer(ProfileCollectorImpl.CORE_CONSUMER_PREFIX + "WatchdogService");
        cons.setProfileLevel(ProfileLevel.MAX);
        ObjectName name = new ObjectName(WatchdogServiceMXBean.MXBEAN_NAME);
        WatchdogServiceMXBean bean = (WatchdogServiceMXBean) profileCollector.getRegisteredMBean(WatchdogServiceMXBean.MXBEAN_NAME);
        assertNotNull(bean);
        long addNodeListener = (Long) mbsc.getAttribute(name, "AddNodeListenerCalls");
        long addRecoveryListener = (Long) mbsc.getAttribute(name, "AddRecoveryListenerCalls");
        long getBackup = (Long) mbsc.getAttribute(name, "GetBackupCalls");
        long getNode = (Long) mbsc.getAttribute(name, "GetNodeCalls");
        long getNodes = (Long) mbsc.getAttribute(name, "GetNodesCalls");
        long isLocalNodeAlive = (Long) mbsc.getAttribute(name, "IsLocalNodeAliveCalls");
        long isLocalNodeAliveNonTransactional = (Long) mbsc.getAttribute(name, "IsLocalNodeAliveNonTransactionalCalls");
        CompositeData getStatusInfo = (CompositeData) mbsc.getAttribute(name, "StatusInfo");
        WatchdogServiceMXBean proxy = JMX.newMXBeanProxy(mbsc, name, WatchdogServiceMXBean.class);
        assertTrue(addNodeListener <= proxy.getAddNodeListenerCalls());
        assertTrue(addRecoveryListener <= proxy.getAddRecoveryListenerCalls());
        assertTrue(getBackup <= proxy.getGetBackupCalls());
        assertTrue(getNode <= proxy.getGetNodeCalls());
        assertTrue(getNodes <= proxy.getGetNodesCalls());
        assertTrue(isLocalNodeAlive <= proxy.getIsLocalNodeAliveCalls());
        assertTrue(isLocalNodeAliveNonTransactional <= proxy.getIsLocalNodeAliveNonTransactionalCalls());
        assertTrue(addNodeListener <= bean.getAddNodeListenerCalls());
        assertTrue(addRecoveryListener <= bean.getAddRecoveryListenerCalls());
        assertTrue(getBackup <= bean.getGetBackupCalls());
        assertTrue(getNode <= bean.getGetNodeCalls());
        assertTrue(getNodes <= bean.getGetNodesCalls());
        assertTrue(isLocalNodeAlive <= bean.getIsLocalNodeAliveCalls());
        assertTrue(isLocalNodeAliveNonTransactional <= bean.getIsLocalNodeAliveNonTransactionalCalls());
        assertTrue((Boolean) getStatusInfo.get("live"));
        assertTrue(proxy.getStatusInfo().isLive());
        assertTrue(bean.getStatusInfo().isLive());
        txnScheduler.runTask(new TestAbstractKernelRunnable() {

            public void run() {
                long nodeId = serverNode.getDataService().getLocalNodeId();
                serverNode.getWatchdogService().getNode(nodeId);
            }
        }, taskOwner);
        long newValue = proxy.getGetNodeCalls();
        assertTrue(getNode < newValue);
        assertTrue(getNode < bean.getGetNodeCalls());
    }

    @Test
    public void testNodeMapServiceMXBean() throws Exception {
        ProfileConsumer cons = getCollector(serverNode).getConsumer(ProfileCollectorImpl.CORE_CONSUMER_PREFIX + "NodeMappingService");
        cons.setProfileLevel(ProfileLevel.MAX);
        ObjectName name = new ObjectName(NodeMappingServiceMXBean.MXBEAN_NAME);
        NodeMappingServiceMXBean bean = (NodeMappingServiceMXBean) profileCollector.getRegisteredMBean(NodeMappingServiceMXBean.MXBEAN_NAME);
        assertNotNull(bean);
        long addNodeMapListener = (Long) mbsc.getAttribute(name, "AddNodeMappingListenerCalls");
        long assignNode = (Long) mbsc.getAttribute(name, "AssignNodeCalls");
        long getIds = (Long) mbsc.getAttribute(name, "GetIdentitiesCalls");
        long getNode = (Long) mbsc.getAttribute(name, "GetNodeCalls");
        long setStatus = (Long) mbsc.getAttribute(name, "SetStatusCalls");
        NodeMappingServiceMXBean proxy = JMX.newMXBeanProxy(mbsc, name, NodeMappingServiceMXBean.class);
        assertTrue(addNodeMapListener <= proxy.getAddNodeMappingListenerCalls());
        assertTrue(assignNode <= proxy.getAssignNodeCalls());
        assertTrue(getIds <= proxy.getGetIdentitiesCalls());
        assertTrue(getNode <= proxy.getGetNodeCalls());
        assertTrue(setStatus <= proxy.getSetStatusCalls());
        serverNode.getNodeMappingService().assignNode(NodeMappingService.class, new IdentityImpl("first"));
        assertTrue(assignNode < proxy.getAssignNodeCalls());
        assertTrue(assignNode < bean.getAssignNodeCalls());
    }

    @Test
    public void testTaskServiceMXBean() throws Exception {
        ProfileConsumer cons = getCollector(serverNode).getConsumer(ProfileCollectorImpl.CORE_CONSUMER_PREFIX + "TaskService");
        cons.setProfileLevel(ProfileLevel.MAX);
        ObjectName name = new ObjectName(TaskServiceMXBean.MXBEAN_NAME);
        TaskServiceMXBean bean = (TaskServiceMXBean) profileCollector.getRegisteredMBean(TaskServiceMXBean.MXBEAN_NAME);
        assertNotNull(bean);
        long delayed = (Long) mbsc.getAttribute(name, "ScheduleDelayedTaskCalls");
        long nondurable = (Long) mbsc.getAttribute(name, "ScheduleNonDurableTaskCalls");
        long nondurableDelayed = (Long) mbsc.getAttribute(name, "ScheduleNonDurableTaskDelayedCalls");
        long periodic = (Long) mbsc.getAttribute(name, "SchedulePeriodicTaskCalls");
        long task = (Long) mbsc.getAttribute(name, "ScheduleTaskCalls");
        TaskServiceMXBean proxy = JMX.newMXBeanProxy(mbsc, name, TaskServiceMXBean.class);
        assertTrue(delayed <= proxy.getScheduleDelayedTaskCalls());
        assertTrue(nondurable <= proxy.getScheduleNonDurableTaskCalls());
        assertTrue(nondurableDelayed <= proxy.getScheduleNonDurableTaskDelayedCalls());
        assertTrue(periodic <= proxy.getSchedulePeriodicTaskCalls());
        assertTrue(task <= proxy.getScheduleTaskCalls());
        txnScheduler.runTask(new TestAbstractKernelRunnable() {

            public void run() {
                serverNode.getTaskService().scheduleNonDurableTask(new TestAbstractKernelRunnable() {

                    public void run() {
                    }
                }, false);
            }
        }, taskOwner);
        assertTrue(nondurable < proxy.getScheduleNonDurableTaskCalls());
        assertTrue(nondurable < bean.getScheduleNonDurableTaskCalls());
    }

    @Test
    public void testSessionServiceMXBean() throws Exception {
        ProfileConsumer cons = getCollector(serverNode).getConsumer(ProfileCollectorImpl.CORE_CONSUMER_PREFIX + "ClientSessionService");
        cons.setProfileLevel(ProfileLevel.MAX);
        ObjectName name = new ObjectName(ClientSessionServiceMXBean.MXBEAN_NAME);
        ClientSessionServiceMXBean bean = (ClientSessionServiceMXBean) profileCollector.getRegisteredMBean(ClientSessionServiceMXBean.MXBEAN_NAME);
        assertNotNull(bean);
        long reg = (Long) mbsc.getAttribute(name, "AddSessionStatusListenerCalls");
        long get = (Long) mbsc.getAttribute(name, "GetSessionProtocolCalls");
        ClientSessionServiceMXBean proxy = JMX.newMXBeanProxy(mbsc, name, ClientSessionServiceMXBean.class);
        assertTrue(reg <= proxy.getAddSessionStatusListenerCalls());
        assertTrue(get <= proxy.getGetSessionProtocolCalls());
        serverNode.getClientSessionService().getSessionProtocol(new BigInteger("555"));
        assertTrue(get < proxy.getGetSessionProtocolCalls());
        assertTrue(get < bean.getGetSessionProtocolCalls());
    }

    @Test
    public void testChannelServiceMXBean() throws Exception {
        ProfileConsumer cons = getCollector(serverNode).getConsumer(ProfileCollectorImpl.CORE_CONSUMER_PREFIX + "ChannelService");
        cons.setProfileLevel(ProfileLevel.MAX);
        ObjectName name = new ObjectName(ChannelServiceMXBean.MXBEAN_NAME);
        ChannelServiceMXBean bean = (ChannelServiceMXBean) profileCollector.getRegisteredMBean(ChannelServiceMXBean.MXBEAN_NAME);
        assertNotNull(bean);
        long create = (Long) mbsc.getAttribute(name, "CreateChannelCalls");
        long get = (Long) mbsc.getAttribute(name, "GetChannelCalls");
        ChannelServiceMXBean proxy = JMX.newMXBeanProxy(mbsc, name, ChannelServiceMXBean.class);
        assertTrue(create <= proxy.getCreateChannelCalls());
        assertTrue(get <= proxy.getGetChannelCalls());
        txnScheduler.runTask(new TestAbstractKernelRunnable() {

            public void run() {
                try {
                    serverNode.getChannelService().getChannel("foo");
                } catch (NameNotBoundException nnb) {
                    System.out.println("Got expected exception " + nnb);
                }
            }
        }, taskOwner);
        assertTrue(get < proxy.getGetChannelCalls());
        assertTrue(get < bean.getGetChannelCalls());
    }

    @Test
    public void testProfileControllerMXBean() throws Exception {
        ProfileControllerMXBean bean = (ProfileControllerMXBean) profileCollector.getRegisteredMBean(ProfileControllerMXBean.MXBEAN_NAME);
        assertNotNull(bean);
        ProfileControllerMXBean proxy = (ProfileControllerMXBean) JMX.newMXBeanProxy(mbsc, new ObjectName(ProfileControllerMXBean.MXBEAN_NAME), ProfileControllerMXBean.class);
        String[] consumers = proxy.getProfileConsumers();
        for (String con : consumers) {
            System.out.println("Found consumer " + con);
        }
        assertEquals(ProfileLevel.MIN, proxy.getDefaultProfileLevel());
        proxy.setDefaultProfileLevel(ProfileLevel.MEDIUM);
        assertEquals(ProfileLevel.MEDIUM, proxy.getDefaultProfileLevel());
        String consName = ProfileCollectorImpl.CORE_CONSUMER_PREFIX + "DataService";
        ProfileLevel level = proxy.getConsumerLevel(consName);
        assertEquals(ProfileLevel.MIN, level);
        DataServiceMXBean dataProxy = JMX.newMXBeanProxy(mbsc, new ObjectName(DataServiceMXBean.MXBEAN_NAME), DataServiceMXBean.class);
        txnScheduler.runTask(new TestAbstractKernelRunnable() {

            public void run() {
                ManagedObject dummy = new DummyManagedObject();
                serverNode.getDataService().setBinding("dummy", dummy);
            }
        }, taskOwner);
        assertEquals(0, dataProxy.getSetBindingCalls());
        proxy.setConsumerLevel(consName, ProfileLevel.MAX);
        assertEquals(ProfileLevel.MAX, proxy.getConsumerLevel(consName));
        txnScheduler.runTask(new TestAbstractKernelRunnable() {

            public void run() {
                ManagedObject dummy = new DummyManagedObject();
                serverNode.getDataService().setBinding("dummy", dummy);
            }
        }, taskOwner);
        assertTrue(dataProxy.getSetBindingCalls() > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProfileControllerMXBeanBadGetLevel() throws Exception {
        ProfileControllerMXBean proxy = (ProfileControllerMXBean) JMX.newMXBeanProxy(mbsc, new ObjectName(ProfileControllerMXBean.MXBEAN_NAME), ProfileControllerMXBean.class);
        ProfileLevel level = proxy.getConsumerLevel("noSuchConsumer");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProfileControllerMXBeanBadSetLevel() throws Exception {
        ProfileControllerMXBean proxy = (ProfileControllerMXBean) JMX.newMXBeanProxy(mbsc, new ObjectName(ProfileControllerMXBean.MXBEAN_NAME), ProfileControllerMXBean.class);
        proxy.setConsumerLevel("notFound", ProfileLevel.MIN);
    }

    /**
     * A simple object implementing an MBean interface.
     */
    public class SimpleTest implements SimpleTestMBean {

        private int something;

        public int getSomething() {
            return something;
        }

        public void setSomething(int value) {
            something = value;
        }

        public void clearSomething() {
            something = 0;
        }
    }

    /**
     * A simple MBean interface.
     */
    public interface SimpleTestMBean {

        int getSomething();

        void setSomething(int value);

        void clearSomething();
    }

    /**
     * A simple XMBean interface.  MXBeans don't need to have their 
     * implementation classes in the same directory as the interface,
     * and don't need to follow the MBean naming conventions.
     */
    public static interface TestMXBean {

        int getSomething();

        void setSomething(int value);

        void clearSomething();
    }

    /**
     * A simple object implementing an MXBean interface.
     */
    public static class TestMXImpl implements TestMXBean {

        private int something;

        public int getSomething() {
            return something;
        }

        public void setSomething(int value) {
            something = value;
        }

        public void clearSomething() {
            something = 0;
        }
    }

    /**
     * A simple JMX listener that holds a map of notifications to the
     * number of times the notification occurred.
     */
    private class TestJMXNotificationListener implements NotificationListener {

        ConcurrentHashMap<String, AtomicLong> notificationMap = new ConcurrentHashMap<String, AtomicLong>();

        public void handleNotification(Notification notification, Object handback) {
            System.out.println("Received JMX notification: " + notification);
            String type = notification.getType();
            notificationMap.putIfAbsent(type, new AtomicLong());
            notificationMap.get(type).incrementAndGet();
        }
    }
}

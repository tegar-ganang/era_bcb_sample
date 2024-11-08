package biz.xsoftware.test.nio.tcp;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import junit.framework.TestCase;
import biz.xsoftware.api.nio.ChannelManager;
import biz.xsoftware.api.nio.ChannelService;
import biz.xsoftware.api.nio.ChannelServiceFactory;
import biz.xsoftware.api.nio.Settings;
import biz.xsoftware.api.nio.libs.BufferFactory;
import biz.xsoftware.api.nio.libs.FactoryCreator;
import biz.xsoftware.api.nio.libs.PacketProcessorFactory;
import biz.xsoftware.api.nio.testutil.HandlerForTests;
import biz.xsoftware.api.nio.testutil.MockNIOServer;

public class TestAsynchWrites extends TestCase {

    private static final Logger log = Logger.getLogger(TestAsynchWrites.class.getName());

    private ChannelServiceFactory factory;

    private PacketProcessorFactory procFactory;

    private Settings factoryHolder;

    private BufferFactory bufFactory;

    private InetSocketAddress svrAddr;

    private ChannelService chanMgr;

    private MockNIOServer mockServer;

    public TestAsynchWrites() {
        if (getBufFactory() == null) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(FactoryCreator.KEY_IS_DIRECT, false);
            FactoryCreator creator = FactoryCreator.createFactory(null);
            bufFactory = creator.createBufferFactory(map);
        }
        ChannelServiceFactory basic = ChannelServiceFactory.createFactory(null);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ChannelServiceFactory.KEY_IMPLEMENTATION_CLASS, ChannelServiceFactory.VAL_PACKET_CHANNEL_MGR);
        props.put(ChannelServiceFactory.KEY_CHILD_CHANNELMGR_FACTORY, basic);
        ChannelServiceFactory packetFactory = ChannelServiceFactory.createFactory(props);
        Map<String, Object> props2 = new HashMap<String, Object>();
        props2.put(ChannelServiceFactory.KEY_IMPLEMENTATION_CLASS, ChannelServiceFactory.VAL_EXCEPTION_CHANNEL_MGR);
        props2.put(ChannelServiceFactory.KEY_CHILD_CHANNELMGR_FACTORY, packetFactory);
        factory = ChannelServiceFactory.createFactory(props2);
        FactoryCreator creator = FactoryCreator.createFactory(null);
        procFactory = creator.createPacketProcFactory(null);
        factoryHolder = new Settings(null, procFactory);
    }

    protected void setUp() throws Exception {
        HandlerForTests.setupLogging();
        if (chanMgr == null) {
            chanMgr = getClientChanMgr();
        }
        if (mockServer == null) {
            ChannelService svcChanMgr = getServerChanMgr();
            mockServer = new MockNIOServer(svcChanMgr, getServerFactoryHolder());
        }
        chanMgr.start();
        svrAddr = mockServer.start();
        log.fine("server port =" + svrAddr);
    }

    protected void tearDown() throws Exception {
        chanMgr.stop();
        chanMgr = null;
        mockServer.stop();
        HandlerForTests.checkForWarnings();
    }

    protected ChannelService getClientChanMgr() {
        Map<String, Object> p = new HashMap<String, Object>();
        p.put(ChannelManager.KEY_ID, "[client]");
        p.put(ChannelManager.KEY_BUFFER_FACTORY, getBufFactory());
        return factory.createChannelManager(p);
    }

    protected ChannelService getServerChanMgr() {
        Map<String, Object> p = new HashMap<String, Object>();
        p.put(ChannelManager.KEY_ID, "[server]");
        p.put(ChannelManager.KEY_BUFFER_FACTORY, getBufFactory());
        return factory.createChannelManager(p);
    }

    protected Settings getClientFactoryHolder() {
        return factoryHolder;
    }

    protected Settings getServerFactoryHolder() {
        return factoryHolder;
    }

    protected String getChannelImplName() {
        return "biz.xsoftware.impl.nio.cm.exception.TCPChannelImpl";
    }

    protected String getServerChannelImplName() {
        return "biz.xsoftware.impl.nio.cm.exception.TCPServerChannelImpl";
    }

    public void testNothing() {
    }

    protected BufferFactory getBufFactory() {
        return bufFactory;
    }
}

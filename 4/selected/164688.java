package org.opennms.netmgt.capsd;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.core.concurrent.RunnableConsumerThreadPool;
import org.opennms.mock.snmp.MockSnmpAgent;
import org.opennms.netmgt.config.CapsdConfigFactory;
import org.opennms.netmgt.config.CollectdConfigFactory;
import org.opennms.netmgt.config.DataCollectionConfigFactory;
import org.opennms.netmgt.config.DatabaseSchemaConfigFactory;
import org.opennms.netmgt.config.DefaultCapsdConfigManager;
import org.opennms.netmgt.config.OpennmsServerConfigFactory;
import org.opennms.netmgt.config.PollerConfigFactory;
import org.opennms.netmgt.dao.support.RrdTestUtils;
import org.opennms.netmgt.mock.OpenNMSTestCase;
import org.opennms.netmgt.model.events.AnnotationBasedEventListenerAdapter;
import org.opennms.test.ConfigurationTestUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

public class ScanSuspectTest extends OpenNMSTestCase {

    private Capsd m_capsd;

    private MockSnmpAgent m_agent;

    @Override
    protected void setUp() throws Exception {
        System.setProperty("opennms.db.nextNodeId", "select max(nodeId) + 1 from node");
        super.setUp();
        m_agent = MockSnmpAgent.createAgentAndRun(new ClassPathResource("org/opennms/netmgt/snmp/stonegate.properties"), this.myLocalHost() + "/9161");
        InputStream configStream = ConfigurationTestUtils.getInputStreamForResource(this, "/org/opennms/netmgt/capsd/capsd-configuration.xml");
        DefaultCapsdConfigManager capsdConfig = new DefaultCapsdConfigManager(configStream);
        configStream.close();
        CapsdConfigFactory.setInstance(capsdConfig);
        configStream = ConfigurationTestUtils.getInputStreamForConfigFile("database-schema.xml");
        DatabaseSchemaConfigFactory dbConfigFactory = new DatabaseSchemaConfigFactory(configStream);
        configStream.close();
        DatabaseSchemaConfigFactory.setInstance(dbConfigFactory);
        OpennmsServerConfigFactory onmsSvrConfig = new OpennmsServerConfigFactory(ConfigurationTestUtils.getInputStreamForConfigFile("opennms-server.xml"));
        OpennmsServerConfigFactory.setInstance(onmsSvrConfig);
        PollerConfigFactory.setInstance(new PollerConfigFactory(System.currentTimeMillis(), ConfigurationTestUtils.getInputStreamForResource(this, "/org/opennms/netmgt/capsd/poller-configuration.xml"), onmsSvrConfig.getServerName(), onmsSvrConfig.verifyServer()));
        RrdTestUtils.initialize();
        DataCollectionConfigFactory.setInstance(new DataCollectionConfigFactory(ConfigurationTestUtils.getInputStreamForResource(this, "/org/opennms/netmgt/capsd/datacollection-config.xml")));
        CollectdConfigFactory.setInstance(new CollectdConfigFactory(ConfigurationTestUtils.getInputStreamForResource(this, "/org/opennms/netmgt/capsd/collectd-configuration.xml"), onmsSvrConfig.getServerName(), onmsSvrConfig.verifyServer()));
        JdbcTemplate jdbcTemplate = new JdbcTemplate(m_db);
        JdbcCapsdDbSyncer syncer = new JdbcCapsdDbSyncer();
        syncer.setJdbcTemplate(jdbcTemplate);
        syncer.setOpennmsServerConfig(OpennmsServerConfigFactory.getInstance());
        syncer.setCapsdConfig(CapsdConfigFactory.getInstance());
        syncer.setPollerConfig(PollerConfigFactory.getInstance());
        syncer.setCollectdConfig(CollectdConfigFactory.getInstance());
        syncer.setNextSvcIdSql(m_db.getNextServiceIdStatement());
        syncer.afterPropertiesSet();
        PluginManager pluginManager = new PluginManager();
        pluginManager.setCapsdConfig(capsdConfig);
        pluginManager.afterPropertiesSet();
        DefaultProcessorFactory defaultProcessorFactory = new DefaultProcessorFactory();
        defaultProcessorFactory.setCapsdDbSyncer(syncer);
        defaultProcessorFactory.setPluginManager(pluginManager);
        RunnableConsumerThreadPool suspectRunner = new RunnableConsumerThreadPool("SuspectRunner", 0.0f, 0.0f, 1);
        RunnableConsumerThreadPool rescanRunner = new RunnableConsumerThreadPool("RescanRunner", 0.0f, 0.0f, 1);
        Scheduler scheduler = new Scheduler(rescanRunner.getRunQueue(), defaultProcessorFactory);
        BroadcastEventProcessor eventHandler = new BroadcastEventProcessor();
        eventHandler.setSuspectEventProcessorFactory(defaultProcessorFactory);
        eventHandler.setLocalServer("localhost");
        eventHandler.setScheduler(scheduler);
        eventHandler.setSuspectQueue(suspectRunner.getRunQueue());
        eventHandler.afterPropertiesSet();
        AnnotationBasedEventListenerAdapter adapter = new AnnotationBasedEventListenerAdapter(eventHandler, m_eventdIpcMgr);
        m_capsd = new Capsd();
        m_capsd.setCapsdDbSyncer(syncer);
        m_capsd.setSuspectEventProcessorFactory(defaultProcessorFactory);
        m_capsd.setCapsdConfig(capsdConfig);
        m_capsd.setSuspectRunner(suspectRunner);
        m_capsd.setRescanRunner(rescanRunner);
        m_capsd.setScheduler(scheduler);
        m_capsd.setEventListener(adapter);
        m_capsd.afterPropertiesSet();
    }

    @Override
    public String getSnmpConfig() {
        return "<?xml version=\"1.0\"?>\n" + "<snmp-config " + " retry=\"3\" timeout=\"3000\"\n" + " read-community=\"public\"" + " write-community=\"private\"\n" + " port=\"161\"\n" + " version=\"v1\">\n" + "\n" + "   <definition port=\"9161\" version=\"v2c\" " + "       security-name=\"opennmsUser\" \n" + "       auth-passphrase=\"0p3nNMSv3\" \n" + "       privacy-passphrase=\"0p3nNMSv3\" >\n" + "       <specific>" + myLocalHost() + "</specific>\n" + "   </definition>\n" + "\n" + "   <definition version=\"v2c\" port=\"9161\" read-community=\"public\" proxy-host=\"" + myLocalHost() + "\">\n" + "<specific>149.134.45.45</specific>\n" + "<specific>172.16.201.2</specific>\n" + "<specific>172.17.1.230</specific>\n" + "<specific>172.31.1.1</specific>\n" + "<specific>172.31.3.1</specific>\n" + "<specific>172.31.3.9</specific>\n" + "<specific>172.31.3.17</specific>\n" + "<specific>172.31.3.25</specific>\n" + "<specific>172.31.3.33</specific>\n" + "<specific>172.31.3.41</specific>\n" + "<specific>172.31.3.49</specific>\n" + "<specific>172.31.3.57</specific>\n" + "<specific>172.31.3.65</specific>\n" + "<specific>172.31.3.73</specific>\n" + "<specific>172.100.10.1</specific>\n" + "<specific>203.19.73.1</specific>\n" + "<specific>203.220.17.53</specific>\n" + "   </definition>\n" + "</snmp-config>";
    }

    @Override
    protected void tearDown() throws Exception {
        m_agent.shutDownAndWait();
        super.tearDown();
    }

    protected String myLocalHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            fail("Exception getting localhost");
        }
        return null;
    }

    public final void testStartStop() throws MarshalException, ValidationException, IOException {
        m_capsd.start();
        m_capsd.scanSuspectInterface(this.myLocalHost());
        m_capsd.stop();
    }
}

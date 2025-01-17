package org.opennms.netmgt.config;

import java.io.Reader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import junit.framework.TestCase;
import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.opennms.test.mock.MockLogAppender;

public class SnmpPeerFactoryTest extends TestCase {

    private int m_version;

    protected void setUp() throws Exception {
        setVersion(SnmpAgentConfig.VERSION2C);
        Reader rdr = new StringReader(getSnmpConfig());
        SnmpPeerFactory.setInstance(new SnmpPeerFactory(rdr));
        MockLogAppender.setupLogging(false);
    }

    public void setVersion(int version) {
        m_version = version;
    }

    /**
     * String representing snmp-config.xml
     */
    public String getSnmpConfig() {
        return "<?xml version=\"1.0\"?>\n" + "<snmp-config " + " retry=\"3\" timeout=\"3000\"\n" + " read-community=\"public\"" + " write-community=\"private\"\n" + " port=\"161\"\n" + " max-vars-per-pdu = \"23\" " + " version=\"v1\">\n" + "\n" + "   <definition port=\"9161\" version=\"" + myVersion() + "\" " + "       security-name=\"opennmsUser\" \n" + "       auth-passphrase=\"0p3nNMSv3\" \n" + "       privacy-passphrase=\"0p3nNMSv3\" >\n" + "       <specific>" + myLocalHost() + "</specific>\n" + "   </definition>\n" + "\n" + "   <definition version=\"v1\" read-community=\"specificv1\">\n" + "       <specific>10.0.0.1</specific>\n" + "   </definition>\n" + "\n" + "   <definition version=\"v1\" read-community=\"specificv1\" max-request-size=\"484\">\n" + "       <specific>10.0.0.2</specific>\n" + "   </definition>\n" + "\n" + "   <definition version=\"v1\" read-community=\"specificv1\" proxy-host=\"" + myLocalHost() + "\">\n" + "       <specific>10.0.0.3</specific>\n" + "   </definition>\n" + "\n" + "   <definition version=\"v3\" " + "       security-name=\"opennmsUser\" \n" + "       auth-passphrase=\"0p3nNMSv3\" >\n" + "       <specific>20.20.20.20</specific>\n" + "   </definition>\n" + "   <definition version=\"v3\" " + "       security-name=\"opennmsRangeUser\" \n" + "       auth-passphrase=\"0p3nNMSv3\" >\n" + "       <range begin=\"1.1.1.1\" end=\"1.1.1.100\"/>\n" + "   </definition>\n" + "\n" + "   <definition version=\"v1\" read-community=\"rangev1\" max-vars-per-pdu=\"55\"> \n" + "       <range begin=\"10.0.0.101\" end=\"10.0.0.200\"/>\n" + "   </definition>\n" + "\n" + "   <definition version=\"v2c\" read-community=\"rangev2c\">\n" + "       <range begin=\"10.0.1.100\" end=\"10.0.5.100\"/>\n" + "       <range begin=\"10.7.20.100\" end=\"10.7.25.100\"/>\n" + "   </definition>\n" + "\n" + "   <definition version=\"v2c\" read-community=\"specificv2c\">\n" + "       <specific>192.168.0.50</specific>\n" + "   </definition>\n" + "\n" + "   <definition version=\"v2c\" read-community=\"ipmatch\" max-vars-per-pdu=\"128\" max-repetitions=\"7\" >\n" + "       <ip-match>77.5-12,15.1-255.255</ip-match>\n" + "   </definition>\n" + "\n" + "</snmp-config>";
    }

    protected String myLocalHost() {
        return "127.0.0.1";
    }

    private String myVersion() {
        switch(m_version) {
            case SnmpAgentConfig.VERSION1:
                return "v1";
            case SnmpAgentConfig.VERSION2C:
                return "v2c";
            case SnmpAgentConfig.VERSION3:
                return "v3";
            default:
                return "v1";
        }
    }

    protected void tearDown() {
    }

    public void testProxiedAgent() throws UnknownHostException {
        SnmpAgentConfig agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName("10.0.0.3"));
        assertEquals("10.0.0.3", agentConfig.getProxyFor().getHostAddress());
        assertEquals("127.0.0.1", agentConfig.getAddress().getHostAddress());
        agentConfig.toString();
    }

    public void testDefaultMaxRequestSize() throws UnknownHostException {
        SnmpAgentConfig agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName("10.0.0.1"));
        assertEquals(SnmpAgentConfig.DEFAULT_MAX_REQUEST_SIZE, agentConfig.getMaxRequestSize());
        agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName("10.0.0.2"));
        assertEquals(484, agentConfig.getMaxRequestSize());
    }

    public void testDefaultMaxVarsPerPdu() throws UnknownHostException {
        SnmpAgentConfig agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(myLocalHost()));
        assertEquals(23, agentConfig.getMaxVarsPerPdu());
    }

    public void testConfigureDefaultMaxVarsPerPdu() throws UnknownHostException {
        SnmpAgentConfig agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName("10.0.0.150"));
        assertEquals(55, agentConfig.getMaxVarsPerPdu());
    }

    public void testGetMaxRepetitions() throws UnknownHostException {
        SnmpAgentConfig agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName("77.5.5.255"));
        assertEquals("ipmatch", agentConfig.getReadCommunity());
        assertEquals(7, agentConfig.getMaxRepetitions());
        agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName("10.0.0.1"));
        assertEquals("specificv1", agentConfig.getReadCommunity());
        assertEquals(2, agentConfig.getMaxRepetitions());
    }

    public void testGetTargetFromPatterns() throws UnknownHostException {
        SnmpAgentConfig agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName("77.5.5.255"));
        assertEquals("ipmatch", agentConfig.getReadCommunity());
        assertEquals(128, agentConfig.getMaxVarsPerPdu());
        agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName("77.15.80.255"));
        assertEquals("ipmatch", agentConfig.getReadCommunity());
        assertEquals(7, agentConfig.getMaxRepetitions());
        agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName("77.4.5.255"));
        assertEquals("public", agentConfig.getReadCommunity());
        agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName("77.6.0.255"));
        assertEquals("public", agentConfig.getReadCommunity());
    }

    public void testGetSnmpAgentConfig() throws UnknownHostException {
        SnmpAgentConfig agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(myLocalHost()));
        assertEquals(SnmpAgentConfig.VERSION2C, agentConfig.getVersion());
    }

    /**
     * This tests getting an SnmpAgentConfig
     * @throws UnknownHostException
     */
    public void testGetConfig() throws UnknownHostException {
        assertNotNull(SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getLocalHost()));
    }

    /**
     * This tests for ranges configured for a v2 node and community string
     * @throws UnknownHostException
     */
    public void testGetv2cInRange() throws UnknownHostException {
        SnmpAgentConfig agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName("10.7.23.100"));
        assertNotNull(agentConfig);
        assertEquals(SnmpAgentConfig.VERSION2C, agentConfig.getVersion());
        assertEquals("rangev2c", agentConfig.getReadCommunity());
    }

    /**
     * This tests for ranges configured for v3 node and security name
     * @throws UnknownHostException 
     */
    public void testGetv3ConfigInRange() throws UnknownHostException {
        SnmpAgentConfig agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName("1.1.1.50"));
        assertNotNull(agentConfig);
        assertEquals(SnmpAgentConfig.VERSION3, agentConfig.getVersion());
        assertEquals("opennmsRangeUser", agentConfig.getSecurityName());
    }

    /**
     * This tests getting a v1 config
     * @throws UnknownHostException
     */
    public void testGetV1Config() throws UnknownHostException {
        SnmpAgentConfig agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName("10.0.0.1"));
        assertNotNull(agentConfig);
        assertTrue(agentConfig.getVersion() == SnmpAgentConfig.VERSION1);
        assertEquals("specificv1", agentConfig.getReadCommunity());
    }

    /**
     * This tests for a specifically defined v2c agentConfig
     * @throws UnknownHostException
     */
    public void testGetV2cConfig() throws UnknownHostException {
        SnmpAgentConfig agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName("192.168.0.50"));
        assertNotNull(agentConfig);
        assertEquals(agentConfig.getVersion(), SnmpAgentConfig.VERSION2C);
        assertEquals("specificv2c", agentConfig.getReadCommunity());
    }
}

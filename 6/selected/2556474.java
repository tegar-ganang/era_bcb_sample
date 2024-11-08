package org.ithacasoft.smack.packet.extensions.xmlbeans;

import org.apache.xmlbeans.XmlString;
import org.apache.xmlbeans.XmlBase64Binary;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.Collection;
import junitx.util.PropertyManager;
import static org.junit.Assert.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;

/**
 * @version $Id$
 * @since 0.0.1
 * @author <a href="mailto:kartashev@gmail.com">Andrey V. Kartashev</a>
 */
public class SimpleMapperTest {

    public static final String TEST_STRING1 = "ABCD <It is test string> <<<<<";

    public static final String TEST_STRING2 = "<test/>";

    public TestStringDocument testXmlString1;

    public TestStringDocument testXmlString2;

    public static final String XMPP_ACCOUNT1 = PropertyManager.getProperty("account1");

    public static final String XMPP_ADDRESS1 = PropertyManager.getProperty("address1");

    public static final String XMPP_PORT1 = PropertyManager.getProperty("port1");

    public static final String XMPP_LOGIN1 = PropertyManager.getProperty("login1");

    public static final String XMPP_PASSWORD1 = PropertyManager.getProperty("password1");

    public static final String XMPP_ACCOUNT2 = PropertyManager.getProperty("account2");

    public static final String XMPP_ADDRESS2 = PropertyManager.getProperty("address2");

    public static final String XMPP_PORT2 = PropertyManager.getProperty("port2");

    public static final String XMPP_LOGIN2 = PropertyManager.getProperty("login2");

    public static final String XMPP_PASSWORD2 = PropertyManager.getProperty("password2");

    public SimpleMapperTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        testXmlString1 = TestStringDocument.Factory.newInstance();
        testXmlString1.setTestString(TEST_STRING1);
        testXmlString2 = TestStringDocument.Factory.newInstance();
        testXmlString2.setTestString(TEST_STRING2);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of put and get methods of class SimpleMapper with XMLString.
     */
    @Test
    public void testPutGetXmlString() throws Exception {
        Packet packet = new Message();
        SimpleMapper<TestStringDocument> instance = new SimpleMapper(TestStringDocument.class);
        instance.addExtension(packet, testXmlString1);
        TestStringDocument result = instance.getExtension(packet);
        assertEquals("Get must return value which was put", result.getTestString(), TEST_STRING1);
    }

    /**
     * Test of get method for non-existent extension.
     */
    @Test
    public void testGetNonExisted1() throws Exception {
        Packet packet = new Message();
        SimpleMapper<TestStringDocument> instance = new SimpleMapper(TestStringDocument.class);
        TestStringDocument result = instance.getExtension(packet);
        assertNull("Extension was not put, so result of get must be null", result);
    }

    /**
     * Test of getExtensions method of class SimpleMapper with XMLString.
     */
    @Test
    public void testGetExtensions() throws Exception {
        Packet packet = new Message();
        SimpleMapper<TestStringDocument> instance = new SimpleMapper(TestStringDocument.class);
        instance.addExtension(packet, testXmlString1);
        instance.addExtension(packet, testXmlString2);
        Collection<TestStringDocument> result = instance.getExtensions(packet);
        assertEquals("getExtensions must return values which were put", 2, result.size());
        assertEquals("getExtensions must return values which were put", testXmlString1, result.toArray()[0]);
        assertEquals("getExtensions must return values which were put", testXmlString2, result.toArray()[1]);
    }

    /**
     * Test of get method for non-existed extension.
     */
    @Test
    public void testGetNonExisted2() throws Exception {
        Packet packet = new Message();
        SimpleMapper<TestStringDocument> instance = new SimpleMapper(TestStringDocument.class);
        instance.addExtension(packet, testXmlString1);
        SimpleMapper<TestIntegerDocument> instanceForGet = new SimpleMapper(TestIntegerDocument.class);
        TestIntegerDocument result = instanceForGet.getExtension(packet);
        assertNull("Extension was not put, so result of get must be null", result);
    }

    /**
     * Test of put method of class SimpleMapper with null value.
     */
    @Test
    public void testPutNull() throws Exception {
        Packet packet = new Message();
        SimpleMapper<TestStringDocument> instance = new SimpleMapper(TestStringDocument.class);
        instance.addExtension(packet, testXmlString1);
        instance.addExtension(packet, null);
        TestStringDocument result = instance.getExtension(packet);
        assertEquals("getExtension must return value which was put", testXmlString1, result);
    }

    /**
     * Test of addExtension method of class SimpleMapper.
     */
    @Test
    public void testAdd() throws Exception {
        Packet packet = new Message();
        SimpleMapper<TestStringDocument> instance = new SimpleMapper(TestStringDocument.class);
        instance.addExtension(packet, testXmlString1);
        instance.addExtension(packet, testXmlString2);
        TestStringDocument result = instance.getExtension(packet);
        assertEquals("getExtension must return first value which was put", result.getTestString(), TEST_STRING1);
    }

    /**
     * Test of get method of class SimpleMapper with null packet argument.
     */
    @Test
    public void testFailGetNullPacket() throws Exception {
        Packet packet = new Message();
        SimpleMapper<TestStringDocument> instance = new SimpleMapper(TestStringDocument.class);
        instance.addExtension(packet, testXmlString1);
        try {
            TestStringDocument result = instance.getExtension(null);
        } catch (NullPointerException npe) {
            return;
        }
        assertTrue(TEST_STRING1, false);
    }

    /**
     * Test of the put method of class SimpleMapper with null packet argument.
     */
    @Test
    public void testFailPutNullPacket() throws Exception {
        Packet packet = new Message();
        SimpleMapper<TestStringDocument> instance = new SimpleMapper(TestStringDocument.class);
        try {
            instance.addExtension(null, testXmlString1);
        } catch (NullPointerException npe) {
            return;
        }
        assertTrue("NullPointerException was expected.", false);
    }

    /**
     * Test of put and get methods of class SimpleMapper with XMLString.
     */
    @Test
    public void testPutSendReceiveGetXmlString() throws Exception {
        if (XMPP_ACCOUNT1 != null) {
            ConnectionConfiguration cc = new ConnectionConfiguration(XMPP_ADDRESS1, Integer.valueOf(XMPP_PORT1));
            XMPPConnection conn = new XMPPConnection(cc);
            conn.connect();
            conn.login(XMPP_LOGIN1, XMPP_PASSWORD1);
            Message packet = new Message(XMPP_ACCOUNT2);
            packet.setFrom(XMPP_ACCOUNT1);
            packet.setBody("Test");
            SimpleMapper<TestStringDocument> instance = new SimpleMapper(TestStringDocument.class);
            instance.addExtension(packet, testXmlString1);
            conn.sendPacket(packet);
            conn.disconnect();
            ConnectionConfiguration cc2 = new ConnectionConfiguration(XMPP_ADDRESS2, Integer.valueOf(XMPP_PORT2));
            XMPPConnection conn2 = new XMPPConnection(cc2);
            conn2.connect();
            PacketCollector pc = conn2.createPacketCollector(new PacketTypeFilter(Message.class));
            conn2.login(XMPP_LOGIN2, XMPP_PASSWORD2);
            Packet p = pc.nextResult();
            TestStringDocument result = instance.getExtension(p);
            assertEquals(TEST_STRING1, result.getTestString());
            conn2.disconnect();
        }
    }
}

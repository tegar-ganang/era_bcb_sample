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
 * @since 0.0.3
 * @author <a href="mailto:kartashev@gmail.com">Andrey V. Kartashev</a>
 */
public class DemoTest {

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

    @Before
    public void setUp() {
        testXmlString1 = TestStringDocument.Factory.newInstance();
        testXmlString1.setTestString(TEST_STRING1);
        testXmlString2 = TestStringDocument.Factory.newInstance();
        testXmlString2.setTestString(TEST_STRING2);
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
            MapperFactory factory = new SimpleMapperFactory();
            Mapper<TestStringDocument> instance = factory.createInstance(TestStringDocument.class);
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

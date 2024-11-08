package org.mobicents.servlet.sip.testsuite.session;

import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import javax.sip.SipProvider;
import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.SipServletTestCase;
import org.mobicents.servlet.sip.testsuite.ProtocolObjects;
import org.mobicents.servlet.sip.testsuite.TestSipListener;

public class SipAppSessionTerminationTest extends SipServletTestCase {

    private String CLICK2DIAL_URL;

    private String CLICK2DIAL_PARAMS;

    private static transient Logger logger = Logger.getLogger(SipAppSessionTerminationTest.class);

    TestSipListener receiver;

    ProtocolObjects receiverProtocolObjects;

    private static final String TRANSPORT = "udp";

    private static final boolean AUTODIALOG = true;

    private static final int TIMEOUT = 10000;

    public SipAppSessionTerminationTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        CLICK2DIAL_URL = "http://" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":8080/click2call/call";
        CLICK2DIAL_PARAMS = "?from=sip:sipAppTest@" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":5056&to=sip:to@" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":5057";
        receiverProtocolObjects = new ProtocolObjects("receiver", "gov.nist", TRANSPORT, AUTODIALOG, null, null, null);
        receiver = new TestSipListener(5057, 5070, receiverProtocolObjects, true);
        SipProvider receiverProvider = receiver.createProvider();
        receiverProvider.addSipListener(receiver);
        receiverProtocolObjects.start();
    }

    @Override
    public void tearDown() throws Exception {
        receiverProtocolObjects.destroy();
        logger.info("Test completed");
        super.tearDown();
    }

    @Override
    public void deployApplication() {
        assertTrue(tomcat.deployContext(projectHome + "/sip-servlets-test-suite/applications/click-to-call-servlet/src/main/sipapp", "click2call-context", "/click2call"));
    }

    @Override
    protected String getDarConfigurationFile() {
        return "file:///" + projectHome + "/sip-servlets-test-suite/testsuite/src/test/resources/" + "org/mobicents/servlet/sip/testsuite/click2call/click-to-call-dar.properties";
    }

    /**
	 * Test that the sip app session is not invalidated and destroyed when only the sip session is invalidated and not the http session
	 * @throws Exception
	 */
    public void testSipAppSessionTerminationHttpSessionStillAlive() throws Exception {
        logger.info("Trying to reach url : " + CLICK2DIAL_URL + CLICK2DIAL_PARAMS);
        URL url = new URL(CLICK2DIAL_URL + CLICK2DIAL_PARAMS);
        InputStream in = url.openConnection().getInputStream();
        byte[] buffer = new byte[10000];
        int len = in.read(buffer);
        String httpResponse = "";
        for (int q = 0; q < len; q++) httpResponse += (char) buffer[q];
        logger.info("Received the follwing HTTP response: " + httpResponse);
        Thread.sleep(TIMEOUT);
        assertTrue(receiver.getOkToByeReceived());
        Thread.sleep(TIMEOUT);
        Iterator<String> allMessagesIterator = receiver.getAllMessagesContent().iterator();
        logger.info("all messages received : ");
        while (allMessagesIterator.hasNext()) {
            String message = (String) allMessagesIterator.next();
            logger.info(message);
        }
        assertFalse(receiver.getAllMessagesContent().contains("sipAppSessionDestroyed"));
    }

    /**
	 * Test if the sip app session is invalidated and destroyed when both the sip session and http session are invalidated
	 * @throws Exception
	 */
    public void testSipAppSessionTerminationHttpSessionInvalidated() throws Exception {
        logger.info("Trying to reach url : " + CLICK2DIAL_URL + CLICK2DIAL_PARAMS + "&invalidateHttpSession=true");
        URL url = new URL(CLICK2DIAL_URL + CLICK2DIAL_PARAMS + "&invalidateHttpSession=true");
        InputStream in = url.openConnection().getInputStream();
        byte[] buffer = new byte[10000];
        int len = in.read(buffer);
        String httpResponse = "";
        for (int q = 0; q < len; q++) httpResponse += (char) buffer[q];
        logger.info("Received the follwing HTTP response: " + httpResponse);
        Thread.sleep(TIMEOUT);
        assertTrue(receiver.getOkToByeReceived());
        Thread.sleep(TIMEOUT);
        Iterator<String> allMessagesIterator = receiver.getAllMessagesContent().iterator();
        logger.info("all messages received : ");
        while (allMessagesIterator.hasNext()) {
            String message = (String) allMessagesIterator.next();
            logger.info(message);
        }
        assertTrue(receiver.getAllMessagesContent().contains("sipAppSessionDestroyed"));
    }
}

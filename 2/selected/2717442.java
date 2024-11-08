package org.mobicents.servlet.sip.testsuite.concurrency;

import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import javax.sip.SipProvider;
import javax.sip.address.SipURI;
import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.SipServletTestCase;
import org.mobicents.servlet.sip.annotation.ConcurrencyControlMode;
import org.mobicents.servlet.sip.catalina.SipStandardManager;
import org.mobicents.servlet.sip.startup.SipContextConfig;
import org.mobicents.servlet.sip.startup.SipStandardContext;
import org.mobicents.servlet.sip.testsuite.ProtocolObjects;
import org.mobicents.servlet.sip.testsuite.TestSipListener;

public class ConcurrentyControlAsyncWorkSessionIsolationTest extends SipServletTestCase {

    private static transient Logger logger = Logger.getLogger(ConcurrentyControlAsyncWorkSessionIsolationTest.class);

    private String CLICK2DIAL_URL;

    private static final String TRANSPORT = "udp";

    private static final boolean AUTODIALOG = true;

    TestSipListener sender;

    ProtocolObjects senderProtocolObjects;

    public ConcurrentyControlAsyncWorkSessionIsolationTest(String name) {
        super(name);
        autoDeployOnStartup = false;
    }

    public void deployApplication() {
    }

    public void deployApplication(ConcurrencyControlMode concurrencyControlMode) {
        SipStandardContext context = new SipStandardContext();
        context.setDocBase(projectHome + "/sip-servlets-test-suite/applications/click-to-call-servlet/src/main/sipapp");
        context.setName("click2call");
        context.setPath("/click2call");
        context.addLifecycleListener(new SipContextConfig());
        context.setManager(new SipStandardManager());
        context.setConcurrencyControlMode(concurrencyControlMode);
        assertTrue(tomcat.deployContext(context));
    }

    @Override
    protected String getDarConfigurationFile() {
        return "file:///" + projectHome + "/sip-servlets-test-suite/testsuite/src/test/resources/" + "org/mobicents/servlet/sip/testsuite/concurrency/click-to-call-dar.properties";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CLICK2DIAL_URL = "http://" + System.getProperty("org.mobicents.testsuite.testhostaddr") + ":8080/click2call/call";
        senderProtocolObjects = new ProtocolObjects("sender", "gov.nist", TRANSPORT, AUTODIALOG, null, null, null);
        sender = new TestSipListener(5080, 5070, senderProtocolObjects, true);
        SipProvider senderProvider = sender.createProvider();
        senderProvider.addSipListener(sender);
        senderProtocolObjects.start();
    }

    public void testElapsedTimeAndSessionOverlapping() throws Exception {
        deployApplication(ConcurrencyControlMode.SipSession);
        String fromName = "asyncWork";
        String fromSipAddress = "sip-servlets.com";
        SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(fromName, fromSipAddress);
        fromAddress.setParameter("mode", ConcurrencyControlMode.SipSession.toString());
        String toUser = "receiver";
        String toSipAddress = "sip-servlets.com";
        SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(toUser, toSipAddress);
        sender.setSendBye(false);
        sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, false);
        Thread.sleep(3000);
        String sasId = new String(sender.getFinalResponse().getRawContent());
        String CLICK2DIAL_PARAMS = "?asyncWorkMode=" + ConcurrencyControlMode.SipSession + "&asyncWorkSasId=" + sasId;
        sender.sendInDialogSipRequest("OPTIONS", "1", "text", "plain", null, null);
        Thread.sleep(100);
        sender.sendInDialogSipRequest("OPTIONS", "2", "text", "plain", null, null);
        Thread.sleep(100);
        logger.info("Trying to reach url : " + CLICK2DIAL_URL + CLICK2DIAL_PARAMS);
        URL url = new URL(CLICK2DIAL_URL + CLICK2DIAL_PARAMS);
        InputStream in = url.openConnection().getInputStream();
        byte[] buffer = new byte[10000];
        int len = in.read(buffer);
        String httpResponse = "";
        for (int q = 0; q < len; q++) httpResponse += (char) buffer[q];
        logger.info("Received the follwing HTTP response: " + httpResponse);
        sender.sendInDialogSipRequest("OPTIONS", "3", "text", "plain", null, null);
        Thread.sleep(100);
        sender.sendBye();
        Thread.sleep(20000);
        assertTrue(!sender.isServerErrorReceived());
        assertTrue(sender.isAckSent());
        assertTrue(sender.getOkToByeReceived());
        Iterator<String> allMessagesIterator = sender.getAllMessagesContent().iterator();
        while (allMessagesIterator.hasNext()) {
            String message = (String) allMessagesIterator.next();
            logger.info(message);
        }
        assertTrue(sender.getAllMessagesContent().contains("OK"));
    }

    public void testElapsedTimeAndSipApplicationSessionOverlapping() throws Exception {
        deployApplication(ConcurrencyControlMode.SipApplicationSession);
        String fromName = "asyncWork";
        String fromSipAddress = "sip-servlets.com";
        SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(fromName, fromSipAddress);
        fromAddress.setParameter("mode", ConcurrencyControlMode.SipApplicationSession.toString());
        String toUser = "receiver";
        String toSipAddress = "sip-servlets.com";
        SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(toUser, toSipAddress);
        sender.setSendBye(false);
        sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, false);
        Thread.sleep(3000);
        String sasId = new String(sender.getFinalResponse().getRawContent());
        String CLICK2DIAL_PARAMS = "?asyncWorkMode=" + ConcurrencyControlMode.SipApplicationSession + "&asyncWorkSasId=" + sasId;
        sender.sendInDialogSipRequest("OPTIONS", "1", "text", "plain", null, null);
        Thread.sleep(100);
        sender.sendInDialogSipRequest("OPTIONS", "2", "text", "plain", null, null);
        Thread.sleep(100);
        logger.info("Trying to reach url : " + CLICK2DIAL_URL + CLICK2DIAL_PARAMS);
        URL url = new URL(CLICK2DIAL_URL + CLICK2DIAL_PARAMS);
        InputStream in = url.openConnection().getInputStream();
        byte[] buffer = new byte[10000];
        int len = in.read(buffer);
        String httpResponse = "";
        for (int q = 0; q < len; q++) httpResponse += (char) buffer[q];
        logger.info("Received the follwing HTTP response: " + httpResponse);
        sender.sendInDialogSipRequest("OPTIONS", "3", "text", "plain", null, null);
        Thread.sleep(100);
        sender.sendBye();
        Thread.sleep(20000);
        assertTrue(!sender.isServerErrorReceived());
        assertTrue(sender.isAckSent());
        assertTrue(sender.getOkToByeReceived());
        Iterator<String> allMessagesIterator = sender.getAllMessagesContent().iterator();
        while (allMessagesIterator.hasNext()) {
            String message = (String) allMessagesIterator.next();
            logger.info(message);
        }
        assertTrue(sender.getAllMessagesContent().contains("OK"));
    }

    public void testElapsedTimeAndSipApplicationSessionDeadlock() throws Exception {
        deployApplication(ConcurrencyControlMode.SipApplicationSession);
        String fromName = "Thread";
        String fromSipAddress = "sip-servlets.com";
        SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(fromName, fromSipAddress);
        fromAddress.setParameter("mode", ConcurrencyControlMode.SipApplicationSession.toString());
        String toUser = "receiver";
        String toSipAddress = "sip-servlets.com";
        SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(toUser, toSipAddress);
        String CLICK2DIAL_PARAMS = "?asyncWorkMode=Thread&asyncWorkSasId=test";
        logger.info("Trying to reach url : " + CLICK2DIAL_URL + CLICK2DIAL_PARAMS);
        URL url = new URL(CLICK2DIAL_URL + CLICK2DIAL_PARAMS);
        InputStream in = url.openConnection().getInputStream();
        byte[] buffer = new byte[10000];
        int len = in.read(buffer);
        String httpResponse = "";
        for (int q = 0; q < len; q++) httpResponse += (char) buffer[q];
        logger.info("Received the follwing HTTP response: " + httpResponse);
        Thread.sleep(10000);
        Iterator<String> allMessagesIterator = sender.getAllMessagesContent().iterator();
        while (allMessagesIterator.hasNext()) {
            String message = (String) allMessagesIterator.next();
            logger.info(message);
        }
        assertTrue(sender.getAllMessagesContent().contains("OK"));
    }

    public void testElapsedTimeAndSessionOverlappingWithNoConcurrencyControl() throws Exception {
        deployApplication(ConcurrencyControlMode.None);
        String fromName = "asyncWork";
        String fromSipAddress = "sip-servlets.com";
        SipURI fromAddress = senderProtocolObjects.addressFactory.createSipURI(fromName, fromSipAddress);
        fromAddress.setParameter("mode", ConcurrencyControlMode.None.toString());
        String toUser = "receiver";
        String toSipAddress = "sip-servlets.com";
        SipURI toAddress = senderProtocolObjects.addressFactory.createSipURI(toUser, toSipAddress);
        sender.setSendBye(false);
        sender.sendSipRequest("INVITE", fromAddress, toAddress, null, null, false);
        Thread.sleep(3000);
        String sasId = new String(sender.getFinalResponse().getRawContent());
        String CLICK2DIAL_PARAMS = "?asyncWorkMode=" + ConcurrencyControlMode.None + "&asyncWorkSasId=" + sasId;
        sender.sendInDialogSipRequest("OPTIONS", "1", "text", "plain", null, null);
        Thread.sleep(100);
        sender.sendInDialogSipRequest("OPTIONS", "2", "text", "plain", null, null);
        Thread.sleep(100);
        logger.info("Trying to reach url : " + CLICK2DIAL_URL + CLICK2DIAL_PARAMS);
        URL url = new URL(CLICK2DIAL_URL + CLICK2DIAL_PARAMS);
        InputStream in = url.openConnection().getInputStream();
        byte[] buffer = new byte[10000];
        int len = in.read(buffer);
        String httpResponse = "";
        for (int q = 0; q < len; q++) httpResponse += (char) buffer[q];
        logger.info("Received the follwing HTTP response: " + httpResponse);
        sender.sendInDialogSipRequest("OPTIONS", "3", "text", "plain", null, null);
        Thread.sleep(100);
        sender.sendBye();
        Thread.sleep(20000);
        assertTrue(sender.isServerErrorReceived());
        assertTrue(sender.isAckSent());
        assertTrue(sender.getOkToByeReceived());
        Iterator<String> allMessagesIterator = sender.getAllMessagesContent().iterator();
        while (allMessagesIterator.hasNext()) {
            String message = (String) allMessagesIterator.next();
            logger.info(message);
        }
        assertTrue(sender.getAllMessagesContent().contains("KO"));
    }

    @Override
    protected void tearDown() throws Exception {
        senderProtocolObjects.destroy();
        logger.info("Test completed");
        super.tearDown();
    }
}

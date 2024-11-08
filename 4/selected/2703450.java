package com.volantis.mps.channels;

import com.volantis.mps.message.MessageException;
import com.volantis.mps.message.MultiChannelMessage;
import com.volantis.mps.message.MultiChannelMessageImpl;
import com.volantis.mps.recipient.MessageRecipients;
import com.volantis.mps.recipient.MessageRecipient;
import com.volantis.mps.recipient.RecipientException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.net.URL;
import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.io.IOException;

/**
 * This tests the NowSMSWAPPushChannelAdapter using the superclass tests in
 * <code>PhoneGatewayChannelAdapterTestAbstract</code> and also specific tests
 * in this class.
 */
public class NowSMSWAPPushChannelAdapterTestCase extends PhoneGatewayChannelAdapterTestAbstract {

    /**
     * The channel name used for all of the tests for this channel.
     */
    protected static final String CHANNEL_NAME = "WAPPush";

    /**
     * Initialise a new instance of this test case.
     */
    public NowSMSWAPPushChannelAdapterTestCase() {
    }

    /**
     * Initialise a new named instance of this test case.
     *
     * @param s The name of the test case.
     */
    public NowSMSWAPPushChannelAdapterTestCase(String s) {
        super(s);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Testing the send
     */
    public void testSendImpl() throws Exception {
        NowSMSWAPPushChannelAdapter channel = new NowSMSWAPPushChannelAdapter(CHANNEL_NAME, createChannelInfo()) {

            protected boolean sendAsGetRequest(String paramString) throws IOException, MalformedURLException {
                return !(paramString.startsWith("PhoneNumber=%2B44failure"));
            }
        };
        MultiChannelMessage multiChannelMessage = new MultiChannelMessageImpl();
        multiChannelMessage.setMessageURL(new URL("http://test.message/file"));
        multiChannelMessage.setSubject("Test Message");
        MessageRecipients messageRecipients = createTestRecipients();
        MessageRecipient messageSender = new MessageRecipient(new InternetAddress("mps@volantis.com"), "Master");
        MessageRecipients failures = channel.sendImpl(multiChannelMessage, messageRecipients, messageSender);
        assertNotNull("Should be a failure list, even if it is empty", failures);
        assertTrue("Expected some failures", failures.getIterator().hasNext());
        for (Iterator i = failures.getIterator(); i.hasNext(); ) {
            MessageRecipient recipient = (MessageRecipient) i.next();
            System.out.println("Failure:\n");
            assertEquals("Failed recipient MSISDN should match", "failure", recipient.getMSISDN());
            assertEquals("Failed recipient channel name should match", CHANNEL_NAME, recipient.getChannelName());
        }
    }

    /**
     * A utility method that creates some sample recipients of messages to
     * enable testing of sending messages in this channel.
     *
     * @return A list of recipients.
     *
     * @throws RecipientException If there is a problem creating a recipient or
     *                            adding one to the list of recipients.
     * @throws AddressException   If there is a problem creating the internet
     *                            address of a recipient.
     */
    private MessageRecipients createTestRecipients() throws RecipientException, AddressException {
        MessageRecipients messageRecipients = new MessageRecipients();
        MessageRecipient recipient = new MessageRecipient();
        recipient.setMSISDN("0123456789");
        recipient.setAddress(new InternetAddress("test@volantis.com"));
        recipient.setChannelName(CHANNEL_NAME);
        recipient.setDeviceName("Master");
        messageRecipients.addRecipient(recipient);
        recipient = new MessageRecipient();
        recipient.setMSISDN("+44123456789");
        recipient.setAddress(new InternetAddress("me@volantis.com"));
        recipient.setChannelName(CHANNEL_NAME);
        recipient.setDeviceName("Master");
        messageRecipients.addRecipient(recipient);
        recipient = new MessageRecipient();
        recipient.setMSISDN("failure");
        recipient.setAddress(new InternetAddress("you@volantis.com"));
        recipient.setChannelName(CHANNEL_NAME);
        recipient.setDeviceName("Master");
        messageRecipients.addRecipient(recipient);
        recipient = new MessageRecipient();
        recipient.setMSISDN("1234567890");
        recipient.setAddress(new InternetAddress("them@volantis.com"));
        recipient.setChannelName("SMTP");
        recipient.setDeviceName("Master");
        messageRecipients.addRecipient(recipient);
        return messageRecipients;
    }

    /**
     * Testing the creation of a test message
     */
    public void testCreateMessage() throws Exception {
        NowSMSWAPPushChannelAdapter channel = (NowSMSWAPPushChannelAdapter) getTestInstance();
        MultiChannelMessage multiChannelMessage = new MultiChannelMessageImpl() {

            public URL generateTargetMessageAsURL(String deviceName, String mssUrl) throws MessageException {
                URL url = null;
                try {
                    url = new URL("http://host.domain/servlet/id");
                } catch (MalformedURLException mue) {
                }
                return url;
            }
        };
        String expected = "host.domain/servlet/id";
        String messageOne = channel.createMessage(multiChannelMessage, "Device One");
        assertNotNull("Message should exist (1)", messageOne);
        assertEquals("Message should match expected (1)", messageOne, expected);
        String messageTwo = channel.createMessage(multiChannelMessage, "Device Two");
        assertNotNull("Message should exist (2)", messageTwo);
        assertEquals("Message should match expected (2)", messageTwo, expected);
        assertEquals("Retrieved messages should be the same", messageOne, messageTwo);
    }

    /**
     * Testing the construction of various parameter strings
     */
    public void testConstructParameters() throws Exception {
        NowSMSWAPPushChannelAdapter channel = (NowSMSWAPPushChannelAdapter) getTestInstance();
        String msisdn = "+441483739739";
        String messageLink = "www.volantis.com/wappush";
        String subject = "Test WAP Push";
        try {
            channel.constructParameters(null, messageLink, subject);
            fail("Previous call should have caused an exception (1)");
        } catch (MessageException me) {
        }
        try {
            channel.constructParameters(msisdn, null, subject);
            fail("Previous call should have caused an exception (2)");
        } catch (MessageException me) {
        }
        String paramString = channel.constructParameters(msisdn, messageLink, subject);
        String expected = NowSMSWAPPushChannelAdapter.DESTINATION + "=" + msisdn + "&" + NowSMSWAPPushChannelAdapter.MESSAGE + "=" + messageLink + "&" + NowSMSWAPPushChannelAdapter.SUBJECT + "=" + URLEncoder.encode(subject);
        assertEquals("Parameter strings should match (1)", expected, paramString);
        paramString = channel.constructParameters(msisdn, messageLink, null);
        expected = NowSMSWAPPushChannelAdapter.DESTINATION + "=" + msisdn + "&" + NowSMSWAPPushChannelAdapter.MESSAGE + "=" + messageLink + "&" + NowSMSWAPPushChannelAdapter.NO_SUBJECT_KEY + "=" + NowSMSWAPPushChannelAdapter.NO_SUBJECT_VALUE;
        assertEquals("Parameter strings should match (2)", expected, paramString);
    }

    /**
     * Testing removing the protocol part of a URL in string form.
     */
    public void testRemoveProtocol() throws Exception {
        NowSMSWAPPushChannelAdapter channel = (NowSMSWAPPushChannelAdapter) getTestInstance();
        URL url = new URL("http://www.volantis.com");
        String expected = "www.volantis.com";
        String removed = channel.removeProtocol(url);
        assertNotNull("Should have a valid removed string (1)", removed);
        assertFalse("Should not match original URL (1)", url.toExternalForm().equals(removed));
        assertEquals("Should match expected (1)", expected, removed);
        url = new URL("ftp://www.volantis.com");
        removed = channel.removeProtocol(url);
        assertNotNull("Should have a valid removed string (2)", removed);
        assertFalse("Should not match original URL (2)", url.toExternalForm().equals(removed));
        assertEquals("Should match expected (2)", expected, removed);
        url = null;
        try {
            removed = channel.removeProtocol(url);
            fail("Should have had an exception thrown by the previous line");
        } catch (MessageException me) {
        }
        url = new URL("http://www.volantis.com/path/to/some/file.xml");
        expected = "www.volantis.com/path/to/some/file.xml";
        removed = channel.removeProtocol(url);
        assertNotNull("Should have a valid removed string (3)", removed);
        assertFalse("Should not match original URL (3)", url.toExternalForm().equals(removed));
        assertEquals("Should match expected (3)", expected, removed);
    }

    /**
     * Create a testable channel info for use in creating test instances of
     * this channel.
     *
     * @return An initialised channel info map containing {@link
     *         PhoneGatewayChannelAdapter#URL} and {@link PhoneGatewayChannelAdapter#DEFAULT_COUNTRY_CODE}.
     */
    private Map createChannelInfo() {
        Map channelInfo = new HashMap();
        channelInfo.put(NowSMSWAPPushChannelAdapter.URL, getDefaultHost());
        channelInfo.put(NowSMSWAPPushChannelAdapter.DEFAULT_COUNTRY_CODE, getDefaultCountryCode());
        channelInfo.put(NowSMSWAPPushChannelAdapter.MSS_URL, getDefaultMSSUrl());
        return channelInfo;
    }

    protected PhoneGatewayChannelAdapter getTestInstance() throws Exception {
        return new NowSMSWAPPushChannelAdapter(CHANNEL_NAME, createChannelInfo());
    }

    protected String getDefaultHost() {
        return DEFAULT_HOST;
    }

    protected String getDefaultCountryCode() {
        return "+44";
    }

    /**
     * Returnds the default URL for the MSS servlet
     *
     * @return
     */
    protected String getDefaultMSSUrl() {
        return "http://myserver:8080/mss";
    }
}

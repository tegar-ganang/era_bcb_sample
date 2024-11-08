package com.volantis.mps.message;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Map;
import java.io.File;
import com.volantis.mcs.runtime.Volantis;
import com.volantis.mcs.utilities.MarinerURL;
import com.volantis.mps.attachment.DeviceMessageAttachment;
import com.volantis.mps.attachment.MessageAttachments;
import com.volantis.testtools.stubs.ServletContextStub;
import com.volantis.synergetics.testtools.TestCaseAbstract;
import com.volantis.synergetics.testtools.net.FakeSocketFactory;

/**
 *
 * @author ianw
 */
public class MultiChannelMessageTestCase extends TestCaseAbstract {

    protected Volantis volantisBean;

    protected ServletContextStub servletContext;

    /**
     * Local file URL
     */
    protected static String[] LOCAL_FILE_URLS;

    /**
     * Temporary filename prefix
     */
    protected static final String TMP_FILE_PREFIX = "TestFile";

    public MultiChannelMessageTestCase(String testName) {
        super(testName);
    }

    /**
    * Set up the bean and an empty ServletContext.
    */
    public void setUp() throws Exception {
        super.setUp();
        LOCAL_FILE_URLS = new String[3];
        for (int i = 0; i < LOCAL_FILE_URLS.length; i++) {
            File urlFile = File.createTempFile(TMP_FILE_PREFIX, null);
            LOCAL_FILE_URLS[i] = urlFile.toURI().toURL().toString();
        }
    }

    /**
    * Destroy the objects created by the previous set up.
    */
    public void tearDown() throws Exception {
        super.tearDown();
        for (int i = 0; i < LOCAL_FILE_URLS.length; i++) {
            (new File(LOCAL_FILE_URLS[i])).delete();
        }
    }

    /** Test of setMessageURL method, of class com.volantis.mps.message.MultiChannelMessage. */
    public void testSetMessageURL() throws MalformedURLException, MessageException {
        URL url = new URL("http://www.volantis.com");
        MultiChannelMessage message = new MultiChannelMessageImpl();
        message.setMessageURL(url);
        assertEquals(url, message.getMessageURL());
    }

    /** Test of setMessage method, of class com.volantis.mps.message.MultiChannelMessage. */
    public void testSetMessage() throws MessageException {
        String messageText = "Ardvark";
        MultiChannelMessage message = new MultiChannelMessageImpl();
        message.setMessage(messageText);
        assertEquals(messageText, message.getMessage());
    }

    /** Test of setSubject method, of class com.volantis.mps.message.MultiChannelMessage. */
    public void testSetSubject() throws MessageException {
        String subject = "Ardvark";
        MultiChannelMessage message = new MultiChannelMessageImpl();
        message.setSubject(subject);
        assertEquals(subject, message.getSubject());
    }

    /** Test set/getCharacterEncoding methods and charset constructors */
    public void testCharacterEncoding() throws MessageException {
        final String charSetA = "UTF-8";
        final String charSetB = "Big5";
        MultiChannelMessage mcma = new MultiChannelMessageImpl();
        assertEquals("Charset should be null", null, mcma.getCharacterEncoding());
        mcma.setCharacterEncoding(charSetA);
        assertEquals("Charset should be set to " + charSetA, charSetA, mcma.getCharacterEncoding());
        mcma.setCharacterEncoding(charSetB);
        assertEquals("Charset should be set to " + charSetB, charSetB, mcma.getCharacterEncoding());
        MultiChannelMessage mcmb = new MultiChannelMessageImpl(charSetB);
        assertEquals("Charset should be set to " + charSetB, charSetB, mcmb.getCharacterEncoding());
        mcmb.setCharacterEncoding(charSetA);
        assertEquals("Charset should be set to " + charSetA, charSetA, mcmb.getCharacterEncoding());
    }

    /**
     * Check that MultiChannelMessage is checking the character sets we send in
     *
     * @throws Exception
     */
    public void testCharacterEncodingValidity() throws Exception {
        final String validCharset = "UTF-8";
        final String invalidCharset = "YUCK";
        final URL testURL = new URL("http://localhost/mcm.xml");
        final String sub = "My Subject content";
        final String msgContent = "<?xml version='1.0'?><root></root>";
        MultiChannelMessage mcm = null;
        try {
            mcm = new MultiChannelMessageImpl(null);
            mcm = new MultiChannelMessageImpl(validCharset);
        } catch (IllegalArgumentException e) {
            fail("Exception should not be thrown for a valid charset");
        }
        try {
            mcm = new MultiChannelMessageImpl(invalidCharset);
            fail("Exception should be thrown for invalid charset");
        } catch (IllegalArgumentException e) {
        }
        try {
            mcm = new MultiChannelMessageImpl(testURL, sub, null);
            mcm = new MultiChannelMessageImpl(testURL, sub, validCharset);
        } catch (IllegalArgumentException e) {
            fail("Exception should not be thrown for a valid charset");
        }
        try {
            mcm = new MultiChannelMessageImpl(testURL, sub, invalidCharset);
            fail("Exception should be thrown for invalid charset");
        } catch (IllegalArgumentException e) {
        }
        try {
            mcm = new MultiChannelMessageImpl(msgContent, sub, null);
            mcm = new MultiChannelMessageImpl(msgContent, sub, validCharset);
        } catch (IllegalArgumentException e) {
            fail("Exception should not be thrown for a valid charset");
        }
        try {
            mcm = new MultiChannelMessageImpl(msgContent, sub, invalidCharset);
            fail("Exception should be thrown for invalid charset");
        } catch (IllegalArgumentException e) {
        }
        try {
            mcm = new MultiChannelMessageImpl();
            mcm.setCharacterEncoding(validCharset);
            mcm.setCharacterEncoding(null);
        } catch (IllegalArgumentException e) {
            fail("Exception should not be thrown for a valid charset");
        }
        try {
            mcm = new MultiChannelMessageImpl();
            mcm.setCharacterEncoding(invalidCharset);
            fail("Exception should be thrown for invalid charset");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Testing the generation of a message as a URL.
     *
     * @todo This test fails with Java 1.4.2_13 and Java 1.4.2_14, but works OK
     * with Java 1.4.2_10. The stack trace with Java 1.4.2_13 is
     * java.lang.reflect.UndeclaredThrowableException: java.net.ConnectException: Connection refused
     *     at java.net.PlainSocketImpl.socketConnect(Native Method)
     *     at java.net.PlainSocketImpl.doConnect(PlainSocketImpl.java:305)
     *     at java.net.PlainSocketImpl.connectToAddress(PlainSocketImpl.java:171)
     *     at java.net.PlainSocketImpl.connect(PlainSocketImpl.java:158)
     *     at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
     *     at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
     *     at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
     *     at java.lang.reflect.Method.invoke(Method.java:324)
     *     at junitx.util.PrivateAccessor.invoke(PrivateAccessor.java:253)
     *     at com.volantis.synergetics.testtools.net.SocketImplWrapper.connect(SocketImplWrapper.java:118)
     *     at java.net.Socket.connect(Socket.java:464)
     *     at java.net.Socket.connect(Socket.java:414)
     *     at java.net.Socket.<init>(Socket.java:310)
     *     at java.net.Socket.<init>(Socket.java:125)
     *     at our.apache.commons.httpclient.protocol.DefaultProtocolSocketFactory.createSocket(DefaultProtocolSocketFactory.java:86)
     *     at our.apache.commons.httpclient.HttpConnection$1.doit(HttpConnection.java:660)
     *     at our.apache.commons.httpclient.HttpConnection$SocketTask.run(HttpConnection.java:1291)
     *     at java.lang.Thread.run(Thread.java:534)
     *
     * The test is commented out until we identify and fix the problem.
     */
    public void no_testGenerateTargetMessageAsURL() throws Exception {
        final String baseURL = "volantis";
        final String internalURL = "http://localhost/";
        MultiChannelMessage message = new MultiChannelMessageImpl() {

            protected Volantis getVolantis() {
                return new Volantis() {

                    public String getPageBase() {
                        return baseURL;
                    }

                    public MarinerURL getInternalURL() {
                        return new MarinerURL(internalURL);
                    }
                };
            }
        };
        final String testURL = "http://test.message/file";
        final String deviceName = "Master";
        final String id = "123456789";
        final URL messageURL = new URL(testURL);
        message.setMessageURL(messageURL);
        message.setSubject("Test Message");
        String expected = internalURL + baseURL;
        if (!MultiChannelMessageImpl.SERVLET_PARTIAL_URL.startsWith("/")) {
            expected += "/";
        }
        String servletURL = expected + MultiChannelMessageImpl.SERVLET_PARTIAL_URL;
        URL responseURL = message.generateTargetMessageAsURL(deviceName, servletURL);
        assertEquals("URLs should be the same", messageURL, responseURL);
        String[] content = new String[] { "HTTP/1.1 200 OK", "xmlid: " + id };
        FakeSocketFactory.putResponse(content, 80);
        message.setMessageURL(null);
        message.setMessage("Some sample xml,,,");
        responseURL = message.generateTargetMessageAsURL(deviceName, servletURL);
        expected += MultiChannelMessageImpl.SERVLET_PARTIAL_URL + "%3fpageid%3d" + id;
        assertEquals("URL should match expected", expected, responseURL.toExternalForm());
        FakeSocketFactory.release(80);
        content = new String[] { "HTTP/1.1 200 OK" };
        FakeSocketFactory.putResponse(content, 80);
        try {
            responseURL = message.generateTargetMessageAsURL(deviceName, servletURL);
            fail("This should cause an exception because of no id");
        } catch (MessageException me) {
        }
        FakeSocketFactory.release(80);
        content = new String[] { "HTTP/1.1 500 Internal Server Error" };
        FakeSocketFactory.putResponse(content, 80);
        try {
            responseURL = message.generateTargetMessageAsURL(deviceName, servletURL);
            fail("This should cause an exception because of a failed send");
        } catch (MessageException me) {
        }
        FakeSocketFactory.release(80);
    }

    /** Test of generateTargetMessageAsString method, of class com.volantis.mps.message.MultiChannelMessage. */
    public void testGenerateTargetMessageAsString() {
        MultiChannelMessage message = new MultiChannelMessageImpl();
    }

    /** Test of generateTargetMessageAsMimeMultipart method, of class com.volantis.mps.message.MultiChannelMessage. */
    public void testGenerateTargetMessageAsMimeMultipart() {
    }

    /** Test of addHeader method, of class com.volantis.mps.message.MultiChannelMessage. */
    public void testAddHeader() throws MessageException {
        String headerNameAll = "HeaderAll";
        String headerNameMHTML = "HeaderMHTML";
        String headerNameMMS = "HeaderMMS";
        String headerValueAll = "All";
        String headerValueMHTML = "MHTML";
        String headerValueMMS = "MMS";
        MultiChannelMessage message = new MultiChannelMessageImpl();
        message.addHeader(MultiChannelMessage.ALL, headerNameAll, headerValueAll);
        message.addHeader(MultiChannelMessage.MHTML, headerNameMHTML, headerValueMHTML);
        message.addHeader(MultiChannelMessage.MMS, headerNameMMS, headerValueMMS);
        Map headersAll = message.getHeaders(MultiChannelMessage.ALL);
        Map headersMHTML = message.getHeaders(MultiChannelMessage.MHTML);
        Map headersMMS = message.getHeaders(MultiChannelMessage.MMS);
        Iterator i = headersAll.keySet().iterator();
        int count = 0;
        while (i.hasNext()) {
            count++;
            String key = (String) i.next();
            String value = (String) headersAll.get(key);
            assertEquals(key, headerNameAll);
            assertEquals(value, headerValueAll);
        }
        if (count != 1) {
            fail("Expected one header of type ALL got " + count);
        }
        i = headersMMS.keySet().iterator();
        count = 0;
        while (i.hasNext()) {
            count++;
            String key = (String) i.next();
            String value = (String) headersMMS.get(key);
            assertEquals(key, headerNameMMS);
            assertEquals(value, headerValueMMS);
        }
        if (count != 1) {
            fail("Expected one header of type MMS got " + count);
        }
        i = headersMHTML.keySet().iterator();
        count = 0;
        while (i.hasNext()) {
            count++;
            String key = (String) i.next();
            String value = (String) headersMHTML.get(key);
            assertEquals(key, headerNameMHTML);
            assertEquals(value, headerValueMHTML);
        }
        if (count != 1) {
            fail("Expected one header of type MHTML got " + count);
        }
    }

    /** Test of addAttachments method, of class com.volantis.mps.message.MultiChannelMessage. */
    public void testAddAttachments() throws MessageException {
        MultiChannelMessage message = new MultiChannelMessageImpl();
        MessageAttachments attachments = new MessageAttachments();
        DeviceMessageAttachment attachment = new DeviceMessageAttachment();
        String attachmentChannel = "SMTP";
        String attachmentDevice = "Outlook";
        String attachmentMimeType = "text/plain";
        String attachmentValue = LOCAL_FILE_URLS[0];
        attachment.setChannelName(attachmentChannel);
        attachment.setDeviceName(attachmentDevice);
        attachment.setMimeType(attachmentMimeType);
        attachment.setValue(attachmentValue);
        attachment.setValueType(DeviceMessageAttachment.URL);
        attachments.addAttachment(attachment);
        message.addAttachments(attachments);
        MessageAttachments retrievedMessageAttachments = message.getAttachments();
        Iterator i = retrievedMessageAttachments.iterator();
        int count = 0;
        while (i.hasNext()) {
            count++;
            DeviceMessageAttachment retrievedAttachment = (DeviceMessageAttachment) i.next();
            assertEquals(retrievedAttachment.getChannelName(), attachmentChannel);
            assertEquals(retrievedAttachment.getDeviceName(), attachmentDevice);
            assertEquals(retrievedAttachment.getMimeType(), attachmentMimeType);
            assertEquals(retrievedAttachment.getValue(), attachmentValue);
        }
        if (count != 1) {
            fail("Expected one attachment got " + count);
        }
    }

    /** Test of removeAttachments method, of class com.volantis.mps.message.MultiChannelMessage. */
    public void testRemoveAttachments() throws MessageException {
        MultiChannelMessage message = new MultiChannelMessageImpl();
        MessageAttachments attachments = new MessageAttachments();
        String attachmentChannel = "SMTP";
        String attachmentDevice = "Outlook";
        String attachmentMimeType = "text/plain";
        String[] attachmentValues = { LOCAL_FILE_URLS[0], LOCAL_FILE_URLS[1], LOCAL_FILE_URLS[2] };
        for (int n = 0; n < attachmentValues.length; n++) {
            DeviceMessageAttachment attachment = new DeviceMessageAttachment();
            attachment.setChannelName(attachmentChannel);
            attachment.setDeviceName(attachmentDevice);
            attachment.setMimeType(attachmentMimeType);
            attachment.setValue(attachmentValues[n]);
            attachment.setValueType(DeviceMessageAttachment.URL);
            attachments.addAttachment(attachment);
        }
        message.addAttachments(attachments);
        message.removeAttachments();
        MessageAttachments retrievedMessageAttachments = message.getAttachments();
        assertNull(retrievedMessageAttachments);
    }

    /** Test of getHeaders method, of class com.volantis.mps.message.MultiChannelMessage. */
    public void testGetHeaders() throws MessageException {
        String headerNameAll = "HeaderAll";
        String headerNameMHTML = "HeaderMHTML";
        String headerNameMMS = "HeaderMMS";
        String headerValueAll = "All";
        String headerValueMHTML = "MHTML";
        String headerValueMMS = "MMS";
        MultiChannelMessage message = new MultiChannelMessageImpl();
        message.addHeader(MultiChannelMessage.ALL, headerNameAll, headerValueAll);
        message.addHeader(MultiChannelMessage.MHTML, headerNameMHTML, headerValueMHTML);
        message.addHeader(MultiChannelMessage.MMS, headerNameMMS, headerValueMMS);
        Map headersAll = message.getHeaders(MultiChannelMessage.ALL);
        Map headersMHTML = message.getHeaders(MultiChannelMessage.MHTML);
        Map headersMMS = message.getHeaders(MultiChannelMessage.MMS);
        Iterator i = headersAll.keySet().iterator();
        int count = 0;
        while (i.hasNext()) {
            count++;
            String key = (String) i.next();
            String value = (String) headersAll.get(key);
            assertEquals(key, headerNameAll);
            assertEquals(value, headerValueAll);
        }
        if (count != 1) {
            fail("Expected one header of type ALL got " + count);
        }
        i = headersMMS.keySet().iterator();
        count = 0;
        while (i.hasNext()) {
            count++;
            String key = (String) i.next();
            String value = (String) headersMMS.get(key);
            assertEquals(key, headerNameMMS);
            assertEquals(value, headerValueMMS);
        }
        if (count != 1) {
            fail("Expected one header of type MMS got " + count);
        }
        i = headersMHTML.keySet().iterator();
        count = 0;
        while (i.hasNext()) {
            count++;
            String key = (String) i.next();
            String value = (String) headersMHTML.get(key);
            assertEquals(key, headerNameMHTML);
            assertEquals(value, headerValueMHTML);
        }
        if (count != 1) {
            fail("Expected one header of type MHTML got " + count);
        }
    }

    /** Test of getAttachments method, of class com.volantis.mps.message.MultiChannelMessage. */
    public void testGetAttachments() throws MessageException {
        MultiChannelMessage message = new MultiChannelMessageImpl();
        MessageAttachments attachments = new MessageAttachments();
        DeviceMessageAttachment attachment = new DeviceMessageAttachment();
        String attachmentChannel = "SMTP";
        String attachmentDevice = "Outlook";
        String attachmentMimeType = "text/plain";
        String attachmentValue = LOCAL_FILE_URLS[0];
        attachment.setChannelName(attachmentChannel);
        attachment.setDeviceName(attachmentDevice);
        attachment.setMimeType(attachmentMimeType);
        attachment.setValue(attachmentValue);
        attachment.setValueType(DeviceMessageAttachment.URL);
        attachments.addAttachment(attachment);
        message.addAttachments(attachments);
        MessageAttachments retrievedMessageAttachments = message.getAttachments();
        Iterator i = retrievedMessageAttachments.iterator();
        int count = 0;
        while (i.hasNext()) {
            count++;
            DeviceMessageAttachment retrievedAttachment = (DeviceMessageAttachment) i.next();
            assertEquals(retrievedAttachment.getChannelName(), attachmentChannel);
            assertEquals(retrievedAttachment.getDeviceName(), attachmentDevice);
            assertEquals(retrievedAttachment.getMimeType(), attachmentMimeType);
            assertEquals(retrievedAttachment.getValue(), attachmentValue);
        }
        if (count != 1) {
            fail("Expected one attachment got " + count);
        }
    }

    /** Test of clone method, of class com.volantis.mps.message.MultiChannelMessage. */
    public void testClone() throws MalformedURLException, MessageException {
        String messageText = "Message Text 1";
        URL url = new URL("http://www.volantis.com/1");
        String subject = "Subject 1";
        String headerAllName = "HeaderAll1";
        String headerAllValue = "AllValue1";
        String headerMMSName = "HeaderMMS1";
        String headerMMSValue = "value1";
        String headerMHTMLName = "HeaderMHTML1";
        String headerMHTMLValue = "value1";
        MessageAttachments attachments = new MessageAttachments();
        DeviceMessageAttachment attachment = new DeviceMessageAttachment();
        String attachmentChannel = "SMTP";
        String attachmentDevice = "Outlook";
        String attachmentMimeType = "text/plain";
        String attachmentValue = LOCAL_FILE_URLS[0];
        MultiChannelMessageImpl message = new MultiChannelMessageImpl();
        message.setMessage(messageText);
        message.setMessageURL(url);
        message.setSubject(subject);
        message.addHeader(MultiChannelMessage.ALL, headerAllName, headerAllValue);
        message.addHeader(MultiChannelMessage.MMS, headerMMSName, headerMMSValue);
        message.addHeader(MultiChannelMessage.MHTML, headerMHTMLName, headerMHTMLValue);
        attachment.setChannelName(attachmentChannel);
        attachment.setDeviceName(attachmentDevice);
        attachment.setMimeType(attachmentMimeType);
        attachment.setValue(attachmentValue);
        attachment.setValueType(DeviceMessageAttachment.URL);
        attachments.addAttachment(attachment);
        message.addAttachments(attachments);
        MultiChannelMessage clone = (MultiChannelMessage) message.clone();
        message.removeAttachments();
        message.setMessage(null);
        message.setMessageURL(null);
        message.setSubject(null);
        message = null;
        assertEquals(clone.getMessage(), messageText);
        assertEquals(clone.getMessageURL(), url);
        assertEquals(clone.getSubject(), subject);
        MessageAttachments retrievedMessageAttachments = clone.getAttachments();
        Iterator i = retrievedMessageAttachments.iterator();
        int count = 0;
        while (i.hasNext()) {
            count++;
            DeviceMessageAttachment retrievedAttachment = (DeviceMessageAttachment) i.next();
            assertEquals(retrievedAttachment.getChannelName(), attachmentChannel);
            assertEquals(retrievedAttachment.getDeviceName(), attachmentDevice);
            assertEquals(retrievedAttachment.getMimeType(), attachmentMimeType);
            assertEquals(retrievedAttachment.getValue(), attachmentValue);
        }
        if (count != 1) {
            fail("Expected one attachment got " + count);
        }
        Map headersAll = clone.getHeaders(MultiChannelMessage.ALL);
        Map headersMHTML = clone.getHeaders(MultiChannelMessage.MHTML);
        Map headersMMS = clone.getHeaders(MultiChannelMessage.MMS);
        i = headersAll.keySet().iterator();
        count = 0;
        while (i.hasNext()) {
            count++;
            String key = (String) i.next();
            String value = (String) headersAll.get(key);
            assertEquals(key, headerAllName);
            assertEquals(value, headerAllValue);
        }
        if (count != 1) {
            fail("Expected one header of type ALL got " + count);
        }
        i = headersMMS.keySet().iterator();
        count = 0;
        while (i.hasNext()) {
            count++;
            String key = (String) i.next();
            String value = (String) headersMMS.get(key);
            assertEquals(key, headerMMSName);
            assertEquals(value, headerMMSValue);
        }
        if (count != 1) {
            fail("Expected one header of type MMS got " + count);
        }
        i = headersMHTML.keySet().iterator();
        count = 0;
        while (i.hasNext()) {
            count++;
            String key = (String) i.next();
            String value = (String) headersMHTML.get(key);
            assertEquals(key, headerMHTMLName);
            assertEquals(value, headerMHTMLValue);
        }
        if (count != 1) {
            fail("Expected one header of type MHTML got " + count);
        }
    }
}

package com.volantis.mps.recipient;

import javax.mail.internet.InternetAddress;
import com.volantis.mcs.runtime.Volantis;
import com.volantis.testtools.stubs.ServletContextStub;
import com.volantis.synergetics.testtools.TestCaseAbstract;
import junitx.util.PrivateAccessor;

/**
 * Test the <code>MessageRecipient</code> class.
 */
public class MessageRecipientTestCase extends TestCaseAbstract {

    protected Volantis volantisBean = null;

    protected ServletContextStub servletContext;

    public MessageRecipientTestCase(java.lang.String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test constructing {@link MessageRecipient} instances
     */
    public void testContructors() throws Exception {
        MessageRecipient messageRecipient1 = new MessageRecipient();
        InternetAddress internetAddress = new InternetAddress("ianw@volantis.com");
        String deviceName = "PC";
        MessageRecipient messageRecipient2 = new MessageRecipient(internetAddress, deviceName);
        if (messageRecipient1.getAddress() != null) {
            fail("Empty MessageRecipient getAddress not null");
        }
        if (messageRecipient1.getDeviceName() != null) {
            fail("Empty MessageRecipient getDeviceName not null");
        }
        if (messageRecipient2.getAddress() != internetAddress) {
            fail("Non empty MessageRecipient getAddress not equal");
        }
        if (!messageRecipient2.getDeviceName().equals(deviceName)) {
            fail("Non empty MessageRecipient getDeviceName not equal");
        }
    }

    /**
     * Test of setAddress method from {@link MessageRecipient}
     */
    public void testSetAddress() throws Exception {
        MessageRecipient messageRecipient = new MessageRecipient();
        InternetAddress internetAddress = new InternetAddress("ianw@volantis.com");
        messageRecipient.setAddress(internetAddress);
        if (messageRecipient.getAddress() != internetAddress) {
            fail("Address not equal");
        }
    }

    /**
     * Test of getAddress method from {@link MessageRecipient}
     */
    public void testGetAddress() throws Exception {
        MessageRecipient messageRecipient = new MessageRecipient();
        InternetAddress internetAddress = new InternetAddress("ianw@volantis.com");
        messageRecipient.setAddress(internetAddress);
        if (!messageRecipient.getAddress().equals(internetAddress)) {
            fail("Address not equal");
        }
    }

    /**
     * Test of setMSISDN method from MessageRecipient}
     */
    public void testSetMSISDN() throws Exception {
        MessageRecipient messageRecipient = new MessageRecipient();
        String msISDN = "12345678";
        messageRecipient.setMSISDN(msISDN);
        if (!messageRecipient.getMSISDN().equals(msISDN)) {
            fail("MSISDN not equal");
        }
    }

    /**
     * Test of setDeviceName method from {@link MessageRecipient}
     */
    public void testSetDeviceName() throws Exception {
        MessageRecipient messageRecipient = new MessageRecipient();
        String deviceName = "PC";
        messageRecipient.setDeviceName(deviceName);
        if (!messageRecipient.getDeviceName().equals(deviceName)) {
            fail("DeviceName not equal");
        }
    }

    /**
     * Test of setChannelName method from {@link MessageRecipient}
     */
    public void testSetChannelName() throws Exception {
        MessageRecipient messageRecipient = new MessageRecipient();
        String channelName = "SMTP";
        messageRecipient.setChannelName(channelName);
        if (!messageRecipient.getChannelName().equals(channelName)) {
            fail("ChannelName not equal");
        }
    }

    /**
     * Test of setRecipientType method from {@link MessageRecipient}
     */
    public void testSetRecipientType() throws Exception {
        MessageRecipient messageRecipient = new MessageRecipient();
        int recipientType = MessageRecipient.NOT_RESOLVED;
        messageRecipient.setRecipientType(recipientType);
        assertEquals("Recipient type should be NOT_RESOLVED", messageRecipient.getRecipientType(), MessageRecipient.NOT_RESOLVED);
        recipientType = MessageRecipient.OK;
        messageRecipient.setRecipientType(recipientType);
        assertEquals("Recipient type should be OK", messageRecipient.getRecipientType(), MessageRecipient.OK);
    }

    /**
     * Test of resolveDeviceName method from {@link MessageRecipient}
     */
    public void testResolveDeviceName() throws Exception {
        MessageRecipient messageRecipient = new MessageRecipient();
        PrivateAccessor.setField(messageRecipient, "messageRecipientInfo", new MessageRecipientInfoTestHelper());
        messageRecipient.resolveDeviceName(false);
        assertEquals("Resolved device not equal to default", "Outlook", messageRecipient.getDeviceName());
        messageRecipient.setDeviceName("aardvark");
        messageRecipient.resolveDeviceName(false);
        assertEquals("Resolved device overrode current value", "aardvark", messageRecipient.getDeviceName());
        messageRecipient.resolveDeviceName(true);
        assertEquals("Resolved device did not overide current value", "Outlook", messageRecipient.getDeviceName());
    }

    /**
     * Test of resolveChannelName method from {@link MessageRecipient}
     */
    public void testResolveChannelName() throws Exception {
        MessageRecipient messageRecipient = new MessageRecipient();
        PrivateAccessor.setField(messageRecipient, "messageRecipientInfo", new MessageRecipientInfoTestHelper());
        messageRecipient.resolveChannelName(false);
        assertEquals("Resolved channel not equal to default", "smtp", messageRecipient.getChannelName());
        messageRecipient.setChannelName("aardvark");
        messageRecipient.resolveChannelName(false);
        assertEquals("Resolved channel overrode current value", "aardvark", messageRecipient.getChannelName());
        messageRecipient.resolveChannelName(true);
        assertEquals("Resolved channel did not override current value", "smtp", messageRecipient.getChannelName());
    }

    /**
     * Test of clone method from {@link MessageRecipient}
     */
    public void testClone() throws Exception {
        MessageRecipient messageRecipient = new MessageRecipient();
        InternetAddress internetAddressOrig = new InternetAddress("ianw@volantis.com");
        InternetAddress internetAddressNew = new InternetAddress("mat@volantis.com");
        String channelNameOrig = "smtp";
        String channelNameNew = "smsc";
        String deviceNameOrig = "Outlook";
        String deviceNameNew = "SMS Hamdset";
        String msISDNOrig = "12345678";
        String msISDNNew = "87654321";
        messageRecipient.setAddress(internetAddressOrig);
        messageRecipient.setChannelName(channelNameOrig);
        messageRecipient.setDeviceName(deviceNameOrig);
        messageRecipient.setMSISDN(msISDNOrig);
        MessageRecipient clone = (MessageRecipient) messageRecipient.clone();
        assertEquals("Cloned address not equal", messageRecipient.getAddress(), clone.getAddress());
        assertEquals("Cloned channel name not equal", messageRecipient.getChannelName(), clone.getChannelName());
        assertEquals("Cloned device name not equal", messageRecipient.getDeviceName(), clone.getDeviceName());
        assertEquals("Cloned MSISDN not equal", messageRecipient.getMSISDN(), clone.getMSISDN());
        messageRecipient.setAddress(internetAddressNew);
        messageRecipient.setChannelName(channelNameNew);
        messageRecipient.setDeviceName(deviceNameNew);
        messageRecipient.setMSISDN(msISDNNew);
        assertFalse("Cloned address equal", messageRecipient.getAddress().equals(clone.getAddress()));
        assertFalse("Cloned channel name equal", messageRecipient.getChannelName().equals(clone.getChannelName()));
        assertFalse("Cloned device name equal", messageRecipient.getDeviceName().equals(clone.getDeviceName()));
        assertFalse("Cloned MSISDN equal", messageRecipient.getMSISDN().equals(clone.getDeviceName()));
    }
}

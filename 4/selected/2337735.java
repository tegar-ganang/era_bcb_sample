package com.volantis.mps.attachment;

import com.volantis.mps.message.MessageException;

/**
 * This specialises the MessageAttachement tests for
 * {@link DeviceMessageAttachment}.
 */
public class DeviceMessageAttachmentTestCase extends MessageAttachmentTestAbstract {

    /**
     * A valid device name that can be used for the test instances.
     */
    protected static final String DEVICE_NAME = "Thunderbird";

    /**
     * A valid channle name that can be used for the test instances.
     */
    protected static final String CHANNEL_NAME = "smtp";

    /**
     * Initialise a new instance of this test case.
     */
    public DeviceMessageAttachmentTestCase() {
    }

    /**
     * Initialise a new named instance of this test case.
     *
     * @param s The name of the test case.
     */
    public DeviceMessageAttachmentTestCase(String s) {
        super(s);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * This tests the creation of DeviceMessageAttachements
     */
    public void testDeviceMessageAttachmentCreation() throws Exception {
        DeviceMessageAttachment ma = (DeviceMessageAttachment) getTestInstance();
        ma = (DeviceMessageAttachment) getTestInstance(VALUE, "text/plain", MessageAttachment.FILE);
        try {
            ma = new DeviceMessageAttachment(VALUE, "text/plain", MessageAttachment.FILE, null, "smtp");
            fail("Previous call should have caused an exception (1)");
        } catch (MessageException me) {
        }
        try {
            ma = new DeviceMessageAttachment(VALUE, "text/plain", MessageAttachment.FILE, "Thunderbird", null);
            fail("Previous call should have caused an exception (2)");
        } catch (MessageException me) {
        }
    }

    /**
     * This tests {set|get}DeviceName methods.
     */
    public void testDeviceNames() throws Exception {
        final String anotherDevice = "Mozilla";
        DeviceMessageAttachment ma = new DeviceMessageAttachment(VALUE, MIME, VALUE_TYPE, DEVICE_NAME, CHANNEL_NAME);
        String retrieved = ma.getDeviceName();
        assertEquals("Values should match", DEVICE_NAME, retrieved);
        try {
            ma.setDeviceName(null);
            fail("Previous call should have caused an exception");
        } catch (MessageException me) {
        }
        ma.setDeviceName(anotherDevice);
        retrieved = ma.getDeviceName();
        assertEquals("Values should match", anotherDevice, retrieved);
    }

    /**
     * This tests {set|get}ChannelName methods.
     */
    public void testChannelNames() throws Exception {
        final String anotherChannel = "Mozilla";
        DeviceMessageAttachment ma = new DeviceMessageAttachment(VALUE, MIME, VALUE_TYPE, DEVICE_NAME, CHANNEL_NAME);
        String retrieved = ma.getChannelName();
        assertEquals("Values should match", CHANNEL_NAME, retrieved);
        try {
            ma.setChannelName(null);
            fail("Previous call should have caused an exception");
        } catch (MessageException me) {
        }
        ma.setChannelName(anotherChannel);
        retrieved = ma.getChannelName();
        assertEquals("Values should match", anotherChannel, retrieved);
    }

    /**
     * This tests the equality functionality.
     */
    public void testEquals() throws Exception {
        DeviceMessageAttachment attachment = (DeviceMessageAttachment) getTestInstance();
        DeviceMessageAttachment anotherAttachment = (DeviceMessageAttachment) getTestInstance();
        assertTrue("Attachments should be equal (1)", attachment.equals(anotherAttachment));
        assertTrue("Attachments should be equal (2)", anotherAttachment.equals(attachment));
        DeviceMessageAttachment one = new DeviceMessageAttachment(VALUE, MIME, VALUE_TYPE, "Evolution", CHANNEL_NAME);
        DeviceMessageAttachment two = new DeviceMessageAttachment(VALUE, MIME, VALUE_TYPE, "Evolution", CHANNEL_NAME);
        DeviceMessageAttachment three = new DeviceMessageAttachment(VALUE, MIME, VALUE_TYPE, "Netscape", CHANNEL_NAME);
        assertTrue("Attachments should be equal (3)", one.equals(two));
        assertTrue("Attachments should not be equal", !one.equals(three));
    }

    public MessageAttachment getTestInstance() {
        return new DeviceMessageAttachment();
    }

    public MessageAttachment getTestInstance(String value, String mimeType, int valueType) throws MessageException {
        return new DeviceMessageAttachment(value, mimeType, valueType, DEVICE_NAME, CHANNEL_NAME);
    }
}

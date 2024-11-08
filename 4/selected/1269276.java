package com.volantis.mps.recipient;

import java.util.Iterator;
import javax.mail.internet.InternetAddress;
import com.volantis.mcs.runtime.Volantis;
import com.volantis.synergetics.testtools.TestCaseAbstract;
import com.volantis.testtools.stubs.ServletContextStub;
import junitx.util.PrivateAccessor;

/**
 * This tests the <code>MessageRecipients</code> class.
 */
public class MessageRecipientsTestCase extends TestCaseAbstract {

    protected Volantis volantisBean = null;

    protected ServletContextStub servletContext;

    public MessageRecipientsTestCase(java.lang.String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of addRecipient method from {@link MessageRecipients}
     */
    public void testAddRecipient() throws Exception {
        MessageRecipients messageRecipients = new MessageRecipients();
        String userPrefix = "bozo";
        String userSuffix = "@volantis.com";
        for (int n = 9; n > 0; n--) {
            InternetAddress address = new InternetAddress(userPrefix + n + userSuffix);
            MessageRecipient messageRecipient = new MessageRecipient(address, null);
            messageRecipients.addRecipient(messageRecipient);
        }
        Iterator i = messageRecipients.getIterator();
        int tot = 0;
        while (i.hasNext()) {
            MessageRecipient messageRecipient = (MessageRecipient) i.next();
            InternetAddress testAddress = new InternetAddress(userPrefix + (tot + 1) + userSuffix);
            assertEquals("Wrong recipient ", testAddress.getAddress(), messageRecipient.getAddress().getAddress());
            tot++;
        }
        assertEquals("Expecting 10 recipients", tot, 9);
    }

    /**
     * Test of removeRecipient method from {@link MessageRecipients}
     */
    public void testRemoveRecipient() throws Exception {
        MessageRecipients messageRecipients = new MessageRecipients();
        String userPrefix = "bozo";
        String userSuffix = "@volantis.com";
        InternetAddress userToRemove = new InternetAddress("bozo5@volantis.com");
        for (int n = 9; n > 0; n--) {
            InternetAddress address = new InternetAddress(userPrefix + n + userSuffix);
            MessageRecipient messageRecipient = new MessageRecipient(address, null);
            messageRecipients.addRecipient(messageRecipient);
        }
        messageRecipients.removeRecipient(new MessageRecipient(userToRemove, null));
        Iterator i = messageRecipients.getIterator();
        int tot = 0;
        while (i.hasNext()) {
            MessageRecipient messageRecipient = (MessageRecipient) i.next();
            InternetAddress testAddress = new InternetAddress(userPrefix + (tot + 1) + userSuffix);
            if ((tot + 1) == 5) {
                assertFalse("Message recipient " + testAddress.getAddress() + " should have been removed", messageRecipient.getAddress().equals(testAddress));
                tot++;
                testAddress = new InternetAddress(userPrefix + (tot + 1) + userSuffix);
            }
            assertEquals("Wrong recipient ", testAddress.getAddress(), messageRecipient.getAddress().getAddress());
            tot++;
        }
        assertEquals("Expecting 9 recipients", tot, 9);
    }

    /**
     * Test of resolveDeviceNames method from {@link MessageRecipients}
     */
    public void testResolveDeviceNames() throws Exception {
        MessageRecipients messageRecipients = new MessageRecipients();
        String userPrefix = "bozo";
        String userSuffix = "@volantis.com";
        String deviceName;
        for (int n = 9; n > 0; n--) {
            InternetAddress address = new InternetAddress(userPrefix + n + userSuffix);
            if (n % 2 == 0) {
                deviceName = "Ardvark";
            } else {
                deviceName = null;
            }
            MessageRecipient messageRecipient = new MessageRecipient(address, deviceName);
            PrivateAccessor.setField(messageRecipient, "messageRecipientInfo", new MessageRecipientInfoTestHelper());
            messageRecipients.addRecipient(messageRecipient);
        }
        messageRecipients.resolveDeviceNames(false);
        Iterator i = messageRecipients.getIterator();
        int tot = 0;
        while (i.hasNext()) {
            MessageRecipient messageRecipient = (MessageRecipient) i.next();
            int n;
            if (tot < 4) {
                n = (tot + 1) * 2;
            } else {
                n = (tot - 4) * 2 + 1;
            }
            InternetAddress testAddress = new InternetAddress(userPrefix + n + userSuffix);
            assertEquals("Wrong recipient ", testAddress.getAddress(), messageRecipient.getAddress().getAddress());
            if (n % 2 == 0) {
                assertEquals("Device name has been overidden", "Ardvark", messageRecipient.getDeviceName());
            } else {
                assertEquals("Device name has not been overidden", "Outlook", messageRecipient.getDeviceName());
            }
            tot++;
        }
        assertEquals("Expecting 9 recipients", tot, 9);
        messageRecipients.resolveDeviceNames(true);
        i = messageRecipients.getIterator();
        tot = 0;
        while (i.hasNext()) {
            tot++;
            MessageRecipient messageRecipient = (MessageRecipient) i.next();
            InternetAddress testAddress = new InternetAddress(userPrefix + tot + userSuffix);
            assertEquals("Wrong recipient ", testAddress.getAddress(), messageRecipient.getAddress().getAddress());
            assertEquals("Device name has not been overidden", "Outlook", messageRecipient.getDeviceName());
        }
        assertEquals("Expecting 9 recipients", tot, 9);
    }

    /**
     * Test of resolveChannelNames method from {@link MessageRecipients}
     */
    public void testResolveChannelNames() throws Exception {
        MessageRecipients messageRecipients = new MessageRecipients();
        String userPrefix = "bozo";
        String userSuffix = "@volantis.com";
        String channelName;
        for (int n = 9; n > 0; n--) {
            InternetAddress address = new InternetAddress(userPrefix + n + userSuffix);
            if (n % 2 == 0) {
                channelName = "Ardvark";
            } else {
                channelName = null;
            }
            MessageRecipient messageRecipient = new MessageRecipient(address, "Outlook");
            messageRecipient.setChannelName(channelName);
            PrivateAccessor.setField(messageRecipient, "messageRecipientInfo", new MessageRecipientInfoTestHelper());
            messageRecipients.addRecipient(messageRecipient);
        }
        messageRecipients.resolveChannelNames(false);
        Iterator i = messageRecipients.getIterator();
        int tot = 0;
        while (i.hasNext()) {
            MessageRecipient messageRecipient = (MessageRecipient) i.next();
            int n;
            if (tot < 4) {
                n = (tot + 1) * 2;
            } else {
                n = (tot - 4) * 2 + 1;
            }
            InternetAddress testAddress = new InternetAddress(userPrefix + n + userSuffix);
            assertEquals("Wrong recipient ", testAddress.getAddress(), messageRecipient.getAddress().getAddress());
            if (n % 2 == 0) {
                assertEquals("Channel name has been overriden", "Ardvark", messageRecipient.getChannelName());
            } else {
                assertEquals("Channel name has been overriden", "smtp", messageRecipient.getChannelName());
            }
            tot++;
        }
        assertEquals("Expecting 9 recipients", tot, 9);
        messageRecipients.resolveChannelNames(true);
        i = messageRecipients.getIterator();
        tot = 0;
        while (i.hasNext()) {
            tot++;
            MessageRecipient messageRecipient = (MessageRecipient) i.next();
            InternetAddress testAddress = new InternetAddress(userPrefix + tot + userSuffix);
            assertEquals("Wrong recipient ", testAddress.getAddress(), messageRecipient.getAddress().getAddress());
            assertEquals("Channel name has not been overriden", messageRecipient.getChannelName(), "smtp");
        }
        assertEquals("Expecting 9 recipients got ", tot, 9);
    }

    /**
     * Test of getIterator method from {@link MessageRecipients}
     */
    public void notestGetIterator() {
    }
}

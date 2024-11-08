package com.volantis.mps.bms;

import junit.framework.TestCase;
import java.net.URL;

public class ModelObjectsCreationInvariantsTestCase extends TestCase {

    MessageFactory factory;

    protected void setUp() throws Exception {
        super.setUp();
        factory = MessageFactory.getDefaultInstance();
    }

    public void testCreateMSISDN() throws Exception {
        MSISDN msisdn = factory.createMSISDN("+447777123456789");
        assertNotNull(msisdn);
        assertEquals("+447777123456789", msisdn.getValue());
    }

    public void testNullMSISDNFails() throws Exception {
        try {
            factory.createMSISDN(null);
            fail("Shouldn't allow null msisdn");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testCreateSMTPAddress() throws Exception {
        SMTPAddress address = factory.createSMTPAddress("james.abley@volantis.com");
        assertNotNull(address);
        assertEquals("james.abley@volantis.com", address.getValue());
    }

    public void testNullSMTPAddressFails() throws Exception {
        try {
            factory.createSMTPAddress(null);
            fail("Shouldn't allow null smtp address");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testCreateRecipient() throws Exception {
        Address address = factory.createMSISDN("07777123456789");
        Recipient recipient = factory.createRecipient(address, "Nokia-6600");
        assertNotNull(recipient);
        assertEquals("07777123456789", recipient.getAddress().getValue());
        assertEquals("Default recipient Type is TO", RecipientType.TO, recipient.getRecipientType());
        assertEquals("Nokia-6600", recipient.getDeviceName());
        assertNull("Default Channel is null", recipient.getChannel());
    }

    public void testCreateRecipientWithNullAddressFails() throws Exception {
        try {
            factory.createRecipient(null, "Nokia-6600");
            fail("Shouldn't allow recipient with null address");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testCreateRecipientWithNullDeviceFails() throws Exception {
        try {
            Address address = factory.createMSISDN("07777123456789");
            factory.createRecipient(address, null);
            fail("Shouldn't allow recipient with null device");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testCreateMessage() throws Exception {
        Message message = factory.createMessage(new URL("http://localhost/mcs/some.page.xdime"));
        assertNotNull(message);
    }
}

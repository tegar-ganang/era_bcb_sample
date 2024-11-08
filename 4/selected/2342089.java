package com.volantis.mps.bms.impl.ser;

import com.volantis.mps.bms.Address;
import com.volantis.mps.bms.Failures;
import com.volantis.mps.bms.Message;
import com.volantis.mps.bms.MessageFactory;
import com.volantis.mps.bms.Recipient;
import com.volantis.mps.bms.RecipientType;
import com.volantis.mps.bms.SendRequest;
import org.custommonkey.xmlunit.XMLTestCase;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

public class JiBXParserTestCase extends XMLTestCase {

    /**
     * A test message to be embedded in a send request document. Note that
     * in production use the contents of the message to be sent should be
     * XDIME (XML encoded) wrapped in the message element.
     */
    private static String MESSAGE = "Test message";

    private static final String RECIPIENT_ALICE = "<recipient>" + "<address>alice@volantis.com</address>" + "<channel-name>smtp</channel-name>" + "<device-name>Nokia-6600</device-name>" + "</recipient>";

    private static final String RECIPIENT_ALICE_WITH_TYPE = "<recipient>" + "<address>alice@volantis.com</address>" + "<channel-name>smtp</channel-name>" + "<device-name>Nokia-6600</device-name>" + "<type>TO</type>" + "</recipient>";

    private static final String RECIPIENT_BOB = "<recipient>" + "<address>bob@volantis.com</address>" + "<channel-name>smtp</channel-name>" + "<device-name>Nokia-6800</device-name>" + "</recipient>";

    private static final String RECIPIENT_BOB_WITH_TYPE = "<recipient>" + "<address>bob@volantis.com</address>" + "<channel-name>smtp</channel-name>" + "<device-name>Nokia-6800</device-name>" + "<type>TO</type>" + "</recipient>";

    private static final String SEND_REQUEST_BASIC = "<?xml version='1.0' encoding='UTF-8'?>" + "<send-request xmlns='http://www.volantis.com/xmlns/2006/11/mps/ws'>" + "<recipients>" + RECIPIENT_ALICE + "</recipients>" + "<message>" + "<subject>Hello</subject>" + "<url>http://localhost:8080/volantis/welcome/welcome.xdime</url>" + "</message>" + "</send-request>";

    private static final String SEND_REQUEST_BASIC_EMBEDDED_CONTENT = "<?xml version='1.0' encoding='UTF-8'?>" + "<send-request xmlns='http://www.volantis.com/xmlns/2006/11/mps/ws'>" + "<recipients>" + RECIPIENT_ALICE + "</recipients>" + "<message>" + "<subject/>" + "<content>" + MESSAGE + "</content>" + "</message>" + "<sender/>" + "</send-request>";

    private static final String SEND_REQUEST_BASIC_EMBEDDED_CONTENT_WITH_TYPE = "<?xml version='1.0' encoding='UTF-8'?>" + "<send-request xmlns='http://www.volantis.com/xmlns/2006/11/mps/ws'>" + "<recipients>" + RECIPIENT_ALICE_WITH_TYPE + "</recipients>" + "<message>" + "<subject/>" + "<content>" + MESSAGE + "</content>" + "</message>" + "<sender/>" + "</send-request>";

    private static final String SEND_REQUEST_BASIC_WITH_TYPE = "<?xml version='1.0' encoding='UTF-8'?>" + "<send-request xmlns='http://www.volantis.com/xmlns/2006/11/mps/ws'>" + "<recipients>" + RECIPIENT_ALICE_WITH_TYPE + "</recipients>" + "<message>" + "<subject>Hello</subject>" + "<url>http://localhost:8080/volantis/welcome/welcome.xdime</url>" + "</message>" + "<sender/>" + "</send-request>";

    private static final String RECIPIENT_CAROL = "<recipient>" + "<address>carol@volantis.com</address>" + "<channel-name>smtp</channel-name>" + "<device-name>SonyEriccson-973i</device-name>" + "</recipient>";

    private static final String RECIPIENT_CAROL_WITH_TYPE = "<recipient>" + "<address>carol@volantis.com</address>" + "<channel-name>smtp</channel-name>" + "<device-name>SonyEriccson-973i</device-name>" + "<type>TO</type>" + "</recipient>";

    private static final String RECIPIENT_DAVE = "<recipient>" + "<address>dave@volantis.com</address>" + "<channel-name>smtp</channel-name>" + "<device-name>Samsung-D700</device-name>" + "</recipient>";

    private static final String RECIPIENT_DAVE_WITH_TYPE = "<recipient>" + "<address>dave@volantis.com</address>" + "<channel-name>smtp</channel-name>" + "<device-name>Samsung-D700</device-name>" + "<type>TO</type>" + "</recipient>";

    private static final String SENDER_EVE = "<sender>" + "<smtp-address>eve@volantis.com</smtp-address>" + "</sender>";

    private static final String SEND_REQUEST_MULTIPLE_RECIPIENT = "<?xml version='1.0' encoding='UTF-8'?>" + "<send-request xmlns='http://www.volantis.com/xmlns/2006/11/mps/ws'>" + "<recipients>" + RECIPIENT_ALICE + RECIPIENT_BOB + RECIPIENT_CAROL + RECIPIENT_DAVE + "</recipients>" + "<message>" + "<subject>Goodbye</subject>" + "<url>http://some.host.com:6000/volantis/welcome/welcome.xdime</url>" + "</message>" + "</send-request>";

    private static final String SEND_REQUEST_BASIC_WITH_SENDER = "<?xml version='1.0' encoding='UTF-8'?>" + "<send-request xmlns='http://www.volantis.com/xmlns/2006/11/mps/ws'>" + "<recipients>" + RECIPIENT_ALICE + "</recipients>" + "<message>" + "<subject>Hello</subject>" + "<url>http://localhost:8080/volantis/welcome/welcome.xdime</url>" + "</message>" + SENDER_EVE + "</send-request>";

    private static final String SEND_REQUEST_MULTIPLE_RECIPIENT_WITH_SENDER = "<?xml version='1.0' encoding='UTF-8'?>" + "<send-request xmlns='http://www.volantis.com/xmlns/2006/11/mps/ws'>" + "<recipients>" + RECIPIENT_ALICE + RECIPIENT_BOB + RECIPIENT_CAROL + RECIPIENT_DAVE + "</recipients>" + "<message>" + "<subject>Goodbye</subject>" + "<url>http://some.host.com:6000/volantis/welcome/welcome.xdime</url>" + "</message>" + SENDER_EVE + "</send-request>";

    private static final String SEND_REQUEST_MULTIPLE_RECIPIENT_WITH_SENDER_AND_TYPES = "<?xml version='1.0' encoding='UTF-8'?>" + "<send-request xmlns='http://www.volantis.com/xmlns/2006/11/mps/ws'>" + "<recipients>" + RECIPIENT_ALICE_WITH_TYPE + RECIPIENT_BOB_WITH_TYPE + RECIPIENT_CAROL_WITH_TYPE + RECIPIENT_DAVE_WITH_TYPE + "</recipients>" + "<message>" + "<subject>Goodbye</subject>" + "<url>http://some.host.com:6000/volantis/welcome/welcome.xdime</url>" + "</message>" + SENDER_EVE + "</send-request>";

    private static final String FAILURES_BASIC = "<?xml version='1.0' encoding='UTF-8'?>" + "<failures xmlns='http://www.volantis.com/xmlns/2006/11/mps/ws'>" + RECIPIENT_ALICE_WITH_TYPE + RECIPIENT_BOB_WITH_TYPE + RECIPIENT_DAVE_WITH_TYPE + "</failures>";

    private static final String UTF_8 = "UTF-8";

    private ModelParser parser;

    protected void setUp() throws Exception {
        super.setUp();
        parser = new JiBXModelParser();
    }

    public void testSendRequestBasicDeserialization() throws Exception {
        InputStream in = new ByteArrayInputStream(SEND_REQUEST_BASIC.getBytes(UTF_8));
        SendRequest request = parser.readSendRequest(in);
        assertEquals(1, request.getRecipients().length);
        Recipient recipient = request.getRecipients()[0];
        assertEquals("alice@volantis.com", recipient.getAddress().getValue());
        assertEquals("smtp", recipient.getChannel());
        assertEquals("Nokia-6600", recipient.getDeviceName());
        assertEquals("Default type has been assigned", RecipientType.TO, recipient.getRecipientType());
        assertEquals("Hello", request.getMessage().getSubject());
        assertEquals(new URL("http://localhost:8080/volantis/welcome/welcome.xdime"), request.getMessage().getURL());
        assertNotNull("default sender", request.getSender());
        assertNull(request.getSender().getSMTPAddress());
        assertNull(request.getSender().getMSISDN());
    }

    public void testSendRequestBasicSerialization() throws Exception {
        MessageFactory factory = MessageFactory.getDefaultInstance();
        Address address = factory.createSMTPAddress("alice@volantis.com");
        Recipient alice = factory.createRecipient(address, "Nokia-6600");
        alice.setChannel("smtp");
        SendRequest request = factory.createSendRequest();
        request.addRecipient(alice);
        Message message = factory.createMessage(new URL("http://localhost:8080/volantis/welcome/welcome.xdime"));
        message.setSubject("Hello");
        request.setMessage(message);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        parser.write(request, out);
        assertXMLEqual(SEND_REQUEST_BASIC_WITH_TYPE, out.toString(UTF_8));
    }

    public void testSendRequestBasicWithEmbeddedContentDeserilization() throws Exception {
        InputStream in = new ByteArrayInputStream(SEND_REQUEST_BASIC_EMBEDDED_CONTENT.getBytes(UTF_8));
        SendRequest request = parser.readSendRequest(in);
        assertEquals(1, request.getRecipients().length);
        Recipient recipient = request.getRecipients()[0];
        assertEquals("alice@volantis.com", recipient.getAddress().getValue());
        assertEquals("smtp", recipient.getChannel());
        assertEquals("Nokia-6600", recipient.getDeviceName());
        assertEquals("Default type has been assigned", RecipientType.TO, recipient.getRecipientType());
        assertEquals(MESSAGE, request.getMessage().getContent());
        assertNotNull("default sender", request.getSender());
        assertNull(request.getSender().getSMTPAddress());
        assertNull(request.getSender().getMSISDN());
    }

    public void testSendRequestBasicSerializationUsingStringBasedContent() throws Exception {
        MessageFactory factory = MessageFactory.getDefaultInstance();
        Address address = factory.createSMTPAddress("alice@volantis.com");
        Recipient alice = factory.createRecipient(address, "Nokia-6600");
        alice.setChannel("smtp");
        SendRequest request = factory.createSendRequest();
        request.addRecipient(alice);
        Message message = factory.createMessage(MESSAGE);
        request.setMessage(message);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        parser.write(request, out);
        assertXMLEqual(SEND_REQUEST_BASIC_EMBEDDED_CONTENT_WITH_TYPE, out.toString(UTF_8));
    }

    public void testMultipleRecipientsSendRequest() throws Exception {
        InputStream in = new ByteArrayInputStream(SEND_REQUEST_MULTIPLE_RECIPIENT.getBytes(UTF_8));
        SendRequest request = parser.readSendRequest(in);
        final Recipient[] recipients = request.getRecipients();
        assertEquals(4, recipients.length);
        Recipient alice = recipients[0];
        assertEquals("alice@volantis.com", alice.getAddress().getValue());
        assertEquals("smtp", alice.getChannel());
        assertEquals("Nokia-6600", alice.getDeviceName());
        assertEquals("Default type has been assigned", RecipientType.TO, alice.getRecipientType());
        Recipient bob = recipients[1];
        assertEquals("bob@volantis.com", bob.getAddress().getValue());
        assertEquals("smtp", bob.getChannel());
        assertEquals("Nokia-6800", bob.getDeviceName());
        assertEquals("Default type has been assigned", RecipientType.TO, bob.getRecipientType());
        Recipient carol = recipients[2];
        assertEquals("carol@volantis.com", carol.getAddress().getValue());
        assertEquals("smtp", carol.getChannel());
        assertEquals("SonyEriccson-973i", carol.getDeviceName());
        assertEquals("Default type has been assigned", RecipientType.TO, carol.getRecipientType());
        Recipient dave = recipients[3];
        assertEquals("dave@volantis.com", dave.getAddress().getValue());
        assertEquals("smtp", dave.getChannel());
        assertEquals("Samsung-D700", dave.getDeviceName());
        assertEquals("Default type has been assigned", RecipientType.TO, dave.getRecipientType());
        assertEquals("Goodbye", request.getMessage().getSubject());
        assertEquals(new URL("http://some.host.com:6000/volantis/welcome/welcome.xdime"), request.getMessage().getURL());
        assertNotNull("Default sender", request.getSender());
        assertNull(request.getSender().getMSISDN());
        assertNull(request.getSender().getSMTPAddress());
    }

    public void testSenderIsDeserializedCorrectly() throws Exception {
        InputStream in = new ByteArrayInputStream(SEND_REQUEST_BASIC_WITH_SENDER.getBytes(UTF_8));
        SendRequest request = parser.readSendRequest(in);
        final Recipient[] recipients = request.getRecipients();
        assertEquals(1, recipients.length);
        Recipient alice = recipients[0];
        assertEquals("alice@volantis.com", alice.getAddress().getValue());
        assertEquals("smtp", alice.getChannel());
        assertEquals("Nokia-6600", alice.getDeviceName());
        assertEquals("Default type has been assigned", RecipientType.TO, alice.getRecipientType());
        assertEquals("Hello", request.getMessage().getSubject());
        assertEquals(new URL("http://localhost:8080/volantis/welcome/welcome.xdime"), request.getMessage().getURL());
        assertEquals("eve@volantis.com", request.getSender().getSMTPAddress().getValue());
    }

    public void testRoundingTrippingOfSendRequest() throws Exception {
        InputStream in = new ByteArrayInputStream(SEND_REQUEST_MULTIPLE_RECIPIENT_WITH_SENDER.getBytes(UTF_8));
        SendRequest request = parser.readSendRequest(in);
        final Recipient[] recipients = request.getRecipients();
        assertEquals(4, recipients.length);
        Recipient alice = recipients[0];
        assertEquals("alice@volantis.com", alice.getAddress().getValue());
        assertEquals("smtp", alice.getChannel());
        assertEquals("Nokia-6600", alice.getDeviceName());
        assertEquals("Default type has been assigned", RecipientType.TO, alice.getRecipientType());
        Recipient bob = recipients[1];
        assertEquals("bob@volantis.com", bob.getAddress().getValue());
        assertEquals("smtp", bob.getChannel());
        assertEquals("Nokia-6800", bob.getDeviceName());
        assertEquals("Default type has been assigned", RecipientType.TO, bob.getRecipientType());
        Recipient carol = recipients[2];
        assertEquals("carol@volantis.com", carol.getAddress().getValue());
        assertEquals("smtp", carol.getChannel());
        assertEquals("SonyEriccson-973i", carol.getDeviceName());
        assertEquals("Default type has been assigned", RecipientType.TO, carol.getRecipientType());
        Recipient dave = recipients[3];
        assertEquals("dave@volantis.com", dave.getAddress().getValue());
        assertEquals("smtp", dave.getChannel());
        assertEquals("Samsung-D700", dave.getDeviceName());
        assertEquals("Default type has been assigned", RecipientType.TO, dave.getRecipientType());
        assertEquals("Goodbye", request.getMessage().getSubject());
        assertEquals(new URL("http://some.host.com:6000/volantis/welcome/welcome.xdime"), request.getMessage().getURL());
        assertEquals("eve@volantis.com", request.getSender().getSMTPAddress().getValue());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        parser.write(request, out);
        assertXMLEqual(SEND_REQUEST_MULTIPLE_RECIPIENT_WITH_SENDER_AND_TYPES, out.toString(UTF_8));
    }

    public void testBasicFailuresDeserialization() throws Exception {
        InputStream in = new ByteArrayInputStream(FAILURES_BASIC.getBytes(UTF_8));
        Failures failures = parser.readFailures(in);
        assertEquals(3, failures.getRecipients().length);
        final Recipient[] recipients = failures.getRecipients();
        Recipient alice = recipients[0];
        assertEquals("alice@volantis.com", alice.getAddress().getValue());
        assertEquals("smtp", alice.getChannel());
        assertEquals("Nokia-6600", alice.getDeviceName());
        assertEquals("Default type has been assigned", RecipientType.TO, alice.getRecipientType());
        Recipient bob = recipients[1];
        assertEquals("bob@volantis.com", bob.getAddress().getValue());
        assertEquals("smtp", bob.getChannel());
        assertEquals("Nokia-6800", bob.getDeviceName());
        assertEquals("Default type has been assigned", RecipientType.TO, bob.getRecipientType());
        Recipient dave = recipients[2];
        assertEquals("dave@volantis.com", dave.getAddress().getValue());
        assertEquals("smtp", dave.getChannel());
        assertEquals("Samsung-D700", dave.getDeviceName());
        assertEquals("Default type has been assigned", RecipientType.TO, dave.getRecipientType());
    }

    public void testBasicFailuresSerialization() throws Exception {
        MessageFactory factory = MessageFactory.getDefaultInstance();
        Address address = factory.createSMTPAddress("alice@volantis.com");
        Recipient recipient = factory.createRecipient(address, "Nokia-6600");
        recipient.setChannel("smtp");
        Failures failures = factory.createFailures();
        failures.add(recipient);
        address = factory.createSMTPAddress("bob@volantis.com");
        recipient = factory.createRecipient(address, "Nokia-6800");
        recipient.setChannel("smtp");
        failures.add(recipient);
        address = factory.createSMTPAddress("dave@volantis.com");
        recipient = factory.createRecipient(address, "Samsung-D700");
        recipient.setChannel("smtp");
        failures.add(recipient);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        parser.write(failures, out);
        assertXMLEqual(FAILURES_BASIC, out.toString(UTF_8));
    }

    public void testRoundTrippingOfFailures() throws Exception {
        InputStream in = new ByteArrayInputStream(FAILURES_BASIC.getBytes(UTF_8));
        Failures failures = parser.readFailures(in);
        assertEquals(3, failures.getRecipients().length);
        final Recipient[] recipients = failures.getRecipients();
        Recipient alice = recipients[0];
        assertEquals("alice@volantis.com", alice.getAddress().getValue());
        assertEquals("smtp", alice.getChannel());
        assertEquals("Nokia-6600", alice.getDeviceName());
        assertEquals("Default type has been assigned", RecipientType.TO, alice.getRecipientType());
        Recipient bob = recipients[1];
        assertEquals("bob@volantis.com", bob.getAddress().getValue());
        assertEquals("smtp", bob.getChannel());
        assertEquals("Nokia-6800", bob.getDeviceName());
        assertEquals("Default type has been assigned", RecipientType.TO, bob.getRecipientType());
        Recipient dave = recipients[2];
        assertEquals("dave@volantis.com", dave.getAddress().getValue());
        assertEquals("smtp", dave.getChannel());
        assertEquals("Samsung-D700", dave.getDeviceName());
        assertEquals("Default type has been assigned", RecipientType.TO, dave.getRecipientType());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        parser.write(failures, out);
        assertXMLEqual(FAILURES_BASIC, out.toString(UTF_8));
    }
}

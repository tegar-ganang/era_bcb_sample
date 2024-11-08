package com.android.email.mail.transport;

import com.android.email.mail.Address;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Transport;
import com.android.email.provider.EmailProvider;
import com.android.email.provider.EmailContent.Attachment;
import com.android.email.provider.EmailContent.Body;
import com.android.email.provider.EmailContent.Message;
import org.apache.commons.io.IOUtils;
import android.content.Context;
import android.test.ProviderTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This is a series of unit tests for the SMTP Sender class.  These tests must be locally
 * complete - no server(s) required.
 *
 * These tests can be run with the following command:
 *   runtest -c com.android.email.mail.transport.SmtpSenderUnitTests email
 */
@SmallTest
public class SmtpSenderUnitTests extends ProviderTestCase2<EmailProvider> {

    EmailProvider mProvider;

    Context mProviderContext;

    Context mContext;

    private SmtpSender mSender = null;

    private static final String TEST_STRING = "Hello, world";

    private static final String TEST_STRING_BASE64 = "SGVsbG8sIHdvcmxk";

    public SmtpSenderUnitTests() {
        super(EmailProvider.class, EmailProvider.EMAIL_AUTHORITY);
    }

    /**
     * Setup code.  We generate a lightweight SmtpSender for testing.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mProviderContext = getMockContext();
        mContext = getContext();
        mSender = (SmtpSender) SmtpSender.newInstance(mProviderContext, "smtp://user:password@server:999");
    }

    /**
     * Confirms simple non-SSL non-TLS login
     */
    public void testSimpleLogin() throws MessagingException {
        MockTransport mockTransport = openAndInjectMockTransport();
        setupOpen(mockTransport, null);
        mSender.open();
    }

    /**
     * Confirms TLS login
     */
    public void testTlsLogin() throws MessagingException {
        MockTransport mockDash = new MockTransport();
        mockDash.setSecurity(Transport.CONNECTION_SECURITY_TLS, false);
        mockDash.setTlsAllowed(true);
        mSender.setTransport(mockDash);
        mockDash.expect(null, "220 MockTransport 2000 Ready To Assist You Peewee");
        mockDash.expect("EHLO .*", "250-10.20.30.40 hello");
        mockDash.expect(null, "250-STARTTLS");
        mockDash.expect(null, "250 AUTH LOGIN PLAIN CRAM-MD5");
        mockDash.expect("STARTTLS", "220 Ready to start TLS");
        mockDash.expect("EHLO .*", "250-10.20.30.40 hello");
        mockDash.expect(null, "250-STARTTLS");
        mockDash.expect(null, "250 AUTH LOGIN PLAIN CRAM-MD5");
        mockDash.expect("AUTH PLAIN .*", "235 2.7.0 ... authentication succeeded");
        mSender.open();
        assertTrue("dash", mockDash.getTlsReopened());
        MockTransport mockSpace = new MockTransport();
        mockSpace.setSecurity(Transport.CONNECTION_SECURITY_TLS, false);
        mockSpace.setTlsAllowed(true);
        mSender.setTransport(mockSpace);
        mockSpace.expect(null, "220 MockTransport 2000 Ready To Assist You Peewee");
        mockSpace.expect("EHLO .*", "250-10.20.30.40 hello");
        mockSpace.expect(null, "250-AUTH LOGIN PLAIN CRAM-MD5");
        mockSpace.expect(null, "250 STARTTLS");
        mockSpace.expect("STARTTLS", "220 Ready to start TLS");
        mockSpace.expect("EHLO .*", "250-10.20.30.40 hello");
        mockSpace.expect(null, "250-AUTH LOGIN PLAIN CRAM-MD5");
        mockSpace.expect(null, "250 STARTTLS");
        mockSpace.expect("AUTH PLAIN .*", "235 2.7.0 ... authentication succeeded");
        mSender.open();
        assertTrue("space", mockSpace.getTlsReopened());
    }

    /**
     * Confirms TLS required but not supported
     */
    public void testTlsRequiredNotSupported() throws MessagingException {
        MockTransport mockTransport = new MockTransport();
        mockTransport.setSecurity(Transport.CONNECTION_SECURITY_TLS, false);
        mSender.setTransport(mockTransport);
        mockTransport.expect(null, "220 MockTransport 2000 Ready To Assist You Peewee");
        mockTransport.expect("EHLO .*", "");
        setupOpen(mockTransport, "");
        try {
            mSender.open();
            fail("Should not be able to open() without TLS.");
        } catch (MessagingException me) {
        }
    }

    /**
     * Test:  Open and send a single message (sunny day)
     */
    public void testSendMessageWithBody() throws MessagingException {
        MockTransport mockTransport = openAndInjectMockTransport();
        mockTransport.expectClose();
        setupOpen(mockTransport, null);
        Message message = setupSimpleMessage();
        message.save(mProviderContext);
        Body body = new Body();
        body.mMessageKey = message.mId;
        body.mTextContent = TEST_STRING;
        body.save(mProviderContext);
        expectSimpleMessage(mockTransport);
        mockTransport.expect("Content-Type: text/plain; charset=utf-8");
        mockTransport.expect("Content-Transfer-Encoding: base64");
        mockTransport.expect("");
        mockTransport.expect(TEST_STRING_BASE64);
        mockTransport.expect("\r\n\\.", "250 2.0.0 kv2f1a00C02Rf8w3Vv mail accepted for delivery");
        mSender.sendMessage(message.mId);
    }

    /**
     * Test:  Open and send a single message with an empty attachment (no file) (sunny day)
     */
    public void testSendMessageWithEmptyAttachment() throws MessagingException, IOException {
        MockTransport mockTransport = openAndInjectMockTransport();
        mockTransport.expectClose();
        setupOpen(mockTransport, null);
        Message message = setupSimpleMessage();
        message.save(mProviderContext);
        Attachment attachment = setupSimpleAttachment(mProviderContext, message.mId, false);
        attachment.save(mProviderContext);
        expectSimpleMessage(mockTransport);
        mockTransport.expect("MIME-Version: 1.0");
        mockTransport.expect("Content-Type: multipart/mixed; boundary=\".*");
        mockTransport.expect("");
        mockTransport.expect("----.*");
        expectSimpleAttachment(mockTransport, attachment);
        mockTransport.expect("");
        mockTransport.expect("----.*--");
        mockTransport.expect("\r\n\\.", "250 2.0.0 kv2f1a00C02Rf8w3Vv mail accepted for delivery");
        mSender.sendMessage(message.mId);
    }

    /**
     * Test:  Open and send a single message with an attachment (sunny day)
     */
    public void testSendMessageWithAttachment() throws MessagingException, IOException {
        MockTransport mockTransport = openAndInjectMockTransport();
        mockTransport.expectClose();
        setupOpen(mockTransport, null);
        Message message = setupSimpleMessage();
        message.save(mProviderContext);
        Attachment attachment = setupSimpleAttachment(mProviderContext, message.mId, true);
        attachment.save(mProviderContext);
        expectSimpleMessage(mockTransport);
        mockTransport.expect("MIME-Version: 1.0");
        mockTransport.expect("Content-Type: multipart/mixed; boundary=\".*");
        mockTransport.expect("");
        mockTransport.expect("----.*");
        expectSimpleAttachment(mockTransport, attachment);
        mockTransport.expect("");
        mockTransport.expect("----.*--");
        mockTransport.expect("\r\n\\.", "250 2.0.0 kv2f1a00C02Rf8w3Vv mail accepted for delivery");
        mSender.sendMessage(message.mId);
    }

    /**
     * Test:  Open and send a single message with two attachments
     */
    public void testSendMessageWithTwoAttachments() throws MessagingException, IOException {
        MockTransport mockTransport = openAndInjectMockTransport();
        mockTransport.expectClose();
        setupOpen(mockTransport, null);
        Message message = setupSimpleMessage();
        message.save(mProviderContext);
        Attachment attachment = setupSimpleAttachment(mProviderContext, message.mId, true);
        attachment.save(mProviderContext);
        Attachment attachment2 = setupSimpleAttachment(mProviderContext, message.mId, true);
        attachment2.save(mProviderContext);
        expectSimpleMessage(mockTransport);
        mockTransport.expect("MIME-Version: 1.0");
        mockTransport.expect("Content-Type: multipart/mixed; boundary=\".*");
        mockTransport.expect("");
        mockTransport.expect("----.*");
        expectSimpleAttachment(mockTransport, attachment);
        mockTransport.expect("");
        mockTransport.expect("----.*");
        expectSimpleAttachment(mockTransport, attachment2);
        mockTransport.expect("");
        mockTransport.expect("----.*--");
        mockTransport.expect("\r\n\\.", "250 2.0.0 kv2f1a00C02Rf8w3Vv mail accepted for delivery");
        mSender.sendMessage(message.mId);
    }

    /**
     * Test:  Open and send a single message with body & attachment (sunny day)
     */
    public void testSendMessageWithBodyAndAttachment() throws MessagingException, IOException {
        MockTransport mockTransport = openAndInjectMockTransport();
        mockTransport.expectClose();
        setupOpen(mockTransport, null);
        Message message = setupSimpleMessage();
        message.save(mProviderContext);
        Body body = new Body();
        body.mMessageKey = message.mId;
        body.mTextContent = TEST_STRING;
        body.save(mProviderContext);
        Attachment attachment = setupSimpleAttachment(mProviderContext, message.mId, true);
        attachment.save(mProviderContext);
        expectSimpleMessage(mockTransport);
        mockTransport.expect("MIME-Version: 1.0");
        mockTransport.expect("Content-Type: multipart/mixed; boundary=\".*");
        mockTransport.expect("");
        mockTransport.expect("----.*");
        mockTransport.expect("Content-Type: text/plain; charset=utf-8");
        mockTransport.expect("Content-Transfer-Encoding: base64");
        mockTransport.expect("");
        mockTransport.expect(TEST_STRING_BASE64);
        mockTransport.expect("----.*");
        expectSimpleAttachment(mockTransport, attachment);
        mockTransport.expect("");
        mockTransport.expect("----.*--");
        mockTransport.expect("\r\n\\.", "250 2.0.0 kv2f1a00C02Rf8w3Vv mail accepted for delivery");
        mSender.sendMessage(message.mId);
    }

    /**
     * Prepare to send a simple message (see setReceiveSimpleMessage)
     */
    private Message setupSimpleMessage() {
        Message message = new Message();
        message.mTimeStamp = System.currentTimeMillis();
        message.mFrom = Address.parseAndPack("Jones@Registry.Org");
        message.mTo = Address.parseAndPack("Smith@Registry.Org");
        message.mMessageId = "1234567890";
        return message;
    }

    /**
     * Prepare to receive a simple message (see setupSimpleMessage)
     */
    private void expectSimpleMessage(MockTransport mockTransport) {
        mockTransport.expect("MAIL FROM: <Jones@Registry.Org>", "250 2.1.0 <Jones@Registry.Org> sender ok");
        mockTransport.expect("RCPT TO: <Smith@Registry.Org>", "250 2.1.5 <Smith@Registry.Org> recipient ok");
        mockTransport.expect("DATA", "354 enter mail, end with . on a line by itself");
        mockTransport.expect("Date: .*");
        mockTransport.expect("Message-ID: .*");
        mockTransport.expect("From: Jones@Registry.Org");
        mockTransport.expect("To: Smith@Registry.Org");
    }

    /**
     * Prepare to send a simple attachment
     */
    private Attachment setupSimpleAttachment(Context context, long messageId, boolean withBody) throws IOException {
        Attachment attachment = new Attachment();
        attachment.mFileName = "the file.jpg";
        attachment.mMimeType = "image/jpg";
        attachment.mSize = 0;
        attachment.mContentId = null;
        attachment.mContentUri = "content://com.android.email/1/1";
        attachment.mMessageKey = messageId;
        attachment.mLocation = null;
        attachment.mEncoding = null;
        if (withBody) {
            InputStream inStream = new ByteArrayInputStream(TEST_STRING.getBytes());
            File cacheDir = context.getCacheDir();
            File tmpFile = File.createTempFile("setupSimpleAttachment", "tmp", cacheDir);
            OutputStream outStream = new FileOutputStream(tmpFile);
            IOUtils.copy(inStream, outStream);
            attachment.mContentUri = "file://" + tmpFile.getAbsolutePath();
        }
        return attachment;
    }

    /**
     * Prepare to receive a simple attachment (note, no multipart support here)
     */
    private void expectSimpleAttachment(MockTransport mockTransport, Attachment attachment) {
        mockTransport.expect("Content-Type: " + attachment.mMimeType + ";");
        mockTransport.expect(" name=\"" + attachment.mFileName + "\"");
        mockTransport.expect("Content-Transfer-Encoding: base64");
        mockTransport.expect("Content-Disposition: attachment;");
        mockTransport.expect(" filename=\"" + attachment.mFileName + "\";");
        mockTransport.expect(" size=" + Long.toString(attachment.mSize));
        mockTransport.expect("");
        if (attachment.mContentUri != null && attachment.mContentUri.startsWith("file://")) {
            mockTransport.expect(TEST_STRING_BASE64);
        }
    }

    /**
     * Test:  Recover from a server closing early (or returning an empty string)
     */
    public void testEmptyLineResponse() {
        MockTransport mockTransport = openAndInjectMockTransport();
        mockTransport.expectClose();
        mockTransport.expect(null, "220 MockTransport 2000 Ready To Assist You Peewee");
        mockTransport.expect("EHLO .*", "");
        try {
            mSender.sendMessage(-1);
            fail("Should not be able to send with failed open()");
        } catch (MessagingException me) {
        }
    }

    /**
     * Set up a basic MockTransport. open it, and inject it into mStore
     */
    private MockTransport openAndInjectMockTransport() {
        MockTransport mockTransport = new MockTransport();
        mockTransport.setSecurity(Transport.CONNECTION_SECURITY_NONE, false);
        mSender.setTransport(mockTransport);
        return mockTransport;
    }

    /**
     * Helper which stuffs the mock with enough strings to satisfy a call to SmtpSender.open()
     * 
     * @param mockTransport the mock transport we're using
     * @param capabilities if non-null, comma-separated list of capabilities
     */
    private void setupOpen(MockTransport mockTransport, String capabilities) {
        mockTransport.expect(null, "220 MockTransport 2000 Ready To Assist You Peewee");
        mockTransport.expect("EHLO .*", "250-10.20.30.40 hello");
        if (capabilities == null) {
            mockTransport.expect(null, "250-HELP");
            mockTransport.expect(null, "250-AUTH LOGIN PLAIN CRAM-MD5");
            mockTransport.expect(null, "250-SIZE 15728640");
            mockTransport.expect(null, "250-ENHANCEDSTATUSCODES");
            mockTransport.expect(null, "250-8BITMIME");
        } else {
            for (String capability : capabilities.split(",")) {
                mockTransport.expect(null, "250-" + capability);
            }
        }
        mockTransport.expect(null, "250+OK");
        mockTransport.expect("AUTH PLAIN .*", "235 2.7.0 ... authentication succeeded");
    }
}

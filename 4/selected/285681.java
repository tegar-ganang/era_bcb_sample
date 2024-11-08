package net.sf.gm.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.Iterator;
import java.util.UUID;
import junit.framework.TestCase;
import net.sf.gm.ContactType;
import net.sf.gm.IClient;
import net.sf.gm.beans.GMAttachment;
import net.sf.gm.beans.GMComposedMessage;
import net.sf.gm.beans.GMContact;
import net.sf.gm.beans.GMFilter;
import net.sf.gm.beans.GMFilterOptions;
import net.sf.gm.beans.GMInfo;
import net.sf.gm.beans.GMMessage;
import net.sf.gm.beans.GMSearchOptions;
import net.sf.gm.beans.GMSearchResponse;
import net.sf.gm.beans.GMSendResponse;
import net.sf.gm.beans.GMThread;
import net.sf.gm.beans.GMThreadSnapshot;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Make sure you pass in -Duser=YOUR_EMAIL -Dpass=YOUR_PASS to the Java VM
 */
public class TestClient extends TestCase {

    protected static final Log log = LogFactory.getLog(TestClient.class);

    protected GMLoginInfo loginInfo = new GMLoginInfo();

    protected IClient client;

    public TestClient(String arg0) {
        super(arg0);
        String email = System.getProperty("user");
        String password = System.getProperty("pass");
        if (email == null || password == null) throw new InvalidParameterException("you must set the environment variables for 'user' and 'pass'.");
        loginInfo.setUsername(email);
        loginInfo.setPassword(password);
    }

    protected void setUp() throws Exception {
        super.setUp();
        client = new GMClient(loginInfo);
        boolean status = client.connect();
        assert (status);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        client.disconnect();
    }

    public void xtestConnect() {
    }

    public void xtestGetContacts() throws Exception {
        Iterable<GMContact> contacts = client.getContacts(ContactType.CONTACT_ALL);
        int count = 0;
        for (Iterator iter = contacts.iterator(); iter.hasNext(); ) {
            GMContact contact = (GMContact) iter.next();
            log.info("Contact[" + count++ + "] = " + contact);
        }
    }

    public void xtestSearchContacts() throws Exception {
        Iterable<GMContact> contacts = client.getContacts("tom");
        int count = 0;
        for (Iterator iter = contacts.iterator(); iter.hasNext(); ) {
            GMContact contact = (GMContact) iter.next();
            log.info("Contact[" + count++ + "] = " + contact);
        }
    }

    public void xtestSearchMail() throws Exception {
        GMSearchOptions options = new GMSearchOptions();
        options.setFrom(loginInfo.getUsername() + "*");
        GMSearchResponse response = client.getMail(options);
        assertNotNull(response);
        int count = 0;
        for (Iterator iter = response.getThreadSnapshots().iterator(); iter.hasNext(); ) {
            GMThreadSnapshot thread = (GMThreadSnapshot) iter.next();
            log.info("THREAD[" + count++ + "]: " + thread);
        }
        assertEquals(response.getTotalThreads(), count);
        response = client.getMail(null);
        assertNotNull(response);
        count = 0;
        for (Iterator iter = response.getThreadSnapshots().iterator(); iter.hasNext(); ) {
            GMThreadSnapshot thread = (GMThreadSnapshot) iter.next();
            log.info("THREAD[" + count++ + "]: " + thread);
        }
        assertEquals(response.getTotalThreads(), count);
    }

    public void xtestFreeFormSearch() throws Exception {
        GMSearchResponse response = client.getMail("from:" + loginInfo.getUsername() + "*", 0);
        assertNotNull(response);
        for (Iterator iter = response.getThreadSnapshots().iterator(); iter.hasNext(); ) {
            GMThreadSnapshot thread = (GMThreadSnapshot) iter.next();
            log.info("THREAD: " + thread);
        }
    }

    public void xtestSendMail() throws Exception {
        GMComposedMessage message = new GMComposedMessage();
        String email = loginInfo.getUsername();
        if (email.indexOf('@') == -1) email = email.trim() + "@gmail.com";
        message.addTo(email);
        message.setSubject("test message to myself!");
        message.setMessageBody("<b>And this is the bolded test message.</b> <p/><br/><i>This the italic part</i> <p/><span>Regular text</span>");
        message.setHtml(true);
        GMSendResponse response = client.send(message);
        assertNotNull(response);
        assertTrue(client.applyLabel("myNewLabel", response.getMessageID()));
        assertTrue(client.removeLabel("myNewLabel", response.getMessageID()));
    }

    public void xtestSendAttachment() throws Exception {
        File tempFile = File.createTempFile("temp", ".txt", new File(System.getProperty("user.home")));
        FileOutputStream fout = new FileOutputStream(tempFile);
        for (int i = 0; i < 1000; ++i) fout.write(("This is line number " + i + "\n").getBytes());
        fout.close();
        GMComposedMessage message = new GMComposedMessage();
        String email = loginInfo.getUsername();
        if (email.indexOf('@') == -1) email = email.trim() + "@gmail.com";
        message.addTo(email);
        message.setSubject("test message with text attachment");
        message.setMessageBody("<b>This message has a text file attachment</b>");
        message.setHtml(true);
        message.addAttachment(tempFile);
        GMSendResponse response = client.send(message);
        tempFile.delete();
        assertNotNull(response);
    }

    public void xtestSendExistingAttachment() throws Exception {
        GMSearchOptions options = new GMSearchOptions();
        options.setFrom(loginInfo.getUsername() + "*");
        GMSearchResponse mail = client.getMail(options);
        for (Iterator it = mail.getThreadSnapshots().iterator(); it.hasNext(); ) {
            GMThreadSnapshot threadSnapshot = (GMThreadSnapshot) it.next();
            GMThread thread = client.getThread(threadSnapshot.getThreadID());
            log.info("Most Recent Thread: " + thread);
            for (Iterator iter = thread.getMessages().iterator(); iter.hasNext(); ) {
                GMMessage message = (GMMessage) iter.next();
                log.info("Message: " + message);
                Iterable<GMAttachment> attachments = message.getAttachments();
                for (Iterator iterator = attachments.iterator(); iterator.hasNext(); ) {
                    GMAttachment attachment = (GMAttachment) iterator.next();
                    GMComposedMessage newMessage = new GMComposedMessage();
                    String email = loginInfo.getUsername();
                    if (email.indexOf('@') == -1) email = email.trim() + "@gmail.com";
                    newMessage.addTo(email);
                    newMessage.setSubject("test message with attachments from [" + message.getSubject() + "]");
                    newMessage.setMessageBody("<b>This message has a text file attachment</b>");
                    newMessage.setHtml(true);
                    newMessage.addAttachmentLink(thread.getThreadID(), message.getMessageID(), attachment.getId());
                    GMSendResponse response = client.send(newMessage);
                    log.info("GMResponse: " + response);
                    assertNotNull(response);
                    return;
                }
            }
        }
    }

    public void xtestAddRemoveLabel() throws Exception {
        String newLabel = UUID.randomUUID().toString();
        assertTrue(client.createLabel(newLabel));
        GMInfo info = client.queryInfo();
        assertTrue(info.getLabelMap().containsKey(newLabel));
        assertTrue(client.removeLabel(newLabel));
        info = client.queryInfo();
        assertFalse(info.getLabelMap().containsKey(newLabel));
    }

    public void xtestGetFilters() throws Exception {
        Iterable<GMFilter> filters = client.getFilters();
        for (Iterator iter = filters.iterator(); iter.hasNext(); ) {
            GMFilter filter = (GMFilter) iter.next();
            log.info(filter);
        }
    }

    public void xtestGetThread() throws Exception {
        GMSearchOptions options = new GMSearchOptions();
        options.setFrom(loginInfo.getUsername() + "*");
        options.setSubject("message*");
        GMSearchResponse mail = client.getMail(options);
        for (Iterator it = mail.getThreadSnapshots().iterator(); it.hasNext(); ) {
            GMThreadSnapshot threadSnapshot = (GMThreadSnapshot) it.next();
            GMThread thread = client.getThread(threadSnapshot.getThreadID());
            log.info("Most Recent Thread: " + thread);
            for (Iterator iter = thread.getMessages().iterator(); iter.hasNext(); ) {
                GMMessage message = (GMMessage) iter.next();
                log.info("Message: " + message);
                Iterable<GMAttachment> attachments = message.getAttachments();
                for (Iterator iterator = attachments.iterator(); iterator.hasNext(); ) {
                    GMAttachment attachment = (GMAttachment) iterator.next();
                    String ext = FilenameUtils.getExtension(attachment.getFilename());
                    if (ext.trim().length() > 0) ext = "." + ext;
                    String base = FilenameUtils.getBaseName(attachment.getFilename());
                    File file = File.createTempFile(base, ext, new File(System.getProperty("user.home")));
                    log.info("Saving attachment: " + file.getPath());
                    InputStream attStream = client.getAttachmentAsStream(attachment.getId(), message.getMessageID());
                    IOUtils.copy(attStream, new FileOutputStream(file));
                    attStream.close();
                    assertEquals(file.length(), attachment.getSize());
                    log.info("Done. Successfully saved: " + file.getPath());
                    file.delete();
                }
            }
        }
    }

    public void testAddRemoveFilter() throws Exception {
        GMFilterOptions options = new GMFilterOptions();
        options.setStarIt(true);
        options.setFrom(loginInfo.getUsername() + "*");
        GMFilter filter = client.createFilter(options);
        assertNotNull(filter);
        log.info("New Filter: " + filter);
        assertTrue(client.removeFilter(filter.getFilterID()));
    }
}

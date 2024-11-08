package ch.unibas.jmeter.snmp.sampler.mailroundtrip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.apache.jmeter.protocol.mail.sampler.MailReaderSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import ch.unibas.debug.Debug;

public class MailRoundtripSampler extends MailReaderSampler {

    private static final String MAILBOX_ERROR = "500";

    private static final String SMTP_ERROR = "400";

    private static final long serialVersionUID = 7403235502163418843L;

    private static final Logger log = LoggingManager.getLoggerForClass();

    private static final String NEW_LINE = "\n";

    private static final String WAIT_TIME = "waitTime";

    private static final String SLEEP_TIME = "sleepTime";

    private static final String USE_TLS = "useTLS";

    private static final String SMTP_SERVER = "smtpServer";

    private static final String FROM_ADRESS = "fromAdress";

    private static final String TO_ADRESS = "toAdress";

    private static final String SMTP_USERNAME = "smtpUsername";

    private static final String SNMTP_PASSWORD = "smtpPassword";

    private static final String USE_ATHENTIFICATION = "useAuthentification";

    private static final String MAX_MESSAGE_COUNT = "maxMessageCount";

    private static final String CHECK_SUBSTRING = "checkSubstring";

    public SampleResult sample(Entry e) {
        SampleResult result = new SampleResult();
        boolean isOK = false;
        result.setSampleLabel(getName());
        result.setSamplerData(getServerType() + "://" + getUserName() + "@" + getServer());
        String date = new Date().toString();
        String subject = "snmpJMeter Roundtrip message " + date;
        String body = "";
        long sleepTime = getSleepTime();
        result.sampleStart();
        try {
            sendMail(subject, body);
        } catch (MessagingException ex) {
            log.warn("SMTP Failure", ex);
            result.setResponseCode(SMTP_ERROR);
            String errMsg = "SMTP Failure: " + ex.toString();
            result.setResponseMessage(errMsg);
            Debug.debug(errMsg);
            result.setSuccessful(false);
            return result;
        }
        long endTime = System.currentTimeMillis() + getWaitTime();
        try {
            Debug.debug("MailRoundtripSampler.sample() opening " + getServerType() + "://" + getUserName() + "@" + getServer(), 3);
            if (Debug.isDebug(4)) {
                long now_tmp = System.currentTimeMillis();
                long delta_tmp = endTime - now_tmp;
                Debug.debug("Endtime " + endTime + " now " + now_tmp + " delta " + delta_tmp, 4);
            }
            for (int i = 0; !isOK && System.currentTimeMillis() < endTime; i++) {
                Thread.sleep(sleepTime);
                Debug.debug("Checking mail retry " + i, 2);
                if (Debug.isDebug(4)) {
                    long now_tmp = System.currentTimeMillis();
                    long delta_tmp = endTime - now_tmp;
                    Debug.debug("Endtime " + endTime + " now " + now_tmp + " delta " + delta_tmp, 4);
                }
                isOK = readMail(result, subject);
            }
            if (result.getEndTime() == 0) {
                result.sampleEnd();
            }
            if (isOK) {
                result.setResponseCodeOK();
                result.setResponseMessage("OK");
            } else {
                result.setResponseCode("Error");
                result.setResponseMessage("Could not find roundtrip mail");
            }
            deleteMessages(result);
        } catch (Exception ex) {
            log.warn("", ex);
            result.setResponseCode(MAILBOX_ERROR);
            result.setResponseMessage(ex.toString());
            Debug.debug(ex.toString());
        }
        result.setSuccessful(isOK);
        return result;
    }

    private void deleteMessages(SampleResult result) {
        Debug.debug("Check if excess messages should be deleted", 5);
        int maxMessageCount = getMaxMessageCount();
        if (maxMessageCount < 1) {
            return;
        }
        Folder folder = null;
        try {
            folder = openFolder(true);
            Debug.debug("MailRoundtripSampler.deleteMessages() folder " + folder, 3);
            int messageCount = folder.getMessageCount();
            int deleteCount = messageCount - maxMessageCount;
            Debug.debug("Num Messages: " + messageCount + " max allowed " + maxMessageCount + " going to delete " + deleteCount + " messages");
            if (deleteCount > 0) {
                for (int i = maxMessageCount; i < messageCount; i++) {
                    Debug.debug("Deleting message number " + i, 4);
                    folder.getMessage(i).setFlag(Flags.Flag.DELETED, true);
                }
                folder.expunge();
                String tmp = "Deleted " + deleteCount + "messages";
                result.setSamplerData(result.getSamplerData() + "\n\n" + tmp);
                Debug.debug(tmp);
            }
        } catch (MessagingException e) {
            log.debug("Error deleting exess messages", e);
        } finally {
            closeFolder(folder);
        }
    }

    private Folder openFolder(boolean readwrite) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        Debug.debug("got session", 4);
        Store store = session.getStore(getServerType());
        store.connect(getServer(), getUserName(), getPassword());
        Debug.debug("got store", 4);
        Folder folder = store.getFolder(getFolder());
        if (readwrite) {
            folder.open(Folder.READ_WRITE);
        } else {
            folder.open(Folder.READ_ONLY);
        }
        return folder;
    }

    private void closeFolder(Folder folder) {
        try {
            if (folder != null) {
                Store store = folder.getStore();
                Debug.debug("Closing mail folder");
                folder.close(true);
                if (store != null) {
                    Debug.debug("Closing mail store");
                    store.close();
                }
            }
        } catch (MessagingException e) {
            log.warn("Error closing the mail folder", e);
        }
    }

    private boolean readMail(SampleResult parent, String subject) throws NoSuchProviderException, MessagingException, IOException {
        boolean deleteMessages = getDeleteMessages();
        Folder folder = openFolder(deleteMessages);
        try {
            Debug.debug("opened folder", 4);
            Message messages[] = folder.getMessages();
            Message message;
            StringBuffer pdata = new StringBuffer();
            int numMsgs = messages.length;
            Debug.debug("Found " + numMsgs + " messages in folder");
            pdata.append(numMsgs);
            pdata.append(" messages found\n");
            for (int i = 0; i < numMsgs; i++) {
                StringBuffer cdata = new StringBuffer();
                SampleResult child = new SampleResult();
                child.sampleStart();
                message = messages[i];
                String cur_subj = message.getSubject();
                cur_subj = cur_subj == null ? "" : cur_subj;
                Debug.debug("looking for messagesubject " + subject, 6);
                Debug.debug("Found message with subject " + cur_subj, 6);
                boolean foundTestMail = subject.equals(cur_subj);
                if (!foundTestMail && isCheckForSubstring()) {
                    foundTestMail = cur_subj.indexOf(subject) > -1;
                }
                if (foundTestMail) {
                    Debug.debug("FOUND MESSAGE! ", 4);
                    {
                        child.setContentType(message.getContentType());
                    }
                    cdata.append("Message ");
                    cdata.append(message.getMessageNumber());
                    child.setSampleLabel(cdata.toString());
                    child.setSamplerData(cdata.toString());
                    cdata.setLength(0);
                    if (isStoreMimeMessage()) {
                        appendMessageAsMime(cdata, message);
                    } else {
                        appendMessageData(cdata, message);
                    }
                    if (deleteMessages) {
                        message.setFlag(Flags.Flag.DELETED, true);
                    }
                    child.setResponseData(cdata.toString().getBytes());
                    child.setDataType(SampleResult.TEXT);
                    child.setResponseCodeOK();
                    child.setResponseMessage("OK");
                    child.setSuccessful(true);
                    child.sampleEnd();
                    parent.addSubResult(child);
                    parent.setResponseData(pdata.toString().getBytes());
                    parent.setDataType(SampleResult.TEXT);
                    parent.setContentType("text/plain");
                    return true;
                }
            }
        } finally {
            closeFolder(folder);
        }
        return false;
    }

    public void sendMail(String subject, String body) throws AddressException, MessagingException {
        Properties props = System.getProperties();
        Authenticator auth = null;
        String smtpServer = getSmtpServer();
        Debug.debug("MailRoundtripSampler.sendMail() sending via " + smtpServer, 3);
        if (isUseAuthentification()) {
            if (isUseTLS()) {
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "true");
            } else {
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.fallback", "false");
            }
            SecurityManager security = System.getSecurityManager();
            props.put("mail.smtp.auth", "true");
            auth = new Authenticator(getSmtpUsername(), getSmtpPassword());
            Debug.debug("MailRoundtripSampler.sendMail() athenticating as " + getSmtpUsername(), 3);
        }
        props.put("mail.smtp.host", smtpServer);
        Debug.debug("MailRoundtripSampler.sendMail()  mail.smtp.host property " + props.getProperty("mail.smtp.host"), 3);
        Session session = Session.getInstance(props, auth);
        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(getFrom()));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(getTo(), false));
        msg.setSubject(subject);
        msg.setText(body);
        msg.setHeader("X-Mailer", "snmpJMeter");
        msg.setSentDate(new Date());
        Transport tr = session.getTransport("smtp");
        tr.connect(smtpServer, getSmtpUsername(), getSmtpPassword());
        msg.saveChanges();
        tr.sendMessage(msg, msg.getAllRecipients());
        tr.close();
        Debug.debug("Sent mail to " + getTo() + " subject " + subject, 2);
        System.out.println("Message sent OK.");
    }

    private static class Authenticator extends javax.mail.Authenticator {

        private PasswordAuthentication authentication;

        public Authenticator(String username, String password) {
            authentication = new PasswordAuthentication(username, password);
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return authentication;
        }
    }

    private void appendMessageAsMime(StringBuffer cdata, Message message) throws MessagingException, IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        message.writeTo(bout);
        cdata.append(bout);
    }

    private void appendMessageData(StringBuffer cdata, Message message) throws MessagingException, IOException {
        cdata.append("Date: ");
        cdata.append(message.getSentDate());
        cdata.append(NEW_LINE);
        cdata.append("To: ");
        Address[] recips = message.getAllRecipients();
        for (int j = 0; j < recips.length; j++) {
            cdata.append(recips[j].toString());
            if (j < recips.length - 1) {
                cdata.append("; ");
            }
        }
        cdata.append(NEW_LINE);
        cdata.append("From: ");
        Address[] from = message.getFrom();
        for (int j = 0; j < from.length; j++) {
            cdata.append(from[j].toString());
            if (j < from.length - 1) {
                cdata.append("; ");
            }
        }
        cdata.append(NEW_LINE);
        cdata.append("Subject: ");
        cdata.append(message.getSubject());
        cdata.append(NEW_LINE);
        cdata.append(NEW_LINE);
        Object content = message.getContent();
        if (content instanceof MimeMultipart) {
            MimeMultipart mmp = (MimeMultipart) content;
            int count = mmp.getCount();
            cdata.append("Multipart. Count: ");
            cdata.append(count);
            cdata.append(NEW_LINE);
            for (int j = 0; j < count; j++) {
                BodyPart bodyPart = mmp.getBodyPart(j);
                cdata.append("Type: ");
                cdata.append(bodyPart.getContentType());
                cdata.append(NEW_LINE);
                try {
                    cdata.append(bodyPart.getContent());
                } catch (UnsupportedEncodingException ex) {
                    cdata.append(ex.getLocalizedMessage());
                }
            }
        } else {
            cdata.append(content);
        }
    }

    public long getWaitTime() {
        return getPropertyAsLong(WAIT_TIME);
    }

    public void setWaitTime(String waitTime) {
        setProperty(WAIT_TIME, waitTime);
    }

    public long getSleepTime() {
        return getPropertyAsLong(SLEEP_TIME);
    }

    public void setSleepTime(String sleepTime) {
        setProperty(SLEEP_TIME, sleepTime);
    }

    public boolean isUseTLS() {
        return getPropertyAsBoolean(USE_TLS);
    }

    public void setUseTLS(boolean useTLS) {
        setProperty(USE_TLS, useTLS);
    }

    public String getSmtpServer() {
        return getPropertyAsString(SMTP_SERVER);
    }

    public void setSmtpServer(String smtpServer) {
        setProperty(SMTP_SERVER, smtpServer);
    }

    public String getFrom() {
        return getPropertyAsString(FROM_ADRESS);
    }

    public void setFrom(String from) {
        setProperty(FROM_ADRESS, from);
    }

    public String getTo() {
        return getPropertyAsString(TO_ADRESS);
    }

    public void setTo(String to) {
        setProperty(TO_ADRESS, to);
    }

    public String getSmtpUsername() {
        return getPropertyAsString(SMTP_USERNAME);
    }

    public void setSmtpUsername(String username) {
        setProperty(SMTP_USERNAME, username);
    }

    public String getSmtpPassword() {
        return getPropertyAsString(SNMTP_PASSWORD);
    }

    public void setSmtpPassword(String password) {
        setProperty(SNMTP_PASSWORD, password);
    }

    public boolean isUseAuthentification() {
        return getPropertyAsBoolean(USE_ATHENTIFICATION);
    }

    public void setUseAuthentification(boolean useAthentification) {
        setProperty(USE_ATHENTIFICATION, useAthentification);
    }

    public int getMaxMessageCount() {
        Debug.debug("MailRoundtripSampler.getMaxMessageCount() -> " + getProperty(MAX_MESSAGE_COUNT), 7);
        return getPropertyAsInt(MAX_MESSAGE_COUNT);
    }

    public void setMaxMessageCount(String msgCount) {
        Debug.debug("MailRoundtripSampler.setMaxMessageCount(" + msgCount + ")", 7);
        setProperty(MAX_MESSAGE_COUNT, msgCount);
    }

    public boolean isCheckForSubstring() {
        return getPropertyAsBoolean(CHECK_SUBSTRING);
    }

    public void setCheckForSubstring(boolean useAthentification) {
        setProperty(CHECK_SUBSTRING, useAthentification);
    }
}

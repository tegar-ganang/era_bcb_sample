package org.schalm.mailcheck;

import java.util.ArrayList;
import java.util.Properties;
import static javax.mail.Folder.READ_ONLY;
import static javax.mail.Folder.READ_WRITE;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import org.apache.log4j.Logger;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A mailbox like steve.jobs@hotmail.com.
 * 
 * @author <a href="mailto:cschalm@users.sourceforge.net">Carsten Schalm</a>
 * @version $Id: MailAccount.java 116 2011-10-31 22:12:13Z cschalm $
 */
public class MailAccount extends Observable {

    private static Logger log = Logger.getLogger(MailAccount.class);

    private static final String CONNECTION_TIMEOUT = "30000";

    private String mailHost = null;

    private Session session = null;

    private MailAuthenticator authenticator = null;

    private Protocol protocol = Protocol.POP3;

    private String accountName = null;

    private int pollPeriode = 10;

    private float elapsedTime = 0;

    private MailBoxState state = MailBoxState.NOT_CHECKED;

    private int noOfMails = 0;

    private HashMap<Integer, MailMessage> messages = new HashMap<Integer, MailMessage>();

    private Store store = null;

    private boolean newMessages = false;

    private int lastTime = 0;

    public MailAccount(String userName, String password, String mailHost, String accountName, int pollPeriode) {
        this.accountName = accountName;
        this.authenticator = new MailAuthenticator(userName, password);
        this.mailHost = mailHost;
        this.session = Session.getInstance(getConnectionProperties(), this.authenticator);
        this.pollPeriode = pollPeriode;
    }

    public MailAccount(Node node) throws NoSuchProviderException {
        String userName = null;
        String password = null;
        String host = null;
        String displayName = null;
        int polling = -1;
        Protocol prot = null;
        NodeList nodes = node.getChildNodes();
        for (int j = 0; j < nodes.getLength(); j++) {
            Node current = nodes.item(j);
            String nodeName = current.getNodeName();
            if ("userName".equals(nodeName)) {
                userName = ((CDATASection) current.getFirstChild()).getData();
            } else if ("password".equals(nodeName)) {
                password = ((CDATASection) current.getFirstChild()).getData();
            } else if ("host".equals(nodeName)) {
                host = ((CDATASection) current.getFirstChild()).getData();
            } else if ("displayName".equals(nodeName)) {
                displayName = ((CDATASection) current.getFirstChild()).getData();
            } else if ("pollingInterval".equals(nodeName)) {
                polling = Integer.valueOf(current.getFirstChild().getNodeValue()).intValue();
            } else if ("protocol".equals(nodeName)) {
                prot = Protocol.values()[Byte.parseByte(current.getFirstChild().getNodeValue())];
            }
        }
        this.accountName = displayName;
        this.authenticator = new MailAuthenticator(userName, password);
        this.mailHost = host;
        this.session = Session.getInstance(getConnectionProperties(), this.authenticator);
        this.pollPeriode = polling;
        this.setProtocol(prot);
    }

    private Properties getConnectionProperties() {
        Properties systemProperties = System.getProperties();
        systemProperties.put("mail.pop3.connectiontimeout", CONNECTION_TIMEOUT);
        systemProperties.put("mail.pop3.timeout", CONNECTION_TIMEOUT);
        systemProperties.put("mail.imap.connectiontimeout", CONNECTION_TIMEOUT);
        systemProperties.put("mail.imap.timeout", CONNECTION_TIMEOUT);
        return systemProperties;
    }

    /**
	 * The supported protocol types for fetching mails.
	 *
	 * @author <a href="mailto:cschalm@users.sourceforge.net">Carsten Schalm</a>
	 * @version $Id: MailAccount.java 116 2011-10-31 22:12:13Z cschalm $
	 */
    public enum Protocol {

        POP3, IMAP
    }

    /**
	 * The type of state change of a mailbox.
	 *
	 * @author <a href="mailto:cschalm@users.sourceforge.net">Carsten Schalm</a>
	 * @version $Id: MailAccount.java 116 2011-10-31 22:12:13Z cschalm $
	 */
    public enum StateChangeType {

        MAILACCOUNT, MAILBOX
    }

    /**
	 * The state of a mailbox.
	 *
	 * @author <a href="mailto:cschalm@users.sourceforge.net">Carsten Schalm</a>
	 * @version $Id: MailAccount.java 116 2011-10-31 22:12:13Z cschalm $
	 */
    public enum MailBoxState {

        NOT_CHECKED, CHECKED, ERROR, FETCHINGALL, FETCHING, DELETING, CONNECTING, CONNECTED
    }

    public String getAccountName() {
        return this.accountName;
    }

    public MailAuthenticator getAuthenticator() {
        return this.authenticator;
    }

    public int getElapsedTime() {
        return (int) this.elapsedTime;
    }

    public String getMailHost() {
        return this.mailHost;
    }

    public int getNoOfMails() {
        return this.noOfMails;
    }

    public MailMessage[] getMessageArray() {
        return this.messages.values().toArray(new MailMessage[this.messages.values().size()]);
    }

    public List<MailMessage> getMessages() {
        List<MailMessage> vector = new ArrayList<MailMessage>();
        vector.addAll(this.messages.values());
        return vector;
    }

    public int getPollPeriode() {
        return this.pollPeriode;
    }

    /**
	 * @param protocol
	 * @throws NoSuchProviderException
	 */
    public void setStoreType(Protocol protocol) throws NoSuchProviderException {
        this.protocol = protocol;
        switch(this.protocol) {
            case IMAP:
                this.store = this.session.getStore(new URLName("imap://" + this.mailHost));
                break;
            default:
                this.store = this.session.getStore("pop3");
        }
    }

    public Protocol getStoreType() {
        return this.protocol;
    }

    public void checkAccount() throws MessagingException {
        try {
            HashSet<Integer> serverMailsSet = new HashSet<Integer>();
            synchronized (this) {
                Folder inbox = this.connect4read();
                if (inbox != null) {
                    this.setState(StateChangeType.MAILACCOUNT, MailBoxState.FETCHINGALL);
                    Message[] msgs = inbox.getMessages();
                    if (msgs != null) {
                        this.noOfMails = msgs.length;
                        for (Message msg : msgs) {
                            String[] addresses = this.getAddresses(msg);
                            MailMessage mm = new MailMessage(addresses[0], addresses[1], msg.getSubject(), msg.getSentDate(), msg.getSize());
                            mm.setMailAccount(this);
                            mm.setRead(msg.isSet(Flags.Flag.SEEN) || msg.isSet(Flags.Flag.ANSWERED));
                            Integer hashCode = Integer.valueOf(mm.hashCode());
                            serverMailsSet.add(hashCode);
                            if (!this.messages.containsKey(hashCode)) {
                                this.messages.put(hashCode, mm);
                                this.newMessages = true;
                            }
                            if (this.newMessages) {
                                if (log.isDebugEnabled()) {
                                    log.debug(this.getAccountName() + " has new messages because of " + mm);
                                }
                            }
                        }
                    }
                    inbox.close(false);
                }
            }
            List<Integer> toDelete = new ArrayList<Integer>();
            for (Integer hashCode : this.messages.keySet()) {
                if (!serverMailsSet.contains(hashCode)) {
                    toDelete.add(hashCode);
                }
            }
            for (Integer hashCode : toDelete) {
                this.messages.remove(hashCode);
            }
            this.elapsedTime = 0f;
            this.store.close();
            this.setState(StateChangeType.MAILBOX, MailBoxState.CHECKED);
        } catch (MessagingException me) {
            this.setState(StateChangeType.MAILBOX, MailBoxState.ERROR);
            throw me;
        } finally {
            if (this.store.isConnected()) {
                try {
                    this.store.close();
                } catch (MessagingException me) {
                    throw me;
                }
            }
        }
    }

    public void setElapsedTime(int seconds) {
        if (seconds > 0) {
            float tick = Float.valueOf(seconds);
            tick /= 1000 * 60;
            this.elapsedTime += tick;
            if ((this.pollPeriode > 0) && (this.elapsedTime >= this.pollPeriode)) {
                try {
                    this.checkAccount();
                } catch (MessagingException e) {
                    log.warn("Error checking mail for " + this.getAccountName() + ": " + e.getMessage());
                }
            } else if (((int) this.elapsedTime) != this.lastTime) {
                this.lastTime = ((int) this.elapsedTime);
                this.setState(StateChangeType.MAILACCOUNT, this.state);
            }
        }
    }

    public MailBoxState getState() {
        return this.state;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setMailHost(String mailhost) {
        this.mailHost = mailhost;
    }

    public void setPollPeriode(int pollPeriode) {
        this.pollPeriode = pollPeriode;
    }

    public void setUserName(String userName) {
        this.authenticator.setUserName(userName);
    }

    private Folder connect4read() throws MessagingException {
        Folder inbox = this.connect();
        inbox.open(READ_ONLY);
        return inbox;
    }

    private Folder connect4readwrite() throws MessagingException {
        Folder inbox = this.connect();
        inbox.open(READ_WRITE);
        return inbox;
    }

    private Folder connect() throws MessagingException {
        this.setState(StateChangeType.MAILACCOUNT, MailBoxState.CONNECTING);
        if (!this.store.isConnected()) {
            switch(this.protocol) {
                case IMAP:
                    this.store.connect();
                    break;
                default:
                    this.store.connect(mailHost, null, null);
            }
        }
        Folder inbox = this.store.getDefaultFolder();
        inbox = inbox.getFolder("INBOX");
        this.setState(StateChangeType.MAILACCOUNT, MailBoxState.CONNECTED);
        return inbox;
    }

    @SuppressWarnings("unchecked")
    public String getContent(MailMessage mailMessage, boolean withHeader) throws MessagingException {
        try {
            StringBuilder result = new StringBuilder();
            if (!mailMessage.isCached()) {
                synchronized (this) {
                    StringBuilder content = new StringBuilder();
                    StringBuilder headers = new StringBuilder();
                    Folder inbox = this.connect4read();
                    if (inbox != null) {
                        this.setState(StateChangeType.MAILACCOUNT, MailBoxState.FETCHING);
                        Message message = this.findMessage(inbox, mailMessage);
                        if (message != null) {
                            Enumeration<String> header = ((MimeMessage) message).getAllHeaderLines();
                            while (header.hasMoreElements()) {
                                headers.append(header.nextElement()).append("\n");
                            }
                            mailMessage.setHeader(headers.toString());
                            if (message.isMimeType("text/*")) {
                                try {
                                    content.append((String) message.getContent());
                                } catch (Exception e) {
                                    log.warn(e.getMessage());
                                }
                            } else if (message.isMimeType("multipart/*")) {
                                try {
                                    Multipart mp = (Multipart) message.getContent();
                                    int count = mp.getCount();
                                    for (int c = 0; c < count; c++) {
                                        MimeBodyPart mbp = (MimeBodyPart) mp.getBodyPart(c);
                                        if (mbp.isMimeType("text/*")) {
                                            content.append("\nTEXTPART -->\n\n");
                                            String cont = (String) mbp.getContent();
                                            content.append(cont).append("\n\n<-- END-OF-TEXTPART\n");
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn(e.getMessage());
                                }
                            }
                            mailMessage.setContent(content.toString());
                            mailMessage.setRead(true);
                        }
                        this.elapsedTime = 0f;
                        inbox.close(false);
                        this.store.close();
                        this.setState(StateChangeType.MAILACCOUNT, MailBoxState.CHECKED);
                    }
                }
            }
            if (withHeader) {
                result.append(mailMessage.getHeader());
                result.append("\n\n");
            }
            result.append(mailMessage.getContent());
            return result.toString();
        } catch (MessagingException me) {
            this.setState(StateChangeType.MAILBOX, MailBoxState.ERROR);
            throw me;
        } finally {
            if (this.store.isConnected()) {
                try {
                    this.store.close();
                } catch (MessagingException me) {
                    throw me;
                }
            }
        }
    }

    private void setState(StateChangeType type, MailBoxState state) {
        this.state = state;
        super.setChanged();
        super.notifyObservers(type);
    }

    private Message findMessage(Folder folder, MailMessage toFind) throws MessagingException {
        Message[] msgs = folder.getMessages();
        if (msgs != null) {
            for (Message msg : msgs) {
                String[] addresses = this.getAddresses(msg);
                MailMessage currentMail = new MailMessage(addresses[0], addresses[1], msg.getSubject(), msg.getSentDate(), msg.getSize());
                if (currentMail.hashCode() == toFind.hashCode()) {
                    return msg;
                }
            }
        }
        return null;
    }

    public void deleteMailMessage(MailMessage toDelete) throws MessagingException {
        try {
            synchronized (this) {
                Folder inbox = this.connect4readwrite();
                if (inbox != null) {
                    this.setState(StateChangeType.MAILACCOUNT, MailBoxState.DELETING);
                    Message message = this.findMessage(inbox, toDelete);
                    if (message != null) {
                        message.setFlag(Flags.Flag.DELETED, true);
                    }
                    inbox.close(true);
                    this.store.close();
                    this.messages.remove(toDelete.hashCode());
                    this.elapsedTime = 0f;
                    this.noOfMails--;
                    this.setState(StateChangeType.MAILBOX, MailBoxState.CHECKED);
                }
            }
        } catch (MessagingException me) {
            this.setState(StateChangeType.MAILBOX, MailBoxState.ERROR);
            throw me;
        } finally {
            if (this.store.isConnected()) {
                try {
                    this.store.close();
                } catch (MessagingException me) {
                    throw me;
                }
            }
        }
    }

    public void deleteMailMessage(List<MailMessage> toDeleteList) throws MessagingException {
        try {
            synchronized (this) {
                Folder inbox = this.connect4readwrite();
                if (inbox != null) {
                    this.setState(StateChangeType.MAILACCOUNT, MailBoxState.DELETING);
                    for (MailMessage mailMessage : toDeleteList) {
                        Message message = this.findMessage(inbox, mailMessage);
                        if (message != null) {
                            message.setFlag(Flags.Flag.DELETED, true);
                        }
                        this.messages.remove(mailMessage.hashCode());
                    }
                    inbox.close(true);
                    this.store.close();
                    this.elapsedTime = 0f;
                    this.noOfMails -= toDeleteList.size();
                    this.setState(StateChangeType.MAILBOX, MailBoxState.CHECKED);
                }
            }
        } catch (MessagingException me) {
            this.setState(StateChangeType.MAILBOX, MailBoxState.ERROR);
            throw me;
        } finally {
            if (this.store.isConnected()) {
                try {
                    this.store.close();
                } catch (MessagingException me) {
                    throw me;
                }
            }
        }
    }

    public Node toXml(Document doc) {
        Element account = doc.createElement("account");
        Element userName = doc.createElement("userName");
        userName.appendChild(doc.createCDATASection(this.getAuthenticator().getUserName()));
        Element password = doc.createElement("password");
        password.appendChild(doc.createCDATASection(this.getAuthenticator().getPassword()));
        Element host = doc.createElement("host");
        host.appendChild(doc.createCDATASection(this.getMailHost()));
        Element displayName = doc.createElement("displayName");
        displayName.appendChild(doc.createCDATASection(this.getAccountName()));
        Element poll = doc.createElement("pollingInterval");
        poll.appendChild(doc.createTextNode(String.valueOf(this.getPollPeriode())));
        Element eProtocol = doc.createElement("protocol");
        eProtocol.appendChild(doc.createTextNode(Integer.toString(this.getStoreType().ordinal())));
        account.appendChild(userName);
        account.appendChild(password);
        account.appendChild(host);
        account.appendChild(displayName);
        account.appendChild(poll);
        account.appendChild(eProtocol);
        return account;
    }

    public boolean hasNewMessages() {
        if (log.isDebugEnabled()) {
            log.debug(this.accountName + " has new messages: " + this.newMessages);
        }
        return this.newMessages;
    }

    public void resetNewMessages() {
        this.newMessages = false;
    }

    @Override
    public String toString() {
        return this.getAccountName();
    }

    private String[] getAddresses(Message message) {
        String from = "Illegal character(s) in sender-address";
        try {
            from = message.getFrom()[0].toString();
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Sender-Address is wrong: " + e.getMessage(), e);
            }
        }
        String to = "";
        try {
            Address[] toAddresses = message.getRecipients(Message.RecipientType.TO);
            if (toAddresses != null && toAddresses.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (Address toAddress : toAddresses) {
                    if (sb.length() > 1) {
                        sb.append(", ");
                    }
                    sb.append(toAddress.toString());
                }
                to = sb.toString();
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Error getting recipients: " + e.getMessage(), e);
            }
        }
        return new String[] { from, to };
    }

    private void setProtocol(Protocol protocol) throws NoSuchProviderException {
        this.setStoreType(protocol);
    }

    public boolean containsMessage(MailMessage mailMessage) {
        Integer hashCode = Integer.valueOf(mailMessage.hashCode());
        return this.messages.containsKey(hashCode);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MailAccount other = (MailAccount) obj;
        if (this.authenticator != other.authenticator && (this.authenticator == null || !this.authenticator.equals(other.authenticator))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.authenticator != null ? this.authenticator.hashCode() : 0);
        return hash;
    }
}

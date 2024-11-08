package org.compiere.server;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.mail.*;
import org.compiere.*;
import org.compiere.model.*;
import org.compiere.util.*;

/**
 *	Request Mail Processor
 *	
 *  @author Jorg Janke
 *  @version $Id: EMailProcessor.java,v 1.3 2006/07/30 00:53:33 jjanke Exp $
 */
public class EMailProcessor {

    /**
	 * 	EMail Processor
	 *	@param client client
	 */
    public EMailProcessor(MClient client) {
        this(client.getSMTPHost(), client.getRequestUser(), client.getRequestUserPW());
    }

    /**
	 * 	EMail Processor
	 * 	@param host host
	 * 	@param user user id
	 * 	@param password password
	 */
    public EMailProcessor(String host, String user, String password) {
        m_host = host;
        m_user = user;
        m_pass = password;
    }

    /**	EMail Host Parameter		*/
    private String m_host = null;

    /**	EMail User Parameter		*/
    private String m_user = null;

    /**	Password Parameter			*/
    private String m_pass = null;

    /**	Session				*/
    private Session m_session = null;

    /**	Store				*/
    private Store m_store = null;

    /**	Logger			*/
    protected CLogger log = CLogger.getCLogger(getClass());

    /**	Process Error				*/
    private static final int ERROR = 0;

    /**	Process Request				*/
    private static final int REQUEST = 1;

    /**	Process Workflow			*/
    private static final int WORKFLOW = 2;

    /**	Process Delivery Confirm	*/
    private static final int DELIVERY = 9;

    /**
	 * 	Process Messages in InBox
	 *	@return number of mails processed
	 */
    public int processMessages() {
        int processed = 0;
        try {
            getSession();
            getStore();
            processed = processInBox();
        } catch (Exception e) {
            log.log(Level.SEVERE, "processInBox", e);
        }
        try {
            if (m_store.isConnected()) m_store.close();
        } catch (Exception e) {
        }
        m_store = null;
        return processed;
    }

    /**************************************************************************
	 * 	Get Session
	 *	@return Session
	 *	@throws Exception
	 */
    private Session getSession() throws Exception {
        if (m_session != null) return m_session;
        Properties props = System.getProperties();
        props.put("mail.store.protocol", "smtp");
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.host", m_host);
        props.put("mail.smtp.auth", "true");
        EMailAuthenticator auth = new EMailAuthenticator(m_user, m_pass);
        m_session = Session.getDefaultInstance(props, auth);
        m_session.setDebug(CLogMgt.isLevelFinest());
        log.fine("getSession - " + m_session);
        return m_session;
    }

    /**
	 * 	Get Store
	 *	@return Store
	 *	@throws Exception
	 */
    private Store getStore() throws Exception {
        if (m_store != null) return m_store;
        if (getSession() == null) throw new IllegalStateException("No Session");
        m_store = m_session.getStore("imap");
        m_store.connect();
        log.fine("getStore - " + m_store);
        return m_store;
    }

    /**
	 * 	Process InBox
	 *	@return number of processed
	 *	@throws Exception
	 */
    private int processInBox() throws Exception {
        Folder folder;
        folder = m_store.getDefaultFolder();
        if (folder == null) throw new IllegalStateException("No default folder");
        Folder inbox = folder.getFolder("INBOX");
        if (!inbox.exists()) throw new IllegalStateException("No Inbox");
        inbox.open(Folder.READ_WRITE);
        log.fine("processInBox - " + inbox.getName() + "; Messages Total=" + inbox.getMessageCount() + "; New=" + inbox.getNewMessageCount());
        Folder requestFolder = folder.getFolder("CRequest");
        if (!requestFolder.exists() && !requestFolder.create(Folder.HOLDS_MESSAGES)) throw new IllegalStateException("Cannot create Request Folder");
        requestFolder.open(Folder.READ_WRITE);
        Folder workflowFolder = folder.getFolder("CWorkflow");
        if (!workflowFolder.exists() && !workflowFolder.create(Folder.HOLDS_MESSAGES)) throw new IllegalStateException("Cannot create Workflow Folder");
        workflowFolder.open(Folder.READ_WRITE);
        Folder errorFolder = folder.getFolder("AdempiereError");
        if (!errorFolder.exists() && !errorFolder.create(Folder.HOLDS_MESSAGES)) throw new IllegalStateException("Cannot create Error Folder");
        errorFolder.open(Folder.READ_WRITE);
        Message[] messages = inbox.getMessages();
        int noProcessed = 0;
        int noError = 0;
        for (int i = 0; i < messages.length; i++) {
            Message msg = messages[i];
            int result = processMessage(msg);
            if (result == REQUEST) {
                msg.setFlag(Flags.Flag.SEEN, true);
                msg.setFlag(Flags.Flag.ANSWERED, true);
                requestFolder.appendMessages(new Message[] { msg });
            } else if (result == WORKFLOW) {
                msg.setFlag(Flags.Flag.SEEN, true);
                msg.setFlag(Flags.Flag.ANSWERED, true);
                workflowFolder.appendMessages(new Message[] { msg });
            } else if (result == DELIVERY) {
                msg.setFlag(Flags.Flag.SEEN, true);
                msg.setFlag(Flags.Flag.ANSWERED, true);
            } else {
                errorFolder.appendMessages(new Message[] { msg });
                noError++;
            }
            noProcessed++;
        }
        log.info("processInBox - Total=" + noProcessed + " - Errors=" + noError);
        errorFolder.close(false);
        requestFolder.close(false);
        workflowFolder.close(false);
        inbox.close(true);
        return noProcessed;
    }

    /**
	 * 	Process Message
	 *	@param msg message
	 *	@return Type of Message
	 *	@throws Exception
	 */
    private int processMessage(Message msg) throws Exception {
        dumpEnvelope(msg);
        dumpBody(msg);
        printOut(":::::::::::::::");
        printOut(getSubject(msg));
        printOut(":::::::::::::::");
        printOut(getMessage(msg));
        printOut(":::::::::::::::");
        String delivery = getDeliveryReport(msg);
        printOut(delivery);
        printOut(":::::::::::::::");
        return ERROR;
    }

    /**
	 * 	Get Subject
	 *	@param msg message
	 *	@return subject or ""
	 */
    private String getSubject(Message msg) {
        try {
            String str = msg.getSubject();
            if (str != null) return str.trim();
        } catch (MessagingException e) {
            log.log(Level.SEVERE, "getSubject", e);
        }
        return "";
    }

    /**
	 * 	Get Message
	 *	@param msg Message
	 *	@return message or ""
	 */
    private String getMessage(Part msg) {
        StringBuffer sb = new StringBuffer();
        try {
            if (msg.isMimeType("text/plain")) {
                sb.append(msg.getContent());
            } else if (msg.isMimeType("text/*")) {
                sb.append(msg.getContent());
            } else if (msg.isMimeType("message/rfc822")) {
                sb.append(msg.getContent());
            } else if (msg.isMimeType("multipart/alternative")) {
                String plainText = null;
                String otherStuff = null;
                Multipart mp = (Multipart) msg.getContent();
                int count = mp.getCount();
                for (int i = 0; i < count; i++) {
                    Part part = mp.getBodyPart(i);
                    Object content = part.getContent();
                    if (content == null || content.toString().trim().length() == 0) continue;
                    if (part.isMimeType("text/plain")) plainText = content.toString(); else otherStuff = content.toString();
                }
                if (plainText != null) sb.append(plainText); else if (otherStuff != null) sb.append(otherStuff);
            } else if (msg.isMimeType("multipart/*")) {
                Multipart mp = (Multipart) msg.getContent();
                int count = mp.getCount();
                for (int i = 0; i < count; i++) {
                    String str = getMessage(mp.getBodyPart(i));
                    if (str.length() > 0) {
                        if (sb.length() > 0) sb.append("\n-----\n");
                        sb.append(str);
                    }
                }
            } else {
                Object o = msg.getContent();
                if (o instanceof String) {
                    sb.append(o);
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "getMessage", e);
        }
        return sb.toString().trim();
    }

    /**
	 * 	Get Delivery Report
	 *	@param msg message
	 *	@return delivery info or null
	 */
    private String getDeliveryReport(Part msg) {
        try {
            if (msg.isMimeType("multipart/report")) {
                String deliveryMessage = null;
                String otherStuff = null;
                Multipart mp = (Multipart) msg.getContent();
                int count = mp.getCount();
                for (int i = 0; i < count; i++) {
                    Part part = mp.getBodyPart(i);
                    Object content = part.getContent();
                    if (content == null) continue;
                    if (part.isMimeType("message/*")) deliveryMessage = getDeliveredReportDetail(part); else otherStuff = content.toString().trim();
                }
                if (deliveryMessage != null) return deliveryMessage;
                return otherStuff;
            } else if (msg.isMimeType("message/*")) {
                return getDeliveredReportDetail(msg);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "getDeliveryReport", e);
        }
        return null;
    }

    /**
	 * 	Get Delivered Report Detail
	 *	@param part Mime Type message/*
	 *	@return info or null
	 *	@throws Exception
	 */
    private String getDeliveredReportDetail(Part part) throws Exception {
        Object content = part.getContent();
        if (content == null) return null;
        String deliveryMessage = null;
        if (content instanceof InputStream) {
            StringBuffer sb = new StringBuffer();
            InputStream is = (InputStream) content;
            int c;
            while ((c = is.read()) != -1) sb.append((char) c);
            deliveryMessage = sb.toString().trim();
        } else deliveryMessage = content.toString().trim();
        if (deliveryMessage == null) return null;
        int index = deliveryMessage.indexOf("Final-Recipient:");
        if (index != -1) {
            String finalRecipient = deliveryMessage.substring(index);
            int atIndex = finalRecipient.indexOf('@');
            if (atIndex != -1) {
                index = finalRecipient.lastIndexOf(' ', atIndex);
                if (index != -1) finalRecipient = finalRecipient.substring(index + 1);
                atIndex = finalRecipient.indexOf('@');
                if (atIndex != -1) index = finalRecipient.indexOf(' ', atIndex);
                if (index != -1) finalRecipient = finalRecipient.substring(0, index);
                index = finalRecipient.indexOf('\n');
                if (index != -1) finalRecipient = finalRecipient.substring(0, index);
                return finalRecipient.trim();
            }
        }
        return deliveryMessage;
    }

    /**************************************************************************
	 * 	Print Envelope
	 *	@param m message
	 *	@throws Exception
	 */
    private void dumpEnvelope(Message m) throws Exception {
        printOut("-----------------------------------------------------------------");
        Address[] a;
        if ((a = m.getFrom()) != null) {
            for (int j = 0; j < a.length; j++) printOut("FROM: " + a[j].toString());
        }
        if ((a = m.getRecipients(Message.RecipientType.TO)) != null) {
            for (int j = 0; j < a.length; j++) printOut("TO: " + a[j].toString());
        }
        printOut("SUBJECT: " + m.getSubject());
        java.util.Date d = m.getSentDate();
        printOut("SendDate: " + (d != null ? d.toString() : "UNKNOWN"));
        Flags flags = m.getFlags();
        StringBuffer sb = new StringBuffer();
        Flags.Flag[] sf = flags.getSystemFlags();
        boolean first = true;
        for (int i = 0; i < sf.length; i++) {
            String s;
            Flags.Flag f = sf[i];
            if (f == Flags.Flag.ANSWERED) s = "\\Answered"; else if (f == Flags.Flag.DELETED) s = "\\Deleted"; else if (f == Flags.Flag.DRAFT) s = "\\Draft"; else if (f == Flags.Flag.FLAGGED) s = "\\Flagged"; else if (f == Flags.Flag.RECENT) s = "\\Recent"; else if (f == Flags.Flag.SEEN) s = "\\Seen"; else continue;
            if (first) first = false; else sb.append(' ');
            sb.append(s);
        }
        String[] uf = flags.getUserFlags();
        for (int i = 0; i < uf.length; i++) {
            if (first) first = false; else sb.append(' ');
            sb.append(uf[i]);
        }
        printOut("FLAGS: " + sb.toString());
        String[] hdrs = m.getHeader("X-Mailer");
        if (hdrs != null) {
            StringBuffer sb1 = new StringBuffer("X-Mailer: ");
            for (int i = 0; i < hdrs.length; i++) sb1.append(hdrs[i]).append("  ");
            printOut(sb1.toString());
        } else printOut("X-Mailer NOT available");
        hdrs = m.getHeader("Message-ID");
        if (hdrs != null) {
            StringBuffer sb1 = new StringBuffer("Message-ID: ");
            for (int i = 0; i < hdrs.length; i++) sb1.append(hdrs[i]).append("  ");
            printOut(sb1.toString());
        } else printOut("Message-ID NOT available");
        printOut("ALL HEADERs:");
        Enumeration en = m.getAllHeaders();
        while (en.hasMoreElements()) {
            Header hdr = (Header) en.nextElement();
            printOut("  " + hdr.getName() + " = " + hdr.getValue());
        }
        printOut("-----------------------------------------------------------------");
    }

    /**
	 * 	Print Body
	 *	@param p
	 *	@throws Exception
	 */
    private void dumpBody(Part p) throws Exception {
        printOut("=================================================================");
        printOut("CONTENT-TYPE: " + p.getContentType());
        if (p.isMimeType("text/plain")) {
            printOut("Plain text ---------------------------");
            System.out.println((String) p.getContent());
        } else if (p.getContentType().toUpperCase().startsWith("TEXT")) {
            printOut("Other text ---------------------------");
            System.out.println((String) p.getContent());
        } else if (p.isMimeType("multipart/*")) {
            printOut("Multipart ---------------------------");
            Multipart mp = (Multipart) p.getContent();
            int count = mp.getCount();
            for (int i = 0; i < count; i++) dumpBody(mp.getBodyPart(i));
        } else if (p.isMimeType("message/rfc822")) {
            printOut("Nested ---------------------------");
            dumpBody((Part) p.getContent());
        } else {
            Object o = p.getContent();
            if (o instanceof String) {
                printOut("This is a string ---------------------------");
                System.out.println((String) o);
            } else if (o instanceof InputStream) {
                printOut("This is just an input stream ---------------------------");
                InputStream is = (InputStream) o;
                int c;
                while ((c = is.read()) != -1) System.out.write(c);
            } else {
                printOut("This is an unknown type ---------------------------");
                printOut(o.toString());
            }
        }
        printOut("=================================================================");
    }

    /**
	 * 	Print
	 *	@param s string
	 */
    private static void printOut(String s) {
        System.out.println(s);
    }

    /**************************************************************************
	 *	Main Test
	 *	@param args ignored
	 */
    public static void main(String[] args) {
        Adempiere.startupEnvironment(true);
        EMailProcessor m = new EMailProcessor("admin", "test", "testadempiere");
        m.processMessages();
    }
}

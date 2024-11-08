package com.lbslogics.amorph.input;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import org.jdom.Element;
import com.lbslogics.amorph.output.MailAuthenticator;

/** amorph data getter which gets data to be prepared and
 *  transformed from a pop3 inbox as specified by some
 *  configurabe criteria (within the recipe).
 *
 * @author hans
 */
public class Pop3Getter implements DataGetter {

    private String host;

    private String user;

    private String pass;

    private String url;

    private String protocol;

    private boolean deleteMatch = true;

    private boolean deleteNoMatch = false;

    private int attnum = 1;

    private InputStream result;

    /** JavaMail debug mode on/off */
    private boolean mailDebug = false;

    /** POP3 APOP setting.
     *
     *  default value for APOP is false because some mail
     *  servers make problems when true.
     */
    private String mailPop3Apop = "false";

    private HashMap criteria;

    public void initialize(List params, Map context) {
        criteria = new HashMap();
        final Iterator pit = params.iterator();
        while (pit.hasNext()) {
            Element param = (Element) pit.next();
            String pname = param.getAttributeValue("name");
            String pvalue = param.getAttributeValue("value");
            if ("mail.host".equals(pname)) {
                host = pvalue;
            } else if ("mail.user".equals(pname)) {
                user = pvalue;
            } else if ("mail.pass".equals(pname)) {
                pass = pvalue;
            } else if ("mail.protocol".equals(pname)) {
                protocol = pvalue;
            } else if ("mail.url".equals(pname)) {
                url = pvalue;
            } else if ("match.delete".equals(pname)) {
                deleteMatch = new Boolean(pvalue).booleanValue();
            } else if ("nomatch.delete".equals(pname)) {
                deleteNoMatch = new Boolean(pvalue).booleanValue();
            } else if ("mail.pop3.apop".equals(pname)) {
                mailPop3Apop = pvalue;
            } else if ("mail.debug".equals(pname)) {
                mailDebug = new Boolean(pvalue).booleanValue();
            } else if (pname.startsWith("crit.")) {
                String critKey = pname.substring("crit.".length());
                System.out.println("adding criteria: " + critKey + "=" + pvalue);
                criteria.put(critKey, pvalue.trim());
            } else {
                System.out.println("unknown param:" + pname);
            }
        }
    }

    /** reads mail from the pop3 account as configured and returns
     *  the data read as input stream for further processing.
     * @throws GetterException
     */
    public InputStream getIngredientsAsStream() throws GetterException {
        Properties props = System.getProperties();
        Properties sendProps = new Properties();
        sendProps.setProperty("mail.transport.protocol", "smtp");
        sendProps.setProperty("mail.host", host);
        sendProps.setProperty("mail.user", user);
        sendProps.setProperty("mail.password", pass);
        sendProps.setProperty("mail.pop3.apop", mailPop3Apop);
        MailAuthenticator mAuth = null;
        if ((user != null) && (pass != null)) {
            mAuth = new MailAuthenticator(user, pass);
        }
        Session session = Session.getInstance(sendProps, mAuth);
        session.setDebug(mailDebug);
        Store store = null;
        Folder rf = null;
        try {
            if (url != null) {
                URLName urln = new URLName(url);
                store = session.getStore(urln);
                store.connect();
            } else {
                if (protocol != null) store = session.getStore(protocol); else store = session.getStore();
                if (host != null || user != null || pass != null) store.connect(host, user, pass); else store.connect();
            }
        } catch (Exception e) {
            throw new GetterException(e, "could_not_connect_to_mail_store: " + e.getMessage());
        }
        System.out.println("connected to mail store.");
        try {
            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);
            Message messages[] = folder.getMessages();
            System.out.println(messages.length + " messages available.");
            for (int i = 0; (i < messages.length) && (result == null); i++) {
                Message m = messages[i];
                int mnum = m.getMessageNumber();
                handleMessage(m);
            }
            folder.close(true);
            store.close();
        } catch (MessagingException e) {
            throw new GetterException(e, "error_while_handling_messages: " + e.getMessage());
        } catch (IOException e) {
            throw new GetterException(e, "error_while_handling_messages: " + e.getMessage());
        } catch (GetterException e) {
            throw new GetterException(e, "error_while_handling_messages: " + e.getMessage());
        }
        return result;
    }

    /** handles a single message
     *
     * @param m
     * @throws MessagingException
     * @throws IOException
     * @throws GetterException
     */
    private void handleMessage(Message m) throws MessagingException, IOException, GetterException {
        String ct = m.getContentType();
        String filename = m.getFileName();
        String msub = m.getSubject();
        System.out.println("handling message: " + msub + " content-type=" + ct + " clazz=" + m.getClass().getName());
        if (checkCriteria(m)) {
            if (m.isMimeType("multipart/*")) {
                Multipart mp = (Multipart) m.getContent();
                int count = mp.getCount();
                System.out.println("handling " + count + " message parts.");
                for (int i = 0; i < count; i++) {
                    if (handlePart(mp.getBodyPart(i))) {
                        if (deleteMatch) {
                            m.setFlag(Flags.Flag.DELETED, true);
                        } else {
                            System.out.println("no message part did match.");
                        }
                    }
                }
            }
        } else {
            if (deleteNoMatch) {
                m.setFlag(Flags.Flag.DELETED, true);
            }
        }
    }

    /** checks if a message fits the defined criteria
     *
     * @throws MessagingException */
    private boolean checkCriteria(Message m) throws MessagingException {
        boolean fit = true;
        final Iterator keyIt = criteria.keySet().iterator();
        while (keyIt.hasNext() && fit) {
            final String key = (String) keyIt.next();
            final String crit = (String) criteria.get(key);
            if (key.equals("subject")) {
                String msgSub = m.getSubject();
                System.out.println("checking subject: '" + msgSub + "'==>'" + crit + "'");
                fit = crit.equals(msgSub);
            }
        }
        return fit;
    }

    /** handles a message part
     *
     * @param p
     * @throws MessagingException
     * @throws GetterException
     */
    private boolean handlePart(Part p) throws MessagingException, GetterException {
        String filename = p.getFileName();
        if (!p.isMimeType("multipart/*")) {
            String disp = p.getDisposition();
            if (disp == null || disp.equalsIgnoreCase(Part.ATTACHMENT)) {
                if (checkCriteria(p)) {
                    if (filename == null) filename = "Attachment" + attnum++;
                    if (result == null) {
                        try {
                            File f = File.createTempFile("amorph_pop3-", ".tmp");
                            f.deleteOnExit();
                            OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
                            InputStream is = p.getInputStream();
                            int c;
                            while ((c = is.read()) != -1) os.write(c);
                            os.close();
                            result = new FileInputStream(f);
                            System.out.println("saved attachment to file: " + f.getAbsolutePath());
                            return true;
                        } catch (IOException ex) {
                            throw new GetterException(ex, "Failed to save attachment: " + ex);
                        }
                    }
                }
            }
        }
        return false;
    }

    /** checks if a message parts fits the defined criteria
     *
     * @throws MessagingException */
    private boolean checkCriteria(Part p) throws MessagingException {
        boolean fit = true;
        final Iterator keyIt = criteria.keySet().iterator();
        while (keyIt.hasNext() && fit) {
            final String key = (String) keyIt.next();
            final String crit = (String) criteria.get(key);
            if (key.equals("content-type")) {
                String pCt = p.getContentType();
                if (pCt.indexOf(";") > -1) {
                    pCt = pCt.substring(0, pCt.indexOf(";"));
                }
                System.out.println("checking part content-type: '" + pCt + "'==>'" + crit + "'");
                fit = crit.equals(pCt);
            } else if (key.equals("filename")) {
                String filename = p.getFileName();
                System.out.println("checking part filename: '" + filename + "'==>'" + crit + "'");
                fit = crit.equals(filename);
            }
        }
        System.out.println("returning " + fit);
        return fit;
    }
}

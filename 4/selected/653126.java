package ru.adv.test;

import java.io.InputStream;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Store;
import javax.mail.search.SubjectTerm;
import org.springframework.beans.factory.InitializingBean;

public class MailReaderImpl implements InitializingBean, MailReader {

    private Properties props;

    private String _mbox = "INBOX";

    private javax.mail.Session _session;

    private Store store;

    private Folder folder;

    private boolean isClosed = false;

    private static boolean showStructure = false;

    private static int level;

    private String mailStoreProtocol;

    private String mailHost;

    private String mailUser;

    private String mailPassword;

    public MailReaderImpl() throws Exception {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        props = new Properties();
        props.setProperty("mail.store.protocol", getMailStoreProtocol());
        props.setProperty("mail.host", getMailHost());
        props.setProperty("mail.user", getMailUser());
        props.setProperty("mail.password", getMailPassword());
        _session = javax.mail.Session.getDefaultInstance(props, null);
    }

    public String getMailStoreProtocol() {
        return mailStoreProtocol;
    }

    public void setMailStoreProtocol(String mailStoreProtocol) {
        this.mailStoreProtocol = mailStoreProtocol;
    }

    public String getMailHost() {
        return mailHost;
    }

    public void setMailHost(String mailHost) {
        this.mailHost = mailHost;
    }

    public String getMailUser() {
        return mailUser;
    }

    public void setMailUser(String mailUser) {
        this.mailUser = mailUser;
    }

    public String getMailPassword() {
        return mailPassword;
    }

    public void setMailPassword(String mailPassword) {
        this.mailPassword = mailPassword;
    }

    public void emptyBox(String subject) throws MessagingException {
        open();
        Message[] msgs = find(subject);
        for (int i = 0; i < msgs.length; i++) {
            msgs[i].setFlag(Flags.Flag.DELETED, true);
        }
        close();
    }

    public Message[] find(String subject) throws MessagingException {
        Message[] msgs = subject != null ? folder.search(new SubjectTerm(subject)) : folder.getMessages();
        if (msgs.length > 0) {
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(FetchProfile.Item.FLAGS);
            fp.add("X-Mailer");
            folder.fetch(msgs, fp);
        }
        return msgs;
    }

    public void open() throws MessagingException {
        isClosed = false;
        store = _session.getStore(getProtocol());
        store.connect(getHost(), getUser(), getPassword());
        folder = store.getDefaultFolder();
        if (folder == null) {
            throw new MessagingException("No default folder");
        }
        folder = folder.getFolder(_mbox);
        if (folder == null) {
            throw new MessagingException("Invalid folder");
        }
        folder.open(Folder.READ_WRITE);
    }

    private String getPassword() {
        String password = props.getProperty("mail.password", null);
        if (password == null) {
            password = props.getProperty("mail." + getProtocol() + ".password", null);
        }
        return password;
    }

    public String getUser() {
        String user = props.getProperty("mail.user", null);
        if (user == null) {
            user = props.getProperty("mail." + getProtocol() + ".user", null);
        }
        return user;
    }

    public String getHost() {
        String host = props.getProperty("mail.host", null);
        if (host == null) {
            host = props.getProperty("mail." + getProtocol() + ".host", null);
            if (host == null) {
                host = System.getProperty("mail.host");
                if (host == null || host.length() == 0 || host.equals("${mail.host}")) {
                    host = "localhost";
                }
            }
        }
        return host;
    }

    private String getProtocol() {
        return props.getProperty("mail.store.protocol", null);
    }

    public void close() throws MessagingException {
        if (isClosed) {
            return;
        }
        folder.close(true);
        store.close();
        isClosed = true;
    }

    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public static void dumpPart(Part p) throws Exception {
        if (p instanceof Message) dumpEnvelope((Message) p);
        pr("CONTENT-TYPE: " + p.getContentType());
        if (p.isMimeType("text/plain")) {
            pr("This is plain text");
            pr("---------------------------");
            if (!showStructure) System.out.println((String) p.getContent());
        } else if (p.isMimeType("multipart/*")) {
            pr("This is a Multipart");
            pr("---------------------------");
            Multipart mp = (Multipart) p.getContent();
            level++;
            int count = mp.getCount();
            for (int i = 0; i < count; i++) dumpPart(mp.getBodyPart(i));
            level--;
        } else if (p.isMimeType("message/rfc822")) {
            pr("This is a Nested Message");
            pr("---------------------------");
            level++;
            dumpPart((Part) p.getContent());
            level--;
        } else if (!showStructure) {
            Object o = p.getContent();
            if (o instanceof String) {
                pr("This is a string");
                pr("---------------------------");
                System.out.println((String) o);
            } else if (o instanceof InputStream) {
                pr("This is just an input stream");
                pr("---------------------------");
                InputStream is = (InputStream) o;
                int c;
                while ((c = is.read()) != -1) System.out.write(c);
            } else {
                pr("This is an unknown type");
                pr("---------------------------");
                pr(o.toString());
            }
        } else {
            pr("This is an unknown type");
            pr("---------------------------");
        }
    }

    public static void dumpEnvelope(Message m) throws Exception {
        pr("This is the message envelope");
        pr("---------------------------");
        Address[] a;
        if ((a = m.getFrom()) != null) {
            for (int j = 0; j < a.length; j++) pr("FROM: " + a[j].toString());
        }
        if ((a = m.getRecipients(Message.RecipientType.TO)) != null) {
            for (int j = 0; j < a.length; j++) pr("TO: " + a[j].toString());
        }
        pr("SUBJECT: " + m.getSubject());
        java.util.Date d = m.getSentDate();
        pr("SendDate: " + (d != null ? d.toString() : "UNKNOWN"));
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
        pr("FLAGS: " + sb.toString());
        String[] hdrs = m.getHeader("X-Mailer");
        if (hdrs != null) pr("X-Mailer: " + hdrs[0]); else pr("X-Mailer NOT available");
    }

    static String indentStr = "                  ";

    public static void pr(String s) {
        if (showStructure) System.out.print(indentStr.substring(0, level * 2));
        System.out.println(s);
    }
}

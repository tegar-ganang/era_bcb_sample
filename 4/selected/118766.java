package gawky.mail;

import gawky.global.Constant;
import gawky.global.Option;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.AndTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Ingo Harbeck
 *  
 */
public class Mail {

    private static Log log = LogFactory.getLog(Mail.class);

    public static int ERROR_INVALIDADDRESS = 1;

    public static int ERROR_SENDFAILED = 2;

    public static int ERROR_IO = 3;

    public static int STATUS_OK = 0;

    private static String charsettext = Constant.ENCODE_UTF8;

    public static void main(String[] args) throws Exception {
        Option.init();
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        String host = Option.getProperty("mail.server");
        String user = Option.getProperty("mail.user");
        String pass = Option.getProperty("mail.password");
        Store store = session.getStore("imap");
        store.connect(host, user, pass);
        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_ONLY);
        SearchTerm st = new AndTerm(new SubjectTerm("TICKING"), new FlagTerm(new Flags(Flags.Flag.SEEN), false));
        Message messages[] = folder.search(st);
        for (Message message : messages) {
            MailParter cmessage;
            cmessage = new MailParter(message);
            System.out.println(cmessage.getMessageNumber() + ": " + cmessage.getFrom() + " " + cmessage.getSubject());
            System.out.println(cmessage.getBodytext());
            System.out.println(cmessage.getAttachmentCount());
            for (gawky.mail.Attachment att : cmessage.getAttachments()) {
                System.out.println(att.getFilename());
            }
        }
        folder.close(false);
        store.close();
    }

    public static int sendMailMain(String host, String username, String password, String doauth, InternetAddress from, ArrayList<InternetAddress> to, ArrayList<InternetAddress> reply, String bounceaddress, String subject, String body, boolean html, InputStream stream, String attachName, boolean dozip, Map<String, String> templateparameter, Map<String, String> cids) {
        if (templateparameter != null) {
            body = templateReplacer(body, templateparameter);
            subject = templateReplacer(subject, templateparameter);
        }
        try {
            System.setProperty("mail.mime.charset", charsettext);
            java.util.Properties prop = new Properties();
            prop.put("mail.transport.protocol", "smtp");
            prop.put("mail.smtp.host", host);
            prop.put("mail.smtp.auth", doauth);
            if (bounceaddress != null && !bounceaddress.equals("")) prop.put("mail.smtp.from", bounceaddress);
            Authenticator auth = new SMTPAuthenticator(username, password);
            Session session = Session.getDefaultInstance(prop, auth);
            MimeMessage message = new MimeMessage(session);
            message.setHeader("Content-Transfer-Encoding", "8bit");
            message.setHeader("Content-Type", "text/plain; charset=UTF-8");
            message.setFrom(from);
            for (InternetAddress addr : to) message.addRecipient(javax.mail.Message.RecipientType.TO, addr);
            message.setSubject(subject.trim(), charsettext);
            if (reply != null) message.setReplyTo((Address[]) reply.toArray(new Address[reply.size()]));
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setHeader("Content-Transfer-Encoding", "8bit");
            if (html) {
                messageBodyPart.setContent(body, "text/html; charset=utf-8");
            } else {
                messageBodyPart.setContent(body, "text/plain; charset=utf-8");
                messageBodyPart.setText(body, charsettext);
            }
            Multipart multipart = new MimeMultipart("mixed");
            multipart.addBodyPart(messageBodyPart);
            if (cids != null) {
                String imagepath = Option.getProperty("mail.images");
                for (String cid : cids.keySet()) {
                    String imagefilename = imagepath + "/" + cids.get(cid);
                    MimeBodyPart inlineimg = new MimeBodyPart();
                    FileDataSource fileds = new FileDataSource(imagefilename);
                    inlineimg.setFileName(fileds.getName());
                    inlineimg.setText("dd");
                    inlineimg.setDataHandler(new DataHandler(fileds));
                    inlineimg.setHeader("Content-ID", "<" + cid + ">");
                    inlineimg.setDisposition("inline");
                    multipart.addBodyPart(inlineimg);
                }
            }
            if (stream != null && stream.available() > 0) {
                messageBodyPart = new MimeBodyPart();
                javax.activation.DataSource source = null;
                if (!dozip) source = new ByteArrayDataSource(stream, "application/octet-stream"); else source = new ByteArrayDataSource(zipStream(stream), "application/octet-stream");
                messageBodyPart.setDataHandler(new DataHandler(source));
                messageBodyPart.setFileName(attachName);
                multipart.addBodyPart(messageBodyPart);
            }
            message.setContent(multipart);
            Transport transport = session.getTransport();
            transport.connect();
            transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
            transport.close();
        } catch (IOException e) {
            log.error(e);
            return ERROR_IO;
        } catch (AddressException e) {
            log.error(e);
            return ERROR_INVALIDADDRESS;
        } catch (MessagingException e) {
            log.error(e);
            return ERROR_SENDFAILED;
        }
        return STATUS_OK;
    }

    public static byte[] zipStream(InputStream stream) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        GZIPOutputStream out = new GZIPOutputStream(bout);
        int ch;
        while ((ch = stream.read()) != -1) {
            out.write(ch);
        }
        out.finish();
        out.close();
        return bout.toByteArray();
    }

    /**
     * replace {key} with corresponding HashValue
     * 
     * @param message
     * @param hash
     * @return
     */
    public static final String templateReplacer(String message, Map<String, String> hash) {
        for (String key : hash.keySet()) {
            message = message.replaceAll("\\{" + key + "\\}", (String) hash.get(key));
        }
        return message;
    }
}

/**
 * 
 * Datasource Wrapper for attachment handling
 * 
 * @author Ingo Harbeck
 *
 */
class ByteArrayDataSource implements DataSource {

    private byte data[];

    private String type;

    public ByteArrayDataSource(InputStream is, String type) {
        this.type = type;
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int ch;
            while ((ch = is.read()) != -1) os.write(ch);
            data = os.toByteArray();
        } catch (IOException ioexception) {
        }
    }

    public ByteArrayDataSource(byte data[], String type) {
        this.data = data;
        this.type = type;
    }

    public ByteArrayDataSource(String data, String type) {
        try {
            this.data = data.getBytes("iso-8859-1");
        } catch (UnsupportedEncodingException unsupportedencodingexception) {
        }
        this.type = type;
    }

    public InputStream getInputStream() throws IOException {
        if (data == null) throw new IOException("no data"); else return new ByteArrayInputStream(data);
    }

    public OutputStream getOutputStream() throws IOException {
        throw new IOException("cannot do this");
    }

    public String getContentType() {
        return type;
    }

    public String getName() {
        return "ByteArrayDataSource";
    }
}

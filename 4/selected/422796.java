package acoma;

import it.softeco.commius.mailmetadatastore.exception.MailMetadataStoreException;
import it.softeco.commius.mailmetadatastore.services.MailMetadataStoreService;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import nanoxml.XMLElement;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import commius.module.Module;
import commius.module.ModuleResult;
import eu.commius.mpp.util.EmailBuilder;
import acoma.common.ReadWriteTextFile;
import acoma.common.XMLResult;
import acoma.AttachmentProcessing;

/**
 * 
 * EmailHandler represents an object responsible for decomposition, saving, preparing email parts, etc...
 * 
 * @author Martin �eleng
 *
 */
public class EmailHandler {

    /**
	 * 
	 * HtmlData represents the data, which will be added to the email and the name of the part
	 * 
	 * @author Martin �eleng
	 *
	 */
    class HtmlData {

        private String filename = "";

        private String data = "";

        HtmlData() {
            filename = "";
            data = "";
        }
    }

    private static Logger log = Logger.getLogger(EmailHandler.class.getName());

    private int attachmentCount = 0;

    private AttachmentProcessing attachmentprocessing = new AttachmentProcessing();

    private String body = "";

    private boolean first = true;

    private static String ATTACHMENTS = "attachments";

    private static String HEADER = "header";

    private static String NOTES_TXT = "notes.txt";

    private static String NOTES_XML = "notes.xml";

    private static String EMAIL_EML = "email.eml";

    private static String xml = "";

    /**
	 * AddNotes method add a notes to the email. There are several ways how-to do it:
	 * 1. as a one inline attachment - only http link to the container (jetty, tomcat, ...)
	 * 2. as a two inline attachments - http link and text of notes
	 * 3. as a replacement to the original email and original email as an attachment
	 * @param originalEmail is a text of the original email
	 * @param html is an array of added information to the email
	 * @param acomaid is an id of the email
	 * @param link is a http link to the notes
	 * @param notes is a text of notes/hints
	 * @param type is a parameter which tells Acoma which type of the email message would be composed
	 * @return The text of the new enriched email in raw format.
	 */
    public StringBuffer AddNotes(String originalEmail, HtmlData[] html, String acomaid, String link, String notes, String type) {
        MimeMessage message = null;
        BodyPart messageBodyPart = new MimeBodyPart();
        Multipart multipart = new MimeMultipart("mixed");
        try {
            log.debug("Creating new message");
            if (type.equals("COMPLEX")) {
                int mainMessageIndex = getMainMessage(html);
                if (mainMessageIndex == -1) {
                    message = CreateMainMessage(originalEmail, "", acomaid, "SIMPLE", multipart);
                    for (int i = 0; i < html.length; i++) {
                        messageBodyPart = AddAttachment(html[i].data, "text/html", html[i].filename);
                        multipart.addBodyPart(messageBodyPart);
                    }
                } else {
                    if (html.length == 0) message = CreateMainMessage(originalEmail, "", acomaid, "SIMPLE", multipart); else {
                        log.debug("Creating complex message");
                        message = CreateMainMessage(originalEmail, html[mainMessageIndex].data, acomaid, type, multipart);
                        messageBodyPart = AddAttachment(originalEmail, "message/rfc822", "originalemail.eml");
                        multipart.addBodyPart(messageBodyPart);
                    }
                }
            } else message = CreateMainMessage(originalEmail, "", acomaid, type, multipart);
            if (type.equals("SIMPLE") || type.equals("TEXT") || type.equals("COMPLEX")) {
                log.debug("Creating simple message");
                messageBodyPart = AddAttachment(link, "text/plain", "link.txt");
                multipart.addBodyPart(messageBodyPart);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                multipart.writeTo(os);
            }
            if ((type.equals("TEXT") || type.equals("COMPLEX")) && notes.length() != 0) {
                log.debug("Creating text message");
                messageBodyPart = AddAttachment(notes, "text/plain", NOTES_TXT);
                multipart.addBodyPart(messageBodyPart);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                multipart.writeTo(os);
            }
            if (type.equals("HTML")) {
                log.debug("Creating HTML message");
            }
            message.setContent(multipart);
            message.saveChanges();
            log.debug("Message prepared");
        } catch (MessagingException e) {
            log.error(e);
            e.printStackTrace();
        } catch (IOException e) {
            log.error(e);
            e.printStackTrace();
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            message.writeTo(os);
            return new StringBuffer(os.toString());
        } catch (Exception e) {
            log.error(e);
            e.printStackTrace();
            return new StringBuffer();
        }
    }

    /**
	 * AddNotes method add a notes to the email. There are several ways how-to do it:
	 * 1. as a one inline attachment - only http link to the container (jetty, tomcat, ...)
	 * 2. as a two inline attachments - http link and text of notes
	 * 3. as a replacement to the original email and original email as an attachment
	 * @param originalEmail is a byte stream of the original email
	 * @param html is an array of added information to the email
	 * @param acomaid is an id of the email
	 * @param link is a http link to the notes
	 * @param notes is a text of notes/hints
	 * @param type is a parameter which tells Acoma which type of the email message would be composed
	 * @return The text of the new enriched email in raw format.
	 */
    private byte[] AddNotes(byte[] originalEmail, HtmlData[] html, String acomaid, String link, String notes, String type) {
        MimeMessage message = null;
        BodyPart messageBodyPart = new MimeBodyPart();
        Multipart multipart = new MimeMultipart("mixed");
        try {
            log.debug("Creating new message");
            if (type.equals("COMPLEX")) {
                int mainMessageIndex = getMainMessage(html);
                if (mainMessageIndex == -1) {
                    message = CreateMainMessage(originalEmail, "", acomaid, "SIMPLE", multipart);
                    for (int i = 0; i < html.length; i++) {
                        messageBodyPart = AddAttachment(html[i].data, "text/html", html[i].filename);
                        multipart.addBodyPart(messageBodyPart);
                    }
                } else {
                    if (html.length == 0) message = CreateMainMessage(originalEmail, "", acomaid, "SIMPLE", multipart); else {
                        log.debug("Creating complex message");
                        message = CreateMainMessage(originalEmail, html[mainMessageIndex].data, acomaid, type, multipart);
                        messageBodyPart = AddAttachment(originalEmail, "message/rfc822", "originalemail.eml");
                        multipart.addBodyPart(messageBodyPart);
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        multipart.writeTo(os);
                    }
                }
            } else message = CreateMainMessage(originalEmail, "", acomaid, type, multipart);
            if (type.equals("SIMPLE") || type.equals("TEXT") || type.equals("COMPLEX")) {
                log.debug("Creating simple message");
                messageBodyPart = AddAttachment(link, "text/plain", "link.txt");
                multipart.addBodyPart(messageBodyPart);
            }
            if ((type.equals("TEXT") || type.equals("COMPLEX")) && notes.length() != 0) {
                log.debug("Creating text message");
                messageBodyPart = AddAttachment(notes, "text/plain", NOTES_TXT);
                multipart.addBodyPart(messageBodyPart);
            }
            if (type.equals("HTML")) {
                log.debug("Creating HTML message");
            }
            message.setContent(multipart);
            message.saveChanges();
            log.debug("Message prepared");
        } catch (MessagingException e) {
            log.error(e);
            e.printStackTrace();
        } catch (IOException e) {
            log.error(e);
            e.printStackTrace();
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            message.writeTo(os);
            return os.toByteArray();
        } catch (Exception e) {
            log.error(e);
            e.printStackTrace();
            return new byte[0];
        }
    }

    /**
	 * Method responsible for checking if inside the annotated part is also body of the email message
	 * @param html object of the annotated part
	 * @return The position of the body message in the html object. In case of that there is no annotated body inside the html object, it returns -1.
	 */
    private int getMainMessage(HtmlData[] html) {
        int mainMessage = -1;
        File bodyFile = new File(body);
        for (int i = 0; i < html.length; i++) {
            File testFile = new File(html[i].filename);
            if (bodyFile.getName().equalsIgnoreCase(testFile.getName())) {
                mainMessage = i;
                break;
            }
        }
        return mainMessage;
    }

    /**
	 * Method responsible for creating default email message with added link to the Message Post Processing GUI. It is called in case of errors!!!
	 * @param originalEmail represents the text of the original email
	 * @param acomaid is ID of the email added by acoma framework
	 * @param link is the link into the Message Post Processing GUI
	 * @return Return the text of the new email in the raw format.
	 * @throws MessagingException
	 * @throws IOException
	 */
    public StringBuffer CreateDefaultMessage(String originalEmail, String acomaid, String link) throws MessagingException, IOException {
        log.debug("Creating default message: Original Email + Link to the GUI");
        MimeMessage message = null;
        BodyPart messageBodyPart = new MimeBodyPart();
        Multipart multipart = new MimeMultipart("mixed");
        message = CreateMainMessage(originalEmail, "", acomaid, "SIMPLE", multipart);
        log.debug("Creating simple message");
        messageBodyPart = AddAttachment(link, "text/plain", "link.txt");
        multipart.addBodyPart(messageBodyPart);
        message.setContent(multipart);
        message.saveChanges();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            message.writeTo(os);
            return new StringBuffer(os.toString());
        } catch (Exception e) {
            log.error(e);
            e.printStackTrace();
            return new StringBuffer();
        }
    }

    /**
	 * Method responsible for creating default email message with added link to the Message Post Processing GUI. It is called in case of errors!!!
	 * @param originalEmail represents the byte array of the original email
	 * @param acomaid is ID of the email added by acoma framework
	 * @param link is the link into the Message Post Processing GUI
	 * @return Return the text of the new email in the raw format.
	 * @throws MessagingException
	 * @throws IOException
	 */
    public byte[] CreateDefaultMessage(byte[] originalEmail, String acomaid, String link) throws MessagingException, IOException {
        log.debug("Creating default message: Original Email + Link to the GUI");
        MimeMessage message = null;
        BodyPart messageBodyPart = new MimeBodyPart();
        Multipart multipart = new MimeMultipart("mixed");
        message = CreateMainMessage(originalEmail, "", acomaid, "SIMPLE", multipart);
        log.debug("Creating simple message");
        messageBodyPart = AddAttachment(link, "text/plain", "link.txt");
        multipart.addBodyPart(messageBodyPart);
        message.setContent(multipart);
        message.saveChanges();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            message.writeTo(os);
            return os.toByteArray();
        } catch (Exception e) {
            log.error(e);
            e.printStackTrace();
            return new byte[0];
        }
    }

    /**
	 * Method responsible for creating body of the new email. It can be only the original email, if there is no annotated body of the original email message.
	 * The second type is already enriched email with annotated part.
	 * @param originalEmail is the text of the original email in raw format
	 * @param addedEmail is the part that will be added to the original email.
	 * @param acomaid is ID of the email added by acoma framework
	 * @param type can be SIMPLE or COMPLEX, which represents if we will build only original email or also we will switch from original email to the addedEmail
	 * @param multipart is the Multipart JavaMail object created before and used to add multi-part objects inside the email
	 * @return Returns the raw text of the new enriched email.
	 * @throws MessagingException
	 * @throws IOException
	 */
    MimeMessage CreateMainMessage(String originalEmail, String addedEmail, String acomaid, String type, Multipart multipart) throws MessagingException, IOException {
        MimeMessage message = null;
        log.debug("Creating main body of the email message");
        if (type.equals("COMPLEX")) {
            Properties props = System.getProperties();
            Session mailSession = Session.getDefaultInstance(props, null);
            InputStream source = new ByteArrayInputStream(originalEmail.getBytes());
            message = new MimeMessage(mailSession, source);
            String newEmail = "";
            InternetHeaders hdr = new InternetHeaders();
            String header = "";
            Enumeration headers = message.getAllHeaderLines();
            while (headers.hasMoreElements()) {
                header = headers.nextElement().toString();
                String[] keyvalue = header.split(":");
                if (keyvalue[0].toUpperCase().equals("CONTENT-TYPE")) newEmail = newEmail + keyvalue[0] + ": text/html" + "\n"; else newEmail = newEmail + header + "\n";
            }
            newEmail = newEmail + "\n" + addedEmail;
            source = new ByteArrayInputStream(newEmail.getBytes());
            message = new MimeMessage(mailSession, source);
            message.addHeader("Acoma-ID", acomaid);
            message.addHeader("Content-Type", "text/html");
            message.addHeader("Content-Transfer-Encoding", "8bit");
            message.saveChanges();
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(message.getContent(), message.getContentType());
            messageBodyPart.setHeader("Content-Type", "text/html");
            messageBodyPart.addHeader("Content-Transfer-Encoding", "8bit");
            multipart.addBodyPart(messageBodyPart);
        } else {
            Properties props = System.getProperties();
            Session mailSession = Session.getDefaultInstance(props, null);
            InputStream source = new ByteArrayInputStream(originalEmail.getBytes());
            message = new MimeMessage(mailSession, source);
            message.addHeader("Acoma-ID", acomaid);
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(message.getContent(), message.getContentType());
            multipart.addBodyPart(messageBodyPart);
        }
        return message;
    }

    /**
	 * Method responsible for creating body of the new email. It can be only the original email, if there is no annotated body of the original email message.
	 * The second type is already enriched email with annotated part.
	 * @param originalEmail is the byte stream of the original email in raw format
	 * @param addedEmail is the part that will be added to the original email.
	 * @param acomaid is ID of the email added by acoma framework
	 * @param type can be SIMPLE or COMPLEX, which represents if we will build only original email or also we will switch from original email to the addedEmail
	 * @param multipart is the Multipart JavaMail object created before and used to add multi-part objects inside the email
	 * @return Returns the raw text of the new enriched email.
	 * @throws MessagingException
	 * @throws IOException
	 */
    private MimeMessage CreateMainMessage(byte[] originalEmail, String addedEmail, String acomaid, String type, Multipart multipart) throws MessagingException, IOException {
        MimeMessage message = null;
        log.debug("Creating main body of the email message");
        if (type.equals("COMPLEX")) {
            Properties props = System.getProperties();
            Session mailSession = Session.getDefaultInstance(props, null);
            InputStream source = new ByteArrayInputStream(originalEmail);
            message = new MimeMessage(mailSession, source);
            InternetHeaders hdr = new InternetHeaders();
            String header = "";
            Enumeration headers = message.getAllHeaderLines();
            while (headers.hasMoreElements()) {
                header = headers.nextElement().toString();
                String[] keyvalue = header.split(":");
                if (!keyvalue[0].toUpperCase().equals("CONTENT-TYPE")) {
                }
            }
            message.setContent(addedEmail, "text/html");
            hdr.addHeader("Content-Type", "text/html; charset=UTF-8");
            BodyPart messageBodyPart = new MimeBodyPart(hdr, addedEmail.getBytes("UTF-8"));
            multipart.addBodyPart(messageBodyPart);
        } else {
            Properties props = System.getProperties();
            Session mailSession = Session.getDefaultInstance(props, null);
            InputStream source = new ByteArrayInputStream(originalEmail);
            message = new MimeMessage(mailSession, source);
            message.addHeader("Acoma-ID", acomaid);
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(message.getContent(), message.getContentType());
            multipart.addBodyPart(messageBodyPart);
        }
        return message;
    }

    /**
	 * Method for adding mail part to the email as an inline attachment. 
	 * @param text is the raw text of added part
	 * @param type is the type of added part (text/plain, text/html, message/rfc822, ...)
	 * @param filename is file name of the attachment (it is not used in many email clients - they leave it empty)
	 * @return It returns the MimeBodyPart JavaMail object which can be added to the multipart message
	 * @throws MessagingException 
	 * @throws UnsupportedEncodingException 
	 * @throws UnsupportedEncodingException 
	 */
    private BodyPart AddAttachment(String text, String type, String filename) throws MessagingException, UnsupportedEncodingException {
        InternetHeaders hdr = new InternetHeaders();
        filename = filename.substring(filename.lastIndexOf(File.separator) + 1);
        if (type.contains("html")) filename = filename + ".html";
        hdr.addHeader("Content-Type", type + "; charset=UTF-8");
        hdr.addHeader("Content-Disposition", "inline;" + " filename=\"" + filename + "\"");
        return new MimeBodyPart(hdr, text.getBytes("UTF-8"));
    }

    /**
	 * Method for adding mail part to the email as an inline attachment. 
	 * @param text is the byte representation of the text added to the email message
	 * @param type is the type of added part (text/plain, text/html, message/rfc822, ...)
	 * @param filename is file name of the attachment (it is not used in many email clients - they leave it empty)
	 * @return It returns the MimeBodyPart JavaMail object which can be added to the multipart message
	 * @throws MessagingException
	 * @throws UnsupportedEncodingException
	 */
    private BodyPart AddAttachment(byte[] text, String type, String filename) throws MessagingException, UnsupportedEncodingException {
        InternetHeaders hdr = new InternetHeaders();
        filename = filename.substring(filename.lastIndexOf(File.separator) + 1);
        if (type.contains("html")) filename = filename + ".html";
        hdr.addHeader("Content-Type", type + "; charset=UTF-8");
        hdr.addHeader("Content-Transfer-Encoding", "8bit");
        hdr.addHeader("Content-Disposition", "inline;" + " filename=\"" + filename + "\"");
        return new MimeBodyPart(hdr, text);
    }

    /**
	 * Method responsible for message decomposition and saving individual email parts. It is called recursively, so every part is saved.
	 * @param part is the email message part (JavaMail object Part)
	 * @param path Directory for attachments saving 
	 * @throws MessagingException 
	 * @throws IOException this is typically thrown by the DataHandler. Refer to the documentation for javax.activation.DataHandler for more details.
	 */
    public void EmailPart(Part part, String path) throws IOException, MessagingException {
        Object object = part.getContent();
        if (object instanceof String) {
            log.debug("Instance of a string");
            log.debug("Type of part: " + part.getContentType());
            log.debug("Disposition: " + part.getDisposition());
            log.debug("Description: " + part.getDescription());
            log.debug("FileName: " + part.getFileName());
            log.debug("Content :\n" + part.getContent());
            File tmpFile;
            String extension = "";
            if (part.getFileName() == null) {
                extension = part.getContentType().substring(part.getContentType().indexOf("/") + 1);
                if (extension.indexOf(";") != -1) {
                    extension = extension.substring(0, extension.indexOf(";"));
                }
                if (extension.equalsIgnoreCase("plain")) extension = "txt";
                tmpFile = new File(path + "/" + attachmentCount + "." + extension);
                if (first) {
                    body = path + "/" + attachmentCount + "." + extension;
                    first = false;
                }
            } else {
                if (part.getFileName().indexOf(".") == -1) {
                    extension = "txt";
                    tmpFile = new File(path + "/" + part.getFileName() + "." + extension);
                    if (first) {
                        body = path + "/" + part.getFileName() + "." + extension;
                        first = false;
                    }
                } else {
                    extension = part.getFileName().substring(part.getFileName().lastIndexOf(".") + 1);
                    if (extension.equalsIgnoreCase("plain")) extension = "txt";
                    tmpFile = new File(path + "/" + part.getFileName().substring(0, part.getFileName().lastIndexOf(".") + 1) + extension);
                    if (first) {
                        body = path + "/" + part.getFileName().substring(0, part.getFileName().lastIndexOf(".") + 1) + extension;
                        first = false;
                    }
                }
            }
            ReadWriteTextFile.setContents(new File(tmpFile.getAbsolutePath()), part.getContent().toString(), true);
            xml += attachmentprocessing.runScript(tmpFile.getAbsolutePath(), extension);
            attachmentCount++;
        } else if (object instanceof Multipart) {
            log.debug("-------------");
            log.debug("Instance of a multipart");
            log.debug("Type of part: " + part.getContentType());
            log.debug("Disposition: " + part.getDisposition());
            log.debug("Description: " + part.getDescription());
            log.debug("FileName: " + part.getFileName());
            log.debug("Content:\n" + part.getContent());
            log.debug("-------------");
            Multipart mp = (Multipart) object;
            int count = mp.getCount();
            for (int i = 0; i < count; i++) {
                EmailPart(mp.getBodyPart(i), path);
            }
        } else if (object instanceof InputStream) {
            log.debug("-------------");
            log.debug("Instance of a inputstream");
            log.debug("Type of part: " + part.getContentType());
            log.debug("Disposition: " + part.getDisposition());
            log.debug("Description: " + part.getDescription());
            log.debug("FileName: " + part.getFileName());
            log.debug("Content:\n" + part.getContent());
            log.debug("-------------");
            InputStream is = (InputStream) object;
            File tmpFile;
            String extension = "";
            if (part.getFileName() == null) {
                extension = part.getContentType().substring(part.getContentType().indexOf("/") + 1);
                if (extension.indexOf(";") != -1) {
                    extension = extension.substring(0, extension.indexOf(";"));
                }
                if (extension.equalsIgnoreCase("plain")) extension = "txt";
                tmpFile = new File(path + "/" + attachmentCount + "." + extension);
                if (first) {
                    body = path + "/" + attachmentCount + "." + extension;
                    first = false;
                }
            } else {
                if (part.getFileName().indexOf(".") == -1) {
                    extension = "txt";
                    tmpFile = new File(path + "/" + part.getFileName() + "." + extension);
                } else {
                    extension = part.getFileName().substring(part.getFileName().lastIndexOf(".") + 1);
                    if (extension.equalsIgnoreCase("plain")) extension = "txt";
                    tmpFile = new File(path + "/" + part.getFileName().substring(0, part.getFileName().lastIndexOf(".") + 1) + extension);
                    if (first) {
                        body = path + "/" + part.getFileName().substring(0, part.getFileName().lastIndexOf(".") + 1) + extension;
                        first = false;
                    }
                }
            }
            int count;
            byte data[] = new byte[1000000];
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile), 1000000);
            while ((count = is.read(data, 0, 1000000)) != -1) out.write(data, 0, count);
            out.flush();
            out.close();
            xml += attachmentprocessing.runScript(tmpFile.getAbsolutePath(), extension);
            attachmentCount++;
        } else if (object instanceof Message) {
            log.debug("-------------");
            log.debug("Instance of a message");
            log.debug("Type of part: " + part.getContentType());
            log.debug("Disposition: " + part.getDisposition());
            log.debug("Description: " + part.getDescription());
            log.debug("FileName: " + part.getFileName());
            log.debug("Content:\n" + part.getContent());
            log.debug("-------------");
            Message message = (Message) object;
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                message.writeTo(os);
                ReadWriteTextFile.setContents(new File(path + "/" + part.getFileName()), os.toString(), true);
            } catch (Exception e) {
                log.error(e);
                e.printStackTrace();
            }
            EmailPart((Message) object, path);
        }
    }

    /**
	 * Method responsible for retrieving the Message-ID value from email header
	 * @param email is the text of the email message in raw email format
	 * @return The Message-ID value. It will be later replaced by the acoma ID
	 * @throws MessagingException
	 */
    public static String GetMessageID(String email) throws MessagingException {
        MimeMessage tmpMessage = null;
        Properties tmpProps = System.getProperties();
        Session mailSession = Session.getDefaultInstance(tmpProps, null);
        InputStream tmpSource = new ByteArrayInputStream(email.getBytes());
        tmpMessage = new MimeMessage(mailSession, tmpSource);
        return tmpMessage.getMessageID();
    }

    /**
	 * Reads the email message from file and saves the email and decomposed email parts to the acoma repository
	 * @param femail is File object of the email
	 * @param useacomaid represents the way, which we will generate acoma ID. If useacomaid is true we will use message id from email as acoma ID. 
	 * If false we will use the name of the email as acomaid
	 * @throws MessagingException 
	 * @throws IOException this is typically thrown by the DataHandler. Refer to the documentation for javax.activation.DataHandler for more details.
	 */
    public void MessageDecomposition(File femail, boolean useacomaid) throws MessagingException, IOException {
        String email = ReadWriteTextFile.getContents(femail);
        String dir = femail.getName();
        dir = (dir.lastIndexOf(".") == -1) ? dir : dir.substring(0, dir.lastIndexOf("."));
        if (useacomaid) MessageDecomposition(email); else MessageDecomposition(email, dir + File.separator, femail.getAbsolutePath().substring(0, femail.getAbsolutePath().lastIndexOf(File.separator) + 1));
    }

    /**
	 * Saves the email and decomposed email parts to the acoma repository
	 * @param email is Raw text of the email
	 * @throws MessagingException
	 * @throws MessagingException 
	 * @throws IOException this is typically thrown by the DataHandler. Refer to the documentation for javax.activation.DataHandler for more details.
	 */
    public void MessageDecomposition(String email) throws MessagingException, IOException {
        String acomaid = GetMessageID(email);
        String emailDIR = acomaid.substring(acomaid.indexOf("@") + 1, acomaid.indexOf(">"));
        String emailID = acomaid.substring("<".length(), acomaid.indexOf("@"));
        acomaid = emailDIR + File.separator + emailID;
        MessageDecomposition(email, acomaid, MultiServer.configAcoma.EMAIL_DIR);
    }

    /**
	 * Saves the email and decomposed email parts to the acoma repository
	 * @param email is Raw text of the email
	 * @param acomaid is the ID of the email
	 * @param path is the path to the acoma repository
	 * @throws MessagingException 
	 * @throws IOException this is typically thrown by the DataHandler. Refer to the documentation for javax.activation.DataHandler for more details.
	 */
    public void MessageDecomposition(String email, String acomaid, String path) throws MessagingException, IOException {
        String emailDIR = acomaid.substring(0, acomaid.indexOf(File.separator));
        String emailID = acomaid.substring(acomaid.indexOf(File.separator) + 1, acomaid.length());
        (new File(path + emailDIR + File.separator + emailID + File.separator + ATTACHMENTS)).mkdirs();
        ReadWriteTextFile.setContents(new File(path + emailDIR + File.separator + emailID + File.separator + EMAIL_EML), email, true);
        MimeMessage tmpMessage = null;
        Properties tmpProps = System.getProperties();
        Session mailSession = Session.getDefaultInstance(tmpProps, null);
        InputStream tmpSource = new ByteArrayInputStream(email.getBytes());
        tmpMessage = new MimeMessage(mailSession, tmpSource);
        tmpMessage.addHeader("Acoma-ID", acomaid);
        SaveHeaders(tmpMessage.getAllHeaderLines(), path + emailDIR + File.separator + emailID + File.separator + HEADER);
        first = true;
        EmailPart(tmpMessage, path + emailDIR + File.separator + emailID + File.separator + ATTACHMENTS);
    }

    /**
	 * 
	 * Saves the email and decomposed email parts to the acoma repository
	 * @param email is bytearray of the email
	 * @param acomaid is the ID of the email
	 * @param path is the path to the acoma repository
	 * @throws MessagingException 
	 * @throws IOException 
	 */
    public void MessageDecomposition(byte[] email, String acomaid, String path) throws MessagingException, IOException {
        String emailDIR = acomaid.substring(0, acomaid.indexOf(File.separator));
        String emailID = acomaid.substring(acomaid.indexOf(File.separator) + 1, acomaid.length());
        (new File(path + emailDIR + File.separator + emailID + File.separator + ATTACHMENTS)).mkdirs();
        MimeMessage tmpMessage = null;
        Properties tmpProps = System.getProperties();
        Session mailSession = Session.getDefaultInstance(tmpProps, null);
        InputStream tmpSource = new ByteArrayInputStream(email);
        tmpMessage = new MimeMessage(mailSession, tmpSource);
        SaveHeaders(tmpMessage.getAllHeaderLines(), path + emailDIR + File.separator + emailID + File.separator + HEADER);
        tmpMessage.addHeader("Acoma-ID", acomaid);
        ReadWriteTextFile.setContents(new File(path + emailDIR + File.separator + emailID + File.separator + EMAIL_EML), email, true);
        first = true;
        CreateMailMetadataStoreXML(tmpMessage, path + emailDIR + File.separator + emailID);
        EmailPart(tmpMessage, path + emailDIR + File.separator + emailID + File.separator + ATTACHMENTS);
        xml += " </Attachments>\n" + "</Email>";
        MailMetadataStoreService mailMetadataStoreService = (MailMetadataStoreService) MultiServer.felixCore.mailMetadataStoreService.getService();
        try {
            if (mailMetadataStoreService != null) mailMetadataStoreService.setMetadata(MailMetadataStoreService.SYSTEM_LAYER, "MAIL_" + tmpMessage.getMessageID(), xml);
        } catch (MailMetadataStoreException e) {
            log.error(e);
        }
    }

    private void CreateMailMetadataStoreXML(MimeMessage message, String path) throws MessagingException, UnsupportedEncodingException {
        String dateText = "";
        Date date = message.getSentDate();
        if (date != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateText = formatter.format(date);
        }
        xml = "<Email id=\"" + StringEscapeUtils.escapeXml(message.getMessageID()) + "\">\n" + "  <Message-ID>" + StringEscapeUtils.escapeXml(message.getMessageID()) + "</Message-ID>\n";
        Address[] from;
        if ((from = message.getFrom()) != null) for (int i = 0; i < from.length; i++) xml += "  <From>" + StringEscapeUtils.escapeXml(MimeUtility.decodeText(from[i].toString())) + "</From>\n";
        Address[] to;
        if ((to = message.getRecipients(Message.RecipientType.TO)) != null) for (int i = 0; i < to.length; i++) xml += "  <To>" + StringEscapeUtils.escapeXml(MimeUtility.decodeText(to[i].toString())) + "</To>\n";
        Address[] cc;
        if ((cc = message.getRecipients(Message.RecipientType.CC)) != null) for (int i = 0; i < cc.length; i++) xml += "  <Cc>" + StringEscapeUtils.escapeXml(MimeUtility.decodeText(cc[i].toString())) + "</Cc>\n";
        Address[] bcc;
        if ((bcc = message.getRecipients(Message.RecipientType.BCC)) != null) for (int i = 0; i < bcc.length; i++) xml += "  <Bcc>" + StringEscapeUtils.escapeXml(MimeUtility.decodeText(bcc[i].toString())) + "</Bcc>\n";
        Address[] Rto;
        if ((Rto = message.getReplyTo()) != null) for (int i = 0; i < Rto.length; i++) xml += "  <Reply-To>" + StringEscapeUtils.escapeXml(MimeUtility.decodeText(Rto[i].toString())) + "</Reply-To>\n";
        xml += "  <Subject>" + StringEscapeUtils.escapeXml(MimeUtility.decodeText(message.getSubject())) + "</Subject>\n";
        xml += "  <Path>" + path + "</Path>\n";
        xml += "  <User-ID>" + "nobody" + "</User-ID>\n";
        xml += "  <Date>" + StringEscapeUtils.escapeXml(dateText) + "</Date>\n";
        xml += "  <Attachments>\n";
    }

    /**
	 * Method responsible for saving email header to the acoma repository
	 * @param headers is an email header represented as key/value pairs 
	 * @param filename is the name of the stored file
	 * @throws IOException
	 */
    private void SaveHeaders(Enumeration headers, String filename) throws IOException {
        String header = "";
        while (headers.hasMoreElements()) header = header + headers.nextElement().toString() + "\n";
        header = header.substring(0, header.lastIndexOf("\n"));
        ReadWriteTextFile.setContents(new File(filename), header, true);
    }

    /**
	 * This method will annotate and prepare an email message with added notes, links and other stuff added by executed modules
	 * @param email is a byte array of an email message
	 * @param acomaid is an ID of the email message
	 * @param link is a link to the "local" Jetty web container
	 * @param path to the acoma repository
	 * @param module is the name of module which will be used to process this email message, it can be null and then we will execute 
	 * all modules running in the Apache Felix container
	 * @return Prepared text of email with attached knowledge. The text of email is in raw format.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public byte[] PrepareEmail(byte[] email, String acomaid, String link, String path, Map<String, String> module) throws FileNotFoundException, IOException {
        /**
		 * 
		 * @deprecated
		 */
        class DataNotes {

            private String filename = "";

            private String notes = "";

            DataNotes() {
                filename = "";
                notes = "";
            }
        }
        Set<ModuleResult> setNotes = RunModules(path, acomaid, module);
        String notes = "";
        String notesFile = "";
        Iterator<ModuleResult> itnotes = setNotes.iterator();
        String filename = "MPP";
        while (itnotes.hasNext()) {
            ModuleResult moduleresult = itnotes.next();
            if (!(moduleresult == null) && moduleresult.getTextName().contains("0.txt")) filename = new String(body);
            notes = notes + moduleresult.toText() + "\n";
            notesFile = notesFile + moduleresult.toString() + "\n";
        }
        byte[] tmpEmail = null;
        log.debug("Received notes from modules:\n" + notes);
        String tmpNotes = notesFile.replaceAll(" ", "");
        if (!tmpNotes.equalsIgnoreCase("")) {
            ReadWriteTextFile.setContents(new File(path + NOTES_TXT), notesFile, true);
            ReadWriteTextFile.setContents(new File(path + NOTES_XML), XMLResult.noteToXML(setNotes, acomaid), true);
            log.debug("Notes saved to the repository");
        } else log.debug("No notes saved to the repository, notes are empty!!");
        if (MultiServer.configAcoma.EMAIL_GUI.equals("COMPLEX")) {
            HtmlData[] html = null;
            if (!tmpNotes.equalsIgnoreCase("")) {
                html = new HtmlData[1];
                InputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(path + "notes.xml");
                } catch (Exception e) {
                    System.err.println("File input error");
                }
                html[0] = new HtmlData();
                html[0].data = EmailBuilder.getEmailAsHTML(inputStream, EmailBuilder.SHOW_ALL | EmailBuilder.SORT_BY_OCCURRENCE);
                html[0].filename = filename;
                log.debug("HTML code by MPP:" + html[0].data + "\n");
            } else html = new HtmlData[0];
            tmpEmail = AddNotes(email, html, acomaid, link, notes, "COMPLEX");
        } else tmpEmail = AddNotes(email, new HtmlData[0], acomaid, link, notes, MultiServer.configAcoma.EMAIL_GUI);
        return tmpEmail;
    }

    /**
	 * This method will annotate and prepare an email message with added notes, links and other stuff added by executed modules
	 * @param email is a raw text of an email message
	 * @param acomaid is an ID of the email message
	 * @param link is a link to the "local" Jetty web container
	 * @param path to the acoma repository
	 * @param module is the name of module which will be used to process this email message, it can be null and then we will execute 
	 * all modules running in the Apache Felix container
	 * @return Prepared text of email with attached knowledge. The text of email is in raw format.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public StringBuffer PrepareEmail(String email, String acomaid, String link, String path, Map<String, String> module) throws FileNotFoundException, IOException {
        /**
		 * 
		 * @deprecated
		 */
        class DataNotes {

            private String filename = "";

            private String notes = "";

            DataNotes() {
                filename = "";
                notes = "";
            }
        }
        Set<ModuleResult> setNotes = RunModules(path, acomaid, module);
        String notes = "";
        String notesFile = "";
        Iterator<ModuleResult> itnotes = setNotes.iterator();
        String filename = "MPP";
        while (itnotes.hasNext()) {
            ModuleResult moduleresult = itnotes.next();
            if (!(moduleresult == null) && moduleresult.getTextName().contains("0.txt")) filename = new String(body);
            notes = notes + moduleresult.toText() + "\n";
            notesFile = notesFile + moduleresult.toString() + "\n";
        }
        StringBuffer tmpEmail = new StringBuffer();
        log.debug("Received notes from modules:\n" + notes);
        String tmpNotes = notesFile.replaceAll(" ", "");
        if (!tmpNotes.equalsIgnoreCase("")) {
            ReadWriteTextFile.setContents(new File(path + NOTES_TXT), notesFile, true);
            ReadWriteTextFile.setContents(new File(path + NOTES_XML), XMLResult.noteToXML(setNotes, acomaid), true);
            log.debug("Notes saved to the repository");
        } else log.debug("No notes saved to the repository, notes are empty!!");
        if (MultiServer.configAcoma.EMAIL_GUI.equals("COMPLEX")) {
            HtmlData[] html = null;
            if (!tmpNotes.equalsIgnoreCase("")) {
                html = new HtmlData[1];
                InputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(path + "notes.xml");
                } catch (Exception e) {
                    System.err.println("File input error");
                }
                html[0] = new HtmlData();
                html[0].data = EmailBuilder.getEmailAsHTML(inputStream, EmailBuilder.SHOW_ALL | EmailBuilder.SORT_BY_OCCURRENCE);
                html[0].filename = filename;
                log.debug("HTML code by MPP:" + html[0].data + "\n");
            } else html = new HtmlData[0];
            tmpEmail = AddNotes(email, html, acomaid, link, notes, "COMPLEX");
        } else tmpEmail = AddNotes(email, new HtmlData[0], acomaid, link, notes, MultiServer.configAcoma.EMAIL_GUI);
        return tmpEmail;
    }

    /**
	 * Method responsible for executing individual modules
	 * @param path is the path to the acoma repository
	 * @param acomaid  is an ID of the email message
	 * @param module is the name of module which will be used to process this email message, it can be null and then we will execute 
	 * all modules running in the Apache Felix container
	 * @return outputs of individual modules integrated together
	 */
    private Set<ModuleResult> RunModules(String path, String acomaid, Map<String, String> module) {
        Set<ModuleResult> notes = new HashSet<ModuleResult>();
        log.debug("Trying to identifying and running modules");
        Object[] services = MultiServer.felixCore.tracker.getServices();
        if (services != null) log.debug("Number of services found: " + services.length); else log.warn("Number of services found: 0" + "\n" + "Please try to install some Commius modules!");
        for (int i = 0; (services != null) && (i < services.length); i++) {
            try {
                Set<ModuleResult> tmpnotes = null;
                if (module.size() == 0) {
                    log.info("Executing module: " + ((Module) services[i]).getName() + " with description: " + ((Module) services[i]).getDescription());
                    tmpnotes = ((Module) services[i]).execute(path, acomaid, MultiServer.configAcoma.ACOMA_URL, null);
                    log.debug("Notes received from the module: " + ((Module) services[i]).getName() + ":" + tmpnotes + "\n");
                } else {
                    String module_name = module.get("ModuleID");
                    String params = module.get("configID");
                    if (((Module) services[i]).getName().equalsIgnoreCase(module_name)) {
                        log.info("Executing module: " + ((Module) services[i]).getName() + " with description: " + ((Module) services[i]).getDescription() + " with config: " + params);
                        String[] parameters = new String[1];
                        parameters[0] = params;
                        tmpnotes = ((Module) services[i]).execute(path, acomaid, MultiServer.configAcoma.ACOMA_URL, parameters);
                        log.debug("Notes received from the module: " + ((Module) services[i]).getName() + ":" + tmpnotes + "\n");
                    }
                }
                if (tmpnotes != null) notes.addAll(tmpnotes);
            } catch (Exception e) {
                e.printStackTrace();
                log.error(e);
            }
        }
        return notes;
    }
}

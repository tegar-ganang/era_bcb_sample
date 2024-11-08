package net.teqlo.components.standard.emailV0_1.support;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;
import net.teqlo.TeqloException;
import net.teqlo.runtime.RuntimeAttributes;
import net.teqlo.util.Loggers;
import net.teqlo.util.MailUtil;
import javax.mail.Authenticator;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import com.sun.mail.imap.IMAPFolder;

public class ImapSupport implements RuntimeAttributes.BindingListener {

    private String currentUID = "";

    private String currentPWD = "";

    private Store currentStore = null;

    private IMAPFolder currentFolder = null;

    /**
	 * Default no-arg constructor
	 *
	 */
    public ImapSupport() {
    }

    public void bound(RuntimeAttributes object) {
    }

    public void unbound(RuntimeAttributes object) {
        try {
            this.disconnect();
        } catch (Exception e) {
            Loggers.XML_RUNTIME.error("Error disconnecting IMAP", e);
        }
    }

    /**
	 * This method simply tidies up the object
	 * 
	 * @throws Exception
	 */
    public void disconnect() throws Exception {
    }

    /**
	 * This is the login method for the Email - IMAP
	 * @param userName
	 * @param password
	 * @param host
	 * @param port
	 * @param protocol
	 * @throws TeqloException
	 */
    public void authenticate(String userName, String password, String host, String port, String protocol) throws TeqloException {
        try {
            Properties props = new Properties();
            props.put("mail." + protocol + ".host", host);
            props.put("mail." + protocol + ".port", port);
            props.put("mail.store.protocol", protocol);
            props.put("mail.transport.protocol", protocol);
            Authenticator auth = null;
            props.put("mail.user", userName);
            props.put("mail.password", password);
            Session session = Session.getInstance(props, auth);
            Store store = session.getStore(protocol);
            store.connect(host, userName, password);
            this.currentStore = store;
        } catch (AuthenticationFailedException e) {
            throw new TeqloException(this, "Authentication", e, "Authentication failed. Please check username and password.");
        } catch (Exception e) {
            throw new TeqloException(this, "Authentication", e, "Error retrieving IMAP mail: %1$s.");
        }
        this.currentUID = userName;
        this.currentPWD = password;
    }

    /**
	 * This method retrieves all the messages for the given folder.
	 * @param emailFolder
	 * @return
	 * @throws TeqloException
	 */
    public Message[] getAllMessages(String emailFolder) throws TeqloException {
        try {
            Store store = this.getStore();
            if (store != null) {
                IMAPFolder folder = (IMAPFolder) store.getFolder(emailFolder);
                folder.open(Folder.READ_WRITE);
                FetchProfile profile = new FetchProfile();
                profile.add(UIDFolder.FetchProfileItem.UID);
                profile.add(FetchProfile.Item.ENVELOPE);
                Message[] messages = folder.getMessages();
                folder.fetch(messages, profile);
                this.currentFolder = folder;
                return messages;
            }
        } catch (MessagingException e) {
            throw new TeqloException(this, "FetchMail", e, "Failed to fetch emails from IMAP server. Please try again.");
        } catch (Exception e) {
            throw new TeqloException(this, "FetchMail", e, "Failed to fetch emails from IMAP server. Please try again.");
        }
        return null;
    }

    /**
	 * This method retrieves message object of the given uid of the message
	 * @param folderFqn - Should be a full name of the folder. eg. Inbox/test
	 * @param uid
	 * @return
	 * @throws TeqloException
	 */
    public Message getMessage(String folderFqn, String uid) throws TeqloException {
        try {
            Store store = this.getStore();
            if (store != null) {
                IMAPFolder folder = (IMAPFolder) store.getFolder(folderFqn);
                Message message = folder.getMessageByUID(Long.valueOf(uid));
                return message;
            }
        } catch (MessagingException e) {
            throw new TeqloException(this, "FetchMail", e, "Failed to fetch emails from IMAP server. Please try again.");
        } catch (Exception e) {
            throw new TeqloException(this, "FetchMail", e, "Failed to fetch emails from IMAP server. Please try again.");
        }
        return null;
    }

    /**
	 * This function moves the message from current folder to the specified new folder and
	 * updates the document.
	 * @param newFolderFqn
	 * @param folderFqn
	 * @param uid
	 * @throws TeqloException
	 */
    public void moveMessage(String newFolderFqn, String folderFqn, String uid) throws TeqloException {
        try {
            Store store = this.getStore();
            Message message = this.getMessage(folderFqn, uid);
            IMAPFolder newParentFolder = (IMAPFolder) store.getFolder(newFolderFqn);
            newParentFolder.open(Folder.READ_WRITE);
            Message[] messages = new Message[1];
            messages[0] = message;
            newParentFolder.addMessages(messages);
            message.setFlag(Flags.Flag.DELETED, true);
            if (message.isSet(Flags.Flag.DELETED)) {
                System.out.println("Message : Deleted: ");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
	 * This function removes the message from the specified folder and
	 * updates the email document.
	 * @param folderFqn
	 * @param uid
	 * @throws TeqloException
	 */
    public void removeMessage(String folderFqn, String uid) throws TeqloException {
        try {
            Message message = this.getMessage(folderFqn, uid);
            message.setFlag(Flags.Flag.DELETED, true);
            if (message.isSet(Flags.Flag.DELETED)) {
                System.out.println("Message : Deleted: ");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
	 * This function constructs xml(assuming) for the message body and its associated files.
	 * @param folderFqn
	 * @param uid
	 * @throws TeqloException
	 */
    public void getMessageBody(String folderFqn, String uid) throws TeqloException {
        try {
            Message message = this.getMessage(folderFqn, uid);
            String content = "";
            if (message.isMimeType("text/plain") || message.isMimeType("text/html")) {
                try {
                    content = (String) message.getContent();
                } catch (Exception e) {
                    content = "Could not retrieve content of message: " + e;
                }
            }
            if (message.isMimeType("multipart/alternative")) content = MailUtil.parseMailPart(message); else if (message.isMimeType("multipart/*")) {
                MimeMultipart multipart = (MimeMultipart) message.getContent();
                for (int k = 0; k < multipart.getCount(); k++) {
                    MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(k);
                    String disposition = part.getDisposition();
                    if (disposition == null) {
                        content = MailUtil.parseMailPart(part);
                    } else if (disposition.equalsIgnoreCase(Part.ATTACHMENT) || disposition.equalsIgnoreCase(Part.INLINE)) {
                        int fileSize = part.getSize() / 1024;
                        String fileName = part.getFileName();
                        if (fileSize == 0) fileSize = 1;
                        if (fileName == null || fileName.equals(null) || fileName.equals("")) {
                            content += "\n\n" + MailUtil.parseMailPart(part);
                        } else {
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
	 * This function sends buffer of the attachment to the browser.
	 * @param folderFqn
	 * @param uid
	 * @param attachmentName
	 * @throws TeqloException
	 */
    public void getAttachment(String folderFqn, String uid, String attachmentName) throws TeqloException {
        try {
            Message message = this.getMessage(folderFqn, uid);
            MimeMultipart multipart = (MimeMultipart) message.getContent();
            for (int k = 0; k < multipart.getCount(); k++) {
                MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(k);
                String disposition = part.getDisposition();
                if (disposition.equalsIgnoreCase(Part.ATTACHMENT) || disposition.equalsIgnoreCase(Part.INLINE)) {
                    String fileName = part.getFileName();
                    if (fileName.equals(attachmentName)) {
                        InputStream istream = part.getInputStream();
                        ByteArrayOutputStream bout = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4000];
                        int length;
                        while ((length = istream.read(buffer)) >= 0) bout.write(buffer, 0, length);
                        bout.flush();
                        buffer = bout.toByteArray();
                        bout.close();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
	 * This function creates a new folder under the specified parent folder  
	 * @param parentFolder - should be a valid full name of the folder.
	 * @param folderName
	 */
    public void createFolder(String parentFolderFqn, String folderName) throws TeqloException {
        try {
            Store store = this.getStore();
            if (store != null) {
                IMAPFolder folder = (IMAPFolder) store.getFolder(parentFolderFqn);
                IMAPFolder newFolder = (IMAPFolder) folder.getFolder(folderName);
                if (!newFolder.exists()) {
                    if (newFolder.create(Folder.HOLDS_MESSAGES)) System.out.println("Created.");
                }
            }
        } catch (MessagingException e) {
            throw new TeqloException(this, "FetchMail", e, "Failed to create folder " + folderName + " in the IMAP server. Please try again.");
        } catch (Exception e) {
            throw new TeqloException(this, "FetchMail", e, "Failed to create folder. Please try again.");
        }
    }

    /**
	 * This function removes the specified folder from the server  
	 * @param folderName
	 */
    public void removeFolder(String folderName) throws TeqloException {
        try {
            Store store = this.getStore();
            if (store != null) {
                IMAPFolder folder = (IMAPFolder) store.getFolder(folderName);
                if (folder.exists()) {
                    folder.delete(true);
                    if (!folder.exists()) {
                        System.out.println("Folder Deleted");
                    }
                }
            }
        } catch (MessagingException e) {
            throw new TeqloException(this, "FetchMail", e, "Failed to delete folder " + folderName + " in the IMAP server. Please try again.");
        } catch (Exception e) {
            throw new TeqloException(this, "FetchMail", e, "Failed to delete folder. Please try again.");
        }
    }

    /**
	 * This function moves the folder to the specified new parent location. As Javamail doesn't have
	 * any move folder API, this function creates a folder under new parent and copies all the messages
	 * from old location to the new location and deletes the old location folder.
	 * @param newParentFQN
	 * @param folderFQN
	 * @param folderName
	 * @throws TeqloException
	 */
    public void moveFolder(String newParentFqn, String folderFqn, String folderName) throws TeqloException {
        try {
            Store store = this.getStore();
            if (store != null) {
                IMAPFolder oldFolder = (IMAPFolder) store.getFolder(folderFqn);
                IMAPFolder newParentFolder = (IMAPFolder) store.getFolder(newParentFqn);
                if (newParentFolder.exists()) {
                    IMAPFolder newFolder = (IMAPFolder) newParentFolder.getFolder(folderName);
                    if (!newFolder.exists()) {
                        if (newFolder.create(Folder.HOLDS_MESSAGES)) {
                            System.out.println("New Folder Created.");
                            if (oldFolder.exists()) {
                                oldFolder.open(Folder.READ_WRITE);
                                oldFolder.copyMessages(oldFolder.getMessages(), newFolder);
                                oldFolder.close(true);
                                oldFolder.delete(true);
                            }
                        }
                    }
                }
            }
        } catch (MessagingException e) {
            throw new TeqloException(this, "FetchMail", e, "Failed to delete folder " + folderName + " in the IMAP server. Please try again.");
        } catch (Exception e) {
            throw new TeqloException(this, "FetchMail", e, "Failed to delete folder. Please try again.");
        }
    }

    /**
	 * 
	 * @param folder
	 * @param recurse
	 * @throws MessagingException
	 */
    private void syncFolders(Folder folder, boolean recurse) throws MessagingException {
        System.out.println("Folder " + folder.getFullName() + " type is " + folder.getType());
        if ((folder.getType() & Folder.HOLDS_FOLDERS) == 0) {
            System.out.println("Folder " + folder.getFullName() + " cannot contain subfolders.");
            return;
        }
        Folder[] listFolders = folder.list();
        System.out.println("Listing sub folders of " + folder.getFullName());
        for (int i = 0; i < listFolders.length; i++) {
            Folder subFolder = listFolders[i];
            System.out.println("Found folder " + subFolder.getFullName());
            String folderName = subFolder.getFullName();
            boolean folderExist = this.parseFolders(folderName);
            if (!folderExist) {
                System.out.println(" Create folder node called: " + folderName);
            }
            if (recurse) {
                syncFolders(subFolder, recurse);
            }
        }
    }

    /**
	 * This function checks if the folder is present in the email document.
	 * @param folderFullName
	 * @return
	 */
    @SuppressWarnings("unchecked")
    public boolean parseFolders(String folderFullName) {
        boolean folderExist = true;
        String emailDoc = "";
        try {
            SAXBuilder builder = new SAXBuilder();
            Document listDoc = builder.build(new StringReader(emailDoc));
            Element element = listDoc.getRootElement();
            for (Element folder : (List<Element>) element.getChildren("folder")) {
                String folderFqn = folder.getAttributeValue("folderFqn");
                if (!folderFqn.equalsIgnoreCase(folderFullName)) folderExist = false;
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return folderExist;
    }

    /**
	 * Getter for currentUID
	 */
    public String getCurrentUID() {
        return currentUID;
    }

    /**
     * Getter for currentPWD
     */
    public String getCurrentPWD() {
        return currentPWD;
    }

    /**
     * Getter for currentStore
     */
    public Store getStore() {
        return currentStore;
    }

    /**
	 * Getter for currentFolder
	 */
    public IMAPFolder getFolder() {
        return currentFolder;
    }

    @Override
    public String toString() {
        return "Teqlo IMAP Support Class";
    }
}

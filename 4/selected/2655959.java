package xlion.maildisk.mail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.search.SubjectTerm;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.log4j.Logger;
import xlion.maildisk.account.IAccount;
import xlion.maildisk.file.MailFile;
import xlion.maildisk.server.intf.ISmtpConnectionConfig;
import xlion.maildisk.util.Base64;
import xlion.maildisk.util.FilePathUtil;

public class MailFileHelper {

    /**
	 * Logger for this class
	 */
    private static final Logger logger = Logger.getLogger(MailFileHelper.class);

    private final MailFolder mailFolder;

    public MailFileHelper(final IAccount account) {
        super();
        mailFolder = new MailFolder(account);
    }

    public IAccount getAccount() {
        return mailFolder.getAccount();
    }

    public void close() {
        mailFolder.close();
    }

    public void open() throws IllegalStateException, MessagingException {
        mailFolder.open();
    }

    public void updateFolder() throws IllegalStateException, MessagingException {
        mailFolder.updateFolder();
    }

    public boolean isDirectory(String relativePath) throws MessagingException {
        MailFile mailFile = mailFolder.getFileRoot();
        MailFile targetFile = mailFile.getFile(relativePath);
        if (targetFile == null) {
            throw new MessagingException("Path:" + relativePath + " not exist.");
        } else {
            return targetFile.isDirectory();
        }
    }

    public void saveFolderToMail(String relativeFolder) {
        File file = new File(getAccount().getCenterPath() + "/" + relativeFolder);
        if (!file.getName().startsWith(".")) {
            if (file.isDirectory()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("saveFolderToMail(String) -handle directory[" + file.getAbsolutePath() + "]");
                }
                for (File sfile : file.listFiles()) {
                    saveFolderToMail(relativeFolder + "/" + sfile.getName());
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("saveFolderToMail(String) -handle file[" + file.getAbsolutePath() + "]");
                }
                try {
                    saveFileToMail(relativeFolder);
                } catch (MessagingException e) {
                    logger.error("saveFolderToMail(String) -handle file[" + file.getAbsolutePath() + "] fail", e);
                }
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("saveFolderToMail(String) -ignore file[" + file.getAbsolutePath() + "]");
            }
        }
    }

    public void getFolderFromMail(String relativeFolder) throws MessagingException {
        MailFile root = mailFolder.getFileRoot();
        MailFile file = root.getFile(relativeFolder);
        if (file.isDirectory()) {
            if (logger.isDebugEnabled()) {
                logger.debug("getFolderFromMail(String) -handle directory[" + file.getPath() + "]");
            }
            for (MailFile mFile : file.listFiles()) {
                getFolderFromMail(relativeFolder + "/" + mFile.getName());
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("getFolderFromMail(String) -handle file[" + file.getPath() + "]");
            }
            try {
                getFileFromMail(relativeFolder);
            } catch (IOException e) {
                logger.error("getFolderFromMail(String) -handle file[" + file.getPath() + "] fail", e);
            }
        }
    }

    public void saveFileToMail(String relativePath) throws MessagingException {
        MultiPartEmail email = new MultiPartEmail();
        ISmtpConnectionConfig config = getAccount().getServerConfig().getSmtpServerConfig();
        email.setHostName(config.getHostName());
        email.setSslSmtpPort(String.valueOf(config.getPort()));
        email.setTLS(config.isTLS());
        email.setCharset("GB18030");
        email.setAuthentication(getAccount().getLoginID(), getAccount().getPassword());
        relativePath = relativePath.replace("\\", "/");
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        try {
            email.addTo(getAccount().getEmailAddress(), getAccount().getLoginID());
            email.setFrom(getAccount().getEmailAddress(), "Me");
            email.setSubject(FilePathUtil.encodePath(relativePath));
            email.setMsg(relativePath + "\nCurrent Time: " + (new Date()));
            EmailAttachment attachment = new EmailAttachment();
            attachment.setPath(getAccount().getCenterPath() + "/" + relativePath);
            attachment.setDisposition(EmailAttachment.ATTACHMENT);
            attachment.setDescription(FilePathUtil.getFileName(relativePath));
            attachment.setName(Base64.encode(FilePathUtil.getFileName(relativePath)));
            email.attach(attachment);
            email.send();
        } catch (EmailException e) {
            logger.error("sendMail([" + relativePath + "]) fail.", e);
            e.printStackTrace();
            throw new MessagingException("sendMail(IFileInformation) fail.", e);
        }
    }

    public String[] list(String directoryPath) throws MessagingException {
        String[] list = new String[0];
        Folder inbox = mailFolder.getFolder();
        Message[] messages = inbox.search(new SubjectTerm(FilePathUtil.getSubfilePattern(directoryPath)));
        HashSet<String> filesSet = new HashSet<String>();
        for (Message msg : messages) {
            filesSet.add(FilePathUtil.decodePath(msg.getSubject()));
        }
        list = filesSet.toArray(new String[filesSet.size()]);
        if (logger.isDebugEnabled()) {
            logger.debug("list() - list=" + Arrays.toString(list));
        }
        return list;
    }

    public void getFileFromMail(String relativePath) throws MessagingException, IOException {
        Folder inbox = mailFolder.getFolder();
        Message[] messages = inbox.search(new SubjectTerm(FilePathUtil.getSelfPattern(relativePath)));
        if (messages == null || messages.length == 0) {
            throw new MessagingException("File[" + relativePath + "] not exist in server");
        } else {
            Arrays.sort(messages, new Comparator<Message>() {

                public int compare(Message o1, Message o2) {
                    try {
                        return o2.getSentDate().compareTo(o1.getSentDate());
                    } catch (MessagingException e) {
                        logger.warn("ignore error:-" + e.getMessage(), e);
                        return 0;
                    }
                }
            });
            saveAttach(messages[0], relativePath);
        }
    }

    private void saveAttach(Message msg, String filePath) throws IOException, MessagingException {
        if (!(msg.getContent() instanceof Multipart)) {
            throw new MessagingException("No attachment in Message" + msg);
        }
        Multipart mp = (Multipart) msg.getContent();
        int mpCount = mp.getCount();
        int attachNumber = 0;
        for (int m = 0; m < mpCount; m++) {
            BodyPart part = mp.getBodyPart(m);
            String disposition = part.getDisposition();
            if (disposition != null && disposition.equals(Part.ATTACHMENT)) {
                if (attachNumber != 0) {
                    throw new MessagingException("More than one attch found in Message" + msg);
                }
                attachNumber++;
                saveAttach(part, filePath);
            }
        }
    }

    private void saveAttach(BodyPart part, String filePath) throws MessagingException, IOException {
        String srcFileName = part.getFileName();
        String decodeFileName = Base64.decode(srcFileName);
        if (logger.isDebugEnabled()) {
            logger.debug("saveAttach(BodyPart) - decodeFileName=" + decodeFileName + ", srcFileName=" + srcFileName + ", filePath=" + filePath);
        }
        InputStream in = part.getInputStream();
        FileOutputStream writer = new FileOutputStream(new File(getAccount().getCenterPath() + "/" + filePath));
        byte[] content = new byte[255];
        int read = 0;
        while ((read = in.read(content)) != -1) {
            writer.write(content, 0, read);
        }
        writer.close();
        in.close();
    }
}

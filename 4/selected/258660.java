package com.xpresso.utils.email;

import java.io.*;
import java.util.ArrayList;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeUtility;
import org.apache.log4j.Logger;
import com.xpresso.utils.exceptions.XpressoException;

/**
 * This class will be responsible to process
 * email messages by saving each part (the body
 * included) and the attachments into different 
 * files on the path specified.
 */
public class EmailProcessor {

    Logger log = Logger.getLogger(getClass().getName());

    protected String filePath;

    protected String formatedMessageHeader;

    protected EmailConfiguration emailconf;

    /** 
	 * This is the main method of this class. This 
	 * method will enumerate the parts of the message
	 * and will create a file for the body and each 
	 * attachment. The file names and folders are
	 * based on the attNumber and approved att number 
	 * @param Message, attNumber, apprAttNumber
	 * @return Arraylist (files)
	 */
    public ProcessedMessage processMessage(Message message) throws XpressoException {
        ProcessedMessage pMessage = new ProcessedMessage();
        ArrayList<String> currentFileList = new ArrayList<String>();
        try {
            String messageId = getMessageId(message);
            String currentMessageFolder = this.emailconf.getAttachmentPath() + messageId + "/";
            (new File(currentMessageFolder)).mkdir();
            pMessage.setMessageHeader(getFormatedMessageHeader(message));
            pMessage.setFolderPath(currentMessageFolder);
            pMessage.setAttachments(currentFileList);
            pMessage.setTheMessage(message);
            pMessage.setMessageId(messageId);
            pMessage.setSentDate(message.getSentDate());
            pMessage.setFrom(message.getFrom()[0].toString());
            pMessage.setSubject(message.getSubject());
            pMessage.setBodyProcessed(false);
            pMessage.setRecipients(getMessageRecipients(message.getAllRecipients()));
            Object body = message.getContent();
            if (body instanceof Multipart) {
                processMultipart((Multipart) body, pMessage);
            } else {
                processPart(message, pMessage);
            }
        } catch (IOException e) {
            throw new XpressoException("An IOException occurred", e);
        } catch (MessagingException e) {
            throw new XpressoException("An MessagingException occurred", e);
        }
        pMessage.setDeleteFlag(this.handleProcessedMessage(pMessage));
        return pMessage;
    }

    protected String[] getMessageRecipients(Address[] allRecipients) {
        String[] addresses = new String[allRecipients.length];
        int i = 0;
        for (Address a : allRecipients) {
            addresses[i] = a.toString();
            if (addresses[i] != null) {
                addresses[i] = addresses[i].replaceAll("\"", "");
            }
            i++;
        }
        return addresses;
    }

    protected String getMessageId(Message message) throws MessagingException {
        return message.getFrom()[0].toString() + message.getSentDate().getTime();
    }

    public boolean handleProcessedMessage(ProcessedMessage message) throws XpressoException {
        return false;
    }

    /**
	 * This method creates the header of the file that 
	 * contains the body of the message:
	 * From: xxx
	 * Subject: xxx
	 * Sent: xxx
	 * @param message
	 * @return String
	 */
    protected String getFormatedMessageHeader(Message message) throws MessagingException {
        StringBuffer sb = new StringBuffer();
        sb.append("From: ");
        for (int i = 0; i < message.getFrom().length; i++) {
            sb.append(message.getFrom()[i] + "; ");
        }
        sb.append("\nSubject: " + message.getSubject());
        sb.append("\nSent: " + message.getSentDate());
        sb.append("\n\n");
        return sb.toString();
    }

    /**
	 * This method opens a multipart message and calls the
	 * processPart() method for each message part that it
	 * contains
	 * @param Multipart
	 */
    protected void processMultipart(Multipart mp, ProcessedMessage pMessage) throws MessagingException, IOException {
        for (int i = 0; i < mp.getCount(); i++) {
            processPart(mp.getBodyPart(i), pMessage);
        }
    }

    /**
	 * This method process a single part of an email 
	 * message creating a file for the part passed
	 * as parameter.
	 * @param Part
	 */
    protected void processPart(Part p, ProcessedMessage pMessage) throws MessagingException, IOException {
        String fileName = p.getFileName();
        log.debug("DBUG: The file name of the attachment is: " + fileName);
        if (fileName != null) {
            fileName = MimeUtility.decodeText(fileName);
        }
        String disposition = p.getDisposition();
        String contentType = p.getContentType();
        if (contentType.toLowerCase().startsWith("multipart/")) {
            processMultipart((Multipart) p.getContent(), pMessage);
        } else if (fileName == null && (Part.ATTACHMENT.equalsIgnoreCase(disposition) || !contentType.equalsIgnoreCase("text/plain"))) {
            if (contentType.startsWith("text/plain") && !pMessage.isBodyProcessed()) {
                fileName = File.createTempFile(pMessage.getMessageId(), ".txt").getName();
                writeMessageHeader(fileName, pMessage);
                savePart(p, fileName, true, pMessage);
                pMessage.setMessageBody(p.getContent().toString());
                pMessage.setBodyProcessed(true);
            } else if (contentType.startsWith("text/html") && !pMessage.isBodyProcessed()) {
                fileName = File.createTempFile(pMessage.getMessageId(), ".html").getName();
                savePart(p, fileName, false, pMessage);
                pMessage.setMessageBody(p.getContent().toString());
                pMessage.setBodyProcessed(true);
            } else {
                return;
            }
        } else {
            savePart(p, fileName, false, pMessage);
        }
    }

    /**
	 * This method writes the message header for the
	 * file that will store the body of the email message
	 * @param fileName
	 */
    private void writeMessageHeader(String fileName, ProcessedMessage pMessage) throws FileNotFoundException, IOException {
        File f = new File(pMessage.getFolderPath() + fileName);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(f));
        InputStream in = new ByteArrayInputStream(pMessage.getMessageHeader().getBytes());
        int b;
        while ((b = in.read()) != -1) out.write(b);
        out.flush();
        out.close();
        in.close();
    }

    /**
	 * This method saves the part to the file system with 
	 * the given file name.
	 * @param Part
	 * @param String fileName
	 * @param append - Set to true only for the body of the message
	 */
    private void savePart(Part p, String fileName, boolean append, ProcessedMessage pMessage) throws FileNotFoundException, IOException, MessagingException {
        if (fileName != null) {
            FileOutputStream appendedFile = new FileOutputStream(pMessage.getFolderPath() + fileName, append);
            InputStream in = new BufferedInputStream(p.getInputStream());
            int b;
            while ((b = in.read()) != -1) appendedFile.write(b);
            appendedFile.flush();
            appendedFile.close();
            in.close();
            pMessage.getAttachments().add(fileName);
        }
    }

    public EmailConfiguration getEmailconf() {
        return emailconf;
    }

    public void setEmailconf(EmailConfiguration emailconf) {
        this.emailconf = emailconf;
    }
}

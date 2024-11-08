package com.ericdaugherty.mail.server.persistence.pop3;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.ericdaugherty.mail.server.errors.TooManyErrorsException;
import com.ericdaugherty.mail.server.info.*;
import com.ericdaugherty.mail.server.persistence.LocalDeliveryFactory;
import com.ericdaugherty.mail.server.persistence.POP3MessagePersistenceProccessor;
import com.ericdaugherty.mail.server.services.general.StreamHandler;
import com.ericdaugherty.mail.server.utils.DelimitedInputStream;

/**
 * A file system based POP3 persistance engine.
 *
 * @author Andreas Kyrmegalos
 */
public class SimpleFileIOProcessor implements POP3MessagePersistenceProccessor {

    private static final String US_ASCII = "US-ASCII";

    /** Logger */
    private Log log = LogFactory.getLog(SimpleFileIOProcessor.class);

    private User user;

    private String userRepository;

    public SimpleFileIOProcessor() {
    }

    public void setUser(User user) {
        this.user = user;
        userRepository = LocalDeliveryFactory.getInstance().getLocalDeliveryProccessor().getUserRepository(user);
    }

    private String getUserRepository() {
        return userRepository;
    }

    public String[] populatePOP3MessageList() {
        final File directory = new File(getUserRepository());
        String[] messageNames = directory.list(new FilenameFilter() {

            public boolean accept(File directoryFile, String file) {
                if (directoryFile.equals(directory)) {
                    if (file.toLowerCase().endsWith(".loc")) {
                        return true;
                    }
                }
                return false;
            }
        });
        return messageNames;
    }

    public String[] deleteMessages() {
        Message[] messages = user.getMessages();
        int numMessage = messages.length;
        if (numMessage == 0) {
            return null;
        }
        File userDirectory = new File(getUserRepository());
        int attempts;
        String[] failedMessages = new String[numMessage];
        int failedMessageCount = 0;
        File aMessageFile;
        for (Message currentMessage : messages) {
            attempts = 0;
            if (currentMessage.isDeleted()) {
                aMessageFile = new File(userDirectory, currentMessage.getMessageLocation());
                while (attempts++ < 5) {
                    aMessageFile.delete();
                    if (aMessageFile.exists()) {
                        if (attempts == 5) {
                            failedMessages[failedMessageCount++] = currentMessage.getUniqueId();
                        }
                        try {
                            Thread.sleep(attempts * 5000);
                        } catch (InterruptedException ex) {
                            Thread.interrupted();
                        }
                    }
                }
            }
        }
        if (failedMessageCount == 0) {
            return null;
        }
        String[] returnFailedMessages = new String[failedMessageCount];
        System.arraycopy(failedMessages, 0, returnFailedMessages, 0, failedMessageCount);
        return returnFailedMessages;
    }

    public void retreiveMessage(StreamHandler pop3CH, int messageNumber) throws TooManyErrorsException, FileNotFoundException, IOException {
        DelimitedInputStream reader = null;
        List<byte[]> dataLines = new ArrayList<byte[]>(250);
        try {
            String messageLocation = user.getMessage(messageNumber).getMessageLocation();
            reader = new DelimitedInputStream(new FileInputStream(new File(getUserRepository(), messageLocation)));
            messageLocation = messageLocation.substring(messageLocation.lastIndexOf(File.separator) + 1, messageLocation.lastIndexOf('.'));
            boolean foundRPLCRCPT = false, foundRPLCID = false;
            String singleLine;
            byte[] currentLine = reader.readLine();
            while (currentLine != null) {
                dataLines.add(currentLine);
                singleLine = new String(currentLine, US_ASCII);
                if (singleLine.indexOf("<REPLACE-RCPT>") != -1) {
                    dataLines.set(dataLines.size() - 1, ("        for <" + user.getFullUsername() + ">" + singleLine.substring(singleLine.indexOf(';'))).getBytes(US_ASCII));
                    foundRPLCRCPT = true;
                } else if (singleLine.indexOf("<REPLACE-ID>") != -1) {
                    dataLines.set(dataLines.size() - 1, (singleLine.substring(0, singleLine.indexOf('<')) + messageLocation + (singleLine.charAt(singleLine.length() - 1) == ';' ? ";" : "")).getBytes(US_ASCII));
                    foundRPLCID = true;
                }
                currentLine = reader.readLine();
                if (currentLine.length == 0 || (foundRPLCRCPT && foundRPLCID)) break;
            }
            while (currentLine != null) {
                dataLines.add(currentLine);
                currentLine = reader.readLine();
                if (dataLines.size() == 250) {
                    for (byte[] readLine : dataLines) {
                        pop3CH.write(readLine);
                    }
                    dataLines.clear();
                }
            }
            int lineCount = dataLines.size();
            if (lineCount > 0) {
                for (byte[] readLine : dataLines) {
                    pop3CH.write(readLine);
                }
                dataLines.clear();
            }
            pop3CH.write(new byte[] { 0x2e });
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                }
                reader = null;
            }
            dataLines.clear();
            dataLines = null;
        }
    }

    public void retreiveMessageTop(StreamHandler pop3CH, int messageNumber, long numLines) throws TooManyErrorsException, FileNotFoundException, IOException {
        DelimitedInputStream reader = null;
        try {
            reader = new DelimitedInputStream(new FileInputStream(new File(getUserRepository(), user.getMessage(messageNumber).getMessageLocation())));
            byte[] currentLine = reader.readLine();
            while (currentLine != null && currentLine.length != 0) {
                pop3CH.write(currentLine);
                currentLine = reader.readLine();
            }
            pop3CH.write(currentLine);
            currentLine = reader.readLine();
            int index = 0;
            while (index < numLines && currentLine != null) {
                pop3CH.write(currentLine);
                currentLine = reader.readLine();
                index++;
            }
            pop3CH.write(new byte[] { 0x2e });
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                }
            }
        }
    }
}

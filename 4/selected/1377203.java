package com.ericdaugherty.mail.server.persistence.smtp;

import java.io.*;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.ericdaugherty.mail.server.services.smtp.*;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.errors.InvalidAddressException;
import com.ericdaugherty.mail.server.info.EmailAddress;
import com.ericdaugherty.mail.server.persistence.SMTPMessagePersistenceProccessor;
import com.ericdaugherty.mail.server.services.smtp.support.Utils;
import com.ericdaugherty.mail.server.utils.DelimitedInputStream;
import com.ericdaugherty.mail.server.utils.FileUtils;

/**
 * A file system based SMTP persistance processor.
 *
 * @author Andreas Kyrmegalos
 */
public final class IncrementalFileIOProccessor implements SMTPMessagePersistenceProccessor {

    private static final String US_ASCII = "US-ASCII";

    private static final byte[] EOL;

    static {
        byte[] line_separator = null;
        try {
            line_separator = System.getProperty("line.separator").getBytes(US_ASCII);
        } catch (UnsupportedEncodingException ex) {
            line_separator = System.getProperty("line.separator").getBytes();
        } finally {
            EOL = line_separator;
        }
    }

    /** Logger */
    private static final Log log = LogFactory.getLog(IncrementalFileIOProccessor.class);

    /** The ConfigurationManager */
    private final ConfigurationManager configurationManager = ConfigurationManager.getInstance();

    private SMTPMessage message;

    private File messageLocation;

    private final String characters = "fedcba9876543210";

    private final Random random = new Random();

    public IncrementalFileIOProccessor() {
    }

    public void setMessage(SMTPMessage message) {
        this.message = message;
    }

    public Object getPersistedID() {
        return messageLocation.getPath();
    }

    /**
    * @return the messageLocation
    */
    public File getMessageLocation() {
        return messageLocation;
    }

    /**
    * @param messageLocation the messageLocation to set
    */
    public void setMessageLocation(File messageLocation) {
        this.messageLocation = messageLocation;
    }

    public void initializeMessage(String filename, boolean headersOnly) throws IOException {
        if (message == null) throw new IOException("No message passed");
        File messageFile = new File(filename);
        DelimitedInputStream reader = new DelimitedInputStream(new FileInputStream(messageFile));
        try {
            String stringLine = new String(reader.readLine(), US_ASCII);
            stringLine = stringLine.substring(stringLine.indexOf(":") + 2);
            if (log.isDebugEnabled()) log.debug("Loading SMTP Message " + messageFile.getName() + " version " + stringLine);
            if (!Utils.FILE_VERSION.equals(stringLine)) {
                log.error("Error loading SMTP Message.  Can not handle file version: " + stringLine);
                throw new IOException("Invalid file version: " + stringLine);
            }
            setMessageLocation(messageFile);
            stringLine = new String(reader.readLine(), US_ASCII);
            stringLine = stringLine.substring(stringLine.indexOf(":") + 2);
            message.setSMTPUID(stringLine);
            String fromAddress = new String(reader.readLine(), US_ASCII);
            fromAddress = fromAddress.substring(fromAddress.indexOf(":") + 2);
            if (fromAddress.length() == 0) {
                message.setFromAddress(new EmailAddress());
            } else {
                message.setFromAddress(new EmailAddress(fromAddress));
            }
            stringLine = new String(reader.readLine(), US_ASCII);
            stringLine = stringLine.substring(stringLine.indexOf(":") + 2);
            message.setToAddresses(Utils.inflateAddresses(stringLine));
            stringLine = new String(reader.readLine(), US_ASCII);
            stringLine = stringLine.substring(stringLine.indexOf(":") + 2);
            message.setTimeReceived(new Date(Long.parseLong(stringLine)));
            stringLine = new String(reader.readLine(), US_ASCII);
            stringLine = stringLine.substring(stringLine.indexOf(":") + 2);
            message.set8bitMIME(Boolean.parseBoolean(stringLine));
            stringLine = new String(reader.readLine(), US_ASCII);
            stringLine = stringLine.substring(stringLine.indexOf(":") + 2);
            message.setScheduledDelivery(new Date(Long.parseLong(stringLine)));
            stringLine = new String(reader.readLine(), US_ASCII);
            stringLine = stringLine.substring(stringLine.indexOf(":") + 2);
            message.setDeliveryAttempts(Integer.parseInt(stringLine));
            if (!headersOnly) {
                byte[] inputLine = reader.readLine();
                while (inputLine != null) {
                    addDataLine(inputLine);
                    inputLine = reader.readLine();
                }
            }
        } catch (InvalidAddressException invalidAddressException) {
            throw new IOException("Unable to parse the address from the stored file.");
        } catch (NumberFormatException numberFormatException) {
            throw new IOException("Unable to parse the data from the stored file into a number.  " + numberFormatException.toString());
        } finally {
            if (reader != null) {
                reader.close();
                reader = null;
            }
        }
    }

    public long getSize() {
        long size = 0;
        List<byte[]> stringLines = null;
        try {
            Iterator<byte[]> iter;
            int count = 8;
            stringLines = loadIncrementally(count);
            while (stringLines.size() > 0) {
                iter = stringLines.iterator();
                while (iter.hasNext()) {
                    size += iter.next().length;
                }
                stringLines.clear();
                count += 250;
                stringLines = loadIncrementally(count);
            }
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            if (stringLines != null) {
                stringLines.clear();
                stringLines = null;
            }
        }
        return size;
    }

    public void addDataLine(byte[] line) {
        message.incrementSize(line.length);
        message.getDataLines().add(line);
    }

    /**
     * Saves the message to the Mail Spool Directory. Used when rescheduling.
     */
    public void save(boolean useAmavisSMTPDirectory) throws IOException {
        if (!saveBegin(useAmavisSMTPDirectory)) {
            throw new IOException("Renaming or copying a message file failed.");
        }
        File messageFile = new File(getMessageLocation().getPath().substring(0, getMessageLocation().getPath().lastIndexOf(".") + 1) + "bak");
        String messageName = messageFile.getPath();
        List stringLines = null;
        try {
            int count = 8;
            stringLines = loadIncrementally(count, messageName);
            if (stringLines.size() > 0) {
                saveIncrement(stringLines, true, false);
                stringLines.clear();
                count += 250;
                stringLines = loadIncrementally(count, messageName);
                while (stringLines.size() > 0) {
                    saveIncrement(stringLines, false, true);
                    stringLines.clear();
                    count += 250;
                    stringLines = loadIncrementally(count, messageName);
                }
            }
        } catch (IOException ioe) {
            message.getDataLines().clear();
            deleteMessage();
            File toFilename = new File(getMessageLocation().getPath().substring(0, getMessageLocation().getPath().lastIndexOf(".") + 1) + "ser");
            if (messageFile.renameTo(toFilename)) {
                setMessageLocation(toFilename);
            }
            throw (ioe);
        } finally {
            if (stringLines != null) {
                stringLines.clear();
                stringLines = null;
            }
        }
        if (saveFinish()) {
            if (messageFile.delete()) {
                return;
            }
        }
        throw new IOException("A file operation has failed.");
    }

    public boolean saveBegin(boolean useAmavisSMTPDirectory) {
        File smtpDirectory = new File(useAmavisSMTPDirectory ? configurationManager.getAmavisSMTPDirectory() : configurationManager.getSMTPDirectory());
        if (getMessageLocation() == null) {
            File messageFile;
            StringBuilder sb;
            int i;
            do {
                sb = new StringBuilder(8);
                for (i = 0; i < 8; i++) {
                    sb.append(characters.charAt(random.nextInt(16)));
                }
                message.setSMTPUID(sb.toString());
                messageFile = new File(smtpDirectory, "smtp" + message.getSMTPUID() + ".tmp");
                if (!messageFile.exists() && !new File(smtpDirectory, "smtp" + message.getSMTPUID() + ".ser").exists()) break;
            } while (true);
            setMessageLocation(messageFile);
            return true;
        } else {
            File toFilename = new File(getMessageLocation().getPath().substring(0, getMessageLocation().getPath().lastIndexOf(".") + 1) + "bak");
            try {
                FileUtils.copyFile(getMessageLocation(), toFilename);
            } catch (IOException ioe) {
                log.error("Error copying file " + getMessageLocation().getPath() + " to " + toFilename.getPath());
                return false;
            }
            toFilename = new File(toFilename.getPath().substring(0, toFilename.getPath().lastIndexOf(".") + 1) + "tmp");
            if (!toFilename.exists()) {
                deleteMessage();
                setMessageLocation(toFilename);
                return true;
            }
            return false;
        }
    }

    public void saveIncrement(List<byte[]> dataLines, boolean writeHeaders, boolean append) throws IOException {
        int length = dataLines.size();
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(getMessageLocation(), append));
            if (writeHeaders) {
                bos.write(("X-JES-File-Version: " + Utils.FILE_VERSION).getBytes(US_ASCII));
                bos.write(EOL);
                bos.write(("X-JES-UID: " + message.getSMTPUID()).getBytes(US_ASCII));
                bos.write(EOL);
                bos.write(("X-JES-MAIL-FROM: " + message.getFromAddress().toString()).getBytes(US_ASCII));
                bos.write(EOL);
                bos.write(("X-JES-RCPT-TO: " + Utils.flattenAddresses(message.getToAddresses())).getBytes(US_ASCII));
                bos.write(EOL);
                bos.write(("X-JES-Date: " + String.valueOf(message.getTimeReceived().getTime())).getBytes(US_ASCII));
                bos.write(EOL);
                bos.write(("X-JES-8bitMIME: " + String.valueOf(message.is8bitMIME())).getBytes(US_ASCII));
                bos.write(EOL);
                bos.write(("X-JES-Delivery-Date: " + String.valueOf(message.getScheduledDelivery().getTime())).getBytes(US_ASCII));
                bos.write(EOL);
                bos.write(("X-JES-Delivery-Count: " + String.valueOf(message.getDeliveryAttempts())).getBytes(US_ASCII));
                bos.write(EOL);
            }
            for (int index = 0; index < length; index++) {
                bos.write(dataLines.get(index));
                bos.write(EOL);
            }
        } catch (FileNotFoundException e) {
        } catch (UnsupportedEncodingException e) {
        } finally {
            if (null != bos) {
                try {
                    bos.close();
                } catch (IOException e) {
                    log.warn("Unable to close spool file for SMTPMessage " + getMessageLocation().getAbsolutePath());
                }
                bos = null;
            }
        }
    }

    public boolean saveFinish() {
        File toFilename = new File(getMessageLocation().getPath().substring(0, getMessageLocation().getPath().lastIndexOf(".") + 1) + "ser");
        if (getMessageLocation().renameTo(toFilename)) {
            setMessageLocation(toFilename);
            return true;
        }
        deleteMessage();
        return false;
    }

    public List loadIncrementally(int start) throws IOException {
        return loadIncrementally(start, getMessageLocation().getPath());
    }

    public final List loadIncrementally(int start, String messageName) throws IOException {
        DelimitedInputStream reader = new DelimitedInputStream(new FileInputStream(new File(messageName)));
        List<byte[]> stringLines = new ArrayList<byte[]>(250);
        try {
            for (int i = 0; i < start; i++) {
                reader.readLine();
            }
            byte[] inputLine = reader.readLine();
            while (inputLine != null) {
                stringLines.add(inputLine);
                if (stringLines.size() == 250) break;
                inputLine = reader.readLine();
            }
        } catch (IOException ioe) {
            message.getDataLines().clear();
            throw ioe;
        } finally {
            if (reader != null) {
                reader.close();
                reader = null;
            }
            return stringLines;
        }
    }

    /**
     * Moves the message to the 'failed' Directory.
     */
    public void moveToFailedFolder() throws Exception {
        File failedDir = new File(configurationManager.getFailedDirectory());
        File messageFile = getMessageLocation();
        if (!messageFile.renameTo(new File(failedDir, messageFile.getName()))) {
            try {
                FileUtils.copyFile(messageFile, new File(failedDir, messageFile.getName()));
                messageFile.delete();
            } catch (Exception e) {
                throw new Exception("moveToFailedFolder failed for message " + messageFile.getPath());
            }
        }
    }

    public boolean isNotSavedInAmavis() {
        return getMessageLocation().getPath().toUpperCase().indexOf("AMAVIS") == -1;
    }

    public long getPersistedSize() {
        return getMessageLocation().length();
    }

    public boolean deleteMessage() {
        return getMessageLocation().delete();
    }

    public void redirectToPostmaster() throws IOException {
        File smtpDirectory = new File(configurationManager.getAmavisSMTPDirectory());
        File messageFile = new File(smtpDirectory, getMessageLocation().getName());
        FileUtils.copyFile(getMessageLocation(), messageFile);
        deleteMessage();
    }
}

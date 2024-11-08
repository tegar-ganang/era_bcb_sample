package us.conxio.hl7.hl7stream;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import us.conxio.hl7.hl7message.HL7Message;

/**
 * A HL7Stream class which delivers HL7 XML as either a single file
 * or directory of one or more transactions, 1 per file.
 * @author scott
 */
public class HL7XMLFileWriter extends HL7StreamBase implements HL7Stream {

    private boolean singleFile = false;

    private File filePath = null;

    private static final String STRING_XML = "xml";

    /**
    * Fundamental parameterless constructor.
    */
    private HL7XMLFileWriter() {
    }

    /**
    * Fundamental parameterized constructor.
    * @param path The file path for the delivery destination.
    * If the path exists and is not a directory then single file mode is assumed
    * and the XML transactions are appended to the end of the file.
    * Otherwise the given path is assumed to be a directory and it, and
    * non-existent parent directories, are created
    */
    public HL7XMLFileWriter(File path) {
        filePath = path;
        if (filePath.exists() && filePath.isFile()) singleFile = true;
    }

    /**
    * Universal parameterized constructor.
    * @param uri
    */
    public HL7XMLFileWriter(URI uri) {
        if (uri == null) throw new IllegalArgumentException();
        String uriScheme = uri.getScheme();
        if (uriScheme != null && uriScheme.toLowerCase().contains(STRING_XML)) {
            String query = uri.getQuery();
            if (query != null && query.toLowerCase().contains("single")) {
                singleFile = true;
            }
            filePath = new File(uri.getPath());
        }
    }

    /**
    * HL7Stream initialization.
    * @return true, always.
    * @throws HL7IOException
    */
    public boolean open() throws HL7IOException {
        directive = HL7Stream.WRITER;
        mediaType = HL7Stream.FILE_TYPE;
        statusValue = HL7Stream.OPEN;
        return true;
    }

    /**
    * HL7Stream finalization.
    * @return true.
    * @throws HL7IOException
    */
    public boolean close() throws HL7IOException {
        return true;
    }

    /**
    * Always throws an HL7IOException exception
    * @return nothing.
    * @throws HL7IOException
    */
    public HL7Message read() throws HL7IOException {
        throw new HL7IOException("read:", new UnsupportedOperationException("writer does not read."));
    }

    /**
    * Writes the argument HL7Message as an XML file.
    * @param msg A structured HL7Message object.
    * @return true, always.
    * @throws HL7IOException
    */
    public boolean write(HL7Message msg) throws HL7IOException {
        return writeHL7XMLMessageFile(msg);
    }

    /**
    * Creates a message based file with the extension(s) ".hl7.xml", as follows:
    *   - If the message control id is non-empty then the it precedes the extension(s).
    *   - Otherwise the name is created from the message type code, message event
    *     code, and the message date time stamp concatenated in that order, and
    *     separated by a dot ('.'), and precedes the extension(s).
    * @param msg the message from which, and for which to create the file name.
    * @return a String representation of the created file name.
    */
    private String hl7XMLMessageFileName(HL7Message msg) {
        String msgID = msg.controlID();
        if (StringUtils.isEmpty(msgID)) {
            String msgTypeCode = msg.get("MSH.9.1");
            String msgEventCode = msg.get("MSH.9.2");
            String msgDateTime = msg.get("MSH.7");
            StringBuilder msgIDBuffer = new StringBuilder();
            if (StringUtils.isNotEmpty(msgTypeCode)) msgIDBuffer.append(msgTypeCode);
            if (StringUtils.isNotEmpty(msgEventCode)) {
                if (msgIDBuffer.length() > 0) msgIDBuffer.append(".");
                msgIDBuffer.append(msgEventCode);
            }
            if (StringUtils.isNotEmpty(msgDateTime)) {
                if (msgIDBuffer.length() > 0) msgIDBuffer.append(".");
                msgIDBuffer.append(msgDateTime);
            }
            msgID = msgIDBuffer.toString();
        }
        return new StringBuffer(filePath.toString()).append(File.separator).append(msgID).append(".hl7.xml").toString();
    }

    /**
    * The write supervisor for creation, writing, and finalization of individual
    * message files.
    * @param msg the message to be written.
    * @return true, always.
    */
    private boolean writeHL7XMLMessageFile(HL7Message msg) throws HL7IOException {
        if (!filePath.exists()) createPath();
        File xmlFile = singleFile ? filePath : new File(hl7XMLMessageFileName(msg));
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(xmlFile, singleFile));
            writer.write(msg.toXMLString());
        } catch (IOException ioEx) {
            throw new HL7IOException("write caught IOException:", ioEx);
        } finally {
            try {
                writer.close();
            } catch (IOException ioEx) {
                throw new HL7IOException("close caught IOException:", ioEx);
            }
        }
        return true;
    }

    private void createPath() throws HL7IOException {
        if (singleFile) {
            File parentPath = filePath.getParentFile();
            if (!parentPath.exists()) parentPath.mkdirs();
            if (!filePath.exists()) {
                try {
                    filePath.createNewFile();
                } catch (IOException ioEx) {
                    throw new HL7IOException("IOException:", ioEx);
                }
            }
            return;
        }
        if (filePath.exists()) {
            if (filePath.isFile()) singleFile = true;
            return;
        }
        filePath.mkdirs();
    }
}

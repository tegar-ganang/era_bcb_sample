package jimm.twice.ice;

import jimm.twice.util.Logger;
import java.io.*;

/**
 * Manages XML text data, large or small. Since large amounts of content
 * may be delivered inline, we want to avoid keeping all that text in
 * memory. If the text given to us exceeds a certain size, we store the
 * data in a temp file on disk. The text data is retrievable as a string
 * or as an <code>InputStream</code>.
 * <p>
 * Behavior is undefined if you retrieve data using
 * <code>getTextDataInputStream</code> and then continue to add data by
 * calling <code>append</code>.
 * <p>
 * It would be trivial to keep track of how big the data is and provide
 * a method to return that value, or a boolean method that indicates
 * if the data is on disk or in memory, but we don't need them yet.
 *
 * @author Jim Menard, <a href="mailto:jimm@io.com">jimm@io.com</a> */
public class XMLTextData {

    protected static final int IN_MEMORY_MAX_SIZE = 4096;

    protected static final int BUFSIZ = 4096;

    protected static final String TEMP_FILE_PREFIX = "ice-xml-txt";

    protected static final String TEMP_FILE_SUFFIX = ".tmp";

    protected static final String LOGGER_PREFIX = "ice";

    protected StringBuffer textBuffer;

    protected File tempFile;

    protected FileWriter tempFileWriter;

    public void append(char[] ch, int start, int length) {
        if (tempFileWriter != null) {
            try {
                tempFileWriter.write(ch, start, length);
            } catch (IOException ioe) {
                Logger.instance().log(Logger.ERROR, LOGGER_PREFIX, "XMLTextData.append", ioe);
            }
            return;
        }
        int newLen = length;
        if (textBuffer != null) newLen += textBuffer.length();
        if (newLen > IN_MEMORY_MAX_SIZE) {
            try {
                tempFile = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
                tempFile.deleteOnExit();
                tempFileWriter = new FileWriter(tempFile);
                tempFileWriter.write(textBuffer.toString());
                tempFileWriter.write(ch, start, length);
                textBuffer = null;
            } catch (IOException ioe) {
                Logger.instance().log(Logger.ERROR, LOGGER_PREFIX, "XMLTextData.append", ioe);
            }
        } else {
            if (textBuffer == null) textBuffer = new StringBuffer();
            textBuffer.append(ch, start, length);
        }
    }

    /**
 * Returns the text we are holding, or <code>null</code> if
 * there is no data. If the data is being stored in a temp file, that text
 * is read into memory.
 *
 * @return an input stream on our text; may be <code>null</code>
 */
    public String getTextData() {
        if (tempFileWriter != null) {
            try {
                tempFileWriter.flush();
                tempFileWriter.close();
                FileReader in = new FileReader(tempFile);
                StringWriter out = new StringWriter();
                int len;
                char[] buf = new char[BUFSIZ];
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                out.close();
                in.close();
                return out.toString();
            } catch (IOException ioe) {
                Logger.instance().log(Logger.ERROR, LOGGER_PREFIX, "XMLTextData.getTextData", ioe);
                return "";
            }
        } else if (textBuffer != null) return textBuffer.toString(); else return null;
    }

    /**
 * Returns an input stream on the text we are holding, or <code>null</code>
 * if there is no data.
 *
 * @return a reader on our text; may be <code>null</code>
 */
    public InputStream getTextDataInputStream() {
        if (tempFileWriter != null) {
            try {
                tempFileWriter.flush();
                tempFileWriter.close();
                return new FileInputStream(tempFile);
            } catch (IOException ioe) {
                Logger.instance().log(Logger.ERROR, LOGGER_PREFIX, "XMLTextData.getTextDataInputStream", ioe);
                return new ByteArrayInputStream(new byte[0]);
            }
        } else if (textBuffer != null) return new ByteArrayInputStream(textBuffer.toString().getBytes()); else return null;
    }

    protected void finalize() throws Throwable {
        cleanup();
    }

    public void cleanup() {
        try {
            if (tempFileWriter != null) {
                tempFileWriter.close();
                tempFileWriter = null;
            }
            if (tempFile != null) {
                tempFile.delete();
                tempFile = null;
            }
        } catch (IOException ioe) {
            Logger.instance().log(Logger.ERROR, LOGGER_PREFIX, "XMLTextData.cleanup", ioe);
        }
        textBuffer = null;
    }

    public String toString() {
        if (tempFileWriter != null) return "[XMLTextData; tempFile=" + tempFile + "]"; else if (textBuffer != null) return textBuffer.toString(); else return "[XMLTextData; no data]";
    }
}

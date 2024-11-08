package org.xaware.server.engine.channel.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.springframework.core.io.Resource;
import org.xaware.server.engine.IScopedChannel;
import org.xaware.server.resources.ResourceHelper;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.XAwareConstants.FileMode;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * This class implements IScopedChannel for File input and output. It allows us to keep files open as long as necessary,
 * providing an input or output stream.
 * 
 * @author jtarnowski
 */
public class FileTemplate implements IScopedChannel {

    /** Our InputStream that can be used for reading a file */
    InputStream inStream = null;

    /** Our OutputStream that can be used for writing to a file */
    OutputStream outStream = null;

    /** The name of our file */
    String fileName = null;

    /** READ, WRITE or APPEND */
    FileMode mode;

    /** Our name for logging */
    private static final String className = "FileTemplate";

    /** Our logger */
    private XAwareLogger logger = XAwareLogger.getXAwareLogger(className);

    /**
     * Constructor you can't call
     */
    private FileTemplate() {
    }

    /**
     * Constructor you must call
     */
    public FileTemplate(FileBizDriver driver) throws XAwareException {
        driver.createChannelObject();
        fileName = driver.getData().getFileName();
        String modeStr = driver.getData().getMode();
        if (XAwareConstants.XAWARE_FILE_READ.equals(modeStr)) {
            mode = XAwareConstants.FileMode.READ;
        } else if (XAwareConstants.XAWARE_FILE_WRITE.equals(modeStr)) {
            mode = XAwareConstants.FileMode.WRITE;
        } else if (XAwareConstants.XAWARE_FILE_APPEND.equals(modeStr)) {
            mode = XAwareConstants.FileMode.APPEND;
        } else {
            throw new XAwareException("FileTemplate: " + XAwareConstants.BIZCOMPONENT_ATTR_REQUEST_TYPE + " must be \"read\", \"write\" or \"append\"");
        }
    }

    /**
     * implement the interface's getScopedChannelType() Get the scoped channel type for this scoped channel instance.
     * 
     * @return the IScopedChannel.Type for this IScopedChannel instance.
     */
    public Type getScopedChannelType() {
        return Type.FILE;
    }

    /**
     * implement the interface's close() - close open stream
     * 
     */
    public void close(boolean success) {
        if (inStream != null) {
            try {
                inStream.close();
            } catch (IOException e) {
            }
        }
        if (outStream != null) {
            try {
                outStream.flush();
                outStream.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Close just the instream - call this when you are at the end of file
     * and want to specifically close
     */
    public void closeInstream() {
        if (inStream != null) {
            try {
                inStream.close();
            } catch (IOException e) {
            }
            inStream = null;
        }
    }

    /**
     * Initialize the reader or writer
     * 
     * @throws XAwareException
     */
    public void initResources() throws XAwareException {
        try {
            Resource resource = ResourceHelper.getResource(fileName);
            File file = null;
            switch(mode) {
                case READ:
                    if (inStream == null) {
                        if (!resource.exists()) throw new XAwareException(fileName + "(The system cannot find the file specified)");
                        inStream = resource.getInputStream();
                    }
                    break;
                case WRITE:
                    if (outStream == null) {
                        try {
                            file = resource.getFile();
                            if (!file.exists()) {
                                file = new File(fileName);
                            }
                        } catch (Exception e) {
                            file = new File(fileName);
                        }
                        file.createNewFile();
                        outStream = new FileOutputStream(file);
                    }
                    break;
                case APPEND:
                    if (outStream == null) {
                        try {
                            file = resource.getFile();
                            if (!file.exists()) {
                                file = new File(fileName);
                            }
                        } catch (Exception e) {
                            file = new File(fileName);
                        }
                        file.createNewFile();
                        outStream = new FileOutputStream(file, true);
                    }
                    break;
            }
        } catch (Exception e) {
            logger.severe("Error opening file " + fileName + " " + e.getLocalizedMessage(), className, "initResources");
            throw new XAwareException(e);
        }
    }

    /**
     * @return the inStream
     */
    public InputStream getInStream() throws XAwareException {
        if (inStream == null) {
            if (mode == FileMode.READ) {
                initResources();
            }
        }
        return inStream;
    }

    /**
     * @param inStream
     *            the inStream to set
     */
    public void setInStream(InputStream inStream) {
        this.inStream = inStream;
    }

    /**
     * @return the outStream
     */
    public OutputStream getOutStream() throws XAwareException {
        if (outStream == null) {
            if (mode != FileMode.READ) {
                initResources();
            }
        }
        return outStream;
    }

    /**
     * @param outStream
     *            the outStream to set
     */
    public void setOutStream(OutputStream outStream) {
        this.outStream = outStream;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName
     *            the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return the mode
     */
    public FileMode getMode() {
        return mode;
    }

    /**
     * @param mode
     *            the mode to set
     */
    public void setMode(FileMode mode) {
        this.mode = mode;
    }
}

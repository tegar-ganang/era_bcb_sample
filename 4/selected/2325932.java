package org.xaware.server.engine.channel.file;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.springframework.core.io.Resource;
import org.xaware.server.engine.IScopedChannel;
import org.xaware.server.resources.ResourceHelper;
import org.xaware.server.utils.ant.filters.StringInputStream;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.XAwareConstants.FileMode;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * @deprecated
 * This class implements IScopedChannel for File input and output. It allows us to keep files open as long as necessary,
 * providing an input or output stream.
 * 
 * @author jtarnowski
 */
public class FileTemplate54 implements IScopedChannel {

    /** Our InputStream that can be used for reading a file */
    InputStream inStream = null;

    /** Our OutputStream that can be used for writing to a file */
    OutputStream outStream = null;

    /** The name of our file */
    String fileName = null;

    /** READ, WRITE or APPEND */
    FileMode mode;

    /** file media or string (supporting in-memory formatting) */
    String mediaType;

    /** initial buffer data that we'll read (instead of reading from a file) */
    String bufferData;

    /**
     * @return the media type
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * set the media type - buffer or file
     * @param mediaType
     */
    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    /** Our name for logging */
    private static final String className = "FileTemplate";

    /** Our logger */
    private XAwareLogger logger = XAwareLogger.getXAwareLogger(className);

    /**
     * Constructor you can't call
     */
    @SuppressWarnings("unused")
    private FileTemplate54() {
    }

    /**
     * Constructor you must call
     */
    public FileTemplate54(FileBizDriver driver) throws XAwareException {
        FileDriverData54 data = (FileDriverData54) driver.createChannelObject();
        fileName = data.getFileName();
        mediaType = data.getMediaType();
        bufferData = data.getBufferData();
        String modeStr = data.getMode();
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
            inStream = null;
        }
        if (outStream != null) {
            try {
                outStream.flush();
                outStream.close();
            } catch (IOException e) {
            }
            outStream = null;
        }
    }

    /**
     * Close just the instream - call this when you are at the end of file and want to specifically close
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
                        if (mediaType.equals(FileBizDriver.BUFFER_MEDIA)) {
                            inStream = new StringInputStream(bufferData);
                        } else {
                            if (!resource.exists()) throw new XAwareException(fileName + "(The system cannot find the file specified)");
                            inStream = resource.getInputStream();
                        }
                    }
                    break;
                case WRITE:
                case APPEND:
                    if (outStream == null) {
                        if (mediaType.equals(FileBizDriver.BUFFER_MEDIA)) {
                            outStream = new ByteArrayOutputStream();
                        } else {
                            try {
                                file = resource.getFile();
                                if (!file.exists()) {
                                    file = new File(fileName);
                                }
                            } catch (Exception e) {
                                file = new File(fileName);
                            }
                            file.createNewFile();
                            outStream = new FileOutputStream(file, mode == FileMode.APPEND);
                        }
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

    /** If OutStream exist, then return true. */
    public boolean isOutStreamOpened() {
        if (outStream == null) return false; else return true;
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

    /**
     * @return the buffer data
     */
    public String getBufferData() {
        return bufferData;
    }

    /**
	 * set the buffer data
	 * @param bufferData
	 */
    public void setBufferData(String bufferData) {
        this.bufferData = bufferData;
    }

    @Override
    protected void finalize() throws Throwable {
        this.close(true);
        super.finalize();
    }
}

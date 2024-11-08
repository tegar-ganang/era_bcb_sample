package org.alfresco.repo.content.filestore;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.FileCopyUtils;

/**
 * Provides direct access to a local file.
 * <p>
 * This class does not provide remote access to the file.
 * 
 * @author Derek Hulley
 */
public class FileContentWriter implements ContentWriter {

    private static final Log logger = LogFactory.getLog(FileContentWriter.class);

    private File file;

    private String uri;

    private boolean allowRandomAccess;

    /**
	 * Constructor that builds a URL based on the absolute path of the file.
	 * 
	 * @param file
	 *            the file for writing. This will most likely be directly
	 *            related to the content URL.
	 */
    public FileContentWriter(File file, String uri) {
        this.file = file;
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    void setAllowRandomAccess(boolean allow) {
        this.allowRandomAccess = allow;
    }

    /**
	 * @return Returns the file that this writer accesses
	 */
    public File getFile() {
        return file;
    }

    /**
	 * @return Returns the size of the underlying file or
	 */
    public long getSize() {
        if (file == null) return 0L; else if (!file.exists()) return 0L; else return file.length();
    }

    /**
	 * @see Channels#newOutputStream(java.nio.channels.WritableByteChannel)
	 */
    public OutputStream getContentOutputStream() {
        try {
            WritableByteChannel channel = getWritableChannel();
            OutputStream is = new BufferedOutputStream(Channels.newOutputStream(channel));
            return is;
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public WritableByteChannel getWritableChannel() {
        try {
            if (file.exists() && file.length() > 0) {
                throw new IOException("File exists - overwriting not allowed");
            }
            WritableByteChannel channel = null;
            if (allowRandomAccess) {
                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                channel = randomAccessFile.getChannel();
            } else {
                OutputStream os = new FileOutputStream(file);
                channel = Channels.newChannel(os);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Opened write channel to file: \n" + "   file: " + file + "\n" + "   random-access: " + allowRandomAccess);
            }
            return channel;
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public void putContent(InputStream is) {
        try {
            OutputStream os = getContentOutputStream();
            FileCopyUtils.copy(is, os);
        } catch (IOException e) {
            throw new ContentIOException("Failed to copy content from input stream: \n" + "   writer: " + this, e);
        }
    }

    @Override
    public void putContent(File file) {
        try {
            OutputStream os = getContentOutputStream();
            FileInputStream is = new FileInputStream(file);
            FileCopyUtils.copy(is, os);
        } catch (IOException e) {
            throw new ContentIOException("Failed to copy content from file: \n" + "   writer: " + this + "\n" + "   file: " + file, e);
        }
    }

    @Override
    public void putContent(String content, String encoding) {
        try {
            byte[] bytes = (encoding == null) ? content.getBytes() : content.getBytes(encoding);
            OutputStream os = getContentOutputStream();
            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            FileCopyUtils.copy(is, os);
        } catch (IOException e) {
            throw new ContentIOException("Failed to copy content from string: \n" + "   writer: " + this + "   content length: " + content.length(), e);
        }
    }
}

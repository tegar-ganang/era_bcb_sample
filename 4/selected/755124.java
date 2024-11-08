package org.alfresco.repo.content.filestore;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
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
public class FileContentReader implements ContentReader {

    /**
	 * message key for missing content. Parameters are
	 * <ul>
	 * <li>{@link org.alfresco.service.cmr.repository.NodeRef NodeRef}</li>
	 * <li>{@link ContentReader ContentReader}</li>
	 * </ul>
	 */
    public static final String MSG_MISSING_CONTENT = "content.content_missing";

    private static final Log logger = LogFactory.getLog(FileContentReader.class);

    private File file;

    private String uri;

    private boolean allowRandomAccess;

    private ReadableByteChannel channel;

    /**
	 * Constructor that explicitely sets the URL that the reader represents.
	 * 
	 * @param file
	 *            the file for reading. This will most likely be directly
	 *            related to the content URL.
	 * @param url
	 *            the relative url that the reader represents
	 */
    public FileContentReader(File file, String url) {
        this.uri = url;
        this.file = file;
        allowRandomAccess = true;
    }

    void setAllowRandomAccess(boolean allow) {
        this.allowRandomAccess = allow;
    }

    /**
	 * @return Returns the file that this reader accesses
	 */
    public File getFile() {
        return file;
    }

    public boolean exists() {
        return file.exists();
    }

    public String getUri() {
        return uri;
    }

    /**
	 * @see File#length()
	 */
    public long getSize() {
        if (!exists()) {
            return 0L;
        } else {
            return file.length();
        }
    }

    /**
	 * @see File#lastModified()
	 */
    public long getLastModified() {
        if (!exists()) {
            return 0L;
        } else {
            return file.lastModified();
        }
    }

    public ReadableByteChannel getDirectReadableChannel() {
        try {
            if (!file.exists()) {
                throw new IOException("File does not exist: " + file);
            }
            ReadableByteChannel channel = null;
            if (allowRandomAccess) {
                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
                channel = randomAccessFile.getChannel();
            } else {
                InputStream is = new FileInputStream(file);
                channel = Channels.newChannel(is);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Opened write channel to file: \n" + "   file: " + file + "\n" + "   random-access: " + allowRandomAccess);
            }
            return channel;
        } catch (Throwable e) {
            throw new ContentIOException("Failed to open file channel: " + this, e);
        }
    }

    /**
	 * @return Returns false as this is a reader
	 */
    public boolean canWrite() {
        return false;
    }

    @Override
    public InputStream getContentInputStream() {
        try {
            ReadableByteChannel channel = getReadableChannel();
            InputStream is = new BufferedInputStream(Channels.newInputStream(channel));
            return is;
        } catch (Throwable e) {
            throw new ContentIOException("Failed to open stream onto channel: \n" + "   accessor: " + this, e);
        }
    }

    @Override
    public String getContentString() {
        try {
            InputStream is = getContentInputStream();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            FileCopyUtils.copy(is, os);
            byte[] bytes = os.toByteArray();
            String encoding = getEncoding();
            String content = (encoding == null) ? new String(bytes) : new String(bytes, encoding);
            return content;
        } catch (IOException e) {
            throw new ContentIOException("Failed to copy content to string: \n" + "   accessor: " + this, e);
        }
    }

    private String getEncoding() {
        return "UTF-8";
    }

    @Override
    public String getContentString(int length) {
        if (length < 0 || length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Character count must be positive and within range");
        }
        Reader reader = null;
        try {
            char[] buffer = new char[length];
            String encoding = getEncoding();
            if (encoding == null) {
                reader = new InputStreamReader(getContentInputStream());
            } else {
                reader = new InputStreamReader(getContentInputStream(), encoding);
            }
            int count = reader.read(buffer, 0, length);
            return (count != -1 ? new String(buffer, 0, count) : "");
        } catch (IOException e) {
            throw new ContentIOException("Failed to copy content to string: \n" + "   accessor: " + this + "\n" + "   length: " + length, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable e) {
                    logger.error(e);
                }
            }
        }
    }

    @Override
    public ReadableByteChannel getReadableChannel() {
        if (channel != null) {
            throw new RuntimeException("A channel has already been opened");
        }
        return getDirectReadableChannel();
    }

    @Override
    public boolean isClosed() {
        if (channel != null) {
            return !channel.isOpen();
        } else {
            return false;
        }
    }
}

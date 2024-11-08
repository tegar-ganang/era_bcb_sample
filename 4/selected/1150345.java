package com.ever365.vfile;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import net.gqu.utils.FileCopyUtils;

/**
 * Provides direct access to a local file.
 * <p>
 * This class does not provide remote access to the file.
 * 
 * @author Derek Hulley
 */
public class FileContentReader {

    /**
	 * message key for missing content. Parameters are
	 * <ul>
	 * <li>{@link org.alfresco.service.cmr.repository.NodeRef NodeRef}</li>
	 * <li>{@link ContentReader ContentReader}</li>
	 * </ul>
	 */
    public static final String MSG_MISSING_CONTENT = "content.content_missing";

    private File file;

    private String encoding;

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
    public FileContentReader(File file, String encoding) {
        this.file = file;
        this.encoding = encoding;
        allowRandomAccess = true;
    }

    void setAllowRandomAccess(boolean allow) {
        this.allowRandomAccess = allow;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
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
        return file.getAbsolutePath();
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
            return channel;
        } catch (Throwable e) {
            throw new ContentException("Failed to open file channel: " + this);
        }
    }

    /**
	 * @return Returns false as this is a reader
	 */
    public boolean canWrite() {
        return false;
    }

    public InputStream getContentInputStream() {
        try {
            ReadableByteChannel channel = getReadableChannel();
            InputStream is = new BufferedInputStream(Channels.newInputStream(channel));
            return is;
        } catch (Throwable e) {
            throw new ContentException("Failed to open stream onto channel: \n" + "   accessor: " + this);
        }
    }

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
            throw new ContentException("Failed to copy content to string: \n" + "   accessor: " + this, e);
        }
    }

    private String getEncoding() {
        if (encoding == null) {
            return "UTF-8";
        } else {
            return encoding;
        }
    }

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
            throw new ContentException("Failed to copy content to string: \n" + "   accessor: " + this + "\n" + "   length: " + length, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public ReadableByteChannel getReadableChannel() {
        if (channel != null) {
            throw new RuntimeException("A channel has already been opened");
        }
        return getDirectReadableChannel();
    }

    public boolean isClosed() {
        if (channel != null) {
            return !channel.isOpen();
        } else {
            return false;
        }
    }

    public void readInto(OutputStream outputStream) {
        try {
            InputStream inputStream = getContentInputStream();
            FileCopyUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            throw new ContentException("Failed to copy content to output stream: \n" + "   accessor: " + this, e);
        }
    }

    public File getLocalFile() {
        return file;
    }

    /**
     * Copies the {@link #getContentInputStream() input stream} to the given
     * <code>OutputStream</code>
     */
    public final void getContent(OutputStream os) throws ContentException {
        try {
            InputStream is = getContentInputStream();
            FileCopyUtils.copy(is, os);
        } catch (IOException e) {
            throw new ContentException("Failed to copy content to output stream: \n" + "   accessor: " + this, e);
        }
    }

    public Reader getReader(String charset) {
        InputStream is = getContentInputStream();
        try {
            return new InputStreamReader(is, charset);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}

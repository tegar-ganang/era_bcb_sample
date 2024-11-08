package org.torweg.pulse.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import javax.activation.DataSource;
import javax.activation.FileTypeMap;
import org.torweg.pulse.service.PulseException;
import org.torweg.pulse.vfs.VirtualFile;
import org.torweg.pulse.vfs.VirtualFileSystem;

/**
 * represents a serialisable data source.
 * 
 * @author Thomas Weber
 * @version $Revision: 1.1 $
 */
public class SerializableDataSource implements DataSource, INamed, Serializable {

    /**
	 * serialVersionUID.
	 */
    private static final long serialVersionUID = 6225932694512763871L;

    /**
	 * the content-type.
	 */
    private String contentType = "application/octet-stream";

    /**
	 * the file to be used as an data source.
	 */
    private File file;

    /**
	 * the virtual file to be used as an data source.
	 */
    private VirtualFile virtualFile;

    /**
	 * the byte buffer to be used as an data source.
	 */
    private byte[] buffer;

    /**
	 * the name of the data source.
	 */
    private String name;

    /**
	 * the length of the source.
	 */
    private long length;

    /**
	 * creates a new serialisable data source from a given
	 * <code>DataSource</code>.
	 * 
	 * @param s
	 *            the source <code>DataSource</code>
	 * @throws IOException
	 *             on errors reading from the <code>DataSource</code>'s
	 *             <code>InputStream</code>
	 */
    public SerializableDataSource(final DataSource s) throws IOException {
        super();
        this.name = s.getName();
        this.contentType = s.getContentType();
        bufferStream(s.getInputStream());
    }

    /**
	 * creates a new data source from a given file.
	 * <p>
	 * <strong>This constructor assumes that the file will still be at the given
	 * location upon sending time.</strong> If you are working with a temporary
	 * file, use
	 * {@link SerializableDataSource#SerializableDataSource(InputStream,String)}
	 * instead.
	 * </p>
	 * 
	 * @param f
	 *            the file
	 */
    public SerializableDataSource(final File f) {
        super();
        this.file = f;
        this.name = f.getName();
        this.length = f.length();
    }

    /**
	 * creates a new data source from a given file with the given name.
	 * <p>
	 * <strong>This constructor assumes that the file will still be at the given
	 * location upon sending time.</strong> If you are working with a temporary
	 * file, use
	 * {@link SerializableDataSource#SerializableDataSource(InputStream, String)}
	 * instead.
	 * </p>
	 * 
	 * @param f
	 *            the file
	 * @param n
	 *            the name
	 */
    public SerializableDataSource(final File f, final String n) {
        super();
        this.file = f;
        this.length = f.length();
        if (n != null) {
            this.name = n;
        } else {
            this.name = f.getName();
        }
    }

    /**
	 * creates a new data source from a given virtual file.
	 * <p>
	 * <strong>This constructor assumes that the virtual file will still be at
	 * the given location upon sending time.</strong> If you are working with a
	 * temporary file, use
	 * {@link SerializableDataSource#SerializableDataSource(InputStream,String)}
	 * instead.
	 * </p>
	 * 
	 * @param f
	 *            the file
	 */
    public SerializableDataSource(final VirtualFile f) {
        super();
        this.virtualFile = f;
        this.name = f.getName();
        this.length = f.getFilesize();
    }

    /**
	 * creates a new data source from a given virtual file with the given name.
	 * <p>
	 * <strong>This constructor assumes that the virtual file will still be at
	 * the given location upon sending time.</strong> If you are working with a
	 * temporary file, use
	 * {@link SerializableDataSource#SerializableDataSource(InputStream,String)}
	 * instead.
	 * </p>
	 * 
	 * @param f
	 *            the file
	 * @param n
	 *            the name
	 */
    public SerializableDataSource(final VirtualFile f, final String n) {
        super();
        this.virtualFile = f;
        this.length = f.getFilesize();
        if (n != null) {
            this.name = n;
        } else {
            this.name = f.getName();
        }
    }

    /**
	 * creates a new data source from a given <code>InputStream</code> and the
	 * given name with a content-type of "application/octet-stream", which will
	 * be buffered until sending time.
	 * 
	 * @param i
	 *            the input stream
	 * @param n
	 *            the name
	 * @throws IOException
	 *             on errors during buffering
	 */
    public SerializableDataSource(final InputStream i, final String n) throws IOException {
        super();
        if (n != null) {
            this.name = n;
        } else {
            throw new NullPointerException("The name must not be null");
        }
        bufferStream(i);
    }

    /**
	 * creates a new data source from a given <code>InputStream</code>, the
	 * given name and the given content-type.&nbsp;The stream will be buffered
	 * until sending time.
	 * 
	 * @param i
	 *            the input stream
	 * @param n
	 *            the name
	 * @param c
	 *            the content-type
	 * 
	 * @throws IOException
	 *             on errors during buffering
	 */
    public SerializableDataSource(final InputStream i, final String n, final String c) throws IOException {
        super();
        if (n != null) {
            this.name = n;
        } else {
            throw new NullPointerException("The name must not be null");
        }
        if (c != null) {
            this.contentType = c;
        }
        bufferStream(i);
    }

    /**
	 * creates a new data source from a given byte buffer, the given name and
	 * the given content-type.
	 * 
	 * @param byteArray
	 *            the buffer
	 * @param n
	 *            the name
	 * @param c
	 *            the content-type
	 */
    public SerializableDataSource(final byte[] byteArray, final String n, final String c) {
        super();
        if (n != null) {
            this.name = n;
        } else {
            throw new NullPointerException("The name must not be null");
        }
        if (c != null) {
            this.contentType = c;
        }
        this.buffer = new byte[byteArray.length];
        this.length = byteArray.length;
        System.arraycopy(byteArray, 0, this.buffer, 0, byteArray.length);
    }

    /**
	 * creates a data source from a given string, the given name and the given
	 * content-type.
	 * 
	 * @param s
	 *            the text content as a string
	 * @param n
	 *            the name
	 * @param c
	 *            the content type
	 */
    public SerializableDataSource(final String s, final String n, final String c) {
        super();
        String encoding = "utf-8";
        if (c.indexOf("encoding=") != -1) {
            this.contentType = c;
            encoding = c.substring(c.indexOf("encoding=") + 9);
        } else {
            this.contentType = c + "; encoding=utf-8";
        }
        try {
            this.buffer = s.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            throw new PulseException(e);
        }
        this.name = n;
        this.length = this.buffer.length;
    }

    /**
	 * does the stream buffering.
	 * 
	 * @param i
	 *            the stream to be buffered
	 * @throws IOException
	 *             on errors during buffering
	 */
    private void bufferStream(final InputStream i) throws IOException {
        ByteArrayOutputStream sink = new ByteArrayOutputStream(1024);
        while (i.available() > 0) {
            sink.write(i.read());
        }
        i.close();
        this.buffer = sink.toByteArray();
        this.length = buffer.length;
    }

    /**
	 * returns the content-type of the data source.
	 * 
	 * @return the content-type
	 * @see javax.activation.DataSource#getContentType()
	 */
    public final String getContentType() {
        if (this.buffer != null) {
            return this.contentType;
        } else if (this.virtualFile != null) {
            return FileTypeMap.getDefaultFileTypeMap().getContentType(this.virtualFile.getName());
        }
        return FileTypeMap.getDefaultFileTypeMap().getContentType(this.file);
    }

    /**
	 * returns an <code>InputStream</code> for the data source.
	 * 
	 * @return an input stream
	 * @throws IOException
	 *             on errors accessing the <code>InputStream</code>
	 * @see javax.activation.DataSource#getInputStream()
	 */
    public final InputStream getInputStream() throws IOException {
        if (this.buffer != null) {
            return new ByteArrayInputStream(this.buffer);
        } else if (this.virtualFile != null) {
            return VirtualFileSystem.getInstance().getInputStream(this.virtualFile);
        }
        return new FileInputStream(this.file);
    }

    /**
	 * returns the name.
	 * 
	 * @return the name
	 * @see javax.activation.DataSource#getName()
	 */
    public final String getName() {
        return this.name;
    }

    /**
	 * will always throw an <code>IOException</code>.
	 * 
	 * @throws IOException
	 *             always
	 * @return nothing
	 * @see javax.activation.DataSource#getOutputStream()
	 */
    public final OutputStream getOutputStream() throws IOException {
        throw new IOException("Not implemented.");
    }

    /**
	 * returns the length of the source.
	 * 
	 * @return the length of the source
	 */
    public final long getLength() {
        return this.length;
    }
}

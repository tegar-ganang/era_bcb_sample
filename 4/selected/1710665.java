package x.java.io;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import x.java.nio.channels.FileChannel;

/**
 * @author qiangli
 *
 */
public class FileInputStream extends java.io.FileInputStream {

    protected InputStream is = null;

    protected java.io.File file = null;

    /**
	 * If the named file does not exist, is a directory rather than a regular file, 
	 * or for some other reason cannot be opened for reading then a FileNotFoundException is thrown. 
	 * @param file
	 * @throws FileNotFoundException
	 */
    public FileInputStream(java.io.File file) throws FileNotFoundException {
        super(new FileDescriptor());
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            throw new FileNotFoundException(file.getPath());
        }
        this.file = file;
        try {
            this.is = file.toURL().openStream();
        } catch (MalformedURLException e) {
            throw new FileNotFoundException(file.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * @param name
	 * @throws FileNotFoundException
	 */
    public FileInputStream(String name) throws FileNotFoundException {
        this(new File(name));
    }

    public int available() throws IOException {
        return is.available();
    }

    public void close() throws IOException {
        is.close();
    }

    public java.nio.channels.FileChannel getChannel() {
        try {
            java.io.File cache = java.io.File.createTempFile("vfs", ".channel");
            return new FileChannel(file, new java.io.FileInputStream(cache).getChannel());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public int read() throws IOException {
        return is.read();
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return is.read(b, off, len);
    }

    public int read(byte[] b) throws IOException {
        return is.read(b);
    }

    public long skip(long n) throws IOException {
        return is.skip(n);
    }

    public synchronized void mark(int readlimit) {
        is.mark(readlimit);
    }

    public boolean markSupported() {
        return is.markSupported();
    }

    public synchronized void reset() throws IOException {
        is.reset();
    }

    public java.io.File getFile() {
        return this.file;
    }
}

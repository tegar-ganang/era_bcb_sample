package com.totalchange.wtframework.spider;

import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxies a stream - writing its contents to a temporary file. The aim is that
 * we cache a local copy of content so we don't have to repeatedly fetch it.
 * 
 * @author Ralph Jones
 */
class WtfSpiderContentStream extends InputStream {

    private static final int BUFFER_SIZE = 4 * 1024;

    private static Logger logger = LoggerFactory.getLogger(WtfSpiderContentStream.class);

    private InputStream wrapped;

    private File tempFile;

    private OutputStream out;

    WtfSpiderContentStream(InputStream in) throws IOException {
        this.wrapped = in;
        this.tempFile = File.createTempFile("wtf", ".tmp");
        this.tempFile.deleteOnExit();
        this.out = new BufferedOutputStream(new FileOutputStream(this.tempFile));
    }

    File getTempFile() {
        return this.tempFile;
    }

    @Override
    public int read() throws IOException {
        int read = wrapped.read();
        out.write(read);
        return read;
    }

    @Override
    public int available() throws IOException {
        return wrapped.available();
    }

    @Override
    public void close() throws IOException {
        logger.debug("Closing wrapped stream - passing through remainder");
        byte[] buf = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = wrapped.read(buf)) > -1) {
            out.write(buf, 0, read);
        }
        logger.debug("Now actually closing the wrapped stream and copied " + "stream");
        wrapped.close();
        out.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = wrapped.read(b, off, len);
        out.write(b, off, len);
        return read;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int read = wrapped.read(b);
        out.write(b);
        return read;
    }

    @Override
    public synchronized void reset() throws IOException {
    }

    @Override
    public long skip(long n) throws IOException {
        byte[] buf = new byte[(int) n];
        DataInput di = new DataInputStream(wrapped);
        di.readFully(buf);
        out.write(buf);
        return n;
    }
}

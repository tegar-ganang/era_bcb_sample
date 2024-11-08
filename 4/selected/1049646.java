package org.prevayler.implementation;

import java.io.*;

/**
   A FileOutputStream that counts the number of bytes written and forces all
   buffers to synchronize with the underlying device when flushed.
*/
final class DelegatingByteCountStream extends OutputStream {

    private OutputStream theBuffer;

    private FileOutputStream theFile;

    DelegatingByteCountStream(File file, int aBufferSize) throws IOException {
        System.err.println("Open: " + file);
        theFile = new FileOutputStream(file);
        if (aBufferSize > 0) theBuffer = new BufferedOutputStream(theFile, aBufferSize); else theBuffer = null;
    }

    public void close() throws IOException {
        flush();
        getTopStream().close();
    }

    public void flush() throws IOException {
        getTopStream().flush();
        theFile.getChannel().force(false);
    }

    private OutputStream getTopStream() {
        if (theBuffer == null) return theFile; else return theBuffer;
    }

    public void write(byte[] b) throws IOException {
        getTopStream().write(b);
        bytesWritten += b.length;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        getTopStream().write(b, off, len);
        bytesWritten += len;
    }

    public void write(int b) throws IOException {
        getTopStream().write(b);
        ++bytesWritten;
    }

    public long bytesWritten() {
        return bytesWritten;
    }

    private long bytesWritten;
}

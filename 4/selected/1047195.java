package org.prevayler.implementation;

import java.io.*;

/** A FileOutputStream that counts the number of bytes written and forces all buffers to synchronize with the underlying device when flushed.
*/
final class ByteCountStream extends FileOutputStream {

    public ByteCountStream(File file) throws IOException {
        super(file);
    }

    public void flush() throws IOException {
        super.flush();
        getChannel().force(false);
    }

    public void write(byte[] b) throws IOException {
        super.write(b);
        bytesWritten += b.length;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        bytesWritten += len;
    }

    public void write(int b) throws IOException {
        super.write(b);
        ++bytesWritten;
    }

    public long bytesWritten() {
        return bytesWritten;
    }

    private long bytesWritten;
}

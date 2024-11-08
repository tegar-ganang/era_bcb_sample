package net.community.chest.io.output;

import java.io.Closeable;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channel;
import net.community.chest.io.IOAccessEmbedder;
import net.community.chest.io.OptionallyCloseable;

/**
 * Copyright 2007 as per GPLv2
 * 
 * Embeds an {@link OutputStream} while also implementing the
 * {@link OptionallyCloseable} interface
 * 
 * @author Lyor G.
 * @since Jun 13, 2007 4:34:25 PM
 */
public class OutputStreamEmbedder extends FilterOutputStream implements OptionallyCloseable, IOAccessEmbedder<OutputStream>, Channel {

    @Override
    public boolean isMutableRealClosure() {
        return true;
    }

    private boolean _realClosure;

    @Override
    public boolean isRealClosure() {
        return _realClosure;
    }

    @Override
    public void setRealClosure(boolean enabled) throws UnsupportedOperationException {
        _realClosure = enabled;
    }

    @Override
    public OutputStream getEmbeddedAccess() {
        return this.out;
    }

    @Override
    public void setEmbeddedAccess(OutputStream c) throws IOException {
        this.out = c;
    }

    @Override
    public boolean isOpen() {
        return (getEmbeddedAccess() != null);
    }

    public OutputStreamEmbedder(OutputStream outStream, boolean realClosure) {
        super(outStream);
        _realClosure = realClosure;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (!isOpen()) throw new IOException("write(" + off + "/" + len + ") not open");
        if (null == this.out) throw new IOException("write(" + off + "/" + len + ") real stream already closed");
        this.out.write(b, off, len);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(int val) throws IOException {
        if (!isOpen()) throw new IOException("write(" + val + ") not open");
        if (null == this.out) throw new IOException("write(" + val + ") real stream already closed");
        this.out.write(val);
    }

    @Override
    public void flush() throws IOException {
        if (!isOpen()) throw new IOException("flush() not open");
        if (null == this.out) throw new IOException("No stream to flush");
        super.flush();
    }

    @Override
    public void close() throws IOException {
        final Closeable s = getEmbeddedAccess();
        if (s != null) {
            try {
                if (isRealClosure()) s.close();
            } finally {
                setEmbeddedAccess(null);
            }
        }
    }
}

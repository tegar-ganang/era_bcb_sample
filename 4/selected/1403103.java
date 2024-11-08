package com.griddynamics.openspaces.convergence.utils.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RedirectingInputStream extends FilterInputStream implements RedirectibleInput {

    final byte[] buf;

    final boolean autoFlush;

    final boolean autoClose;

    static final int DEFAULT_BUF_LEN = 2048;

    public RedirectingInputStream(InputStream in) {
        this(in, DEFAULT_BUF_LEN);
    }

    public RedirectingInputStream(InputStream in, int buflen) {
        this(in, true, false, buflen);
    }

    public RedirectingInputStream(InputStream in, boolean autoFlush, boolean autoClose) {
        this(in, autoFlush, autoClose, DEFAULT_BUF_LEN);
    }

    public RedirectingInputStream(InputStream in, boolean autoFlush, boolean autoClose, int buflen) {
        super(in);
        this.autoFlush = autoFlush;
        this.autoClose = autoClose;
        this.buf = new byte[buflen];
    }

    public int redirect(OutputStream out, int len) throws IOException {
        int read = read(buf);
        if (read < 0) {
            if (autoClose) out.close();
        } else {
            out.write(buf, 0, read);
            if (autoFlush) out.flush();
        }
        return read;
    }

    public int redirectAll(OutputStream out) throws IOException {
        int total = 0;
        int read;
        while (true) {
            read = read(buf);
            if (read < 0) {
                if (autoClose) out.close();
                return total;
            }
            out.write(buf, 0, read);
            if (autoFlush) out.flush();
            total += read;
        }
    }
}

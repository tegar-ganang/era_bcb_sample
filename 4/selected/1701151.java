package org.allcolor.xml.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * DOCUMENT ME!
 *
 * @author Quentin Anciaux
 */
public class CInputStreamBuffer extends InputStream {

    /** DOCUMENT ME! */
    boolean booMark = false;

    /** DOCUMENT ME! */
    private ByteArrayOutputStream bout = new ByteArrayOutputStream(2048);

    /** DOCUMENT ME! */
    private InputStream in;

    /**
	 * Creates a new CInputStreamBuffer object.
	 *
	 * @param in DOCUMENT ME!
	 */
    public CInputStreamBuffer(final InputStream in) {
        super();
        this.in = in;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    public int available() throws IOException {
        return in.available() + bout.toByteArray().length;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    public void close() throws IOException {
        in.close();
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param readlimit DOCUMENT ME!
	 */
    public void mark(final int readlimit) {
        booMark = true;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public boolean markSupported() {
        return true;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    public int read() throws IOException {
        byte buffer[] = new byte[1];
        int read = read(buffer, 0, 1);
        if (read != -1) {
            return buffer[0];
        }
        return -1;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param b DOCUMENT ME!
	 * @param off DOCUMENT ME!
	 * @param len DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    public int read(final byte b[], final int off, int len) throws IOException {
        if (!booMark && (bout.toByteArray().length > 0)) {
            byte array[] = bout.toByteArray();
            if (len > array.length) {
                len = array.length;
            }
            System.arraycopy(array, 0, b, off, len);
            bout.reset();
            if (array.length > len) {
                bout.write(array, len, array.length - len);
            }
            return len;
        }
        int read = in.read(b, off, len);
        if (booMark && (read != -1)) {
            bout.write(b, off, read);
        }
        return read;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param b DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    public int read(final byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    /**
	 * DOCUMENT ME!
	 */
    public void reset() {
        booMark = false;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param n DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    public long skip(final long n) throws IOException {
        return in.skip(n);
    }
}

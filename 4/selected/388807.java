package alto.lang.buffer;

import alto.lang.Buffer;

/**
 * <p> A buffer implementation of {@link
 * alto.sys.IO}. </p>
 * 
 * <p> <i>N.B.</i> Not MT SAFE, read and write position states are
 * internal to this buffer. </p>
 */
public class Abstract extends alto.io.u.Bbuf implements alto.sys.IO.Edge, Buffer {

    protected static final alto.io.Uri UriDefault = new alto.io.u.Uri("buffer:default");

    protected long last;

    protected java.lang.String lastString;

    protected InputStream in;

    protected OutputStream out;

    public Abstract() {
        super();
    }

    public Abstract(byte[] content) {
        super(content);
    }

    public alto.io.Uri getUri() {
        return UriDefault;
    }

    public java.nio.channels.ReadableByteChannel openChannelReadable() {
        return null;
    }

    public java.nio.channels.ReadableByteChannel getChannelReadable() {
        return null;
    }

    public long lastModified() {
        return last;
    }

    public java.lang.String lastModifiedString() {
        java.lang.String lastString = this.lastString;
        if (null == lastString) {
            long last = this.last;
            if (0L < last) {
                lastString = alto.lang.Date.ToString(last);
                this.lastString = lastString;
            }
        }
        return lastString;
    }

    public long getLastModified() {
        return last;
    }

    public java.lang.String getLastModifiedString() {
        return this.lastModifiedString();
    }

    public java.io.InputStream openInputStream() throws java.io.IOException {
        return (this.in = new InputStream(this));
    }

    public alto.io.Input openInput() throws java.io.IOException {
        return (this.in = new InputStream(this));
    }

    public java.io.InputStream getInputStream() throws java.io.IOException {
        InputStream in = this.in;
        if (null == in) {
            in = new InputStream(this);
            this.in = in;
        }
        return in;
    }

    public alto.io.Input getInput() throws java.io.IOException {
        InputStream in = this.in;
        if (null == in) {
            in = new InputStream(this);
            this.in = in;
        }
        return in;
    }

    public java.nio.channels.WritableByteChannel openChannelWritable() {
        return null;
    }

    public java.nio.channels.WritableByteChannel getChannelWritable() {
        return null;
    }

    public boolean setLastModified(long last) {
        this.last = last;
        return true;
    }

    public java.io.OutputStream openOutputStream() throws java.io.IOException {
        return (this.out = new OutputStream(this));
    }

    public alto.io.Output openOutput() throws java.io.IOException {
        return (this.out = new OutputStream(this));
    }

    public java.io.OutputStream getOutputStream() throws java.io.IOException {
        OutputStream out = this.out;
        if (null == out) {
            out = new OutputStream(this);
            this.out = out;
        }
        return out;
    }

    public alto.io.Output getOutput() throws java.io.IOException {
        OutputStream out = this.out;
        if (null == out) {
            out = new OutputStream(this);
            this.out = out;
        }
        return out;
    }

    public void close() throws java.io.IOException {
        this.in = null;
        this.out = null;
    }

    public byte[] getBuffer() {
        try {
            return this.toByteArray();
        } catch (java.io.IOException notreached) {
            throw new alto.sys.Error.State();
        }
    }

    public int getBufferLength() {
        try {
            return this.length();
        } catch (java.io.IOException notreached) {
            throw new alto.sys.Error.State();
        }
    }

    public CharSequence getCharContent(boolean igEncErr) throws java.io.IOException {
        byte[] bits = this.getBuffer();
        if (null == bits) return null; else {
            char[] cary = alto.io.u.Utf8.decode(bits);
            return new String(cary, 0, cary.length);
        }
    }
}

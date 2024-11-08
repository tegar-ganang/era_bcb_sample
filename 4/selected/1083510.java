package jaxlib.io.stream.adapter;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import jaxlib.io.stream.XDataOutput;
import jaxlib.io.stream.XOutputStream;

/**
 * Handles a standard {@link java.io.DataOutput} as a {@link jaxlib.io.stream.XDataOutput}.
 * <p>
 * Methods inherited from the {@link DataOutput}, {@link XDataOutput} or {@link Closeable} interface or from the {@link OutputStream} class are redirected 
 * to the underlying stream if it implements the interface.
 * Otherwise those methods behave exactly like documented by <code>XDataOutput</code>.
 * </p><p>
 * This class is intented to be used for "extending" other stream classes where subclassing is not apprecitable, without loosing the behaviour and 
 * inbuild optimizations of underlying stream implementations.
 * <p>
 * <i>Note:</i> A call to {@link #closeInstance()} closes the <tt>AdapterDataOutput</tt> but not the
 * stream it delegates to.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: AdapterDataOutput.java,v 1.2 2004/09/01 00:18:21 joerg_wassmer Exp $
 */
public class AdapterDataOutput extends XOutputStream implements XDataOutput {

    public static OutputStream asOutputStream(DataOutput out) {
        if (out instanceof OutputStream) return (OutputStream) out; else return new AdapterDataOutput(out);
    }

    public static XDataOutput asXDataOutput(DataOutput out) {
        if (out instanceof XDataOutput) return (XDataOutput) out; else return new AdapterDataOutput(out);
    }

    public static XOutputStream asXOutputStream(DataOutput out) {
        if (out instanceof XOutputStream) return (XOutputStream) out; else return new AdapterDataOutput(out);
    }

    private DataOutput out;

    private XDataOutput xout;

    /**
   * Normally only subclasses are calling this constructor, other callers should use one of the static factory methods.
   * <p>
   * You may want to use this constructor to create an <code>AdapterDataOutput</code> which hides the concrete implementation of the underlying stream.
   * This may be usefull when passing stream instances to untrusted code.
   * </p>
   *
   * @since JaXLib 1.0
   */
    public AdapterDataOutput(DataOutput out) {
        super();
        setOutImpl(out);
    }

    private void setOutImpl(DataOutput out) {
        this.out = out;
        this.xout = (out instanceof XDataOutput) ? (XDataOutput) out : null;
    }

    final DataOutput ensureOpen() throws IOException {
        DataOutput out = this.out;
        if (out == null) throw new ClosedChannelException();
        return out;
    }

    /**
   * Returns the stream this <tt>AdapterDataOutput</tt> delegates to.
   *
   * @since JaXLib 1.0
   */
    protected DataOutput getOut() {
        return this.out;
    }

    /**
   * Set the stream this <tt>AdapterDataOutput</tt> delegates to.
   *
   * @since JaXLib 1.0
   */
    protected void setOut(DataOutput out) throws IOException {
        setOutImpl(out);
    }

    @Override
    public void close() throws IOException {
        DataOutput out = this.out;
        if (out != null) {
            closeInstance();
            if (out instanceof Closeable) ((Closeable) out).close();
        }
    }

    @Override
    public void closeInstance() throws IOException {
        try {
            flush();
        } finally {
            setOut(null);
        }
    }

    @Override
    public void flush() throws IOException {
        if (this.out instanceof Flushable) ((Flushable) this.out).flush();
    }

    @Override
    public boolean isOpen() {
        return this.out != null;
    }

    @Override
    public long transferFrom(InputStream in, long maxCount) throws IOException {
        if (this.xout == null) return super.transferFrom(in, maxCount); else return this.xout.transferFrom(in, maxCount);
    }

    @Override
    public void transferBytesFullyFrom(DataInput in, long maxCount) throws IOException {
        if (this.xout == null) super.transferBytesFullyFrom(in, maxCount); else this.xout.transferBytesFullyFrom(in, maxCount);
    }

    @Override
    public void write(int b) throws IOException {
        ensureOpen().write(b);
    }

    @Override
    public void write(byte[] source, int off, int len) throws IOException {
        ensureOpen().write(source, off, len);
    }

    @Override
    public int write(ByteBuffer source) throws IOException {
        if (this.xout != null) return this.xout.write(source); else return super.write(source);
    }

    @Override
    public int write(CharBuffer source) throws IOException {
        if (this.xout != null) return this.xout.write(source); else return super.write(source);
    }

    @Override
    public int write(DoubleBuffer source) throws IOException {
        if (this.xout != null) return this.xout.write(source); else return super.write(source);
    }

    @Override
    public int write(FloatBuffer source) throws IOException {
        if (this.xout != null) return this.xout.write(source); else return super.write(source);
    }

    @Override
    public int write(IntBuffer source) throws IOException {
        if (this.xout != null) return this.xout.write(source); else return super.write(source);
    }

    @Override
    public int write(LongBuffer source) throws IOException {
        if (this.xout != null) return this.xout.write(source); else return super.write(source);
    }

    @Override
    public int write(ShortBuffer source) throws IOException {
        if (this.xout != null) return this.xout.write(source); else return super.write(source);
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        ensureOpen().writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        ensureOpen().writeByte(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        ensureOpen().writeChar(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        ensureOpen().writeDouble(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        ensureOpen().writeFloat(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        ensureOpen().writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        ensureOpen().writeLong(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        ensureOpen().writeShort(v);
    }

    @Override
    public void writeBytes(String source) throws IOException {
        ensureOpen().writeBytes(source);
    }

    @Override
    public void writeChars(String source) throws IOException {
        ensureOpen().writeChars(source);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        ensureOpen().writeUTF(s);
    }
}

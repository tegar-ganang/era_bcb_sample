package jaxlib.io.stream.adapter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import jaxlib.io.stream.ByteOrders;
import jaxlib.io.stream.XDataOutput;
import jaxlib.io.stream.XOutputStream;

/**
 * Handles a standard {@link java.io.OutputStream} as a {@link jaxlib.io.stream.XOutputStream}.
 * <p>
 * Methods inherited from the {@link DataOutput} or the {@link XDataOutput} interface are redirected to the underlying stream if it implements the interface.
 * Otherwise those methods behave exactly like documented by <code>XOutputStream</code>.
 * </p><p>
 * This class is intented to be used for "extending" other stream classes where subclassing is not apprecitable, without loosing the behaviour and 
 * inbuild optimizations of underlying stream implementations.
 * <p>
 * <i>Note:</i> A call to {@link #closeInstance()} closes the <tt>AdapterOutputStream</tt> but not the
 * stream it delegates to.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: AdapterOutputStream.java,v 1.3 2004/11/04 21:24:01 joerg_wassmer Exp $
 */
public class AdapterOutputStream extends XOutputStream implements XDataOutput {

    public static DataOutput asDataOutput(OutputStream delegate) {
        if (delegate instanceof DataOutput) return (DataOutput) delegate; else return new AdapterOutputStream(delegate);
    }

    public static XDataOutput asXDataOutput(OutputStream delegate) {
        if (delegate instanceof XDataOutput) return (XDataOutput) delegate; else return new AdapterOutputStream(delegate);
    }

    public static XOutputStream asXOutputStream(OutputStream delegate) {
        if (delegate instanceof XOutputStream) return (XOutputStream) delegate; else return new AdapterOutputStream(delegate);
    }

    private OutputStream out;

    private DataOutput dout;

    private FileOutputStream fout;

    private XDataOutput xout;

    /**
   * Normally only subclasses are calling this constructor, other callers should use one of the static factory methods.
   * <p>
   * You may want to use this constructor to create an <code>AdapterOutputStream</code> which hides the 
   * concrete implementation of the underlying stream.
   * This may be usefull when passing stream instances to untrusted code.
   * </p>
   *
   * @since JaXLib 1.0
   */
    public AdapterOutputStream(OutputStream out) {
        super();
        setOutImpl(out);
    }

    private void setOutImpl(OutputStream out) {
        this.out = out;
        this.dout = (out instanceof DataOutput) ? (DataOutput) out : null;
        this.fout = (out instanceof FileOutputStream) ? (FileOutputStream) out : null;
        this.xout = (out instanceof XDataOutput) ? (XDataOutput) out : null;
    }

    final OutputStream ensureOpen() throws IOException {
        OutputStream out = this.out;
        if (out == null) throw new ClosedChannelException();
        return out;
    }

    /**
   * Returns the stream this <tt>AdapterOutputStream</tt> delegates to.
   *
   * @since JaXLib 1.0
   */
    protected OutputStream getOut() {
        return this.out;
    }

    /**
   * Set the stream this <tt>AdapterOutputStream</tt> delegates to.
   *
   * @since JaXLib 1.0
   */
    protected void setOut(OutputStream out) throws IOException {
        setOutImpl(out);
    }

    @Override
    public void close() throws IOException {
        OutputStream out = this.out;
        if (out != null) {
            closeInstance();
            out.close();
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
        OutputStream out = this.out;
        if (out != null) out.flush();
    }

    @Override
    public boolean isOpen() {
        OutputStream out = this.out;
        return (out != null) && (!(out instanceof Channel) || ((Channel) out).isOpen());
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
    public void writeBoolean(boolean v) throws IOException {
        if (this.dout == null) ensureOpen().write(v ? 1 : 0); else this.dout.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        if (this.dout == null) ensureOpen().write(v); else this.dout.writeByte(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        if (this.dout == null) ByteOrders.BIG_ENDIAN.writeChar(ensureOpen(), v); else this.dout.writeChar(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        if (this.dout == null) ByteOrders.BIG_ENDIAN.writeDouble(ensureOpen(), v); else this.dout.writeDouble(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        if (this.dout == null) ByteOrders.BIG_ENDIAN.writeFloat(ensureOpen(), v); else this.dout.writeFloat(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        if (this.dout == null) ByteOrders.BIG_ENDIAN.writeInt(ensureOpen(), v); else this.dout.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        if (this.dout == null) ByteOrders.BIG_ENDIAN.writeLong(ensureOpen(), v); else this.dout.writeLong(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        if (this.dout == null) ByteOrders.BIG_ENDIAN.writeShort(ensureOpen(), v); else this.dout.writeShort(v);
    }

    @Override
    public void write(byte[] source, int off, int len) throws IOException {
        ensureOpen().write(source, off, len);
    }

    @Override
    public int write(ByteBuffer source) throws IOException {
        if (this.xout != null) return this.xout.write(source); else if (this.fout != null) {
            int count = source.remaining();
            while (source.hasRemaining()) this.fout.getChannel().write(source);
            return count;
        } else return super.write(source);
    }

    @Override
    public int write(CharBuffer source) throws IOException {
        return (this.xout == null) ? super.write(source) : this.xout.write(source);
    }

    @Override
    public int write(DoubleBuffer source) throws IOException {
        return (this.xout == null) ? super.write(source) : this.xout.write(source);
    }

    @Override
    public int write(FloatBuffer source) throws IOException {
        return (this.xout == null) ? super.write(source) : this.xout.write(source);
    }

    @Override
    public int write(IntBuffer source) throws IOException {
        return (this.xout == null) ? super.write(source) : this.xout.write(source);
    }

    @Override
    public int write(LongBuffer source) throws IOException {
        return (this.xout == null) ? super.write(source) : this.xout.write(source);
    }

    @Override
    public int write(ShortBuffer source) throws IOException {
        return (this.xout == null) ? super.write(source) : this.xout.write(source);
    }

    @Override
    public void writeBytes(String s) throws IOException {
        if (this.dout != null) this.dout.writeBytes(s); else super.writeBytes(s);
    }

    @Override
    public void writeChars(String s) throws IOException {
        if (this.dout != null) this.dout.writeChars(s); else super.writeChars(s);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        if (this.dout != null) this.dout.writeUTF(s); else super.writeUTF(s);
    }
}

package jaxlib.io.stream.adapter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.FileInputStream;
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
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import jaxlib.io.stream.IOStreams;
import jaxlib.io.stream.XDataInput;
import jaxlib.io.stream.XInputStream;

/**
 * Handles a standard {@link java.io.InputStream} as a {@link jaxlib.io.stream.XInputStream}.
 * <p>
 * Methods inherited from the {@link DataInput} or the {@link XDataInput} interface are redirected to the underlying stream if it implements the interface.
 * Otherwise those methods behave exactly like documented by <code>XInputStream</code>.
 * </p><p>
 * This class is intented to be used for "extending" other stream classes where subclassing is not apprecitable, without loosing the behaviour and 
 * inbuild optimizations of underlying stream implementations.
 * </p><p>
 * <i>Note:</i> A call to {@link #closeInstance()} closes the <tt>AdapterInputStream</tt> but not the
 * stream it delegates to.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: AdapterInputStream.java,v 1.3 2004/09/01 00:18:21 joerg_wassmer Exp $
 */
public class AdapterInputStream extends XInputStream {

    public static DataInput asDataInput(InputStream delegate) {
        if (delegate instanceof DataInput) return (DataInput) delegate; else return new AdapterInputStream(delegate);
    }

    public static XDataInput asXDataInput(InputStream delegate) {
        if (delegate instanceof XDataInput) return (XDataInput) delegate; else return new AdapterInputStream(delegate);
    }

    public static XInputStream asXInputStream(InputStream delegate) {
        if (delegate instanceof XInputStream) return (XInputStream) delegate; else return new AdapterInputStream(delegate);
    }

    private InputStream in;

    private DataInput din;

    private XDataInput xin;

    private FileInputStream fin;

    /**
   * Normally only subclasses are calling this constructor.
   * <p>
   * Other callers may use it to create an <code>AdapterInputStream</code> which hides the concrete 
   * implementation of the underlying stream.
   * This may be usefull when passing stream instances to untrusted code.
   * </p>
   *
   * @since JaXLib 1.0
   */
    public AdapterInputStream(InputStream in) {
        super();
        setInImpl(in);
    }

    private void ensureOpen() throws ClosedChannelException {
        if (this.in == null) throw new ClosedChannelException();
    }

    private void setInImpl(InputStream in) {
        this.in = in;
        this.din = (in instanceof DataInput) ? (DataInput) in : null;
        this.fin = (in instanceof FileInputStream) ? (FileInputStream) in : null;
        this.xin = (in instanceof XDataInput) ? (XDataInput) in : null;
    }

    /**
   * Returns the stream this <tt>AdapterInputStream</tt> delegates to.
   *
   * @since JaXLib 1.0
   */
    protected InputStream getIn() {
        return this.in;
    }

    /**
   * Sets the stream this <tt>AdapterInputStream</tt> delegates to.
   *
   * @since JaXLib 1.0
   */
    protected void setIn(InputStream in) throws IOException {
        setInImpl(in);
    }

    @Override
    public int available() throws IOException {
        InputStream in = this.in;
        if (in == null) throw new ClosedChannelException();
        return in.available();
    }

    @Override
    public void close() throws IOException {
        InputStream in = this.in;
        if (in != null) {
            closeInstance();
            in.close();
        }
    }

    @Override
    public void closeInstance() throws IOException {
        setIn(null);
    }

    @Override
    public boolean isOpen() {
        InputStream in = this.in;
        return (in != null) && (!(in instanceof Channel) || ((Channel) in).isOpen());
    }

    @Override
    public void mark(int readLimit) {
        if (this.in != null) this.in.mark(readLimit);
    }

    @Override
    public boolean markSupported() {
        return (this.in == null) ? false : this.in.markSupported();
    }

    @Override
    public void reset() throws IOException {
        ensureOpen();
        this.in.reset();
    }

    @Override
    public int read() throws IOException {
        ensureOpen();
        return this.in.read();
    }

    @Override
    public boolean readBoolean() throws IOException {
        return (this.din == null) ? super.readBoolean() : this.din.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return (this.din == null) ? super.readByte() : this.din.readByte();
    }

    @Override
    public char readChar() throws IOException {
        return (this.din == null) ? super.readChar() : this.din.readChar();
    }

    @Override
    public double readDouble() throws IOException {
        return (this.din == null) ? super.readDouble() : this.din.readDouble();
    }

    @Override
    public float readFloat() throws IOException {
        return (this.din == null) ? super.readFloat() : this.din.readFloat();
    }

    @Override
    public int readInt() throws IOException {
        return (this.din == null) ? super.readInt() : this.din.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return (this.din == null) ? super.readLong() : this.din.readLong();
    }

    @Override
    public short readShort() throws IOException {
        return (this.din == null) ? super.readShort() : this.din.readShort();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return (this.din == null) ? super.readUnsignedByte() : this.din.readUnsignedByte();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return (this.din == null) ? super.readUnsignedShort() : this.din.readUnsignedShort();
    }

    @Override
    public int read(ByteBuffer dest) throws IOException {
        ensureOpen();
        if (this.xin != null) return this.xin.read(dest); else if (this.fin != null) return this.fin.getChannel().read(dest); else return super.read(dest);
    }

    @Override
    public int read(byte[] dest, int off, int len) throws IOException {
        ensureOpen();
        return this.in.read(dest, off, len);
    }

    @Override
    public void readFully(byte[] dest, int off, int len) throws IOException {
        ensureOpen();
        if (this.din != null) this.din.readFully(dest, off, len); else if (this.fin != null) readFully(ByteBuffer.wrap(dest, off, len)); else super.readFully(dest, off, len);
    }

    @Override
    public void readFully(ByteBuffer dest) throws IOException {
        if (this.xin == null) super.readFully(dest); else this.xin.readFully(dest);
    }

    @Override
    public void readFully(CharBuffer dest) throws IOException {
        if (this.xin == null) super.readFully(dest); else this.xin.readFully(dest);
    }

    @Override
    public void readFully(DoubleBuffer dest) throws IOException {
        if (this.xin == null) super.readFully(dest); else this.xin.readFully(dest);
    }

    @Override
    public void readFully(FloatBuffer dest) throws IOException {
        if (this.xin == null) super.readFully(dest); else this.xin.readFully(dest);
    }

    @Override
    public void readFully(IntBuffer dest) throws IOException {
        if (this.xin == null) super.readFully(dest); else this.xin.readFully(dest);
    }

    @Override
    public void readFully(LongBuffer dest) throws IOException {
        if (this.xin == null) super.readFully(dest); else this.xin.readFully(dest);
    }

    @Override
    public void readFully(ShortBuffer dest) throws IOException {
        if (this.xin == null) super.readFully(dest); else this.xin.readFully(dest);
    }

    @Deprecated
    public String readLine() throws IOException {
        return (this.din == null) ? super.readLine() : this.din.readLine();
    }

    @Override
    public String readUTF() throws IOException {
        return (this.din == null) ? super.readUTF() : this.din.readUTF();
    }

    @Override
    public long skip(long count) throws IOException {
        ensureOpen();
        return this.in.skip(count);
    }

    @Override
    public long transferBytesTo(DataOutput dest, long maxCount) throws IOException {
        return (this.xin == null) ? super.transferBytesTo(dest, maxCount) : this.xin.transferBytesTo(dest, maxCount);
    }

    @Override
    public long transferTo(OutputStream dest, long maxCount) throws IOException {
        ensureOpen();
        return (this.xin == null) ? IOStreams.transfer(this.in, dest, maxCount) : this.xin.transferTo(dest, maxCount);
    }

    @Override
    public long transferToByteChannel(WritableByteChannel dest, long maxCount) throws IOException {
        return (this.xin == null) ? super.transferToByteChannel(dest, maxCount) : this.xin.transferToByteChannel(dest, maxCount);
    }
}

package jaxlib.io.stream;

import java.io.DataInput;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import jaxlib.io.stream.adapter.AdapterDataOutput;
import jaxlib.jaxlib_private.CheckArg;
import jaxlib.jaxlib_private.io.CheckIOArg;

/**
 * Provides an extented version of {@link java.io.OutputStream}.
 * <p>
 * By default <tt>XOutputStream</tt> methods are <b>not</b> threadsafe. 
 * If you need a threadsafe <tt>XOutputStream</tt> you may use {@link jaxlib.io.stream.concurrent.SynchronizedXOutputStream}.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: XOutputStream.java,v 1.2 2004/09/14 19:59:39 joerg_wassmer Exp $
 */
public abstract class XOutputStream extends OutputStream implements XDataOutput {

    protected XOutputStream() {
        super();
    }

    public abstract void closeInstance() throws IOException;

    public abstract boolean isOpen();

    public abstract void write(int b) throws IOException;

    public void close() throws IOException {
        closeInstance();
    }

    public long transferFrom(InputStream in, long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) return 0;
        long count = 0;
        if (maxCount < 0) {
            for (int b; (b = in.read()) >= 0; ) {
                write(b);
                count++;
            }
        } else {
            for (int b; (count < maxCount) && ((b = in.read()) >= 0); ) {
                write(b);
                count++;
            }
        }
        return count;
    }

    public void transferBytesFullyFrom(DataInput in, long count) throws IOException {
        DataIOImpl.transferBytesFullyToStream(in, this, count);
    }

    public void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    public void writeByte(int v) throws IOException {
        write(v);
    }

    public void writeChar(int v) throws IOException {
        write((v >>> 8) & 0xFF);
        write((v >>> 0) & 0xFF);
    }

    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    public void writeInt(int v) throws IOException {
        write((v >>> 24) & 0xFF);
        write((v >>> 16) & 0xFF);
        write((v >>> 8) & 0xFF);
        write((v >>> 0) & 0xFF);
    }

    public void writeLong(long v) throws IOException {
        write((int) (v >>> 56) & 0xFF);
        write((int) (v >>> 48) & 0xFF);
        write((int) (v >>> 40) & 0xFF);
        write((int) (v >>> 32) & 0xFF);
        write((int) (v >>> 24) & 0xFF);
        write((int) (v >>> 16) & 0xFF);
        write((int) (v >>> 8) & 0xFF);
        write((int) (v >>> 0) & 0xFF);
    }

    public void writeShort(int v) throws IOException {
        writeChar(v);
    }

    public void write(byte[] source) throws IOException {
        write(source, 0, source.length);
    }

    public void write(byte[] source, int off, int len) throws IOException {
        CheckIOArg.range(source, off, len);
        while (len-- > 0) write(source[off++]);
    }

    public int write(ByteBuffer source) throws IOException {
        final int count = source.remaining();
        if (source.hasArray()) {
            write(source.array(), source.arrayOffset() + source.position(), count);
            source.position(source.limit());
        } else {
            int remaining = count;
            while (remaining-- > 0) write(source.get());
        }
        return count;
    }

    public int write(CharBuffer source) throws IOException {
        final int count = source.remaining();
        for (int r = count; --r >= 0; ) writeChar(source.get());
        return count;
    }

    public int write(DoubleBuffer source) throws IOException {
        final int count = source.remaining();
        for (int r = count; --r >= 0; ) writeDouble(source.get());
        return count;
    }

    public int write(FloatBuffer source) throws IOException {
        final int count = source.remaining();
        for (int r = count; --r >= 0; ) writeFloat(source.get());
        return count;
    }

    public int write(IntBuffer source) throws IOException {
        final int count = source.remaining();
        for (int r = count; --r >= 0; ) writeInt(source.get());
        return count;
    }

    public int write(LongBuffer source) throws IOException {
        final int count = source.remaining();
        for (int r = count; --r >= 0; ) writeLong(source.get());
        return count;
    }

    public int write(ShortBuffer source) throws IOException {
        final int count = source.remaining();
        for (int r = count; --r >= 0; ) writeShort(source.get());
        return count;
    }

    public void writeBytes(String s) throws IOException {
        for (int i = 0, hi = s.length(); i < hi; ) write((byte) s.charAt(i++));
    }

    public void writeChars(String s) throws IOException {
        write(CharBuffer.wrap(s));
    }

    public void writeUTF(String str) throws IOException {
        ByteOrders.BIG_ENDIAN.writeUTFImpl(this, this, str);
    }

    final void writeSecurely(byte[] source, int off, int len) throws IOException {
        CheckIOArg.range(source.length, off, len);
        if (len == 0) {
            if (!isOpen()) throw new IOException("stream closed");
        } else writeSecurelyImpl(source, off, len);
    }

    void writeSecurelyImpl(byte[] source, int off, int len) throws IOException {
        if (len < 64) {
            for (int hi = off + len; off < hi; off++) write(source[off]);
        } else {
            int capacity = Math.min(1024, len);
            byte[] a = new byte[capacity];
            while (len > 0) {
                int step = Math.min(capacity, len);
                System.arraycopy(source, off, a, 0, step);
                write(a, 0, step);
                len -= step;
                off += step;
            }
        }
    }
}

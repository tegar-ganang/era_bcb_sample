package jaxlib.io.stream;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import jaxlib.io.channel.FileChannels;
import jaxlib.lang.ReturnValueException;
import jaxlib.util.CheckBounds;

/**
 * An input stream which reads from a {@code ReadableByteChannel}.
 * <p>
 * This class provides similar functionality as the input stream returned by
 * {@link java.nio.channels.Channels#newInputStream(ReadableByteChannel)
 * Channels.newInputStream(ReadableByteChannel)}. In difference, {@code ByteChannelInputStream} applies no
 * thread synchronization. Operations are assuming end of stream if the channel is in non-blocking mode
 * and provides not at least one byte.
 * </p><p>
 * Instances of this class are <b>not</b> threadsafe.
 * If you need a threadsafe stream you may use {@link jaxlib.io.stream.concurrent.SynchronizedXInputStream}.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: ByteChannelInputStream.java 3029 2011-12-29 00:36:48Z joerg_wassmer $
 */
public class ByteChannelInputStream extends XInputStream {

    private static final int WRAP_LIMIT = 8192;

    /**
   * Returns a view of the specified channel as an {@code InputStream}.
   * If the specified channel already is instance of {@code InputStream} then the channel itself,
   * otherwise a new {@code ByteChannelInputStream} is returned.
   * <p>
   * Please note a class may implement different functionality for its {@code InputStream} and
   * {@code ReadableByteChannel} implementation. This method trusts that nobody makes use of such a
   * horrible design.
   * </p>
   *
   * @since JaXLib 1.0
   */
    public static InputStream asInputStream(ReadableByteChannel delegate) {
        if (delegate instanceof InputStream) return (InputStream) delegate; else return new ByteChannelInputStream(delegate);
    }

    /**
   * Returns a view of the specified channel as an {@code XInputStream}.
   * If the specified channel already is instance of {@code XInputStream} then the channel itself,
   * otherwise a new {@code ByteChannelInputStream} is returned.
   *
   * @since JaXLib 1.0
   */
    public static XInputStream asXInputStream(ReadableByteChannel delegate) {
        if (delegate instanceof XInputStream) return (XInputStream) delegate; else return new ByteChannelInputStream(delegate);
    }

    private ReadableByteChannel in;

    private ByteBuffer smallBuffer;

    private ByteBuffer wrappingBuffer;

    /**
   * Creates a new input stream which reads from the specified channel.
   *
   * @since JaXLib 1.0
   */
    public ByteChannelInputStream(final ReadableByteChannel in) {
        super();
        this.in = in;
    }

    private ByteBuffer readNumber(final int len) throws IOException {
        final ReadableByteChannel in = this.in;
        if (in == null) throw new IOException("closed");
        ByteBuffer buf = this.smallBuffer;
        if (buf == null) this.smallBuffer = buf = ByteBuffer.allocate(8);
        buf.limit(len).position(0);
        final int count = in.read(buf);
        if (count == len) {
            return buf;
        } else {
            this.wrappingBuffer = null;
            this.smallBuffer = null;
            if (count != 0) throw new EOFException(); else throw new IllegalBlockingModeException();
        }
    }

    private ByteBuffer smallBuffer(final int len) throws IOException {
        ByteBuffer buf = this.smallBuffer;
        if (buf == null) {
            if (this.in == null) throw new IOException("closed"); else this.smallBuffer = buf = ByteBuffer.allocate(8);
        }
        buf.limit(len).position(0);
        return buf;
    }

    @Override
    public void close() throws IOException {
        final ReadableByteChannel in = this.in;
        if (in != null) {
            Exception ex = null;
            try {
                closeInstance();
            } catch (final Exception t) {
                ex = t;
            } finally {
                this.in = null;
                this.smallBuffer = null;
                this.wrappingBuffer = null;
                try {
                    in.close();
                } catch (final Exception t) {
                    if (ex == null) ex = t;
                }
            }
            if (ex != null) {
                if (ex instanceof IOException) throw (IOException) ex; else if (ex instanceof RuntimeException) throw (RuntimeException) ex; else throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void closeInstance() throws IOException {
        ReadableByteChannel in = this.in;
        if (in != null) {
            this.in = null;
            this.smallBuffer = null;
            this.wrappingBuffer = null;
        }
    }

    @Override
    public boolean isOpen() {
        ReadableByteChannel in = this.in;
        if (in == null) return false; else if (in.isOpen()) return true; else {
            in = null;
            this.in = null;
            this.smallBuffer = null;
            this.wrappingBuffer = null;
            return false;
        }
    }

    @Override
    public final int read() throws IOException {
        final ReadableByteChannel in = this.in;
        if (in == null) throw new IOException("closed");
        final ByteBuffer buf = smallBuffer(1);
        final int count = in.read(buf);
        if (count == 1) {
            return buf.get(0) & 0xff;
        } else if (count == 0) {
            return -1;
        } else if (count < 0) {
            this.smallBuffer = null;
            this.wrappingBuffer = null;
            return -1;
        } else {
            throw new ReturnValueException("in", "read(ByteBuffer)", count, "<= 1");
        }
    }

    @Override
    public final int read(final byte[] a, final int offs, final int len) throws IOException {
        CheckBounds.offset(a, offs, len);
        if (len > 1) {
            final ReadableByteChannel in = this.in;
            if (in == null) throw new IOException("closed");
            ByteBuffer buf;
            if (a.length <= WRAP_LIMIT) {
                buf = this.wrappingBuffer;
                if ((buf == null) || (buf.array() != a)) this.wrappingBuffer = buf = ByteBuffer.wrap(a);
            } else {
                buf = ByteBuffer.wrap(a);
                this.wrappingBuffer = null;
            }
            buf.limit(offs + len).position(offs);
            final int count = in.read(buf);
            if (count > 0) {
                return count;
            } else if (count == 0) {
                return -1;
            } else {
                this.smallBuffer = null;
                this.wrappingBuffer = null;
                return -1;
            }
        } else if (len == 1) {
            final int b = read();
            if (b >= 0) {
                a[offs] = (byte) b;
                return 1;
            } else {
                return -1;
            }
        } else {
            return 0;
        }
    }

    @Override
    public final int read(final ByteBuffer dest) throws IOException {
        final ReadableByteChannel in = this.in;
        if (in == null) throw new IOException("closed");
        final int count = in.read(dest);
        if (count > 0) return count; else if (count == 0) return 0; else {
            this.smallBuffer = null;
            this.wrappingBuffer = null;
            return -1;
        }
    }

    @Override
    public final boolean readBoolean() throws IOException {
        return readNumber(1).get(0) != 0;
    }

    @Override
    public final byte readByte() throws IOException {
        return readNumber(1).get(0);
    }

    @Override
    public final char readChar() throws IOException {
        return readNumber(2).getChar(0);
    }

    @Override
    public final double readDouble() throws IOException {
        return readNumber(8).getDouble(0);
    }

    @Override
    public final float readFloat() throws IOException {
        return readNumber(4).getFloat(0);
    }

    @Override
    public final int readInt() throws IOException {
        return readNumber(4).getInt(0);
    }

    @Override
    public final long readLong() throws IOException {
        return readNumber(8).getLong(0);
    }

    @Override
    public final short readShort() throws IOException {
        return readNumber(2).getShort(0);
    }

    @Override
    public final int readUnsignedByte() throws IOException {
        return readNumber(1).get(0) & 0xff;
    }

    @Override
    public final long readUnsignedInt() throws IOException {
        return readNumber(4).getInt(0) & 0xffffffffL;
    }

    @Override
    public final int readUnsignedShort() throws IOException {
        return readNumber(2).getChar(0);
    }

    @Override
    public long skip(long maxCount) throws IOException {
        if (maxCount == 0) {
            return 0;
        } else {
            final ReadableByteChannel in = this.in;
            if (in == null) throw new IOException("closed");
            if (in instanceof FileChannel) {
                final FileChannel fc = (FileChannel) in;
                final long size = fc.size();
                final long pos = fc.position();
                maxCount = Math.min(maxCount, size - pos);
                if (maxCount > 0) fc.position(pos + maxCount);
                return Math.max(0, maxCount);
            } else {
                return super.skip(maxCount);
            }
        }
    }

    @Override
    public long transferToByteChannel(final WritableByteChannel dest, final long maxCount) throws IOException {
        if (maxCount == 0) {
            return 0;
        } else {
            ReadableByteChannel in = this.in;
            if (in == null) throw new IOException("closed"); else if (in instanceof FileChannel) return FileChannels.transferToByteChannel((FileChannel) in, dest, maxCount); else if (dest instanceof FileChannel) return FileChannels.transferFromByteChannel(in, (FileChannel) dest, maxCount); else return super.transferToByteChannel(dest, maxCount);
        }
    }
}

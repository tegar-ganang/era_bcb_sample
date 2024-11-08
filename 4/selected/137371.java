package jaxlib.io.channel;

import java.io.DataOutput;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import javax.annotation.Nonnull;
import jaxlib.io.stream.adapter.AdapterDataOutput;
import jaxlib.lang.ReturnValueException;
import jaxlib.util.CheckArg;

/**
 * Utilities to work with channels.
 *
 * @see java.nio.channels.Channel
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: IOChannels.java 3029 2011-12-29 00:36:48Z joerg_wassmer $
 */
public class IOChannels {

    protected IOChannels() throws InstantiationException {
        throw new InstantiationException();
    }

    @Nonnull
    public static ReadableByteChannel asReadableByteChannel(final InputStream delegate) {
        if (delegate instanceof ReadableByteChannel) {
            return (ReadableByteChannel) delegate;
        } else if (delegate instanceof FileInputStream) {
            final ReadableByteChannel ch = ((FileInputStream) delegate).getChannel();
            if (ch != null) return ch;
        }
        return new IOChannels.InputStreamChannel(delegate);
    }

    @Nonnull
    public static WritableByteChannel asWritableByteChannel(final OutputStream delegate) {
        if (delegate instanceof WritableByteChannel) {
            return (WritableByteChannel) delegate;
        } else if (delegate instanceof FileOutputStream) {
            final WritableByteChannel ch = ((FileOutputStream) delegate).getChannel();
            if (ch != null) return ch;
        }
        return new IOChannels.OutputStreamChannel(delegate);
    }

    @Nonnull
    public static WritableByteChannel asWritableByteChannel(final DataOutput delegate) {
        if (delegate instanceof WritableByteChannel) return (WritableByteChannel) delegate; else if (delegate instanceof RandomAccessFile) return ((RandomAccessFile) delegate).getChannel(); else if (delegate instanceof OutputStream) return new IOChannels.OutputStreamChannel((OutputStream) delegate); else return new AdapterDataOutput(delegate);
    }

    public static boolean flush(final WritableByteChannel channel) throws IOException {
        if (channel instanceof Flushable) {
            ((Flushable) channel).flush();
            return true;
        } else if (channel instanceof FileChannel) {
            ((FileChannel) channel).force(false);
            return true;
        } else if (channel instanceof SocketChannel) {
            final SocketChannel sh = (SocketChannel) channel;
            if (sh.isBlocking()) {
                sh.socket().getOutputStream().flush();
                return true;
            }
        }
        return false;
    }

    public static int readFully(final ReadableByteChannel in, final ByteBuffer dest) throws IOException {
        final int count = dest.remaining();
        int remaining = dest.remaining();
        while (remaining > 0) {
            final int step = in.read(dest);
            if (step < 0) throw new EOFException(); else if (step > remaining) throw new ReturnValueException(in, "read(ByteBuffer)", step, "<= ", remaining); else if (step == 0) throw new IllegalBlockingModeException(); else remaining -= step;
        }
        return count;
    }

    /**
   * Writes all remaining bytes of specified buffer to the specified channel.
   * Unlike the method {@link WritableByteChannel#write(ByteBuffer) WritableByteChannel.write(ByteBuffer)}
   * this method guarantees that all bytes have been written if this method returns normally.
   *
   * @throws IOException
   *  if an I/O error occurs.
   * @throws IllegalBlockingModeException
   *  if the specified channel is non-blocking and is not instance of {@link OutputByteChannel}.
   *
   * @since JaXLib 1.0
   */
    public static int writeFully(final WritableByteChannel out, final ByteBuffer src) throws IOException {
        final int count = src.remaining();
        if (out instanceof OutputByteChannel) {
            if (((OutputByteChannel) out).writeFully(src) != count) throw new ReturnValueException(out, "writeFully(ByteBuffer)", count, "buffer.remaining");
            if (src.hasRemaining()) throw new ReturnValueException("ByteBuffer.hasRemaining still true", out, "writeFully(ByteBuffer)");
        } else {
            for (int r = count; r > 0; ) {
                final int step = out.write(src);
                if ((step > r) || (step < 0)) throw new ReturnValueException(out, "write(ByteBuffer)", step, "<= ", r, "&& >= 0");
                if (step == 0) throw new IllegalBlockingModeException();
                r -= step;
            }
        }
        return count;
    }

    private static final class InputStreamChannel extends AbstractInterruptibleChannel implements ReadableByteChannel {

        private final Object lock = new Object();

        private final InputStream in;

        private byte buf[] = null;

        InputStreamChannel(InputStream in) {
            super();
            CheckArg.notNull(in, "in");
            this.in = in;
        }

        @Override
        public final int read(final ByteBuffer dst) throws IOException {
            final int len = dst.remaining();
            int totalRead = 0;
            synchronized (this.lock) {
                int bytesRead = 0;
                while (totalRead < len) {
                    if ((totalRead > 0) && !(this.in.available() > 0)) break;
                    final int bytesToRead = Math.min((len - totalRead), 8192);
                    if (dst.hasArray()) {
                        try {
                            begin();
                            bytesRead = this.in.read(dst.array(), dst.arrayOffset() + dst.position(), bytesToRead);
                            if (bytesRead > 0) {
                                dst.position(dst.position() + bytesRead);
                                totalRead += bytesRead;
                            } else {
                                break;
                            }
                        } finally {
                            end(bytesRead > 0);
                        }
                    } else {
                        byte[] buf = this.buf;
                        if ((buf == null) || (buf.length < bytesToRead)) {
                            this.buf = null;
                            buf = new byte[bytesToRead];
                        }
                        try {
                            begin();
                            bytesRead = this.in.read(buf, 0, bytesToRead);
                        } finally {
                            end(bytesRead > 0);
                        }
                        if (bytesRead > 0) totalRead += bytesRead; else break;
                        dst.put(buf, 0, bytesRead);
                        this.buf = buf;
                    }
                }
                if ((bytesRead < 0) && (totalRead == 0)) {
                    this.buf = null;
                    return -1;
                }
                return totalRead;
            }
        }

        @Override
        protected final void implCloseChannel() throws IOException {
            this.buf = null;
            this.in.close();
        }
    }

    private static final class OutputStreamChannel extends AbstractInterruptibleChannel implements WritableByteChannel {

        private final Object lock = new Object();

        private final OutputStream out;

        private byte buf[] = null;

        OutputStreamChannel(final OutputStream out) {
            super();
            CheckArg.notNull(out, "out");
            this.out = out;
        }

        @Override
        public final int write(final ByteBuffer src) throws IOException {
            final int len = src.remaining();
            int totalWritten = 0;
            synchronized (this.lock) {
                while (totalWritten < len) {
                    final int bytesToWrite = Math.min(len - totalWritten, 8192);
                    if (src.hasArray()) {
                        try {
                            begin();
                            this.out.write(src.array(), src.arrayOffset() + src.position(), bytesToWrite);
                            src.position(src.position() + bytesToWrite);
                        } finally {
                            end(bytesToWrite > 0);
                        }
                    } else {
                        byte[] buf = this.buf;
                        if ((buf == null) || (buf.length < bytesToWrite)) {
                            this.buf = null;
                            buf = new byte[bytesToWrite];
                        }
                        src.get(buf, 0, bytesToWrite);
                        try {
                            begin();
                            this.out.write(buf, 0, bytesToWrite);
                        } finally {
                            end(bytesToWrite > 0);
                        }
                        this.buf = buf;
                    }
                    totalWritten += bytesToWrite;
                }
            }
            return totalWritten;
        }

        @Override
        protected final void implCloseChannel() throws IOException {
            this.buf = null;
            this.out.close();
        }
    }
}

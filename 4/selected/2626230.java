package jaxlib.net.ssl;

import java.io.Flushable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import jaxlib.buffer.ByteBuffers;
import jaxlib.io.CloseableInstance;
import jaxlib.io.channel.IOChannels;
import jaxlib.io.channel.OutputByteChannel;
import jaxlib.lang.Bytes;
import jaxlib.util.CheckArg;

/**
 * A byte channel supporting non-blocking IO via the <i>Secure Socket Layer</i> protocol.
 * <p>
 * The instances of this class are not safe to be used by multiple threads concurrently.
 * </p>
 *
 * @author  jw
 * @since   JaXLib 1.0
 * @version $Id: SSLChannel.java 3029 2011-12-29 00:36:48Z joerg_wassmer $
 */
@NotThreadSafe
public class SSLChannel extends Object implements ByteChannel, Flushable, OutputByteChannel, CloseableInstance {

    @CheckForNull
    private SSLEngine engine;

    @CheckForNull
    private ReadableByteChannel in;

    @CheckForNull
    private WritableByteChannel out;

    private boolean eof;

    @CheckForNull
    private ByteBuffer decodedInBuffer;

    @CheckForNull
    private ByteBuffer decodedOutBuffer;

    @CheckForNull
    private ByteBuffer encodedInBuffer;

    @CheckForNull
    private ByteBuffer encodedOutBuffer;

    @CheckForNull
    private SSLEngineResult.HandshakeStatus pendingHandshake;

    public SSLChannel(final SSLEngine engine, final ByteChannel delegate) {
        this(engine, delegate, delegate);
    }

    public SSLChannel(final SSLEngine engine, final ReadableByteChannel in, final WritableByteChannel out) {
        super();
        CheckArg.notNull(in, "in");
        CheckArg.notNull(out, "out");
        CheckArg.notNull(engine, "engine");
        this.engine = engine;
        this.in = in;
        this.out = out;
        this.decodedInBuffer = Bytes.EMPTY_BUFFER;
        this.decodedOutBuffer = Bytes.EMPTY_BUFFER;
        this.encodedInBuffer = Bytes.EMPTY_BUFFER;
        this.encodedOutBuffer = Bytes.EMPTY_BUFFER;
    }

    @Override
    public void close() throws IOException {
        final ReadableByteChannel in = this.in;
        final WritableByteChannel out = this.out;
        IOException ex = null;
        try {
            closeInstance();
        } catch (final IOException x) {
            ex = x;
        }
        if (out != null) {
            try {
                out.close();
            } catch (final IOException x) {
                if (ex == null) ex = x;
            }
        }
        if ((in != null) && (in != out)) {
            try {
                in.close();
            } catch (final IOException x) {
                if (ex == null) ex = x;
            }
        }
        if (ex != null) throw ex;
    }

    @Override
    public void closeInstance() throws IOException {
        if ((this.in != null) || (this.out != null)) {
            IOException ex = null;
            try {
                shutdownOutput();
            } catch (final ClosedChannelException x) {
            } catch (final IOException x) {
                ex = x;
            }
            shutdownInput();
            this.decodedInBuffer = null;
            this.decodedOutBuffer = null;
            this.encodedInBuffer = null;
            this.encodedOutBuffer = null;
            this.in = null;
            this.out = null;
            if (ex != null) throw ex;
        }
    }

    /**
   * Continue with the next step in SSL handshaking.
   *
   * @return
   *  true if and only if after this call there still are more steps pending in the SSL handshake.
   *
   * @since JaXLib 1.0
   */
    public boolean continueHandshake() throws IOException {
        SSLEngineResult.HandshakeStatus h = this.pendingHandshake;
        if (h == null) {
            engine().beginHandshake();
            h = engine().getHandshakeStatus();
            this.pendingHandshake = (h == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) ? SSLEngineResult.HandshakeStatus.FINISHED : h;
        }
        if (h == SSLEngineResult.HandshakeStatus.FINISHED) return false; else {
            handshake(h, null);
            flush();
            return this.pendingHandshake != SSLEngineResult.HandshakeStatus.FINISHED;
        }
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
        IOChannels.flush(out());
    }

    @Override
    public final boolean isOpen() {
        return this.decodedInBuffer != null;
    }

    public final boolean isOutboundBufferEmpty() {
        ByteBuffer buffer = this.decodedOutBuffer;
        if ((buffer != null) && buffer.hasRemaining()) return false;
        buffer = this.encodedOutBuffer;
        if ((buffer != null) && buffer.hasRemaining()) return false;
        return true;
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        CheckArg.notNull(dst, "dst");
        ByteBuffer decodedInBuffer = decodedInBuffer();
        if (!decodedInBuffer.hasRemaining()) {
            if (continueHandshake()) return this.eof ? -1 : 0;
            if (this.eof) return -1;
            decodedInBuffer = null;
            handshake(unwrap());
            decodedInBuffer = decodedInBuffer();
            if (continueHandshake()) return this.eof ? -1 : 0;
            if (this.eof) return -1;
        }
        final int step = ByteBuffers.write(decodedInBuffer, dst);
        if (!decodedInBuffer.hasRemaining() && this.eof) {
            this.decodedInBuffer = Bytes.EMPTY_BUFFER;
            if (step <= 0) return -1;
        }
        return step;
    }

    public void shutdownOutput() throws IOException {
        try {
            final SSLEngine engine = this.engine;
            if (isOpen() && (engine != null) && !engine.isOutboundDone()) {
                flush();
                engine.closeOutbound();
                SSLEngineResult.HandshakeStatus h = engine.getHandshakeStatus();
                this.pendingHandshake = h = (h == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) ? SSLEngineResult.HandshakeStatus.FINISHED : h;
                while (h != SSLEngineResult.HandshakeStatus.FINISHED) {
                    continueHandshake();
                    final SSLEngineResult.HandshakeStatus nh = this.pendingHandshake;
                    if (nh == h) break;
                    h = nh;
                    flush();
                    h = nh;
                }
            }
        } catch (final AsynchronousCloseException ex) {
            throw ex;
        } catch (final ClosedChannelException ex) {
            throw (AsynchronousCloseException) new AsynchronousCloseException().initCause(ex);
        }
    }

    @Override
    public long transferFromByteChannel(final ReadableByteChannel in, final long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        final ByteBuffer buf = prepareWrite();
        if (buf == null) return 0;
        int step = (maxCount < 0) ? buf.remaining() : (int) Math.min(buf.remaining(), maxCount);
        final int lim = buf.limit();
        try {
            buf.limit(buf.position() + step);
            step = Math.max(0, in.read(buf));
        } finally {
            buf.limit(lim);
        }
        if (!buf.hasRemaining()) handshake(wrap());
        return step;
    }

    @Override
    public int write(final ByteBuffer src) throws IOException {
        CheckArg.notNull(src, "src");
        final ByteBuffer buf = prepareWrite();
        if (buf == null) return 0;
        final int count = ByteBuffers.write(src, buf);
        if (!buf.hasRemaining()) handshake(wrap());
        return count;
    }

    @Override
    public int writeFully(final ByteBuffer src) throws IOException {
        final int count = src.remaining();
        while (src.hasRemaining()) write(src);
        return count;
    }

    private ByteBuffer decodedInBuffer() throws ClosedChannelException {
        final ByteBuffer buffer = this.decodedInBuffer;
        if (buffer == null) throw new ClosedChannelException();
        return buffer;
    }

    private ByteBuffer decodedOutBuffer() throws ClosedChannelException {
        final ByteBuffer buffer = this.decodedOutBuffer;
        if (buffer == null) throw new ClosedChannelException();
        return buffer;
    }

    private ByteBuffer encodedInBuffer() throws ClosedChannelException {
        final ByteBuffer buffer = this.encodedInBuffer;
        if (buffer == null) throw new ClosedChannelException();
        return buffer;
    }

    private ByteBuffer encodedOutBuffer() throws ClosedChannelException {
        final ByteBuffer buffer = this.encodedOutBuffer;
        if (buffer == null) throw new ClosedChannelException();
        return buffer;
    }

    private void endIn(final boolean sslClose) throws IOException {
        this.eof = true;
        if (sslClose) {
            this.pendingHandshake = SSLEngineResult.HandshakeStatus.FINISHED;
            this.encodedInBuffer = Bytes.EMPTY_BUFFER;
            ByteBuffer buf = this.decodedInBuffer;
            if ((buf != null) && !buf.hasRemaining()) this.decodedInBuffer = Bytes.EMPTY_BUFFER;
            buf = null;
            final SSLEngine engine = this.engine;
            if (engine != null) engine.closeInbound();
        }
    }

    private void endOut() throws IOException {
        this.decodedOutBuffer = Bytes.EMPTY_BUFFER;
        ByteBuffer buf = this.encodedOutBuffer;
        if ((buf != null) && !buf.hasRemaining()) this.encodedOutBuffer = Bytes.EMPTY_BUFFER;
        buf = null;
        final SSLEngine engine = this.engine;
        if (engine != null) engine.closeOutbound();
    }

    @Nonnull
    private SSLEngine engine() throws ClosedChannelException {
        final SSLEngine engine = this.engine;
        if (engine == null) throw new ClosedChannelException();
        return engine;
    }

    private void flushBuffer() throws IOException {
        flushEncodedBuffer();
        while (true) {
            final int decoded = decodedOutBuffer().remaining();
            if (decoded <= 0) break;
            handshake(wrap());
            flushEncodedBuffer();
            if (decodedOutBuffer().remaining() == decoded) break;
        }
    }

    private void flushEncodedBuffer() throws IOException {
        final ByteBuffer buf = encodedOutBuffer();
        if (buf.hasRemaining()) {
            final WritableByteChannel out = out();
            if (out instanceof OutputByteChannel) {
                try {
                    ((OutputByteChannel) out).writeFully(buf);
                } catch (final IllegalBlockingModeException ex) {
                }
            }
            while (buf.hasRemaining() && ((out.write(buf) > 0) || (IOChannels.flush(out) && (out.write(buf) > 0)))) {
                continue;
            }
            IOChannels.flush(out);
        }
    }

    @Nonnull
    private SSLEngineResult handshake(@Nullable final SSLEngineResult r) throws IOException {
        return handshake(r.getHandshakeStatus(), r);
    }

    @Nonnull
    private SSLEngineResult handshake(SSLEngineResult.HandshakeStatus hs, @Nullable SSLEngineResult r) throws IOException {
        HANDSHAKE: while (true) {
            switch(hs) {
                case FINISHED:
                case NOT_HANDSHAKING:
                    this.pendingHandshake = SSLEngineResult.HandshakeStatus.FINISHED;
                    return r;
                default:
                    break;
            }
            if (r != null) {
                switch(r.getStatus()) {
                    case BUFFER_OVERFLOW:
                    case BUFFER_UNDERFLOW:
                        this.pendingHandshake = r.getHandshakeStatus();
                        return r;
                    default:
                        break;
                }
            }
            while (hs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                engine().getDelegatedTask().run();
                hs = engine().getHandshakeStatus();
            }
            switch(hs) {
                case FINISHED:
                    continue HANDSHAKE;
                case NEED_WRAP:
                    r = wrap();
                    hs = r.getHandshakeStatus();
                    continue HANDSHAKE;
                case NEED_UNWRAP:
                    r = unwrap();
                    hs = r.getHandshakeStatus();
                    continue HANDSHAKE;
                default:
                    throw new AssertionError(hs);
            }
        }
    }

    @Nonnull
    private ReadableByteChannel in() throws ClosedChannelException {
        final ReadableByteChannel in = this.in;
        if (in == null) throw new ClosedChannelException();
        return in;
    }

    @Nonnull
    private WritableByteChannel out() throws ClosedChannelException {
        final WritableByteChannel out = this.out;
        if (out == null) throw new ClosedChannelException();
        return out;
    }

    @CheckForNull
    private ByteBuffer prepareWrite() throws IOException {
        if (continueHandshake()) return null;
        ByteBuffer decodedOutBuffer = decodedOutBuffer();
        if (decodedOutBuffer.hasRemaining()) return decodedOutBuffer;
        final int applicationBufferSize = engine().getSession().getApplicationBufferSize();
        if (decodedOutBuffer.capacity() < applicationBufferSize) {
            if (decodedOutBuffer.position() == 0) {
                if (engine().isOutboundDone()) throw new SSLException("SSL outbound shutdown");
                this.decodedOutBuffer = decodedOutBuffer = Bytes.EMPTY_BUFFER;
            }
            final ByteBuffer newBuffer = ByteBuffer.allocate(applicationBufferSize);
            decodedOutBuffer.flip();
            newBuffer.put(decodedOutBuffer);
            this.decodedOutBuffer = decodedOutBuffer = newBuffer;
        }
        if (decodedOutBuffer.hasRemaining()) return decodedOutBuffer;
        decodedOutBuffer = null;
        handshake(wrap());
        if (continueHandshake()) return null;
        return decodedOutBuffer();
    }

    private void shutdownInput() {
        final SSLEngine engine = this.engine;
        if (engine != null) {
            try {
                engine.closeInbound();
            } catch (final SSLException ex) {
            }
        }
    }

    @Nonnull
    private SSLEngineResult unwrap() throws IOException {
        final int packetSize = engine().getSession().getPacketBufferSize();
        ByteBuffer encodedInBuffer = encodedInBuffer();
        if (encodedInBuffer.capacity() < packetSize) {
            if (!encodedInBuffer.hasRemaining()) {
                if (engine().isInboundDone()) return new SSLEngineResult(SSLEngineResult.Status.CLOSED, engine().getHandshakeStatus(), 0, 0);
                this.encodedInBuffer = encodedInBuffer = Bytes.EMPTY_BUFFER;
            }
            final ByteBuffer newBuffer = ByteBuffer.allocate(packetSize);
            newBuffer.put(encodedInBuffer);
            newBuffer.flip();
            this.encodedInBuffer = encodedInBuffer = newBuffer;
        }
        final int applicationBufferSize = engine().getSession().getApplicationBufferSize();
        ByteBuffer decodedInBuffer = decodedInBuffer();
        if (decodedInBuffer.capacity() < applicationBufferSize) {
            if (!decodedInBuffer.hasRemaining()) this.decodedInBuffer = decodedInBuffer = Bytes.EMPTY_BUFFER;
            final ByteBuffer newBuffer = ByteBuffer.allocate(applicationBufferSize);
            newBuffer.put(decodedInBuffer);
            newBuffer.flip();
            this.decodedInBuffer = decodedInBuffer = newBuffer;
        }
        decodedInBuffer.compact();
        SSLEngineResult r = engine().unwrap(encodedInBuffer, decodedInBuffer);
        decodedInBuffer.flip();
        switch(r.getStatus()) {
            case BUFFER_UNDERFLOW:
                {
                    encodedInBuffer.compact();
                    final int step = in().read(encodedInBuffer);
                    encodedInBuffer.flip();
                    if (step < 0) endIn(false); else if (step > 0) {
                        decodedInBuffer.compact();
                        r = engine().unwrap(encodedInBuffer, decodedInBuffer);
                        decodedInBuffer.flip();
                    }
                }
                break;
            case CLOSED:
                endIn(true);
                break;
            default:
                break;
        }
        return r;
    }

    private SSLEngineResult wrap() throws IOException {
        ByteBuffer decodedOutBuffer = decodedOutBuffer();
        final int packetSize = engine().getSession().getPacketBufferSize();
        ByteBuffer encodedOutBuffer = encodedOutBuffer();
        if (encodedOutBuffer.capacity() < packetSize) {
            if (!encodedOutBuffer.hasRemaining()) {
                if (engine().isOutboundDone()) return new SSLEngineResult(SSLEngineResult.Status.CLOSED, engine().getHandshakeStatus(), 0, 0);
                this.encodedOutBuffer = encodedOutBuffer = Bytes.EMPTY_BUFFER;
            }
            final ByteBuffer newBuffer = ByteBuffer.allocate(packetSize);
            newBuffer.put(encodedOutBuffer);
            newBuffer.flip();
            this.encodedOutBuffer = encodedOutBuffer = newBuffer;
        }
        decodedOutBuffer.flip();
        encodedOutBuffer.compact();
        final SSLEngineResult r = engine().wrap(decodedOutBuffer, encodedOutBuffer);
        try {
            decodedOutBuffer.compact();
            encodedOutBuffer.flip();
            decodedOutBuffer = null;
            final SSLEngineResult.HandshakeStatus oldh = this.pendingHandshake;
            SSLEngineResult.HandshakeStatus h = r.getHandshakeStatus();
            if (h == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) h = SSLEngineResult.HandshakeStatus.FINISHED;
            this.pendingHandshake = h;
            if (encodedOutBuffer.hasRemaining()) {
                if ((h != SSLEngineResult.HandshakeStatus.FINISHED) || (oldh != SSLEngineResult.HandshakeStatus.FINISHED)) {
                    encodedOutBuffer = null;
                    flushEncodedBuffer();
                } else if ((encodedOutBuffer.limit() == encodedOutBuffer.capacity()) || (r.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW)) {
                    out().write(encodedOutBuffer);
                }
            }
        } finally {
            if (r.getStatus() == SSLEngineResult.Status.CLOSED) endOut();
        }
        return r;
    }
}

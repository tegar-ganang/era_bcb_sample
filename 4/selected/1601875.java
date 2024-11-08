package org.apache.tomcat.util.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import java.nio.channels.Selector;
import org.apache.tomcat.util.MutableInteger;

/**
 * 
 * Implementation of a secure socket channel
 * @author Filip Hanik
 * @version 1.0
 */
public class SecureNioChannel extends NioChannel {

    protected ByteBuffer netInBuffer;

    protected ByteBuffer netOutBuffer;

    protected SSLEngine sslEngine;

    protected boolean initHandshakeComplete = false;

    protected HandshakeStatus initHandshakeStatus;

    protected boolean closed = false;

    protected boolean closing = false;

    protected NioSelectorPool pool;

    public SecureNioChannel(SocketChannel channel, SSLEngine engine, ApplicationBufferHandler bufHandler, NioSelectorPool pool) throws IOException {
        super(channel, bufHandler);
        this.sslEngine = engine;
        int appBufSize = sslEngine.getSession().getApplicationBufferSize();
        int netBufSize = sslEngine.getSession().getPacketBufferSize();
        if (netInBuffer == null) netInBuffer = ByteBuffer.allocateDirect(netBufSize);
        if (netOutBuffer == null) netOutBuffer = ByteBuffer.allocateDirect(netBufSize);
        this.pool = pool;
        bufHandler.expand(bufHandler.getReadBuffer(), appBufSize);
        bufHandler.expand(bufHandler.getWriteBuffer(), appBufSize);
        reset();
    }

    public void reset(SSLEngine engine) throws IOException {
        this.sslEngine = engine;
        reset();
    }

    public void reset() throws IOException {
        super.reset();
        netOutBuffer.position(0);
        netOutBuffer.limit(0);
        netInBuffer.position(0);
        netInBuffer.limit(0);
        initHandshakeComplete = false;
        closed = false;
        closing = false;
        sslEngine.beginHandshake();
        initHandshakeStatus = sslEngine.getHandshakeStatus();
    }

    public int getBufferSize() {
        int size = super.getBufferSize();
        size += netInBuffer != null ? netInBuffer.capacity() : 0;
        size += netOutBuffer != null ? netOutBuffer.capacity() : 0;
        return size;
    }

    /**
     * returns true if the network buffer has 
     * been flushed out and is empty
     * @return boolean
     */
    public boolean flush(boolean block, Selector s, long timeout, MutableInteger lastWrite) throws IOException {
        if (!block) {
            flush(netOutBuffer);
        } else {
            pool.write(netOutBuffer, this, s, timeout, block, lastWrite);
        }
        return !netOutBuffer.hasRemaining();
    }

    /**
     * Flushes the buffer to the network, non blocking
     * @param buf ByteBuffer
     * @return boolean true if the buffer has been emptied out, false otherwise
     * @throws IOException
     */
    protected boolean flush(ByteBuffer buf) throws IOException {
        int remaining = buf.remaining();
        if (remaining > 0) {
            int written = sc.write(buf);
            return written >= remaining;
        } else {
            return true;
        }
    }

    /**
     * Performs SSL handshake, non blocking, but performs NEED_TASK on the same thread.<br>
     * Hence, you should never call this method using your Acceptor thread, as you would slow down
     * your system significantly.<br>
     * The return for this operation is 0 if the handshake is complete and a positive value if it is not complete.
     * In the event of a positive value coming back, reregister the selection key for the return values interestOps.
     * @param read boolean - true if the underlying channel is readable
     * @param write boolean - true if the underlying channel is writable
     * @return int - 0 if hand shake is complete, otherwise it returns a SelectionKey interestOps value
     * @throws IOException
     */
    public int handshake(boolean read, boolean write) throws IOException {
        if (initHandshakeComplete) return 0;
        if (!flush(netOutBuffer)) return SelectionKey.OP_WRITE;
        SSLEngineResult handshake = null;
        while (!initHandshakeComplete) {
            switch(initHandshakeStatus) {
                case NOT_HANDSHAKING:
                    {
                        throw new IOException("NOT_HANDSHAKING during handshake");
                    }
                case FINISHED:
                    {
                        initHandshakeComplete = !netOutBuffer.hasRemaining();
                        return initHandshakeComplete ? 0 : SelectionKey.OP_WRITE;
                    }
                case NEED_WRAP:
                    {
                        handshake = handshakeWrap(write);
                        if (handshake.getStatus() == Status.OK) {
                            if (initHandshakeStatus == HandshakeStatus.NEED_TASK) initHandshakeStatus = tasks();
                        } else {
                            throw new IOException("Unexpected status:" + handshake.getStatus() + " during handshake WRAP.");
                        }
                        if (initHandshakeStatus != HandshakeStatus.NEED_UNWRAP || (!flush(netOutBuffer))) {
                            return SelectionKey.OP_WRITE;
                        }
                    }
                case NEED_UNWRAP:
                    {
                        handshake = handshakeUnwrap(read);
                        if (handshake.getStatus() == Status.OK) {
                            if (initHandshakeStatus == HandshakeStatus.NEED_TASK) initHandshakeStatus = tasks();
                        } else if (handshake.getStatus() == Status.BUFFER_UNDERFLOW) {
                            return SelectionKey.OP_READ;
                        } else {
                            throw new IOException("Invalid handshake status:" + initHandshakeStatus + " during handshake UNWRAP.");
                        }
                        break;
                    }
                case NEED_TASK:
                    {
                        initHandshakeStatus = tasks();
                        break;
                    }
                default:
                    throw new IllegalStateException("Invalid handshake status:" + initHandshakeStatus);
            }
        }
        return initHandshakeComplete ? 0 : (SelectionKey.OP_WRITE | SelectionKey.OP_READ);
    }

    /**
     * Executes all the tasks needed on the same thread.
     * @return HandshakeStatus
     */
    protected SSLEngineResult.HandshakeStatus tasks() {
        Runnable r = null;
        while ((r = sslEngine.getDelegatedTask()) != null) {
            r.run();
        }
        return sslEngine.getHandshakeStatus();
    }

    /**
     * Performs the WRAP function
     * @param doWrite boolean
     * @return SSLEngineResult
     * @throws IOException
     */
    protected SSLEngineResult handshakeWrap(boolean doWrite) throws IOException {
        netOutBuffer.clear();
        SSLEngineResult result = sslEngine.wrap(bufHandler.getWriteBuffer(), netOutBuffer);
        netOutBuffer.flip();
        initHandshakeStatus = result.getHandshakeStatus();
        if (doWrite) flush(netOutBuffer);
        return result;
    }

    /**
     * Perform handshake unwrap
     * @param doread boolean
     * @return SSLEngineResult
     * @throws IOException
     */
    protected SSLEngineResult handshakeUnwrap(boolean doread) throws IOException {
        if (netInBuffer.position() == netInBuffer.limit()) {
            netInBuffer.clear();
        }
        if (doread) {
            int read = sc.read(netInBuffer);
            if (read == -1) throw new IOException("EOF encountered during handshake.");
        }
        SSLEngineResult result;
        boolean cont = false;
        do {
            netInBuffer.flip();
            result = sslEngine.unwrap(netInBuffer, bufHandler.getReadBuffer());
            netInBuffer.compact();
            initHandshakeStatus = result.getHandshakeStatus();
            if (result.getStatus() == SSLEngineResult.Status.OK && result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                initHandshakeStatus = tasks();
            }
            cont = result.getStatus() == SSLEngineResult.Status.OK && initHandshakeStatus == HandshakeStatus.NEED_UNWRAP;
        } while (cont);
        return result;
    }

    /**
     * Sends a SSL close message, will not physically close the connection here.<br>
     * To close the connection, you could do something like
     * <pre><code>
     *   close();
     *   while (isOpen() && !myTimeoutFunction()) Thread.sleep(25);
     *   if ( isOpen() ) close(true); //forces a close if you timed out
     * </code></pre>
     * @throws IOException if an I/O error occurs
     * @throws IOException if there is data on the outgoing network buffer and we are unable to flush it
     * @todo Implement this java.io.Closeable method
     */
    public void close() throws IOException {
        if (closing) return;
        closing = true;
        sslEngine.closeOutbound();
        if (!flush(netOutBuffer)) {
            throw new IOException("Remaining data in the network buffer, can't send SSL close message, force a close with close(true) instead");
        }
        netOutBuffer.clear();
        SSLEngineResult handshake = sslEngine.wrap(getEmptyBuf(), netOutBuffer);
        if (handshake.getStatus() != SSLEngineResult.Status.CLOSED) {
            throw new IOException("Invalid close state, will not send network data.");
        }
        netOutBuffer.flip();
        flush(netOutBuffer);
        closed = (!netOutBuffer.hasRemaining() && (handshake.getHandshakeStatus() != HandshakeStatus.NEED_WRAP));
    }

    /**
     * Force a close, can throw an IOException
     * @param force boolean
     * @throws IOException
     */
    public void close(boolean force) throws IOException {
        try {
            close();
        } finally {
            if (force || closed) {
                closed = true;
                sc.socket().close();
                sc.close();
            }
        }
    }

    /**
     * Reads a sequence of bytes from this channel into the given buffer.
     *
     * @param dst The buffer into which bytes are to be transferred
     * @return The number of bytes read, possibly zero, or <tt>-1</tt> if the channel has reached end-of-stream
     * @throws IOException If some other I/O error occurs
     * @throws IllegalArgumentException if the destination buffer is different than bufHandler.getReadBuffer()
     * @todo Implement this java.nio.channels.ReadableByteChannel method
     */
    public int read(ByteBuffer dst) throws IOException {
        if (dst != bufHandler.getReadBuffer()) throw new IllegalArgumentException("You can only read using the application read buffer provided by the handler.");
        if (closing || closed) return -1;
        if (!initHandshakeComplete) throw new IllegalStateException("Handshake incomplete, you must complete handshake before reading data.");
        int netread = sc.read(netInBuffer);
        if (netread == -1) return -1;
        int read = 0;
        SSLEngineResult unwrap;
        do {
            netInBuffer.flip();
            unwrap = sslEngine.unwrap(netInBuffer, dst);
            netInBuffer.compact();
            if (unwrap.getStatus() == Status.OK || unwrap.getStatus() == Status.BUFFER_UNDERFLOW) {
                read += unwrap.bytesProduced();
                if (unwrap.getHandshakeStatus() == HandshakeStatus.NEED_TASK) tasks();
                if (unwrap.getStatus() == Status.BUFFER_UNDERFLOW) break;
            } else if (unwrap.getStatus() == Status.BUFFER_OVERFLOW && read > 0) {
                break;
            } else {
                throw new IOException("Unable to unwrap data, invalid status: " + unwrap.getStatus());
            }
        } while ((netInBuffer.position() != 0));
        return (read);
    }

    /**
     * Writes a sequence of bytes to this channel from the given buffer.
     *
     * @param src The buffer from which bytes are to be retrieved
     * @return The number of bytes written, possibly zero
     * @throws IOException If some other I/O error occurs
     * @todo Implement this java.nio.channels.WritableByteChannel method
     */
    public int write(ByteBuffer src) throws IOException {
        if (src == this.netOutBuffer) {
            int written = sc.write(src);
            return written;
        } else {
            if (src != bufHandler.getWriteBuffer()) throw new IllegalArgumentException("You can only write using the application write buffer provided by the handler.");
            if (closing || closed) throw new IOException("Channel is in closing state.");
            int written = 0;
            if (!flush(netOutBuffer)) {
                return written;
            }
            netOutBuffer.clear();
            SSLEngineResult result = sslEngine.wrap(src, netOutBuffer);
            written = result.bytesConsumed();
            netOutBuffer.flip();
            if (result.getStatus() == Status.OK) {
                if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) tasks();
            } else {
                throw new IOException("Unable to wrap data, invalid engine state: " + result.getStatus());
            }
            flush(netOutBuffer);
            return written;
        }
    }

    /**
     * Callback interface to be able to expand buffers
     * when buffer overflow exceptions happen
     */
    public static interface ApplicationBufferHandler {

        public ByteBuffer expand(ByteBuffer buffer, int remaining);

        public ByteBuffer getReadBuffer();

        public ByteBuffer getWriteBuffer();
    }

    public ApplicationBufferHandler getBufHandler() {
        return bufHandler;
    }

    public boolean isInitHandshakeComplete() {
        return initHandshakeComplete;
    }

    public boolean isClosing() {
        return closing;
    }

    public SSLEngine getSslEngine() {
        return sslEngine;
    }

    public ByteBuffer getEmptyBuf() {
        return emptyBuf;
    }

    public void setBufHandler(ApplicationBufferHandler bufHandler) {
        this.bufHandler = bufHandler;
    }

    public SocketChannel getIOChannel() {
        return sc;
    }
}

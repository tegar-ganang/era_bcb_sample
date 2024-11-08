package org.szegedi.nbpipe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;

/**
 * NonblockingPipe is a device for bridging between one-connection-per-thread
 * code and the multiple-connections-per-thread (nio) code. The legacy code
 * must be shielded from both the complexity of non-blocking I/O and from
 * getting blocked on I/O. This is where a NonblockingPipe comes in. This
 * pipe uses a ByteBufferPool to satisfy its memory needs, and does not work
 * with an ordinary cyclic buffer that blocks write operations when full. This
 * way, it is ideal for presenting InputStream and OutputStream interfaces for 
 * legacy code, while retaining full non-blocking I/O capacity. 
 */
public class NonblockingPipe {

    private final LinkedList blocks = new LinkedList();

    private int available = 0;

    private final ByteBufferPool bufferPool;

    private PipeInputStream in = new PipeInputStream();

    private PipeOutputStream out = new PipeOutputStream();

    private boolean closedForWriting = false;

    private WritableByteChannel autoWriteChannel;

    private ChannelWriteListener channelWriteListener;

    private int readPos = 0;

    private boolean readMode = false;

    public NonblockingPipe(ByteBufferPool bufferPool) {
        this.bufferPool = bufferPool;
    }

    /**
     * Sets a writable byte channel to which all writes on the output stream
     * are automatically transferred whenever an internal pipe buffer is
     * ready.
     * @param channel the channel to which bytes are transferred automatically
     * @param listener a listener that gets notified of closing the output
     * stream as well as of any IOExceptions that are thrown during the channel 
     * write operations.
     */
    public void setAutoWriteChannel(WritableByteChannel channel, ChannelWriteListener listener) {
        synchronized (blocks) {
            autoWriteChannel = channel;
            channelWriteListener = listener;
        }
    }

    /**
     * Returns an input stream through which the contents of the pipe
     * can be read. Reading from the stream retrieves the same data that
     * is transferred through <tt>autoTransferTo()</tt>.
     */
    public InputStream getInputStream() {
        synchronized (blocks) {
            if (in == null) in = new PipeInputStream();
        }
        return in;
    }

    /**
     * Returns an output stream through which the contents of the pipe
     * can be written. Writing to the stream is equivalent to using the
     * <tt>transferFrom</tt> method.
     */
    public OutputStream getOutputStream() {
        synchronized (blocks) {
            if (out == null) out = new PipeOutputStream();
        }
        return out;
    }

    /**
     * Returns the number of bytes currently available in the pipe.
     */
    public int available() {
        synchronized (blocks) {
            return available;
        }
    }

    /**
     * Returns true if the pipe contains no data, and is closed for writing.
     */
    public boolean isExhausted() {
        synchronized (blocks) {
            return isExhaustedInternal();
        }
    }

    private boolean isExhaustedInternal() {
        return blocks.isEmpty() && closedForWriting;
    }

    /**
     * Transfers as many bytes are possible from this pipe's buffer to the
     * channel specified in the call to <tt>setAutoWriteChannel</tt>.
     * It will transfer until it can write 0 bytes.
     * @return the number of bytes transferred. -1 is returned if the pipe
     * is exhausted.
     */
    public int autoTransferTo() throws IOException {
        int transferCount = 0;
        synchronized (blocks) {
            if (autoWriteChannel == null) throw new IllegalStateException("autoWriteChannel not set");
            int blocksSize;
            while ((blocksSize = blocks.size()) > 0) {
                boolean lastBuffer = blocksSize == 1;
                ByteBuffer buf = (ByteBuffer) blocks.getFirst();
                if (lastBuffer && !readMode) {
                    enterReadMode(buf);
                }
                int written = autoWriteChannel.write(buf);
                if (!buf.hasRemaining()) {
                    releaseBuffer(buf);
                }
                if (written == 0) {
                    break;
                }
                available -= written;
                transferCount += written;
            }
            return transferCount == 0 && isExhaustedInternal() ? -1 : transferCount;
        }
    }

    public int transferFrom(ReadableByteChannel channel) throws IOException {
        return transferFrom(channel, Integer.MAX_VALUE);
    }

    /**
     * Transfers as many bytes are possible from the specified channel to
     * this pipe's buffer. It will transfer until it can read 0 bytes. After
     * this method has been called (potentially multiple times), you
     * should call {@link #closeForWriting()} to indicate you have written
     * all data to the pipe (or alternatively, you can close the pipe's
     * output stream to achieve the same effect).
     * @param channel the channel to transfer bytes from
     * @param maxbytes maximum number of bytes to transfer
     * @return the number of bytes transferred
     */
    public int transferFrom(ReadableByteChannel channel, int maxbytes) throws IOException {
        checkNotClosed();
        int transferCount = 0;
        synchronized (blocks) {
            ByteBuffer buf = blocks.isEmpty() ? addNewBuffer() : (ByteBuffer) blocks.getLast();
            boolean firstBuffer = blocks.size() == 1;
            if (firstBuffer && readMode) {
                enterWriteMode(buf);
            }
            while (transferCount < maxbytes) {
                int maxread = maxbytes - transferCount;
                boolean bufferTooLarge = buf.remaining() > maxread;
                if (bufferTooLarge) {
                    buf.limit(buf.position() + maxread);
                }
                int read = channel.read(buf);
                if (bufferTooLarge) {
                    buf.limit(buf.capacity());
                }
                if (!buf.hasRemaining()) {
                    buf.flip();
                    buf = addNewBuffer();
                }
                if (read == 0) break;
                if (read == -1) {
                    if (transferCount == 0) transferCount = -1;
                    break;
                }
                available += read;
                transferCount += read;
            }
            if (buf.position() == 0) {
                releaseBuffer(buf);
            }
            if (transferCount > 0) afterWrite();
            return transferCount;
        }
    }

    /**
     * Transfers as many bytes are possible from the specified byte buffer to
     * this pipe's buffer. After this method has been called (potentially 
     * multiple times), you should call {@link #closeForWriting()} to indicate 
     * you have written all data to the pipe (or alternatively, you can close 
     * the pipe's output stream to achieve the same effect).
     * @param srcbuf the buffer to transfer bytes from
     * @param maxbytes maximum number of bytes to transfer
     * @return the number of bytes transferred
     */
    public int transferFrom(ByteBuffer srcbuf, int maxbytes) throws IOException {
        checkNotClosed();
        int transferCount = 0;
        synchronized (blocks) {
            ByteBuffer buf = blocks.isEmpty() ? addNewBuffer() : (ByteBuffer) blocks.getLast();
            boolean firstBuffer = blocks.size() == 1;
            if (firstBuffer && readMode) {
                enterWriteMode(buf);
            }
            while (transferCount < maxbytes) {
                int maxread = maxbytes - transferCount;
                boolean bufferTooLarge = buf.remaining() > maxread;
                if (bufferTooLarge) {
                    buf.limit(buf.position() + maxread);
                }
                int posBefore = buf.position();
                buf.put(srcbuf);
                int read = buf.position() - posBefore;
                if (bufferTooLarge) {
                    buf.limit(buf.capacity());
                }
                if (!buf.hasRemaining()) {
                    buf.flip();
                    buf = addNewBuffer();
                }
                if (read == 0) break;
                if (read == -1) {
                    if (transferCount == 0) transferCount = -1;
                    break;
                }
                available += read;
                transferCount += read;
            }
            if (buf.position() == 0) {
                releaseBuffer(buf);
            }
            if (transferCount > 0) afterWrite();
            return transferCount;
        }
    }

    public void clear() {
        synchronized (blocks) {
            while (!blocks.isEmpty()) {
                bufferPool.putBuffer((ByteBuffer) blocks.removeFirst());
            }
            available = 0;
            readMode = false;
        }
    }

    /**
     * Called to signal that no more data will be transferred from an
     * external channel to the pipe.
     */
    public void closeForWriting() {
        synchronized (blocks) {
            closedForWriting = true;
            if (in != null) blocks.notify();
        }
    }

    private void checkNotClosed() throws IOException {
        if (closedForWriting) throw new IOException("Pipe closed for writing");
    }

    private ByteBuffer addNewBuffer() {
        ByteBuffer buf = blocks.size() < 4 ? bufferPool.getMemoryBuffer() : bufferPool.getFileBuffer();
        blocks.addLast(buf);
        readPos = 0;
        readMode = false;
        return buf;
    }

    private void afterWrite() throws IOException {
        if (in != null) blocks.notify();
        if (autoWriteChannel != null) {
            try {
                autoTransferTo();
            } catch (IOException e) {
                if (channelWriteListener != null) channelWriteListener.ioExceptionRaised(e);
                throw e;
            }
        }
    }

    private void releaseBuffer(ByteBuffer buf) {
        blocks.removeFirst();
        bufferPool.putBuffer(buf);
    }

    private void enterReadMode(ByteBuffer buf) {
        buf.limit(buf.position());
        buf.position(readPos);
        readMode = true;
    }

    private void enterWriteMode(ByteBuffer buf) {
        readPos = buf.position();
        buf.position(buf.limit());
        buf.limit(buf.capacity());
        readMode = false;
    }

    private int read() throws IOException {
        synchronized (blocks) {
            ByteBuffer buf = getReadReadyBuffer();
            if (buf == null) return -1;
            int retval = buf.get();
            if (retval < 0) retval += 256;
            releaseBuffer(buf);
            --available;
            return retval;
        }
    }

    private int read(byte[] b, int ofs, int len) throws IOException {
        if (len > 0) {
            int ofs1 = ofs;
            synchronized (blocks) {
                while (len > 0) {
                    ByteBuffer buf = getReadReadyBuffer();
                    if (buf == null) break;
                    int maxlen = Math.min(len, buf.remaining());
                    buf.get(b, ofs, maxlen);
                    ofs += maxlen;
                    len -= maxlen;
                    available -= maxlen;
                    if (!buf.hasRemaining()) {
                        releaseBuffer(buf);
                    }
                }
            }
            int readCount = ofs - ofs1;
            return readCount == 0 ? -1 : readCount;
        } else if (len < 0) throw new IllegalArgumentException("len < 0"); else return 0;
    }

    private ByteBuffer getReadReadyBuffer() throws IOException {
        for (; ; ) {
            switch(blocks.size()) {
                case 0:
                    {
                        if (isExhaustedInternal()) return null;
                        break;
                    }
                case 1:
                    {
                        ByteBuffer buf = (ByteBuffer) blocks.getFirst();
                        if (!readMode) {
                            enterReadMode(buf);
                        }
                        if (buf.hasRemaining()) return buf;
                        break;
                    }
                default:
                    {
                        return (ByteBuffer) blocks.getFirst();
                    }
            }
            try {
                blocks.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    private void write(int b) throws IOException {
        synchronized (blocks) {
            getWriteBuffer().put((byte) b);
            ++available;
            afterWrite();
        }
    }

    private void write(byte[] b, int ofs, int len) throws IOException {
        if (len > 0) {
            synchronized (blocks) {
                while (len > 0) {
                    ByteBuffer buf = getWriteBuffer();
                    int maxlen = Math.min(len, buf.remaining());
                    buf.put(b, ofs, maxlen);
                    ofs += maxlen;
                    len -= maxlen;
                    available += maxlen;
                }
                afterWrite();
            }
        } else if (len < 0) throw new IllegalArgumentException("len < 0");
    }

    private ByteBuffer getWriteBuffer() throws IOException {
        checkNotClosed();
        int blocksSize = blocks.size();
        if (blocksSize == 0) return addNewBuffer();
        ByteBuffer buf = (ByteBuffer) blocks.getLast();
        if (readMode && blocksSize == 1) {
            enterWriteMode(buf);
        }
        if (!buf.hasRemaining()) {
            buf.flip();
            buf = addNewBuffer();
        }
        return buf;
    }

    private class PipeInputStream extends InputStream {

        public int available() {
            synchronized (blocks) {
                return available;
            }
        }

        public void close() throws IOException {
            closeForWriting();
            clear();
        }

        public int read() throws IOException {
            return NonblockingPipe.this.read();
        }

        public int read(byte[] b) throws IOException {
            return NonblockingPipe.this.read(b, 0, b.length);
        }

        public int read(byte[] b, int ofs, int len) throws IOException {
            return NonblockingPipe.this.read(b, ofs, len);
        }
    }

    private class PipeOutputStream extends OutputStream {

        public void close() throws IOException {
            closeForWriting();
        }

        public void write(int b) throws IOException {
            NonblockingPipe.this.write(b);
        }

        public void write(byte[] b) throws IOException {
            NonblockingPipe.this.write(b, 0, b.length);
        }

        public void write(byte[] b, int ofs, int len) throws IOException {
            NonblockingPipe.this.write(b, ofs, len);
        }

        public void flush() {
        }
    }
}

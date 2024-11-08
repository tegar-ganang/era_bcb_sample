package naga;

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * A Socket reader handles read/writes on a socket.
 *
 * @author Christoffer Lerno
 */
class SocketReader {

    private final NIOService m_nioService;

    private ByteBuffer m_previousBytes;

    private long m_bytesRead;

    SocketReader(NIOService nioService) {
        m_nioService = nioService;
        m_bytesRead = 0;
    }

    public int read(SocketChannel channel) throws IOException {
        ByteBuffer buffer = getBuffer();
        buffer.clear();
        if (m_previousBytes != null) {
            buffer.position(m_previousBytes.remaining());
        }
        int read = channel.read(buffer);
        if (read < 0) throw new EOFException("Buffer read -1");
        if (!buffer.hasRemaining()) throw new BufferOverflowException();
        m_bytesRead += read;
        if (read == 0) return 0;
        if (m_previousBytes != null) {
            int position = buffer.position();
            buffer.position(0);
            buffer.put(m_previousBytes);
            buffer.position(position);
            m_previousBytes = null;
        }
        buffer.flip();
        return read;
    }

    /**
     * Moves any unread bytes to a buffer to be available later.
     */
    public void compact() {
        ByteBuffer buffer = getBuffer();
        if (buffer.remaining() > 0) {
            m_previousBytes = NIOUtils.copy(buffer);
        }
    }

    /**
     * Return the number of raw bytes read.
     *
     * @return the number of bytes read.
     */
    public long getBytesRead() {
        return m_bytesRead;
    }

    /**
     * Returns the shared buffer (associated with the NIOService) for read/write.
     *
     * @return the shared buffer-
     */
    public ByteBuffer getBuffer() {
        return m_nioService.getSharedBuffer();
    }
}

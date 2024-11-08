package games.strategy.net.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A packet of data being read over the network.
 * <p>
 * 
 * A Packet does not correspond to a network packet, rather it is the bytes for 1 serialized java object.
 * <p>
 * 
 * @author sgb
 */
class SocketReadData {

    public static final int MAX_MESSAGE_SIZE = 1000 * 1000 * 10;

    private static final Logger s_logger = Logger.getLogger(SocketReadData.class.getName());

    private static final AtomicInteger s_counter = new AtomicInteger();

    public static final int MAGIC = 0x9b000000;

    private int m_targetSize = -1;

    private ByteBuffer m_sizeBuffer;

    private ByteBuffer m_contentBuffer;

    private final SocketChannel m_channel;

    private final int m_number = s_counter.incrementAndGet();

    private int m_readCalls;

    public SocketReadData(final SocketChannel channel) {
        this.m_channel = channel;
    }

    /**
	 * Read data from the channel, returning true if this packet is done.
	 * <p>
	 * 
	 * If we detect the socket is closed, we will throw an IOExcpetion
	 */
    public boolean read(final SocketChannel channel) throws IOException {
        m_readCalls++;
        if (m_targetSize < 0) {
            if (m_sizeBuffer == null) {
                m_sizeBuffer = ByteBuffer.allocate(4);
            }
            final int size = channel.read(m_sizeBuffer);
            if (s_logger.isLoggable(Level.FINEST)) {
                s_logger.finest("read size_buffer bytes:" + size);
            }
            if (size == -1) throw new IOException("Socket closed");
            if (!m_sizeBuffer.hasRemaining()) {
                m_sizeBuffer.flip();
                m_targetSize = m_sizeBuffer.getInt();
                if ((m_targetSize & 0xFF000000) != MAGIC) throw new IOException("Did not write magic!");
                m_targetSize = m_targetSize & 0x00ffffff;
                if (m_targetSize <= 0 || m_targetSize > MAX_MESSAGE_SIZE) {
                    throw new IOException("Invalid triplea packet size:" + m_targetSize);
                }
                m_contentBuffer = ByteBuffer.allocate(m_targetSize);
                m_sizeBuffer = null;
            } else {
                return false;
            }
        }
        final int size = channel.read(m_contentBuffer);
        if (s_logger.isLoggable(Level.FINEST)) {
            s_logger.finest("read content bytes:" + size);
        }
        if (size == -1) throw new IOException("Socket closed");
        return !m_contentBuffer.hasRemaining();
    }

    public SocketChannel getChannel() {
        return m_channel;
    }

    /**
	 * Get the data as a byte[].
	 * 
	 * This method can only be called once.
	 * 
	 */
    public byte[] getData() {
        final byte[] rVal = new byte[m_contentBuffer.capacity()];
        m_contentBuffer.flip();
        m_contentBuffer.get(rVal);
        m_contentBuffer = null;
        return rVal;
    }

    public int size() {
        return m_targetSize + 4;
    }

    public int getReadCalls() {
        return m_readCalls;
    }

    @Override
    public String toString() {
        return "<id:" + m_number + " size:" + m_targetSize + ">";
    }
}

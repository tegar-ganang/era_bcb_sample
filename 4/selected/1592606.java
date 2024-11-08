package naga;

import naga.exception.ProtocolViolationException;
import naga.packetreader.RawPacketReader;
import naga.packetwriter.RawPacketWriter;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Undocumented Class
 *
 * @author Christoffer Lerno
 */
public class SSLPacketHandler implements PacketReader, PacketWriter {

    private static final Executor TASK_HANDLER = Executors.newSingleThreadExecutor();

    private static final ThreadLocal<ByteBuffer> SSL_BUFFER = new ThreadLocal<ByteBuffer>() {

        @Override
        protected ByteBuffer initialValue() {
            return ByteBuffer.allocate(64 * 1024);
        }
    };

    private final SSLEngine m_engine;

    private PacketReader m_reader;

    private PacketWriter m_writer;

    private ByteBuffer m_partialIncomingBuffer;

    private ByteBuffer[] m_initialOutBuffer;

    private final NIOSocket m_socket;

    private final SSLSocketChannelResponder m_responder;

    private boolean m_sslInitiated;

    public SSLPacketHandler(SSLEngine engine, NIOSocket socket, SSLSocketChannelResponder responder) {
        m_engine = engine;
        m_socket = socket;
        m_partialIncomingBuffer = null;
        m_writer = RawPacketWriter.INSTANCE;
        m_reader = RawPacketReader.INSTANCE;
        m_responder = responder;
        m_sslInitiated = false;
    }

    public PacketReader getReader() {
        return m_reader;
    }

    public void setReader(PacketReader reader) {
        m_reader = reader;
    }

    public PacketWriter getWriter() {
        return m_writer;
    }

    public void setWriter(PacketWriter writer) {
        m_writer = writer;
    }

    private void queueSSLTasks() {
        if (!m_sslInitiated) return;
        int tasksScheduled = 0;
        Runnable task;
        while ((task = m_engine.getDelegatedTask()) != null) {
            TASK_HANDLER.execute(task);
            tasksScheduled++;
        }
        if (tasksScheduled == 0) {
            return;
        }
        TASK_HANDLER.execute(new Runnable() {

            public void run() {
                m_socket.queue(new Runnable() {

                    public void run() {
                        reactToHandshakeStatus(m_engine.getHandshakeStatus());
                    }
                });
            }
        });
    }

    public byte[] nextPacket(ByteBuffer byteBuffer) throws ProtocolViolationException {
        if (!m_sslInitiated) {
            return m_reader.nextPacket(byteBuffer);
        }
        try {
            ByteBuffer targetBuffer = SSL_BUFFER.get();
            targetBuffer.clear();
            SSLEngineResult result = m_engine.unwrap(byteBuffer, targetBuffer);
            switch(result.getStatus()) {
                case BUFFER_UNDERFLOW:
                    return null;
                case BUFFER_OVERFLOW:
                    throw new ProtocolViolationException("SSL Buffer Overflow");
                case CLOSED:
                    m_responder.connectionBroken(m_socket, new EOFException("SSL Connection closed"));
                    return null;
                case OK:
            }
            reactToHandshakeStatus(result.getHandshakeStatus());
            return retrieveDecryptedPacket(targetBuffer);
        } catch (SSLException e) {
            m_responder.closeDueToSSLException(e);
            return null;
        }
    }

    private void reactToHandshakeStatus(SSLEngineResult.HandshakeStatus status) {
        if (!m_sslInitiated) return;
        switch(status) {
            case NOT_HANDSHAKING:
            case NEED_UNWRAP:
                break;
            case NEED_TASK:
                queueSSLTasks();
                break;
            case FINISHED:
                m_socket.write(new byte[0]);
                break;
            case NEED_WRAP:
                m_socket.write(new byte[0]);
                break;
        }
    }

    private byte[] retrieveDecryptedPacket(ByteBuffer targetBuffer) throws ProtocolViolationException {
        targetBuffer.flip();
        m_partialIncomingBuffer = NIOUtils.join(m_partialIncomingBuffer, targetBuffer);
        if (m_partialIncomingBuffer == null || m_partialIncomingBuffer.remaining() == 0) return SKIP_PACKET;
        return m_reader.nextPacket(m_partialIncomingBuffer);
    }

    public ByteBuffer[] write(ByteBuffer[] byteBuffers) {
        if (!m_sslInitiated) {
            return m_writer.write(byteBuffers);
        }
        if (m_engine.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            if (!NIOUtils.isEmpty(byteBuffers)) {
                m_initialOutBuffer = NIOUtils.concat(m_initialOutBuffer, m_writer.write(byteBuffers));
                byteBuffers = new ByteBuffer[0];
            }
            ByteBuffer buffer = SSL_BUFFER.get();
            ByteBuffer[] buffers = null;
            try {
                SSLEngineResult result = null;
                while (m_engine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                    buffer.clear();
                    result = m_engine.wrap(byteBuffers, buffer);
                    buffer.flip();
                    buffers = NIOUtils.concat(buffers, NIOUtils.copy(buffer));
                }
                if (result == null) return null;
                if (result.getStatus() != SSLEngineResult.Status.OK) throw new SSLException("Unexpectedly not ok wrapping handshake data, was " + result.getStatus());
                reactToHandshakeStatus(result.getHandshakeStatus());
            } catch (SSLException e) {
                throw new RuntimeException(e);
            }
            return buffers;
        }
        ByteBuffer buffer = SSL_BUFFER.get();
        buffer.clear();
        if (NIOUtils.isEmpty(byteBuffers)) {
            if (m_initialOutBuffer == null) return null;
        } else {
            byteBuffers = m_writer.write(byteBuffers);
        }
        if (m_initialOutBuffer != null) {
            byteBuffers = NIOUtils.concat(m_initialOutBuffer, byteBuffers);
            m_initialOutBuffer = null;
        }
        ByteBuffer[] encrypted = null;
        while (!NIOUtils.isEmpty(byteBuffers)) {
            buffer.clear();
            try {
                m_engine.wrap(byteBuffers, buffer);
            } catch (SSLException e) {
                throw new RuntimeException(e);
            }
            buffer.flip();
            encrypted = NIOUtils.concat(encrypted, NIOUtils.copy(buffer));
        }
        return encrypted;
    }

    public SSLEngine getSSLEngine() {
        return m_engine;
    }

    void begin() throws SSLException {
        m_engine.beginHandshake();
        m_sslInitiated = true;
        reactToHandshakeStatus(m_engine.getHandshakeStatus());
    }

    public void closeEngine() {
        if (!m_sslInitiated) return;
        m_engine.closeOutbound();
        m_responder.write(new byte[0]);
    }

    public boolean isEncrypted() {
        return m_sslInitiated;
    }
}

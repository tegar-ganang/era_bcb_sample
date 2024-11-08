package ultramc;

import ultramc.buffer.*;
import ultramc.connect.*;
import java.io.IOException;
import java.net.SocketAddress;
import ultramc.connect.*;
import ultramc.buffer.*;
import java.nio.ByteBuffer;
import java.util.*;

public class MemCachedClient {

    private BufferPool m_bufferPool;

    private KeyEncoder m_defaultKeyEncoder;

    private ValueTranscoder m_defaultValueTranscoder;

    private int m_defaultExpiry;

    private long m_defaultTimeout;

    private boolean m_defaultReply;

    private ServerConnectionPool m_connectionPool;

    public MemCachedClient(SocketAddress address) {
        init(address, 1024);
    }

    public MemCachedClient(SocketAddress address, int bufferSize) {
        init(address, bufferSize);
    }

    private void init(SocketAddress address, int bufferSize) {
        m_bufferPool = new BufferPool(bufferSize);
        m_connectionPool = new ServerConnectionPool(address);
        m_defaultKeyEncoder = new NoKeyEncoder();
        m_defaultValueTranscoder = new DefaultValueTranscoder(m_bufferPool);
        m_defaultExpiry = 60 * 60 * 24;
        m_defaultTimeout = 0;
        m_defaultReply = true;
    }

    public MemCachedClient setMaxConnectionPoolSize(int size) {
        m_connectionPool.setMaxPoolSize(size);
        return (this);
    }

    public MemCachedClient setMinConnectionPoolSize(int size) {
        m_connectionPool.setMinPoolSize(size);
        return (this);
    }

    public MemCachedClient setMaxBufferPoolSize(int size) {
        m_bufferPool.setMaxPoolSize(size);
        return (this);
    }

    public MemCachedClient setMinBufferPoolSize(int size) {
        m_bufferPool.setMinPoolSize(size);
        return (this);
    }

    public MemCachedClient setDefaultKeyEncoder(KeyEncoder defaultKeyEncoder) {
        m_defaultKeyEncoder = defaultKeyEncoder;
        return (this);
    }

    public KeyEncoder getDefaultKeyEncoder() {
        return (m_defaultKeyEncoder);
    }

    public MemCachedClient setDefaultValueTranscoder(ValueTranscoder defaultValueTranscoder) {
        m_defaultValueTranscoder = defaultValueTranscoder;
        return (this);
    }

    public ValueTranscoder getDefaultValueTranscoder() {
        return (m_defaultValueTranscoder);
    }

    public MemCachedClient setDefaultExpiry(int defaultExpiry) {
        m_defaultExpiry = defaultExpiry;
        return (this);
    }

    public int getDefaultExpiry() {
        return (m_defaultExpiry);
    }

    public MemCachedClient setDefaultTimeout(long defaultTimeout) {
        m_defaultTimeout = defaultTimeout;
        return (this);
    }

    public long getDefaultTimeout() {
        return (m_defaultTimeout);
    }

    public MemCachedClient setDefaultReply(boolean defaultReply) {
        m_defaultReply = defaultReply;
        return (this);
    }

    public boolean getDefaultReply() {
        return (m_defaultReply);
    }

    public MemCachedClient setBufferPool(BufferPool bufferPool) {
        m_bufferPool = bufferPool;
        return (this);
    }

    public BufferPool getBufferPool() {
        return (m_bufferPool);
    }

    public SetOperation createSet(String key, Object value) {
        SetOperation setOp = new SetOperation(key, value, this);
        return (setOp);
    }

    public GetOperation createGet(String key) {
        GetOperation getOp = new GetOperation(key, this);
        return (getOp);
    }

    public GetOperation createGet(List<String> keys) {
        GetOperation getOp = new GetOperation(keys, this);
        return (getOp);
    }

    public DeleteOperation createDelete(String key) {
        DeleteOperation delOp = new DeleteOperation(key, this);
        return (delOp);
    }

    public String flushAll() {
        String resp = Operation.ERROR;
        ServerConnection serverConnection = getServerConnection("");
        if (serverConnection == null) {
            return (Operation.ERROR);
        }
        String command = "flush_all\r\n";
        ByteBuffer[] sendBuffers = new ByteBuffer[1];
        sendBuffers[0] = Operation.UTF8.encode(command);
        int bytesToWrite = sendBuffers[0].limit();
        BufferSet bs = getBufferSet();
        try {
            Operation.writeToChannel(serverConnection.getChannel(), sendBuffers, bytesToWrite);
            Operation.readResponse(serverConnection, bs, 0, Operation.END_OF_LINE);
            resp = Operation.readLine(new ByteBufferInputStream(bs));
            serverConnection.recycleConnection();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            serverConnection.closeConnection();
        }
        bs.freeBuffers();
        return (resp);
    }

    public void close() {
        m_connectionPool.close();
        m_bufferPool.close();
    }

    BufferSet getBufferSet() {
        return (m_bufferPool.getBufferSet());
    }

    ServerConnection getServerConnection(String hashKey) {
        return (m_connectionPool.getServerConnection());
    }
}

package ultramc;

import ultramc.buffer.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import ultramc.connect.ServerConnection;

public abstract class StorageOperation<T extends StorageOperation> extends KeyedOperation<T> {

    public static final String STORED = "STORED";

    public static final String NOT_STORED = "NOT_STORED";

    private Object m_value;

    private int m_expiry;

    private boolean m_reply;

    private String m_response;

    public StorageOperation(String key, Object value, MemCachedClient client) {
        super(key, client);
        m_value = value;
        m_expiry = client.getDefaultExpiry();
        m_reply = client.getDefaultReply();
        m_response = NOT_CALLED;
    }

    protected abstract String getOperation();

    @SuppressWarnings("unchecked")
    public T setExpiry(int exp) {
        m_expiry = exp;
        return ((T) this);
    }

    @SuppressWarnings("unchecked")
    public T setReply(boolean reply) {
        m_reply = reply;
        return ((T) this);
    }

    @SuppressWarnings("unchecked")
    public T run() {
        int flags = 0;
        String resp = NOT_STORED;
        String key = m_keyEncoder.encodeKey(m_key);
        ServerConnection serverConnection = m_client.getServerConnection(m_hashKey);
        if (serverConnection == null) {
            m_response = ERROR;
            return ((T) this);
        }
        BufferSet bs = m_client.getBufferSet();
        m_valueEncoder.encodeValue(m_value, new ByteBufferOutputStream(bs));
        bs.flipBuffers();
        List<ByteBuffer> bufferList = bs.getBuffers();
        int bufferCount = bufferList.size() + 2;
        ByteBuffer[] sendBuffers = new ByteBuffer[bufferCount];
        int dataSize = 0;
        for (int I = 1; I < sendBuffers.length - 1; I++) {
            sendBuffers[I] = bufferList.get(I - 1);
            dataSize += sendBuffers[I].limit();
        }
        StringBuilder command = new StringBuilder();
        command.append(getOperation()).append(" ");
        command.append(key).append(" ");
        command.append(flags).append(" ");
        command.append(m_expiry).append(" ");
        command.append(dataSize).append(" ");
        if (!m_reply) command.append("noreply");
        command.append("\r\n");
        sendBuffers[0] = UTF8.encode(command.toString());
        sendBuffers[bufferCount - 1] = ByteBuffer.wrap(END_OF_LINE);
        int bytesToWrite = sendBuffers[0].limit();
        bytesToWrite = dataSize + END_OF_LINE.length;
        try {
            writeToChannel(serverConnection.getChannel(), sendBuffers, bytesToWrite);
            bs.freeBuffers();
            if (m_reply) {
                readResponse(serverConnection, bs, m_timeout, END_OF_LINE);
                String line = readLine(new ByteBufferInputStream(bs));
                if (line != null) resp = line;
            } else resp = STORED;
            serverConnection.recycleConnection();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            serverConnection.closeConnection();
        }
        bs.freeBuffers();
        m_response = resp;
        return ((T) this);
    }

    @SuppressWarnings("unchecked")
    public T runAsync() {
        return ((T) this);
    }

    public String getResponse() {
        return (m_response);
    }
}

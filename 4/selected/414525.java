package ultramc;

import java.util.*;
import java.nio.ByteBuffer;
import java.io.IOException;
import ultramc.connect.ServerConnection;
import ultramc.buffer.*;

public class DeleteOperation extends KeyedOperation<DeleteOperation> {

    public static final String DELETED = "DELETED";

    public static final String NOT_FOUND = "NOT_FOUND";

    private String m_response;

    private boolean m_reply;

    DeleteOperation(String key, MemCachedClient client) {
        super(key, client);
        m_response = NOT_CALLED;
        m_reply = client.getDefaultReply();
    }

    public DeleteOperation setReply(boolean reply) {
        m_reply = reply;
        return (this);
    }

    public String getResponse() {
        return (m_response);
    }

    public DeleteOperation run() {
        String resp = NOT_FOUND;
        String key = m_keyEncoder.encodeKey(m_key);
        ServerConnection serverConnection = m_client.getServerConnection(m_hashKey);
        if (serverConnection == null) {
            m_response = ERROR;
            return (this);
        }
        StringBuilder command = new StringBuilder();
        command.append("delete ");
        command.append(key).append(" ");
        if (!m_reply) command.append("noreply");
        command.append("\r\n");
        ByteBuffer[] sendBuffers = new ByteBuffer[1];
        sendBuffers[0] = UTF8.encode(command.toString());
        int bytesToWrite = sendBuffers[0].limit();
        BufferSet bs = m_client.getBufferSet();
        try {
            writeToChannel(serverConnection.getChannel(), sendBuffers, bytesToWrite);
            if (m_reply) {
                readResponse(serverConnection, bs, m_timeout, END_OF_LINE);
                String line = readLine(new ByteBufferInputStream(bs));
                if (line != null) resp = line;
            } else resp = DELETED;
            serverConnection.recycleConnection();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            serverConnection.closeConnection();
        }
        bs.freeBuffers();
        m_response = resp;
        return (this);
    }

    public DeleteOperation runAsnyc() {
        return (this);
    }
}

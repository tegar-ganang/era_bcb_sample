package net.sf.profiler4j.console.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import net.sf.profiler4j.agent.AgentConstants;
import net.sf.profiler4j.agent.ThreadInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class the implements the Profiler4j remote client.
 * <p>
 * Notice that if a {@link ClientException} is thrown the connection is automatically
 * closed.
 * 
 * @author Antonio S. R. Gomes
 */
public class Client {

    private static final Log log = LogFactory.getLog(Client.class);

    private Socket s;

    private ObjectOutputStream out;

    private ObjectInputStream in;

    public synchronized void connect(String host, int port) throws ClientException {
        try {
            if (isConnected()) {
                throw new ClientException("Client already connected");
            }
            s = new Socket(host, port);
            out = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
            in = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
            String serverVersion = in.readUTF();
            if (!serverVersion.equals(AgentConstants.VERSION)) {
                s.close();
                s = null;
                out = null;
                in = null;
                throw new ClientException("Version of remote agent is incompatible: console is '" + AgentConstants.VERSION + "' but agent is '" + serverVersion + "'");
            }
            log.info("Client connected to " + host + ":" + port);
        } catch (Exception e) {
            handleException(e);
        }
    }

    public synchronized boolean isConnected() {
        return s != null && s.isConnected() && !s.isInputShutdown();
    }

    public synchronized void disconnect() throws ClientException {
        assertConnected();
        try {
            sendAndWaitAck(AgentConstants.CMD_DISCONNECT);
            close();
        } catch (Exception e) {
            handleException(e);
        }
    }

    public synchronized void reset() throws ClientException {
        assertConnected();
        try {
            sendAndWaitAck(AgentConstants.CMD_RESET_STATS);
        } catch (Exception e) {
            handleException(e);
        }
    }

    public synchronized RuntimeInfo getRuntimeInfo() throws ClientException {
        assertConnected();
        try {
            sendAndWaitAck(AgentConstants.CMD_GET_RUNTIME_INFO);
            return RuntimeInfo.read(in);
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    public synchronized MemoryInfo getMemoryInfo() throws ClientException {
        assertConnected();
        try {
            sendAndWaitAck(AgentConstants.CMD_GET_MEMORY_INFO);
            return MemoryInfo.read(in);
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    public synchronized boolean[] setThreadMonitoring(boolean threadMonitoring, boolean cpuTimeMonitoring) throws ClientException {
        assertConnected();
        try {
            out.writeInt(AgentConstants.CMD_SET_THREAD_MONITORING);
            out.writeUnshared(new boolean[] { threadMonitoring, cpuTimeMonitoring });
            out.flush();
            return (boolean[]) in.readUnshared();
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    public synchronized ThreadInfo[] getThreadInfo(long[] ids, int maxDepth) throws ClientException {
        assertConnected();
        try {
            sendAndWaitAck(AgentConstants.CMD_GET_THREAD_INFO);
            out.writeUnshared(ids);
            out.writeInt(maxDepth);
            out.flush();
            return (ThreadInfo[]) in.readUnshared();
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    public synchronized Snapshot getSnapshot() throws ClientException {
        assertConnected();
        try {
            sendAndWaitAck(AgentConstants.CMD_SNAPSHOT);
            return Snapshot.read(in);
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    public void runGc() throws ClientException {
        assertConnected();
        try {
            sendAndWaitAck(AgentConstants.CMD_GC);
        } catch (Exception e) {
            handleException(e);
        }
    }

    public synchronized void restoreClasses(ProgressCallback callback) throws ClientException {
        applyRules("*(*):reject", "", callback);
    }

    public void applyRules(String rules, String defaultRuleOptions, ProgressCallback callback) throws ClientException {
        assertConnected();
        try {
            sendAndWaitAck(AgentConstants.CMD_APPLY_RULES);
            out.writeUTF(defaultRuleOptions);
            out.writeUTF(rules);
            out.flush();
            int batchSize = in.readInt();
            if (callback != null && batchSize > 0) {
                callback.operationStarted(batchSize);
            }
            int i;
            while ((i = in.readInt()) != -1) {
                callback.update(i);
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    public synchronized ClassInfo[] listLoadedClasses() throws ClientException {
        assertConnected();
        try {
            sendAndWaitAck(AgentConstants.CMD_LIST_CLASSES);
            int n = in.readInt();
            ClassInfo[] names = new ClassInfo[n];
            for (int i = 0; i < n; i++) {
                names[i] = new ClassInfo(in.readUTF(), in.readBoolean());
            }
            return names;
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    private void assertConnected() throws ClientException {
        if (!isConnected()) {
            log.error("ClientConnected assertion failed");
            throw new ClientException("Client not connected");
        }
    }

    private void sendAndWaitAck(int cmdId) throws ClientException {
        try {
            out.writeInt(cmdId);
            out.flush();
            expectOk();
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void expectOk() throws ClientException {
        try {
            int status = in.readInt();
            if (status != 0) {
                throw new ClientException("Command Error: code=" + status);
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    /**
     * Handles an exception (this method NEVER returns)
     * @param e Exception to handle
     * 
     * @throws ClientException
     */
    private void handleException(Exception e) throws ClientException {
        close(e);
        if (e instanceof ClientException) {
            throw (ClientException) e;
        }
        if (e instanceof IOException) {
            throw new ClientException("I/O Error", e);
        }
        throw new ClientException("Unexpeced Client Error", e);
    }

    private void close() {
        close(null);
    }

    private void close(Throwable t) {
        if (s != null) {
            try {
                s.close();
            } catch (Exception any) {
            }
            s = null;
            in = null;
            out = null;
            if (t != null) {
                log.error("Client being disconnected due to error", t);
            } else {
                log.info("Client being disconnected normally");
            }
        }
    }
}

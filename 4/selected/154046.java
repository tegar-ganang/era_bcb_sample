package com.continuent.tungsten.replicator.thl.log;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import com.continuent.tungsten.replicator.ReplicatorException;
import com.continuent.tungsten.replicator.thl.THLException;

/**
 * Encapsulates management of connections and their log cursors. Log cursors are
 * a position in a particular log file and can only move in a forward direction.
 * If clients move backward in the log we need to allocated a new cursor.
 * 
 * @author <a href="mailto:robert.hodges@continuent.com">Robert Hodges</a>
 */
public class LogConnectionManager {

    private static Logger logger = Logger.getLogger(LogConnectionManager.class);

    private boolean done;

    private LogConnection writeConnection;

    private List<LogConnection> readConnections = new ArrayList<LogConnection>();

    /**
     * Create a new log cursor manager.
     */
    public LogConnectionManager() {
    }

    /**
     * Stores a new connection.
     */
    public synchronized void store(LogConnection connection) throws ReplicatorException {
        assertNotDone(connection);
        if (writeConnection != null && writeConnection.isDone()) writeConnection = null;
        int readConnectionsSize = readConnections.size();
        for (int i = 0; i < readConnectionsSize; i++) {
            int index = readConnectionsSize - i - 1;
            if (readConnections.get(index).isDone()) readConnections.remove(index);
        }
        if (!connection.isReadonly() && writeConnection != null) throw new THLException("Write connection already exists: connection=" + writeConnection.toString());
        if (connection.isReadonly()) readConnections.add(connection); else writeConnection = connection;
    }

    /**
     * Releases an existing connection.
     */
    public synchronized void release(LogConnection connection) {
        if (done) {
            logger.warn("Attempt to release connection after connection manager shutdown: " + connection);
            return;
        }
        connection.releaseInternal();
        if (connection.isReadonly()) {
            if (!readConnections.remove(connection)) logger.warn("Unable to free read-only connection: " + connection);
        } else {
            if (writeConnection == connection) writeConnection = null; else {
                logger.warn("Unable to free write connection: " + connection);
            }
        }
    }

    /**
     * Releases all connections. This must be called when terminating to ensure
     * file descriptors are released.
     */
    public synchronized void releaseAll() {
        if (!done) {
            if (this.writeConnection != null) {
                writeConnection.releaseInternal();
                writeConnection = null;
            }
            for (LogConnection connection : readConnections) {
                connection.releaseInternal();
            }
            readConnections = null;
            done = true;
        }
    }

    private void assertNotDone(LogConnection client) throws ReplicatorException {
        if (done) {
            throw new THLException("Illegal access on closed log: client=" + client);
        }
    }
}

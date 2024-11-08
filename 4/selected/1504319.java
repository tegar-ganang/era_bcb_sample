package ru.javatalks.net.sc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ConnectionDescriptor - all data that describes connection through SC
 *
 * @author Eugene Matyushkin aka Skipy
 * @version $Id: ConnectionDescriptor.java 4 2011-03-01 15:08:24Z javatalks $
 * @since 01.03.2011
 */
public class ConnectionDescriptor {

    /**
     * Global request counter
     */
    private static final AtomicInteger counter = new AtomicInteger(0);

    /**
     * Channel from SC to proxy
     */
    private SocketChannel upStream;

    /**
     * Channel from client to SC
     */
    private SocketChannel downStream;

    /**
     * File channel to store request/response data
     */
    private FileChannel logStream;

    /**
     * Selection key for up stream to close it when connection is closed
     */
    SelectionKey upKey = null;

    /**
     * Selection key for down stream to close it when connection is closed
     */
    SelectionKey downKey = null;

    /**
     * Current connection id
     */
    private int connectionId;

    /**
     * Connection close flag
     */
    final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Constructs descriptor. Creates file stream with the file name based on current time and connection id.
     *
     * @param upStream   up stream - to proxy/target host
     * @param downStream down stream - from client
     * @throws IOException if error occur when when opening file channel
     */
    ConnectionDescriptor(SocketChannel upStream, SocketChannel downStream) throws IOException {
        this.upStream = upStream;
        this.downStream = downStream;
        connectionId = counter.getAndIncrement();
        logStream = new FileOutputStream("log-" + System.currentTimeMillis() + "-" + connectionId + ".txt").getChannel();
    }

    /**
     * @return down stream
     */
    SocketChannel getDownStream() {
        return downStream;
    }

    /**
     * @return file channel
     */
    FileChannel getLogStream() {
        return logStream;
    }

    /**
     * @return up stream
     */
    SocketChannel getUpStream() {
        return upStream;
    }

    /**
     * Returns current connection ID.
     *
     * @return connection id
     */
    public int getConnectionId() {
        return connectionId;
    }
}

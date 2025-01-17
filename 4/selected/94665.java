package com.ams.server;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class SocketConnector extends Connector {

    private static final int MIN_READ_BUFFER_SIZE = 256;

    private static final int MAX_READ_BUFFER_SIZE = 64 * 1024;

    private SocketChannel channel = null;

    private ByteBuffer readBuffer = null;

    public SocketConnector() {
        super();
    }

    public SocketConnector(SocketChannel channel) {
        super();
        this.channel = channel;
    }

    public void connect(String host, int port) throws IOException {
        if (channel == null || !channel.isOpen()) {
            channel = SocketChannel.open();
            channel.configureBlocking(false);
        }
        ConnectionListner listener = new ConnectionListner() {

            public void connectionEstablished(Connector conn) {
                synchronized (conn) {
                    conn.notifyAll();
                }
            }

            public void connectionClosed(Connector conn) {
            }
        };
        addListner(listener);
        getDispatcher().addChannelToRegister(new ChannelInterestOps(channel, SelectionKey.OP_CONNECT, this));
        long start = System.currentTimeMillis();
        try {
            synchronized (this) {
                channel.connect(new InetSocketAddress(host, port));
                wait(DEFAULT_TIMEOUT_MS);
            }
        } catch (InterruptedException e) {
            throw new IOException("connect interrupted");
        } finally {
            removeListner(listener);
        }
        long now = System.currentTimeMillis();
        if (now - start >= timeout) {
            throw new IOException("connect time out");
        }
    }

    public void finishConnect(SelectionKey key) throws IOException {
        if (channel.isConnectionPending()) {
            channel.finishConnect();
            open();
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    public void readChannel(SelectionKey key) throws IOException {
        if (readBuffer == null || readBuffer.remaining() < MIN_READ_BUFFER_SIZE) {
            readBuffer = ByteBufferFactory.allocate(MAX_READ_BUFFER_SIZE);
        }
        long readBytes = channel.read(readBuffer);
        if (readBytes > 0) {
            ByteBuffer slicedBuffer = readBuffer.slice();
            readBuffer.flip();
            offerReadBuffer(new ByteBuffer[] { readBuffer });
            readBuffer = slicedBuffer;
            if (isClosed()) {
                open();
            }
        } else if (readBytes == -1) {
            close();
            key.cancel();
            key.attach(null);
        }
        keepAlive();
    }

    protected synchronized void writeToChannel(ByteBuffer[] data) throws IOException {
        Selector writeSelector = null;
        SelectionKey writeKey = null;
        int retry = 0;
        int writeTimeout = 1000;
        try {
            while (data != null) {
                long len = channel.write(data);
                if (len < 0) {
                    throw new EOFException();
                }
                boolean hasRemaining = false;
                for (ByteBuffer buf : data) {
                    if (buf.hasRemaining()) {
                        hasRemaining = true;
                        break;
                    }
                }
                if (!hasRemaining) {
                    break;
                }
                if (len > 0) {
                    retry = 0;
                } else {
                    retry++;
                    if (writeSelector == null) {
                        writeSelector = SelectorFactory.getInstance().get();
                        try {
                            writeKey = channel.register(writeSelector, SelectionKey.OP_WRITE);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (writeSelector.select(writeTimeout) == 0) {
                        if (retry > 2) {
                            throw new IOException("Client disconnected");
                        }
                    }
                }
            }
        } finally {
            if (writeKey != null) {
                writeKey.cancel();
                writeKey = null;
            }
            if (writeSelector != null) {
                SelectorFactory.getInstance().free(writeSelector);
            }
            keepAlive();
        }
    }

    public SocketAddress getLocalEndpoint() {
        return channel.socket().getLocalSocketAddress();
    }

    public SocketAddress getRemoteEndpoint() {
        return channel.socket().getRemoteSocketAddress();
    }

    public SelectableChannel getChannel() {
        return channel;
    }
}

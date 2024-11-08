package net.sf.distributor;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CancelledKeyException;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

class DataMover implements Runnable {

    Target target;

    boolean halfClose;

    Selector selector;

    Logger logger;

    List distributionAlgorithms;

    Map clients;

    Map servers;

    List newConnections;

    List channelsToReactivate;

    DelayedMover delayedMover;

    long clientToServerByteCount;

    long serverToClientByteCount;

    Thread thread;

    final int BUFFER_SIZE = 128 * 1024;

    protected DataMover(Distributor distributor, Target target, boolean halfClose) {
        logger = distributor.getLogger();
        distributionAlgorithms = distributor.getDistributionAlgorithms();
        this.target = target;
        this.halfClose = halfClose;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            logger.severe("Error creating selector: " + e.getMessage());
            System.exit(1);
        }
        clients = new HashMap();
        servers = new HashMap();
        newConnections = new LinkedList();
        channelsToReactivate = new LinkedList();
        delayedMover = new DelayedMover();
        clientToServerByteCount = 0;
        serverToClientByteCount = 0;
        thread = new Thread(this, toString());
        thread.start();
    }

    protected void addConnection(Connection conn) {
        synchronized (newConnections) {
            newConnections.add(conn);
        }
        selector.wakeup();
    }

    private boolean processNewConnections() {
        Iterator iter;
        Connection conn;
        SocketChannel client;
        SocketChannel server;
        boolean didSomething = false;
        synchronized (newConnections) {
            iter = newConnections.iterator();
            while (iter.hasNext()) {
                conn = (Connection) iter.next();
                iter.remove();
                client = conn.getClient();
                server = conn.getServer();
                try {
                    logger.finest("Setting channels to non-blocking mode");
                    client.configureBlocking(false);
                    server.configureBlocking(false);
                    clients.put(client, server);
                    servers.put(server, client);
                    logger.finest("Registering channels with selector");
                    client.register(selector, SelectionKey.OP_READ);
                    server.register(selector, SelectionKey.OP_READ);
                } catch (IOException e) {
                    logger.warning("Error setting channels to non-blocking mode: " + e.getMessage());
                    try {
                        logger.fine("Closing channels");
                        client.close();
                        server.close();
                    } catch (IOException ioe) {
                        logger.warning("Error closing channels: " + ioe.getMessage());
                    }
                }
                didSomething = true;
            }
        }
        return didSomething;
    }

    protected void addToReactivateList(SocketChannel channel) {
        synchronized (channelsToReactivate) {
            channelsToReactivate.add(channel);
        }
        selector.wakeup();
    }

    private boolean processReactivateList() {
        Iterator iter;
        SocketChannel channel;
        SelectionKey key;
        boolean didSomething = false;
        synchronized (channelsToReactivate) {
            iter = channelsToReactivate.iterator();
            while (iter.hasNext()) {
                channel = (SocketChannel) iter.next();
                iter.remove();
                key = channel.keyFor(selector);
                try {
                    key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                } catch (CancelledKeyException e) {
                }
                didSomething = true;
            }
        }
        return didSomething;
    }

    public void run() {
        ByteBuffer buffer;
        boolean pncReturn;
        boolean prlReturn;
        int selectFailureOrZeroCount = 0;
        int selectReturn;
        Iterator keyIter;
        SelectionKey key;
        SocketChannel src;
        SocketChannel dst;
        boolean clientToServer;
        boolean readMore;
        int numberOfBytes;
        buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        WHILETRUE: while (true) {
            pncReturn = processNewConnections();
            prlReturn = processReactivateList();
            if (pncReturn || prlReturn) {
                selectFailureOrZeroCount = 0;
            }
            if (selectFailureOrZeroCount >= 10) {
                logger.warning("select appears to be failing repeatedly, pausing");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                selectFailureOrZeroCount = 0;
            }
            selectReturn = 0;
            try {
                selectReturn = selector.select();
                if (selectReturn > 0) {
                    selectFailureOrZeroCount = 0;
                } else {
                    selectFailureOrZeroCount++;
                }
            } catch (IOException e) {
                logger.warning("Error when selecting for ready channel: " + e.getMessage());
                selectFailureOrZeroCount++;
                continue WHILETRUE;
            }
            logger.finest("select reports " + selectReturn + " channels ready to read");
            keyIter = selector.selectedKeys().iterator();
            KEYITER: while (keyIter.hasNext()) {
                key = (SelectionKey) keyIter.next();
                keyIter.remove();
                src = (SocketChannel) key.channel();
                if (clients.containsKey(src)) {
                    clientToServer = true;
                    dst = (SocketChannel) clients.get(src);
                } else if (servers.containsKey(src)) {
                    clientToServer = false;
                    dst = (SocketChannel) servers.get(src);
                } else {
                    key.cancel();
                    continue KEYITER;
                }
                try {
                    readMore = true;
                    while (readMore) {
                        readMore = false;
                        buffer.clear();
                        numberOfBytes = 0;
                        if (src.isConnected()) numberOfBytes = src.read(buffer); else readMore = false;
                        logger.finest("Read " + numberOfBytes + " bytes from " + src);
                        if (numberOfBytes > 0) {
                            if (moveData(buffer, src, dst, clientToServer, key)) {
                                readMore = true;
                            }
                        } else if (numberOfBytes == -1) {
                            handleEOF(key, src, dst, clientToServer);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.warning("Error moving data between channels: " + e.getMessage());
                    closeConnection(src, dst, clientToServer);
                }
            }
        }
    }

    private boolean moveData(ByteBuffer buffer, SocketChannel src, SocketChannel dst, boolean clientToServer, SelectionKey sourceKey) throws IOException {
        Iterator iter;
        DistributionAlgorithm algo;
        ByteBuffer reviewedBuffer;
        buffer.flip();
        if (clientToServer) {
            clientToServerByteCount += buffer.remaining();
        } else {
            serverToClientByteCount += buffer.remaining();
        }
        iter = distributionAlgorithms.iterator();
        reviewedBuffer = buffer;
        while (iter.hasNext()) {
            algo = (DistributionAlgorithm) iter.next();
            if (clientToServer) {
                reviewedBuffer = algo.reviewClientToServerData(src, dst, reviewedBuffer);
            } else {
                reviewedBuffer = algo.reviewServerToClientData(src, dst, reviewedBuffer);
            }
        }
        dst.write(reviewedBuffer);
        if (reviewedBuffer.hasRemaining()) {
            logger.finer("Delaying " + reviewedBuffer.remaining() + " bytes from " + src + " to " + dst);
            ByteBuffer delayedBuffer = ByteBuffer.allocate(reviewedBuffer.remaining());
            delayedBuffer.put(reviewedBuffer);
            delayedBuffer.flip();
            try {
                sourceKey.interestOps(sourceKey.interestOps() ^ SelectionKey.OP_READ);
                delayedMover.addToQueue(new DelayedDataInfo(dst, delayedBuffer, src, clientToServer));
            } catch (CancelledKeyException e) {
            }
            return false;
        } else {
            return true;
        }
    }

    private void handleEOF(SelectionKey key, SocketChannel src, SocketChannel dst, boolean clientToServer) throws IOException {
        if (halfClose) {
            Socket srcSocket;
            Socket dstSocket;
            key.cancel();
            srcSocket = src.socket();
            dstSocket = dst.socket();
            if (srcSocket.isOutputShutdown()) {
                logger.finer("Closing source socket");
                srcSocket.close();
            } else {
                logger.finest("Shutting down source input");
                srcSocket.shutdownInput();
            }
            if (dstSocket.isInputShutdown()) {
                logger.finer("Closing destination socket");
                dstSocket.close();
            } else {
                logger.finest("Shutting down dest output");
                dstSocket.shutdownOutput();
            }
            if (srcSocket.isClosed() && dstSocket.isClosed()) {
                dumpState(src, dst, clientToServer);
            }
        } else {
            closeConnection(src, dst, clientToServer);
        }
    }

    private void closeConnection(SocketChannel src, SocketChannel dst, boolean clientToServer) {
        SocketChannel client;
        SocketChannel server;
        if (clientToServer) {
            client = src;
            server = dst;
        } else {
            server = src;
            client = dst;
        }
        closeConnection(client, server);
    }

    protected void closeConnection(SocketChannel client, SocketChannel server) {
        try {
            logger.fine("Closing channels");
            client.close();
            server.close();
        } catch (IOException ioe) {
            logger.warning("Error closing channels: " + ioe.getMessage());
        }
        dumpState(client, server);
    }

    private void dumpState(SocketChannel src, SocketChannel dst, boolean clientToServer) {
        SocketChannel client;
        SocketChannel server;
        if (clientToServer) {
            client = src;
            server = dst;
        } else {
            server = src;
            client = dst;
        }
        dumpState(client, server);
    }

    private void dumpState(SocketChannel client, SocketChannel server) {
        clients.remove(client);
        servers.remove(server);
        delayedMover.dumpDelayedState(client, server);
    }

    public long getClientToServerByteCount() {
        return clientToServerByteCount;
    }

    public long getServerToClientByteCount() {
        return serverToClientByteCount;
    }

    public String toString() {
        return getClass().getName() + " for " + target.getInetAddress() + ":" + target.getPort();
    }

    protected String getMemoryStats(String indent) {
        String stats;
        stats = indent + clients.size() + " entries in clients Map\n";
        stats += indent + servers.size() + " entries in servers Map\n";
        stats += indent + newConnections.size() + " entries in newConnections List\n";
        stats += indent + channelsToReactivate.size() + " entries in channelsToReactivate List\n";
        stats += indent + selector.keys().size() + " entries in selector key Set\n";
        stats += indent + "DelayedMover:\n";
        stats += delayedMover.getMemoryStats(indent);
        return stats;
    }

    class DelayedMover implements Runnable {

        Selector delayedSelector;

        List queue;

        Map delayedInfo;

        Thread thread;

        DelayedMover() {
            try {
                delayedSelector = Selector.open();
            } catch (IOException e) {
                logger.severe("Error creating selector: " + e.getMessage());
                System.exit(1);
            }
            queue = new LinkedList();
            delayedInfo = new HashMap();
            thread = new Thread(this, toString());
            thread.start();
        }

        void addToQueue(DelayedDataInfo info) {
            synchronized (queue) {
                queue.add(info);
            }
            delayedSelector.wakeup();
        }

        private boolean processQueue() {
            Iterator iter;
            DelayedDataInfo info;
            SocketChannel dst;
            SelectionKey key;
            boolean didSomething = false;
            synchronized (queue) {
                iter = queue.iterator();
                while (iter.hasNext()) {
                    info = (DelayedDataInfo) iter.next();
                    iter.remove();
                    dst = info.getDest();
                    synchronized (delayedInfo) {
                        delayedInfo.put(dst, info);
                    }
                    key = dst.keyFor(delayedSelector);
                    if (key == null) {
                        logger.finest("Registering channel with selector");
                        try {
                            dst.register(delayedSelector, SelectionKey.OP_WRITE);
                        } catch (ClosedChannelException e) {
                        }
                    } else {
                        try {
                            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                        } catch (CancelledKeyException e) {
                        }
                    }
                    didSomething = true;
                }
            }
            return didSomething;
        }

        public void run() {
            int selectReturn;
            int selectFailureOrZeroCount = 0;
            boolean pqReturn;
            Iterator keyIter;
            SelectionKey key;
            SocketChannel dst;
            DelayedDataInfo info;
            ByteBuffer delayedBuffer;
            int numberOfBytes;
            SocketChannel src;
            WHILETRUE: while (true) {
                pqReturn = processQueue();
                if (pqReturn) {
                    selectFailureOrZeroCount = 0;
                }
                if (selectFailureOrZeroCount >= 10) {
                    logger.warning("select appears to be failing repeatedly, pausing");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                    selectFailureOrZeroCount = 0;
                }
                try {
                    selectReturn = delayedSelector.select();
                    if (selectReturn > 0) {
                        selectFailureOrZeroCount = 0;
                    } else {
                        selectFailureOrZeroCount++;
                    }
                } catch (IOException e) {
                    logger.warning("Error when selecting for ready channel: " + e.getMessage());
                    selectFailureOrZeroCount++;
                    continue WHILETRUE;
                }
                logger.finest("select reports " + selectReturn + " channels ready to write");
                keyIter = delayedSelector.selectedKeys().iterator();
                KEYITER: while (keyIter.hasNext()) {
                    key = (SelectionKey) keyIter.next();
                    keyIter.remove();
                    dst = (SocketChannel) key.channel();
                    synchronized (delayedInfo) {
                        info = (DelayedDataInfo) delayedInfo.get(dst);
                    }
                    delayedBuffer = info.getBuffer();
                    try {
                        numberOfBytes = dst.write(delayedBuffer);
                        logger.finest("Wrote " + numberOfBytes + " delayed bytes to " + dst + ", " + delayedBuffer.remaining() + " bytes remain delayed");
                        if (!delayedBuffer.hasRemaining()) {
                            try {
                                key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);
                            } catch (CancelledKeyException e) {
                            }
                            src = info.getSource();
                            dumpDelayedState(info.getDest());
                            addToReactivateList(src);
                        }
                    } catch (IOException e) {
                        logger.warning("Error writing delayed data: " + e.getMessage());
                        closeConnection(dst, info.getSource(), info.isClientToServer());
                    }
                }
            }
        }

        void dumpDelayedState(SocketChannel client, SocketChannel server) {
            dumpDelayedState(client);
            dumpDelayedState(server);
        }

        private void dumpDelayedState(SocketChannel dst) {
            synchronized (delayedInfo) {
                delayedInfo.remove(dst);
            }
        }

        public String toString() {
            return getClass().getName() + " for " + target.getInetAddress() + ":" + target.getPort();
        }

        protected String getMemoryStats(String indent) {
            String stats;
            stats = indent + queue.size() + " entries in queue List\n";
            stats = indent + delayedInfo.size() + " entries in delayedInfo Map\n";
            stats += indent + delayedSelector.keys().size() + " entries in delayedSelector key Set";
            return stats;
        }
    }

    class DelayedDataInfo {

        SocketChannel dst;

        ByteBuffer buffer;

        SocketChannel src;

        boolean clientToServer;

        DelayedDataInfo(SocketChannel dst, ByteBuffer buffer, SocketChannel src, boolean clientToServer) {
            this.dst = dst;
            this.buffer = buffer;
            this.src = src;
            this.clientToServer = clientToServer;
        }

        SocketChannel getDest() {
            return dst;
        }

        ByteBuffer getBuffer() {
            return buffer;
        }

        SocketChannel getSource() {
            return src;
        }

        boolean isClientToServer() {
            return clientToServer;
        }
    }
}

package net.sourceforge.nekomud.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.sourceforge.nekomud.Configuration;
import net.sourceforge.nekomud.service.NetworkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkServiceNioImpl implements NetworkService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public NetworkServiceNioImpl() {
        connections = Collections.synchronizedList(new ArrayList<Connection>());
    }

    public List<Connection> getConnections() {
        return connections;
    }

    public void start() {
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            InetAddress localHost = InetAddress.getLocalHost();
            InetSocketAddress inetSocketAddress = new InetSocketAddress(localHost, configuration.getPort());
            serverSocketChannel.socket().bind(inetSocketAddress);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            selectionThread = new SelectionThread(selector, connections);
            new Thread(selectionThread).start();
        } catch (IOException e) {
            logger.warn("IOException: " + e.getMessage());
        }
    }

    public void stop() {
        selectionThread.stop();
        while (selectionThread.hasStopped() == false) {
        }
        for (Connection c : connections) {
            try {
                c.getChannel().close();
            } catch (Exception e) {
            }
        }
        connections.clear();
        try {
            serverSocketChannel.close();
        } catch (Exception e) {
        }
        try {
            selector.close();
        } catch (Exception e) {
        }
    }

    private List<Connection> connections;

    private SelectionThread selectionThread;

    private Selector selector;

    private ServerSocketChannel serverSocketChannel;

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    private Configuration configuration;
}

class SelectionThread implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public SelectionThread(Selector selector, Collection<Connection> connections) {
        this.connections = connections;
        this.selector = selector;
    }

    public void run() {
        finished = false;
        while (finished == false) {
            try {
                int numKeys = selector.select(10000);
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext(); ) {
                    SelectionKey selectionKey = i.next();
                    i.remove();
                    if (selectionKey.isAcceptable()) {
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        socketChannel.configureBlocking(false);
                        Connection connection = new Connection(socketChannel);
                        connections.add(connection);
                        socketChannel.register(selector, SelectionKey.OP_READ, connection);
                    }
                    if (selectionKey.isWritable()) {
                        Connection connection = (Connection) selectionKey.attachment();
                        connection.handleWrite(selectionKey);
                    }
                    if (selectionKey.isReadable()) {
                        Connection connection = (Connection) selectionKey.attachment();
                        connection.handleRead(selectionKey);
                    }
                }
            } catch (IOException e) {
                logger.warn("IOException: " + e.getMessage());
            }
        }
        stopped = true;
    }

    public void stop() {
        finished = true;
        selector.wakeup();
    }

    public boolean hasStopped() {
        return stopped;
    }

    private Collection<Connection> connections;

    private boolean finished;

    private boolean stopped;

    private Selector selector;
}

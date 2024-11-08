package com.ohua.clustering.client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.ohua.clustering.daemons.SelectorHandler;
import com.ohua.clustering.daemons.SelectorListener;
import com.ohua.clustering.network.OhuaServerSocketFactory;
import com.ohua.clustering.network.OhuaSocketFactory;
import com.ohua.clustering.operators.SimpleNetworkReaderOperator;
import com.ohua.clustering.operators.SimpleNetworkWriterOperator;

/**
 * tasks:
 * <p>
 * 1. Create input and output channels.
 * <p>
 * 2. Create input and output ops and assign channels to them.
 * <p>
 * 3. Push the according input op into the scheduler queue as soon as data is available.
 * <p>
 * 4. Wait for data.
 */
public class LocalConnectionManager implements SelectorListener, Runnable {

    private Logger _logger = Logger.getLogger(getClass().getCanonicalName());

    private Selector _selector = null;

    private SelectorHandler _selectionHandler = new SelectorHandler();

    private List<SimpleNetworkReaderOperator> _inputOperators = new ArrayList<SimpleNetworkReaderOperator>();

    private List<SimpleNetworkWriterOperator> _runningOutputOperators = new ArrayList<SimpleNetworkWriterOperator>();

    private static LocalConnectionManager _instance = null;

    private static AtomicBoolean _wasInstantiated = new AtomicBoolean(false);

    private Set<ServerSocket> _openServerSockets = new HashSet<ServerSocket>();

    private Set<Socket> _openSockets = new HashSet<Socket>();

    public static LocalConnectionManager getInstance() {
        if (_wasInstantiated.compareAndSet(false, true)) {
            _instance = new LocalConnectionManager();
        }
        return _instance;
    }

    private LocalConnectionManager() {
    }

    public void initServerConnections() throws IOException {
        _selector = Selector.open();
        createReaderServerConnections();
    }

    public void initSocketConnections() throws IOException {
        createWriterConnections();
    }

    /**
   * Register an incoming data connection where data is only read not written.
   * @param channel
   * @throws ClosedChannelException
   */
    public void registerSocketChannelForReading(SocketChannel channel) throws ClosedChannelException {
        channel.register(_selector, SelectionKey.OP_READ);
    }

    /**
   * Register an outgoing data connection where data is only written not read.
   * @param channel
   * @throws ClosedChannelException
   */
    public void registerSocketChannelForWriting(SocketChannel channel) throws ClosedChannelException {
        channel.register(_selector, SelectionKey.OP_WRITE);
    }

    /**
   * Wait for all incoming connections to be set up.
   */
    public void waitForConnectionSetup() {
    }

    /**
   * Setup all outgoing connections.
   */
    public void setupOutgoingConnections() {
    }

    public void registerInputOperator(SimpleNetworkReaderOperator inputOperator) {
        _inputOperators.add(inputOperator);
    }

    public void registerOutputOperator(SimpleNetworkWriterOperator outputOperator) {
        _runningOutputOperators.add(outputOperator);
    }

    public Selector getGlobalSelector() {
        return _selector;
    }

    /**
   * A NetworkReaderOperator gets a connection request from the according NetworkWriterOperator.
   * Establish the connection.
   */
    public void handleAcceptable(SelectionKey selKey) throws IOException {
        ServerSocketChannel socketChannel = (ServerSocketChannel) selKey.channel();
        ServerSocket serverSocket = socketChannel.socket();
        Socket socket = serverSocket.accept();
        socket.setOOBInline(true);
        _openSockets.add(socket);
        SimpleNetworkReaderOperator readerOp = (SimpleNetworkReaderOperator) selKey.attachment();
        readerOp.setChannel(socket.getChannel());
        socket.getChannel().configureBlocking(false);
        socket.getChannel().register(getGlobalSelector(), SelectionKey.OP_READ, readerOp);
    }

    /**
   * An output channel of a NetworkWriterOperator has finished its init phase. Make it sensitive
   * to writing.
   */
    public void handleConnectable(SelectionKey selKey) throws IOException {
        SelectableChannel channel = selKey.channel();
        ((SocketChannel) channel).finishConnect();
        if (!(selKey.attachment() instanceof SimpleNetworkReaderOperator)) {
            return;
        }
        channel.register(getGlobalSelector(), SelectionKey.OP_READ, selKey.attachment());
    }

    /**
   * A NetworkReaderOperator has received data from downstream. Push the according operator into
   * the queue of the scheduler.
   */
    public void handleReadable(SelectionKey selKey) throws IOException {
        SimpleNetworkReaderOperator readerOp = (SimpleNetworkReaderOperator) selKey.attachment();
        _selectionHandler.issueChangeRequest(((SocketChannel) selKey.channel()), 0);
        readerOp.activateChannel((SocketChannel) selKey.channel());
    }

    public void waitingForRead(SimpleNetworkReaderOperator readerOp) {
        SocketChannel channel = readerOp.getDataChannel();
        _selectionHandler.issueChangeRequest(channel, SelectionKey.OP_READ);
    }

    public void waitingForRead(SocketChannel channel) {
        if (channel == null) {
            throw new RuntimeException("Invariant broken: channel to be waited on is null!");
        }
        _selectionHandler.issueChangeRequest(channel, SelectionKey.OP_READ);
    }

    /**
   * An output operator is writing to the network.
   */
    public void handleWriteable(SelectionKey selKey) {
        SimpleNetworkWriterOperator writerOp = (SimpleNetworkWriterOperator) selKey.attachment();
        _selectionHandler.issueChangeRequest(((SocketChannel) selKey.channel()), 0);
    }

    public void waitingForWrite(SimpleNetworkWriterOperator writerOp) {
        SocketChannel channel = writerOp.getChannel();
        _selectionHandler.issueChangeRequest(channel, SelectionKey.OP_WRITE);
    }

    public void run() {
        _logger.log(Level.ALL, "Waiting for something to happen on the wire ...");
        _selectionHandler.waitForSelector(getGlobalSelector(), this);
        cleanUp();
    }

    private void createWriterConnections() throws IOException {
        for (SimpleNetworkWriterOperator writer : _runningOutputOperators) {
            Socket socket = OhuaSocketFactory.getInstance().createSocket(writer.getRemoteIP(), writer.getRemotePort());
            SocketChannel channel = socket.getChannel();
            channel.configureBlocking(false);
            writer.setChannel(channel);
            channel.finishConnect();
            channel.register(getGlobalSelector(), SelectionKey.OP_WRITE, writer);
            handleOOBsupport(socket, writer);
        }
    }

    /**
   * Allow out-of-band data to support Fast Travelers over the network!
   * @param socket
   * @throws SocketException
   */
    private void handleOOBsupport(Socket socket, SimpleNetworkWriterOperator writer) throws IOException {
        socket.setOOBInline(true);
        Socket metadataSocket = OhuaSocketFactory.getInstance().createSocket(writer.getRemoteIP(), writer.getRemotePort());
        SocketChannel channel = metadataSocket.getChannel();
        channel.configureBlocking(false);
        writer.setMetadataChannel(channel);
        channel.finishConnect();
        channel.register(getGlobalSelector(), SelectionKey.OP_WRITE, writer);
        System.out.println("OOB connection successfully established!");
    }

    private void cleanUp() {
        for (Socket socket : _openSockets) {
            closeSocket(socket);
        }
        for (ServerSocket serverSocket : _openServerSockets) {
            closeServerSocket(serverSocket);
        }
    }

    private void closeServerSocket(ServerSocket serverSocket) {
        try {
            assert !serverSocket.isClosed();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createReaderServerConnections() {
        for (SimpleNetworkReaderOperator reader : _inputOperators) {
            try {
                ServerSocket socket = OhuaServerSocketFactory.getInstance().createServerSocket(reader.getLocalPort());
                _openServerSockets.add(socket);
                System.out.println("Listening on " + socket.getInetAddress());
                socket.getChannel().register(getGlobalSelector(), SelectionKey.OP_ACCEPT, reader);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getAddress(String remoteIP, int remorePort) {
        return remoteIP + ":" + remorePort;
    }

    public void finish() {
        _selectionHandler.stop();
        _selector.wakeup();
    }
}

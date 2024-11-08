package com.ohua.clustering.daemons;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.ohua.clustering.network.OhuaServerSocketFactory;
import com.ohua.clustering.protocols.SlaveStartupProtocol;
import com.ohua.engine.EngineEvents;
import com.ohua.engine.IOperatorEventListener;
import com.ohua.engine.ISystemEventListener;
import com.ohua.engine.OperatorEvents;
import com.ohua.engine.AbstractProcessManager.ProcessState;
import com.ohua.engine.flowgraph.elements.operator.OperatorCore;
import com.ohua.engine.multithreading.communication.ProcessObserver;

public class SimpleSlaveDaemon implements Runnable, SelectorListener, ProcessObserver, ISystemEventListener {

    private Logger _logger = Logger.getLogger(getClass().getCanonicalName());

    private ByteBuffer _communicationBuffer = ByteBuffer.allocateDirect(1024);

    private int _serverPort = 1113;

    private ServerSocket _serverSocket = null;

    private Socket _metaDataSocket = null;

    private Selector _selector = null;

    private SelectorHandler _selectionHandler = new SelectorHandler();

    private AtomicInteger _pendingCallbacks = new AtomicInteger(0);

    private String _daemonID = null;

    public void run() {
        try {
            _selector = Selector.open();
            _serverSocket = OhuaServerSocketFactory.getInstance().createServerSocket(_serverPort);
            _serverSocket.getChannel().accept();
            _serverSocket.getChannel().register(_selector, SelectionKey.OP_ACCEPT);
            _selectionHandler.waitForSelector(_selector, this);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cleanUp();
            System.out.println("Slave Daemon is shutting down!");
        }
    }

    private void cleanUp() {
        try {
            _metaDataSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            _selector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            _serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getDaemonID() {
        return _daemonID;
    }

    public void setDaemonID(String _daemonid) {
        _daemonID = _daemonid;
    }

    public int getServerPort() {
        return _serverPort;
    }

    public void setServerPort(int port) {
        _serverPort = port;
    }

    public void handleAcceptable(SelectionKey selKey) throws IOException {
        ServerSocketChannel socketChannel = (ServerSocketChannel) selKey.channel();
        ServerSocket serverSocket = socketChannel.socket();
        _metaDataSocket = serverSocket.accept();
        _metaDataSocket.getChannel().configureBlocking(false);
        _metaDataSocket.getChannel().register(_selector, SelectionKey.OP_READ);
        selKey.cancel();
    }

    public void handleConnectable(SelectionKey selKey) throws IOException {
        assert false;
    }

    public void handleReadable(SelectionKey selKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selKey.channel();
        int count = 0;
        _communicationBuffer.clear();
        while ((count = socketChannel.read(_communicationBuffer)) > 0) {
            _communicationBuffer.flip();
            while (_communicationBuffer.hasRemaining()) {
                handleMasterRequests(socketChannel, _communicationBuffer);
            }
            _communicationBuffer.clear();
        }
        if (count == -1) {
            selKey.cancel();
        }
    }

    protected void handleMasterRequests(SocketChannel socketChannel, ByteBuffer buffer) {
        switch(buffer.get()) {
            case SlaveStartupProtocol.MASTER_INIT_PROCESS_SIGNAL:
                _logger.log(Level.ALL, "MASTER_INIT_PROCESS_SIGNAL received");
                System.out.println("MASTER_INIT_PROCESS_SIGNAL received");
                try {
                    sendResponse(socketChannel, SlaveStartupProtocol.SLAVE_PROCESS_INIT_SUCCESS);
                    _logger.log(Level.ALL, "SLAVE_PROCESS_INIT_SUCCESS sent");
                    System.out.println("SLAVE_PROCESS_INIT_SUCCESS sent");
                } catch (Throwable t) {
                    t.printStackTrace();
                    try {
                        sendResponse(socketChannel, SlaveStartupProtocol.SLAVE_PROCESS_INIT_FAILURE);
                        System.out.println("SLAVE_PROCESS_INIT_FAILURE sent");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case SlaveStartupProtocol.MASTER_INIT_CONNECTIONS_SIGNAL:
                _logger.log(Level.ALL, "MASTER_INIT_CONNECTIONS_SIGNAL received");
                System.out.println("MASTER_INIT_CONNECTIONS_SIGNAL received");
                try {
                    sendResponse(socketChannel, SlaveStartupProtocol.SLAVE_CONNECTION_INIT_SUCCESS);
                    _logger.log(Level.ALL, "SLAVE_CONNECTION_INIT_SUCCESS sent");
                    System.out.println("SLAVE_CONNECTION_INIT_SUCCESS sent");
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        sendResponse(socketChannel, SlaveStartupProtocol.SLAVE_CONNECTION_INIT_FAILURE);
                        _logger.log(Level.ALL, "SLAVE_CONNECTION_INIT_FAILURE sent");
                        System.out.println("SLAVE_CONNECTION_INIT_FAILURE sent");
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
                break;
            case SlaveStartupProtocol.MASTER_PREPARE_CYCLE_ANALYSIS_SIGNAL:
                _logger.log(Level.ALL, "MASTER_PREPARE_CYCLE_ANALYSIS_SIGNAL received");
                System.out.println("MASTER_PREPARE_CYCLE_ANALYSIS_SIGNAL received");
                try {
                    raiseSystemEvent(EngineEvents.CYCLE_DETECTION_GRAPH_ANALYSIS_FINISHED);
                    sendResponse(socketChannel, SlaveStartupProtocol.SLAVE_CYCLE_ANALYSIS_PREPARATION_SUCCESS);
                    _logger.log(Level.ALL, "SLAVE_CYCLE_ANALYSIS_PREPARATION_SUCCESS sent");
                    System.out.println("SLAVE_CYCLE_ANALYSIS_PREPARATION_SUCCESS sent");
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        sendResponse(socketChannel, SlaveStartupProtocol.SLAVE_CYCLE_ANALYSIS_PREPARATION_FAILURE);
                        _logger.log(Level.ALL, "SLAVE_CYCLE_ANALYSIS_PREPARATION_FAILURE sent");
                        System.out.println("SLAVE_CYCLE_ANALYSIS_PREPARATION_FAILURE sent");
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
                break;
            case SlaveStartupProtocol.MASTER_RUN_CYCLE_ANALYSIS_SIGNAL:
                _logger.log(Level.ALL, "MASTER_RUN_CYCLE_ANALYSIS_SIGNAL received");
                System.out.println("MASTER_RUN_CYCLE_ANALYSIS_SIGNAL received");
                break;
            case SlaveStartupProtocol.MASTER_START_SIGNAL:
                _logger.log(Level.ALL, "MASTER_START_SIGNAL received");
                System.out.println("MASTER_START_SIGNAL received");
                startProcessing();
                break;
            case SlaveStartupProtocol.MASTER_TEAR_DOWN_SIGNAL:
                _selectionHandler.stop();
                _selector.wakeup();
                break;
            default:
                break;
        }
    }

    protected void startProcessing() {
    }

    protected void sendResponse(SocketChannel socketChannel, byte slaveResponse) throws IOException {
        _communicationBuffer.clear();
        _communicationBuffer.put(slaveResponse);
        _communicationBuffer.flip();
        socketChannel.write(_communicationBuffer);
    }

    public void handleWriteable(SelectionKey selKey) {
        assert false;
    }

    /**
   * <ol>
   * <li> Report to the Master Daemon that we are done with processing.
   * <li> Wakeup the selector and finish. TODO make this another message from the MasterDaemon
   * as we might want to run this process another time.
   * </ol>
   */
    public void notifyOnDoneStatus() {
        try {
            sendResponse(_metaDataSocket.getChannel(), SlaveStartupProtocol.SLAVE_DONE_PROCESSING);
            _logger.log(Level.ALL, "SLAVE_DONE_PROCESSING sent");
            System.out.println("SLAVE_DONE_PROCESSING sent");
        } catch (IOException e) {
            e.printStackTrace();
        }
        raiseSystemEvent(EngineEvents.FINISHED_PROCESSING);
    }

    public void notifyOnEvent(EngineEvents event) {
        if (event != EngineEvents.CYCLE_DETECTION_GRAPH_ANALYSIS_FINISHED) {
            return;
        }
        int pendingCallbacks = _pendingCallbacks.decrementAndGet();
        if (pendingCallbacks > 0) {
            return;
        }
        try {
            sendResponse(_metaDataSocket.getChannel(), SlaveStartupProtocol.SLAVE_CYCLE_ANALYSIS_DONE);
            _logger.log(Level.ALL, "SLAVE_CYCLE_ANALYSIS_DONE sent");
            System.out.println("SLAVE_CYCLE_ANALYSIS_DONE sent");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ProcessState getProcessState() {
        throw new UnsupportedOperationException();
    }

    public void raiseSystemEvent(EngineEvents event) {
        throw new UnsupportedOperationException();
    }

    public void registerForOperatorEvent(OperatorEvents opEvent, IOperatorEventListener opEventListener, OperatorCore op) {
        throw new UnsupportedOperationException();
    }
}

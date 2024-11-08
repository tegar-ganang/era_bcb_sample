package com.ohua.clustering.daemons;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.ohua.clustering.network.OhuaServerSocketFactory;
import com.ohua.clustering.network.OhuaSocketFactory;
import com.ohua.clustering.protocols.SlaveStartupProtocol;

/**
 * Really the idea of a Master Daemon consists of the following:<br>
 * <ol>
 * <li> There should be a server socket that allows slaves to register at the master. This is
 * used when new slave nodes are being added to the set of processing nodes during(!)
 * processing.
 * <li> On init the Master Daemon will receive a list of nodes that are already running and are
 * waiting for a connection. Therefore the first step after spawning the Master Daemon process
 * is to establish a connection to these nodes that are waiting. In the following we will refer
 * to this set of nodes as the <it>start-up set</it>. Among this set the algorithm that splits
 * the graph will take into consideration.
 * <li> The init phase for the start-up set are:
 * <ol>
 * <li> Send the flow graph part that has been computed by the graph-split algorithm from the
 * global data flow graph to the nodes in the start-up set.
 * <li> Construct the process and report status back to Master.
 * <li> Init the process and report status back to Master. Note that the init order for the
 * slave nodes is a backward traversal of the section graph. The Master has to take care of
 * that!
 * <li> Send the start signal to the slaves and wait until all of them have finished processing.
 * </ol>
 * </ol>
 * <p>
 * This is a simplified version of the Master Daemon as it assumes that the nodes in the
 * start-up set already know there part of the data flow graph.
 * 
 * @author sertel
 * 
 */
public class SimpleMasterDaemon implements SelectorListener, Runnable {

    protected Logger _logger = Logger.getLogger(getClass().getCanonicalName());

    public class SlaveAddress {

        public String _IP = null;

        public int _port = -1;
    }

    private Selector _selector = null;

    private SelectorHandler _selectionHandler = new SelectorHandler();

    private List<SlaveAddress> _startUpSetInputSections = new ArrayList<SlaveAddress>();

    private List<SlaveAddress> _startUpSetNonInputSections = new ArrayList<SlaveAddress>();

    private int _serverPort = 1112;

    private ByteBuffer _communicationBuffer = ByteBuffer.allocateDirect(1024);

    private List<ServerSocket> _serverSockets = new ArrayList<ServerSocket>();

    protected List<Socket> _nonInputSectionSockets = new ArrayList<Socket>();

    protected List<Socket> _inputSectionSockets = new ArrayList<Socket>();

    protected AtomicInteger _currentSuccessResponses = new AtomicInteger(0);

    protected int _expectedSuccessResponses = 0;

    public void prepare() throws IOException {
        _selector = Selector.open();
        connectStartUpSet();
        ServerSocket socket = OhuaServerSocketFactory.getInstance().createServerSocket(_serverPort);
        _serverSockets.add(socket);
        socket.getChannel().register(_selector, SelectionKey.OP_ACCEPT);
    }

    private void connectStartUpSet() throws UnknownHostException, IOException {
        connectSlaves(_startUpSetInputSections, _inputSectionSockets);
        connectSlaves(_startUpSetNonInputSections, _nonInputSectionSockets);
    }

    private void connectSlaves(List<SlaveAddress> slaveList, List<Socket> socketList) throws IOException, UnknownHostException, ClosedChannelException {
        for (SlaveAddress nodeAddress : slaveList) {
            Socket socket = OhuaSocketFactory.getInstance().createSocket(nodeAddress._IP, nodeAddress._port);
            socketList.add(socket);
            socket.getChannel().register(_selector, SelectionKey.OP_READ, nodeAddress);
        }
    }

    public void waitForSlaves() {
        _selectionHandler.waitForSelector(_selector, this);
    }

    public void handleConnectable(SelectionKey selKey) throws IOException {
        assert false;
    }

    /**
   * Here we do the following:<br> - Check the return values of the slaves.<br> - Spread the
   * process parts across the slaves<br> - Send the connection information for the output
   * operators<br>
   */
    public void handleReadable(SelectionKey selKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selKey.channel();
        @SuppressWarnings("unused") int count = 0;
        _communicationBuffer.clear();
        while ((count = socketChannel.read(_communicationBuffer)) > 0) {
            _communicationBuffer.flip();
            while (_communicationBuffer.hasRemaining()) {
                handleSlaveResponses(socketChannel, _communicationBuffer);
            }
            _communicationBuffer.clear();
        }
        if (count == -1) {
            selKey.cancel();
        }
    }

    protected boolean handleSlaveResponses(SocketChannel channel, ByteBuffer buffer) throws IOException {
        int currentSuccessResponses = 0;
        switch(buffer.get()) {
            case SlaveStartupProtocol.SLAVE_PROCESS_INIT_FAILURE:
                assert false;
                return false;
            case SlaveStartupProtocol.SLAVE_PROCESS_INIT_SUCCESS:
                _logger.log(Level.ALL, "SLAVE_PROCESS_INIT_SUCCESS received");
                System.out.println("SLAVE_PROCESS_INIT_SUCCESS received");
                currentSuccessResponses = _currentSuccessResponses.incrementAndGet();
                if (_expectedSuccessResponses <= currentSuccessResponses) {
                    initiateConnectionSetUp();
                }
                return true;
            case SlaveStartupProtocol.SLAVE_CONNECTION_INIT_FAILURE:
                assert false;
                return false;
            case SlaveStartupProtocol.SLAVE_CONNECTION_INIT_SUCCESS:
                _logger.log(Level.ALL, "SLAVE_CONNECTION_INIT_SUCCESS received");
                System.out.println("SLAVE_CONNECTION_INIT_SUCCESS received");
                currentSuccessResponses = _currentSuccessResponses.incrementAndGet();
                if (_expectedSuccessResponses <= currentSuccessResponses) {
                    initiateCycleAnalysis();
                }
                return true;
            case SlaveStartupProtocol.SLAVE_CYCLE_ANALYSIS_PREPARATION_FAILURE:
                assert false;
                return false;
            case SlaveStartupProtocol.SLAVE_CYCLE_ANALYSIS_PREPARATION_SUCCESS:
                _logger.log(Level.ALL, "SLAVE_CYCLE_ANALYSIS_PREPARATION_SUCCESS received");
                System.out.println("SLAVE_CYCLE_ANALYSIS_PREPARATION_SUCCESS received");
                currentSuccessResponses = _currentSuccessResponses.incrementAndGet();
                if (_expectedSuccessResponses <= currentSuccessResponses) {
                    igniteCycleAnalysis();
                }
                return true;
            case SlaveStartupProtocol.SLAVE_CYCLE_ANALYSIS_DONE:
                _logger.log(Level.ALL, "SLAVE_CYCLE_ANALYSIS_DONE received");
                System.out.println("SLAVE_CYCLE_ANALYSIS_DONE received");
                currentSuccessResponses = _currentSuccessResponses.incrementAndGet();
                if (_expectedSuccessResponses <= currentSuccessResponses) {
                    initiateProcessing();
                }
                return true;
            case SlaveStartupProtocol.SLAVE_DONE_PROCESSING:
                _logger.log(Level.ALL, "SLAVE_DONE_PROCESSING received");
                System.out.println("SLAVE_DONE_PROCESSING received");
                currentSuccessResponses = _currentSuccessResponses.incrementAndGet();
                if (_expectedSuccessResponses <= currentSuccessResponses) {
                    sendTearDownSignal();
                    _selectionHandler.stop();
                }
                return true;
            default:
                return false;
        }
    }

    private void sendTearDownSignal() {
        _logger.log(Level.ALL, "MASTER_TEAR_DOWN_SIGNAL sent");
        System.out.println("MASTER_TEAR_DOWN_SIGNAL sent");
        _communicationBuffer.clear();
        _communicationBuffer.put(SlaveStartupProtocol.MASTER_TEAR_DOWN_SIGNAL);
        _communicationBuffer.flip();
        _communicationBuffer.mark();
        sendRequest(_inputSectionSockets);
        sendRequest(_nonInputSectionSockets);
    }

    private void igniteCycleAnalysis() {
        _logger.log(Level.ALL, "MASTER_RUN_CYCLE_ANALYSIS_SIGNAL sent");
        System.out.println("MASTER_RUN_CYCLE_ANALYSIS_SIGNAL sent");
        _communicationBuffer.clear();
        _communicationBuffer.put(SlaveStartupProtocol.MASTER_RUN_CYCLE_ANALYSIS_SIGNAL);
        _communicationBuffer.flip();
        _communicationBuffer.mark();
        _expectedSuccessResponses = _nonInputSectionSockets.size() + _inputSectionSockets.size();
        _currentSuccessResponses.set(0);
        sendRequest(_inputSectionSockets);
        sendRequest(_nonInputSectionSockets);
    }

    private void initiateConnectionSetUp() {
        _logger.log(Level.ALL, "MASTER_INIT_CONNECTIONS_SIGNAL sent");
        System.out.println("MASTER_INIT_CONNECTIONS_SIGNAL sent");
        _communicationBuffer.clear();
        _communicationBuffer.put(SlaveStartupProtocol.MASTER_INIT_CONNECTIONS_SIGNAL);
        _communicationBuffer.flip();
        _communicationBuffer.mark();
        _expectedSuccessResponses = _nonInputSectionSockets.size() + _inputSectionSockets.size();
        _currentSuccessResponses.set(0);
        sendRequest(_nonInputSectionSockets);
        sendRequest(_inputSectionSockets);
    }

    private void initiateCycleAnalysis() {
        _logger.log(Level.ALL, "MASTER_PREPARE_CYCLE_ANALYSIS_SIGNAL sent");
        System.out.println("MASTER_PREPARE_CYCLE_ANALYSIS_SIGNAL sent");
        _communicationBuffer.clear();
        _communicationBuffer.put(SlaveStartupProtocol.MASTER_PREPARE_CYCLE_ANALYSIS_SIGNAL);
        _communicationBuffer.flip();
        _communicationBuffer.mark();
        _expectedSuccessResponses = _nonInputSectionSockets.size() + _inputSectionSockets.size();
        _currentSuccessResponses.set(0);
        sendRequest(_nonInputSectionSockets);
        sendRequest(_inputSectionSockets);
    }

    protected void initiateProcessing() {
        _logger.log(Level.ALL, "MASTER_START_SIGNAL sent");
        System.out.println("MASTER_START_SIGNAL sent");
        _communicationBuffer.clear();
        _communicationBuffer.put(SlaveStartupProtocol.MASTER_START_SIGNAL);
        _communicationBuffer.flip();
        _communicationBuffer.mark();
        _expectedSuccessResponses = _nonInputSectionSockets.size() + _inputSectionSockets.size();
        _currentSuccessResponses.set(0);
        sendRequest(_nonInputSectionSockets);
        sendRequest(_inputSectionSockets);
    }

    protected void sendRequest(List<Socket> sectionSockets) {
        for (Socket slaveSocket : sectionSockets) {
            try {
                slaveSocket.getChannel().write(_communicationBuffer);
                _communicationBuffer.reset();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleAcceptable(SelectionKey selKey) throws IOException {
        assert false;
    }

    public void handleWriteable(SelectionKey selKey) {
        assert false;
    }

    public void run() {
        try {
            initiateProcessInit();
            waitForSlaves();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cleanUp();
        }
    }

    protected void cleanUp() {
        for (ServerSocket serverSocket : _serverSockets) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (Socket slaveSockets : _inputSectionSockets) {
            try {
                slaveSockets.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (Socket slaveSockets : _nonInputSectionSockets) {
            try {
                slaveSockets.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initiateProcessInit() throws IOException {
        _logger.log(Level.ALL, "MASTER_INIT_PROCESS_SIGNAL sent");
        System.out.println("MASTER_INIT_PROCESS_SIGNAL sent");
        _communicationBuffer.clear();
        _communicationBuffer.put(SlaveStartupProtocol.MASTER_INIT_PROCESS_SIGNAL);
        _communicationBuffer.flip();
        _communicationBuffer.mark();
        _expectedSuccessResponses = _nonInputSectionSockets.size() + _inputSectionSockets.size();
        _currentSuccessResponses.set(0);
        sendRequest(_nonInputSectionSockets);
        sendRequest(_inputSectionSockets);
    }

    public List<SlaveAddress> getStartUpSetInputSections() {
        return _startUpSetInputSections;
    }

    public void setStartUpSetInputSections(List<SlaveAddress> inputSections) {
        _startUpSetInputSections = inputSections;
    }

    public List<SlaveAddress> getStartUpSetNonInputSections() {
        return _startUpSetNonInputSections;
    }

    public void setStartUpSetNonInputSections(List<SlaveAddress> nonInputSections) {
        _startUpSetNonInputSections = nonInputSections;
    }

    public int getServerPort() {
        return _serverPort;
    }

    public void setServerPort(int port) {
        _serverPort = port;
    }

    protected ByteBuffer getCommunicationBuffer() {
        return _communicationBuffer;
    }
}

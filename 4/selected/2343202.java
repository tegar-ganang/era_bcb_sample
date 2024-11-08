package com.ohua.clustering.daemons;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import com.ohua.clustering.client.LocalProcessManager;
import com.ohua.clustering.client.SlaveGraphRewrite;
import com.ohua.clustering.client.SlaveProcess;
import com.ohua.clustering.network.OhuaSocketFactory;
import com.ohua.clustering.protocols.SlaveStartupProtocol;
import com.ohua.clustering.serialization.SerializableNetworkInfo;
import com.ohua.clustering.serialization.SerializableProcess;

/**
 * This is the class that gets invoked by the script and starts the process on the slave nodes.<br>
 * It still remain TBD on whether we send an XML string that describes the section to be
 * deployed or let this process wait and then send it over via a socket connection.
 * @author sertel
 * 
 */
public class SlaveDaemon implements Runnable, SelectorListener {

    private SocketChannel _socketChannel = null;

    private ByteBuffer _communicationBuffer = ByteBuffer.allocateDirect(1024);

    private String[] _commandLineArgs = null;

    class CommandLineParser {

        int _sectionID = -1;

        String _masterIP = null;

        int _port = -1;

        void parseCommandLineParams(String[] args) {
            assert args.length > 2;
            _masterIP = args[0];
            _port = Integer.parseInt(args[1]);
            _sectionID = Integer.parseInt(args[2]);
        }
    }

    public void run() {
        CommandLineParser parser = new CommandLineParser();
        parser.parseCommandLineParams(_commandLineArgs);
        Selector selector = null;
        try {
            Socket socket = OhuaSocketFactory.getInstance().createSocket(parser._masterIP, parser._port, InetAddress.getLocalHost(), 1222);
            selector = Selector.open();
            socket.getChannel().register(selector, SelectionKey.OP_CONNECT);
            _socketChannel = socket.getChannel();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String[] getCommandLineArgs() {
        return _commandLineArgs;
    }

    public void setCommandLineArgs(String[] lineArgs) {
        _commandLineArgs = lineArgs;
    }

    public void handleAcceptable(SelectionKey selKey) throws IOException {
        assert false;
    }

    public void handleConnectable(SelectionKey selKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selKey.channel();
        socketChannel.register(selKey.selector(), SelectionKey.OP_READ & SelectionKey.OP_WRITE);
    }

    public void handleReadable(SelectionKey selKey) throws IOException {
        byte response;
        byte messageType = _communicationBuffer.get();
        switch(messageType) {
            case SlaveStartupProtocol.MASTER_SENDING_PROCESS:
                response = SlaveStartupProtocol.SLAVE_RESPONSE_SENDING_PROCESS_SUCCESS;
                try {
                    SerializableProcess process = SlaveStartupProtocol.receiveProcessDescription(_communicationBuffer);
                    SlaveProcess newSlaveProcess = new SlaveProcess(process);
                    newSlaveProcess.buildProcess();
                    SlaveGraphRewrite.doInputRewrite(newSlaveProcess.getGraph());
                    LocalProcessManager.registerLocalSlaveProcess(newSlaveProcess);
                } catch (Throwable t) {
                    t.printStackTrace();
                    response = SlaveStartupProtocol.SLAVE_RESPONSE_SENDING_PROCESS_FAILURE;
                }
                startUpCallback(response);
                break;
            case SlaveStartupProtocol.MASTER_SENDING_NETWORK_INFO:
                response = SlaveStartupProtocol.SLAVE_RESPONSE_NETWORK_INFO_SUCCESS;
                try {
                    SerializableNetworkInfo networkInfo = SlaveStartupProtocol.receiveNetworkInfo(_communicationBuffer);
                    SlaveProcess process = LocalProcessManager.getProcess(networkInfo._remoteSectionID);
                    assert process != null;
                    SlaveGraphRewrite.doOutputRewrite(process.getGraph(), networkInfo._networkArcs);
                } catch (Throwable t) {
                    t.printStackTrace();
                    response = SlaveStartupProtocol.SLAVE_RESPONSE_NETWORK_INFO_FAILURE;
                }
                startUpCallback(response);
                break;
            case SlaveStartupProtocol.MASTER_SENDING_START_SIGNAL:
                LocalProcessManager.startAllProcesses();
                break;
            default:
                assert false;
        }
    }

    public void handleWriteable(SelectionKey selKey) {
    }

    public void startUpCallback(byte startUpResult) {
        SlaveStartupProtocol.sendSlaveResponse(startUpResult, _socketChannel);
    }
}

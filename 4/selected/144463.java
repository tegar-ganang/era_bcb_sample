package com.ohua.clustering.daemons;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import com.ohua.clustering.network.OhuaServerSocketFactory;
import com.ohua.clustering.protocols.SlaveStartupProtocol;
import com.ohua.clustering.server.RunningSlave;
import com.ohua.clustering.server.RunningSlavesRegistry;

public class MasterDaemon implements SelectorListener, Runnable {

    private Selector _selector = null;

    private ByteBuffer _communicationBuffer = ByteBuffer.allocateDirect(1024);

    public void prepareServer() throws IOException {
        _selector = Selector.open();
        ServerSocket socket = OhuaServerSocketFactory.getInstance().createServerSocket(1112);
        socket.getChannel().register(_selector, SelectionKey.OP_ACCEPT);
    }

    public void waitForSlaves() {
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
        @SuppressWarnings("unused") int count;
        _communicationBuffer.clear();
        boolean slaveResponseSuccess = false;
        while ((count = socketChannel.read(_communicationBuffer)) > 0) {
            _communicationBuffer.flip();
            while (_communicationBuffer.hasRemaining()) {
                slaveResponseSuccess = handleSlaveResponses(socketChannel, _communicationBuffer);
            }
            _communicationBuffer.clear();
        }
        if (slaveResponseSuccess) {
            socketChannel.socket().close();
            socketChannel.close();
        }
    }

    private boolean handleSlaveResponses(SocketChannel channel, ByteBuffer buffer) {
        switch(buffer.get()) {
            case SlaveStartupProtocol.SLAVE_RESPONSE_NETWORK_INFO_FAILURE:
                return false;
            case SlaveStartupProtocol.SLAVE_RESPONSE_NETWORK_INFO_SUCCESS:
                return true;
            case SlaveStartupProtocol.SLAVE_RESPONSE_SENDING_PROCESS_FAILURE:
                return false;
            case SlaveStartupProtocol.SLAVE_RESPONSE_SENDING_PROCESS_SUCCESS:
                sendNetworkInformation(channel);
                return true;
            default:
                return false;
        }
    }

    private void sendNetworkInformation(SocketChannel channel) {
    }

    public void handleAcceptable(SelectionKey selKey) throws IOException {
        ServerSocketChannel socketChannel = (ServerSocketChannel) selKey.channel();
        ServerSocket serverSocket = socketChannel.socket();
        try {
            Socket socket = serverSocket.accept();
            socket.getChannel().configureBlocking(false);
            RunningSlave slave = new RunningSlave();
            slave.setIp(socket.getInetAddress().getHostAddress());
            slave.setPort(socket.getPort());
            slave.setSocket(socket);
            RunningSlavesRegistry.registerRunningSlave(slave);
            socket.getChannel().register(_selector, SelectionKey.OP_READ & SelectionKey.OP_WRITE);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }
    }

    public void handleWriteable(SelectionKey selKey) {
        SocketChannel socketChannel = (SocketChannel) selKey.channel();
        Socket socket = socketChannel.socket();
        String key = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        RunningSlave slave = RunningSlavesRegistry.findRegisteredSlave(key);
        assert slave != null;
        SlaveStartupProtocol.sendProcessDescription(slave.getSectionID(), slave.getGraph(), slave.getManagerType(), slave.getParallelism(), socketChannel);
    }

    public void run() {
        waitForSlaves();
    }
}

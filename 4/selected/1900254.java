package cb_commonobjects.net.socket;

import cb_commonobjects.logging.GlobalLog;
import cb_commonobjects.net.protocol.AbstractProtocol;
import cb_commonobjects.net.protocol.IProtocolSupport;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 *
 * @author B1
 */
public abstract class AbstractNonBlockingSocketClient extends AbstractSocketClient implements ISocketSender {

    protected int READ_BUFFER_SIZE = 128;

    public AbstractNonBlockingSocketClient(AbstractProtocol thisProtocolPrototype) {
        super(thisProtocolPrototype);
    }

    public AbstractSocketConnection openNewConnection() {
        AbstractSocketConnection mySocketConnection = null;
        try {
            SocketChannel client;
            client = SocketChannel.open();
            client.configureBlocking(false);
            client.connect(new java.net.InetSocketAddress(myServer, myPort));
            Selector selector = Selector.open();
            SelectionKey clientKey = client.register(selector, SelectionKey.OP_CONNECT);
            int myConnectTimeout = 2000;
            while (myConnectTimeout > 0 && !clientKey.isConnectable()) {
                try {
                    Thread.sleep(5);
                    myConnectTimeout -= 5;
                } catch (InterruptedException ex) {
                    GlobalLog.logError(ex);
                }
            }
            if (myConnectTimeout > 0) {
                SocketChannel myChannel;
                try {
                    myChannel = (SocketChannel) clientKey.channel();
                    if (clientKey.isConnectable()) {
                        System.out.println("Server Found");
                        if (myChannel.isConnectionPending()) myChannel.finishConnect();
                        mySocketConnection = this.getNewConnection(myChannel);
                        myConnectionManager.registerConnection(myChannel, mySocketConnection, this);
                    }
                } catch (IOException ex) {
                    GlobalLog.logError(ex);
                }
            }
        } catch (IOException ex) {
            GlobalLog.logError(ex);
        }
        return mySocketConnection;
    }

    public void closeConnection(AbstractSocketConnection thisSocketConnection) {
        try {
            myConnectionManager.deregisterConnection(thisSocketConnection.mySocketObj);
            ((SocketChannel) thisSocketConnection.mySocketObj).close();
        } catch (IOException ex) {
            GlobalLog.logError(ex);
        }
    }

    public boolean send(byte[] thisBytes, AbstractSocketConnection thisSocketConnection) {
        ByteBuffer buffer = null;
        try {
            buffer = ByteBuffer.wrap(thisBytes);
            ((SocketChannel) thisSocketConnection.mySocketObj).write(buffer);
            buffer.clear();
            return true;
        } catch (IOException ex) {
            GlobalLog.logError(ex);
        }
        return false;
    }

    public int receive(ByteArrayOutputStream thisOutStream, AbstractSocketConnection thisSocketConnection) {
        int bytesread = 0;
        try {
            ByteBuffer buffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
            bytesread = ((SocketChannel) thisSocketConnection.mySocketObj).read(buffer);
            if (bytesread > 0) thisOutStream.write(buffer.array(), 0, bytesread);
        } catch (IOException ex) {
            GlobalLog.logError(ex);
        }
        return bytesread;
    }

    @Override
    protected AbstractIncomingDataListenerThread getNewDataListenerThread() {
        return new NonBlockingClientIncomingDataListenerThread();
    }

    protected class NonBlockingClientIncomingDataListenerThread extends AbstractIncomingDataListenerThread {

        @Override
        public void run() {
            try {
                Selector selector = Selector.open();
                SocketChannel myClientChannel;
                ByteArrayOutputStream myByteStream = new ByteArrayOutputStream();
                while (!myAbort) {
                    if (selector.select() > 0) {
                        for (SelectionKey myKey : selector.selectedKeys()) {
                            if (myKey.isReadable()) {
                                myClientChannel = (SocketChannel) myKey.channel();
                                if (mySocketClients.contains(myClientChannel)) {
                                    AbstractSocketConnection myConnection = myConnectionManager.getConnection(myClientChannel);
                                    int myBytesReceived = myConnection.mySocketReceiver.receive(myByteStream, myConnection);
                                    if (myBytesReceived > 0) {
                                        myConnection.getProtocol().onReceiveData(myByteStream);
                                        myByteStream.reset();
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                GlobalLog.logError(ex);
            }
        }
    }
}

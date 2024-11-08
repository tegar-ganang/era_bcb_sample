package gnu.java.nio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

public final class ServerSocketChannelImpl extends ServerSocketChannel {

    private NIOServerSocket serverSocket;

    private boolean connected;

    protected ServerSocketChannelImpl(SelectorProvider provider) throws IOException {
        super(provider);
        serverSocket = new NIOServerSocket(this);
        configureBlocking(true);
    }

    public void finalizer() {
        if (connected) {
            try {
                close();
            } catch (Exception e) {
            }
        }
    }

    protected void implCloseSelectableChannel() throws IOException {
        connected = false;
        serverSocket.close();
    }

    protected void implConfigureBlocking(boolean blocking) throws IOException {
        serverSocket.setSoTimeout(blocking ? 0 : NIOConstants.DEFAULT_TIMEOUT);
    }

    public SocketChannel accept() throws IOException {
        if (!isOpen()) throw new ClosedChannelException();
        if (!serverSocket.isBound()) throw new NotYetBoundException();
        boolean completed = false;
        try {
            begin();
            serverSocket.getPlainSocketImpl().setInChannelOperation(true);
            NIOSocket socket = (NIOSocket) serverSocket.accept();
            completed = true;
            return socket.getChannel();
        } catch (SocketTimeoutException e) {
            return null;
        } finally {
            serverSocket.getPlainSocketImpl().setInChannelOperation(false);
            end(completed);
        }
    }

    public ServerSocket socket() {
        return serverSocket;
    }
}

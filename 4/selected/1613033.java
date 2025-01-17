package org.limewire.net;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import org.limewire.nio.observer.ConnectObserver;

public class ConnectObserverStub implements ConnectObserver {

    private volatile SocketChannel channel;

    private volatile Socket socket;

    private volatile IOException ioException;

    private volatile boolean shutdown;

    public SocketChannel getChannel() throws IOException {
        if (channel == null) {
            channel = SocketChannel.open();
            channel.configureBlocking(false);
        }
        return channel;
    }

    public synchronized void handleConnect(Socket socket) throws IOException {
        this.socket = socket;
        notify();
    }

    public void handleIOException(IOException iox) {
        this.ioException = iox;
    }

    public synchronized void shutdown() {
        this.shutdown = true;
        notify();
    }

    public IOException getIoException() {
        return ioException;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public Socket getSocket() {
        return socket;
    }

    public synchronized void waitForResponse(long timeout) throws Exception {
        wait(timeout);
    }
}

package gnu.java.net.local;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SocketChannel;

/**
 * A local, or unix-domain socket. Unix domain sockets are connected on the
 * local filesystem itself, rather than a remote address.
 */
public final class LocalSocket extends Socket {

    private final LocalSocketImpl localimpl;

    boolean localClosed;

    boolean localConnected;

    public LocalSocket() throws SocketException {
        super();
        localimpl = new LocalSocketImpl();
    }

    public LocalSocket(LocalSocketAddress addr) throws SocketException {
        this();
        try {
            connect(addr);
        } catch (IOException ioe) {
            SocketException se = new SocketException();
            se.initCause(ioe);
            throw se;
        }
    }

    LocalSocket(boolean nocreate) throws IOException {
        super();
        localimpl = new LocalSocketImpl(nocreate);
    }

    public void bind(SocketAddress bindpoint) throws IOException {
        throw new SocketException("binding local client sockets is nonsensical");
    }

    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        if (isClosed()) {
            throw new SocketException("socket is closed");
        }
        if (!(endpoint instanceof LocalSocketAddress)) {
            throw new IllegalArgumentException("socket address is not a local address");
        }
        if (getChannel() != null && !getChannel().isBlocking()) {
            throw new IllegalBlockingModeException();
        }
        try {
            localimpl.doCreate();
            localimpl.localConnect((LocalSocketAddress) endpoint);
        } catch (IOException ioe) {
            close();
            throw ioe;
        }
        localConnected = true;
    }

    public InetAddress getInetAddress() {
        return null;
    }

    public InetAddress getLocalAddress() {
        return null;
    }

    public int getPort() {
        return -1;
    }

    public int getLocalPort() {
        return -1;
    }

    public SocketChannel getChannel() {
        return null;
    }

    public SocketAddress getLocalSocketAddress() {
        return localimpl.getLocalAddress();
    }

    public SocketAddress getRemoteSocketAddress() {
        return localimpl.getRemoteAddress();
    }

    public InputStream getInputStream() throws IOException {
        return localimpl.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return localimpl.getOutputStream();
    }

    public void sendUrgentData(int b) throws IOException {
        localimpl.sendUrgentData(b);
    }

    public synchronized void close() throws IOException {
        localimpl.close();
        localClosed = true;
    }

    public void shutdownInput() throws IOException {
        localimpl.shutdownInput();
    }

    public void shutdownOutput() throws IOException {
        localimpl.shutdownOutput();
    }

    public boolean isClosed() {
        return localClosed;
    }

    public boolean isBound() {
        return false;
    }

    public boolean isConnected() {
        return localConnected;
    }

    public void setTcpNoDelay(boolean b) throws SocketException {
        throw new SocketException("local sockets do not support this option");
    }

    public boolean getTcpNoDelay() throws SocketException {
        throw new SocketException("local sockets do not support this option");
    }

    public void setSoLinger(boolean b, int i) throws SocketException {
        throw new SocketException("local sockets do not support this option");
    }

    public int getSoLinger() throws SocketException {
        throw new SocketException("local sockets do not support this option");
    }

    public void setOOBInline(boolean b) throws SocketException {
        throw new SocketException("local sockets do not support this option");
    }

    public boolean getOOBInline() throws SocketException {
        throw new SocketException("local sockets do not support this option");
    }

    public void setSoTimeout(int i) throws SocketException {
    }

    public int getSoTimeout() throws SocketException {
        return 0;
    }

    public void setSendBufferSize(int i) throws SocketException {
        throw new SocketException("local sockets do not support this option");
    }

    public int getSendBufferSize() throws SocketException {
        throw new SocketException("local sockets do not support this option");
    }

    public void setReceiveBufferSize(int i) throws SocketException {
        throw new SocketException("local sockets do not support this option");
    }

    public int getReceiveBufferSize() throws SocketException {
        throw new SocketException("local sockets do not support this option");
    }

    public void setKeepAlive(boolean b) throws SocketException {
        throw new SocketException("local sockets do not support this option");
    }

    public boolean getKeepAlive() throws SocketException {
        throw new SocketException("local sockets do not support this option");
    }

    public void setTrafficClass(int i) throws SocketException {
        throw new SocketException("local sockets do not support this option");
    }

    public int getTrafficClass() throws SocketException {
        throw new SocketException("local sockets do not support this option");
    }

    public void setReuseAddress(boolean b) throws SocketException {
        throw new SocketException("local sockets do not support this option");
    }

    public boolean getReuseAddress() throws SocketException {
        throw new SocketException("local sockets do not support this option");
    }

    LocalSocketImpl getLocalImpl() {
        return localimpl;
    }
}

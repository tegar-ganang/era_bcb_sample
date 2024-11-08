package net.jxta.impl.endpoint.tls;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import net.jxta.impl.endpoint.IPUtils;

/**
 * A "shim" socket which we provide to the TLS layer.
 */
public class TlsSocket extends Socket {

    final JTlsInputStream input;

    final JTlsOutputStream output;

    boolean connected = true;

    /**
     * Creates a new instance of TlsSocket
     */
    public TlsSocket(JTlsInputStream useInput, JTlsOutputStream useOutput) {
        input = useInput;
        output = useOutput;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        input.close();
        output.close();
        connected = false;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return input;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
        return output;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBound() {
        return connected;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public boolean isConnected() {
        return connected;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public InetAddress getInetAddress() {
        return IPUtils.LOOPBACK;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public InetAddress getLocalAddress() {
        return IPUtils.ANYADDRESS;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public SocketAddress getRemoteSocketAddress() {
        return new InetSocketAddress(IPUtils.LOOPBACK, 0);
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public SocketAddress getLocalSocketAddress() {
        return new InetSocketAddress(IPUtils.ANYADDRESS, 0);
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public SocketChannel getChannel() {
        return null;
    }
}

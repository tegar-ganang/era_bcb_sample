package jaxlib.net.socket;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ServerSocketFactory;
import jaxlib.lang.Booleans;
import jaxlib.lang.Objects;
import jaxlib.util.CheckArg;

/**
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: ServerSocketConfiguration.java 3016 2011-11-28 06:17:26Z joerg_wassmer $
 */
public class ServerSocketConfiguration extends ServerSocketFactory implements Cloneable, Serializable {

    /**
   * @since JaXLib 1.0
   */
    private static final long serialVersionUID = 1L;

    /**
   * @serial
   * @since JaXLib 1.0
   */
    private int backlog;

    /**
   * @serial
   * @since JaXLib 1.0
   */
    private volatile boolean configurationReadOnly;

    /**
   * @serial
   * @since JaXLib 1.0
   */
    private SocketPerformancePreferences performancePreferences;

    /**
   * @serial
   * @since JaXLib 1.0
   */
    private int receiveBufferSize;

    /**
   * @serial
   * @since JaXLib 1.0
   */
    private boolean reuseAddress = true;

    /**
   * @serial
   * @since JaXLib 1.0
   */
    private int soTimeout;

    /**
   * @serial
   * @since JaXLib 1.0
   */
    private final ServerSocketFactory factory;

    /**
   * @serial
   * @since JaXLib 1.0
   */
    final SelectorProvider provider;

    public ServerSocketConfiguration() {
        super();
        this.factory = null;
        this.provider = null;
    }

    public ServerSocketConfiguration(@Nullable final ServerSocketFactory factory) {
        super();
        this.factory = factory;
        this.provider = null;
    }

    public ServerSocketConfiguration(@Nullable final SelectorProvider provider) {
        super();
        this.factory = null;
        this.provider = provider;
    }

    private void checkSetProperty() {
        if (this.configurationReadOnly) throw new IllegalStateException("Configuration is set to read-only");
    }

    @Nonnull
    public ServerSocketConfiguration asReadOnlyConfiguration() {
        if (this.configurationReadOnly) {
            return this;
        } else {
            ServerSocketConfiguration clone = clone();
            clone.setConfigurationReadOnly();
            return clone;
        }
    }

    @Override
    public ServerSocketConfiguration clone() {
        final ServerSocketConfiguration clone;
        try {
            clone = (ServerSocketConfiguration) super.clone();
        } catch (final CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
        clone.configurationReadOnly = false;
        return clone;
    }

    public void copyPropertiesFrom(final ServerSocket socket) throws IOException {
        int receiveBufferSize = socket.getReceiveBufferSize();
        boolean reuseAddress = socket.getReuseAddress();
        int soTimeout = socket.getSoTimeout();
        checkSetProperty();
        this.receiveBufferSize = receiveBufferSize;
        this.reuseAddress = reuseAddress;
        this.soTimeout = soTimeout;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ServerSocketConfiguration)) return false;
        final ServerSocketConfiguration b = (ServerSocketConfiguration) o;
        return ((this.receiveBufferSize == b.receiveBufferSize) && (this.backlog == b.backlog) && (this.soTimeout == b.soTimeout) && (this.reuseAddress == b.reuseAddress) && Objects.equals(this.performancePreferences, b.performancePreferences));
    }

    @Override
    public int hashCode() {
        int h = this.backlog;
        h = (31 * h) + Objects.hashCode(this.performancePreferences);
        h = (31 * h) + Booleans.hashCode(this.reuseAddress);
        h = (31 * h) + this.receiveBufferSize;
        h = (31 * h) + this.soTimeout;
        return h;
    }

    public void bindSocket(final ServerSocket socket, final InetSocketAddress bindAddress) throws IOException {
        CheckArg.notNull(bindAddress, "bindAddress");
        configureSocket(socket);
        socket.bind(bindAddress, getBacklog());
    }

    public void configureSocket(final ServerSocket socket) throws IOException {
        socket.setReuseAddress(getReuseAddress());
        socket.setSoTimeout(getSoTimeout());
        final SocketPerformancePreferences performancePreferences = getPerformancePreferences();
        if (performancePreferences != null) performancePreferences.configureSocket(socket);
        final int receiveBufferSize = getReceiveBufferSize();
        if (receiveBufferSize != 0) socket.setReceiveBufferSize(receiveBufferSize);
    }

    @Override
    @Nonnull
    public ServerSocket createServerSocket() throws IOException {
        final ServerSocket socket;
        if (this.factory != null) socket = this.factory.createServerSocket(); else if (this.provider != null) socket = this.provider.openServerSocketChannel().socket(); else socket = ServerSocketChannel.open().socket();
        configureSocket(socket);
        return socket;
    }

    @Nonnull
    public ServerSocketChannel createServerSocketChannel() throws IOException {
        ServerSocket socket;
        if (this.factory != null) {
            if (this.factory instanceof ServerSocketConfiguration) {
                socket = ((ServerSocketConfiguration) this.factory).createServerSocketChannel().socket();
            } else {
                socket = this.factory.createServerSocket();
                if (socket.getChannel() == null) {
                    throw new IllegalStateException("The underlying factory created a server socket without channel: " + this.factory);
                }
            }
        } else if (this.provider != null) {
            socket = this.provider.openServerSocketChannel().socket();
        } else {
            socket = ServerSocketChannel.open().socket();
        }
        configureSocket(socket);
        return socket.getChannel();
    }

    @Override
    @Nonnull
    public ServerSocket createServerSocket(final int port) throws IOException {
        return createServerSocket(port, this.backlog, null);
    }

    @Override
    @Nonnull
    public ServerSocket createServerSocket(final int port, final int backlog) throws IOException {
        return createServerSocket(port, backlog, null);
    }

    @Override
    public ServerSocket createServerSocket(final int port, final int backlog, @Nullable final InetAddress ifAddress) throws IOException {
        final ServerSocket socket;
        if (this.factory != null) {
            socket = this.factory.createServerSocket(port, backlog, ifAddress);
        } else {
            final ServerSocketChannel channel = (this.provider != null) ? this.provider.openServerSocketChannel() : ServerSocketChannel.open();
            socket = channel.socket();
            socket.bind(new InetSocketAddress(ifAddress, port), backlog);
        }
        configureSocket(socket);
        return socket;
    }

    public int getBacklog() {
        return this.backlog;
    }

    @CheckForNull
    public SocketPerformancePreferences getPerformancePreferences() {
        return this.performancePreferences;
    }

    public int getReceiveBufferSize() {
        return this.receiveBufferSize;
    }

    public boolean getReuseAddress() {
        return this.reuseAddress;
    }

    public int getSoTimeout() {
        return this.soTimeout;
    }

    public boolean isConfigurationReadOnly() {
        return this.configurationReadOnly;
    }

    public void setBacklog(final int v) {
        CheckArg.notNegative(v, "backlog");
        checkSetProperty();
        this.backlog = v;
    }

    public void setConfigurationReadOnly() {
        this.configurationReadOnly = true;
    }

    public void setPerformancePreferences(@Nullable final SocketPerformancePreferences prefs) {
        checkSetProperty();
        this.performancePreferences = prefs;
    }

    public void setPerformancePreferences(final int connectionTime, final int latency, final int bandwidth) {
        checkSetProperty();
        SocketPerformancePreferences p = this.performancePreferences;
        if ((p == null) || (p.connectionTime != connectionTime) || (p.latency != latency) || (p.bandwidth != bandwidth)) {
            this.performancePreferences = new SocketPerformancePreferences(connectionTime, latency, bandwidth);
        }
    }

    /**
   * Set the size of the receive buffer of sockets accepted by the server socket.
   *
   * @param size
   *  the preferred size; {@code zero} to use the socket's default.
   *
   * @throws IllegalArgumentException
   *  if {@code size < 0}.
   *
   * @see ServerSocket#setReceiveBufferSize(int)
   *
   * @since JaXLib 1.0
   */
    public void setReceiveBufferSize(final int receiveBufferSize) {
        CheckArg.notNegative(receiveBufferSize, "receiveBufferSize");
        checkSetProperty();
        this.receiveBufferSize = receiveBufferSize;
    }

    public void setReuseAddress(final boolean on) {
        checkSetProperty();
        this.reuseAddress = on;
    }

    public void setSoTimeout(final int timeoutMillis) {
        CheckArg.notNegative(timeoutMillis, "timeoutMillis");
        checkSetProperty();
        this.soTimeout = timeoutMillis;
    }
}

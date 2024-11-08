package net.pesahov.remote.socket.rmi;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import net.pesahov.common.utils.Exceptions;
import net.pesahov.remote.socket.RemoteChannel;
import net.pesahov.remote.socket.UnderlyingSocketProxy;

/**
 * @author Pesahov Dmitry
 * @since 2.0
 */
abstract class RmiUnderlyingSocketProxy implements UnderlyingSocketProxy {

    /**
     * Parent RmiUnderlyingSocketFactory instance.
     */
    protected RmiUnderlyingSocketFactory factory;

    /**
     * Factory RmiUnderlyingInvoker instance.
     */
    protected RmiUnderlyingInvoker invoker;

    /**
     * Creates new RmiUnderlyingSocketProxy instance.
     * @param factory Parent RmiUnderlyingSocketFactory instance.
     */
    protected RmiUnderlyingSocketProxy(RmiUnderlyingSocketFactory factory) throws SocketException {
        if (factory == null) throw new IllegalArgumentException("RmiUnderlyingSocketFactory instance is null!");
        try {
            this.factory = factory;
            this.invoker = factory.createRmiUnderlyingInvoker();
        } catch (IOException ex) {
            throw Exceptions.getNested(SocketException.class, ex);
        }
    }

    /**
     * Creates new RmiUnderlyingSocketProxy instance.
     * @param factory Parent {@link RmiUnderlyingSocketFactory} instance.
     * @param invoker Parent {@link RmiUnderlyingInvoker} instance.
     */
    protected RmiUnderlyingSocketProxy(RmiUnderlyingSocketFactory factory, RmiUnderlyingInvoker invoker) {
        if (factory == null) throw new IllegalArgumentException("RmiUnderlyingSocketFactory instance is null!");
        this.factory = factory;
        this.invoker = invoker;
    }

    public void close() throws IOException {
        invoker.invoke(Void.TYPE, "close", new Class<?>[] {}, new Serializable[] {});
    }

    public RemoteChannel getChannel() {
        try {
            return invoker.invoke(RemoteChannel.class, "getChannel", new Class<?>[] {}, new Serializable[] {});
        } catch (IOException ex) {
            Exceptions.throwNested(RuntimeException.class, ex);
            return null;
        }
    }

    public InetAddress getInetAddress() {
        try {
            return invoker.invoke(InetAddress.class, "getInetAddress", new Class<?>[] {}, new Serializable[] {});
        } catch (IOException ex) {
            Exceptions.throwNested(RuntimeException.class, ex);
            return null;
        }
    }

    public int getLocalPort() {
        try {
            return invoker.invoke(Integer.TYPE, "getLocalPort", new Class<?>[] {}, new Serializable[] {});
        } catch (IOException ex) {
            Exceptions.throwNested(RuntimeException.class, ex);
            return 0;
        }
    }

    public SocketAddress getLocalSocketAddress() {
        try {
            return invoker.invoke(SocketAddress.class, "getLocalSocketAddress", new Class<?>[] {}, new Serializable[] {});
        } catch (IOException ex) {
            Exceptions.throwNested(RuntimeException.class, ex);
            return null;
        }
    }

    public int getReceiveBufferSize() throws SocketException {
        try {
            return invoker.invoke(Integer.TYPE, "getReceiveBufferSize", new Class<?>[] {}, new Serializable[] {});
        } catch (IOException ex) {
            Exceptions.throwNested(SocketException.class, ex);
            return 0;
        }
    }

    public boolean getReuseAddress() throws SocketException {
        try {
            return invoker.invoke(Boolean.TYPE, "getReuseAddress", new Class<?>[] {}, new Serializable[] {});
        } catch (IOException ex) {
            Exceptions.throwNested(SocketException.class, ex);
            return false;
        }
    }

    public boolean isBound() {
        try {
            return invoker.invoke(Boolean.TYPE, "isBound", new Class<?>[] {}, new Serializable[] {});
        } catch (IOException ex) {
            Exceptions.throwNested(RuntimeException.class, ex);
            return false;
        }
    }

    public boolean isClosed() {
        try {
            return invoker.invoke(Boolean.TYPE, "isClosed", new Class<?>[] {}, new Serializable[] {});
        } catch (IOException ex) {
            Exceptions.throwNested(RuntimeException.class, ex);
            return false;
        }
    }

    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        try {
            invoker.invoke(Void.TYPE, "setPerformancePreferences", new Class<?>[] { Integer.TYPE, Integer.TYPE, Integer.TYPE }, new Serializable[] { connectionTime, latency, bandwidth });
        } catch (IOException ex) {
            Exceptions.throwNested(RuntimeException.class, ex);
        }
    }

    public void setReceiveBufferSize(int size) throws SocketException {
        try {
            invoker.invoke(Void.TYPE, "setReceiveBufferSize", new Class<?>[] { Integer.TYPE }, new Serializable[] { size });
        } catch (IOException ex) {
            Exceptions.throwNested(SocketException.class, ex);
        }
    }

    public void setReuseAddress(boolean on) throws SocketException {
        try {
            invoker.invoke(Void.TYPE, "setReuseAddress", new Class<?>[] { Boolean.TYPE }, new Serializable[] { on });
        } catch (IOException ex) {
            Exceptions.throwNested(SocketException.class, ex);
        }
    }

    public void setSoTimeout(int timeout) throws SocketException {
        try {
            invoker.invoke(Void.TYPE, "setSoTimeout", new Class<?>[] { Integer.TYPE }, new Serializable[] { timeout });
        } catch (IOException ex) {
            Exceptions.throwNested(SocketException.class, ex);
        }
    }
}

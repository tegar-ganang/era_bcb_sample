package gnu.java.nio;

import gnu.java.net.PlainSocketImpl;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * @author Michael Koch (konqueror@gmx.de)
 */
public final class NIOServerSocket extends ServerSocket {

    private ServerSocketChannelImpl channel;

    protected NIOServerSocket(ServerSocketChannelImpl channel) throws IOException {
        super();
        this.channel = channel;
    }

    public PlainSocketImpl getPlainSocketImpl() {
        try {
            final Object t = this;
            final Method method = ServerSocket.class.getDeclaredMethod("getImpl", new Class[0]);
            method.setAccessible(true);
            PrivilegedExceptionAction action = new PrivilegedExceptionAction() {

                public Object run() throws Exception {
                    return method.invoke(t, new Object[0]);
                }
            };
            return (PlainSocketImpl) AccessController.doPrivileged(action);
        } catch (Exception e) {
            Error error = new InternalError("unable to invoke method ServerSocket.getImpl()");
            error.initCause(e);
            throw error;
        }
    }

    public ServerSocketChannel getChannel() {
        return channel;
    }

    public Socket accept() throws IOException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkListen(getLocalPort());
        SocketChannel socketChannel = channel.provider().openSocketChannel();
        implAccept(socketChannel.socket());
        return socketChannel.socket();
    }
}

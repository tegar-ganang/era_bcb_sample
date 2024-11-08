package net.sf.alc.connection.tcp;

import java.nio.channels.SocketChannel;

/**
 * @author alain.caron
 */
public final class TcpConnectionContext extends SocketConnectionContext {

    private final SocketChannel mChannel;

    TcpConnectionContext(SocketChannel aChannel, Reactor aReactor) {
        super(aReactor);
        mChannel = aChannel;
    }

    public final SocketChannel getChannel() {
        return mChannel;
    }
}

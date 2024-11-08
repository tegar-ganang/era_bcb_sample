package gnu.java.nio;

import gnu.java.net.PlainSocketImpl;
import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;

/**
 * @author Michael Koch
 */
public final class NIOSocket extends Socket {

    private PlainSocketImpl impl;

    private SocketChannelImpl channel;

    protected NIOSocket(PlainSocketImpl impl, SocketChannelImpl channel) throws IOException {
        super(impl);
        this.impl = impl;
        this.channel = channel;
    }

    public final PlainSocketImpl getPlainSocketImpl() {
        return impl;
    }

    final void setChannel(SocketChannelImpl channel) {
        this.impl = channel.getPlainSocketImpl();
        this.channel = channel;
    }

    public final SocketChannel getChannel() {
        return channel;
    }
}

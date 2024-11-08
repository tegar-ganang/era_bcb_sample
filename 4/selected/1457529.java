package gnu.java.nio;

import gnu.java.net.PlainDatagramSocketImpl;
import java.net.DatagramSocket;
import java.nio.channels.DatagramChannel;

/**
 * @author Michael Koch
 */
public final class NIODatagramSocket extends DatagramSocket {

    private PlainDatagramSocketImpl impl;

    private DatagramChannelImpl channel;

    public NIODatagramSocket(PlainDatagramSocketImpl impl, DatagramChannelImpl channel) {
        super(impl);
        this.impl = impl;
        this.channel = channel;
    }

    public final PlainDatagramSocketImpl getPlainDatagramSocketImpl() {
        return impl;
    }

    public final DatagramChannel getChannel() {
        return channel;
    }
}

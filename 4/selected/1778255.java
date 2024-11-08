package transport.channel;

import java.nio.channels.SocketChannel;

/**
 * 
 */
public class AddChannelCommand {

    private SocketChannel channel;

    public AddChannelCommand(SocketChannel ch) {
        super();
        channel = ch;
    }

    public SocketChannel getChannel() {
        return channel;
    }
}

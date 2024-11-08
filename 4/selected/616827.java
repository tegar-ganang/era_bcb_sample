package transport.channel;

import java.nio.channels.SocketChannel;

/**
 * 
 */
public class GetChannelResponse {

    private int channelID;

    private SocketChannel channel;

    public GetChannelResponse(int id, SocketChannel ch) {
        super();
        channelID = id;
        channel = ch;
    }

    public int getID() {
        return channelID;
    }

    public SocketChannel getChannel() {
        return channel;
    }
}

package tools;

import java.nio.channels.SocketChannel;

public class MainChannel {

    public static SocketChannel sc = null;

    public static void setChannel(SocketChannel socketC) {
        sc = socketC;
    }

    public static SocketChannel getChannel() {
        return sc;
    }
}

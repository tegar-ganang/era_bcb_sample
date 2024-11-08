package org.jeuron.jlightning.container.initiator;

import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Mike Karrys
 * @since 1.1
 */
public class StandardSocketInitiator extends AbstractInitiator {

    private SocketChannel socketChannel = null;

    public Channel getChannel() {
        return socketChannel;
    }

    public void setChannel(Channel channel) {
        if (channel instanceof SocketChannel) {
            this.socketChannel = (SocketChannel) channel;
        }
    }
}

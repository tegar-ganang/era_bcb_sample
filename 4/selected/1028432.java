package org.jeuron.jlightning.container.listener;

import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;

/**
 *
 * @author Mike Karrys
 * @since 1.1
 */
public class StandardSocketListener extends AbstractListener {

    private ServerSocketChannel serverSocketChannel = null;

    public Channel getChannel() {
        return serverSocketChannel;
    }

    public void setChannel(Channel channel) {
        if (channel instanceof ServerSocketChannel) {
            this.serverSocketChannel = (ServerSocketChannel) channel;
        }
    }
}

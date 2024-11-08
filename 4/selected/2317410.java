package org.jeuron.jlightning.container.listener;

import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;

/**
 *
 * @author Mike Karrys
 * @since 1.1
 */
public class StandardDatagramListener extends AbstractListener {

    private DatagramChannel datagramChannel = null;

    public Channel getChannel() {
        return datagramChannel;
    }

    public void setChannel(Channel channel) {
        if (channel instanceof DatagramChannel) {
            this.datagramChannel = (DatagramChannel) channel;
        }
    }
}

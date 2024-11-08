package org.xmatthew.mypractise;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.xmatthew.spy2servers.core.AbstractComponent;
import org.xmatthew.spy2servers.core.MessageAlertChannel;
import org.xmatthew.spy2servers.core.MessageAlertChannelActiveAwareComponent;

/**
 * @author Matthew Xie
 *
 */
@org.xmatthew.spy2servers.annotation.MessageAlertChannelActiveAwareComponent(name = "SimpleChannelAwareComponent")
public class SimpleChannelAwareComponent extends AbstractComponent implements MessageAlertChannelActiveAwareComponent {

    private boolean started;

    private List<MessageAlertChannel> channels = Collections.synchronizedList(new LinkedList<MessageAlertChannel>());

    public List<MessageAlertChannel> getChannels() {
        return channels;
    }

    public void onMessageAlertChannelActive(MessageAlertChannel channel) {
        if (!started) {
            return;
        }
        channels.add(channel);
        printChannel(channel);
    }

    public void startup() {
        started = true;
        setStatusRun();
    }

    public void stop() {
        started = false;
        setStatusStop();
    }

    private void printChannel(MessageAlertChannel channel) {
        if (channel != null) {
            System.out.println("channel aware component say:");
            System.out.print("spyComponent is: ");
            System.out.println(channel.getSpyComponent());
            System.out.print("alertComponent is: ");
            System.out.println(channel.getAlertComponent());
            System.out.print("message is: ");
            System.out.println(channel.getMessage());
        }
    }
}

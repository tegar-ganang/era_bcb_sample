package org.maverickdbms.server.ssh;

import net.lag.jaramiko.Channel;
import org.maverickdbms.basic.term.PseudoTerminal;

public class ChannelNode implements Comparable {

    private int id;

    private String kind;

    private Channel channel;

    private PseudoTerminal pty;

    ChannelNode(int id, String kind) {
        this.id = id;
        this.kind = kind;
    }

    public int compareTo(Object o) {
        return id - ((ChannelNode) o).getId();
    }

    public Channel getChannel() {
        return channel;
    }

    public int getId() {
        return id;
    }

    public PseudoTerminal getPty() {
        return pty;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void setPty(PseudoTerminal pty) {
        this.pty = pty;
    }
}

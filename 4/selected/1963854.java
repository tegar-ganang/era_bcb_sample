package net.sf.jnclib.tp.ssh2.server;

import net.sf.jnclib.tp.ssh2.Channel;
import net.sf.jnclib.tp.ssh2.term.PseudoTerminal;

public final class ChannelNode {

    private int id;

    private String kind;

    private Channel channel;

    private PseudoTerminal pty;

    private Object service;

    public ChannelNode(int id, String kind) {
        this.id = id;
        this.kind = kind;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ChannelNode) {
            if (this.id == ((ChannelNode) obj).getId()) {
                return true;
            }
        }
        return false;
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

    /**
	 * @return the service
	 */
    public Object getService() {
        return service;
    }

    /**
	 * @param service the service to set
	 */
    public void setService(Object service) {
        this.service = service;
    }
}

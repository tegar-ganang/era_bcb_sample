package org.dfdaemon.il2.core.event.console;

import org.dfdaemon.il2.core.event.Event;

/**
 * @author aka50
 */
public class ChannelCreatedEvent extends Event {

    private int channel;

    private String who;

    private String ip;

    private int port;

    public ChannelCreatedEvent(final int channel, final String who, final String ip, final int port) {
        this.channel = channel;
        this.who = who;
        this.ip = ip;
        this.port = port;
    }

    public ChannelCreatedEvent() {
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(final int channel) {
        this.channel = channel;
    }

    public String getWho() {
        return who;
    }

    public void setWho(final String who) {
        this.who = who;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(final String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ChannelCreatedEvent");
        sb.append("{");
        sb.append(super.toString());
        sb.append(", ip='").append(ip).append('\'');
        sb.append(", port='").append(port).append('\'');
        sb.append(", channel=").append(channel);
        sb.append(", who='").append(who).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final ChannelCreatedEvent that = (ChannelCreatedEvent) o;
        if (channel != that.channel) return false;
        if (port != that.port) return false;
        if (ip != null ? !ip.equals(that.ip) : that.ip != null) return false;
        if (who != null ? !who.equals(that.who) : that.who != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + channel;
        result = 31 * result + (who != null ? who.hashCode() : 0);
        result = 31 * result + (ip != null ? ip.hashCode() : 0);
        result = 31 * result + port;
        return result;
    }
}

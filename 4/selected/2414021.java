package org.dfdaemon.il2.core.event.console;

import org.dfdaemon.il2.core.event.Event;

/**
 * @author aka50
 */
public class ChannelLostEvent extends Event {

    private int channel;

    private String ip;

    private int port;

    private String reason;

    public ChannelLostEvent(final int channel, final String reason, final String ip, final int port) {
        this.channel = channel;
        this.ip = ip;
        this.port = port;
        this.reason = reason;
    }

    public ChannelLostEvent() {
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ChannelLostEvent");
        sb.append("{");
        sb.append(super.toString());
        sb.append(", _ip='").append(ip).append('\'');
        sb.append(", _port='").append(port).append('\'');
        sb.append(", _channel='").append(channel).append('\'');
        sb.append(", _reason='").append(reason).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final ChannelLostEvent that = (ChannelLostEvent) o;
        if (channel != that.channel) return false;
        if (port != that.port) return false;
        if (ip != null ? !ip.equals(that.ip) : that.ip != null) return false;
        if (reason != null ? !reason.equals(that.reason) : that.reason != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + channel;
        result = 31 * result + (ip != null ? ip.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        return result;
    }
}

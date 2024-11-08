package org.dfdaemon.il2.core.event.console;

import org.dfdaemon.il2.core.event.Event;

/**
 * @author aka50
 */
public class ChannelStatEvent extends Event {

    private int channel;

    private int ping;

    private int inSpeed;

    private int outSpeed;

    private int unknown1;

    private int unknown2;

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getPing() {
        return ping;
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

    public int getInSpeed() {
        return inSpeed;
    }

    public void setInSpeed(int inSpeed) {
        this.inSpeed = inSpeed;
    }

    public int getOutSpeed() {
        return outSpeed;
    }

    public void setOutSpeed(int outSpeed) {
        this.outSpeed = outSpeed;
    }

    public int getUnknown1() {
        return unknown1;
    }

    public void setUnknown1(int unknown1) {
        this.unknown1 = unknown1;
    }

    public int getUnknown2() {
        return unknown2;
    }

    public void setUnknown2(int unknown2) {
        this.unknown2 = unknown2;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ChannelStatEvent");
        sb.append("{");
        sb.append(super.toString());
        sb.append(", channel=").append(channel);
        sb.append(", ping=").append(ping);
        sb.append(", inSpeed=").append(inSpeed);
        sb.append(", outSpeed=").append(outSpeed);
        sb.append(", unknown1=").append(unknown1);
        sb.append(", unknown2=").append(unknown2);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final ChannelStatEvent that = (ChannelStatEvent) o;
        if (channel != that.channel) return false;
        if (inSpeed != that.inSpeed) return false;
        if (outSpeed != that.outSpeed) return false;
        if (ping != that.ping) return false;
        if (unknown1 != that.unknown1) return false;
        if (unknown2 != that.unknown2) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + channel;
        result = 31 * result + ping;
        result = 31 * result + inSpeed;
        result = 31 * result + outSpeed;
        result = 31 * result + unknown1;
        result = 31 * result + unknown2;
        return result;
    }
}

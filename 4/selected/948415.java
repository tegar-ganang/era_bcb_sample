package be.lassi.lanbox.domain;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import be.lassi.lanbox.ChannelChangeQueue;
import be.lassi.lanbox.LanboxEngine;
import be.lassi.util.Util;

/**
 * A channel change request.  Whenever the application thinks a
 * dmx output channel level should be changed it can create a
 * <code>ChannelChange</code> and put it on the <code>ChannelChangeQueue</code>.
 *
 * @see ChannelChangeQueue
 * @see LanboxEngine
 */
public class ChannelChange {

    /**
     * The dmx channel number.
     */
    private final int channelId;

    /**
     * The new dmx level value.
     *
     */
    private final int dmxValue;

    /**
     * Constructs a new channel change request.
     *
     * @param channelId the dmx channel number
     * @param dmxValue the new dmx level value
     */
    public ChannelChange(final int channelId, final int dmxValue) {
        Util.validate("channelId", channelId, 1, 512);
        Util.validate("dmxValue", dmxValue, 0, 255);
        this.channelId = channelId;
        this.dmxValue = dmxValue;
    }

    /**
     * Gets the dmx channel number.
     *
     * @return the dmx channel number
     */
    public int getChannelId() {
        return channelId;
    }

    /**
     * Gets the new dmx value.
     *
     * @return the new dmx value
     */
    public int getDmxValue() {
        return dmxValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(getClass().getName());
        b.append("(channelId=");
        b.append(channelId);
        b.append(", dmxValue=");
        b.append(dmxValue);
        b.append(")");
        return b.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        boolean result = object == this;
        if (!result && object instanceof ChannelChange) {
            ChannelChange other = (ChannelChange) object;
            EqualsBuilder b = new EqualsBuilder();
            b.append(channelId, other.channelId);
            b.append(dmxValue, other.dmxValue);
            result = b.isEquals();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder b = new HashCodeBuilder(784780763, 1273634681);
        b.append(channelId);
        b.append(dmxValue);
        return b.toHashCode();
    }
}

package be.lassi.lanbox.domain;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import be.lassi.util.Util;

public class ChannelAttribute {

    /**
     * The dmx channel number.
     */
    private final int channelId;

    /**
     * The enabled indicator.
     *
     */
    private final boolean enabled;

    /**
     * Constructs a new channel indicator.
     *
     * @param channelId the dmx channel number
     * @param enabled the enabled indicator
     */
    public ChannelAttribute(final int channelId, final boolean enabled) {
        Util.validate("channelId", channelId, 1, 512);
        this.channelId = channelId;
        this.enabled = enabled;
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
     * Gets the enabled indicator.
     *
     * @return enabled indicator
     */
    public boolean isEnabled() {
        return enabled;
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
        b.append(", enabled=");
        b.append(enabled);
        b.append(")");
        return b.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        boolean result = object == this;
        if (!result && object instanceof ChannelAttribute) {
            ChannelAttribute other = (ChannelAttribute) object;
            EqualsBuilder b = new EqualsBuilder();
            b.append(channelId, other.channelId);
            b.append(enabled, other.enabled);
            result = b.isEquals();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder b = new HashCodeBuilder(-840154173, -1952880697);
        b.append(channelId);
        b.append(enabled);
        return b.toHashCode();
    }
}

package be.lassi.lanbox.domain;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Information about a channel patched to a dimmer.
 *
 */
public class PatchPair {

    private int dimmerId;

    private int channelId;

    /**
     * Constructs a new instance.
     *
     * @param dimmerId the dimmer identifier
     * @param channelId the channel identifier
     */
    public PatchPair(final int dimmerId, final int channelId) {
        this.dimmerId = dimmerId;
        this.channelId = channelId;
    }

    /**
     * Gets the channel identifier.
     *
     * @return the channel identier
     */
    public int getChannelId() {
        return channelId;
    }

    /**
     * Sets the channel identifier.
     *
     * @param channelId the channel identifier to set
     */
    public void setChannelId(final int channelId) {
        this.channelId = channelId;
    }

    /**
     * Gets the dimmer identifier.
     *
     * @return the dimmer identifier
     */
    public int getDimmerId() {
        return dimmerId;
    }

    /**
     * Sets the dimmer identifier.
     *
     * @param dimmerId the dimmer identifier
     */
    public void setDimmerId(final int dimmerId) {
        this.dimmerId = dimmerId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(getClass().getName());
        b.append("(dimmerId=");
        b.append(dimmerId);
        b.append(", channelId=");
        b.append(channelId);
        b.append(")");
        return b.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        boolean result = object == this;
        if (!result && object instanceof PatchPair) {
            PatchPair other = (PatchPair) object;
            EqualsBuilder b = new EqualsBuilder();
            b.append(dimmerId, other.dimmerId);
            b.append(channelId, other.channelId);
            result = b.isEquals();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder b = new HashCodeBuilder(-346586573, -1609721257);
        b.append(dimmerId);
        b.append(channelId);
        return b.toHashCode();
    }
}

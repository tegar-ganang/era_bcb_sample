package be.lassi.lanbox.domain;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class ChannelStatus {

    /**
     * The dmx channel number.
     */
    private final int channelId;

    /**
     * Indicates whether the data of this channel is fed to the mixer.
     */
    private final boolean output;

    /**
     * Indicates whether this channel is enabled for storing into
     * a cue step scene.
     */
    private final boolean edit;

    private final boolean solo;

    /**
     * Indicates whether a fade is currently going on in the channel.
     */
    private final boolean fadeActive;

    public ChannelStatus(final int channelId, final boolean output, final boolean enabled, final boolean solo, final boolean fader) {
        this.channelId = channelId;
        this.output = output;
        this.edit = enabled;
        this.solo = solo;
        this.fadeActive = fader;
    }

    public int getChannelId() {
        return channelId;
    }

    public boolean isOutput() {
        return output;
    }

    public boolean isEdit() {
        return edit;
    }

    public boolean isSolo() {
        return solo;
    }

    /**
     * Indicates whether a fade is currently active in the channel.
     *
     * @return true if fade is currently active
     */
    public boolean isFadeActive() {
        return fadeActive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        boolean result = object instanceof ChannelStatus;
        if (result) {
            ChannelStatus other = (ChannelStatus) object;
            EqualsBuilder b = new EqualsBuilder();
            b.append(channelId, other.channelId);
            b.append(output, other.output);
            b.append(edit, other.edit);
            b.append(solo, other.solo);
            b.append(fadeActive, other.fadeActive);
            result = b.isEquals();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder b = new HashCodeBuilder(493577855, -1081729093);
        b.append(channelId);
        return b.toHashCode();
    }
}

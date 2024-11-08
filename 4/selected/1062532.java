package be.lassi.domain;

import org.apache.commons.lang.builder.HashCodeBuilder;
import be.lassi.base.Dirty;
import be.lassi.base.DirtyStub;
import be.lassi.util.equals.EqualsTester;

/**
 * Represents a physical dimmer channel.  A physical dimmer channel
 * can be assigned to a logical channel. One dimmer channel can be
 * patched to one logical channel only, while one logical channel
 * could be patched to multiple physical dimmer channels.  This is
 * why we have the patching information here.
 *
 */
public class Dimmer extends LevelObject {

    /**
     * The channel that this dimmer is patched, a value of <code>null</code>
     * indicates that this dimmer is currently not patched to a channel.
     */
    private Channel channel;

    private int lanboxChannelId = -1;

    /**
     * Constructs a new instance with given id and name.
     *
     * @param id the of the dimmer in the show dimmer collection
     * @param name the name of the dimmer
     */
    public Dimmer(final int id, final String name) {
        this(new DirtyStub(), id, name);
    }

    /**
     * Constructs a new instance with given id and name.
     *
     * @param dirty the dirty indicator
     * @param id the of the dimmer in the show dimmer collection
     * @param name the name of the dimmer
     */
    public Dimmer(final Dirty dirty, final int id, final String name) {
        super(dirty, id, name);
    }

    /**
     * Gets the channel that this dimmer is patched to, <code>null</code> if
     * no channel is patched to this dimmer.
     *
     * @return the channel this dimmer is patched to, null if not patched
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Sets the channel that this dimmer is patched to, null if no channel
     * is patched to this dimmer.
     *
     * @param channel the channel this dimmer is patched to, null if not patched
     */
    public void setChannel(final Channel channel) {
        if (this.channel != null) {
            this.channel.remove(this);
        }
        if (channel != null) {
            channel.add(this);
        }
        this.channel = channel;
        markDirty();
    }

    /**
     * Gets the identifier of the channel that this dimmer is patched to.
     *
     * @return the identifier of the logical channel that this dimmer
     *    is patched to.  A value of -1 indicates that no channel
     *    is patched to this dimmer
     */
    public int getChannelId() {
        int channelIndex = -1;
        if (channel != null) {
            channelIndex = channel.getId();
        }
        return channelIndex;
    }

    /**
     * Indicates whether this dimmer is patched to a channel.
     *
     * @return true if this dimmer is patched to a channel
     */
    public boolean isPatched() {
        return channel != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        boolean result = this == object;
        if (!result) {
            EqualsTester tester = EqualsTester.get(this, object);
            if (tester.isEquals()) {
                tester.test(super.equals(object));
                final Dimmer other = (Dimmer) object;
                if (channel == null) {
                    tester.test(other.channel == null);
                } else {
                    if (other.channel == null) {
                        tester.test(false);
                    } else {
                        tester.test(channel.getId() == other.channel.getId());
                    }
                }
            }
            result = tester.isEquals();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder b = new HashCodeBuilder(672542163, -2090454173);
        b.append(super.hashCode());
        b.append(channel);
        return b.toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(getClass().getName());
        b.append('(');
        b.append(getName());
        b.append(')');
        String string = b.toString();
        return string;
    }

    public int getLanboxChannelId() {
        return lanboxChannelId;
    }

    public void setLanboxChannelId(final int lanboxChannelId) {
        this.lanboxChannelId = lanboxChannelId;
        doNotMarkDirty();
    }

    public static String getDefaultName(final int id) {
        return "Dimmer " + (id + 1);
    }
}

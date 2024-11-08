package be.lassi.domain;

import org.apache.commons.lang.builder.HashCodeBuilder;
import be.lassi.base.Dirty;
import be.lassi.base.DirtyStub;
import be.lassi.base.LList;
import be.lassi.base.Listeners;
import be.lassi.base.SaveableObject;
import be.lassi.ui.util.Move;
import be.lassi.ui.util.SelectionInterval;
import be.lassi.util.equals.EqualsTester;

/**
 * A collection with a subset of all available channels that can be controlled
 * through the lanbox.  These groups can be used to limit the number of channels
 * that are shown on the sheet and patch definition windows.
 */
public class Group extends SaveableObject {

    /**
     * Indicates whether the channels in this group should be shown.
     */
    private boolean enabled = false;

    /**
     * The group name.
     */
    private String name;

    /**
     * A text comment that can be used by the user to explain
     * what this group is all about.
     */
    private String comment;

    /**
     * Channel collection; note that the order within this collection
     * is the order in which the channels are to be used within the
     * group (in display, etc.).
     */
    private final LList<Channel> channels;

    /**
     * The listeners that observe changes in the channel collection.
     */
    private final Listeners listeners = new Listeners();

    /**
     * Constructs a new group.
     */
    public Group() {
        this(new DirtyStub(), "");
    }

    /**
     * Constructs a new group.
     *
     * @param dirty the dirty indicator
     */
    public Group(final Dirty dirty) {
        this(dirty, "");
    }

    /**
     * Constructs a new group with given name.
     *
     * @param name the group name
     */
    public Group(final String name) {
        this(new DirtyStub(), name);
    }

    /**
     * Constructs a new group with given name.
     *
     * @param dirty the dirty indicator
     * @param name the group name
     */
    public Group(final Dirty dirty, final String name) {
        super(dirty);
        this.name = name;
        channels = new LList<Channel>(dirty);
    }

    /**
     * Sets the group 'enabled' switch.
     *
     * @param enabled true if this group is enabled
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        doNotMarkDirty();
        listeners.changed();
    }

    /**
     * Indicates whether this group is enabled.
     *
     * @return true if this group is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Adds given channel.
     *
     * @param channel the channel to be added
     */
    public void add(final Channel channel) {
        channels.add(channel);
        markDirty();
        listeners.changed();
    }

    /**
     * Removes the channel at given index.
     *
     * @param index the index of the channel to be removed
     */
    public void remove(final int index) {
        channels.remove(index);
        markDirty();
        listeners.changed();
    }

    /**
     * Gets the channels in this group.
     *
     * @return the channels in this group
     */
    public Channel[] getChannels() {
        return channels.toArray(new Channel[channels.size()]);
    }

    /**
     * Gets the group name.
     *
     * @return the group name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the group name.
     *
     * @param name the group name
     */
    public void setName(final String name) {
        this.name = name;
        markDirty();
        listeners.changed();
    }

    /**
     * Gets the channel at given index.
     *
     * @param index the index in the channel collection (not the channel id)
     * @return the channel at given index
     */
    public Channel get(final int index) {
        return channels.get(index);
    }

    /**
     * Gets the number of channels in this group.
     *
     * @return the number of channels in this group
     */
    public int size() {
        return channels.size();
    }

    /**
     * Indicates whether this channel group includes the channel with
     * given id.
     *
     * @param channelId the identifier of the channel to be tested
     * @return true if this group contains given channel
     */
    public boolean includes(final int channelId) {
        boolean result = false;
        for (int i = 0; !result && i < channels.size(); i++) {
            Channel channel = channels.get(i);
            result = channelId == channel.getId();
        }
        return result;
    }

    public boolean includes(final Channel channel) {
        boolean result = false;
        for (int i = 0; !result && i < channels.size(); i++) {
            result = channel == channels.get(i);
        }
        return result;
    }

    /**
     * Moves the channels at given indexes up in the channel collection.
     *
     * @param indexes the indexes of the channels to be moved up
     * @return the new indexes of the channels that have been moved up
     */
    public SelectionInterval moveUp(final int[] indexes) {
        SelectionInterval selection = new Move<Channel>().up(channels, indexes);
        markDirty();
        listeners.changed();
        return selection;
    }

    /**
     * Moves the channels at given indexes down in the channel collection.
     *
     * @param indexes the indexes of the channels to be moved down
     * @return the new indexes of the channels that have been moved down
     */
    public SelectionInterval moveDown(final int[] indexes) {
        SelectionInterval selection = new Move<Channel>().down(channels, indexes);
        markDirty();
        listeners.changed();
        return selection;
    }

    /**
     * Gets the listeners that observe changes in the channel collection.
     *
     * @return the listeners
     */
    public Listeners getListeners() {
        return listeners;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(getClass().getName());
        b.append("(");
        b.append(name);
        b.append(")");
        String string = b.toString();
        return string;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "BC" })
    public boolean equals(final Object object) {
        boolean result = this == object;
        if (!result) {
            EqualsTester tester = EqualsTester.get(this, object);
            if (tester.isEquals()) {
                Group other = (Group) object;
                tester.test(enabled, other.enabled);
                tester.test(name, other.name);
                tester.test(channels, other.channels);
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
        HashCodeBuilder b = new HashCodeBuilder(80631793, 628915191);
        b.append(enabled);
        b.append(name);
        b.append(channels);
        return b.toHashCode();
    }

    /**
     * Gets the group comment text.
     *
     * @return the comment text
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the group comment text.
     *
     * @param comment the comment text
     */
    public void setComment(final String comment) {
        this.comment = comment;
    }
}

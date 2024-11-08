package be.lassi.domain;

import java.util.Iterator;
import org.apache.commons.lang.builder.HashCodeBuilder;
import be.lassi.base.Dirty;
import be.lassi.base.DirtyStub;
import be.lassi.base.LList;
import be.lassi.base.LListListener;
import be.lassi.base.Listeners;
import be.lassi.base.SaveableObject;
import be.lassi.ui.util.Move;
import be.lassi.ui.util.SelectionInterval;
import be.lassi.util.equals.EqualsTester;

/**
 * Collection of channel groups.
 *
 */
public class Groups extends SaveableObject implements Iterable<Group> {

    /**
     * The actual collection of channel groups that is wrapped by this class.
     */
    private final LList<Group> groups;

    /**
     * The listeners interested in changes to the groups collection AND also
     * in any changes in any of the groups in the collection.
     */
    private final Listeners listeners = new Listeners();

    /**
     * Constructs a new instance.
     */
    public Groups() {
        this(new DirtyStub());
    }

    /**
     * Constructs a new instance.
     *
     * @param dirty the dirty indicator
     */
    public Groups(final Dirty dirty) {
        super(dirty);
        groups = new LList<Group>(dirty);
    }

    /**
     * Adds a new channel group.
     *
     * @param group the channel group to be added
     */
    public void add(final Group group) {
        groups.add(group);
        markDirty();
        listeners.changed();
    }

    public void addGroupsListener(final LListListener<Group> listener) {
        groups.addListener(listener);
    }

    public void removeGroupsListener(final LListListener<Group> listener) {
        groups.removeListener(listener);
    }

    /**
     * Removes the channel group at given index.
     *
     * @param index the index of the channel group to be removed
     */
    public void remove(final int index) {
        groups.remove(index);
        markDirty();
        listeners.changed();
    }

    /**
     * Gets the number of channel groups.
     *
     * @return the number of channel groups
     */
    public int size() {
        return groups.size();
    }

    /**
     * Gets the channel group at given index.
     *
     * @param index the index in the channel group collection
     * @return the channel group at given index
     */
    public Group get(final int index) {
        return groups.get(index);
    }

    /**
     * Move the groups at given indexes in the collection up.
     *
     * @param indexes the selected channel group indexes
     * @return the indexes of the channel groups after the move
     */
    public SelectionInterval moveUp(final int[] indexes) {
        SelectionInterval selection = new Move<Group>().up(groups, indexes);
        markDirty();
        listeners.changed();
        return selection;
    }

    /**
     * Move the groups at given indexes in the collection down.
     *
     * @param indexes the selected channel group indexes
     * @return the indexes of the channel groups after the move
     */
    public SelectionInterval moveDown(final int[] indexes) {
        SelectionInterval selection = new Move<Group>().down(groups, indexes);
        markDirty();
        listeners.changed();
        return selection;
    }

    /**
     * Gets the number of channel groups that are currently enabled.
     *
     * @return the number of enabled channel groups
     */
    public int getEnabledGroupCount() {
        int count = 0;
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).isEnabled()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Indicates whether at least one of the channel groups is currently enabled.
     *
     * @return true if at least one of the channel groups is enabled
     */
    public boolean isEnabled() {
        boolean enabled = false;
        for (int i = 0; !enabled && i < groups.size(); i++) {
            enabled = groups.get(i).isEnabled();
        }
        return enabled;
    }

    /**
     * Gets the channel with given index, within the channels of the currently
     * enabled groups.
     *
     * @param index
     * @return
     */
    public Channel getChannel(final int index) {
        Channel channel = null;
        int channelOffset = 0;
        for (int i = 0; channel == null && i < groups.size(); i++) {
            Group group = groups.get(i);
            if (group.isEnabled()) {
                if (index < channelOffset + group.size()) {
                    int channelIndex = index - channelOffset;
                    channel = group.get(channelIndex);
                } else {
                    channelOffset += group.size();
                }
            }
        }
        if (channel == null) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + channelOffset);
        }
        return channel;
    }

    public int getIndexOfChannelWithId(final int id) {
        int result = -1;
        if (isEnabled()) {
            for (int i = 0, index = 0; result == -1 && i < groups.size(); i++) {
                Group group = groups.get(i);
                if (group.isEnabled()) {
                    for (int j = 0; result == -1 && j < group.size(); j++) {
                        if (id == group.get(j).getId()) {
                            result = index;
                        }
                        index++;
                    }
                }
            }
        } else {
            for (int i = 0, index = 0; result == -1 && i < groups.size(); i++) {
                Group group = groups.get(i);
                for (int j = 0; result == -1 && j < group.size(); j++) {
                    if (id == group.get(j).getId()) {
                        result = index;
                    }
                    index++;
                }
            }
        }
        return result;
    }

    /**
     * Sets the 'enabled' switch to given value for all channel groups.
     *
     * @param enabled indicates whether all channels groups should be enabled or not
     */
    public void setAllEnabled(final boolean enabled) {
        for (int i = 0; i < groups.size(); i++) {
            groups.get(i).setEnabled(enabled);
        }
        doNotMarkDirty();
        listeners.changed();
    }

    /**
     * Gets the listeners that are interested in group changes.
     *
     * @return the listeners
     */
    public Listeners getListeners() {
        return listeners;
    }

    /**
     * Gets the overall number of channels all of the groups.  Note that
     * even when a channel can be included in more than one group, it is
     * counted only once.
     *
     * @param totalChannelCount the total number of channels in the show
     * @return the number of channels in the groups
     */
    public int getChannelCount(final int totalChannelCount) {
        int channelCount = 0;
        for (int i = 0; i < totalChannelCount; i++) {
            if (includes(i)) {
                channelCount++;
            }
        }
        return channelCount;
    }

    /**
     * Gets the number of channels in all currently enabled groups.
     * Note that when a channel is included in more than one group, it is
     * counted multiple times.
     *
     * @return the number of channels in the groups
     */
    public int getEnabledGroupsChannelCount() {
        int channelCount = 0;
        for (Group group : groups) {
            if (group.isEnabled()) {
                channelCount += group.size();
            }
        }
        return channelCount;
    }

    /**
     * Indicates whether any of the groups includes the channel
     * with given id.
     *
     * @param channelId the channel identifier
     * @return true if one of the groups contains channel with given id
     */
    private boolean includes(final int channelId) {
        boolean includes = false;
        for (int i = 0; !includes && i < groups.size(); i++) {
            includes = groups.get(i).includes(channelId);
        }
        return includes;
    }

    /**
     * Gets the indexes of the channels that are not included in the currently selected group.
     *
     * @param totalChannelCount the total number of channels in the show
     * @return the indexes of the channels that are not included in any group
     */
    public int[] getChannelsNotInGroup(final boolean any, final int groupIndex, final int totalChannelCount) {
        int channelCount = getChannelsNotInGroupCount(any, groupIndex, totalChannelCount);
        int[] indexes = new int[channelCount];
        if (any) {
            for (int i = 0, j = 0; i < totalChannelCount; i++) {
                if (!includes(i)) {
                    indexes[j++] = i;
                }
            }
        } else {
            if (groupIndex >= 0) {
                Group group = groups.get(groupIndex);
                for (int i = 0, j = 0; i < totalChannelCount; i++) {
                    if (!group.includes(i)) {
                        indexes[j++] = i;
                    }
                }
            } else {
                for (int i = 0; i < totalChannelCount; i++) {
                    indexes[i] = i;
                }
            }
        }
        return indexes;
    }

    public boolean includes(final Channel channel) {
        boolean result = false;
        for (int i = 0; !result && i < groups.size(); i++) {
            result = groups.get(i).includes(channel);
        }
        return result;
    }

    public int getChannelsNotInGroupCount(final boolean any, final int groupIndex, final int totalChannelCount) {
        int channelCount = totalChannelCount;
        if (any) {
            channelCount -= getChannelCount(totalChannelCount);
        } else {
            if (groupIndex >= 0) {
                channelCount -= groups.get(groupIndex).size();
            }
        }
        return channelCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(getClass().getName());
        b.append("(");
        for (int i = 0; i < groups.size(); i++) {
            b.append("\"");
            b.append(groups.get(i).getName());
            b.append("\"");
            if (i < groups.size() - 1) {
                b.append(",");
            }
        }
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
                Groups other = (Groups) object;
                tester.test(groups, other.groups);
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
        HashCodeBuilder b = new HashCodeBuilder(1020042345, 1489862987);
        b.append(groups);
        return b.toHashCode();
    }

    /**
     * Set enabled indicator for group with given index.
     *
     * @param index group index
     * @param enabled true if group is enabled
     */
    public void setEnabled(final int index, final boolean enabled) {
        get(index).setEnabled(enabled);
        doNotMarkDirty();
        listeners.changed();
    }

    /**
     * Sets the name of group with given index.
     *
     * @param index group index
     * @param name group name
     */
    public void setName(final int index, final String name) {
        get(index).setName(name);
        listeners.changed();
    }

    /**
     * Indicates whether group with given index is enabled.
     *
     * @param index group index
     * @return true if given group is enabled
     */
    public boolean isEnabled(final int index) {
        return get(index).isEnabled();
    }

    public Iterator<Group> iterator() {
        return groups.iterator();
    }
}

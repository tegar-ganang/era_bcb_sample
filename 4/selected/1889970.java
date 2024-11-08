package be.lassi.lanbox.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Collection of <code>ChannelChange</code> objects.
 */
public class ChannelChanges implements Iterable<ChannelChange> {

    private final List<ChannelChange> changes;

    /**
     * Constructs a new instance.
     *
     * @param changes initial channel changes
     */
    public ChannelChanges(final List<ChannelChange> changes) {
        this.changes = changes;
    }

    /**
     * Constructs a new instance.
     */
    public ChannelChanges() {
        this(new ArrayList<ChannelChange>());
    }

    /**
     * Adds channel change.
     *
     * @param change the channel change to be added
     */
    public void add(final ChannelChange change) {
        changes.add(change);
    }

    /**
     * Replaces the channel changes in this collection with the
     * channel changes in given collection.
     *
     * @param changes the new channel changes
     */
    public void set(final ChannelChanges changes) {
        this.changes.clear();
        this.changes.addAll(changes.changes);
    }

    /**
     * Constructs and adds a new channel change.
     *
     * @param channelId the channel identifier
     * @param dmxValue the dmx value
     */
    public void add(final int channelId, final int dmxValue) {
        ChannelChange change = new ChannelChange(channelId, dmxValue);
        add(change);
    }

    public ChannelChanges[] split(final int max) {
        int count = ((changes.size() - 1) / max) + 1;
        if (changes.size() == 0) {
            count = 0;
        }
        ChannelChanges[] result = new ChannelChanges[count];
        if (count == 1) {
            result[0] = this;
        } else {
            for (int i = 0; i < result.length; i++) {
                int fromIndex = i * max;
                int toIndex = (i * max) + max;
                if (toIndex > changes.size()) {
                    toIndex = changes.size();
                }
                List<ChannelChange> list = changes.subList(fromIndex, toIndex);
                result[i] = new ChannelChanges(list);
            }
        }
        return result;
    }

    public ChannelChange[] toArray() {
        return changes.toArray(new ChannelChange[changes.size()]);
    }

    /**
     * Gets the number of channel changes in this collection.
     *
     * @return the change count
     */
    public int size() {
        return changes.size();
    }

    /**
     * Reduces the number of channel changes to 1 per channel (only the
     * last channel change is kept if there are more than 1 for a given
     * channel).
     */
    public void eliminateDoubles() {
        int doubles = 0;
        for (int i = 0; i < changes.size() - 1; i++) {
            int channelId = changes.get(i).getChannelId();
            for (int j = i + 1; j < changes.size(); j++) {
                if (channelId == changes.get(j).getChannelId()) {
                    changes.set(i, null);
                    doubles++;
                    break;
                }
            }
        }
        if (doubles > 0) {
            Iterator<ChannelChange> i = changes.iterator();
            while (i.hasNext()) {
                if (i.next() == null) {
                    i.remove();
                }
            }
        }
    }

    public Iterator<ChannelChange> iterator() {
        return changes.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        boolean result = object == this;
        if (!result && object instanceof ChannelChanges) {
            ChannelChanges other = (ChannelChanges) object;
            EqualsBuilder b = new EqualsBuilder();
            b.append(changes, other.changes);
            result = b.isEquals();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder b = new HashCodeBuilder(-17661625, 286669317);
        return b.toHashCode();
    }

    public String getString() {
        StringBuilder b = new StringBuilder();
        appendSingleLine(b);
        String string = b.toString();
        return string;
    }

    private void appendSingleLine(final StringBuilder b) {
        for (ChannelChange change : this) {
            b.append(change.getChannelId());
            b.append("[");
            b.append(change.getDmxValue());
            b.append("] ");
        }
    }

    public void append(final StringBuilder b) {
        for (int i = 0; i < size(); i++) {
            ChannelChange change = get(i);
            if ((i % 10) == 0) {
                b.append("\n     ");
            }
            b.append(" ");
            b.append(change.getChannelId());
            b.append("[");
            b.append(change.getDmxValue());
            b.append("]");
        }
        b.append('\n');
    }

    public ChannelChange get(final int index) {
        return changes.get(index);
    }

    public void sort() {
        Collections.sort(changes, new Comparator<ChannelChange>() {

            public int compare(final ChannelChange cc1, final ChannelChange cc2) {
                return cc1.getChannelId() - cc2.getChannelId();
            }
        });
    }
}

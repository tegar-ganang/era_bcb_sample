package be.lassi.lanbox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class ChannelChanges implements Iterable<ChannelChange> {

    private List<ChannelChange> changes;

    public ChannelChanges(final List<ChannelChange> changes) {
        this.changes = changes;
    }

    public ChannelChanges() {
        this(new ArrayList<ChannelChange>());
    }

    public void add(final ChannelChange change) {
        changes.add(change);
    }

    public void add(final ChannelChanges channelChanges) {
        this.changes.addAll(channelChanges.changes);
    }

    public void set(final ChannelChanges changes) {
        this.changes.clear();
        this.changes.addAll(changes.changes);
    }

    public void add(final int channelId, final int dmxValue) {
        ChannelChange change = new ChannelChange(channelId, dmxValue);
        add(change);
    }

    public ChannelChange get(int index) {
        return changes.get(index);
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
    public int hashCode() {
        HashCodeBuilder b = new HashCodeBuilder(538163346, 655761780);
        return b.toHashCode();
    }

    public String getString() {
        StringBuilder b = new StringBuilder();
        append(b);
        String string = b.toString();
        return string;
    }

    public void append(StringBuilder b) {
        for (ChannelChange change : this) {
            b.append(change.getChannelId());
            b.append("[");
            b.append(change.getDmxValue());
            b.append("] ");
        }
    }
}

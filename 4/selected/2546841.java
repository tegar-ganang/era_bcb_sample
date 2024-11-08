package be.lassi.lanbox.commands.channel;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import be.lassi.lanbox.commands.LanboxCommand;
import be.lassi.lanbox.domain.ChannelChange;
import be.lassi.lanbox.domain.ChannelChanges;
import be.lassi.util.Util;

/**
 * Lanbox command to set channel dmx values.
 */
public class ChannelSetData extends LanboxCommand {

    /**
     * Lanbox command identifier.
     */
    public static final String ID = "C9";

    /**
     * The maximum number of channel changes that can be processed in one
     * command.  Experimentation learns that more than 255 changes are possible
     * in one command, but we limit ourselves to 255 to be on the safe side.
     */
    public static final int MAX_CHANGES = 255;

    private final int bufferId;

    private final ChannelChanges changes;

    public ChannelSetData(final byte[] request) {
        super(request);
        bufferId = getInt(3);
        int changesCount = (request.length - 5) / 6;
        changes = new ChannelChanges();
        for (int i = 0; i < changesCount; i++) {
            int channelId = getInt4(5 + (i * 6));
            int dmxValue = getInt(5 + (i * 6) + 4);
            ChannelChange change = new ChannelChange(channelId, dmxValue);
            changes.add(change);
        }
    }

    /**
     * Constructs the command.
     *
     * @param bufferId the buffer identifier
     * @param changes the channel changes
     */
    public ChannelSetData(final int bufferId, final ChannelChanges changes) {
        super(ID, "*C9ee#".length() + (6 * changes.size()));
        this.bufferId = bufferId;
        this.changes = changes;
        Util.validate("changesCount", changes.size(), 1, MAX_CHANGES);
        set2(3, bufferId);
        for (int i = 0; i < changes.size(); i++) {
            ChannelChange change = changes.get(i);
            set4(5 + (i * 6), change.getChannelId());
            set2(5 + (i * 6) + 4, change.getDmxValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendCommand(final StringBuilder b) {
        b.append("ChannelSetData(bufferId=");
        b.append(bufferId);
        b.append(", channelCount=");
        b.append(changes.size());
        b.append(")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendCommandDetail(final StringBuilder b) {
        changes.append(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        boolean result = object instanceof ChannelSetData;
        if (result) {
            ChannelSetData other = (ChannelSetData) object;
            EqualsBuilder b = new EqualsBuilder();
            b.appendSuper(super.equals(object));
            b.append(bufferId, other.bufferId);
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
        HashCodeBuilder b = new HashCodeBuilder(-1708064471, -760544765);
        b.appendSuper(super.hashCode());
        b.append(bufferId);
        b.append(changes);
        return b.toHashCode();
    }
}

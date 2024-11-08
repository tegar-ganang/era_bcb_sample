package be.lassi.lanbox.commands;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import be.lassi.lanbox.ChannelChange;
import be.lassi.util.Util;

/**
 * Lanbox command to set channel dmx values.  
 */
public class ChannelSetData extends Command {

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

    private int bufferId;

    private ChannelChange[] changes;

    /**
     * Constructs command from request buffer.
     * 
     * @param request the lanbox request buffer
     */
    public ChannelSetData(final byte[] request) {
        super(request);
        bufferId = getInt(3);
        int changesCount = (request.length - 5) / 6;
        changes = new ChannelChange[changesCount];
        for (int i = 0; i < changesCount; i++) {
            int channelId = getInt4(5 + (i * 6));
            int dmxValue = getInt(5 + (i * 6) + 4);
            changes[i] = new ChannelChange(channelId, dmxValue);
        }
    }

    /**
     * Constructs the command.
     * 
     * @param bufferId the buffer identifier
     * @param changes the channel changes
     */
    public ChannelSetData(final int bufferId, final ChannelChange[] changes) {
        super(ID, "*C9ee#".length() + (6 * changes.length));
        this.bufferId = bufferId;
        this.changes = changes;
        Util.validate("changesCount", changes.length, 1, MAX_CHANGES);
        set2(3, bufferId);
        for (int i = 0; i < changes.length; i++) {
            set4(5 + (i * 6), changes[i].getChannelId());
            set2(5 + (i * 6) + 4, changes[i].getDmxValue());
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
        b.append(changes.length);
        b.append(")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendCommandDetail(final StringBuilder b) {
        for (int i = 0; i < changes.length; i++) {
            b.append(" ");
            b.append(changes[i].getChannelId());
            b.append("[");
            b.append(changes[i].getDmxValue());
            b.append("]");
        }
        b.append('\n');
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object object) {
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

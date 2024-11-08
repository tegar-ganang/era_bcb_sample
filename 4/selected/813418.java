package be.lassi.lanbox.commands.channel;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import be.lassi.lanbox.commands.LanboxCommand;
import be.lassi.lanbox.domain.ChannelStatus;
import be.lassi.util.Hex;

/**
 * Lanbox command to read the status of channels.
 */
public class ChannelReadStatus extends LanboxCommand {

    /**
     * Lanbox command identifier.
     */
    public static final String ID = "CE";

    private final int first;

    private final int bufferId;

    private final int channelCount;

    private ChannelStatus[] statusses = new ChannelStatus[0];

    /**
     * Constructs a new instance.
     *
     * @param bufferId the buffer identifier
     * @param first index of the first channel for which to retrieve the status
     * @param channelCount the number of channels for which to retrieve the status
     */
    public ChannelReadStatus(final int bufferId, final int first, final int channelCount) {
        super(ID, "*CEeeffffnn#".length());
        this.bufferId = bufferId;
        this.first = first;
        this.channelCount = channelCount;
        set2(3, bufferId);
        set4(5, first);
        set2(9, channelCount);
    }

    /**
     * Constructs command from request buffer.
     *
     * @param request the lanbox request buffer
     */
    public ChannelReadStatus(final byte[] request) {
        super(request);
        bufferId = getInt(3);
        first = getInt4(5);
        channelCount = getInt(9);
    }

    public ChannelStatus[] getStatusses() {
        return statusses;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processResponse(final byte[] bytes) {
        super.processResponse(bytes);
        int channelId = first;
        statusses = new ChannelStatus[channelCount];
        for (int i = 0; i < channelCount; i++) {
            int status = Hex.getInt(bytes, i + i + 1);
            boolean output = (status & 0x01) != 0;
            boolean enabled = (status & 0x02) != 0;
            boolean solo = (status & 0x04) != 0;
            boolean fader = (status & 0x08) != 0;
            statusses[i] = new ChannelStatus(channelId, output, enabled, solo, fader);
            channelId++;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendCommand(final StringBuilder b) {
        b.append("ChannelReadStatus(bufferId=");
        b.append(bufferId);
        b.append(", first=");
        b.append(first);
        b.append(", channelCount=");
        b.append(channelCount);
        b.append(")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendResponse(final StringBuilder b) {
        for (ChannelStatus status : statusses) {
            b.append("\n  channel=");
            b.append(status.getChannelId());
            b.append(", output=");
            b.append(status.isOutput());
            b.append(", edit=");
            b.append(status.isEdit());
            b.append(", solo=");
            b.append(status.isSolo());
            b.append(", fadeActive=");
            b.append(status.isFadeActive());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        boolean result = object instanceof ChannelReadStatus;
        if (result) {
            ChannelReadStatus other = (ChannelReadStatus) object;
            EqualsBuilder b = new EqualsBuilder();
            b.appendSuper(super.equals(object));
            b.append(bufferId, other.bufferId);
            b.append(channelCount, other.channelCount);
            b.append(first, other.first);
            result = b.isEquals();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder b = new HashCodeBuilder(1050459003, -254018995);
        b.appendSuper(super.hashCode());
        b.append(bufferId);
        b.append(channelCount);
        b.append(first);
        return b.toHashCode();
    }
}

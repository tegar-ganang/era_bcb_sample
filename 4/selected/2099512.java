package be.lassi.lanbox.commands.channel;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import be.lassi.lanbox.commands.LanboxCommand;
import be.lassi.lanbox.domain.ChannelAttribute;
import be.lassi.util.Util;

/**
 * Superclass for Lanbox commands that change channel settings.
 */
public abstract class ChannelSetAttribute extends LanboxCommand {

    /**
     * The maximum number of channel changes that can be processed in one
     * command.  Experimentation learns that more than 255 changes are possible
     * in one command, but we limit ourselves to 255 to be on the safe side.
     */
    public static final int MAX_ATTRIBUTES = 255;

    private final int bufferId;

    private final ChannelAttribute[] attributes;

    /**
     * Constructs a new command.
     */
    protected ChannelSetAttribute(final String id, final int bufferId, final ChannelAttribute[] attributes) {
        super(id, "*XXee#".length() + (6 * attributes.length));
        this.bufferId = bufferId;
        this.attributes = attributes;
        Util.validate("attributesCount", attributes.length, 1, MAX_ATTRIBUTES);
        set2(3, bufferId);
        int offset = 5;
        for (ChannelAttribute attribute : attributes) {
            int channelId = attribute.getChannelId();
            int value = attribute.isEnabled() ? 0xFF : 0;
            set4(offset, channelId);
            set2(offset + 4, value);
            offset += 6;
        }
    }

    /**
     * Constructs a new command from a request buffer.
     *
     * @param request the buffer to construct the command from
     */
    protected ChannelSetAttribute(final byte[] request) {
        super(request);
        bufferId = getInt(3);
        int attributeCount = (request.length - 5) / 6;
        attributes = new ChannelAttribute[attributeCount];
        int offset = 5;
        for (int i = 0; i < attributeCount; i++) {
            int channelId = getInt4(offset);
            boolean enabled = getInt(offset + 4) != 0;
            attributes[i] = new ChannelAttribute(channelId, enabled);
            offset += 6;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendCommand(final StringBuilder b) {
        String name = Util.nameOf(getClass());
        b.append(name);
        b.append("(bufferId=");
        b.append(bufferId);
        b.append("\n  ");
        for (ChannelAttribute indicator : attributes) {
            b.append(indicator.getChannelId());
            b.append("[");
            b.append(indicator.isEnabled());
            b.append("] ");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        boolean result = object instanceof ChannelSetAttribute;
        if (result) {
            ChannelSetAttribute other = (ChannelSetAttribute) object;
            EqualsBuilder b = new EqualsBuilder();
            b.appendSuper(super.equals(object));
            b.append(bufferId, other.bufferId);
            b.append(attributes, other.attributes);
            result = b.isEquals();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder b = new HashCodeBuilder(648584597, -502703281);
        b.appendSuper(super.hashCode());
        b.append(bufferId);
        b.append(attributes);
        return b.toHashCode();
    }
}

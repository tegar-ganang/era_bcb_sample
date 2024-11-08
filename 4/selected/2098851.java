package be.lassi.lanbox.commands;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import be.lassi.lanbox.domain.PatchParameters;
import be.lassi.util.Hex;
import be.lassi.util.Util;

/**
 * Sets the lanbox patching information defining the mapping of dimmers
 * (dmx channels) to light channels.  A maximum of 255 dimmer/channel
 * pairs can be send to the Lanbox.  Note that this class supports 16bit
 * mode only.  Each pair consists of 8 bytes (4 for the dimmer number, and
 * 4 for the channel number).
 */
public class CommonSetPatch extends LanboxCommand {

    /**
     * Lanbox command identifier.
     */
    public static final String ID = "81";

    /**
     * The patch parameters (dimmer/channel pairs).
     */
    private final PatchParameters parameters;

    /**
     * Constructs a new command from raw request data.
     *
     * @param request the raw request data
     */
    public CommonSetPatch(final byte[] request) {
        super(request);
        parameters = new PatchParameters();
        int dimmerCount = (request.length - 4) / 8;
        int offset = 3;
        for (int i = 0; i < dimmerCount; i++) {
            int dimmerId = Hex.getInt4(request, offset) - 1;
            offset += 4;
            int channelId = Hex.getInt4(request, offset) - 1;
            offset += 4;
            parameters.add(dimmerId, channelId);
        }
    }

    /**
     * Constructs a new instance.
     *
     * @param parameters the patch parameters (dimmer/channel pairs)
     */
    public CommonSetPatch(final PatchParameters parameters) {
        super(ID, "*81#".length() + (parameters.size() * 8));
        this.parameters = parameters;
        Util.validate("parameters.size()", parameters.size(), 1, 255);
        int offset = 3;
        for (int i = 0; i < parameters.size(); i++) {
            set4(offset, parameters.getDimmerId(i) + 1);
            offset += 4;
            set4(offset, parameters.getChannelId(i) + 1);
            offset += 4;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendCommand(final StringBuilder b) {
        b.append("CommonSetPatch(dimmerCount=");
        b.append(parameters.size());
        b.append(")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendCommandDetail(final StringBuilder b) {
        for (int i = 0; i < parameters.size(); i++) {
            b.append(" ");
            b.append(parameters.getDimmerId(i) + 1);
            b.append("[");
            b.append(parameters.getChannelId(i) + 1);
            b.append("]");
        }
        b.append('\n');
    }

    /**
     * Gets the patch parameters (dimmer/channel pairs).
     *
     * @return the patch parameters
     */
    public PatchParameters getParameters() {
        return parameters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        boolean result = object instanceof CommonSetPatch;
        if (result) {
            CommonSetPatch other = (CommonSetPatch) object;
            EqualsBuilder b = new EqualsBuilder();
            b.appendSuper(super.equals(object));
            b.append(parameters, other.parameters);
            result = b.isEquals();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder b = new HashCodeBuilder(1840448877, -1610819781);
        b.appendSuper(super.hashCode());
        b.append(parameters);
        return b.toHashCode();
    }
}

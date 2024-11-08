package be.lassi.lanbox.commands;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import be.lassi.lanbox.domain.PatchBufferProcessor;
import be.lassi.util.Hex;
import be.lassi.util.Util;

/**
 * Gets patch information from the lanbox.
 */
public class CommonGetPatch extends LanboxCommand {

    /**
     * Lanbox command identifier.
     */
    public static final String ID = "80";

    /**
     * The dimmers to be updated with patch information from the lanbox.
     */
    private final int startDimmerId;

    private int[] channelIds = new int[0];

    private PatchBufferProcessor processor;

    /**
     * Constructs a new command from raw request data.
     *
     * @param request the raw request data
     */
    public CommonGetPatch(final byte[] request) {
        super(request);
        startDimmerId = getInt4(3) - 1;
        int dimmerCount = getInt(7);
        channelIds = new int[dimmerCount];
    }

    /**
     * Constructs a new command.
     *
     * @param processor processes the patch information
     * @param startDimmerId the identifier of first dimmer
     * @param dimmerCount the number of dimmers for which to get the patch info
     */
    public CommonGetPatch(final PatchBufferProcessor processor, final int startDimmerId, final int dimmerCount) {
        super(ID, "*80llllcc#".length());
        this.processor = processor;
        this.startDimmerId = startDimmerId;
        Util.validate("dimmerCount", dimmerCount, 1, 255);
        Util.validate("startDimmerId", startDimmerId, 0, 511);
        Util.validate("startDimmerId + dimmerCount", startDimmerId + dimmerCount, 1, 512);
        set4(3, startDimmerId + 1);
        set2(7, dimmerCount);
        channelIds = new int[dimmerCount];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processResponse(final byte[] bytes) {
        super.processResponse(bytes);
        int channelCount = (bytes.length - 2) / 4;
        channelIds = new int[channelCount];
        for (int i = 0; i < channelCount; i++) {
            int channelNumber = Hex.getInt4(bytes, (i * 4) + 1);
            channelIds[i] = channelNumber - 1;
        }
        if (processor != null) {
            processor.process(startDimmerId, channelIds);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendCommand(final StringBuilder b) {
        b.append("CommonGetPatch(startDimmerId=");
        b.append(startDimmerId);
        b.append(", dimmerCount=");
        b.append(channelIds.length);
        b.append(")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendResponse(final StringBuilder b) {
        b.append("\n ");
        for (int i = 0; i < channelIds.length; i++) {
            b.append(" ");
            b.append(startDimmerId + i + 1);
            b.append("[");
            b.append(channelIds[i] + 1);
            b.append("]");
        }
    }

    public int getStartDimmerId() {
        return startDimmerId;
    }

    public int[] getChannelIds() {
        return channelIds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        boolean result = object instanceof CommonGetPatch;
        if (result) {
            CommonGetPatch other = (CommonGetPatch) object;
            EqualsBuilder b = new EqualsBuilder();
            b.appendSuper(super.equals(object));
            b.append(startDimmerId, other.startDimmerId);
            b.append(channelIds, other.channelIds);
            result = b.isEquals();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder b = new HashCodeBuilder(-1369372585, 1376996019);
        b.appendSuper(super.hashCode());
        b.append(channelIds);
        b.append(startDimmerId);
        return b.toHashCode();
    }
}

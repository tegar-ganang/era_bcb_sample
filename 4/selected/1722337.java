package be.lassi.lanbox.commands;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import be.lassi.lanbox.domain.ChannelChange;
import be.lassi.lanbox.domain.ChannelChanges;

/**
 * Lanbox command to set channel levels in cue scene.
 */
public class CueSceneWrite extends LanboxCommand {

    /**
     * Lanbox command identifier.
     */
    public static final String ID = "AC";

    /**
     * The maximum number of channels per CueSceneWrite command.
     */
    public static final int MAX_CHANNELS = 97;

    /**
     * Template for the fixed part of the command.
     */
    private static final String TEMPLATE = "*ACccccsssfnnnn#";

    /**
     * The number of bytes per channel in the variable part of the command.
     */
    public static final int BYTES_PER_CHANNEL = 6;

    /**
     * The cue list number.
     */
    private final int cueListNumber;

    /**
     * The cue step number.
     */
    private final int cueStepNumber;

    /**
     * The number of channels to be written.
     */
    private final int channelCount;

    private int sceneFlags;

    /**
     * The channel changes in the variable part of the command.
     */
    private final ChannelChanges channelChanges;

    /**
     * Constructs command from request buffer.
     *
     * @param request the lanbox request buffer
     */
    public CueSceneWrite(final byte[] request) {
        super(request);
        cueListNumber = getInt4(3);
        cueStepNumber = getInt(7);
        sceneFlags = getInt(9);
        channelCount = getInt4(11);
        int channelChangeCount = (request.length - TEMPLATE.length()) / BYTES_PER_CHANNEL;
        channelChanges = new ChannelChanges();
        int offset = 15;
        for (int i = 0; i < channelChangeCount; i++) {
            int channelId = getInt4(offset);
            int dmxValue = getInt(offset + 4);
            channelChanges.add(channelId, dmxValue);
            offset += BYTES_PER_CHANNEL;
        }
    }

    /**
     * Constructs a new command.
     *
     * @param channelCount the number of channels
     * @param cueListNumber the cue list number
     * @param cueStepNumber the cue step number
     * @param channelChanges the channel changes
     */
    public CueSceneWrite(final int channelCount, final int cueListNumber, final int cueStepNumber, final ChannelChanges channelChanges) {
        super(ID, TEMPLATE.length() + (BYTES_PER_CHANNEL * channelChanges.size()));
        this.cueListNumber = cueListNumber;
        this.cueStepNumber = cueStepNumber;
        this.channelCount = channelCount;
        this.channelChanges = channelChanges;
        assertChannelCount(channelChanges.size());
        set4(3, cueListNumber);
        set2(7, cueStepNumber);
        set2(9, 0);
        set4(11, channelCount);
        int offset = 15;
        for (ChannelChange change : channelChanges) {
            set4(offset, change.getChannelId());
            set2(offset + 4, change.getDmxValue());
            offset += BYTES_PER_CHANNEL;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendCommand(final StringBuilder b) {
        b.append("CueSceneWrite(cueListNumber=");
        b.append(cueListNumber);
        b.append(", cueStepNumber=");
        b.append(cueStepNumber);
        b.append(", sceneFlags=");
        b.append(sceneFlags);
        b.append(", channelCount=");
        b.append(channelCount);
        b.append(", channelChangeCount=");
        b.append(channelChanges.size());
        b.append(")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendCommandDetail(final StringBuilder b) {
        channelChanges.append(b);
    }

    private void assertChannelCount(final int count) {
        if (count > MAX_CHANNELS) {
            StringBuilder b = new StringBuilder();
            b.append("CueSceneWrite: Maximum number of channels (");
            b.append(MAX_CHANNELS);
            b.append(") exceeded: ");
            b.append(count);
            String message = b.toString();
            throw new AssertionError(message);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        boolean result = object instanceof CueSceneWrite;
        if (result) {
            CueSceneWrite other = (CueSceneWrite) object;
            EqualsBuilder b = new EqualsBuilder();
            b.appendSuper(super.equals(object));
            b.append(cueListNumber, other.cueListNumber);
            b.append(cueStepNumber, other.cueStepNumber);
            b.append(channelCount, other.channelCount);
            b.append(sceneFlags, other.sceneFlags);
            b.append(channelChanges, other.channelChanges);
            result = b.isEquals();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder b = new HashCodeBuilder(220199791, 217692211);
        b.appendSuper(super.hashCode());
        b.append(cueListNumber);
        b.append(cueStepNumber);
        b.append(channelCount);
        b.append(channelChanges);
        b.append(sceneFlags);
        return b.toHashCode();
    }
}

package be.lassi.lanbox.commands;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import be.lassi.lanbox.domain.ChannelChange;
import be.lassi.lanbox.domain.ChannelChanges;
import be.lassi.util.Hex;

/**
 * Lanbox command to read channel levels in cue scene.
 */
public class CueSceneRead extends LanboxCommand {

    /**
     * Lanbox command identifier.
     */
    public static final String ID = "AD";

    /**
     * The number of bytes per channel in the variable part of the command.
     */
    private static final int BYTES_PER_CHANNEL = CueSceneWrite.BYTES_PER_CHANNEL;

    private final int cueListNumber;

    private final int cueStepNumber;

    private final ChannelChanges channelChanges = new ChannelChanges();

    /**
     * Constructs a new command.
     *
     * @param cueListNumber the cue list identifier
     * @param cueStepNumber the cue step identifier
     */
    public CueSceneRead(final int cueListNumber, final int cueStepNumber) {
        super(ID, "*ABccccss#".length());
        this.cueListNumber = cueListNumber;
        this.cueStepNumber = cueStepNumber;
        set4(3, cueListNumber);
        set2(7, cueStepNumber);
    }

    /**
     * Constructs a new command from a request buffer.
     *
     * @param request the buffer to construct the command from
     */
    public CueSceneRead(final byte[] request) {
        super(request);
        cueListNumber = Hex.getInt4(request, 3);
        cueStepNumber = Hex.getInt(request, 7);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processResponse(final byte[] bytes) {
        super.processResponse(bytes);
        int channelCount = Hex.getInt(bytes, 5);
        int changesCount = (bytes.length - 7) / BYTES_PER_CHANNEL;
        for (int i = 0; i < changesCount; i++) {
            int offset = 7 + (i * BYTES_PER_CHANNEL);
            int channelId = Hex.getInt4(bytes, offset);
            int dmxValue = Hex.getInt(bytes, offset + 4);
            ChannelChange change = new ChannelChange(channelId, dmxValue);
            channelChanges.add(change);
        }
    }

    public ChannelChanges getChannelChanges() {
        return channelChanges;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendCommand(final StringBuilder b) {
        b.append("CueSceneRead(cueListNumber=");
        b.append(cueListNumber);
        b.append(", cueStepNumber=");
        b.append(cueStepNumber);
        b.append(", channelChanges=");
        channelChanges.append(b);
        b.append(")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        boolean result = object instanceof CueSceneRead;
        if (result) {
            CueSceneRead other = (CueSceneRead) object;
            EqualsBuilder b = new EqualsBuilder();
            b.appendSuper(super.equals(object));
            b.append(cueListNumber, other.cueListNumber);
            b.append(cueStepNumber, other.cueStepNumber);
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
        HashCodeBuilder b = new HashCodeBuilder(1368032631, -362285209);
        b.appendSuper(super.hashCode());
        b.append(cueListNumber);
        b.append(cueStepNumber);
        return b.toHashCode();
    }
}

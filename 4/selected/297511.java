package be.lassi.lanbox.udp;

import be.lassi.lanbox.Lanbox;

/**
 * Information about one buffer in a UDP packet that is received
 * from the Lanbox.
 */
public class DmxMessage implements Message {

    /**
     * The buffer identifier.
     */
    private int bufferId;

    /**
     * The index of the first channel.
     */
    private int channelOffset;

    /**
     * The level values. 
     */
    private int[] values;

    /**
     * Constructs a new buffer.
     * 
     * @param bufferId the buffer identifier
     * @param channelOffset the index of the first channel in the buffer
     * @param values the level values
     */
    public DmxMessage(final int bufferId, final int channelOffset, final int[] values) {
        this.bufferId = bufferId;
        this.channelOffset = channelOffset;
        this.values = values;
    }

    /**
     * Gets the buffer identifier
     * 
     * @return the buffer identifier
     */
    public int getBufferId() {
        return bufferId;
    }

    /**
     * Gets the index of the first channel.
     * 
     * @return the index of the first channel
     */
    public int getChannelOffset() {
        return channelOffset;
    }

    /**
     * Gets the level values.
     * 
     * @return the level values
     */
    public int[] getValues() {
        return values;
    }

    public void append(final StringBuilder b) {
        switch(getBufferId()) {
            case Lanbox.MIXER:
                b.append("MIXER  ");
                break;
            case Lanbox.DMX_IN:
                b.append("DMX_IN ");
                break;
            case Lanbox.DMX_OUT:
                b.append("DMX_OUT");
                break;
            case Lanbox.IO:
                b.append("IO     ");
                break;
            default:
                b.append("unknown");
        }
        b.append(": ");
        for (int j = 0; j < values.length; j++) {
            b.append(" ");
            b.append(j + channelOffset);
            b.append("[");
            b.append(values[j]);
            b.append("]");
        }
    }
}

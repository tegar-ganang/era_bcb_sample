package be.lassi.ui.patch;

import be.lassi.domain.Dimmer;

/**
 * Patch information for a dimmer.
 */
public class PatchDetail {

    /**
     * Index of column that indicates whether the dimmer is currently faded in.
     */
    public static final int ON = 0;

    /**
     * Index of dimmer number column.
     */
    public static final int DIMMER_NUMBER = 1;

    /**
     * Index of dimmer name column.
     */
    public static final int DIMMER_NAME = 2;

    /**
     * Index of the lanbox channel number column.
     */
    public static final int LANBOX_CHANNEL_NUMBER = 3;

    /**
     * Index of channel number column.
     */
    public static final int CHANNEL_NUMBER = 4;

    /**
     * Index of channel name column.
     */
    public static final int CHANNEL_NAME = 5;

    /**
     * Indicates whether dimmer is faded in.
     */
    private boolean on;

    /**
     * The dimmer for which we keep the patch details.
     * @aggregation
     */
    private final Dimmer dimmer;

    /**
     * Constructs a new instance.
     *
     * @param dimmer the dimmer for which to keep the patch details
     */
    public PatchDetail(final Dimmer dimmer) {
        this.dimmer = dimmer;
    }

    /**
     * Sets the 'on' indicator.
     *
     * @param on true if the dimmer is faded in
     */
    public void setOn(final boolean on) {
        this.on = on;
    }

    /**
     * Indicates whether the dimmer is faded in.
     *
     * @return true if the dimmer is faded in
     */
    public boolean isOn() {
        return on;
    }

    /**
     * Gets the dimmer.
     *
     * @return the dimmer
     */
    public Dimmer getDimmer() {
        return dimmer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(getClass().getName());
        b.append("(");
        b.append(dimmer.getId());
        b.append("=>");
        b.append(dimmer.getChannelId());
        b.append(")");
        String string = b.toString();
        return string;
    }
}

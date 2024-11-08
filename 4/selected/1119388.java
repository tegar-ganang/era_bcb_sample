package be.lassi.ui.show;

import be.lassi.preferences.DefaultPreferences;
import be.lassi.util.Util;

/**
 * Parameters used for the creation of a show.
 */
public class ShowParameters extends DefaultPreferences {

    public static final String CHANNEL_COUNT = "channelCount";

    public static final String SUBMASTER_COUNT = "submasterCount";

    public static final String CUE_COUNT = "cueCount";

    public static final int MAX_CUE_COUNT = 1000;

    public ShowParameters() {
        this("show", "Show parameters");
    }

    public ShowParameters(final String name, final String description) {
        super(name, description);
        add(CHANNEL_COUNT, "60");
        add(SUBMASTER_COUNT, "12");
        add(CUE_COUNT, "5");
    }

    /**
     * Gets the number of channels in the show.
     *
     * @return the number of channels in the show
     */
    public String getChannelCount() {
        return getString(CHANNEL_COUNT);
    }

    /**
     * Sets the number of channels in the show.
     *
     * @param channelCount the number of channels in the show
     */
    public void setChannelCount(final String channelCount) {
        set(CHANNEL_COUNT, channelCount);
    }

    /**
     * Gets the number of submasters in the show.
     *
     * @return the number of submasters in the show
     */
    public String getSubmasterCount() {
        return getString(SUBMASTER_COUNT);
    }

    /**
     * Sets the number of submasters in the show.
     *
     * @param submasterCount the number of submasters in the show
     */
    public void setSubmasterCount(final String submasterCount) {
        set(SUBMASTER_COUNT, submasterCount);
    }

    /**
     * Gets the initial number of cues in the show.
     *
     * @return the initial number of cues in the show
     */
    public String getCueCount() {
        return getString(CUE_COUNT);
    }

    /**
     * Sets the initial number of cues in the show.
     *
     * @param cueCount the initial number of cues in the show
     */
    public void setCueCount(final String cueCount) {
        set(CUE_COUNT, cueCount);
    }

    /**
     * Gets the number of channels in the show.
     *
     * @return the number of channels in the show
     */
    public int getIntChannelCount() {
        return Util.toInt(getChannelCount());
    }

    /**
     * Gets the number of submasters in the show.
     *
     * @return the number of submasters in the show
     */
    public int getIntSubmasterCount() {
        return Util.toInt(getSubmasterCount());
    }

    /**
     * Gets the initial number of cues in the show.
     *
     * @return the initial number of cues in the show
     */
    public int getIntCueCount() {
        return Util.toInt(getCueCount());
    }
}

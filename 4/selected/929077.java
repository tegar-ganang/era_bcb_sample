package be.lassi.domain;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.builder.HashCodeBuilder;
import be.lassi.base.Dirty;
import be.lassi.base.NamedObject;
import be.lassi.cues.Cues;
import be.lassi.util.equals.EqualsTester;

/**

 *
 *
 */
public class Show extends NamedObject {

    /**
     *
     */
    private final Dimmers dimmers;

    /**
     *
     */
    private final Channels channels;

    /**
     *
     */
    private final Submasters submasters;

    private final Groups groups;

    private final Fixtures fixtures;

    /**
     *
     */
    private Cues cues;

    private final Map<String, FrameProperties> frameProperties = new HashMap<String, FrameProperties>();

    /**
     * Input levels as read from the dmx input. This corresponds to the dimmer
     * outputs from the external dmx source.
     */
    private Levels inputs;

    private Levels outputs;

    /**
     * Input levels as read from the dmx input, but translated to
     * channels based on the patching information available in
     * the dimmer definitions.
     *
     * This is used to synchronize the values from the external
     * dmx input with our own dmx output, assuming that out own
     * patching information is the same as that of the external
     * dmx source.
     */
    private Levels channelInputs;

    /**
     *
     */
    private CueFading cueFading;

    /**
     * Constructs a new instance.
     *
     * @param dirty the dirty indicator
     * @param name
     */
    public Show(final Dirty dirty, final String name) {
        super(dirty, name);
        dimmers = new Dimmers(dirty);
        channels = new Channels(dirty);
        submasters = new Submasters(getDirty());
        fixtures = new Fixtures(getDirty());
        groups = new Groups(getDirty());
    }

    public void contructPart2() {
        inputs = new Levels(dimmers.size());
        outputs = new Levels(channels.size());
        channelInputs = new Levels(channels.size());
        cueFading = new CueFading(channels.size());
        cues = new Cues(getDirty(), submasters, channels.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "BC" })
    public boolean equals(final Object object) {
        boolean result = this == object;
        if (!result) {
            EqualsTester tester = EqualsTester.get(this, object);
            if (tester.isEquals()) {
                Show other = (Show) object;
                tester.test(super.equals(other));
                tester.test(channels, other.channels);
                tester.test(dimmers, other.dimmers);
                tester.test(submasters, other.submasters);
                tester.test(frameProperties, other.frameProperties);
                tester.test(cues, other.cues);
            }
            result = tester.isEquals();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder b = new HashCodeBuilder(78236925, -398807245);
        b.append(super.hashCode());
        b.append(dimmers);
        return b.toHashCode();
    }

    /**
     * Method getChannels.
     * @return Channels
     */
    public Channels getChannels() {
        return channels;
    }

    /**
     * Method getCueFading.
     * @return CueFading
     */
    public CueFading getCueFading() {
        return cueFading;
    }

    /**
     * Method getCueList.
     * @return CueList
     */
    public Cues getCues() {
        return cues;
    }

    /**
     * Method getDimmers.
     * @return Dimmers
     */
    public Dimmers getDimmers() {
        return dimmers;
    }

    /**
     * Method getNumberOfChannels.
     * @return int
     */
    public int getNumberOfChannels() {
        return channels.size();
    }

    /**
     * Method getNumberOfDimmers.
     * @return int
     */
    public int getNumberOfDimmers() {
        return dimmers.size();
    }

    /**
     * Method getNumberOfSubmasters.
     * @return int
     */
    public int getNumberOfSubmasters() {
        return submasters.size();
    }

    /**
     * Method getSubmasters.
     * @return Submasters
     */
    public Submasters getSubmasters() {
        return submasters;
    }

    public Levels getChannelInputs() {
        return channelInputs;
    }

    /**
     *
     */
    public void setInputValue(final int index, final float value) {
        if (index < inputs.size()) {
            inputs.get(index).setValue(value);
            final int channelIndex = dimmers.get(index).getChannelId();
            if (channelIndex != -1) {
                channelInputs.get(channelIndex).setValue(value);
            }
        }
    }

    /**
     *
     */
    public void updateChannelInputs() {
        for (int i = 0; i < channelInputs.size(); i++) {
            float value = 0f;
            boolean found = false;
            for (int j = 0; !found && j < dimmers.size(); j++) {
                final int channelIndex = dimmers.get(j).getChannelId();
                if (channelIndex == i) {
                    found = true;
                    value = dimmers.get(j).getValue();
                }
            }
            channelInputs.get(i).setValue(value);
        }
    }

    /**
     *
     */
    public void setDimmerValue(final int index, final float value) {
        if (index < getNumberOfDimmers()) {
            dimmers.setValue(index, value);
        }
    }

    /**
     * Set the dmx value of the channel with given index.
     *
     * @param channelIndex Index of channel to be changed.
     * @param value        The new channel levelvalue.
     */
    public void setChannelValue(final int channelIndex, final float value) {
        if (channelIndex < getNumberOfChannels()) {
            channels.setValue(channelIndex, value);
        }
    }

    public FrameProperties[] getFrameProperties() {
        final Collection<FrameProperties> c = frameProperties.values();
        return c.toArray(new FrameProperties[c.size()]);
    }

    /**
     * Get the frame position (x, y) and size (width, height).
     *
     * @param frameId identification string of the frame
     * @return the frame position and size
     */
    public FrameProperties getFrameProperties(final String frameId) {
        return frameProperties.get(frameId);
    }

    /**
     * Set the frame position (x, y) and size (width, height)
     * of frame with given id.
     *
     * @param frameId identification string of the frame
     * @param fps     frame properties
     */
    public void setFrameProperties(final String frameId, final FrameProperties fps) {
        frameProperties.put(frameId, fps);
    }

    /**
     * Gets the channel groups.
     *
     * @return the channel groups
     */
    public Groups getGroups() {
        return groups;
    }

    public Fixtures getFixtures() {
        return fixtures;
    }
}

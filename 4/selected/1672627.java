package be.lassi.cues;

import java.util.List;
import be.lassi.lanbox.cuesteps.CueStep;
import be.lassi.lanbox.domain.ChannelChanges;
import be.lassi.util.Dmx;
import be.lassi.util.equals.EqualsTester;
import com.google.common.collect.Lists;

/**
 * A Cue represents a state of lighting on the stage.
 * <p>
 * The lighting state is determined by the lighting levels
 * for individual channels and levels per submaster.
 * <p>
 * Cues can be 'selected' to perform actions such as cut and copy.
 * <p>
 * A cue is 'current' when it is the cue that will be used for the
 * next lighting action (GO).  There should be only one cue that
 * is the current cue at anyone time.  It is the responsibility of
 * the CueList class to make sure that is true.
 * <p>
 * Implementers of CueListener can listen for changes.  To get notified
 * of changes to the cue Timing, a TimingListener has to be registered
 * with the Timing object itself.
 *
 */
public class LightCueDetail extends CueDetail {

    /**
     * Fade up/down and delay timing.
     *
     * @aggregation
     */
    private final Timing timing = new Timing();

    private boolean timingSelected;

    /**
     * Lighting levels per channel.
     *
     * @aggregation
     */
    private final CueChannelLevels channelLevels;

    /**
     * Lighting levels per submaster.
     *
     * @aggregation
     */
    private final CueSubmasterLevels submasterLevels;

    /**
     * Constructs new cue detail.
     *
     * @param number
     * @param prompt
     * @param numberOfChannels
     * @param numberOfSubmasters
     */
    public LightCueDetail(final Timing timing, final int numberOfChannels, final int numberOfSubmasters) {
        this.timing.set(timing);
        channelLevels = new CueChannelLevels(numberOfChannels);
        submasterLevels = new CueSubmasterLevels(numberOfSubmasters);
    }

    public boolean isTimingSelected() {
        return timingSelected;
    }

    public void setTimingSelected(final boolean selected) {
        timingSelected = selected;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return "Light Cue";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LightCueDetail copy() {
        LightCueDetail result = new LightCueDetail(timing, channelLevels.size(), submasterLevels.size());
        result.channelLevels.set(channelLevels);
        result.submasterLevels.set(submasterLevels);
        return result;
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
                LightCueDetail other = (LightCueDetail) object;
                tester.test(timing, other.timing);
                tester.test(channelLevels, other.channelLevels);
                tester.test(submasterLevels, other.submasterLevels);
            }
            result = tester.isEquals();
        }
        return result;
    }

    /**
     * Return the level in this cue for the channel at given index.
     *
     * @param channelIndex
     * @return CueChannelLevel
     */
    public CueChannelLevel getChannelLevel(final int channelIndex) {
        return channelLevels.get(channelIndex);
    }

    /**
     * @return
     */
    public int getNumberOfChannels() {
        return channelLevels.size();
    }

    /**
     * @return
     */
    public int getNumberOfSubmasters() {
        return submasterLevels.size();
    }

    /**
     * Return the level in this cue for the submaster at given index.
     * @param submasterIndex
     * @return CueSubmasterLevel
     */
    public CueSubmasterLevel getSubmasterLevel(final int submasterIndex) {
        return submasterLevels.get(submasterIndex);
    }

    /**
     * Return the cue fade and delay timing.
     * @return Timing
     */
    public Timing getTiming() {
        return timing;
    }

    /**
     * @param timing
     */
    public void setTiming(final Timing timing) {
        setTiming(null, timing);
    }

    /**
     * @param from
     * @param timing
     */
    public void setTiming(final CueListener from, final Timing timing) {
        if (!this.timing.equals(timing)) {
            this.timing.set(timing);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CueStep> getCueSteps() {
        return new LightCueStepsFactory(this).getCueSteps();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void append(final StringBuilder b) {
        b.append("light ");
        timing.append(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(getClass().getName());
        b.append('(');
        timing.append(b);
        b.append(')');
        return b.toString();
    }

    public ChannelChanges getChannelChanges() {
        ChannelChanges changes = new ChannelChanges();
        int channelId = 0;
        for (CueChannelLevel level : channelLevels) {
            channelId++;
            if (level.isActive()) {
                int dmxValue = Dmx.getDmxValue(level.getValue());
                changes.add(channelId, dmxValue);
            }
        }
        return changes;
    }

    public List<CueSubmasterLevel> getActiveLevels() {
        List<CueSubmasterLevel> activeLevels = Lists.newArrayList();
        for (CueSubmasterLevel level : submasterLevels) {
            if (level.isActive()) {
                activeLevels.add(level);
            }
        }
        return activeLevels;
    }
}

package be.lassi.cues;

import be.lassi.domain.LevelValue;
import be.lassi.util.Util;
import be.lassi.util.equals.EqualsTester;

/**
 * Represent the level (intensity) of a channel within a Cue.  The level is
 * composed of the level setting of the individual channel and the contributions
 * of the submasters to this channel.
 * <p>
 * The string representation of the cue channel level consists of the channel
 * and submaster levels separated by two dots.
 * <p>
 * Example: 50..60
 * <p>
 * Note that the submaster level value is composed of the levels of all
 * submasters.  The resulting level information is duplicated here for
 * convenience.  It is the responsibility of the class {@link LightCues LightCues}
 * to keep these values in sync.
 *
 */
public class CueChannelLevel extends CueLevel {

    /**
     * The contribution to the level of the actual channel level.
     */
    private final LevelValue channelLevelValue = new LevelValue(0, true);

    /**
     * The contribution to the level of the submasters.
     */
    private final LevelValue submasterLevelValue = new LevelValue(0, false);

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
                CueChannelLevel other = (CueChannelLevel) object;
                tester.test(super.equals(other));
                tester.test(channelLevelValue, other.channelLevelValue);
                tester.test(submasterLevelValue, other.submasterLevelValue);
            }
            result = tester.isEquals();
        }
        return result;
    }

    /**
     * Get channel level percentage value.
     * @return int
     */
    public int getChannelIntValue() {
        return channelLevelValue.getIntValue();
    }

    public LevelValue getChannelLevelValue() {
        return channelLevelValue;
    }

    public LevelValue getSubmasterLevelValue() {
        return submasterLevelValue;
    }

    /**
     * Get submaster level percentage value.
     * @return int
     */
    public int getSubmasterIntValue() {
        return submasterLevelValue.getIntValue();
    }

    /**
     * Get submaster level float value.
     * @return float
     */
    public float getSubmasterValue() {
        return submasterLevelValue.getValue();
    }

    /**
     * Return the level value, calculated using HTP (Highest Takes Precendence) rule.
     * @return float
     */
    public float getValue() {
        float value = 0f;
        float channelValue = channelLevelValue.getValue();
        float submasterValue = submasterLevelValue.getValue();
        if (channelValue > submasterValue) {
            value = channelValue;
        } else {
            value = submasterValue;
        }
        return value;
    }

    /**
     * Set channel level float value.
     * @param value
     * @deprecated
     */
    @Deprecated
    public void setChannelValue(final float value) {
        channelLevelValue.setValue(value);
    }

    /**
     * Set submaster level float value.
     * @param value
     */
    public void setSubmasterValue(final float value) {
        submasterLevelValue.setValue(value);
    }

    /**
     * Return string representation.
     *
     * <pre>
     *   xx..yy
     *     xx = channel level value
     *     yy = submaster level value
     *
     *   xx
     *     if submaster level value is 0
     *
     * </pre>
     * @return String
     */
    public String string() {
        StringBuilder b = new StringBuilder();
        if (channelLevelValue.isActive()) {
            int value = channelLevelValue.getIntValue();
            b.append(value);
        } else {
            b.append("-");
        }
        if (submasterLevelValue.isActive()) {
            int value = submasterLevelValue.getIntValue();
            if (channelLevelValue.isActive()) {
                if (value > 0) {
                    b.append("..");
                    b.append(value);
                }
            } else {
                b.append("..");
                b.append(value);
            }
        }
        return b.toString();
    }

    /**
     * Set the values of given CueChannelLevel to the values of this instance.
     * @param other
     */
    public void set(final CueChannelLevel other) {
        float channelValue = other.channelLevelValue.getValue();
        float submasterValue = other.submasterLevelValue.getValue();
        channelLevelValue.setValue(channelValue);
        submasterLevelValue.setValue(submasterValue);
        setDerived(other.isDerived());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(getClass().getName());
        b.append("(channelLevelValue=");
        b.append(channelLevelValue.getIntValue());
        b.append(", submasterLevelValue=");
        b.append(submasterLevelValue.getIntValue());
        b.append(", derived=");
        b.append(isDerived());
        b.append(", active=");
        b.append(isActive());
        b.append(")");
        return b.toString();
    }

    public boolean isActive() {
        return channelLevelValue.isActive() || submasterLevelValue.isActive();
    }

    /**
     * Gets percentage value (converted from float value).
     *
     * @return int Percentage value (between 0 and 100).
     */
    public int getIntValue() {
        return Util.toPercentage(getValue());
    }
}

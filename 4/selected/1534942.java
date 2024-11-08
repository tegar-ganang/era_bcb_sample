package be.lassi.cues;

import be.lassi.domain.Level;
import be.lassi.domain.LevelValue;
import be.lassi.domain.Submaster;
import be.lassi.domain.Submasters;

public class LightCueCalculator {

    private final Submasters submasters;

    private final NewLightCues cues;

    private final int channelCount;

    /**
     * The default for a level value is to be 'active'.
     */
    private static final boolean DEFAULT_ACTIVE = true;

    private static final float DEFAULT_LEVEL = 0f;

    public LightCueCalculator(final Submasters submasters, final NewLightCues cues, final int channelCount) {
        this.submasters = submasters;
        this.cues = cues;
        this.channelCount = channelCount;
    }

    public void calculate() {
        calculateChannelLevelValues();
        calculateDerivedSubmasterLevelValues();
        calculateChannelSubmasterLevelValues();
    }

    private void calculateChannelLevelValues() {
        for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
            float value = DEFAULT_LEVEL;
            boolean active = DEFAULT_ACTIVE;
            for (LightCueDetail detail : cues) {
                CueChannelLevel level = detail.getChannelLevel(channelIndex);
                LevelValue levelValue = level.getChannelLevelValue();
                if (level.isDerived()) {
                    levelValue.setValue(value);
                    levelValue.setActive(active);
                } else {
                    value = levelValue.getValue();
                    active = levelValue.isActive();
                }
            }
        }
    }

    private void calculateDerivedSubmasterLevelValues() {
        for (int submasterIndex = 0; submasterIndex < submasters.size(); submasterIndex++) {
            float value = DEFAULT_LEVEL;
            boolean active = DEFAULT_ACTIVE;
            for (LightCueDetail detail : cues) {
                CueSubmasterLevel cueSubmasterLevel = detail.getSubmasterLevel(submasterIndex);
                LevelValue levelValue = cueSubmasterLevel.getLevelValue();
                if (cueSubmasterLevel.isDerived()) {
                    levelValue.setValue(value);
                    levelValue.setActive(active);
                } else {
                    value = levelValue.getValue();
                    active = levelValue.isActive();
                }
            }
        }
    }

    /**
     * Calculates the contribution of channel levels within a submaster
     * definition, on the submaster level of individual channels.
     *
     * Makes sure the submaster levels in the channels are also
     * present in subsequent derived channels.
     */
    private void calculateChannelSubmasterLevelValues() {
        for (LightCueDetail detail : cues) {
            for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
                float value = 0f;
                boolean active = false;
                for (int submasterIndex = 0; submasterIndex < submasters.size(); submasterIndex++) {
                    Submaster submaster = submasters.get(submasterIndex);
                    Level submasterChannelLevel = submaster.getLevel(channelIndex);
                    CueSubmasterLevel cueSubmasterLevel = detail.getSubmasterLevel(submasterIndex);
                    if (cueSubmasterLevel.isActive() && submasterChannelLevel.isActive()) {
                        float value1 = submasterChannelLevel.getValue();
                        float value2 = cueSubmasterLevel.getValue();
                        float contribution = value1 * value2;
                        value = Math.max(value, contribution);
                        active = true;
                    }
                }
                updateCueChannelSubmasterLevel(detail, channelIndex, value, active);
            }
        }
    }

    private void updateCueChannelSubmasterLevel(final LightCueDetail cueDetail, final int channelIndex, final float value, final boolean active) {
        CueChannelLevel level = cueDetail.getChannelLevel(channelIndex);
        LevelValue levelValue = level.getSubmasterLevelValue();
        levelValue.setValue(value);
        levelValue.setActive(active);
    }
}

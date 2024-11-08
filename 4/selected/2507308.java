package be.lassi.cues;

import be.lassi.base.Dirty;
import be.lassi.base.DirtyStub;
import be.lassi.domain.Level;
import be.lassi.domain.LevelValue;
import be.lassi.domain.Submaster;
import be.lassi.domain.Submasters;

/**
 * Collection of Cues.
 *
 *
 *
 *
 */
public class LightCues extends CueCollection {

    /**
     * This reference to the show submasters is used to derive the submaster
     * contribution to the channel values.
     */
    private final Submasters submasters;

    public LightCues(final Submasters submasters) {
        this(new DirtyStub(), submasters);
    }

    /**
     * Create new instance.
     *
     * @param newSubmasters
     */
    public LightCues(final Dirty dirty, final Submasters submasters) {
        super(dirty);
        this.submasters = submasters;
        setCueListener(new MyCueListener());
    }

    public void remove(final int index) {
        removeCue(index);
        updateLightCueIndexes();
        if (index < size()) {
            updateValuesFromPreviousCue(index);
            updateSubsequentCues(index);
        }
    }

    /**
     * Updates the light cue indexes to match the index in the light cue
     * collection.
     */
    private void updateLightCueIndexes() {
        for (int i = 0; i < size(); i++) {
            get(i).setLightCueIndex(i);
        }
    }

    public LightCueDetail getDetail(final int lightCueIndex) {
        return (LightCueDetail) get(lightCueIndex).getDetail();
    }

    /**
     * Get number of submasters.
     *
     * @return int
     */
    private int getNumberOfSubmasters() {
        return submasters.getNumberOfSubmasters();
    }

    /**
     * Add a <code>Cue</code> at given index.
     */
    public void insert(final int index, final Cue cue) {
        if (!cue.isLightCue()) {
            String found = cue.getDetail().getType();
            throw new IllegalArgumentException("Expected light cue, found: " + found);
        }
        insertCue(index, cue);
        updateLightCueIndexes();
        updateValuesFromPreviousCue(index);
        updateSubsequentCues(index);
        updateCueChannelSubmasterActiveIndicators();
        fireAdded(index, cue);
    }

    /**
     * Set value of given channel cell.
     * <p>
     * The cell is set to 'not derived'. Set corresponding levels in subsequent
     * cues to same value until non-derived level is encountered.
     *
     * @param cueIndex
     * @param channelIndex
     * @param value
     */
    public void setChannel(final int cueIndex, final int channelIndex, final float value) {
        updateChannel(cueIndex, channelIndex, false, value);
        markDirty();
    }

    /**
     * Make given channel cell 'derived'. Pick up the value from the previous
     * cue, or set the value to zero if this is the first cue.
     *
     * @param cueIndex
     * @param channelIndex
     */
    public void resetChannel(final int lightCueIndex, final int channelIndex) {
        float value = 0f;
        if (lightCueIndex > 0) {
            LightCueDetail detail = getDetail(lightCueIndex - 1);
            value = detail.getChannelLevel(channelIndex).getChannelLevelValue().getValue();
        }
        updateChannel(lightCueIndex, channelIndex, true, value);
        markDirty();
    }

    public void deactivateChannel(final int lightCueIndex, final int channelIndex) {
        LightCueDetail detail = getDetail(lightCueIndex);
        CueChannelLevel level = detail.getChannelLevel(channelIndex);
        LevelValue levelValue = level.getChannelLevelValue();
        level.setDerived(false);
        levelValue.setActive(false);
        boolean foundNonDerived = false;
        for (int i = lightCueIndex + 1; !foundNonDerived && i < size(); i++) {
            CueChannelLevel next = getDetail(i).getChannelLevel(channelIndex);
            if (next.isDerived()) {
                LevelValue levelValue2 = next.getChannelLevelValue();
                levelValue2.setActive(false);
                levelValue2.setValue(0f);
                next.setSubmasterValue(0f);
                fireChannelLevelChanged(i, channelIndex);
            } else {
                foundNonDerived = true;
            }
        }
        markDirty();
    }

    /**
     * Set value (and 'derived' state) of channel cell at given coordinates, and
     * update subsequent cells until a non-derived cell is encountered.
     *
     * @param cueIndex
     * @param channelIndex
     * @param derived
     * @param value
     */
    private void updateChannel(final int lightCueIndex, final int channelIndex, final boolean derived, final float value) {
        LightCueDetail detail = getDetail(lightCueIndex);
        CueChannelLevel level = detail.getChannelLevel(channelIndex);
        level.setDerived(derived);
        LevelValue levelValue = level.getChannelLevelValue();
        levelValue.setActive(true);
        levelValue.setValue(value);
        fireChannelLevelChanged(lightCueIndex, channelIndex);
        updateSubsequentChannels(lightCueIndex, channelIndex, value);
    }

    /**
     * Set value of given submaster cell.
     * <p>
     * The cell is set to 'not derived'. Set corresponding levels in subsequent
     * cues to same value until non-derived level is encountered.
     *
     * @param cueIndex
     * @param submasterIndex
     * @param value
     */
    public void setCueSubmaster(final int cueIndex, final int submasterIndex, final float value) {
        updateSubmaster(cueIndex, submasterIndex, false, value);
        updateCueChannelSubmasterActiveIndicators(cueIndex);
        markDirty();
    }

    /**
     * Make given submaster cell 'derived'. Pick up the value from the previous
     * cue, or set the value to zero if this is the first cue.
     *
     * @param cueIndex
     * @param submasterIndex
     */
    public void resetSubmaster(final int lightCueIndex, final int submasterIndex) {
        float value = 0f;
        if (lightCueIndex > 0) {
            LightCueDetail detail = getDetail(lightCueIndex - 1);
            value = detail.getSubmasterLevel(submasterIndex).getValue();
        }
        updateSubmaster(lightCueIndex, submasterIndex, true, value);
        markDirty();
    }

    /**
     * Set value (and 'derived' state) of submaster cell at given coordinates.
     * Update corresponding channel values. Update subsequent cells until a
     * non-derived cell is encountered.
     *
     * @param cueIndex
     * @param submasterIndex
     * @param derived
     * @param value
     */
    private void updateSubmaster(final int lightCueIndex, final int submasterIndex, final boolean derived, final float value) {
        LightCueDetail detail = getDetail(lightCueIndex);
        CueSubmasterLevel level = detail.getSubmasterLevel(submasterIndex);
        level.setDerived(derived);
        LevelValue levelValue = level.getLevelValue();
        levelValue.setValue(value);
        levelValue.setActive(true);
        fireSubmasterLevelChanged(lightCueIndex, submasterIndex);
        updateChannelSubmasterValues(lightCueIndex);
        updateSubsequentSubmasters(lightCueIndex, submasterIndex, value);
    }

    /**
     * Update the submaster values in the channels of given cue with the
     * calculated submaster value.
     *
     * @param cueIndex
     */
    private void updateChannelSubmasterValues(final int cueIndex) {
        int numberOfChannels = 0;
        if (submasters.size() > 0) {
            numberOfChannels = submasters.get(0).getNumberOfLevels();
        }
        for (int channelIndex = 0; channelIndex < numberOfChannels; channelIndex++) {
            updateChannelValue(cueIndex, channelIndex);
        }
    }

    /**
     * Recalculates the channel values in all cues for a given channel index,
     * and updates where required.
     *
     * @param channelIndex
     */
    private void updateChannelValues(final int channelIndex) {
        for (int cueIndex = 0; cueIndex < size(); cueIndex++) {
            updateChannelValue(cueIndex, channelIndex);
        }
    }

    /**
     * Recalculates a given channel value for a given cue, and update if
     * required.
     *
     * @param cueIndex
     * @param channelIndex
     */
    private void updateChannelValue(final int lightCueIndex, final int channelIndex) {
        LightCueDetail detail = getDetail(lightCueIndex);
        float value = calculateChannelSubmasterLevelValue(detail, channelIndex);
        CueChannelLevel level = detail.getChannelLevel(channelIndex);
        if (Math.abs(level.getSubmasterValue() - value) > 0.001f) {
            level.setSubmasterValue(value);
            fireChannelLevelChanged(lightCueIndex, channelIndex);
        }
    }

    /**
     * Answer for given channel, the level value coming contributed to by each
     * of the submasters (HTP = Highest Takes Precendence).
     *
     * @param cue
     * @param channelIndex
     * @return float
     */
    private float calculateChannelSubmasterLevelValue(final LightCueDetail cue, final int channelIndex) {
        float max = 0f;
        for (int submasterIndex = 0; submasterIndex < submasters.getNumberOfSubmasters(); submasterIndex++) {
            float submasterValue = cue.getSubmasterLevel(submasterIndex).getValue();
            float channelValue = submasters.get(submasterIndex).getLevelValue(channelIndex);
            float value = submasterValue * channelValue;
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    /**
     * For each cell in given Cue, pick up the value from the previous cue, if
     * the cell is 'derived'.
     *
     * @param cueIndex the cue index
     */
    private void updateValuesFromPreviousCue(final int cueIndex) {
        if (cueIndex > 0) {
            LightCueDetail lightCue = getDetail(cueIndex);
            for (int i = 0; i < lightCue.getNumberOfChannels(); i++) {
                updateChannelValueFromPreviousCue(cueIndex, i);
            }
            for (int i = 0; i < getNumberOfSubmasters(); i++) {
                updateSubmasterValueFromPreviousCue(cueIndex, i);
            }
        }
    }

    /**
     * For a given channel cell, pick up the value from the previous cue, if the
     * cell is 'derived'.
     *
     * @param cueIndex
     * @param channelIndex
     */
    private void updateChannelValueFromPreviousCue(final int lightCueIndex, final int channelIndex) {
        if (lightCueIndex > 0) {
            LightCueDetail detail = getDetail(lightCueIndex);
            LightCueDetail previousLightCue = getDetail(lightCueIndex - 1);
            if (detail.getChannelLevel(channelIndex).isDerived()) {
                float value = previousLightCue.getChannelLevel(channelIndex).getChannelLevelValue().getValue();
                detail.getChannelLevel(channelIndex).setChannelValue(value);
            }
        }
    }

    /**
     * For a given submaster cell, pick up the value from the previous cue, if
     * the cell is 'derived'.
     *
     * @param cueIndex
     * @param submasterIndex
     */
    private void updateSubmasterValueFromPreviousCue(final int lightCueIndex, final int submasterIndex) {
        if (lightCueIndex > 0) {
            LightCueDetail detail = getDetail(lightCueIndex);
            LightCueDetail previousLightCue = getDetail(lightCueIndex - 1);
            if (detail.getSubmasterLevel(submasterIndex).isDerived()) {
                float value = previousLightCue.getSubmasterLevel(submasterIndex).getValue();
                detail.getSubmasterLevel(submasterIndex).getLevelValue().setValue(value);
                updateChannelSubmasterValues(lightCueIndex);
            }
        }
    }

    /**
     * Update all derived cells to the left of given cue.
     *
     * @param cueIndex
     */
    private void updateSubsequentCues(final int lightCueIndex) {
        LightCueDetail lightCue = getDetail(lightCueIndex);
        for (int i = 0; i < lightCue.getNumberOfChannels(); i++) {
            float value = lightCue.getChannelLevel(i).getChannelLevelValue().getValue();
            updateSubsequentChannels(lightCueIndex, i, value);
        }
        for (int i = 0; i < getNumberOfSubmasters(); i++) {
            float value = lightCue.getSubmasterLevel(i).getValue();
            updateSubsequentSubmasters(lightCueIndex, i, value);
        }
    }

    /**
     * Update all derived cells to the left of given channel cell, until a
     * non-derived level cell is encountered.
     *
     * @param cueIndex
     * @param channelIndex
     * @param value
     */
    private void updateSubsequentChannels(final int lightCueIndex, final int channelIndex, final float value) {
        boolean foundNonDerived = false;
        for (int i = lightCueIndex + 1; !foundNonDerived && i < size(); i++) {
            CueChannelLevel next = getDetail(i).getChannelLevel(channelIndex);
            if (next.isDerived() && next.isActive()) {
                next.setChannelValue(value);
                fireChannelLevelChanged(i, channelIndex);
            } else {
                foundNonDerived = true;
            }
        }
    }

    /**
     * Update all derived cells to the left of given submaster cell, until a
     * non-derived level cell is encountered.
     *
     * Update channel submaster values accordingly.
     *
     * @param cueIndex
     * @param submasterIndex
     * @param value
     */
    private void updateSubsequentSubmasters(final int lightCueIndex, final int submasterIndex, final float value) {
        boolean foundNonDerived = false;
        for (int i = lightCueIndex + 1; !foundNonDerived && i < size(); i++) {
            CueSubmasterLevel next = getDetail(i).getSubmasterLevel(submasterIndex);
            if (next.isDerived()) {
                next.getLevelValue().setValue(value);
                fireSubmasterLevelChanged(i, submasterIndex);
                updateChannelSubmasterValues(i);
            } else {
                foundNonDerived = true;
            }
        }
    }

    /**
     * Notify listeners that a channel cell has changed.
     *
     * @param cueIndex
     * @param channelIndex
     */
    private void fireChannelLevelChanged(final int lightCueIndex, final int channelIndex) {
        for (CuesListener listener : getListeners()) {
            listener.channelLevelChanged(lightCueIndex, channelIndex);
        }
    }

    /**
     * Notify listeners that a submaster cell has changed.
     *
     * @param cueIndex
     * @param submasterIndex
     */
    private void fireSubmasterLevelChanged(final int lightCueIndex, final int submasterIndex) {
        for (CuesListener listener : getListeners()) {
            listener.submasterLevelChanged(lightCueIndex, submasterIndex);
        }
    }

    public void setSubmasterChannel(final int submasterIndex, final int channelIndex, final float value) {
        Level level = submasters.get(submasterIndex).getLevel(channelIndex);
        level.setActive(true);
        level.setValue(value);
        updateChannelValues(channelIndex);
        updateCueChannelSubmasterActiveIndicators();
        markDirty();
    }

    private void updateCueChannelSubmasterActiveIndicators() {
        for (int i = 0; i < size(); i++) {
            updateCueChannelSubmasterActiveIndicators(i);
        }
    }

    public void deactivateSubmasterLevel(final int submasterIndex, final int channelIndex) {
        submasters.get(submasterIndex).getLevel(channelIndex).setActive(false);
        updateChannelValues(channelIndex);
        markDirty();
    }

    private class MyCueListener implements CueListener {

        public void currentChanged(final Cue cue) {
            fireCurrentChanged();
        }

        public void numberChanged(final Cue cue) {
            fireCueNumbersChanged();
        }

        public void selectedChanged(final Cue cue) {
            fireSelectionChanged();
        }

        public void descriptionChanged(final Cue cue) {
        }
    }

    public void deactivateCueSubmaster(final int lightCueIndex, final int submasterIndex) {
        LightCueDetail detail = getDetail(lightCueIndex);
        CueSubmasterLevel level = detail.getSubmasterLevel(submasterIndex);
        level.setDerived(false);
        LevelValue levelValue = level.getLevelValue();
        levelValue.setActive(false);
        fireSubmasterLevelChanged(lightCueIndex, submasterIndex);
        updateCueChannelSubmasterActiveIndicators(lightCueIndex);
        boolean foundNonDerived = false;
        for (int i = lightCueIndex + 1; !foundNonDerived && i < size(); i++) {
            CueSubmasterLevel next = getDetail(i).getSubmasterLevel(submasterIndex);
            if (next.isDerived()) {
                LevelValue levelValue2 = next.getLevelValue();
                levelValue2.setActive(false);
                levelValue2.setValue(0f);
                fireSubmasterLevelChanged(i, submasterIndex);
                updateCueChannelSubmasterActiveIndicators(i);
            } else {
                foundNonDerived = true;
            }
        }
        markDirty();
    }

    private void updateCueChannelSubmasterActiveIndicators(final int lightCueIndex) {
        LightCueDetail detail = getDetail(lightCueIndex);
        for (int channelIndex = 0; channelIndex < detail.getNumberOfChannels(); channelIndex++) {
            boolean active = isChannelActiveInOneOfTheSubmasters(detail, channelIndex);
            CueChannelLevel channelLevel = detail.getChannelLevel(channelIndex);
            LevelValue levelValue = channelLevel.getSubmasterLevelValue();
            if (levelValue.isActive() != active) {
                levelValue.setActive(active);
                fireChannelLevelChanged(lightCueIndex, channelIndex);
            }
        }
    }

    private boolean isChannelActiveInOneOfTheSubmasters(final LightCueDetail detail, final int channelIndex) {
        boolean active = false;
        for (int submasterIndex = 0; !active && submasterIndex < submasters.size(); submasterIndex++) {
            CueSubmasterLevel level = detail.getSubmasterLevel(submasterIndex);
            if (level.getLevelValue().isActive()) {
                Submaster submaster = submasters.get(submasterIndex);
                active = submaster.isChannelActive(channelIndex);
            }
        }
        return active;
    }

    public void removeAll() {
        int numberOfCues = size();
        for (int i = 0; i < numberOfCues; i++) {
            removeCue(0);
        }
    }
}

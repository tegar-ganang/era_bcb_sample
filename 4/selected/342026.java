package be.lassi.cues;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the CuesListener interface for testing purposes.
 *
 */
public class TestingCuesListener implements CuesListener {

    private int indexLastCueAdded = -1;

    private int indexLastCueRemoved = -1;

    private Cue lastCueAdded;

    private Cue lastCueRemoved;

    private int numberOfRemovals = 0;

    private boolean currentChanged = false;

    private List<Change> channelChanges = new ArrayList<Change>();

    private List<Change> submasterChanges = new ArrayList<Change>();

    public void added(final int index, final Cue cue) {
        indexLastCueAdded = index;
        lastCueAdded = cue;
    }

    public void currentChanged() {
        setCurrentChanged(true);
    }

    public void selectionChanged() {
    }

    public void cueNumbersChanged() {
    }

    public void removed(final int index, final Cue cue) {
        indexLastCueRemoved = index;
        lastCueRemoved = cue;
        numberOfRemovals++;
    }

    public int getIndexLastCueAdded() {
        return indexLastCueAdded;
    }

    public int getIndexLastCueRemoved() {
        return indexLastCueRemoved;
    }

    public Cue getLastCueAdded() {
        return lastCueAdded;
    }

    public Cue getLastCueRemoved() {
        return lastCueRemoved;
    }

    public int getNumberOfRemovals() {
        return numberOfRemovals;
    }

    public boolean isCurrentChanged() {
        return currentChanged;
    }

    public void setCurrentChanged(final boolean value) {
        currentChanged = value;
    }

    /**
     * Implements the CueListListener interface.  Records the change
     * in our collection of channel cell changes.
     * 
     * @param cueIndex
     * @param channelIndex
     */
    public void channelLevelChanged(final int cueIndex, final int channelIndex) {
        channelChanges.add(new Change(cueIndex, channelIndex));
    }

    /**
     * Implements the CueListListener interface.  Records the change
     * in our collection of submaster cell changes.
     * 
     * @param cueIndex
     * @param submasterIndex
     */
    public void submasterLevelChanged(final int cueIndex, final int submasterIndex) {
        submasterChanges.add(new Change(cueIndex, submasterIndex));
    }

    public int getNumberOfChannelChanges() {
        return channelChanges.size();
    }

    public int getNumberOfSubmasterChanges() {
        return submasterChanges.size();
    }

    public Change getChannelChange(final int index) {
        return channelChanges.get(index);
    }

    public int getChannelChangeIndex(final int index) {
        return getChannelChange(index).getIndex();
    }

    public int getChannelChangeCueIndex(final int index) {
        return getChannelChange(index).getCueIndex();
    }

    private Change getSubmasterChange(final int index) {
        return submasterChanges.get(index);
    }

    public int getSubmasterChangeIndex(final int index) {
        return getSubmasterChange(index).getIndex();
    }

    public int getSubmasterChangeCueIndex(final int index) {
        return getSubmasterChange(index).getCueIndex();
    }
}

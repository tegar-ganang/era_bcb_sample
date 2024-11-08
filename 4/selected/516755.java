package edu.georgetown.nnj.filterblocks.filters;

import edu.georgetown.nnj.data.NNJDataSource;
import edu.georgetown.nnj.events.NNJChangeEvent;
import static de.ifn_magdeburg.kazukazuj.util.KKJArrays.multiply;

/**This class inverts certain channels of data, for instance, only optical
 * channels, or certain analog channels.
 *
 * @author Kentaroh Takagaki
 */
public class NNJFilterInvert extends NNJAbstractFilter {

    public static String filterName = "Data inversion filter";

    @Override
    public String getFilterName() {
        return filterName;
    }

    @Override
    public String getFilterStateDescription() {
        return NULL_STRING;
    }

    /**This filter is turned on by default."*/
    public NNJFilterInvert(NNJDataSource source) {
        this(source, true);
    }

    public NNJFilterInvert(NNJDataSource source, boolean filterActive) {
        this.setFilterActive(filterActive);
        setNNJDataSource(source);
    }

    protected NNJFilterInvert() {
    }

    @Override
    public void setNNJDataSourceImpl() {
    }

    @Override
    public void stateChangedNNJImplCommonPre(NNJChangeEvent evt) {
    }

    @Override
    public void stateChangedNNJImplCommonPost(NNJChangeEvent evt) {
    }

    @Override
    public void stateChangedNNJImplAllChannels(NNJChangeEvent evt) {
    }

    @Override
    public void stateChangedNNJImplSomeChannels(NNJChangeEvent evt) {
    }

    @Override
    public void stateChangedNNJImplLayout(NNJChangeEvent evt) {
        if (inverted.length != getDataLayout().getChannelCount()) {
            inverted = new boolean[getDataLayout().getChannelCount()];
        }
    }

    @Override
    public void stateChangedNNJImplWindow(NNJChangeEvent evt) {
    }

    @Override
    public void stateChangedNNJImplMask(NNJChangeEvent evt) {
    }

    private boolean[] inverted = { true };

    public boolean getInversion(int det) {
        return inverted[det];
    }

    public boolean getInversionAll() {
        boolean tempRet = true;
        for (int k = 0; k < inverted.length; k++) {
            if (!inverted[k]) tempRet = false;
            break;
        }
        return tempRet;
    }

    public boolean getInversionOptical() {
        boolean tempRet = true;
        for (int k = 0; k < this.getDataLayout().getDetectorCount(); k++) {
            if (!inverted[k]) tempRet = false;
            break;
        }
        return tempRet;
    }

    public boolean getInversionNonOptical() {
        boolean tempRet = true;
        for (int k = this.getDataLayout().getDetectorCount(); k < this.getDataLayout().getChannelCount(); k++) {
            if (!inverted[k]) tempRet = false;
            break;
        }
        return tempRet;
    }

    public void setInvert(int det, boolean value) {
        if (inverted[det] != value) {
            inverted[det] = value;
            stateChangedNNJ(NNJChangeEvent.factorySomeChannels(det));
        }
    }

    public void setInvertAll(boolean value) {
        NNJChangeEvent tempEvt = NNJChangeEvent.factorySomeChannels();
        for (int det = 0; det < getDataLayout().getChannelCount(); det++) {
            if (inverted[det] != value) {
                inverted[det] = value;
                tempEvt.setChangedChannels(det);
            }
        }
        if (tempEvt.getChangedChannelsCount() > 0) {
            stateChangedNNJ(tempEvt);
        }
    }

    public void setInvertOptical(boolean value) {
        NNJChangeEvent tempEvt = NNJChangeEvent.factorySomeChannels();
        for (int det = 0; det < this.getDataLayout().getDetectorCount(); det++) {
            if (inverted[det] != value) {
                inverted[det] = value;
                tempEvt.setChangedChannels(det);
            }
        }
        if (tempEvt.getChangedChannelsCount() > 0) {
            stateChangedNNJ(tempEvt);
        }
    }

    public void setInvertNonOptical(boolean value) {
        NNJChangeEvent tempEvt = NNJChangeEvent.factorySomeChannels();
        for (int det = getDataLayout().getDetectorCount(); det < this.getDataLayout().getChannelCount(); det++) {
            if (inverted[det] != value) {
                inverted[det] = value;
                tempEvt.setChangedChannels(det);
            }
        }
        if (tempEvt.getChangedChannelsCount() > 0) {
            stateChangedNNJ(tempEvt);
        }
    }

    @Override
    public int readDataPointImpl(int det, int frame) {
        if (inverted[det]) {
            return -this.getNNJDataSource().readDataPoint(det, frame);
        } else {
            return this.getNNJDataSource().readDataPoint(det, frame);
        }
    }

    @Override
    public int[] readDataTraceSegmentImpl(int channel, int start, int end) {
        int[] tempret;
        if (inverted[channel]) {
            tempret = multiply(this.getNNJDataSource().readDataTraceSegment(channel, start, end), -1);
            return tempret;
        } else {
            return this.getNNJDataSource().readDataTraceSegment(channel, start, end);
        }
    }
}

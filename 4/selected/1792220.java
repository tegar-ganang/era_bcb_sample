package edu.georgetown.nnj.data;

import de.ifn_magdeburg.kazukazuj.K;
import edu.georgetown.nnj.events.NNJChangeEvent;
import edu.georgetown.nnj.events.NNJChangeHandlerImpl;
import edu.georgetown.nnj.events.NNJChangeListener;
import de.ifn_magdeburg.kazukazuj.util.KKJArrays;
import java.util.HashSet;

/**Default implementation basis for NNJDataSource.
 * <p>This abstract class implements all the default
 * data accessor methods to refer to readDataPoint; therefore when extending this
 * abstract class, only readDataPoint need be implemented to obtain a complete 
 * slate of data accessor methods. However, streamlined versions of the other
 * data accessor methods should also be implemented, for speed.<p>
 * This class also provides change handling functionality.
 * @author Kentaroh Takagaki
 * @version 0.4.0
 */
public abstract class NNJAbstractDataSource implements NNJDataSource {

    @Override
    public NNJAbstractDataSource clone() {
        throw new UnsupportedOperationException("DataSources cannot be cloned.");
    }

    @Override
    public int readDataPoint(int det, int fr) {
        if (this.getDataMask().isUnmasked(det)) {
            return readDataPointImpl(det, fr);
        } else {
            return 0;
        }
    }

    public abstract int readDataPointImpl(int det, int fr);

    /**This default implementation in NNJAbstractDataSource.java will call
     * readDataPoint().
     */
    public int[] readDataTraceSegmentImpl(int det, int start, int end) {
        int[] tempret = new int[end - start + 1];
        for (int k = 0; k < tempret.length; k++) {
            tempret[k] = readDataPointImpl(det, k + start);
        }
        return tempret;
    }

    public int[] readDataTraceSegmentImpl(int det, int start, int end, int df) {
        int[] tempret = new int[KKJArrays.decimatedFrameCount(end - start + 1, df)];
        for (int k = 0, dp = start; k < tempret.length; k++, dp += df) {
            tempret[k] = readDataPointImpl(det, dp);
        }
        return tempret;
    }

    /**{@inheritDoc}<p>
     * This default implementation in NNJAbstractDataSource.java will call
     * either readDataTraceImpl (if only data for windowed trace segment are
     * returned) or readDataTraceImplAll (if data for the whole trace are returned).
     * When inheriting, only those methods need be overwritten.
     */
    @Override
    public int[] readDataTrace(int det) {
        int[] tempret = null;
        if (this.getDataMask().isUnmasked(det)) {
            tempret = readDataTraceSegmentImpl(det, this.getDataWindow().getValue(), this.getDataWindow().getValue2());
        } else {
            tempret = new int[this.getDataWindow().getWindowFrameCount()];
        }
        return tempret;
    }

    /**{@inheritDoc}<p>
     * This default implementation in NNJAbstractDataSource.java will call
     * either readDataTraceDecimateImpl (if only data for windowed trace segment are
     * returned) or readDataTraceDecimateImplAll (if data for the whole trace are returned).
     * When inheriting, only those methods need be overwritten.
     */
    @Override
    public int[] readDataTrace(int det, int df) {
        int[] tempret;
        if (this.getDataMask().isUnmasked(det)) {
            tempret = readDataTraceSegmentImpl(det, this.getDataWindow().getValue(), this.getDataWindow().getValue2(), df);
        } else {
            tempret = new int[this.getDataWindow().getWindowFrameCount(df)];
        }
        return tempret;
    }

    /**{@inheritDoc}<p>
     * This default implementation in NNJAbstractDataSource.java will call
     * readDataTraceSegmentImpl().
     */
    @Override
    public int[] readDataTraceSegment(int det, int start, int end) {
        if (this.getDataMask().isUnmasked(det)) {
            return readDataTraceSegmentImpl(det, start, end);
        } else {
            return new int[end - start + 1];
        }
    }

    /**{@inheritDoc}<p>
     * This default implementation in NNJAbstractDataSource.java will call
     * readDataPoint().
     */
    @Override
    public int[] readDataTraceSegment(int det, int start, int end, int df) {
        if (df == 1) {
            return readDataTraceSegment(det, start, end);
        } else if (this.getDataMask().isUnmasked(det)) {
            return readDataTraceSegmentImpl(det, start, end, df);
        } else {
            return new int[KKJArrays.decimatedFrameCount(end - start + 1, df)];
        }
    }

    /**{@inheritDoc}<p>
     * This default final implementation in NNJAbstractDataSource.java will call
     * either readDataPoint, which returns 0 if detector is masked.
     * When inheriting, only those methods need be overwritten.
     */
    @Override
    public int[] readDataFrame(int fr) {
        int[] tempret = new int[getDataLayout().getChannelCount()];
        for (int k = 0; k < tempret.length; k++) {
            tempret[k] = this.readDataPoint(k, fr);
        }
        return tempret;
    }

    /**{@inheritDoc}<p>
     * This default implementation in NNJAbstractDataSource.java will call the following:
     * <ul><li>readDataDecimateImpl... if only data for unmasked detectors in the selected window are
     * returned)</li><li>readDataDecimateImplAllChannels... if data for all channels are returned
     * but only windowed segment are returned</li><li>readDataDecimateImplAllFrames... if data for all channels are returned
     * but only unmasked detectors are returned</li><li>readDataDecimateImplAllChannelsAllFrames... 
     * if data for all channels and all detectors are returned</li></ul>
     * When inheriting, only these methods need be overwritten.
     */
    @Override
    public int[][] readData(int decimationFactor) {
        int[][] tempret = new int[getDataLayout().getChannelCount()][];
        for (int k = 0; k < tempret.length; k++) {
            tempret[k] = this.readDataTrace(k, decimationFactor);
        }
        return tempret;
    }

    @Override
    public int[][] readData() {
        int[][] tempret = new int[getDataLayout().getChannelCount()][];
        for (int k = 0; k < tempret.length; k++) {
            tempret[k] = this.readDataTrace(k);
        }
        return tempret;
    }

    @Override
    public double readDataPointAbsolute(int det, int fr) {
        if (this.getDataMask().isUnmasked(det)) {
            return (double) readDataPointImpl(det, fr) / this.getAbsoluteGain();
        } else {
            return 0;
        }
    }

    @Override
    public double[] readDataTraceAbsolute(int det) {
        if (this.getDataMask().isUnmasked(det)) {
            double tempret[] = K.toDouble(readDataTraceSegmentImpl(det, getDataWindow().getValue(), getDataWindow().getValue2()));
            K.divideTo(tempret, getAbsoluteGain());
            return tempret;
        } else {
            return new double[this.getDataWindow().getWindowFrameCount()];
        }
    }

    @Override
    public double[] readDataTraceAbsolute(int det, int df) {
        if (this.getDataMask().isUnmasked(det)) {
            double tempret[] = K.toDouble(readDataTraceSegmentImpl(det, getDataWindow().getValue(), getDataWindow().getValue2(), df));
            K.divideTo(tempret, getAbsoluteGain());
            return tempret;
        } else {
            return new double[this.getDataWindow().getWindowFrameCount(df)];
        }
    }

    @Override
    public double[] readDataTraceSegmentAbsolute(int det, int start, int end) {
        if (this.getDataMask().isUnmasked(det)) {
            double tempret[] = K.toDouble(readDataTraceSegmentImpl(det, start, end));
            K.divideTo(tempret, getAbsoluteGain());
            return tempret;
        } else {
            return new double[this.getDataWindow().getWindowFrameCount()];
        }
    }

    @Override
    public double[] readDataTraceSegmentAbsolute(int det, int start, int end, int df) {
        if (this.getDataMask().isUnmasked(det)) {
            double tempret[] = K.toDouble(readDataTraceSegmentImpl(det, start, end, df));
            K.divideTo(tempret, getAbsoluteGain());
            return tempret;
        } else {
            return new double[this.getDataWindow().getWindowFrameCount(df)];
        }
    }

    @Override
    public double[] readDataFrameAbsolute(int frame) {
        double tempret[] = new double[this.getDataLayout().getChannelCount()];
        for (int k = 0; k < tempret.length; k++) {
            tempret[k] = this.readDataPointAbsolute(k, frame);
        }
        return tempret;
    }

    @Override
    public double[][] readDataAbsolute(int df) {
        double tempret[][] = new double[getDataLayout().getChannelCount()][];
        for (int k = 0; k < tempret.length; k++) {
            tempret[k] = readDataTraceAbsolute(k, df);
        }
        return tempret;
    }

    @Override
    public double[][] readDataAbsolute() {
        double tempret[][] = new double[this.getDataLayout().getChannelCount()][];
        for (int k = 0; k < tempret.length; k++) {
            tempret[k] = readDataTraceAbsolute(k);
        }
        return tempret;
    }

    @Override
    public abstract int getTotalFrameCount();

    @Override
    public abstract double getSamplingRate();

    @Override
    public final double frameToMs(int frame) {
        return ((double) frame) / this.getSamplingRate() * 1000d;
    }

    ;

    @Override
    public final int msToFrame(double ms) {
        int tempret = (int) Math.round(ms / 1000d * this.getSamplingRate());
        if (tempret < 0) {
            tempret = 0;
        } else if (tempret >= this.getTotalFrameCount()) {
            tempret = this.getTotalFrameCount() - 1;
        }
        return tempret;
    }

    ;

    private NNJChangeHandlerImpl changeHandler = new NNJChangeHandlerImpl();

    @Override
    public final void addChangeListener(NNJChangeListener l) {
        this.changeHandler.addChangeListener(l);
    }

    @Override
    public final HashSet<NNJChangeListener> getChangeListeners() {
        return this.changeHandler.getChangeListeners();
    }

    @Override
    public final void removeChangeListener(NNJChangeListener l) {
        this.changeHandler.removeChangeListener(l);
    }

    @Override
    public final void removeChangeListeners() {
        this.changeHandler.removeChangeListeners();
    }

    @Override
    public final void fireStateChanged(NNJChangeEvent event) {
        this.changeHandler.fireStateChanged(event);
    }

    @Override
    public final int getListenerCount() {
        return changeHandler.getListenerCount();
    }

    @Override
    public boolean isCompatible(NNJDataSource data) {
        return getDataLayout().isCompatible(data.getDataLayout()) && getTotalFrameCount() == data.getTotalFrameCount() && getSamplingRate() == data.getSamplingRate();
    }
}

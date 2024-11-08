package edu.georgetown.nnj.filterblocks.filters;

import edu.georgetown.nnj.data.NNJDataWindow;
import de.ifn_magdeburg.kazukazuj.K;
import edu.georgetown.nnj.data.NNJDataSource;
import edu.georgetown.nnj.events.NNJChangeEvent;
import java.util.HashSet;
import static de.ifn_magdeburg.kazukazuj.util.KKJArrays.*;

/**This filter provides the minimum and maximum of a segment,
 * and also provides functions which give the segment data scaled
 * variably (where each detector scaled such that all values fit
 * within the specified data range).<p>
 * Since this class is based off of NNJFilterPassthru and always plays
 * a passive role in the data stream, the "getFilterActive" is always false.
 * @author Kentaroh Takagaki
 * @version 0.4.2
 */
public final class NNJFilterMinMax extends NNJFilterPassthru {

    public static String filterName = "Min/Max extraction filter (NNJFilterMinMax)";

    @Override
    public String getFilterName() {
        return filterName;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public NNJFilterMinMax(NNJDataSource source) {
        super(source);
    }

    public NNJFilterMinMax(NNJDataSource source, boolean filterActive) {
        super(source, filterActive);
    }

    protected NNJFilterMinMax() {
        super();
    }

    @Override
    public void setNNJDataSourceImpl() {
        flushBuffer();
    }

    @Override
    public void stateChangedNNJImplCommonPre(NNJChangeEvent evt) {
    }

    @Override
    public void stateChangedNNJImplCommonPost(NNJChangeEvent evt) {
    }

    @Override
    public void stateChangedNNJImplAllChannels(NNJChangeEvent evt) {
        flushBuffer();
    }

    @Override
    public void stateChangedNNJImplSomeChannels(NNJChangeEvent evt) {
        HashSet<Integer> ch = evt.getChangedChannels();
        for (int k : ch) {
            flushBuffer(k);
        }
    }

    @Override
    public void stateChangedNNJImplLayout(NNJChangeEvent evt) {
        flushBuffer();
    }

    @Override
    public void stateChangedNNJImplWindow(NNJChangeEvent evt) {
    }

    @Override
    public void stateChangedNNJImplMask(NNJChangeEvent evt) {
    }

    private int decimationFactor = 1;

    public void setDecimationFactor(int decimationFactor) {
        assert decimationFactor > 0 : "Decimation factor must be positive!";
        if (this.decimationFactor != decimationFactor) {
            this.decimationFactor = decimationFactor;
            this.flushBuffer();
        }
    }

    public int getDecimationFactor() {
        return this.decimationFactor;
    }

    private int[][] buffer;

    public void flushBuffer() {
        buffer = new int[getDataLayout().getChannelCount()][];
    }

    public void flushBuffer(int channel) {
        buffer[channel] = null;
    }

    private NNJDataWindow internalWindow = new NNJDataWindow(this.getDataWindow());

    public void checkCurrentWindow() {
        if (!internalWindow.equals(this.getDataWindow())) {
            internalWindow = new NNJDataWindow(this.getDataWindow());
            flushBuffer();
        }
    }

    public int[] readDataTraceMinMax(int channel) {
        assert buffer != null : "buffer initialization missing somewhere!";
        int decimationFactorTemp = this.getDecimationFactor();
        checkCurrentWindow();
        if (buffer[channel] == null) {
            if (decimationFactorTemp == 1) {
                buffer[channel] = K.minMax(this.readDataTrace(channel));
            } else {
                buffer[channel] = K.minMax(this.readDataTrace(channel, decimationFactorTemp));
            }
        }
        return buffer[channel];
    }

    public int[][] readDataMinMax() {
        int[][] tempRet = new int[this.getDataLayout().getChannelCount()][];
        for (int k = 0; k < tempRet.length; k++) {
            tempRet[k] = readDataTraceMinMax(k);
        }
        return tempRet;
    }

    public int readDataTraceMinMaxInterval(int channel) {
        int[] temp = this.readDataTraceMinMax(channel);
        return temp[1] - temp[0];
    }

    public int[] readDataMinMaxInterval() {
        int[] tempRet = new int[this.getDataLayout().getChannelCount()];
        for (int k = 0; k < tempRet.length; k++) {
            tempRet[k] = readDataTraceMinMaxInterval(k);
        }
        return tempRet;
    }

    public double[] readDataTraceMinMaxAbsolute(int channel) {
        double[] tempret = K.toDouble(this.readDataTraceMinMax(channel));
        divideInSitu(tempret, this.getAbsoluteGain());
        return tempret;
    }

    public double[][] readDataMinMaxAbsolute() {
        double[][] tempRet = new double[this.getDataLayout().getChannelCount()][2];
        for (int k = 0; k < tempRet.length; k++) {
            tempRet[k] = readDataTraceMinMaxAbsolute(k);
        }
        return tempRet;
    }

    public double readDataTraceMinMaxAbsoluteInterval(int channel) {
        double tempret = this.readDataTraceMinMaxInterval(channel);
        tempret /= this.getAbsoluteGain();
        return tempret;
    }

    public double[] readDataMinMaxAbsoluteInterval() {
        double[] tempRet = new double[this.getDataLayout().getChannelCount()];
        for (int k = 0; k < tempRet.length; k++) {
            tempRet[k] = readDataTraceMinMaxAbsoluteInterval(k);
        }
        return tempRet;
    }

    /**Reads the data 
     * and returns it variably scaled  within the
     * trace segment specified by the stream NNJDataWindow.
     * @param channel channel specification
     * @param frame frame specification
     * @return scaled data trace segment
     */
    public double readDataPointVariable(int channel, int frame) {
        if (!getDataWindow().isWithinWindowRange(frame)) {
            System.out.println("CAUTION! accessing frame " + frame + ", outside variable scaling window!");
        }
        return readDataPointVariableImpl(channel, frame);
    }

    private double readDataPointVariableImpl(int channel, int frame) {
        int[] minMax = readDataTraceMinMax(channel);
        double tempret = (double) readDataPoint(channel, frame);
        tempret -= minMax[0];
        tempret /= (double) (minMax[1] - minMax[0]);
        return tempret;
    }

    /**Reads the data in the trace segment specified by the stream NNJDataWindow,
     * and returns it variably scaled from [0,1].
     * @param channel channel specification
     * @return scaled data trace segment
     */
    public double[] readDataTraceVariable(int channel) {
        int[] minMax = readDataTraceMinMax(channel);
        double[] tempret = K.toDouble(readDataTrace(channel));
        K.addTo(tempret, (double) (-minMax[0]));
        K.multiplyTo(tempret, 1d / (double) (minMax[1] - minMax[0]));
        return tempret;
    }

    /**Reads the data in the trace segment specified by the stream NNJDataWindow,
     * pseudo-decimated by factor, and returns it variably scaled.
     * @param channel channel specification
     * @param decimationFactor pseudo-decimation factor
     * @return scaled data trace segment
     */
    public double[] readDataTraceVariable(int channel, int decimationFactor) {
        int[] minMax = readDataTraceMinMax(channel);
        double[] tempret = K.toDouble(readDataTrace(channel, decimationFactor));
        K.addTo(tempret, (double) (-minMax[0]));
        K.multiplyTo(tempret, 1d / (double) (minMax[1] - minMax[0]));
        return tempret;
    }

    /**Reads the data frame specified
     * and returns it variably scaled within the
     * trace segment specified by the stream NNJDataWindow.
     * @param frame frame specification
     * @return scaled data trace segment
     */
    public double[] readDataFrameVariable(int frame) {
        if (!getDataWindow().isWithinWindowRange(frame)) {
            System.out.println("CAUTION! accessing frame " + frame + ", outside variable scaling window!");
        }
        double[] tempret = new double[getDataLayout().getChannelCount()];
        for (int k = 0; k < tempret.length; k++) {
            tempret[k] = readDataPointVariableImpl(k, frame);
        }
        return tempret;
    }

    /**Reads the data in the segment specified by the stream NNJDataWindow,
     * and returns it variably scaled.
     * @return scaled data segment
     */
    public double[][] readDataVariable() {
        double[][] tempret = new double[getDataLayout().getChannelCount()][];
        for (int k = 0; k < tempret.length; k++) {
            tempret[k] = readDataTraceVariable(k);
        }
        return tempret;
    }

    /**Reads the data in the segment specified by the stream NNJDataWindow,
     * pseudo-decimated, and returns it variably scaled.
     * @param decimationFactor pseudo-decimation factor
     * @return scaled data segment
     */
    public double[][] readDataVariable(int decimationFactor) {
        double[][] tempret = new double[getDataLayout().getChannelCount()][];
        for (int k = 0; k < tempret.length; k++) {
            tempret[k] = readDataTraceVariable(k, decimationFactor);
        }
        return tempret;
    }
}

package edu.georgetown.nnj.filterblocks.filters;

import de.ifn_magdeburg.kazukazuj.K;
import edu.georgetown.nnj.events.NNJChangeEvent;
import edu.georgetown.nnj.data.layout.NNJDataLayout;
import edu.georgetown.nnj.data.NNJDataSource;
import edu.georgetown.nnj.data.NNJDataWindow;
import java.util.HashMap;

/**
 * This filter block provides buffering function. 
 * By inserting the block into the filtering chain, 
 * intermediate results will be buffered
 * at this stage of calculation within the chain.
 * Therefore, when downstream blocks change,
 * the filter chain will only need to be calculated
 * from this intermediate point downwards.<p>
 *
 * Alternatively, it can be used within a class, if that
 * class includes a high-cost calculation, and results
 * should be buffered by default.
 * 
 * The buff is reset by calling 
 * void stateChanged(NNJChangeEvent evt), with the 
 * NNJChangeEvent properties determining which buffer
 * elements are refreshed. After refreshing, the
 * filter block will send along the change event
 * to any components which inherit it.<p>
 * 
 * All of the data return methods in this class return defensive copies
 * of the buff, therefore, the original buff cannot be tampered with.<p>
 * 
 * //TODO ??Implement background buff fill-in, especially of traces
 * //TODO Fix buffer to not use Hashmap again, Ingeger.MIN_VALUE or so.
 * after only a trace segment has been called. Maybe just trigger from
 * downstream by empty calls, if the downstream source anticipates future
 * need?
 * 
 * 
 * @author Kentaroh Takagaki
 * @version 0.4.1
 */
public final class NNJFilterBuffer extends NNJAbstractFilter {

    static String filterName = "Buffer filter block (NNJFilterBuffer)";

    @Override
    public String getFilterName() {
        return filterName;
    }

    @Override
    public String getFilterStateDescription() {
        return " ";
    }

    protected NNJFilterBuffer() {
    }

    /**This filter is turned on by default."*/
    public NNJFilterBuffer(NNJDataSource source) {
        this(source, true);
    }

    public NNJFilterBuffer(NNJDataSource source, boolean filterActive) {
        this.setFilterActive(filterActive);
        setNNJDataSource(source);
    }

    @Override
    public void setNNJDataSourceImpl() {
        this.flushBuffAll();
    }

    @Override
    public void stateChangedNNJImplCommonPre(NNJChangeEvent evt) {
    }

    @Override
    public void stateChangedNNJImplCommonPost(NNJChangeEvent evt) {
    }

    @Override
    public void stateChangedNNJImplAllChannels(NNJChangeEvent evt) {
        this.flushBuffData();
    }

    @Override
    public void stateChangedNNJImplSomeChannels(NNJChangeEvent evt) {
        for (int ch : evt.getChangedChannels()) {
            this.flushBuffDataTrace(ch);
        }
    }

    @Override
    public void stateChangedNNJImplLayout(NNJChangeEvent evt) {
        this.flushBuffLayout();
    }

    @Override
    public void stateChangedNNJImplWindow(NNJChangeEvent evt) {
        this.flushBuffWindow();
    }

    @Override
    public void stateChangedNNJImplMask(NNJChangeEvent evt) {
    }

    /**The buffered value of getDataWindow().
     * Must never be null.*/
    private NNJDataWindow windowBuff;

    /**The buffered value of getDataLayout().
     * Must never be null.*/
    private NNJDataLayout layoutBuff;

    /**This method will only provide a buffered link, not a separate
     * cloned object.*/
    @Override
    public NNJDataLayout getDataLayout() {
        return layoutBuff;
    }

    /**This method will only provide a buffered link, not a separate
     * cloned object.*/
    @Override
    public NNJDataWindow getDataWindow() {
        return windowBuff;
    }

    private HashMap<Integer, Integer>[] pointBuff;

    /**Buffer range for each trace--length is strictly two.*/
    private int[][] traceBuffRange2;

    private int[][] traceBuff;

    /**Converts absolute frame count within whole data array to buffer array count.
     * @param fr frame number
     * @return corresponding buffer array count
     */
    private int frToBc(int ch, int fr) {
        return fr - traceBuffRange2[ch][0];
    }

    /**Empties the buffer for a single data trace. Is implemented
     * by setting isBuffered to false; value is not actively 
     * erased from buffer.
     * @param ch channel specification
     */
    public void flushBuffDataTrace(int ch) {
        traceBuffRange2[ch] = null;
        traceBuff[ch] = null;
        pointBuff[ch] = null;
    }

    /**Empties the buffer. Is implemented
     * by setting isBuffered to false; value is not actively 
     * erased from buffer.
     */
    public void flushBuffData() {
        this.pointBuff = new HashMap[layoutBuff.getChannelCount()];
        traceBuffRange2 = new int[layoutBuff.getChannelCount()][];
        traceBuff = new int[layoutBuff.getChannelCount()][];
    }

    /**<b><i>Renews</i></b> the format buffer.
     */
    public void flushBuffLayout() {
        layoutBuff = this.getNNJDataSource().getDataLayout();
    }

    /**<b><i>Renews</i></b> the window buffer.
     */
    public void flushBuffWindow() {
        windowBuff = this.getNNJDataSource().getDataWindow();
    }

    /**<b><i>Renews</i></b>/empties all buffers.
     */
    public void flushBuffAll() {
        flushBuffLayout();
        flushBuffWindow();
        flushBuffData();
    }

    @Override
    public int readDataPointImpl(int ch, int fr) {
        if (fr < 0 || fr > this.getDataWindow().getMaximum()) {
            throw new IllegalArgumentException("frame (" + Integer.toString(fr) + ") must be >=0 and <= maximum frame (" + Integer.toString(this.getDataWindow().getMaximum()) + ")!");
        }
        if (traceBuffRange2[ch] != null && traceBuffRange2[ch][0] <= fr && fr <= traceBuffRange2[ch][1]) {
            return traceBuff[ch][frToBc(ch, fr)];
        } else if (pointBuff[ch] != null) {
            return readDataPointImplFromPointBuff(ch, fr);
        } else {
            int tempret = this.getNNJDataSource().readDataPoint(ch, fr);
            pointBuff[ch] = new HashMap<Integer, Integer>();
            pointBuff[ch].put(fr, tempret);
            return tempret;
        }
    }

    private int readDataPointImplFromPointBuff(int ch, int fr) {
        if (pointBuff[ch].containsKey(fr)) {
            return pointBuff[ch].get(fr);
        } else {
            int tempret = this.getNNJDataSource().readDataPoint(ch, fr);
            pointBuff[ch].put(fr, tempret);
            return tempret;
        }
    }

    @Override
    public int[] readDataTraceSegmentImpl(int ch, int start, int end) {
        if (start < 0) {
            throw new IllegalArgumentException("start (" + Integer.toString(start) + ") is invalid!");
        } else if (end < start) {
            throw new IllegalArgumentException("end (" + Integer.toString(end) + ") must be >= start (" + Integer.toString(start) + ")!");
        } else if (end > this.getDataWindow().getMaximum()) {
            throw new IllegalArgumentException("end (" + Integer.toString(end) + ") must be <= maximum frame (" + Integer.toString(this.getDataWindow().getMaximum()) + ")!");
        }
        int[] tempret;
        if (start == end) {
            tempret = new int[] { readDataPointImpl(ch, start) };
        } else if (traceBuffRange2[ch] == null) {
            this.traceBuffRange2[ch] = new int[2];
            this.traceBuffRange2[ch][0] = start;
            this.traceBuffRange2[ch][1] = end;
            this.traceBuff[ch] = getNNJDataSource().readDataTraceSegment(ch, start, end);
            tempret = K.copy(traceBuff[ch]);
        } else if (traceBuffRange2[ch][0] == start && traceBuffRange2[ch][1] == end) {
            tempret = K.copy(traceBuff[ch]);
        } else if (traceBuffRange2[ch][0] > start || traceBuffRange2[ch][1] < end) {
            int tempBufferStart = K.min(traceBuffRange2[ch][0], start);
            int tempBufferEnd = K.max(traceBuffRange2[ch][1], end);
            int[] newTraceBuff = new int[tempBufferEnd - tempBufferStart + 1];
            if (start < traceBuffRange2[ch][0]) {
                K.putTo(newTraceBuff, K.r(0, traceBuffRange2[ch][0] - start - 1), getNNJDataSource().readDataTraceSegment(ch, start, traceBuffRange2[ch][0] - 1));
            }
            if (end > traceBuffRange2[ch][1]) {
                K.putTo(newTraceBuff, K.r(traceBuffRange2[ch][1] - tempBufferStart + 1, -1), getNNJDataSource().readDataTraceSegment(ch, traceBuffRange2[ch][1] + 1, end));
            }
            K.putTo(newTraceBuff, K.r(traceBuffRange2[ch][0] - tempBufferStart, traceBuffRange2[ch][1] - tempBufferStart), traceBuff[ch]);
            traceBuff[ch] = newTraceBuff;
            traceBuffRange2[ch][0] = tempBufferStart;
            traceBuffRange2[ch][1] = tempBufferEnd;
            tempret = K.copy(traceBuff[ch], frToBc(ch, start), frToBc(ch, end));
        } else {
            tempret = K.copy(traceBuff[ch], frToBc(ch, start), frToBc(ch, end));
        }
        return tempret;
    }
}

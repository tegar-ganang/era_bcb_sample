package edu.georgetown.nnj.filterblocks.filters;

import edu.georgetown.nnj.data.NNJAbstractDataSource;
import edu.georgetown.nnj.events.NNJChangeEvent;
import static edu.georgetown.nnj.events.NNJChangeEvent.*;
import edu.georgetown.nnj.data.layout.NNJDataLayout;
import edu.georgetown.nnj.data.NNJDataMask;
import edu.georgetown.nnj.data.NNJDataSource;
import edu.georgetown.nnj.data.NNJDataSourceConsumer;
import edu.georgetown.nnj.data.NNJDataSourceNull;
import edu.georgetown.nnj.data.NNJDataWindow;
import java.util.ArrayList;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/** This abstract class provides the framework for implementing filter elements
 * in the data chain. In addition to overriding methods from NNJDataSource
 * (minimum requirement is to override readDataPoint(channel, frame),
 * the user must also implement stateChangedNNJ implementation methods.
 * This is enforced so that the programmer does not forget to handle changes.<p>
 * 
 * This abstract class also includes default implementations for 
 * NNJFilterMaskSuperimposer (for filters which add more masked detectors to
 * the upstream mask), and NNJFilterWindowChangeListener (for filters, esp.
 * displays) with outputs which depend upon the values of the data window.
 *
 * @author Kentaroh Takagaki
 * @version 0.4.0
 */
public abstract class NNJAbstractFilter extends NNJAbstractDataSource implements NNJFilter {

    @Override
    public abstract String getFilterName();

    protected static String NULL_STRING = "";

    @Override
    public abstract String getFilterStateDescription();

    @Override
    public String toString() {
        if (getFilterActive()) {
            return this.getFilterName() + " (ON)... " + this.getFilterStateDescription();
        } else {
            return this.getFilterName() + " (OFF)... " + this.getFilterStateDescription();
        }
    }

    ;

    @Override
    public ArrayList<String> getStreamDescription() {
        ArrayList<String> tempret;
        if (NNJFilter.class.isInstance(this.getNNJDataSource())) {
            tempret = ((NNJFilter) this.getNNJDataSource()).getStreamDescription();
        } else {
            tempret = new ArrayList<String>();
        }
        tempret.add(toString());
        return tempret;
    }

    protected NNJDataSource source = NNJDataSourceNull.factory();

    protected NNJDataWindow window;

    /**Constructs a filter; argument source is asserted to be non-null
     * @param source source for the data fed to this filter
     */
    public NNJAbstractFilter(NNJDataSource source) {
        this(source, true);
    }

    public NNJAbstractFilter(NNJDataSource source, boolean filterActive) {
        super();
        isMaskSuperimposer = NNJFilterMaskSuperimposer.class.isInstance(this);
        setNNJDataSource(source);
        this.setFilterActive(filterActive);
        window = new NNJDataWindow(this);
    }

    /**The standard constructor should not be called explicitly.*/
    protected NNJAbstractFilter() {
    }

    private boolean filterActive = true;

    @Override
    public boolean getFilterActive() {
        return this.filterActive;
    }

    @Override
    public void setFilterActive(boolean filterActive) {
        this.filterActive = filterActive;
        this.fireStateChanged(NNJChangeEvent.factoryAllChannels());
    }

    @Override
    public abstract int readDataPointImpl(int det, int fr);

    /**{@inheritDoc}<p>
    *
    * In NNJAbstractFilter, the call is answered by NNJAbstractDataSource only if getFilterActive()
    * is true. Otherwise, the call is forwarded to the upstream source object.
    */
    @Override
    public final int readDataPoint(int det, int fr) {
        if (getFilterActive()) {
            return super.readDataPoint(det, fr);
        } else {
            return this.getNNJDataSource().readDataPoint(det, fr);
        }
    }

    /**{@inheritDoc}<p>
    *
    * In NNJAbstractFilter, the call is answered by NNJAbstractDataSource only if getFilterActive()
    * is true. Otherwise, the call is forwarded to the upstream source object.
    */
    @Override
    public final int[] readDataTrace(int det) {
        if (getFilterActive()) {
            return super.readDataTrace(det);
        } else {
            return getNNJDataSource().readDataTrace(det);
        }
    }

    /**{@inheritDoc}<p>
    *
    * In NNJAbstractFilter, the call is answered by NNJAbstractDataSource only if getFilterActive()
    * is true. Otherwise, the call is forwarded to the upstream source object.
    */
    @Override
    public final int[] readDataTrace(int det, int df) {
        if (getFilterActive()) {
            return super.readDataTrace(det, df);
        } else {
            return getNNJDataSource().readDataTrace(det, df);
        }
    }

    /**{@inheritDoc}<p>
    *
    * In NNJAbstractFilter, the call is answered by NNJAbstractDataSource only if getFilterActive()
    * is true. Otherwise, the call is forwarded to the upstream source object.
    */
    @Override
    public final int[] readDataTraceSegment(int det, int start, int end) {
        if (getFilterActive()) {
            return super.readDataTraceSegment(det, start, end);
        } else {
            return getNNJDataSource().readDataTraceSegment(det, start, end);
        }
    }

    /**{@inheritDoc}<p>
    *
    * In NNJAbstractFilter, the call is answered by NNJAbstractDataSource only if getFilterActive()
    * is true. Otherwise, the call is forwarded to the upstream source object.
    */
    @Override
    public final int[] readDataTraceSegment(int det, int start, int end, int df) {
        if (getFilterActive()) {
            return super.readDataTraceSegment(det, start, end, df);
        } else {
            return getNNJDataSource().readDataTraceSegment(det, start, end, df);
        }
    }

    /**{@inheritDoc}<p>
    *
    * In NNJAbstractFilter, the call is answered by NNJAbstractDataSource only if getFilterActive()
    * is true. Otherwise, the call is forwarded to the upstream source object.
    */
    @Override
    public final int[] readDataFrame(int frame) {
        if (getFilterActive()) {
            return super.readDataFrame(frame);
        } else {
            return getNNJDataSource().readDataFrame(frame);
        }
    }

    /**{@inheritDoc}<p>
    *
    * In NNJAbstractFilter, the call is answered by NNJAbstractDataSource only if getFilterActive()
    * is true. Otherwise, the call is forwarded to the upstream source object.
    */
    @Override
    public final int[][] readData() {
        if (getFilterActive()) {
            return super.readData();
        } else {
            return getNNJDataSource().readData();
        }
    }

    /**{@inheritDoc}<p>
    *
    * In NNJAbstractFilter, the call is answered by NNJAbstractDataSource only if getFilterActive()
    * is true. Otherwise, the call is forwarded to the upstream source object.
    */
    @Override
    public final int[][] readData(int df) {
        if (getFilterActive()) {
            return super.readData(df);
        } else {
            return getNNJDataSource().readData(df);
        }
    }

    /**{@inheritDoc}<p>
    *
    * In NNJAbstractFilter, the call is answered by NNJAbstractDataSource only if getFilterActive()
    * is true. Otherwise, the call is forwarded to the upstream source object.
    */
    @Override
    public final double readDataPointAbsolute(int det, int fr) {
        if (getFilterActive()) {
            return super.readDataPointAbsolute(det, fr);
        } else {
            return getNNJDataSource().readDataPointAbsolute(det, fr);
        }
    }

    /**{@inheritDoc}<p>
    *
    * In NNJAbstractFilter, the call is answered by NNJAbstractDataSource only if getFilterActive()
    * is true. Otherwise, the call is forwarded to the upstream source object.
    */
    @Override
    public final double[] readDataTraceAbsolute(int det) {
        if (getFilterActive()) {
            return super.readDataTraceAbsolute(det);
        } else {
            return getNNJDataSource().readDataTraceAbsolute(det);
        }
    }

    /**{@inheritDoc}<p>
    *
    * In NNJAbstractFilter, the call is answered by NNJAbstractDataSource only if getFilterActive()
    * is true. Otherwise, the call is forwarded to the upstream source object.
    */
    @Override
    public final double[] readDataTraceAbsolute(int det, int df) {
        if (getFilterActive()) {
            return super.readDataTraceAbsolute(det, df);
        } else {
            return getNNJDataSource().readDataTraceAbsolute(det, df);
        }
    }

    /**{@inheritDoc}<p>
    *
    * In NNJAbstractFilter, the call is answered by NNJAbstractDataSource only if getFilterActive()
    * is true. Otherwise, the call is forwarded to the upstream source object.
    */
    @Override
    public final double[] readDataTraceSegmentAbsolute(int det, int start, int end) {
        if (getFilterActive()) {
            return super.readDataTraceSegmentAbsolute(det, start, end);
        } else {
            return getNNJDataSource().readDataTraceSegmentAbsolute(det, start, end);
        }
    }

    /**{@inheritDoc}<p>
    *
    * In NNJAbstractFilter, the call is answered by NNJAbstractDataSource only if getFilterActive()
    * is true. Otherwise, the call is forwarded to the upstream source object.
    */
    @Override
    public final double[] readDataTraceSegmentAbsolute(int det, int start, int end, int df) {
        if (getFilterActive()) {
            return super.readDataTraceSegmentAbsolute(det, start, end, df);
        } else {
            return getNNJDataSource().readDataTraceSegmentAbsolute(det, start, end, df);
        }
    }

    /**{@inheritDoc}<p>
    *
    * In NNJAbstractFilter, the call is answered by NNJAbstractDataSource only if getFilterActive()
    * is true. Otherwise, the call is forwarded to the upstream source object.
    */
    @Override
    public final double[] readDataFrameAbsolute(int frame) {
        if (getFilterActive()) {
            return super.readDataFrameAbsolute(frame);
        } else {
            return getNNJDataSource().readDataFrameAbsolute(frame);
        }
    }

    /**{@inheritDoc}<p>
    *
    * In NNJAbstractFilter, the call is answered by NNJAbstractDataSource only if getFilterActive()
    * is true. Otherwise, the call is forwarded to the upstream source object.
    */
    @Override
    public final double[][] readDataAbsolute() {
        if (getFilterActive()) {
            return super.readDataAbsolute();
        } else {
            return getNNJDataSource().readDataAbsolute();
        }
    }

    /**{@inheritDoc}<p>
    *
    * In NNJAbstractFilter, the call is answered by NNJAbstractDataSource only if getFilterActive()
    * is true. Otherwise, the call is forwarded to the upstream source object.
    */
    @Override
    public final double[][] readDataAbsolute(int df) {
        if (getFilterActive()) {
            return super.readDataAbsolute(df);
        } else {
            return getNNJDataSource().readDataAbsolute(df);
        }
    }

    @Override
    public double getAbsoluteGain() {
        return getNNJDataSource().getAbsoluteGain();
    }

    @Override
    public double getSamplingRate() {
        return getNNJDataSource().getSamplingRate();
    }

    @Override
    public String getAbsoluteUnit() {
        return getNNJDataSource().getAbsoluteUnit();
    }

    @Override
    public int getDataExtraBits() {
        return getNNJDataSource().getDataExtraBits();
    }

    @Override
    public int getTotalFrameCount() {
        return getNNJDataSource().getTotalFrameCount();
    }

    /**{@inheritDoc}<p>
     * Default implementation-- simple passthrough to source.*/
    @Override
    public NNJDataLayout getDataLayout() {
        return getNNJDataSource().getDataLayout();
    }

    /**{@inheritDoc}<p>
     * Default implementation-- simple passthrough to source.*/
    @Override
    public NNJDataWindow getDataWindow() {
        return getNNJDataSource().getDataWindow();
    }

    @Override
    public final NNJDataSource getNNJDataSource() {
        return this.source;
    }

    /**This implementation does the following:-
     * <ol><li>sets the source, including handling NNJChangeListener registration.</li>
        <li>runs setNNJDataSourceImpl()</li>
        <li>triggers a stateChangedNNJ(NNJChangeEvent.ALL)</li></ol>
     * @param source
     */
    @Override
    public final void setNNJDataSource(NNJDataSource source) {
        if (this.source == null) {
            throw new NullPointerException("setNNJDataSource was called with null input. " + "This is not allowed--consider sending NNJDataSourceNull.INSTANCE.");
        } else {
            this.source.removeChangeListener(this);
        }
        this.source = source;
        this.source.addChangeListener(this);
        setNNJDataSourceImpl();
        stateChangedNNJ(NNJChangeEvent.factoryAll());
    }

    /**This should provide the filter specific steps to be taken after changing
     * the <code>NNJDataSource source</code>. A call to 
     * {@link edu.georgetown.nnj.events.NNJChangeListener#stateChangedNNJ}
     * is automatically provided after this method is called,
     * and is not necessary.
     */
    public abstract void setNNJDataSourceImpl();

    @Override
    public final void destroyDataStreamBranch() {
        if (this.source != null) {
            if (this.source.getListenerCount() > 1) {
                this.source.removeChangeListener(this);
                this.source = null;
            } else if (!(this.source instanceof NNJDataSourceConsumer) || ((NNJDataSourceConsumer) this.source).getNNJDataSource() == null || ((NNJDataSourceConsumer) this.source).getNNJDataSource() == NNJDataSourceNull.factory()) {
                this.source.removeChangeListener(this);
                this.source = null;
            } else {
                if (this.source instanceof NNJDataSourceConsumer) {
                    ((NNJDataSourceConsumer) this.source).destroyDataStreamBranch();
                }
                this.source.removeChangeListener(this);
                this.removeChangeListeners();
                this.source = null;
            }
        }
    }

    /**This first updates itself by calling:
     * <nl><li>stateChangedNNJImplCommonPre</li><li>individual change
     * events</li><li>stateChangedNNJImplCommonPost</li></nl>
     * Then, it triggers fireStateChanged(), to notify any downstream elements.
     * @param evt
     */
    @Override
    public final void stateChangedNNJ(NNJChangeEvent evt) {
        stateChangedNNJImplCommonPre(evt);
        switch(evt.getType()) {
            case ALL:
                stateChangedNNJImplAll(evt);
                break;
            case ALL_CHANNELS:
                stateChangedNNJImplAllChannels(evt);
                break;
            case SOME_CHANNELS:
                stateChangedNNJImplSomeChannels(evt);
                break;
            case LAYOUT:
                stateChangedNNJImplLayout(evt);
                break;
            case WINDOW:
                stateChangedNNJImplWindow(evt);
                break;
            case MASK:
                stateChangedNNJImplMask(evt);
                break;
        }
        stateChangedNNJImplCommonPost(evt);
        fireStateChanged(evt);
    }

    /**This code is called before the individual state change event is handled.*/
    public abstract void stateChangedNNJImplCommonPre(NNJChangeEvent evt);

    /**This code is called after the individual state change event is handled,
     * but before fireStateChanged() is called.*/
    public abstract void stateChangedNNJImplCommonPost(NNJChangeEvent evt);

    /**Calls this.stateChangedNNJImplAllChannels, ...Window, ...Mask.*/
    public final void stateChangedNNJImplAll(NNJChangeEvent evt) {
        this.stateChangedNNJImplLayout(evt);
        this.stateChangedNNJImplWindow(evt);
        this.stateChangedNNJImplMask(evt);
        this.stateChangedNNJImplAllChannels(evt);
    }

    public abstract void stateChangedNNJImplAllChannels(NNJChangeEvent evt);

    public abstract void stateChangedNNJImplSomeChannels(NNJChangeEvent evt);

    /**The following does not need to take care of deregistering/registering
     * this class as a listener for the new NNJDataWindow. If this class implements
     * NNJFilterWindowChangeListener, stateChangedNNJ
     * will take care of this as necessary, before calling this method.<p>
     * This method is called only when an NNJDataWindow object changes.
     * Do not confuse this with windowContentChange() which is automatically called
     * when the values of the NNJDataWindow change.
     * @param evt
     */
    public abstract void stateChangedNNJImplWindow(NNJChangeEvent evt);

    public abstract void stateChangedNNJImplMask(NNJChangeEvent evt);

    public abstract void stateChangedNNJImplLayout(NNJChangeEvent evt);

    private NNJDataMask maskBuff;

    private boolean isMaskSuperimposer = NNJFilterMaskSuperimposer.class.isInstance(this);

    private NNJDataMask superMask = new NNJDataMask();

    /**Final common implementation for NNJFilterMaskSuperimposer&#46;superMaskFlush()
     * which is defined in NNJAbstractFilter. However,
     * NNJAbstractFilter does not implement NNJFilterMaskSuperimposer, since
     * implementing this interface is the mechanism by which
     * NNJAbstractFilter&#46;getDataMask() knows whether to trigger mask superimposing
     * code.
     * @see NNJFilterMaskSuperimposer#superMaskFlush(boolean)
     * @param triggerChangeEvent
     */
    public final void superMaskFlush(boolean triggerChangeEvent) {
        superMask.setUnmaskAll();
        maskBuff = null;
        if (triggerChangeEvent) {
            stateChangedNNJ(NNJChangeEvent.factoryMask());
        }
    }

    /**Final common implementation for NNJFilterMaskSuperimposer&#46;superMaskFlush()
     * which is defined in NNJAbstractFilter. Can be called from classes which
     * do not implement NNJFilterMaskSuperimposer, but is in this case meaningless.
     * @see NNJFilterMaskSuperimposer#superMaskSetMask(boolean)
     */
    public final void superMaskSetMask(int det, boolean triggerChangeEvent) {
        superMask.setMask(det);
        maskBuff = null;
        if (triggerChangeEvent) {
            stateChangedNNJ(NNJChangeEvent.factoryMask());
        }
    }

    public final void superMaskEdgeChannels(int ring) {
        superMaskEdgeChannels(ring, true);
    }

    /**  If the class implements the interface {@link NNJFilterMaskSuperimposer},
     * then adds all edge channels to the superimposing mask. Otherwise, does nothing.
     * Default final implementation of NNJFilterMaskSuperimposer.superMaskEdgeChannels.
     *
    * @see NNJFilterMaskSuperimposer
    * @author Kenta
    */
    public final void superMaskEdgeChannels(int ring, boolean triggerChangeEvent) {
        if (isMaskSuperimposer) {
            maskBuff = null;
            NNJDataLayout tempLayout = this.getDataLayout();
            for (int ch = 0; ch < tempLayout.getChannelCount(); ch++) {
                if (tempLayout.isEdge(ch, ring)) {
                    superMaskSetMask(ch, false);
                }
            }
            if (triggerChangeEvent) {
                stateChangedNNJ(NNJChangeEvent.factoryMask());
            }
        }
    }

    /**{@inheritDoc}<p>
     * Default implementation-- adds superMask if NNJFilterMaskSuperimposer,
     * otherwise, simple passthrough to source.*/
    @Override
    public NNJDataMask getDataMask() {
        if (maskBuff == null) {
            if (isMaskSuperimposer) {
                maskBuff = new NNJDataMask(this.getNNJDataSource().getDataMask(), this.superMask);
            } else {
                maskBuff = this.getNNJDataSource().getDataMask();
            }
        }
        return maskBuff;
    }

    ;

    /**Simple passthrough to upstream source.*/
    @Override
    public final boolean isCompatible(NNJDataSource data) {
        return getNNJDataSource().isCompatible(data);
    }
}

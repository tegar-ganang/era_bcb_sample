package edu.georgetown.nnj.filterblocks.displays;

import edu.georgetown.nnj.data.NNJDataMask;
import edu.georgetown.nnj.data.NNJDataSource;
import edu.georgetown.nnj.data.NNJDataSourceConsumer;
import edu.georgetown.nnj.data.NNJDataSourceNull;
import edu.georgetown.nnj.data.NNJDataWindow;
import edu.georgetown.nnj.data.layout.NNJDataLayout;
import edu.georgetown.nnj.events.NNJChangeEvent;
import static edu.georgetown.nnj.events.NNJChangeEvent.*;
import edu.georgetown.nnj.events.NNJChangeHandlerImpl;
import edu.georgetown.nnj.events.NNJChangeListener;
import edu.georgetown.nnj.filterblocks.filters.NNJFilter;
import edu.georgetown.nnj.filterblocks.filters.NNJFilterMaskSuperimposer;
import java.util.ArrayList;
import java.util.HashSet;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import edu.georgetown.nnj.colors.NNJColorMap;
import edu.georgetown.nnj.colors.NNJColorMapNP;
import static java.awt.RenderingHints.*;

/**
 *
 * @author Kenta
 * @version 0.4.1
 */
public abstract class BakNNJAbstractDisplay extends JPanel implements NNJDisplay {

    NNJColorMap colorMap = NNJColorMapNP.INSTANCE;

    @Override
    public NNJColorMap getColorMap() {
        return colorMap;
    }

    @Override
    public void setColorMap(NNJColorMap colorMap) {
        this.colorMap = colorMap;
    }

    public BakNNJAbstractDisplay clone() {
        return this;
    }

    public String getFilterName() {
        return this.getClass().toString();
    }

    protected static String NULL_STRING = "";

    public String getFilterStateDescription() {
        return NULL_STRING;
    }

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

    /**Constructs a filter; argument source is asserted to be non-null
     * @param source source for the data fed to this filter
     */
    public BakNNJAbstractDisplay(NNJDataSource source) {
        this(source, true);
    }

    public BakNNJAbstractDisplay(NNJDataSource source, boolean filterActive) {
        this();
        this.setFilterActive(filterActive);
        setNNJDataSource(source);
    }

    /**No argument constructor should not be called explicitly.*/
    protected BakNNJAbstractDisplay() {
        super();
    }

    @Override
    public boolean getFilterActive() {
        return false;
    }

    @Override
    public void setFilterActive(boolean filterActive) {
    }

    @Override
    public int readDataPoint(int channel, int frame) {
        return this.getNNJDataSource().readDataPoint(channel, frame);
    }

    @Override
    public int[] readDataTrace(int channel) {
        return this.getNNJDataSource().readDataTrace(channel);
    }

    @Override
    public int[] readDataTrace(int channel, int decimationFactor) {
        return this.getNNJDataSource().readDataTrace(channel, decimationFactor);
    }

    @Override
    public int[] readDataTraceSegment(int channel, int start, int end) {
        return this.getNNJDataSource().readDataTraceSegment(channel, start, end);
    }

    @Override
    public int[] readDataTraceSegment(int channel, int start, int end, int decimationFactor) {
        return this.getNNJDataSource().readDataTraceSegment(channel, start, end, decimationFactor);
    }

    @Override
    public int[] readDataFrame(int frame) {
        return this.getNNJDataSource().readDataFrame(frame);
    }

    @Override
    public int[][] readData() {
        return this.getNNJDataSource().readData();
    }

    @Override
    public int[][] readData(int decimationFactor) {
        return this.getNNJDataSource().readData(decimationFactor);
    }

    @Override
    public NNJDataLayout getDataLayout() {
        return this.getNNJDataSource().getDataLayout();
    }

    @Override
    public NNJDataWindow getDataWindow() {
        return this.getNNJDataSource().getDataWindow();
    }

    protected NNJDataSource source = NNJDataSourceNull.factory();

    @Override
    public NNJDataSource getNNJDataSource() {
        return this.source;
    }

    @Override
    public void setNNJDataSource(NNJDataSource source) {
        if (this.source != null) {
            this.source.removeChangeListener(this);
        }
        if (source == null) {
            this.source = NNJDataSourceNull.factory();
        } else {
            this.source = source;
        }
        this.source.addChangeListener(this);
        setNNJDataSourceImpl();
        stateChangedNNJ(NNJChangeEvent.factoryAll());
    }

    /**This should provide the filter specific steps to be taken after changing
     * the <code>NNJDataSource source</code>. A call to 
     * {@link edu.georgetown.nnj.events.NNJChangeListener#stateChangedNNJ}
     * is automatically provided in {@link #setNNJDataSource(NNJDataSource)}, after this method is called,
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

    @Override
    public final void stateChangedNNJ(NNJChangeEvent evt) {
        stateChangedNNJImplCommonPre(evt);
        switch(evt.getType()) {
            case ALL:
                maskBuff = null;
                stateChangedNNJImplAll(evt);
                break;
            case ALL_CHANNELS:
                stateChangedNNJImplAllChannels(evt);
                break;
            case SOME_CHANNELS:
                stateChangedNNJImplSomeChannels(evt);
                break;
            case WINDOW:
                stateChangedNNJImplWindow(evt);
                break;
            case INFO:
                stateChangedNNJImplInfo(evt);
                break;
            case MASK:
                maskBuff = null;
                stateChangedNNJImplMask(evt);
                break;
        }
        stateChangedNNJImplCommonPost(evt);
        fireStateChanged(evt);
    }

    public void stateChangedNNJImplCommonPre(NNJChangeEvent evt) {
    }

    public void stateChangedNNJImplCommonPost(NNJChangeEvent evt) {
    }

    public void stateChangedNNJImplAll(NNJChangeEvent evt) {
        this.stateChangedNNJImplAllChannels(evt);
        this.stateChangedNNJImplLayout(evt);
        this.stateChangedNNJImplWindow(evt);
        this.stateChangedNNJImplMask(evt);
        this.stateChangedNNJImplInfo(evt);
    }

    public abstract void stateChangedNNJImplAllChannels(NNJChangeEvent evt);

    public abstract void stateChangedNNJImplSomeChannels(NNJChangeEvent evt);

    public abstract void stateChangedNNJImplLayout(NNJChangeEvent evt);

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

    public abstract void stateChangedNNJImplInfo(NNJChangeEvent evt);

    /**This method is provided to satisfy ChangeListener, so that this filter
     * can be registered as a listener to the NNJDataWindow.
     * @see edu.georgetown.nnj.data.NNJDataWindow
     */
    public final void stateChanged(ChangeEvent e) {
    }

    private NNJDataMask maskBuff;

    protected final boolean isMaskSuperimposer = NNJFilterMaskSuperimposer.class.isInstance(this);

    protected NNJDataMask superMask = new NNJDataMask();

    public final void superMaskFlush(boolean triggerChangeEvent) {
        superMask.setUnmaskAll();
        if (triggerChangeEvent) {
            stateChangedNNJ(NNJChangeEvent.factoryMask());
        }
    }

    public final void superMaskSetHideChannel(int det, boolean triggerChangeEvent) {
        superMask.setMask(det);
        if (triggerChangeEvent) {
            stateChangedNNJ(NNJChangeEvent.factoryMask());
        }
    }

    public final void superMaskEdgeChannels(int ring, boolean triggerChangeEvent) {
        if (isMaskSuperimposer) {
            NNJDataLayout tempLayout = this.getDataLayout();
            for (int ch = 0; ch < tempLayout.getChannelCount(); ch++) {
                if (tempLayout.isEdge(ch, ring)) {
                    superMaskSetHideChannel(ch, false);
                }
            }
            if (triggerChangeEvent) {
                stateChangedNNJ(NNJChangeEvent.factoryMask());
            }
        }
    }

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

    private NNJChangeHandlerImpl changeHandler = new NNJChangeHandlerImpl();

    @Override
    public void addChangeListener(NNJChangeListener l) {
        this.changeHandler.addChangeListener(l);
    }

    @Override
    public HashSet<NNJChangeListener> getChangeListeners() {
        return this.changeHandler.getChangeListeners();
    }

    @Override
    public void removeChangeListener(NNJChangeListener l) {
        this.changeHandler.removeChangeListener(l);
    }

    @Override
    public void removeChangeListeners() {
        this.changeHandler.removeChangeListeners();
    }

    @Override
    public void fireStateChanged(NNJChangeEvent event) {
        this.changeHandler.fireStateChanged(event);
    }

    @Override
    public int getListenerCount() {
        return changeHandler.getListenerCount();
    }

    @Override
    public boolean isCompatible(NNJDataSource data) {
        return getDataLayout().isCompatible(data.getDataLayout());
    }
}

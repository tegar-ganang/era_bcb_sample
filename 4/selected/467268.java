package ti.plato.components.socrates.source;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.ui.PlatformUI;
import oscript.data.Value;
import oscript.exceptions.PackagedScriptObjectException;
import ti.event.Event;
import ti.mcore.Environment;
import ti.mcore.u.log.PlatoLogger;
import ti.plato.components.logger.feedback.DiagnosticMonitorMain;
import ti.plato.components.logger.feedback.DiagnosticMonitorMain.OnGetRangeResult;
import ti.plato.components.logger.process.ContentManager;
import ti.plato.components.logger.process.ExternalProcessor;
import ti.plato.components.logger.process.ReferenceCache;
import ti.plato.components.logger.process.ScrollManager;
import ti.plato.components.logger.process.ScrollManager.ScrollPosition;
import ti.plato.components.socrates.constants.Constants;
import ti.plato.components.socrates.views.SocratesSourceManager;
import ti.plato.registry.Registry;
import ti.plato.registry.RegistryListener;
import ti.plato.shared.types.OnGetProgressInfoResult;
import ti.plato.shared.types.ProgressContributionItem;
import com.ti.dvt.datamodel.core.Buffer;
import com.ti.dvt.datamodel.core.DataModel;
import com.ti.dvt.datamodel.core.FloatingPoint;
import com.ti.dvt.datamodel.core.IChannelDescriptor;
import com.ti.dvt.datamodel.core.IDataField;
import com.ti.dvt.datamodel.core.Label;
import com.ti.dvt.datamodel.core.ULong;
import com.ti.dvt.datamodel.ui.PropertyInfo;

public class PlatoBuffer extends Buffer {

    static final PlatoLogger LOGGER = PlatoLogger.getLogger(PlatoBuffer.class);

    private static final DiagnosticMonitorMain extensionFeedback = DiagnosticMonitorMain.getDefault();

    private boolean yMinValid = false;

    private double yMin = -1.0;

    private boolean yMaxValid = false;

    private double yMax = -1.0;

    private boolean xMinValid = false;

    private double xMin = -1.0;

    private boolean xMaxValid = false;

    private double xMax = -1.0;

    private ProgressContributionItem progressIndicator = null;

    private List<ChannelInfo> channels = new ArrayList<ChannelInfo>();

    /**
	 * Holds info about a channel wired to this buffer, and provides access
	 * to values from this channel.
	 */
    private class ChannelInfo {

        public final String name;

        public final String[] field;

        public int timeShiftCount;

        ChannelInfo(String name, String[] field) {
            this.name = name;
            this.field = field;
            clearCache();
        }

        public void clearCache() {
            timeShiftCount = -1;
        }
    }

    /**
	 * Every element in the series maps to one of these (which is managed by
	 * the {@link U.ReferenceCache}).  Each one of these caches the 
	 * timeshift array value (if there is one calculated via formula) plus 
	 * N references (where N depends on the time-shift amount).
	 * <p>
	 * This way we can cache as much as possible, to avoid re-doing any
	 * calculations, but all cached data is mapped back to individual
	 * elements in the series, so that the series/index can manage 
	 * invalidating the appropriate cache entries when needed (either
	 * because the region position changes, or if new elements are 
	 * inserted in the middle of the index).
	 */
    private final class ReferenceToEvent implements ReferenceCache.EventReference {

        private Event evt;

        private ReferenceToRecord[] refs;

        private Value[] values;

        private Value[] timeShiftValues;

        ReferenceToEvent(Event evt) {
            this.evt = evt;
        }

        public Event getEvent() {
            return evt;
        }

        public ReferenceToRecord getReference(int timeShiftIndex) {
            if (refs == null) refs = new ReferenceToRecord[getTimeShiftCount()];
            if (refs[timeShiftIndex] == null) refs[timeShiftIndex] = new ReferenceToRecord(this, timeShiftIndex);
            return refs[timeShiftIndex];
        }

        public Value getValue(int channelIdx) {
            if (values == null) values = new Value[channels.size()];
            if (values[channelIdx] == null) values[channelIdx] = ContentManager.getValueOptimized(evt, channels.get(channelIdx).field);
            return values[channelIdx];
        }

        public Value getTimeshiftValue(int channelIdx) {
            if (timeShiftValues == null) timeShiftValues = new Value[channels.size()];
            if (timeShiftValues[channelIdx] == null) timeShiftValues[channelIdx] = ContentManager.getTimeShiftValue(evt, channels.get(channelIdx).field);
            return timeShiftValues[channelIdx];
        }
    }

    /**
	 * Every {@link ReferenceToEvent} maps to one or more {@link ReferenceToRecord}s
	 * depending on the timeshift count
	 */
    private class ReferenceToRecord {

        private ReferenceToEvent evtRef;

        private final int timeShiftIndex;

        private IDataField[] fields = new IDataField[(channels.size() * (multiRowMode ? 2 : 1)) + Constants.referenceFields.length];

        ReferenceToRecord(ReferenceToEvent evtRef, int timeShiftIndex) {
            this.evtRef = evtRef;
            this.timeShiftIndex = timeShiftIndex;
        }

        IDataField getField(int iField) {
            if (iField >= fields.length) return null;
            IDataField field = fields[iField];
            if (field == null) {
                switch(iField) {
                    case Constants.referenceFieldNr:
                    case Constants.referenceFieldNrCompressed:
                        double timeShiftNrDelay;
                        if (multiRowMode) timeShiftNrDelay = 0.0; else timeShiftNrDelay = ((double) timeShiftIndex) * (1.0 / ((double) getTimeShiftCount()));
                        long nr = evtRef.getEvent().getMetaData().getNr();
                        if (iField == 0) {
                            fields[0] = new FloatingPoint((double) nr + timeShiftNrDelay);
                        } else if (iField == 1) {
                            int nrc = extensionFeedback.compressIndex(handle, (int) nr);
                            fields[1] = new FloatingPoint((double) nrc + timeShiftNrDelay);
                        }
                        break;
                    case Constants.referenceFieldTime:
                        double timeShiftTimeDelay = 0.0;
                        if (timeShiftDelayArray != null) {
                            timeShiftTimeDelay = timeShiftDelayArray[timeShiftIndex];
                            if (multiRowMode) {
                                timeShiftTimeDelay = 0.0;
                            }
                        }
                        Value value = null;
                        if ((timeShiftDelayArray == null) && (timeShiftField != -1)) {
                            value = evtRef.getTimeshiftValue(timeShiftField);
                        }
                        if (value != null) {
                            Value elem = value.elementAt(U.val(timeShiftIndex));
                            if (Value.NULL.bopEquals(elem).castToBoolean()) fields[2] = FloatingPoint.NaN; else fields[2] = new FloatingPoint(elem.castToInexactNumber());
                        } else {
                            double time = evtRef.getEvent().getTimeStamp();
                            time = time + timeShiftTimeDelay;
                            fields[2] = new FloatingPoint(time);
                        }
                        break;
                    default:
                        populateValueFields();
                        break;
                }
                field = fields[iField];
            }
            if (iField < Constants.referenceFields.length) checkXval(((FloatingPoint) field).doubleValue()); else if (field instanceof FloatingPoint) checkYval(((FloatingPoint) field).doubleValue());
            return field;
        }

        private void populateValueFields() {
            int channelCount = channels.size();
            for (int idx = 0; idx < channelCount; idx++) {
                ChannelInfo channel = channels.get(idx);
                Value value = evtRef.getValue(idx);
                if (value != null) {
                    if ((timeShiftDelayArray != null) && (channel.timeShiftCount == -1)) {
                        Environment.getEnvironment().unhandledException(new Exception("can this happen??"));
                    }
                    if (channel.timeShiftCount != -1) {
                        if (idx == timeShiftField) {
                            if (U.isArray(value)) {
                                value = value.elementAt(U.val(timeShiftIndex)).unhand();
                            }
                        } else {
                            value = null;
                        }
                    }
                    if (U.isArray(value) && !multiRowMode) {
                        channel.timeShiftCount = value.length();
                        multiRowMode = (value.length() > 0) && U.isArray(value.elementAt(U.val(0)));
                        redraw();
                        if (multiRowMode && (sourceManager.stid.compareTo(Constants.barGraphStid) == 0)) {
                            multiRowMode = sourceManager.setMultiRowMode();
                        }
                        PlatformUI.getWorkbench().getDisplay().timerExec(Constants.uiRefreshTime, onTimer);
                        return;
                    }
                    if (multiRowMode && (idx == timeShiftField) && U.isArray(value)) {
                        try {
                            int n = (idx * 2) + 0 + Constants.referenceFields.length;
                            if ((value == null) || (value.length() < 2)) {
                                fields[n + 0] = null;
                                fields[n + 1] = null;
                            } else {
                                fields[n + 0] = v2s(value.elementAt(U.val(1)));
                                fields[n + 1] = v2s(value.elementAt(U.val(0)));
                            }
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    } else if ((value != null) && isValidDisplayValue(value)) {
                        fields[idx + Constants.referenceFields.length] = v2s(value);
                        if (addState(idx, value)) sourceManager.addAutomaticState(idx, value);
                    }
                } else {
                    fields[idx + Constants.referenceFields.length] = null;
                }
            }
        }
    }

    int findNr(int item0, int item1, double value) {
        int timeShiftCount = getTimeShiftCount();
        int nr = (int) value;
        int cnr = extensionFeedback.compressIndex(handle, nr);
        int actualNr = extensionFeedback.decompressIndex(handle, cnr);
        if (actualNr < nr) {
            return -(cnr + 1) * timeShiftCount;
        } else if (actualNr > nr) {
            return -cnr * timeShiftCount;
        } else {
            double timeShiftNrDelay = value - ((double) nr);
            double timeShiftIndexD = timeShiftNrDelay * timeShiftCount;
            int timeShiftIndex = (int) timeShiftIndexD;
            int iRecord = (cnr * timeShiftCount) + timeShiftIndex;
            if (timeShiftIndexD != (double) timeShiftIndex) {
                iRecord = -iRecord - 1;
            }
            return iRecord;
        }
    }

    int findCompressedNr(int item0, int item1, double value) {
        int timeShiftCount = getTimeShiftCount();
        double timeShiftNrDelay = value - ((double) ((int) value));
        double timeShiftIndexD = timeShiftNrDelay * timeShiftCount;
        int timeShiftIndex = (int) timeShiftIndexD;
        int iRecord = (((int) value) * timeShiftCount) + timeShiftIndex;
        if (timeShiftIndexD != (double) timeShiftIndex) {
            iRecord = -iRecord - 1;
        }
        return iRecord;
    }

    int findTimestamp(int item0, int item1, double value) {
        int cnr = extensionFeedback.findCompressedIndexByTimestamp(handle, (long) value);
        long t = extensionFeedback.getTime(handle, cnr);
        if (t == 0) System.err.println("this is bad, invalid cnr=" + cnr);
        OnGetRangeResult getRangeResult = extensionFeedback.onGetRange(handle);
        int rangeMin = getRangeResult.rangeMin;
        int rangeMax = getRangeResult.rangeMax;
        while ((cnr > rangeMin) && (t > value)) {
            cnr--;
            t = extensionFeedback.getTime(handle, cnr);
            if (t == 0) System.err.println("this is bad, invalid cnr=" + cnr);
        }
        while ((cnr < rangeMax) && (t < value)) {
            cnr++;
            t = extensionFeedback.getTime(handle, cnr);
            if (t == 0) System.err.println("this is bad, invalid cnr=" + cnr);
        }
        return -(cnr * getTimeShiftCount());
    }

    /**
	 * If we are in time-shift mode, this is set to <code>true</code> iff we are in
	 * the special case of bar graph "Multiple Rows" mode.  In this case, the value
	 * being displayed is a 2 dimensional array, looking like (a single "frame" of data):
	 * <pre>
	 *   [
	 *       [ &lt;label 1&gt;, &lt;value 1&gt; ],
	 *       [ &lt;label 2&gt;, &lt;value 2&gt; ],
	 *       ....
	 *       [ &lt;label N&gt;, &lt;value N&gt; ]
	 *   ]
	 * </pre>
	 * which gets fed to SoCrates as three columns of data:
	 * <pre>
	 *     &lt;frame N&gt;, &lt;value 1&gt;, &lt;label 1&gt;
	 *     &lt;frame N&gt;, &lt;value 2&gt;, &lt;label 2&gt;
	 *     ....
	 *     &lt;frame N&gt;, &lt;value N&gt;, &lt;label N&gt;
	 * </pre>
	 * where "&lt;frame N&gt;" is the value of the reference field, ie. the timestamp or
	 * Nr of the trace which produces the "frame" of data.
	 * 
	 * this is slightly different from the normal time-shift case, because the time-shift
	 * delay is zero, and two columns of data are produced from each entry in the result
	 * array.
	 */
    private boolean multiRowMode = false;

    /**
	 * Create a buffer to store data in memory that has no upper bound on
	 * amount of storage.
	 */
    public PlatoBuffer() {
        PlatformUI.getWorkbench().getDisplay().timerExec(Constants.uiRefreshTime, onTimer);
        Registry.getRegistry().subscribe(this, ScrollManager.FIRST_VISIBLE_ITEM_ID, new RegistryListener<ScrollPosition>() {

            public void valueChanged(ScrollPosition val) {
                if (sourceManager == null) return;
                sourceManager.setTopIndexFromPlatoValue(val.getNr(), val.getTime());
            }
        });
        Registry.getRegistry().subscribe(this, ScrollManager.SELECTED_ITEM_ID, new RegistryListener<ScrollPosition>() {

            public void valueChanged(ScrollPosition val) {
                if (sourceManager == null) return;
                sourceManager.setCurrentIndex(val.getNr(), val.getTime());
            }
        });
        Registry.getRegistry().subscribe(this, ExternalProcessor.REDRAW_COUNT_ID, new RegistryListener<Integer>() {

            public void valueChanged(Integer val) {
                if (sourceManager == null) return;
                for (int i = 0; i < channels.size(); i++) channels.get(i).clearCache();
                multiRowMode = false;
                redraw();
            }
        });
    }

    private Runnable onTimer = new Runnable() {

        public void run() {
            if (deleteTimer) return;
            if (!sourceManager.hasDataToDisplay) {
                PlatformUI.getWorkbench().getDisplay().timerExec(Constants.uiRefreshTime, onTimer);
                return;
            }
            if (freeze) {
                PlatformUI.getWorkbench().getDisplay().timerExec(Constants.uiRefreshTime, onTimer);
                return;
            }
            OnGetProgressInfoResult onGetProgressInfoResult = extensionFeedback.onGetProgressInfo(handle);
            if (pauseStatus) {
                if (onGetProgressInfoResult.getIndexSpecChanged()) {
                    pauseStatus = false;
                    sourceManager.update();
                } else {
                    PlatformUI.getWorkbench().getDisplay().timerExec(Constants.uiRefreshTime, onTimer);
                    progressIndicator.setMax(onGetProgressInfoResult.getNoCount() - 1);
                    return;
                }
            }
            progressIndicator.setResult(onGetProgressInfoResult);
            int newCount;
            OnGetRangeResult getRangeResult = extensionFeedback.onGetRange(handle);
            int rangeMin = getRangeResult.rangeMin;
            int rangeMax = getRangeResult.rangeMax;
            if (rangeMin == -1 || rangeMax == -1) {
                newCount = 0;
            } else newCount = rangeMax - rangeMin + 1;
            if (newCount != internalCount) {
                internalCount = newCount;
                PlatoBuffer.this.fireUpdateEvent(null, 0, IUpdateEvent.DATA_INSERTED);
                boolean autoScroll = ScrollManager.getAutoScroll();
                if (autoScroll && sourceManager.stid.compareTo(Constants.barGraphStid) == 0) {
                    long nr = extensionFeedback.getNr(handle, internalCount - 1);
                    long time = extensionFeedback.getTime(handle, internalCount - 1);
                    sourceManager.fireUpdateEvent((int) nr, time);
                }
            }
            PlatformUI.getWorkbench().getDisplay().timerExec(Constants.uiRefreshTime, onTimer);
        }
    };

    /**
	 * @see com.ti.dvt.datamodel.core.IBuffer#clear()
	 */
    public void clear() {
    }

    /**
	 * @see com.ti.dvt.datamodel.core.IBuffer#count()
	 */
    private int internalCount = 0;

    public int count() {
        return getTimeShiftCount() * internalCount;
    }

    ReferenceCache<ReferenceToEvent> referenceCache = new ReferenceCache<ReferenceToEvent>() {

        @Override
        protected ReferenceToEvent constructReference(Event evt) {
            return new ReferenceToEvent(evt);
        }
    };

    /** 
	 * @see com.ti.dvt.datamodel.core.IBuffer#getReferenceToRecord(int)
	 */
    public Object getReferenceToRecord(int iRecord) {
        if (internalCount == 0) return null;
        if (channels.size() == 0) return null;
        if (iRecord < 0) return null;
        int timeShiftCount = getTimeShiftCount();
        int outsideRecord = iRecord / timeShiftCount;
        int timeShiftIndex = iRecord - (outsideRecord * timeShiftCount);
        ReferenceToEvent evtRef = referenceCache.getReference(outsideRecord);
        if (evtRef == null) return null;
        return evtRef.getReference(timeShiftIndex);
    }

    /**
	 * @see com.ti.dvt.datamodel.core.IBuffer#getField(Object, int)
	 */
    public IDataField getField(Object ref, int iField) {
        if (ref == null) return null;
        return ((ReferenceToRecord) ref).getField(iField);
    }

    public void setRange(int minimum, int maximum) {
        int timeShiftCount = getTimeShiftCount();
        referenceCache.setRange(minimum / timeShiftCount, maximum / timeShiftCount);
    }

    private final IDataField v2s(Value val) {
        if (val == null) return null;
        if (!U.isString(val)) {
            double currentValue = 0.0;
            try {
                currentValue = val.castToInexactNumber();
            } catch (PackagedScriptObjectException e) {
            }
            return new FloatingPoint(currentValue);
        } else {
            String currentValue = val.castToString();
            if ((sourceManager.stid.compareTo(Constants.stateGraphStid) == 0) || (sourceManager.stid.compareTo(Constants.discreteGraphStid) == 0)) return new ULong(SocratesSourceManager.getStateId(currentValue));
            return new Label(currentValue);
        }
    }

    /**
	 * Called by {@link ReferenceToRecord#getField(int)} before returning an
	 * x-axis value, so the buffer can take care of scrolling and other such
	 * housekeeping.
	 */
    private final void checkXval(double currentValue) {
        if (!xMinValid || xMin > currentValue) {
            xMinValid = true;
            xMin = currentValue;
        }
        if (!xMaxValid || xMax < currentValue) {
            xMaxValid = true;
            xMax = currentValue;
        }
    }

    /**
	 * Called by {@link ReferenceToRecord#getField(int)} before returning an
	 * y-axis value, so the buffer can take care of scrolling and other such
	 * housekeeping.
	 */
    private final void checkYval(double currentValue) {
        boolean modifyRange = false;
        if (!yMinValid || yMin > currentValue) {
            yMinValid = true;
            yMin = currentValue;
            modifyRange = true;
        }
        if (!yMaxValid || yMax < currentValue) {
            yMaxValid = true;
            yMax = currentValue;
            modifyRange = true;
        }
        if (modifyRange) {
            if (yMin != yMax) sourceManager.setYRange(yMin, yMax);
        }
    }

    /**
	 * @see com.ti.dvt.datamodel.core.IBuffer#getRecord(Object, IDataField[])
	 */
    public void getRecord(Object ref, IDataField record[]) {
        int recordCount = record.length;
        int recordIndex;
        for (recordIndex = 0; recordIndex < recordCount; recordIndex++) {
            record[recordIndex] = getField(ref, recordIndex);
        }
    }

    /** 
	 * @see com.ti.dvt.datamodel.core.IDataProcessor.IInput#processInput(IDataField [])
	 */
    public void processInput(IDataField data[]) {
    }

    /** 
	 * @see com.ti.dvt.datamodel.core.IDataProcessor.IInput#processDiscontinuity(String)
	 */
    public void processDiscontinuity(String msg) {
    }

    /** 
	 * @see com.ti.dvt.datamodel.core.IDataProcessor.IInput#configure(IChannelDescriptor)
	 */
    public void configure(IChannelDescriptor format) throws DataModel.ConfigurationException {
        reset();
        super.configure(format);
        this.fmt = format;
    }

    private int handle = -1;

    public String getHandle() {
        return String.valueOf(handle);
    }

    public void setHandle(String newHandle) {
        handle = Integer.parseInt(newHandle);
        referenceCache.setHandle(handle);
    }

    public static Object getHandleInfo() {
        return new PropertyInfo(PropertyInfo.READONLY);
    }

    private SocratesSourceManager sourceManager = null;

    public Object getSourceManager() {
        return sourceManager;
    }

    public void setSourceManager(Object newSourceManager) {
        sourceManager = (SocratesSourceManager) newSourceManager;
    }

    public static Object getSourceManagerInfo() {
        return new PropertyInfo(PropertyInfo.READONLY);
    }

    public void addChannel(String name, String[] field) {
        channels.add(new ChannelInfo(name, U.extendArray(name, field)));
        reinit();
    }

    public void removeChannel(int idx) {
        channels.remove(idx);
        reinit();
    }

    public void redraw() {
        calculateTimeShiftCount();
        reinit();
    }

    private void reinit() {
        referenceCache.clear();
        yMin = -1;
        yMax = -1;
        yMinValid = false;
        yMaxValid = false;
        xMin = -1;
        xMax = -1;
        xMinValid = false;
        xMaxValid = false;
        internalCount = 0;
    }

    private double[] timeShiftDelayArray = null;

    public double[] getTimeShiftDelayArray() {
        return timeShiftDelayArray;
    }

    private int timeShiftField = -1;

    public void calculateTimeShiftCount() {
        timeShiftDelayArray = null;
        timeShiftField = -1;
        int channelCount = channels.size();
        for (int idx = 0; idx < channelCount; idx++) {
            ChannelInfo channel = channels.get(idx);
            Integer originalTimeShiftCountObject = null;
            if (channel.timeShiftCount != -1) {
                originalTimeShiftCountObject = new Integer(channel.timeShiftCount);
            } else {
                originalTimeShiftCountObject = extensionFeedback.getTimeShiftCount(channel.field);
            }
            if (originalTimeShiftCountObject == null) continue;
            int originalTimeShiftCount = originalTimeShiftCountObject.intValue();
            if (originalTimeShiftCount <= 1) continue;
            double[][] timeShiftDelayArrayTmp = extensionFeedback.getTimeShiftDelay(channel.field, originalTimeShiftCount);
            timeShiftField = idx;
            if (timeShiftDelayArrayTmp == null) {
                channel.timeShiftCount = originalTimeShiftCount;
                sourceManager.setTimeShiftIndicator(true);
                continue;
            }
            timeShiftDelayArray = new double[originalTimeShiftCount];
            if (timeShiftDelayArrayTmp[0] != null) {
                for (int i = 0; i < originalTimeShiftCount; i++) {
                    timeShiftDelayArray[i] = (double) i * timeShiftDelayArrayTmp[0][0];
                }
            } else {
                timeShiftDelayArray = timeShiftDelayArrayTmp[1];
            }
            sourceManager.setTimeShiftIndicator(true);
            return;
        }
        sourceManager.setTimeShiftIndicator(false);
    }

    final int getTimeShiftCount() {
        if (timeShiftDelayArray != null) {
            return timeShiftDelayArray.length;
        } else if (timeShiftField != -1) {
            return channels.get(timeShiftField).timeShiftCount;
        }
        return 1;
    }

    public int getDisplayedNumber() {
        return internalCount;
    }

    public double getDisplayedExtent() {
        return xMax - xMin;
    }

    private Value[][] stateChannelArray = null;

    private boolean addState(final int channel, Value value) {
        if ((sourceManager.stid.compareTo(Constants.stateGraphStid) != 0) && (sourceManager.stid.compareTo(Constants.discreteGraphStid) != 0)) return false;
        if (stateChannelArray == null) {
            stateChannelArray = new Value[channel + 1][];
            for (int idx = 0; idx < (channel + 1); idx++) stateChannelArray[idx] = null;
            stateChannelArray[channel] = new Value[1];
            stateChannelArray[channel][0] = value;
            return true;
        } else {
            if (channel < stateChannelArray.length) {
                if (stateChannelArray[channel] != null) {
                    int count = stateChannelArray[channel].length;
                    int index;
                    for (index = 0; index < count; index++) {
                        if (stateChannelArray[channel][index].bopEquals(value).castToBoolean()) return false;
                    }
                }
                if (stateChannelArray[channel] == null) {
                    stateChannelArray[channel] = new Value[1];
                    stateChannelArray[channel][0] = value;
                    return true;
                } else {
                    int count = stateChannelArray[channel].length;
                    int index;
                    Value[] stateArrayTmp = new Value[count + 1];
                    for (index = 0; index < count; index++) {
                        stateArrayTmp[index] = stateChannelArray[channel][index];
                    }
                    stateArrayTmp[index] = value;
                    stateChannelArray[channel] = stateArrayTmp;
                    return true;
                }
            } else {
                Value[][] stateChannelArrayTmp = new Value[channel + 1][];
                int count = channel + 1;
                int index;
                for (index = 0; index < count; index++) {
                    if (index < stateChannelArray.length) stateChannelArrayTmp[index] = stateChannelArray[index]; else stateChannelArrayTmp[index] = null;
                }
                stateChannelArrayTmp[channel] = new Value[1];
                stateChannelArrayTmp[channel][0] = value;
                stateChannelArray = stateChannelArrayTmp;
                return true;
            }
        }
    }

    private boolean deleteTimer = false;

    public void dispose() {
        Registry.getRegistry().unsubscribeAll(this);
        deleteTimer = true;
    }

    public int getChannelCount() {
        return channels.size();
    }

    public String getChannelName(int channel) {
        return channels.get(channel).name;
    }

    public void setProgressIndicator(ProgressContributionItem progressIndicator) {
        this.progressIndicator = progressIndicator;
    }

    private boolean pauseStatus = false;

    public void onPause(boolean status) {
        pauseStatus = status;
    }

    public boolean getPause() {
        return pauseStatus;
    }

    private boolean freeze = false;

    public void setFreeze(boolean status) {
        freeze = status;
    }

    boolean isValidDisplayValue(Value value) {
        String stid = sourceManager.stid;
        if (multiRowMode) return U.isArray(value); else if (stid.equals(Constants.stateGraphStid) || stid.equals(Constants.discreteGraphStid) || stid.equals(Constants.tableGraphStid)) return U.isString(value) || U.isInteger(value); else return U.isInteger(value);
    }
}

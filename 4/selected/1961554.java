package net.sourceforge.olduvai.lrac;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import javax.swing.JFrame;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import net.sourceforge.olduvai.accordiondrawer.AccordionDrawer;
import net.sourceforge.olduvai.accordiondrawer.SplitAxis;
import net.sourceforge.olduvai.accordiondrawer.SplitAxisLogger;
import net.sourceforge.olduvai.accordiondrawer.SplitLine;
import net.sourceforge.olduvai.lrac.darkstardataservice.DataServiceDispatcher;
import net.sourceforge.olduvai.lrac.drawer.AccordionLRACDrawer;
import net.sourceforge.olduvai.lrac.drawer.Groups;
import net.sourceforge.olduvai.lrac.drawer.queries.ActiveSourceQuery;
import net.sourceforge.olduvai.lrac.drawer.queries.SwatchQuery;
import net.sourceforge.olduvai.lrac.drawer.structure.Cell;
import net.sourceforge.olduvai.lrac.drawer.structure.strips.Strip;
import net.sourceforge.olduvai.lrac.drawer.structure.templates.Template;
import net.sourceforge.olduvai.lrac.genericdataservice.AbstractFocusGroup;
import net.sourceforge.olduvai.lrac.genericdataservice.AbstractSource;
import net.sourceforge.olduvai.lrac.genericdataservice.DataRequestInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.DataResultInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.cellviewer.AbstractCellViewer;
import net.sourceforge.olduvai.lrac.genericdataservice.queries.ActiveSourceQueryInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.queries.DetailQueryInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.queries.QueryInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.queries.SwatchQueryInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.records.DetailRecord;
import net.sourceforge.olduvai.lrac.genericdataservice.records.SwatchRecordInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.DataCellInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.FocusGroupInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelGroupInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelItemInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.SourceGroupInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.SourceInterface;
import net.sourceforge.olduvai.lrac.logging.LogEntry;
import net.sourceforge.olduvai.lrac.ui.ConnectionError;
import net.sourceforge.olduvai.lrac.ui.NoDataError;
import net.sourceforge.olduvai.lrac.ui.UI;
import net.sourceforge.olduvai.lrac.ui.VisualLogRequest;
import net.sourceforge.olduvai.lrac.util.Util;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * Central data arbitrator between rendering thread and the data request/reply
 * thread.
 * 
 * Contains all data objects and operations that manipulate those objects.
 * Effectively contains the 'grid'. Cells are retrieved indirectly by means of
 * their containing Device objects.
 * 
 * Also performs all timing operations related to adding & dropping data.
 * 
 * @author Peter McLachlan (spark343@cs.ubc.ca)
 * 
 */
public class DataGrid implements DataResultInterface {

    private static final DataGrid INSTANCE = new DataGrid();

    private static final int STRIP_CHANGED = 0;

    private static final int SOURCE_CHANGED = 1;

    /**
	 * Accessed at logger[AccordionDrawer.X] and logger[AccordionDrawer.Y]
	 */
    private LiveRACSplitAxisLogger[] loggers = new LiveRACSplitAxisLogger[2];

    /**
	 * Creates a new DataStore 
	 * 
	 * Note: in order to use DataStore for anything other than the 
	 * connect call it must be associated with a drawer object 
	 * using the {@link #setLrd(AccordionLRACDrawerFinal)} method.  
	 * 
	 */
    private DataGrid() {
    }

    /**
	 * DataStore must be passed the LRD with the {@link #setLrd(AccordionLRACDrawerFinal)} 
	 * call BEFORE any call other than {@link #connectDataInterface(JFrame)} is made!  
	 * 
	 * @return
	 */
    public static final DataGrid getInstance() {
        return INSTANCE;
    }

    /**
	 * Simple implementation of TimerTask to update the spinner times each
	 * time run() gets called.   
	 * @author Peter McLachlan <spark343@cs.ubc.ca>
	 * 
	 */
    class DataTimerTask extends TimerTask {

        public void run() {
            final UI ui = LiveRAC.getInstance().getUI();
            final Date newBeginDate = new Date(timeRange[0].getTime() + getRefreshAmount() * 1000);
            final Date newEndDate = new Date(timeRange[1].getTime() + getRefreshAmount() * 1000);
            ui.setSpinnerTimes(newBeginDate, newEndDate);
        }
    }

    static boolean firstTimeEver = true;

    private SortedSet<TimeRangeSampleIntervalRelation> sortedTimeRangeSampleRelationSet;

    /**
	 * Selects the correct sample interval to use given a specified time range.  
	 * 
	 * @param timeRange
	 * @return
	 */
    public TimeRangeSampleIntervalRelation getSampleInterval(long timeRange) {
        timeRange = timeRange / 1000;
        Iterator<TimeRangeSampleIntervalRelation> it = sortedTimeRangeSampleRelationSet.iterator();
        while (it.hasNext()) {
            final TimeRangeSampleIntervalRelation interval = it.next();
            final long nextMinRange = interval.getMinTime();
            if (timeRange >= nextMinRange) return interval;
        }
        System.err.println("Selected interval: " + timeRange + " has no valid interval!  Using the smallest available.");
        return sortedTimeRangeSampleRelationSet.last();
    }

    /**
	 * Default getSampleInterval which returns the sample interval to be used based on the range
	 * selected by the time slider.  
	 * @return
	 */
    public TimeRangeSampleIntervalRelation getSampleInterval() {
        return getSampleInterval(getTimeRangeInMillis());
    }

    /**
	 * This method is only valid for the source axis and is used to preserve
	 * the ordering of sources.  It binary searches the source axis to 
	 * find the split line that lies to the left of the new source to be 
	 * inserted.  
	 * 
	 * @param sourceAxis The source axis
	 * @param source
	 * @return line to the left. NEVER maxStuckLine()
	 */
    static final SplitLine findLeftAdjacentSourceSplit(final SplitAxis sourceAxis, final SourceInterface newSource) {
        final SplitLine minStuckLine = sourceAxis.getMinStuckLine();
        final SourceInterface minStuckSource = (SourceInterface) minStuckLine.rowObject;
        if (newSource.compareTo(minStuckSource) < 0) {
            System.err.println("newSource before minStuckSource?  This should be handled already");
        }
        if (sourceAxis.getSize() == 0) return null;
        {
            final SplitLine nextAfterMinLine = sourceAxis.getNextSplit(minStuckLine);
            final SourceInterface nextAfterMinSource = (SourceInterface) nextAfterMinLine.rowObject;
            if (newSource.compareTo(minStuckSource) > 0 && newSource.compareTo(nextAfterMinSource) < 0) {
                return minStuckLine;
            }
        }
        return findAdjacentSourceSplitRecurse(sourceAxis.getRoot(), newSource, sourceAxis);
    }

    static final SplitLine findAdjacentSourceSplitRecurse(final SplitLine currLine, final SourceInterface compSource, final SplitAxis axis) {
        final SourceInterface currSource = (SourceInterface) currLine.getRowObject();
        final int compareResult = compSource.compareTo(currSource);
        if (compareResult < 0) {
            if (currLine.getLeftChild() == null) return axis.getPreviousSplit(currLine); else return findAdjacentSourceSplitRecurse(currLine.getLeftChild(), compSource, axis);
        } else if (compareResult > 0) {
            if (currLine.getRightChild() == null) return currLine; else return findAdjacentSourceSplitRecurse(currLine.getRightChild(), compSource, axis);
        } else {
            System.out.println("Source comparators match: (" + currSource + ") == (" + compSource + ")");
            return currLine;
        }
    }

    /**
	 * This method is only valid for the source axis and is used to preserve
	 * the ordering of sources.  It binary searches the source axis to 
	 * find the split line that lies to the left of the new source to be 
	 * inserted.  
	 * 
	 * @param stripAxis The source axis
	 * @param strip
	 * @return line to the left. NEVER maxStuckLine()
	 */
    static final SplitLine findLeftAdjacentStripSplit(final SplitAxis stripAxis, final Strip strip, Comparator<Strip> cr) {
        if (stripAxis.getSize() == 0) return null;
        final SplitLine minStuckLine = stripAxis.getMinStuckLine();
        final Strip minStuckStrip = (Strip) minStuckLine.getRowObject();
        if (cr.compare(strip, minStuckStrip) < 0) {
            System.err.println("newStrip before minStuckStrip should be handled prior to calling this method.");
        }
        {
            final SplitLine nextAfterMinLine = stripAxis.getNextSplit(minStuckLine);
            final Strip nextAfterMinSource = (Strip) nextAfterMinLine.getRowObject();
            if (cr.compare(strip, minStuckStrip) > 0 && cr.compare(strip, nextAfterMinSource) < 0) {
                return minStuckLine;
            }
        }
        return findAdjacentStripSplitRecurse(stripAxis.getRoot(), strip, stripAxis, cr);
    }

    static final SplitLine findAdjacentStripSplitRecurse(final SplitLine currLine, final Strip compStrip, final SplitAxis axis, Comparator<Strip> cr) {
        final Strip currStrip = (Strip) currLine.getRowObject();
        final int compareResult = cr.compare(compStrip, currStrip);
        if (compareResult < 0) {
            if (currLine.getLeftChild() == null) return axis.getPreviousSplit(currLine); else return findAdjacentStripSplitRecurse(currLine.getLeftChild(), compStrip, axis, cr);
        } else if (compareResult > 0) {
            if (currLine.getRightChild() == null) return currLine; else return findAdjacentStripSplitRecurse(currLine.getRightChild(), compStrip, axis, cr);
        } else {
            System.out.println("Strip comparators match: (" + currStrip + ") == (" + compStrip + ")");
            return currLine;
        }
    }

    public static boolean isFirstTimeEver() {
        return firstTimeEver;
    }

    public static void setFirstTimeEver(boolean firstTimeEver) {
        DataGrid.firstTimeEver = firstTimeEver;
    }

    boolean firstLoad = false;

    /**
	 * Pointer to the drawer
	 */
    AccordionLRACDrawerFinal lrd;

    /**
	 * Individual sources that were selected by the user in the source selection dialog.
	 * This may be different from those sources that are actually active.  For example, 
	 * there may be a filter rule removing some or all of the contents of this list.  
	 * Additionally, this list is not the definitive list of active sources, the 
	 * list of selected source groups also determines active sources. 
	 */
    private List<SourceInterface> selectedSourceList;

    /**
	 * This list of source groups the user has selected from the source selection 
	 * dialog.  
	 */
    private List<SourceGroupInterface> selectedSourceGroupList;

    /**
	 * This is the master list of all strips.  It is first loaded from the data service
	 * but may be modified during program runtime.  Changes are not persistent however
	 * unless an explicit save call is made.
	 * 
	 * Only bound and active strips will be rendered.  Any strip that fails to bind, 
	 * or has been marked as inactive by the user will not be drawn but remains in this list.
	 *   
	 */
    private List<Strip> stripList;

    /**
	 * This is the master list of all templates.  It is first loaded from the data service
	 * but may be modified during program runtime.  Changes are not persistent however
	 * unless an explicit save call is made.  
	 */
    private List<Template> templateList;

    /**
	 * This is the master list of all source focus groups.  It is first loaded from the data service
	 * but may be modified during program runtime.  Changes are not persistent however
	 * unless an explicit save call is made.
	 */
    private List<FocusGroupInterface> sourceFocusGroups;

    /**
	 * This is the master list of all strip focus groups.  It is first loaded from the data service
	 * but may be modified during program runtime.  Changes are not persistent however
	 * unless an explicit save call is made.
	 */
    private List<FocusGroupInterface> stripFocusGroups;

    /**
	 * This is a map containing only the active sources.  It maps from the {@link SourceInterface#getInternalName()} String
	 * to a SourceObject. 
	 * It is redundant with the SourceAxis data structure in that both have a complete list of active
	 * sources, but it offers some conveniences for checking
	 * whether redundant sources are being added.  Although the number of sources 
	 * may be quite large in the future, the potentially arbitrary ordering of the SourceAxis
	 * makes it hard to do away with this structure without suffering a linear insert cost 
	 * to check for redundant entries.  
	 */
    private Map<String, SourceInterface> activeSourceMap;

    /**
	 * This is a set containing only the active strips.  It is technically redundant
	 * with the StripAxis data structure, but it offers some conveniences for checking
	 * whether any redundancies exist and the number of strips is not large.  
	 */
    private Set<Strip> activeStripSet;

    /**
	 * This serves as a reverse map.  Input channels are bound to strips, but 
	 * when new input channels are received the data contained must be assigned
	 * to the correct strip cells.  This map lets us look up the set of strips that 
	 * are bound to the given channel.  
	 */
    private Map<InputChannelItemInterface, Set<Strip>> inputChannelItemToStripSet;

    /**
	 * Contains an ordered set of values for the current source grouping parameter.  
	 * This is used by the 'next source group grow' call.  Basically this is a quick
	 * lookup system so we don't have to search for the next key manually.     
	 */
    private TreeSet<String> sourceGroupValueSet = new TreeSet<String>();

    /**
	 * Store a set of metakeys for the currently loaded sources
	 * This saves us having to regenerate it as needed.  mostly used for 
	 * updating {@link UI#updateSearchTypeBox(Set)}
	 * 
	 * This structure should be maintained by any add or remove selected source operation
	 */
    private TreeSet<String> sourceMetaKeySet = new TreeSet<String>();

    /**
	 * Notifies listeners of various types of change events
	 */
    private EventListenerList listenerList = new EventListenerList();

    final ChangeListener templateChangeListener = new ChangeListener() {

        public void stateChanged(ChangeEvent e) {
            templateChanged();
        }
    };

    /**
	 * Data refresh rate in seconds
	 */
    int refreshRate = 15;

    /**
	 * Seconds to advance with each refresh
	 */
    private int refreshAmount = 300;

    /**
	 * Incremented for each swatch query generated 
	 * Responses for each query should bear the same query number.  
	 */
    private static long swatchQueryId = 0;

    /**
	 * Incremented for each detail query generated.  
	 * Response will have the same queryId 
	 */
    private static long detailQueryId;

    /**
	 * Store whether a redraw is due to movement of the timeSlider or
	 * timeSpinners. This gets reset to false at the end of every draw cycle in
	 * drawPostFrame and gets set to true in the UI class whenever there is
	 * activity on the slider or spinners.
	 */
    private boolean timeChanged = false;

    /**
	 * An array of size 2 to store the beginDate and endDate of the time range
	 * currently being monitored
	 */
    private Date[] timeRange = { null, null };

    /**
	 * Returns the time range in milliseconds.  Derived from {@link #timeRange}.
	 * @return
	 */
    public long getTimeRangeInMillis() {
        return timeRange[1].getTime() - timeRange[0].getTime();
    }

    /**
	 * Threaded handler for managing requests
	 */
    private DataRequestInterface dataInterface;

    /**
	 * Run every refreshRate ms to check for new alarms
	 */
    private Timer updateTimer;

    /**
	 * Cached sourceGridEvent lazily created.  
	 */
    private SourceGridEvent sourceGridEvent;

    /**
	 * Cached stripGridEvent lazily created.  
	 */
    private StripGridEvent stripGridEvent;

    List<List<DetailRecord>> detailQueue = Collections.synchronizedList(new ArrayList<List<DetailRecord>>());

    List<List<SwatchRecordInterface>> swatchQueue = Collections.synchronizedList(new ArrayList<List<SwatchRecordInterface>>());

    /**
	 * Specifies the drawer associated with this datagrid.  This should 
	 * happen immediately after the connection attempt has been 
	 * confirmed.  
	 * 
	 * @param lrd
	 */
    public void setLrd(AccordionLRACDrawerFinal lrd) {
        this.lrd = lrd;
    }

    public void detailQueryResult(List<DetailRecord> results) {
        if (results == null) return;
        this.detailQueue.add(results);
        this.lrd.forceRedraw();
    }

    public void disconnect() {
        this.dataInterface.dispose();
        this.dataInterface = null;
    }

    /**
	 * Indicate the contents of cells has changed, a new swatch request should be generated
	 * and all chart contents should be updated.
	 * TODO: examine in more detail how this is affected by new single-instance-in-memory
	 * swatches/templates  
	 */
    public void fireCellContentsChange() {
        if (lrd == null) return;
        lrd.getLr().partialRefreshCycle();
    }

    /**
	 * suspend query processing, Flush the DataGrid object of all state, resume query processing  
	 * 
	 * @return
	 */
    public boolean flushData() {
        dataInterface.pause();
        stopTimer();
        lrd.shutdown();
        selectedSourceList = null;
        selectedSourceGroupList = null;
        stripList = null;
        templateList = null;
        sourceFocusGroups = null;
        stripFocusGroups = null;
        activeSourceMap = null;
        activeStripSet = null;
        inputChannelItemToStripSet = null;
        sourceGroupValueSet = null;
        swatchQueue.clear();
        detailQueue.clear();
        swatchQueue.clear();
        detailQueue.clear();
        dataInterface.go();
        ;
        return true;
    }

    /**
	 * Retrieve the start of the currently selected time range
	 * @return
	 */
    public Date getBeginDate() {
        return timeRange[0];
    }

    /**
	 * Retrieve the end of the currently selected time range
	 * @return
	 */
    public Date getEndDate() {
        return timeRange[1];
    }

    public int getRefreshAmount() {
        return refreshAmount;
    }

    /**
	 * @return Returns the refreshRate.
	 */
    public int getRefreshRate() {
        return refreshRate;
    }

    /**
	 * Retrieve the list of currently selected groups
	 * @return
	 */
    public List<SourceGroupInterface> getSelectedSourceGroupList() {
        return selectedSourceGroupList;
    }

    /**
	 * Retrieves a List interface for the active, rendered set of Sources.  
	 * This List is obtained by iterating over all Source split lines and retrieving
	 * the associated object.  
	 * 
	 * O(n) where n is the number of sources
	 * 
	 * @return
	 */
    public List<SourceInterface> getSortedSourceList() {
        final SplitAxis sourceAxis = lrd.getSourceAxis();
        if (sourceAxis == null) return null;
        List<SourceInterface> sourceList = new ArrayList<SourceInterface>(sourceAxis.getSize());
        for (Iterator<SplitLine> it = sourceAxis.iterator(); it.hasNext(); ) {
            SplitLine currLine = it.next();
            if (currLine.rowObject != null) {
                sourceList.add((SourceInterface) currLine.getRowObject());
            }
        }
        return sourceList;
    }

    /**
	 * Retrieves a List interface for the active, rendered set of Strips.  
	 * This List is obtained by iterating over all Strip split lines and retrieving
	 * the associated object.  
	 * 
	 * O(n) where n is the number of strips
	 * 
	 * @return
	 */
    public List<Strip> getSortedStripList() {
        final SplitAxis stripAxis = lrd.getStripAxis();
        if (stripAxis == null) return null;
        List<Strip> stripList = new ArrayList<Strip>(stripAxis.getSize());
        for (Iterator<SplitLine> it = stripAxis.iterator(); it.hasNext(); ) {
            final SplitLine currLine = it.next();
            if (currLine.rowObject != null) {
                stripList.add((Strip) currLine.getRowObject());
            }
        }
        return stripList;
    }

    public List<FocusGroupInterface> getSourceFocusGroups() {
        return sourceFocusGroups;
    }

    /**
	 * Retrieve the list of focus groups for strips
	 * @return
	 */
    public List<FocusGroupInterface> getStripFocusGroups() {
        return stripFocusGroups;
    }

    /**
	 * Retrieve the complete list of all strips both active & inactive.  
	 * 
	 * @return
	 */
    public List<Strip> getStripList() {
        return stripList;
    }

    public List<Template> loadTemplateList() {
        if (templateList == null || templateList.size() == 0) try {
            templateList = dataInterface.loadSavedTemplates();
        } catch (IOException e) {
            System.err.println("Error loading templates, returning empty template list");
            templateList = new ArrayList<Template>();
        }
        return templateList;
    }

    /**
	 * Retrieves the current start & end date for the data view.   
	 * @return Date[] array of size 2 containing start and end dates. 
	 */
    public Date[] getTimeRange() {
        return timeRange;
    }

    /**
	 * This should be called when any changes are made to the data grid. 
	 * For example, if rows or columns are reordered, or if the number 
	 * of rows or columns changes
	 *
	 */
    private void gridChanged() {
        for (int i = 0; i <= 1; i++) if (lrd.getSplitAxis(i).getSize() == 0) return;
        reMarkAllCriticals();
        for (Iterator<FocusGroupInterface> it = sourceFocusGroups.iterator(); it.hasNext(); ) {
            it.next().setStale(true);
        }
        for (Iterator<FocusGroupInterface> it = stripFocusGroups.iterator(); it.hasNext(); ) {
            it.next().setStale(true);
        }
    }

    public boolean hasData() {
        if (this.activeStripSet != null && this.activeStripSet.size() != 0 && this.activeSourceMap != null && this.activeSourceMap.size() != 0) return true;
        return false;
    }

    /**
	 * Begin the data connection.   
	 * @return false if connection failed
	 */
    public boolean connectDataInterface(JFrame mainFrame) {
        this.dataInterface = new DataServiceDispatcher(this);
        Date[] result;
        try {
            result = dataInterface.connect(mainFrame);
        } catch (IOException e) {
            disconnect();
            ConnectionError.showErrorDialog(e);
            return false;
        }
        if (result == null) {
            disconnect();
            System.err.println("Connection cancelled");
            return false;
        }
        timeRange = result;
        dataInterface.go();
        return true;
    }

    public boolean isFirstLoad() {
        return firstLoad;
    }

    public List<SourceGroupInterface> getAllSourceGroupList() {
        return dataInterface.getAllSourceGroups();
    }

    /**
	 * Retrieves a complete strip list from the backing store.  
	 * 
	 * Warning: replaces local copy of StripList.  Changes not saved to the backing store
	 * before this call will be discarded. 
	 * 
	 * @return
	 */
    public List<Strip> loadStripList() {
        List<Strip> stripList;
        try {
            stripList = dataInterface.loadSavedStrips();
        } catch (IOException e) {
            System.err.println("Unable to load strips from backing store.");
            e.printStackTrace();
            return new ArrayList<Strip>();
        }
        for (Iterator<Strip> it = stripList.iterator(); it.hasNext(); ) {
            final Strip s = it.next();
            s.bind(loadTemplateList(), dataInterface.getInternalNameToInputChannel(), dataInterface.getInternalNameToInputChannelGroup());
            if (s.isBound()) {
                final Template t = s.getTemplate();
                t.addChangeListener(templateChangeListener);
            }
        }
        return stripList;
    }

    /**
	 * Helper method that loads focus groups from the backing store, 
	 * sorts them into sourceFocusGroups and stripFocusGroups
	 * 
	 * @throws IOException
	 */
    private void loadFocusGroups() throws IOException {
        sourceFocusGroups = new ArrayList<FocusGroupInterface>();
        stripFocusGroups = new ArrayList<FocusGroupInterface>();
        List<FocusGroupInterface> loadedGroups = dataInterface.loadSavedFocusGroups();
        Iterator<FocusGroupInterface> it = loadedGroups.iterator();
        while (it.hasNext()) {
            FocusGroupInterface fg = it.next();
            if (fg.getType() == AbstractFocusGroup.SOURCEGROUP) sourceFocusGroups.add(fg); else stripFocusGroups.add(fg);
        }
        LiveRAC.makeLogEntry(LogEntry.LOAD_SOURCE_FOCUS_GROUP, "Load source focus groups from file.", sourceFocusGroups);
        LiveRAC.makeLogEntry(LogEntry.LOAD_STRIP_FOCUS_GROUP, "Load strip focus groups from file.", stripFocusGroups);
    }

    /**
	 * Loads sources from the server that have been defined in the groupList
	 *
	 */
    private boolean gridSources(List<SourceInterface> selectedSourceList, List<SourceGroupInterface> selectedGroupList) throws IOException {
        if (dataInterface == null || selectedGroupList == null || selectedSourceList == null) throw new IOException("loadSelectedSources: invalid dataInterface or source selections");
        if (selectedGroupList.size() == 0 && selectedSourceList.size() == 0) return true;
        activeSourceMap = new HashMap<String, SourceInterface>();
        ActiveSourceQueryInterface q = new ActiveSourceQuery(selectedGroupList, selectedSourceList);
        Set<SourceInterface> sourceList = dataInterface.loadActiveSources(q);
        Iterator<SourceInterface> it = sourceList.iterator();
        while (it.hasNext()) {
            final SourceInterface newSource = it.next();
            insertSourceIntoGrid(newSource);
        }
        LiveRAC.makeLogEntry(LogEntry.SOURCE_LOAD_TYPE, "Load sources from server.", activeSourceMap);
        fireStateChanged(SOURCE_CHANGED, GridChangedEvent.ADD);
        return true;
    }

    /**
	 * 
	 * @param newStrip Strip to be inserted
	 */
    private void insertStripIntoGrid(Strip newStrip) {
        if (newStrip == null) {
            System.err.println("Null strip insert attempt, ignoring.");
            return;
        } else if (activeStripSet.contains(newStrip)) {
            System.err.println("Duplicate strip insert: " + newStrip + ", ignoring.");
            return;
        } else if (!newStrip.isActive() || !newStrip.isBound()) {
            System.err.println("Strip: " + newStrip + " is not yet bound.  Ignoring.");
            return;
        }
        SplitAxis stripAxis = lrd.getStripAxis();
        Comparator<Strip> cr = new Comparator<Strip>() {

            private List<Strip> stripList = getStripList();

            public int compare(Strip o1, Strip o2) {
                Integer o1pos = stripList.indexOf(o1);
                Integer o2pos = stripList.indexOf(o2);
                return o1pos.compareTo(o2pos);
            }
        };
        if (this.activeStripSet.size() == 0) {
            final SplitLine newline = stripAxis.getMinStuckLine();
            newline.setRowObject(newStrip);
            newStrip.setMinLine(newline);
        } else {
            SplitLine newLine = new SplitLine();
            final SplitLine minStuckLine = stripAxis.getMinStuckLine();
            final Strip minStuckStrip = (Strip) minStuckLine.getRowObject();
            if (cr.compare(newStrip, minStuckStrip) < 0) {
                newLine.setRowObject(minStuckStrip);
                minStuckStrip.setMinLine(newLine);
                minStuckLine.setRowObject(newStrip);
                newStrip.setMinLine(minStuckLine);
                stripAxis.putAt(newLine, minStuckLine);
            } else {
                newLine.setRowObject(newStrip);
                newStrip.setMinLine(newLine);
                final SplitLine adjSplit = findLeftAdjacentStripSplit(stripAxis, newStrip, cr);
                stripAxis.putAt(newLine, adjSplit);
            }
        }
        activeStripSet.add(newStrip);
        List<InputChannelItemInterface> stripChannels = newStrip.getChannels();
        for (Iterator<InputChannelItemInterface> channelIt = stripChannels.iterator(); channelIt.hasNext(); ) {
            final InputChannelItemInterface channel = channelIt.next();
            Set<Strip> stripSet = inputChannelItemToStripSet.get(channel);
            if (stripSet == null) {
                stripSet = new HashSet<Strip>(5);
                inputChannelItemToStripSet.put(channel, stripSet);
            }
            stripSet.add(newStrip);
        }
    }

    private void insertSourceIntoGrid(SourceInterface newSource) {
        final SplitAxis sourceAxis = lrd.getSourceAxis();
        if (newSource == null || activeSourceMap.containsKey(newSource.getInternalName())) {
            if (newSource == null) System.err.println("Null source"); else System.err.println("Duplicate source: " + newSource);
            return;
        }
        if (AbstractSource.getGroupMetaKey() != null) {
            String sourcesortkey = newSource.getMetaValue(AbstractSource.getGroupMetaKey());
            if (sourcesortkey != null) sourceGroupValueSet.add(sourcesortkey);
        }
        sourceMetaKeySet.addAll(newSource.getMetaKeys());
        newSource.setAd(lrd);
        if (this.activeSourceMap.size() == 0) {
            SplitLine newline = sourceAxis.getMinStuckLine();
            newline.setRowObject(newSource);
            newSource.setMinLine(newline);
        } else {
            SplitLine newLine = new SplitLine();
            SplitLine minStuckLine = sourceAxis.getMinStuckLine();
            SourceInterface minStuckSource = (SourceInterface) minStuckLine.getRowObject();
            if (newSource.compareTo(minStuckSource) < 0) {
                newLine.setRowObject(minStuckSource);
                minStuckSource.setMinLine(newLine);
                minStuckLine.setRowObject(newSource);
                newSource.setMinLine(minStuckLine);
                sourceAxis.putAt(newLine, minStuckLine);
            } else {
                newLine.setRowObject(newSource);
                newSource.setMinLine(newLine);
                final SplitLine adjSplit = findLeftAdjacentSourceSplit(sourceAxis, newSource);
                sourceAxis.putAt(newLine, adjSplit);
            }
        }
        this.activeSourceMap.put(newSource.getInternalName(), newSource);
    }

    /**
	 * How many sources are we monitoring?
	 * 
	 * @return
	 */
    public int getNumActiveSources() {
        if (activeSourceMap == null) return 0;
        return activeSourceMap.size();
    }

    /**
	 * Returns the number of strips currently being monitored.  
	 * 
	 * @return
	 */
    public int getNumActiveStrips() {
        if (activeStripSet == null) return 0;
        return activeStripSet.size();
    }

    /**
	 * Iterate through the queue of returned chart data, and add that data to
	 * the chart objects
	 * @param l 
	 */
    private void processChartData() {
        if (this.detailQueue.size() == 0) return;
        final List<DetailRecord> results = this.detailQueue.get(this.detailQueue.size() - 1);
        if (results == null || results.size() == 0) return;
        final Iterator<DetailRecord> resultIt = results.iterator();
        while (resultIt.hasNext()) {
            final DetailRecord resultSet = resultIt.next();
            final SourceInterface s = resultSet.getSource();
            s.addCellDetailData(resultSet);
        }
        final long queryId = results.get(0).getQueryId();
        DataGrid.detailQueryId = queryId;
        this.detailQueue.clear();
    }

    /**
	 * Process all the data that has been received from the data service in the
	 * queues
	 */
    public boolean processDataResponses() {
        boolean newData = false;
        if (swatchQueue.size() != 0 || detailQueue.size() != 0) {
            newData = true;
            if (swatchQueue.size() != 0) {
                final long swatchQueryId = this.processSwatchData();
                Iterator<SourceInterface> it = activeSourceMap.values().iterator();
                while (it.hasNext()) it.next().endSwatchUpdate(swatchQueryId);
                DataGrid.swatchQueryId = swatchQueryId;
            }
            if (detailQueue.size() != 0) {
                processChartData();
            }
        }
        return newData;
    }

    private long processSwatchData() {
        this.lrd.getGroupRanges().clearAllGroups();
        final List<SwatchRecordInterface> results = this.swatchQueue.get(this.swatchQueue.size() - 1);
        if (results == null || results.size() == 0) return -1l;
        final Iterator<SwatchRecordInterface> swatchIt = results.iterator();
        while (swatchIt.hasNext()) {
            final SwatchRecordInterface swatch = swatchIt.next();
            final SourceInterface source = swatch.getSource();
            source.addSwatchData(swatch);
        }
        this.swatchQueue.clear();
        final long queryId = results.get(0).getQueryId();
        return queryId;
    }

    /**
	 * Scan through all sources and add critical ranges back
	 * 
	 */
    public void reMarkAllCriticals() {
        if (activeSourceMap == null || activeSourceMap.size() == 0) return;
        lrd.getGroupRanges().clearGroup(Groups.critGroup);
        final Iterator<SourceInterface> it = this.activeSourceMap.values().iterator();
        while (it.hasNext()) {
            SourceInterface s = it.next();
            Iterator<Cell> cit = s.getCells().values().iterator();
            while (cit.hasNext()) {
                Cell cell = cit.next();
                cell.checkAddToCritGroup();
            }
        }
    }

    /**
	 * Called when one of the defined templates gets changed
	 */
    private void templateChanged() {
        lrd.forceRedraw();
    }

    /**
	 * Requests the data from the data service depending on the 
	 * query type.  
	 * 
	 * @param req
	 */
    public void requestData(final QueryInterface req) {
        try {
            if (req instanceof SwatchQueryInterface) this.dataInterface.swatchQuery((SwatchQueryInterface) req); else if (req instanceof DetailQueryInterface) this.dataInterface.detailQuery((DetailQueryInterface) req); else System.err.println("Unrecognized query:" + req);
        } catch (IOException exception) {
            ConnectionError.showErrorDialog(exception);
            LiveRAC.getInstance().disconnectLRD();
        }
    }

    /**
	 * This gets called when data is first loaded or when any aspect of 
	 * strips / templates get changed and it's necessary to reload all data
	 *
	 */
    public void resetDisplay() {
        LiveRAC lr = lrd.getLr();
        lrd.setSourceFocusGroupValue(null);
        lrd.setStripFocusGroupIndex(-2);
        AccordionDrawer.loaded = false;
        setFirstLoad(false);
        lr.getUI().destroyProgressDialog();
        lr.getUI().showProgressDialog("Loading data from: " + dataInterface.getConnectionName());
        lr.getDrawPanel().remove(lrd.getCanvas());
        flushData();
        sourceGroupValueSet = new TreeSet<String>();
        try {
            selectedSourceGroupList = dataInterface.loadSavedSourceGroups();
            selectedSourceList = dataInterface.loadSavedSources();
            gridSources(selectedSourceList, selectedSourceGroupList);
            loadTimeRangeSampleIntervalRelations();
            stripList = loadStripList();
            loadFocusGroups();
        } catch (IOException e) {
            lr.getUI().destroyProgressDialog();
            ConnectionError.showErrorDialog(e);
        }
        gridStrips();
        gridChanged();
        final int numSources = getNumActiveSources();
        final int numStrips = getNumActiveStrips();
        if (numSources > 0 && numStrips > 0) {
            lrd.initializeFont();
            if (loggers[0] != null || loggers[1] != null) {
                VisualLogRequest v = new VisualLogRequest(LiveRAC.getInstance().getMainFrame(), loggers.clone());
            }
            LiveRACSplitAxisLogger xLogger = null;
            LiveRACSplitAxisLogger yLogger = null;
            if (LiveRAC.LOGGING) {
                final String path = dataInterface.getLocalDataPath();
                final String prefix = LiveRACSplitAxisLogger.getFileName();
                try {
                    xLogger = new LiveRACSplitAxisLogger(prefix, path, SplitAxisLogger.X);
                } catch (IOException e1) {
                    System.err.println("IO Error initializing X axis logger");
                    System.err.println(e1.getMessage());
                    xLogger = null;
                }
                try {
                    yLogger = new LiveRACSplitAxisLogger(prefix, path, SplitAxisLogger.Y);
                } catch (IOException e1) {
                    System.err.println("IO Error initializing Y axis logger");
                    System.err.println(e1.getMessage());
                    yLogger = null;
                }
                lrd.getSourceAxis().setLogger(yLogger);
                lrd.getStripAxis().setLogger(xLogger);
                loggers[AccordionDrawer.X] = xLogger;
                loggers[AccordionDrawer.Y] = yLogger;
            }
            lrd.reset();
            AccordionDrawer.loaded = true;
            lrd.incrementFrameNumber();
            lr.getDrawPanel().add(lrd.getCanvas());
            swatchRequest();
        } else {
            if (!firstLoad) {
                firstLoad = true;
                lrd.getLr().getUI().destroyProgressDialog();
                NoDataError.showDataErrorDialog((numSources == 0) ? NoDataError.NO_SOURCES_TYPE : NoDataError.NO_BOUND_STRIP_TYPE);
            }
        }
    }

    /**
	 * Retrieves the set of valid timerangeinterval samples
	 */
    private void loadTimeRangeSampleIntervalRelations() throws IOException {
        sortedTimeRangeSampleRelationSet = dataInterface.getTimeRangeSampleIntervalRelations();
    }

    /**
	 * Inserts the loaded strips into the strip SplitAxis
	 */
    private void gridStrips() {
        activeStripSet = new HashSet<Strip>();
        inputChannelItemToStripSet = new HashMap<InputChannelItemInterface, Set<Strip>>();
        for (Iterator<Strip> it = stripList.iterator(); it.hasNext(); ) {
            final Strip strip = it.next();
            insertStripIntoGrid(strip);
        }
        fireStateChanged(STRIP_CHANGED, GridChangedEvent.ADD);
    }

    /**
	 * Provides the set of strips which are using this input channel. 
	 * 
	 * @param channel
	 * @return
	 */
    public Set<Strip> getStripSet(InputChannelItemInterface channel) {
        return inputChannelItemToStripSet.get(channel);
    }

    public void saveFocusGroups() {
        List<FocusGroupInterface> focusGroups = new ArrayList<FocusGroupInterface>(stripFocusGroups.size() + sourceFocusGroups.size() + 1);
        focusGroups.addAll(sourceFocusGroups);
        focusGroups.addAll(stripFocusGroups);
        try {
            dataInterface.saveFocusGroups(focusGroups);
        } catch (IOException e) {
            System.err.println("IO Error Saving Focus Groups");
        }
    }

    /**
	 * Saves the arrangement of the strips to the backing store
	 * (Right now this does a complete save of all strip groups, in the 
	 * future may support an interface for only saving the ordering.) 
	 */
    public void saveStripArrangements() {
        List<Strip> strips = new ArrayList<Strip>(stripList.size());
        final SplitAxis stripAxis = lrd.getStripAxis();
        for (Iterator<SplitLine> it = stripAxis.iterator(); it.hasNext(); ) {
            final SplitLine line = it.next();
            if (line == null) continue;
            final Strip strip = (Strip) line.getRowObject();
            if (strip != null) strips.add(strip);
        }
        for (Iterator<Strip> it = stripList.iterator(); it.hasNext(); ) {
            final Strip strip = it.next();
            if (strip.isActive()) continue;
            strips.add(strip);
        }
        saveStrips(strips);
        saveSelectedSourceAndGroupLists(getSelectedSourceList(), getSelectedSourceGroupList(), false);
    }

    /**
	 * Saves the list of strips to the backing store.  Order of the 
	 * strips is preserved.  
	 * @return
	 */
    public boolean saveStrips(List<Strip> strips) {
        try {
            dataInterface.saveStrips(strips);
        } catch (IOException e) {
            System.err.println("Error saving strips");
            return false;
        }
        return true;
    }

    /**
	 * Saves the list of templates to the backing store through the 
	 * dataInterface.  
	 * @return
	 */
    public boolean saveTemplates() {
        try {
            dataInterface.saveTemplates(loadTemplateList());
        } catch (IOException e) {
            System.err.println("Error saving templates");
            return false;
        }
        return true;
    }

    public void setFirstLoad(boolean firstLoad) {
        this.firstLoad = firstLoad;
    }

    public void setLoadingState(boolean state) {
        lrd.getLr().getUI().setLoadingState(state);
    }

    /**
	 * Sets the amount in seconds each refresh will advance
	 * @param refreshAmount
	 */
    public void setRefreshAmount(final int refreshAmount) {
        this.refreshAmount = refreshAmount;
    }

    /**
	 * @param refreshRate
	 *            The refreshRate to set.
	 */
    public void setRefreshRate(final int refreshRate) {
        this.refreshRate = refreshRate;
    }

    /**
	 * Set (and save to backing store) the selected list of source groups, this will have to trigger 
	 * a reload of the selected sources. 
	 * 
	 * @param sourceList
	 * @param groupList
	 * @param reload Whether a reload is necessary. Should be true only if sourceList or groupList has changed. 
	 * @return
	 */
    public boolean saveSelectedSourceAndGroupLists(List<SourceInterface> sourceList, List<SourceGroupInterface> groupList, boolean reload) {
        selectedSourceList = sourceList;
        selectedSourceGroupList = groupList;
        try {
            dataInterface.saveSelectedSourcesAndGroups(groupList, sourceList, new ArrayList<SourceInterface>(), AbstractSource.getGroupMetaKey());
        } catch (IOException e) {
            System.err.println("Error saving selected source and groups.");
        }
        if (reload) resetDisplay();
        return true;
    }

    /**
	 * 
	 * Re-sort the source split lines. The preferred sort comparison operator
	 * must be set on the source before calling this method. 
	 * 
	 * @see SourceInterface#setSortComparable
	 * 
	 * Performance: TODO: Unfortunately we do not sort the list in place: log n + n log n
	 * 
	 * @param sortFieldMetaKey specify which meta key to use for sorting.  Note: null is a valid key, 
	 * in this case the sources are either sorted by the cell values down a specific Strip (if set)
	 * or by source name. 
	 * 
	 */
    public void sort() {
        AccordionLRACDrawer.loaded = false;
        final TreeSet<SourceInterface> sourceTree = new TreeSet<SourceInterface>();
        final SplitAxis sourceAxis = lrd.getSourceAxis();
        for (Iterator<SplitLine> it = sourceAxis.iterator(); it.hasNext(); ) {
            SplitLine currLine = it.next();
            if (currLine.rowObject != null) {
                sourceTree.add((SourceInterface) currLine.rowObject);
            }
        }
        SplitLine currLine = sourceAxis.getMinStuckLine();
        for (Iterator<SourceInterface> it = sourceTree.iterator(); it.hasNext(); ) {
            SourceInterface source = it.next();
            source.setMinLine(currLine);
            currLine.setRowObject(source);
            currLine.setCullingObject(null);
            currLine = sourceAxis.getNextSplit(currLine);
        }
        AccordionLRACDrawer.loaded = true;
        lrd.forceRedraw();
    }

    /**
	 * Retrieves the complete set of meta keys for all active sources.
	 * @return
	 */
    public Set<String> getSourceMetaKeyList() {
        HashSet<String> resultSet = new HashSet<String>();
        for (Iterator<SourceInterface> it = activeSourceMap.values().iterator(); it.hasNext(); ) {
            final SourceInterface source = it.next();
            resultSet.addAll(source.getMetaKeys());
        }
        return resultSet;
    }

    /**
	 * Starts the timer object running. Use setRefreshRate to set the timer
	 * interval before calling this method.
	 * 
	 */
    public void startTimer() {
        if (this.updateTimer == null) this.updateTimer = new Timer("UpdateTimer"); else {
            this.updateTimer.cancel();
            this.updateTimer = new Timer("UpdateTimer");
        }
        final TimerTask task = new DataTimerTask();
        this.updateTimer.schedule(task, getRefreshRate() * 1000, getRefreshRate() * 1000);
        Util.dprint("Scheduled timer!");
    }

    /**
	 * Stops the timer object.
	 * 
	 */
    public void stopTimer() {
        if (updateTimer != null) this.updateTimer.cancel();
    }

    /**
	 * In the splitAxis swap the specified sources
	 * 
	 * @param firstIndex
	 * @param secondIndex
	 */
    public void swapSources(final int firstIndex, final int secondIndex) {
        DataGrid.incrementSwatchQueryId();
        this.lrd.incrementFrameNumber();
        lrd.getGroupRanges().clearAllGroups();
        final SplitAxis axis = this.lrd.getSourceAxis();
        final SplitLine firstLine = axis.getSplitFromIndex(firstIndex);
        final SplitLine secondLine = axis.getSplitFromIndex(secondIndex);
        final SourceInterface firstDev = (SourceInterface) firstLine.getRowObject();
        final SourceInterface secondDev = (SourceInterface) secondLine.getRowObject();
        LiveRAC.makeLogEntry(LogEntry.ARRANGE_SOURCES, "Manual arrangement, swapping (" + firstDev + ", " + secondDev + ")", null);
        firstLine.setRowObject(secondDev);
        secondDev.setMinLine(firstLine);
        secondLine.setRowObject(firstDev);
        firstDev.setMinLine(secondLine);
        gridChanged();
        this.lrd.requestRedraw();
    }

    public void swapStrips(final Strip startStrip, final Strip targetStrip) {
        DataGrid.incrementSwatchQueryId();
        this.lrd.incrementFrameNumber();
        lrd.getGroupRanges().clearAllGroups();
        LiveRAC.makeLogEntry(LogEntry.ARRANGE_STRIPS, "Strip swap (" + startStrip + ", " + targetStrip + ")", null);
        final SplitLine startLine = startStrip.getMinLine();
        final SplitLine targetLine = targetStrip.getMinLine();
        startLine.setRowObject(targetStrip);
        targetStrip.setMinLine(startLine);
        targetLine.setRowObject(startStrip);
        startStrip.setMinLine(targetLine);
        gridChanged();
        this.lrd.requestRedraw();
    }

    public void swatchQueryResult(List<SwatchRecordInterface> results) {
        if (results == null) return;
        this.swatchQueue.add(results);
        if (!firstLoad) {
            firstLoad = true;
            lrd.getLr().getUI().destroyProgressDialog();
            if (firstTimeEver) {
                firstTimeEver = false;
            }
        }
        this.lrd.forceRedraw();
        this.lrd.forceRedraw();
    }

    /**
	 * Helpful parameter-free wrapper method for queueing a swatch query
	 */
    public void swatchRequest() {
        final SwatchQuery req = new SwatchQuery(getBeginDate(), getEndDate(), selectedSourceList, selectedSourceGroupList, activeStripSet, getSampleInterval(), DataGrid.getNextSwatchQueryId());
        this.requestData(req);
    }

    public Set<Strip> getActiveStripSet() {
        return activeStripSet;
    }

    public boolean isTimeChanged() {
        return timeChanged;
    }

    public void setTimeChanged(boolean timeChanged) {
        this.timeChanged = timeChanged;
    }

    public TreeSet<String> getSourceGroupValueSet() {
        return sourceGroupValueSet;
    }

    /**
	 * Specifies a new time range.
	 * @param newTimeRange An array of size 2 of type Date []
	 */
    public void setTimeRange(Date[] newTimeRange) {
        if (newTimeRange == null || newTimeRange.length != 2) {
            System.err.println("Invalid time range specified");
            return;
        }
        this.timeRange = newTimeRange;
    }

    /**
	 * Retrieves a newly created Focus Group implementation from the 
	 * data service. 
	 * 
	 * @param focusGroupName
	 * @param type
	 * @return
	 */
    public FocusGroupInterface newFocusGroup(String focusGroupName, int type) {
        return dataInterface.newFocusGroup(focusGroupName, type);
    }

    /**
	 * Retrieve the userId used in connecting to the backing store. 
	 * @return
	 */
    public String loadUserId() {
        return dataInterface.getUserId();
    }

    /**
	 * Merge individaul input channels and input channel groups into a single list
	 * @return
	 */
    public List<InputChannelItemInterface> getInputChannelItemList() {
        List<InputChannelInterface> inputChannels = dataInterface.getInputChannels();
        List<InputChannelGroupInterface> inputChannelGroups = dataInterface.getInputChannelGroups();
        List<InputChannelItemInterface> mergedList = new ArrayList<InputChannelItemInterface>(inputChannels.size() + inputChannelGroups.size());
        mergedList.addAll(inputChannels);
        mergedList.addAll(inputChannelGroups);
        return mergedList;
    }

    /**
	 * Retrieve a channel by name 
	 * @param channelName
	 * @return
	 */
    public InputChannelInterface getInputChannelByName(String channelName) {
        return dataInterface.getInternalNameToInputChannel().get(channelName);
    }

    /**
	 * Retrieves an appropriate HSSFWork using the dataInterface.  Although this is implemented as a load
	 * call, and is a synchronous request from the dataInterface, it should return very quickly.  The 
	 * data interface generally should not have to make any additional requests to the backing store to 
	 * produce the Excel file.  
	 * 
	 * @param cells
	 * @return
	 */
    public HSSFWorkbook makeExcelExport(List<DataCellInterface> cells) {
        return dataInterface.makeExcelExport(cells);
    }

    /**
     * Adds a <code>ChangeListener</code> to the template.
     * @param l the listener to be added
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    /**
     * Removes a ChangeListener from the template.
     * @param l the listener to be removed
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    /**
     * Returns an array of all the <code>ChangeListener</code>s added
     * to this Template with addChangeListener().
     *
     * @return all of the <code>ChangeListener</code>s added or an empty
     *         array if no listeners have been added
     */
    public ChangeListener[] getChangeListeners() {
        return (ChangeListener[]) (listenerList.getListeners(ChangeListener.class));
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance 
     * is lazily created.
     * @see EventListenerList
     * 
     * @param changeType One of {@link #SOURCE_CHANGED} or {@link #STRIP_CHANGED}
     * @param eventType What type of event happened, one of {@link GridChangedEvent#ADD} {@link GridChangedEvent#DELETE} ...
     */
    protected void fireStateChanged(int changeType, int eventType) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeType == SOURCE_CHANGED) {
                    if (sourceGridEvent == null) sourceGridEvent = new SourceGridEvent(this, eventType);
                    ((ChangeListener) listeners[i + 1]).stateChanged(sourceGridEvent);
                } else {
                    if (stripGridEvent == null) stripGridEvent = new StripGridEvent(this, eventType);
                    ((ChangeListener) listeners[i + 1]).stateChanged(stripGridEvent);
                }
            }
        }
    }

    /**
	 * Use the backing store to retrieve an AbstractCellDataView  
	 * that is used for viewing details concerning a cell.  
	 * 
	 * @param cell
	 * @return
	 */
    public AbstractCellViewer getCellViewer(DataCellInterface cell, JFrame parent) {
        return dataInterface.getCellViewer(cell, parent);
    }

    public SourceInterface getActiveSource(String internalName) {
        return activeSourceMap.get(internalName);
    }

    /**
	 * A helper method for retrieving a strip by name.  Note it is only O(n) efficient where n
	 * is the number of active strips (should be small).  
	 * 
	 * @param stripName
	 * @return
	 */
    public Strip getActiveStrip(String stripName) {
        for (Iterator<Strip> it = activeStripSet.iterator(); it.hasNext(); ) {
            final Strip strip = it.next();
            if (strip.getName().equals(stripName)) return strip;
        }
        return null;
    }

    public Map<String, SourceInterface> getActiveSourceMap() {
        return activeSourceMap;
    }

    public static long getDetailQueryId() {
        return detailQueryId;
    }

    public static long getNextDetailQueryId() {
        return System.currentTimeMillis();
    }

    public static long getSwatchQueryId() {
        return swatchQueryId;
    }

    public static long getNextSwatchQueryId() {
        return System.currentTimeMillis();
    }

    public static void incrementSwatchQueryId() {
        swatchQueryId = System.currentTimeMillis();
    }

    /**
	 * Retrieve the complete list of all sources that 
	 * can be picked from individually.  In many cases
	 * this may be an empty list and only source groups
	 * will be permitted.    
	 * 
	 * @return
	 */
    public Collection<SourceInterface> getAllSourceList() {
        return dataInterface.getAllSources();
    }

    /**
	 * Retrieve the list of sources which have been selected
	 * individually by the user in the source selection dialog.  
	 * 
	 * This may be an empty list, for many applications it 
	 * only makes sense to pick sources by group, not individually. 
	 * 
	 * @return
	 */
    public List<SourceInterface> getSelectedSourceList() {
        return selectedSourceList;
    }

    /**
	 * @return the loggers
	 */
    public LiveRACSplitAxisLogger[] getLoggers() {
        return loggers;
    }
}

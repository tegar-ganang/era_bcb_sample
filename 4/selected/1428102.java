package net.sourceforge.olduvai.lrac.drawer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import javax.swing.JOptionPane;
import net.sourceforge.olduvai.accordiondrawer.AccordionDrawer;
import net.sourceforge.olduvai.accordiondrawer.SplitAxis;
import net.sourceforge.olduvai.accordiondrawer.SplitLine;
import net.sourceforge.olduvai.lrac.AccordionLRACDrawerFinal;
import net.sourceforge.olduvai.lrac.LiveRAC;
import net.sourceforge.olduvai.lrac.dataservice.DataServiceThread;
import net.sourceforge.olduvai.lrac.dataservice.ProcessedChartDataResults;
import net.sourceforge.olduvai.lrac.dataservice.ResultInterface;
import net.sourceforge.olduvai.lrac.dataservice.queries.AbstractQuery;
import net.sourceforge.olduvai.lrac.dataservice.queries.CellSwatchQuery;
import net.sourceforge.olduvai.lrac.dataservice.queries.SourceGroupQuery;
import net.sourceforge.olduvai.lrac.dataservice.queries.SourceQuery;
import net.sourceforge.olduvai.lrac.dataservice.records.CellSwatchRecord;
import net.sourceforge.olduvai.lrac.dataservice.records.SourceAlarmDataResult;
import net.sourceforge.olduvai.lrac.dataservice.records.SourceGroup;
import net.sourceforge.olduvai.lrac.drawer.strips.Strip;
import net.sourceforge.olduvai.lrac.drawer.strips.StripChannelGroup;
import net.sourceforge.olduvai.lrac.drawer.strips.StripHandler;
import net.sourceforge.olduvai.lrac.drawer.templates.Template;
import net.sourceforge.olduvai.lrac.drawer.templates.TemplateHandler;
import net.sourceforge.olduvai.lrac.logging.LogEntry;
import net.sourceforge.olduvai.lrac.ui.UI;
import net.sourceforge.olduvai.lrac.util.Util;

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
public class DataStore implements ResultInterface {

    public static final int STRIPESTIMATE = 30;

    public static final int SOURCEESTIMATE = 1000;

    boolean firstLoad = false;

    static boolean firstTimeEver = true;

    /**
	 * Pointer to the drawer
	 */
    AccordionLRACDrawer lrd;

    /**
	 * Main datastructure for sources for fast lookup by name
	 */
    HashMap<String, Source> sourceTable = new HashMap<String, Source>(SOURCEESTIMATE);

    /**
	 * Maps a channel name to a list of strips. Used to process raw input
	 * metric values to assign them to the correct palettes (columns) which then
	 * allow them to use the source string to find the correct row. The
	 * intersection of the source & strip is a series of Cell
	 * objects.  Strip names are also entered into th map.  
	 */
    HashMap<String, HashSet<Strip>> channelStripMap;

    /**
	 * Complete list of strips that any source COULD have. Note: The order of
	 * this ArrayList defines the order of the columns
	 * 
	 * @see allMetricsIndex
	 */
    ArrayList<Strip> stripOrderedList = new ArrayList<Strip>(DataStore.STRIPESTIMATE);

    /**
	 * This table stores index numbers by palette for rapid lookup. Any
	 * operation that effects one must effect the other
	 */
    HashMap<Strip, Integer> stripIndexTable = new HashMap<Strip, Integer>(DataStore.STRIPESTIMATE);

    /**
	 * A list of the groups that are available for selection by the user
	 * (loaded from server)
	 */
    ArrayList<SourceGroup> availGroupList;

    /**
	 * A list of the user selected (or predefined) group identifiers of sources
	 */
    ArrayList<SourceGroup> groupList;

    /**
	 * Contains an ordered set of keys for the current source sort parameter.  
	 * This is used by the 'next group grow' call.   
	 */
    TreeSet<String> sourceSortKeySet = new TreeSet<String>();

    /**
	 * Stores a list of source focus group
	 */
    List<FocusGroup> sourceFocusGroups = new ArrayList<FocusGroup>();

    /**
	 * Stores a list of strip focus groups
	 */
    List<FocusGroup> stripFocusGroups = new ArrayList<FocusGroup>();

    /**
	 * Data refresh rate in seconds
	 */
    int refreshRate = 15;

    /**
	 * Seconds to advance with each refresh
	 */
    int refreshAmount = 300;

    /**
	 * Store how many data updates have been received from the server. This can
	 * be used by various other services to cache information about the state of
	 * the underlying data
	 */
    long updateNumber = 1;

    /**
	 * Store whether a redraw is due to movement of the timeSlider or
	 * timeSpinners. This gets reset to false at the end of every draw cycle in
	 * drawPostFrame and gets set to true in the UI class whenever there is
	 * activity on the slider or spinners.
	 */
    public boolean timeChanged = false;

    /**
	 * An array of size 2 to store the beginDate and endDate of the time range
	 * currently being monitored
	 */
    public Date[] timeRange = { null, null };

    /**
	 * Threaded handler for managing requests
	 */
    DataServiceThread dataInterface;

    /**
	 * Run every refreshRate ms to check for new alarms
	 */
    Timer updateTimer;

    List<HashMap<String, ProcessedChartDataResults>> chartDataQueue = Collections.synchronizedList(new ArrayList<HashMap<String, ProcessedChartDataResults>>());

    List<Map<String, SourceAlarmDataResult>> alarmDataQueue = Collections.synchronizedList(new ArrayList<Map<String, SourceAlarmDataResult>>());

    List<ArrayList<CellSwatchRecord>> swatchQueue = Collections.synchronizedList(new ArrayList<ArrayList<CellSwatchRecord>>());

    private StripHandler stripHandler;

    private TemplateHandler templateHandler;

    /**
	 * Store a cached pointer to the 'alarm' strip
	 */
    private static Strip cachedAlarmStrip = null;

    /**
	 * Creates a new DataStore and associates it with the specified drawer.
	 * 
	 * @param lrd
	 */
    public DataStore(final AccordionLRACDrawer lrd) {
        this.lrd = lrd;
    }

    /**
	 * @param channel String name of the input channel. 
	 * @return The list of strips associated with the specified input channel
	 * 
	 * This method checks both any prefix of the specified channel as well as 
	 * the full channel name.  If no strips are found an empty list is returned. 
	 * 
	 */
    public List<Strip> getChannelStripList(final String channel) {
        HashSet<Strip> channelStripSet = this.channelStripMap.get(channel);
        final String channelPrefix = StripChannelGroup.getChannelPrefix(channel);
        HashSet<Strip> channelPrefixStripSet = null;
        if (channelPrefix != null) channelPrefixStripSet = this.channelStripMap.get(channelPrefix);
        final int totalSize = ((channelStripSet != null) ? channelStripSet.size() : 0) + ((channelPrefixStripSet != null) ? channelPrefixStripSet.size() : 0);
        ArrayList<Strip> returnList = new ArrayList<Strip>(totalSize);
        if (channelStripSet != null) returnList.addAll(channelStripSet);
        if (channelPrefixStripSet != null) returnList.addAll(channelPrefixStripSet);
        return returnList;
    }

    /**
	 * Loads templates from the specified file
	 * 
	 * @return true if successful
	 */
    private boolean loadTemplates(final InputStream templateFileStream) {
        templateHandler = new TemplateHandler(templateFileStream);
        return true;
    }

    /**
	 * Loads in the strip list, also creates a column for each strip
	 * 
	 * @param stripFileName
	 * @param templateMap
	 *            A map of templates indexed by name
	 * @return
	 */
    private boolean loadStrips(final InputStream stripFileStream, final HashMap<String, Template> templateMap) {
        this.stripHandler = new StripHandler(stripFileStream, templateMap);
        this.channelStripMap = this.stripHandler.getChannelToStripMap();
        Iterator<Strip> it = stripHandler.getStripList().iterator();
        final SplitAxis stripAxis = lrd.stripAxis;
        while (it.hasNext()) {
            Strip p = it.next();
            addStrip(p, stripAxis);
        }
        LiveRAC.makeLogEntry(LogEntry.STRIP_LOAD_TYPE, "Load strips from server.", stripHandler.getStripList());
        return true;
    }

    private boolean loadAvailGroupList() throws IOException {
        if (dataInterface == null) throw new IOException("Unable to load available groups");
        SourceGroupQuery q = new SourceGroupQuery();
        availGroupList = dataInterface.getAvailGroups(q);
        return true;
    }

    /**
	 * Set (and save to file) the list of source groups, this will have to trigger 
	 * a reload of the selected sources. 
	 * 
	 * @param groupList
	 * @return
	 */
    public boolean setGroupList(ArrayList<SourceGroup> groupList) {
        this.groupList = groupList;
        SourceGroup.saveGroups(groupList, Source.getSortKey());
        resetDisplay();
        return true;
    }

    /**
	 * Loads sources from the server that have been defined in the groupList
	 *
	 */
    private boolean loadSources(ArrayList<SourceGroup> groupList) throws IOException {
        if (dataInterface == null || groupList == null) throw new IOException("Unable to load sources");
        if (groupList.size() == 0) return true;
        SourceQuery q = new SourceQuery(groupList);
        ArrayList<Source> sourceList = dataInterface.getSourceList(q);
        final SplitAxis sourceAxis = this.lrd.sourceAxis;
        Iterator<Source> it = sourceList.iterator();
        while (it.hasNext()) {
            final Source newSource = it.next();
            if (newSource == null || sourceTable.containsKey(newSource.sourceID)) {
                if (newSource == null) System.err.println("Null source"); else System.err.println("Duplicate source: " + newSource);
                continue;
            }
            sourceSortKeySet.add(newSource.getSortKeyField());
            newSource.setAd(lrd);
            if (this.sourceTable.size() == 0) {
                SplitLine newline = sourceAxis.getMinStuckLine();
                newline.setRowObject(newSource);
                newSource.setMinLine(newline);
            } else {
                SplitLine newLine = new SplitLine();
                SplitLine minStuckLine = sourceAxis.getMinStuckLine();
                Source minStuckSource = (Source) minStuckLine.getRowObject();
                if (newSource.compareTo(minStuckSource) < 0) {
                    newLine.setRowObject(minStuckSource);
                    minStuckSource.setMinLine(newLine);
                    minStuckLine.setRowObject(newSource);
                    newSource.setMinLine(minStuckLine);
                    sourceAxis.putAt(newLine, minStuckLine);
                } else {
                    newLine.setRowObject(newSource);
                    newSource.setMinLine(newLine);
                    final SplitLine adjSplit = findAdjacentSplit(sourceAxis, newLine);
                    sourceAxis.putAt(newLine, adjSplit);
                }
            }
            this.sourceTable.put(newSource.sourceID, newSource);
        }
        LiveRAC.makeLogEntry(LogEntry.SOURCE_LOAD_TYPE, "Load sources from server.", sourceTable.values());
        return true;
    }

    /**
	 * 
	 * Load the list of source groups the user has specified, or system defaults
	 * @param groupFileStream
	 * @return
	 */
    private boolean loadGroupList(final InputStream groupFileStream) {
        groupList = SourceGroup.loadGroups(groupFileStream);
        LiveRAC.makeLogEntry(LogEntry.GROUP_LOAD_TYPE, "Load groups from file", groupList);
        if (groupList == null) return false;
        return true;
    }

    /**
	 * This should be called when any changes are made to the data grid. 
	 * For example, if rows or columns are reordered, or if the number 
	 * of rows or columns changes
	 *
	 */
    private void gridChanged() {
        reMarkAllCriticals();
        Iterator<FocusGroup> it = sourceFocusGroups.iterator();
        while (it.hasNext()) {
            it.next().setStale(true);
        }
    }

    /**
	 * Binary search the split line tree using the comparable on the attached
	 * row objects
	 * 
	 * @param axis
	 * @param source
	 * @return line to the left. NEVER maxStuckLine()
	 */
    static final SplitLine findAdjacentSplit(final SplitAxis axis, final SplitLine newLine) {
        final Source newSource = (Source) newLine.rowObject;
        final SplitLine minStuckLine = axis.getMinStuckLine();
        final Source minStuckSource = (Source) minStuckLine.rowObject;
        if (newSource.compareTo(minStuckSource) < 0) {
            System.err.println("newSource before minStuckSource?  This should be handled already");
        }
        if (axis.getSize() == 0) return null;
        {
            final SplitLine nextAfterMinLine = axis.getNextSplit(minStuckLine);
            final Source nextAfterMinSource = (Source) nextAfterMinLine.rowObject;
            if (newSource.compareTo(minStuckSource) > 0 && newSource.compareTo(nextAfterMinSource) < 0) {
                return minStuckLine;
            }
        }
        return findAdjacentSplitRecurse(axis.getRoot(), newSource, axis);
    }

    static final SplitLine findAdjacentSplitRecurse(final SplitLine currLine, final Source compSource, final SplitAxis axis) {
        final Source currSource = (Source) currLine.getRowObject();
        final int compareResult = compSource.compareTo(currSource);
        if (compareResult < 0) {
            if (currLine.getLeftChild() == null) return axis.getPreviousSplit(currLine); else return findAdjacentSplitRecurse(currLine.getLeftChild(), compSource, axis);
        } else if (compareResult > 0) {
            if (currLine.getRightChild() == null) return currLine; else return findAdjacentSplitRecurse(currLine.getRightChild(), compSource, axis);
        } else {
            System.err.println("Source should never match! datastore.findAdjacentSplitRecurse()");
            return null;
        }
    }

    /**
	 * Retrieve the specified source, given its name
	 * 
	 * @param sourceName
	 * @return Specified source
	 */
    public Source getSource(final String sourceName) {
        return this.sourceTable.get(sourceName);
    }

    public void queueChartDataCallBack(final HashMap<String, ProcessedChartDataResults> results) {
        if (results == null) return;
        this.chartDataQueue.add(results);
        this.lrd.forceRedraw();
    }

    public void queueAlarmDataCallBack(Map<String, SourceAlarmDataResult> results) {
        if (results == null) return;
        alarmDataQueue.add(0, results);
        lrd.forceRedraw();
    }

    public void queueSliderCallBack(final int[] results) {
        if (results == null) return;
        System.out.println("Slider called back");
        final UI ui = LiveRAC.LRAC.getUI();
        ui.setRangeSliderDensity(results);
    }

    public void queueSwatchResult(final ArrayList<CellSwatchRecord> results) {
        if (results == null) return;
        this.swatchQueue.add(results);
        if (!firstLoad) {
            firstLoad = true;
            lrd.getLr().getUI().destroyProgressDialog();
            if (firstTimeEver) {
                lrd.getLr().getUI().showFirstTimeSurvey();
                firstTimeEver = false;
            }
        }
        this.lrd.forceRedraw();
    }

    /**
	 * Process all the data that has been received from the data service in the
	 * queues
	 */
    public boolean processDataResponses() {
        boolean newData = false;
        if (swatchQueue.size() != 0 || chartDataQueue.size() != 0 || alarmDataQueue.size() != 0) {
            this.incrementUpdateNumber();
            newData = true;
            if (swatchQueue.size() != 0) {
                for (Iterator<Strip> it = stripOrderedList.iterator(); it.hasNext(); ) {
                    it.next().clearChartList();
                }
                this.processSwatchData(getUpdateNumber());
                Iterator<Source> it = sourceTable.values().iterator();
                while (it.hasNext()) it.next().endSwatchUpdate(getUpdateNumber());
            }
            if (chartDataQueue.size() != 0) {
                processChartData(getUpdateNumber());
            }
            if (alarmDataQueue.size() != 0) {
                processAlarmData(getUpdateNumber());
            }
            this.incrementUpdateNumber();
        }
        return newData;
    }

    private void processSwatchData(long updateNumber) {
        this.lrd.groupRanges.clearAllGroups();
        final ArrayList<CellSwatchRecord> results = this.swatchQueue.get(this.swatchQueue.size() - 1);
        final Iterator<CellSwatchRecord> swatchIt = results.iterator();
        while (swatchIt.hasNext()) {
            final CellSwatchRecord swatch = swatchIt.next();
            final String sourceName = swatch.getSource();
            final Source source = sourceTable.get(sourceName);
            if (sourceName.equals("") || sourceName == null || source == null) {
                System.err.println("Datastore processSwatchData: Unknown sourceName: " + sourceName);
            } else {
                source.addSwatchData(swatch, updateNumber);
            }
        }
        this.swatchQueue.clear();
    }

    /**
	 * Iterate through the queue of returned chart data, and add that data to
	 * the chart objects
	 * @param l 
	 */
    private void processChartData(long updatenumber) {
        if (this.chartDataQueue.size() == 0) return;
        final HashMap<String, ProcessedChartDataResults> map = this.chartDataQueue.get(this.chartDataQueue.size() - 1);
        final Iterator<ProcessedChartDataResults> resultIt = map.values().iterator();
        while (resultIt.hasNext()) {
            ProcessedChartDataResults resultSet = resultIt.next();
            Source s = resultSet.getSource();
            s.addCellData(resultSet, updatenumber);
        }
        this.chartDataQueue.clear();
    }

    private void processAlarmData(long updatenumber) {
        if (alarmDataQueue.size() == 0) return;
        final Map<String, SourceAlarmDataResult> alarmRecords = alarmDataQueue.get(0);
        Iterator<SourceAlarmDataResult> it = alarmRecords.values().iterator();
        while (it.hasNext()) {
            final SourceAlarmDataResult r = it.next();
            final Source s = getSource(r.getSourceName());
            if (s != null) ;
            s.addCellData(r, updatenumber);
        }
        alarmDataQueue.clear();
    }

    /**
	 * This is the way to request alarm data from the SWIFT data service. Note:
	 * this is asynchronous, the method enqueues the request which may be
	 * serviced at an undetermined time in the future. (Hopefully fast.)
	 * 
	 * @param req
	 *            AlarmRequest object
	 */
    public void requestData(final AbstractQuery req) {
        this.dataInterface.requestData(req);
    }

    /**
	 * Initialize the connection info and Request an initial set of data to be
	 * added to the visualization
	 * 
	 * @param hostName
	 * @param port
	 */
    public void initDataConn(final Date beginDate, final Date endDate) {
        this.dataInterface = new DataServiceThread(this);
        dataInterface.start();
        this.timeRange[0] = beginDate;
        this.timeRange[1] = endDate;
        LiveRAC.makeLogEntry(LogEntry.CONNECT_TYPE, "Open connection to: " + getCgiUrl(), null);
        resetDisplay();
    }

    /**
	 * Indicate the contents of cells has changed, a new swatch request should be generated
	 * and all chart contents should be updated.  
	 */
    public void fireCellContentsChange() {
        lrd.getLr().refreshCellData(getBeginDate(), getEndDate(), false, false);
    }

    /**
	 * This gets called when data is first loaded or when any aspect of 
	 * strips / templates get changed and it's necessary to reload all data
	 *
	 */
    public void resetDisplay() {
        LiveRAC lr = lrd.getLr();
        lrd.sourceFocusGroupKey = null;
        lrd.stripFocusGroupIndex = -2;
        AccordionDrawer.loaded = false;
        setFirstLoad(false);
        final String connectString = getCgiUrl();
        lr.getUI().showProgressDialog(connectString);
        lr.getDrawPanel().remove(lrd.getCanvas());
        flushData();
        try {
            loadAvailGroupList();
            loadGroupList(SourceGroup.getGroupFileStream(this.getClass().getClassLoader()));
            loadSources(groupList);
            this.loadTemplates(TemplateHandler.getTemplateFileStream(this.getClass().getClassLoader()));
            this.loadStrips(StripHandler.getStripFileStream(this.getClass().getClassLoader()), templateHandler.getTemplateList());
            loadFocusGroups();
        } catch (Exception e) {
            lr.getUI().destroyProgressDialog();
            JOptionPane.showMessageDialog(lr.getUI().getMainFrame(), "<html>Error connecting to server: " + connectString + "<p>Please verify your Visualizer username and password are correct.</html>", "Error connecting . . . ", JOptionPane.ERROR_MESSAGE);
        }
        gridChanged();
        if (lrd.getSplitAxis(AccordionDrawer.X).getSize() > 0 && lrd.getSplitAxis(AccordionDrawer.Y).getSize() > 0) {
            lrd.initializeFont();
            lrd.reset();
            AccordionDrawer.loaded = true;
            lrd.incrementFrameNumber();
            lr.getDrawPanel().add(lrd.getCanvas());
            swatchRequest();
        } else {
            if (!firstLoad) {
                firstLoad = true;
                lrd.getLr().getUI().destroyProgressDialog();
            }
        }
    }

    public String getCgiUrl() {
        return AbstractQuery.getCgiUrl();
    }

    /**
	 * Parameter-free wrapper method for queueing a swatch query
	 */
    public void swatchRequest() {
        final CellSwatchQuery req = new CellSwatchQuery(getBeginDate(), getEndDate(), groupList.iterator(), stripOrderedList.iterator(), lrd.lr.getAlarmRequestType(), AbstractQuery.AGG_AUTO);
        this.requestData(req);
    }

    /**
	 * Flush the data store object, suspend, then disconnect the data source!
	 * 
	 * @return
	 */
    public boolean flushData() {
        dataInterface.halt();
        stopTimer();
        cachedAlarmStrip = null;
        ((AccordionLRACDrawerFinal) lrd).actionmodeReset();
        lrd.shutdown();
        lrd.groupRanges.clearAllGroups();
        lrd.getSourceAxis().clear();
        lrd.getStripAxis().clear();
        availGroupList = null;
        groupList = null;
        stripOrderedList = new ArrayList<Strip>(DataStore.STRIPESTIMATE);
        stripIndexTable = new HashMap<Strip, Integer>(DataStore.STRIPESTIMATE);
        sourceTable.clear();
        swatchQueue.clear();
        chartDataQueue.clear();
        channelStripMap = null;
        stripFocusGroups.clear();
        sourceFocusGroups.clear();
        dataInterface.go();
        ;
        return true;
    }

    public void disconnect() {
        this.dataInterface.halt();
        this.dataInterface = null;
    }

    public boolean hasData() {
        if (this.stripOrderedList != null && this.stripOrderedList.size() != 0 && this.sourceTable != null && this.sourceTable.size() != 0) return true;
        return false;
    }

    /**
	 * How many sources are we monitoring?
	 * 
	 * @return
	 */
    public int numSources() {
        return this.sourceTable.size();
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
	 * @return Returns the refreshRate.
	 */
    public int getRefreshRate() {
        return refreshRate;
    }

    /**
	 * @param refreshRate
	 *            The refreshRate to set.
	 */
    public void setRefreshRate(final int refreshRate) {
        this.refreshRate = refreshRate;
    }

    /**
	 * This class provides the code to make a request for new alarms to the data
	 * source.
	 * 
	 * @author Peter McLachlan (spark343@cs.ubc.ca)
	 * 
	 */
    class DataTimerTask extends TimerTask {

        @Override
        public void run() {
            final UI ui = LiveRAC.LRAC.getUI();
            final Date newBeginDate = new Date(timeRange[0].getTime() + getRefreshAmount() * 1000);
            final Date newEndDate = new Date(timeRange[1].getTime() + getRefreshAmount() * 1000);
            ui.setSpinnerTimes(newBeginDate, newEndDate);
        }
    }

    /**
	 * @return Returns the lookup table for sources based on sourcename.
	 */
    public HashMap<String, Source> getSourceTable() {
        return this.sourceTable;
    }

    public long getUpdateNumber() {
        return this.updateNumber;
    }

    public void incrementUpdateNumber() {
        this.updateNumber++;
    }

    /**
	 * In the splitAxis swap the specified sources
	 * 
	 * @param firstIndex
	 * @param secondIndex
	 */
    public void swapSources(final int firstIndex, final int secondIndex) {
        this.incrementUpdateNumber();
        this.lrd.incrementFrameNumber();
        lrd.groupRanges.clearAllGroups();
        final SplitAxis axis = this.lrd.sourceAxis;
        final SplitLine firstLine = axis.getSplitFromIndex(firstIndex);
        final SplitLine secondLine = axis.getSplitFromIndex(secondIndex);
        final Source firstDev = (Source) firstLine.getRowObject();
        final Source secondDev = (Source) secondLine.getRowObject();
        LiveRAC.makeLogEntry(LogEntry.ARRANGE_SOURCES, "Manual arrangement, swapping (" + firstDev + ", " + secondDev + ")", null);
        firstLine.setRowObject(secondDev);
        secondDev.setMinLine(firstLine);
        secondLine.setRowObject(firstDev);
        firstDev.setMinLine(secondLine);
        gridChanged();
        this.lrd.requestRedraw();
    }

    public void swapStrips(final int firstStripPos, final int secondStripPos) {
        this.incrementUpdateNumber();
        this.lrd.incrementFrameNumber();
        lrd.groupRanges.clearAllGroups();
        final Strip firstStrip = this.stripOrderedList.get(firstStripPos);
        final Strip secondStrip = this.stripOrderedList.get(secondStripPos);
        LiveRAC.makeLogEntry(LogEntry.ARRANGE_STRIPS, "Strip swap (" + firstStrip + ", " + secondStrip + ")", null);
        this.stripOrderedList.set(firstStripPos, secondStrip);
        this.stripOrderedList.set(secondStripPos, firstStrip);
        this.stripIndexTable.remove(firstStrip);
        this.stripIndexTable.remove(secondStrip);
        this.stripIndexTable.put(secondStrip, firstStripPos);
        this.stripIndexTable.put(firstStrip, secondStripPos);
        gridChanged();
        this.lrd.requestRedraw();
    }

    public int getRefreshAmount() {
        return refreshAmount;
    }

    /**
	 * Sets the amount in seconds each refresh will advance
	 * @param refreshAmount
	 */
    public void setRefreshAmount(final int refreshAmount) {
        this.refreshAmount = refreshAmount;
    }

    /**
	 * Scan through all sources and add critical ranges back
	 * 
	 */
    public void reMarkAllCriticals() {
        lrd.getGroupRanges().clearGroup(Groups.critGroup);
        final Iterator<Source> it = this.sourceTable.values().iterator();
        while (it.hasNext()) {
            Source s = it.next();
            Iterator<Cell> cit = s.getCells().values().iterator();
            while (cit.hasNext()) {
                Cell cell = cit.next();
                cell.checkAddToCritGroup();
            }
        }
    }

    public List<Source> getSortedSourceList() {
        final SplitAxis sourceAxis = lrd.getSourceAxis();
        if (sourceAxis == null) return null;
        ArrayList<Source> sourceList = new ArrayList<Source>(sourceAxis.getSize());
        for (Iterator<SplitLine> it = sourceAxis.iterator(); it.hasNext(); ) {
            SplitLine currLine = it.next();
            if (currLine.rowObject != null) {
                sourceList.add((Source) currLine.getRowObject());
            }
        }
        return sourceList;
    }

    /**
	 * 
	 * Re-sort the source split line tree. Note the sort comparison operator
	 * must be set on the source before calling this method
	 * 
	 * Performance: Unfortunately we do not sort the tree in place: log n + n log n
	 * 
	 */
    protected void sortSourceAxis() {
        if (Source.sortKey.equals(Source.SORTOPTIONS[Source.MANUALINDEX]) && Source.getSortStrip() == null) return;
        final TreeSet<Source> sourceTree = new TreeSet<Source>();
        final SplitAxis sourceAxis = lrd.getSourceAxis();
        for (Iterator<SplitLine> it = sourceAxis.iterator(); it.hasNext(); ) {
            SplitLine currLine = it.next();
            if (currLine.rowObject != null) {
                sourceTree.add((Source) currLine.rowObject);
            }
        }
        SplitLine currLine = sourceAxis.getMinStuckLine();
        for (Iterator<Source> it = sourceTree.iterator(); it.hasNext(); ) {
            Source source = it.next();
            source.setMinLine(currLine);
            currLine.setRowObject(source);
            currLine.setCullingObject(null);
            currLine = sourceAxis.getNextSplit(currLine);
        }
    }

    /**
	 * Sets the search term for sources, sorts the tree and redraws the display
	 * 
	 * @param s Search term from static list in Source object
	 * @see Source#SORTOPTIONS
	 */
    public void setSourceSort(String s) {
        AccordionLRACDrawer.loaded = false;
        Source.setSortKey(s);
        Source.setSortStrip(null);
        sortSourceAxis();
        AccordionLRACDrawer.loaded = true;
        lrd.forceRedraw();
    }

    public void sort() {
        AccordionLRACDrawer.loaded = false;
        sortSourceAxis();
        AccordionLRACDrawer.loaded = true;
        lrd.forceRedraw();
    }

    /**
	 * Retrieve the index number of a metric given the name of the metric. -1 if
	 * the metric could not be found.
	 * 
	 * @param strip
	 * @return metric index number
	 */
    public int getStripIndex(final Strip strip) {
        final Integer result = this.stripIndexTable.get(strip);
        if (result == null) {
            System.err.println("Unable to find specified strip");
            return -1;
        } else return result.intValue();
    }

    /**
	 * Retrieve the name of a metric from the list of all monitored metrics
	 * 
	 * @param index
	 * @return
	 */
    public Strip getStripByIndex(final int index) {
        if (index >= 0 && index < this.stripOrderedList.size()) return this.stripOrderedList.get(index);
        System.err.println("DataStore.getStripByIndex: out of range index: " + index);
        return null;
    }

    public Strip getStripBySplit(final SplitLine stripMinLine, final SplitAxis stripAxis) {
        final int stripIndex = stripAxis.getSplitIndex(stripMinLine) + 1;
        return getStripByIndex(stripIndex);
    }

    /**
	 * Removes a row or column from the specified axis if it exists.
	 * 
	 * @param palette
	 */
    public boolean removeStrip(final Strip palette, final SplitAxis paletteAxis) {
        final Integer metricIndex = this.stripIndexTable.remove(palette);
        if (metricIndex == null) return false;
        final HashMap<Strip, Integer> newMetricsIndex = new HashMap<Strip, Integer>();
        final Iterator<Strip> it = this.stripIndexTable.keySet().iterator();
        Strip p;
        Integer cInt;
        while (it.hasNext()) {
            p = it.next();
            cInt = this.stripIndexTable.get(p);
            if (cInt > metricIndex) {
                cInt = new Integer(cInt.intValue() - 1);
                newMetricsIndex.put(p, cInt);
            } else {
                newMetricsIndex.put(p, cInt);
            }
        }
        this.stripIndexTable = newMetricsIndex;
        this.stripOrderedList.remove(metricIndex.intValue());
        final SplitLine lastLine = paletteAxis.getMaxLine();
        paletteAxis.deleteEntry(lastLine);
        return true;
    }

    void addStrip(Strip strip, SplitAxis stripAxis) {
        stripOrderedList.add(strip);
        stripIndexTable.put(strip, new Integer(stripOrderedList.lastIndexOf(strip)));
        SplitLine newLine;
        if (stripOrderedList.size() == 1) {
            newLine = stripAxis.getMinLine();
        } else {
            newLine = new SplitLine();
            stripAxis.putAt(newLine, stripAxis.getMaxLine());
        }
    }

    /**
	 * @return Returns the allMetrics.
	 */
    public ArrayList<Strip> getStripOrderedList() {
        return this.stripOrderedList;
    }

    /**
	 * Writes the new palette order out to disk
	 * Warning: Will overwrite any changes made in PaletteEditor since last reload
	 */
    public void saveStripOrder() {
        stripHandler.saveStrips(Util.getCreateFile(LiveRAC.getSelectedProfileUserPath() + StripHandler.DEFAULTSTRIPFILE), stripOrderedList);
    }

    public void loadFocusGroups() {
        List<FocusGroup> loadedGroups = AbstractFocusGroup.loadFocusGroups(this.getClass().getClassLoader());
        Iterator<FocusGroup> it = loadedGroups.iterator();
        while (it.hasNext()) {
            FocusGroup fg = it.next();
            if (fg.getType() == AbstractFocusGroup.SOURCEGROUP) sourceFocusGroups.add(fg); else stripFocusGroups.add(fg);
        }
        LiveRAC.makeLogEntry(LogEntry.LOAD_SOURCE_FOCUS_GROUP, "Load source focus groups from file.", sourceFocusGroups);
        LiveRAC.makeLogEntry(LogEntry.LOAD_STRIP_FOCUS_GROUP, "Load strip focus groups from file.", stripFocusGroups);
    }

    public void saveFocusGroups() {
        List<FocusGroup> focusGroups = new ArrayList<FocusGroup>(stripFocusGroups.size() + sourceFocusGroups.size() + 1);
        focusGroups.addAll(sourceFocusGroups);
        focusGroups.addAll(stripFocusGroups);
        AbstractFocusGroup.saveGroups(focusGroups);
    }

    public ArrayList<SourceGroup> getGroupList() {
        return groupList;
    }

    public ArrayList<SourceGroup> getAvailGroupList() {
        return availGroupList;
    }

    public DataServiceThread getDataInterface() {
        return dataInterface;
    }

    public Date getBeginDate() {
        return timeRange[0];
    }

    public Date getEndDate() {
        return timeRange[1];
    }

    public void saveArrangements() {
        saveStripOrder();
        SourceGroup.saveGroups(groupList, Source.getSortKey());
    }

    public boolean isFirstLoad() {
        return firstLoad;
    }

    public void setFirstLoad(boolean firstLoad) {
        this.firstLoad = firstLoad;
    }

    /**
	 * Returns an ordered list of unique sort keys
	 * @return
	 */
    public TreeSet<String> getSourceSortKeySet() {
        return sourceSortKeySet;
    }

    public List<FocusGroup> getSourceFocusGroups() {
        return sourceFocusGroups;
    }

    public List<FocusGroup> getStripFocusGroups() {
        return stripFocusGroups;
    }

    /**
	 * Method to retrieve the hard-coded "Alarm strip".  
	 * @return
	 */
    public Strip getAlarmStrip() {
        if (cachedAlarmStrip != null) return cachedAlarmStrip;
        List<Strip> stripList = getStripOrderedList();
        Iterator<Strip> it = stripList.iterator();
        while (it.hasNext()) {
            final Strip strip = it.next();
            if (strip.getStripTitle().equals(Strip.ALARMSTRIPTITLE)) {
                cachedAlarmStrip = strip;
                break;
            }
        }
        return cachedAlarmStrip;
    }

    public Date[] getTimeRange() {
        return timeRange;
    }

    public void setLoadingState(boolean state) {
        lrd.getLr().getUI().setLoadingState(state);
    }

    public StripHandler getStripHandler() {
        return stripHandler;
    }

    public TemplateHandler getTemplateHandler() {
        return templateHandler;
    }

    public static boolean isFirstTimeEver() {
        return firstTimeEver;
    }

    public static void setFirstTimeEver(boolean firstTimeEver) {
        DataStore.firstTimeEver = firstTimeEver;
    }
}

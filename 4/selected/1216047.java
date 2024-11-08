package net.sourceforge.olduvai.lrac.csvdataservice;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.JFrame;
import net.sourceforge.jglchartutil.datamodels.SimpleDataSeries;
import net.sourceforge.olduvai.lrac.TimeRangeSampleIntervalRelation;
import net.sourceforge.olduvai.lrac.csvdataservice.records.CellSwatchRecord;
import net.sourceforge.olduvai.lrac.csvdataservice.structure.FocusGroup;
import net.sourceforge.olduvai.lrac.csvdataservice.structure.InputChannel;
import net.sourceforge.olduvai.lrac.csvdataservice.structure.Source;
import net.sourceforge.olduvai.lrac.csvdataservice.structure.SourceGroup;
import net.sourceforge.olduvai.lrac.drawer.queries.DetailQuery;
import net.sourceforge.olduvai.lrac.drawer.queries.SwatchQuery;
import net.sourceforge.olduvai.lrac.drawer.structure.strips.Strip;
import net.sourceforge.olduvai.lrac.drawer.structure.templates.Template;
import net.sourceforge.olduvai.lrac.genericdataservice.AbstractDataServiceDispatcher;
import net.sourceforge.olduvai.lrac.genericdataservice.DataResultInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.StripDiskLoader;
import net.sourceforge.olduvai.lrac.genericdataservice.TemplateDiskLoader;
import net.sourceforge.olduvai.lrac.genericdataservice.TimeRangeIntervalRelationDiskLoader;
import net.sourceforge.olduvai.lrac.genericdataservice.cellviewer.AbstractCellViewer;
import net.sourceforge.olduvai.lrac.genericdataservice.cellviewer.GenericCellViewer;
import net.sourceforge.olduvai.lrac.genericdataservice.queries.ActiveSourceQueryInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.queries.QueryInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.queries.SwatchQueryInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.queries.TemporalQuery;
import net.sourceforge.olduvai.lrac.genericdataservice.records.DetailRecord;
import net.sourceforge.olduvai.lrac.genericdataservice.records.SwatchRecordInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.DataCellInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.FocusGroupInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelGroupInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelItemInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.SourceGroupInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.SourceInterface;
import net.sourceforge.olduvai.lrac.util.GrowableDoubleArray;
import net.sourceforge.olduvai.lrac.util.Util;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import com.csvreader.CsvReader;

/**
 * Implementation of the DataServiceDispatcher for LiveRAC. 
 * This version reads in data from CSV files.   
 * 
 * @author peter
 *
 */
public class DataServiceDispatcher extends AbstractDataServiceDispatcher {

    private static final String DEFAULTSOURCEGROUPFILE = "groups.xml";

    private static final String FOCUSGROUPFILE = "focusgroups.xml";

    private static final String DEFAULTTEMPLATEFILE = "templates.xml";

    private static final String DEFAULTTIMERANGEINTERVALRELATIONFILE = "intervals.xml";

    private static final String DEFAULTSAVEDSOURCEFILE = "savedsources.xml";

    public static final String DEFAULTSTRIPFILE = "strips.xml";

    public static final String SYSTEMPREFSFOLDER = "net/sourceforge/olduvai/lrac/csvdataservice/config/";

    public static final String USERPREFSFOLDER = System.getProperty("user.home") + "/LiveRAC/csv/";

    static final String DEFAULTUSERCONNINFOFILE = USERPREFSFOLDER + "conn.txt";

    static final double INTERVAL_5MINUTE = 12d;

    static final double INTERVAL_HOUR = 1d;

    static final double INTERVAL_DAY = 1 / 24;

    static final String INTERVAL_5MINUTE_DESC = "Every five minutes";

    static final String INTERVAL_HOUR_DESC = "Hourly";

    static final String INTERVAL_DAY_DESC = "Daily";

    static final String[] sampleIntervalDescriptions = { INTERVAL_5MINUTE_DESC, INTERVAL_HOUR_DESC, INTERVAL_DAY_DESC };

    private static final double[] supportedSampleIntervals = { INTERVAL_5MINUTE, INTERVAL_HOUR, INTERVAL_DAY };

    static final String CSVSOURCES = "sources.csv";

    static final String CSVCHANNELS = "channels.csv";

    static final String CSVSOURCEGROUPS = "sourcegroups.csv";

    /**
	 * Where the csv data files are located
	 */
    String dataFolder;

    /**
	 * Caches data files requested by the user.
	 * 
	 * TODO performance currently the entire data file is read in even if the user is 
	 * only asking for a limited subset of the data.  We might want to look at just using
	 * OS disk caching as the cache and doing binary searches of the data file without
	 * loading anything into memory.   
	 */
    HashMap<String, TimeSeriesData> dataCache = new HashMap<String, TimeSeriesData>();

    Map<String, SourceInterface> allSourceMap;

    List<SourceGroupInterface> allSourceGroupList;

    List<InputChannelInterface> allChannelList;

    List<InputChannelGroupInterface> allChannelGroupList;

    /**
	 * Maps from an internal name to a channel group.  Uses: 
	 * 1) bind strips to the appropriate input channel group.
	 * 
	 */
    private Map<String, InputChannelGroupInterface> internalNameToInputChannelGroup;

    /**
	 * Maps from an internal name to an input channel.  This is used for two things: 
	 * 1) bind strips to the appropriate input channel.
	 * 2) incoming swatch / detail data only has the channel name, we need an efficient way to go from the name to the object  
	 */
    private Map<String, InputChannelInterface> internalNameToInputChannel;

    /**
	 * Initialize the Daytona reader in addition to linking the result interface.
	 * @param ri
	 */
    public DataServiceDispatcher(DataResultInterface ri) {
        super(ri);
    }

    protected void buildRunQueue(List<QueryInterface> requestQueue, List<QueryInterface> runQueue) {
        boolean seenSwatchQuery = false;
        Date startDate = null;
        Date endDate = null;
        synchronized (requestQueue) {
            QueryInterface query;
            HashMap<Strip, ChartDetailBulkQuery> bulkQueryMap = new HashMap<Strip, ChartDetailBulkQuery>();
            for (int i = requestQueue.size() - 1; i >= 0; i--) {
                query = requestQueue.get(i);
                if (query instanceof SwatchQuery) {
                    if (seenSwatchQuery == false) {
                        runQueue.add(query);
                        seenSwatchQuery = true;
                    }
                } else if (query instanceof DetailQuery) {
                    final DetailQuery detailQuery = (DetailQuery) query;
                    if (startDate == null && endDate == null) {
                        startDate = detailQuery.getBeginDate();
                        endDate = detailQuery.getEndDate();
                    }
                    if (startDate == detailQuery.getBeginDate() && endDate == detailQuery.getEndDate()) {
                        ChartDetailBulkQuery bulkquery = bulkQueryMap.get(detailQuery.getStrip());
                        if (bulkquery == null) {
                            bulkquery = new ChartDetailBulkQuery(startDate, endDate, detailQuery.getStrip(), detailQuery.getInterval(), System.currentTimeMillis());
                            bulkQueryMap.put(detailQuery.getStrip(), bulkquery);
                        }
                        bulkquery.addDetailQuery(detailQuery);
                    }
                }
                requestQueue.remove(i);
            }
            if (bulkQueryMap.size() > 0) {
                for (QueryInterface q : bulkQueryMap.values()) {
                    runQueue.add(q);
                }
            }
        }
    }

    /**
	 * TODO: not yet implemented
	 */
    public boolean cancelQuery(int queryNumber) {
        System.err.println("Query cancelling not yet implemented for Daytona");
        return false;
    }

    public Date[] connect(JFrame mainFrame) {
        Date[] dates = null;
        ConnectionDialog connectDialog = new ConnectionDialog(mainFrame);
        connectDialog.setVisible(true);
        Date beginDate = connectDialog.getBeginDate();
        Date endDate = connectDialog.getEndDate();
        if (beginDate != null && endDate != null) {
            dates = new Date[] { beginDate, endDate };
            dataFolder = connectDialog.getDataFolder();
        }
        if (dates != null) {
            try {
                loadInputChannels();
                loadInputChannelGroups();
                loadAllSources();
                loadAllSourceGroups();
            } catch (IOException e) {
                dates = null;
            }
        }
        return dates;
    }

    /**
	 * Used by the file loaders to retrieve either the user specified version of a file 
	 * for a given profile, or the system default version of the file.  
	 * @param fileName
	 * 
	 * @return
	 */
    final InputStream getFileStream(String fileName) {
        final String defaultSystemFile = DataServiceDispatcher.SYSTEMPREFSFOLDER + fileName;
        final String defaultUserFile = DataServiceDispatcher.USERPREFSFOLDER + fileName;
        return Util.chooseStream(this.getClass().getClassLoader(), defaultSystemFile, defaultUserFile, false);
    }

    final InputStream getGlobalConfigFile(String fileName) {
        final String globalFile = SYSTEMPREFSFOLDER + fileName;
        return Util.getJARresource(this.getClass().getClassLoader(), globalFile);
    }

    public List<FocusGroupInterface> loadSavedFocusGroups() throws IOException {
        return FocusGroup.loadFocusGroups(getFileStream(FOCUSGROUPFILE));
    }

    /**
	 * Retrieve groups of input channels from CSV file
	 * TODO unimplemented for CSV version of LiveRAC data wrapper.
	 * 
	 * @throws IOException
	 */
    private void loadInputChannelGroups() throws IOException {
        allChannelGroupList = new ArrayList<InputChannelGroupInterface>();
        Map<String, InputChannelGroupInterface> newChannelGroupMap = new HashMap<String, InputChannelGroupInterface>(allChannelGroupList.size());
        for (Iterator<InputChannelGroupInterface> it = allChannelGroupList.iterator(); it.hasNext(); ) {
            final InputChannelGroupInterface channelGroup = it.next();
            newChannelGroupMap.put(channelGroup.getInternalName(), channelGroup);
        }
        internalNameToInputChannelGroup = newChannelGroupMap;
    }

    public List<InputChannelGroupInterface> getInputChannelGroups() {
        return allChannelGroupList;
    }

    /**
	 * Called at connection time to read in the input channels from CSV
	 */
    private void loadInputChannels() throws IOException {
        allChannelList = new ArrayList<InputChannelInterface>();
        CsvReader reader = null;
        final String fileName = dataFolder + "/" + CSVCHANNELS;
        try {
            reader = new CsvReader(fileName);
        } catch (FileNotFoundException e) {
            throw new IOException("Could not open input channel file: " + fileName);
        }
        reader.readHeaders();
        while (reader.readRecord()) {
            final String channelname = reader.get("channelname");
            final String type = reader.get("type");
            final String unit = reader.get("unit");
            final String label = reader.get("label");
            allChannelList.add(new InputChannel(channelname, type, unit, label));
        }
        Map<String, InputChannelInterface> newChannelMap = new HashMap<String, InputChannelInterface>();
        for (Iterator<InputChannelInterface> it = allChannelList.iterator(); it.hasNext(); ) {
            final InputChannelInterface channel = it.next();
            newChannelMap.put(channel.getInternalName(), channel);
        }
        internalNameToInputChannel = newChannelMap;
    }

    public List<InputChannelInterface> getInputChannels() {
        return allChannelList;
    }

    public Set<SourceInterface> loadActiveSources(ActiveSourceQueryInterface query) throws IOException {
        Set<SourceInterface> selectedSources = new HashSet<SourceInterface>(query.getSourceList());
        for (SourceGroupInterface sg : query.getGroupList()) {
            for (final String s : sg.getSourceList()) {
                System.out.println("-- retrieving source: " + s);
                final SourceInterface source = allSourceMap.get(s);
                if (source != null) selectedSources.add(source);
            }
        }
        return selectedSources;
    }

    private void loadAllSourceGroups() throws IOException {
        allSourceGroupList = new ArrayList<SourceGroupInterface>();
        CsvReader reader = null;
        final String fileName = dataFolder + "/" + CSVSOURCEGROUPS;
        try {
            reader = new CsvReader(fileName);
        } catch (FileNotFoundException e) {
            throw new IOException("Could not open source group file: " + fileName);
        }
        reader.readHeaders();
        while (reader.readRecord()) {
            int i = 0;
            final String groupName = reader.get(i++);
            final String groupType = reader.get(i++);
            SourceGroup group = new SourceGroup(groupName, groupName, groupType);
            for (; i < reader.getColumnCount(); i++) {
                final String sourceName = reader.get(i);
                group.addSource(sourceName);
            }
            allSourceGroupList.add(group);
        }
    }

    public List<SourceGroupInterface> getAllSourceGroups() {
        return allSourceGroupList;
    }

    /**
	 * 
	 * TODO performance there's no real reason we need to have all the sources loaded in memory 
	 * aside from when the user is selecting them from the pick list menu.   
	 * 
	 */
    private void loadAllSources() throws IOException {
        allSourceMap = new HashMap<String, SourceInterface>();
        CsvReader reader = null;
        final String fileName = dataFolder + "/" + CSVSOURCES;
        try {
            reader = new CsvReader(fileName);
        } catch (FileNotFoundException e) {
            throw new IOException("Could not open input channel file: " + fileName);
        }
        reader.readHeaders();
        while (reader.readRecord()) {
            final String sourceName = reader.get("source");
            final String key = reader.get("key");
            final String value = reader.get("value");
            SourceInterface s = allSourceMap.get(sourceName);
            if (s == null) {
                s = Source.createSource(sourceName);
                allSourceMap.put(sourceName, s);
            }
            s.addMetaValue(key, value);
        }
        return;
    }

    public Collection<SourceInterface> getAllSources() {
        return getSourceNameMap().values();
    }

    /**
	 * Retrieves a map that can be used for looking up a given source name
	 * to a Source object.  
	 * 
	 * @return
	 */
    protected Map<String, SourceInterface> getSourceNameMap() {
        return allSourceMap;
    }

    public List<Strip> loadSavedStrips() throws IOException {
        return StripDiskLoader.loadStrips(getFileStream(DEFAULTSTRIPFILE));
    }

    public List<Template> loadSavedTemplates() throws IOException {
        return TemplateDiskLoader.loadTemplates(getFileStream(DEFAULTTEMPLATEFILE));
    }

    public void saveFocusGroups(List<FocusGroupInterface> focusGroups) throws IOException {
        FocusGroup.saveGroups(Util.getCreateFile(USERPREFSFOLDER + FOCUSGROUPFILE), focusGroups);
    }

    public void saveStrips(List<Strip> strips) throws IOException {
        StripDiskLoader.saveStrips(Util.getCreateFile(USERPREFSFOLDER + DEFAULTSTRIPFILE), strips);
    }

    public void saveTemplates(List<Template> templates) throws IOException {
        TemplateDiskLoader.saveTemplates(Util.getCreateFile(USERPREFSFOLDER + DEFAULTTEMPLATEFILE), templates);
    }

    static final int SIZEHINT = 5000;

    /**
	 * Called to load a specific data file into memory
	 * @param fileName
	 */
    private TimeSeriesData getDataFile(String sourceName, String channel, String interval) {
        final String filename = fileNameFromTuple(sourceName, channel, interval);
        if (dataCache.containsKey(filename)) return dataCache.get(filename);
        TimeSeriesData data = null;
        CsvReader reader = null;
        final String filepath = dataFolder + "/" + filename;
        try {
            reader = new CsvReader(filepath);
        } catch (FileNotFoundException e) {
            dataCache.put(filename, data);
            return null;
        }
        final GrowableDoubleArray times = new GrowableDoubleArray(SIZEHINT);
        final GrowableDoubleArray values = new GrowableDoubleArray(SIZEHINT);
        try {
            reader.readHeaders();
            while (reader.readRecord()) {
                final double time = Double.parseDouble(reader.get("time"));
                final double value = Double.parseDouble(reader.get("value"));
                times.add(time);
                values.add(value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        data = TimeSeriesData.createTimeSeries(times.getArray(), values.getArray());
        dataCache.put(filename, data);
        return data;
    }

    private SwatchRecordInterface getSwatch(SourceInterface source, InputChannelInterface channel, Date beginDate, Date endDate, long queryId, String interval) throws IOException {
        final TimeSeriesData data = getDataFile(source.getInternalName(), channel.getInternalName(), interval);
        if (data == null) return null;
        final double[] stats = data.getStats(beginDate, endDate);
        if (stats == null) return null;
        final SwatchRecordInterface record = CellSwatchRecord.createSwatchRecord(source, channel, (float) stats[TimeSeriesData.STATSMIN], (float) stats[TimeSeriesData.STATSMAX], (float) stats[TimeSeriesData.STATSSUM], (int) stats[TimeSeriesData.STATSCOUNT], queryId);
        return record;
    }

    /**
	 * Returns a valid filename for the data folder based on the source/channel/interval tuple. 
	 * 
	 * @param sourceName
	 * @param channel
	 * @param interval
	 */
    static final String fileNameFromTuple(String sourceName, String channel, String interval) {
        return sourceName + "^" + channel + "^" + interval + ".csv";
    }

    @Override
    protected void serverRequest(QueryInterface req) throws IOException {
        final String interval = Integer.toString((int) ((TemporalQuery) req).getInterval().getMinutesPerSample());
        System.out.println("Selected interval is: " + interval);
        if (req instanceof SwatchQueryInterface) {
            final SwatchQueryInterface sreq = (SwatchQueryInterface) req;
            final List<SourceInterface> sources = sreq.getMergedSourceList(getSourceNameMap());
            final List<InputChannelItemInterface> channels = sreq.getInputChannelItemList();
            final List<SwatchRecordInterface> result = new ArrayList<SwatchRecordInterface>(sources.size() * channels.size());
            for (final SourceInterface source : sources) {
                for (final InputChannelItemInterface channel : channels) {
                    if (channel instanceof InputChannelInterface) {
                        final SwatchRecordInterface swatch = getSwatch(source, (InputChannelInterface) channel, sreq.getBeginDate(), sreq.getEndDate(), sreq.getQueryId(), interval);
                        if (swatch != null) result.add(swatch);
                    } else {
                        System.out.println("TODO CSV data dispatcher does not yet handle input channel groups");
                    }
                }
            }
            resultInterface.swatchQueryResult(result);
        } else if (req instanceof ChartDetailBulkQuery) {
            final ChartDetailBulkQuery creq = (ChartDetailBulkQuery) req;
            final List<SourceInterface> sources = creq.getSources();
            final List<InputChannelItemInterface> channels = creq.getChannelList();
            final List<DetailRecord> results = new ArrayList<DetailRecord>(sources.size() * channels.size());
            for (final SourceInterface source : sources) {
                for (final InputChannelItemInterface channel : channels) {
                    if (channel instanceof InputChannelInterface) {
                        final TimeSeriesData data = getDataFile(source.getInternalName(), channel.getInternalName(), interval).getTimeRange(creq.getBeginDate(), creq.getEndDate());
                        if (data == null) continue;
                        final double[] times = data.getTimestamps();
                        final double[] values = data.getValues();
                        final SimpleDataSeries series = new SimpleDataSeries(times, values);
                        final DetailRecord record = new DetailRecord(source, channel, series, creq.getQueryId());
                        results.add(record);
                    } else {
                        System.out.println("TODO CSV data dispatcher does not yet handle input channel groups");
                    }
                }
            }
            resultInterface.detailQueryResult(results);
        }
    }

    public DataResultInterface getResultInterface() {
        return resultInterface;
    }

    public List<SourceGroupInterface> loadSavedSourceGroups() {
        try {
            return SourceGroup.loadSelectedSourceGroups(allSourceGroupList, getFileStream(DEFAULTSOURCEGROUPFILE));
        } catch (IOException e) {
            System.err.println("DataServiceDispatcher: error loading saved source groups");
            return new ArrayList<SourceGroupInterface>();
        }
    }

    public String getConnectionName() {
        return dataFolder;
    }

    public AbstractCellViewer getCellViewer(DataCellInterface cell, JFrame owner) {
        return GenericCellViewer.makeGenericCellViewer(cell, owner);
    }

    public double[] getSupportedSampleIntervals() {
        return supportedSampleIntervals;
    }

    public String getUserId() {
        return "" + Math.random();
    }

    public HSSFWorkbook makeExcelExport(List<DataCellInterface> cells) {
        System.err.println("TODO: Excel export not yet implemented");
        return null;
    }

    public SortedSet<TimeRangeSampleIntervalRelation> getTimeRangeSampleIntervalRelations() {
        try {
            return TimeRangeIntervalRelationDiskLoader.loadRelations(getFileStream(DEFAULTTIMERANGEINTERVALRELATIONFILE));
        } catch (IOException e) {
            System.err.println("DataServiceDispatcher: Error loading TimeRangeSampleIntervalRelations");
            return new TreeSet<TimeRangeSampleIntervalRelation>();
        }
    }

    public FocusGroupInterface newFocusGroup(String focusGroupName, int type) {
        return new FocusGroup(focusGroupName, type);
    }

    public void saveTimeRangeSampleIntervalRelations(List<TimeRangeSampleIntervalRelation> relations) throws IOException {
        TimeRangeIntervalRelationDiskLoader.saveRelations(USERPREFSFOLDER + "/" + DEFAULTTIMERANGEINTERVALRELATIONFILE, relations);
    }

    /**
	 * Right now we are not permitting individually selectable sources, so no user selections 
	 * are being saved either.  
	 *  
	 */
    public List<SourceInterface> loadSavedSources() throws IOException {
        return Source.loadSelectedSources(getSourceNameMap(), getFileStream(DEFAULTSAVEDSOURCEFILE));
    }

    public void saveSelectedSourcesAndGroups(List<SourceGroupInterface> groups, List<SourceInterface> sources, List<SourceInterface> excludedSources, String sortKey) throws IOException {
        Source.saveSelectedSources(Util.getCreateFile(USERPREFSFOLDER + DEFAULTSAVEDSOURCEFILE), sources);
        SourceGroup.saveSelectedSourceGroups(Util.getCreateFile(USERPREFSFOLDER + DEFAULTSOURCEGROUPFILE), groups, sortKey);
    }

    public Map<String, InputChannelGroupInterface> getInternalNameToInputChannelGroup() {
        return internalNameToInputChannelGroup;
    }

    public Map<String, InputChannelInterface> getInternalNameToInputChannel() {
        return internalNameToInputChannel;
    }

    public String getLocalDataPath() {
        return USERPREFSFOLDER;
    }
}

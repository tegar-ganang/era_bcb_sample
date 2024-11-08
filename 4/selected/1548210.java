package net.sourceforge.olduvai.lrac.drawer;

import java.awt.Color;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import net.sourceforge.olduvai.accordiondrawer.SplitAxis;
import net.sourceforge.olduvai.accordiondrawer.SplitLine;
import net.sourceforge.olduvai.lrac.dataservice.ProcessedChartDataResults;
import net.sourceforge.olduvai.lrac.dataservice.records.CellSwatchRecord;
import net.sourceforge.olduvai.lrac.dataservice.records.SourceAlarmDataResult;
import net.sourceforge.olduvai.lrac.drawer.strips.Strip;
import net.sourceforge.olduvai.lrac.logging.Loggable;
import net.sourceforge.olduvai.lrac.util.Util;

/**
 *	This class represents a monitored 'source'.  A source is a networked appliance
 * (be it computer/router/switch/refridgerator/whatever) that has properties we can
 * monitor.  These properties are called 'input channels'.  The channels this device supports
 * are stored in an HashTable called channelss.  
 * 
 * @author Peter McLachlan (spark343@cs.ubc.ca)
 */
public class Source implements Comparable<Source>, Loggable {

    public static final Color foundColor = Color.BLUE;

    static final String TOKENIZER = "\\|";

    public static String[] SORTOPTIONS = { "Source Name", "Business", "Customer", "Group", "Location", "Site", "Manual" };

    public static final int SOURCEINDEX = 0;

    public static final int BUSINESSINDEX = 1;

    public static final int CUSTOMERINDEX = 2;

    public static final int GROUPINDEX = 3;

    public static final int LOCATIONINDEX = 4;

    public static final int SITEINDEX = 5;

    public static final int MANUALINDEX = 6;

    private static final String UNITIALIZED = "Unset";

    static final String[] GROUPKEYS = { "o", "b", "c", "g", "l", "s", "t" };

    public static final int getSortKeyIndex(String key) {
        for (int i = 0; i < GROUPKEYS.length; i++) if (GROUPKEYS[i].equals(key)) return i;
        System.err.println("Unrecognized key: " + key);
        return -1;
    }

    public static final int getSortKeyId() {
        String key = getSortKey();
        for (int i = 0; i < SORTOPTIONS.length; i++) if (SORTOPTIONS[i].equals(key)) return i;
        return 0;
    }

    public static final String getSortKey() {
        return sortKey;
    }

    public static final void setSortKey(String s) {
        sortKey = s;
    }

    /**
	 * When we are sorting by significant value, use the significant value from 
	 * the cell located in this strip.  
	 */
    static Strip sortStrip = null;

    static boolean ascend = true;

    static String sortKey = "Customer";

    static Cell phonyCell = new Cell(null, null);

    public static boolean getAscend() {
        return ascend;
    }

    public static void toggleAscend() {
        ascend = (ascend) ? false : true;
    }

    public static void setSortStrip(Strip sortStrip) {
        Source.sortStrip = sortStrip;
    }

    public static Strip getSortStrip() {
        return sortStrip;
    }

    public String getSortKeyField() {
        if (sortKey.equals(SORTOPTIONS[BUSINESSINDEX])) {
            return business;
        } else if (sortKey.equals(SORTOPTIONS[CUSTOMERINDEX])) {
            return customer;
        } else if (sortKey.equals(SORTOPTIONS[GROUPINDEX])) {
            return group;
        } else if (sortKey.equals(SORTOPTIONS[LOCATIONINDEX])) {
            return location;
        } else if (sortKey.equals(SORTOPTIONS[SITEINDEX])) {
            return site;
        } else {
            return sourceID;
        }
    }

    /**
	 * Reference to drawer
	 */
    AccordionLRACDrawer lrd;

    /**
	 * Split line above me
	 */
    SplitLine minLine;

    /**
	 * Datastructure containing Cell objects associated with each strip for this device.
	 * Note that any given source may not have a cell associated with each strip.  For instance, 
	 * there may not exist any data for a given strip & source, in this case no cell object is 
	 * created to save memory.  
	 * 
	 * Performance is improved by storing a list of old cells between refreshes
	 * which allows cells to be optionally reused instead of being recreated. 
	 */
    Hashtable<Strip, Cell> cells = new Hashtable<Strip, Cell>();

    /**
	 * List of cells from the previous update, these may be swapped in and updated 
	 * with new data values instead of creating a brand new cell. 
	 */
    Hashtable<Strip, Cell> oldCells = null;

    /**
	 * Unique number indicating the last update that was performed on this source
	 */
    long updateNumber = -1;

    /**
	 * ID of this source
	 */
    String sourceID = UNITIALIZED;

    /**
	 * Name of this source (may be identical to ID in most cases)
	 */
    String sourceName = UNITIALIZED;

    /**
	 * Business associated with this device
	 */
    String business = UNITIALIZED;

    /**
	 * Customer associated with this device
	 */
    String customer = UNITIALIZED;

    /**
	 * Group association
	 */
    String group = UNITIALIZED;

    /**
	 * Location association
	 */
    String location = UNITIALIZED;

    /**
	 * Site association
	 */
    String site = UNITIALIZED;

    /**
	 * Type
	 */
    String type = UNITIALIZED;

    final String[] groupvalues = { sourceName, business, customer, group, location, site, type };

    /**
	 * It is preferable to set optional parameters such as business/customer etc. 
	 * after instantiating this object.  
	 * 
	 * @param sourceName
	 * @param lrd
	 */
    public Source(String sourceName, AccordionLRACDrawer lrd) {
        this.sourceID = sourceName;
        setAd(lrd);
    }

    /**
	 * 
	 * @param position
	 * @return MetricCell
	 */
    public Cell getCell(int position) {
        if (position < 0) return null;
        Strip strip = lrd.getDataStore().getStripByIndex(position);
        if (cells == null) return null;
        return cells.get(strip);
    }

    /**
	 * Retrieves a cell given a stirp
	 * @param strip
	 * @return Cell, or null if cell does not exist for specified strip
	 */
    public Cell getCell(Strip strip) {
        if (cells == null) return null;
        return cells.get(strip);
    }

    /**
	 * Retrieves a Cell from the hashtable based on its name. 
	 * If the cell does not exist for this particular source, 
	 * it is created.
	 * 
	 * @param Strip 
	 * @return Cell
	 */
    public Cell getCreateCell(Strip strip) {
        Cell cell = cells.get(strip);
        if (cell == null) {
            if (oldCells != null && (cell = oldCells.get(strip)) != null) {
                cells.put(strip, cell);
            } else {
                DataStore ds = lrd.getDataStore();
                if (strip == ds.getAlarmStrip()) cell = new AlarmCell(this, strip); else cell = new Cell(this, strip);
                cells.put(strip, cell);
            }
        }
        return cell;
    }

    /**
	 * Currently specifically for adding alarm data for the 
	 * hard coded alarm strip.
	 *   
	 * @param r
	 * @param updateNumber
	 */
    public void addCellData(SourceAlarmDataResult r, long updateNumber) {
        this.updateNumber = updateNumber;
        final DataStore ds = lrd.datastore;
        final Strip alarmStrip = ds.getAlarmStrip();
        final AlarmCell cell = (AlarmCell) getCreateCell(alarmStrip);
        cell.addAlarmData(r.getAlarmRecords());
    }

    /**
	 * Adds a data result set to a specific MetricCell
	 * @param resultSet
	 * @param stripList
	 */
    public void addCellData(ProcessedChartDataResults resultSet, long updateNumber) {
        this.updateNumber = updateNumber;
        final Strip strip = resultSet.getStrip();
        Cell cell = getCreateCell(strip);
        if (cell != null) cell.addChartData(resultSet.getDataSet()); else System.err.println("Source.addChartData for (source/strip): (" + sourceID + "/" + strip + ") cell does not exist for this source/strip!");
    }

    /**
	 * Indicate a swatch add operation is beginning, 
	 *
	 */
    private void beginSwatchUpdate(long newUpdateNumber) {
        if (updateNumber == newUpdateNumber) return;
        oldCells = cells;
        cells = new Hashtable<Strip, Cell>(cells.size() * 2);
        updateNumber = newUpdateNumber;
    }

    protected void endSwatchUpdate(long updateNumber) {
        if (this.updateNumber != updateNumber) {
            cells.clear();
        }
        oldCells = null;
    }

    /**
	 * Assigns a swatch value to one or more cells as defined by matching the InputChannel
	 * with strips.  During rendering the strip will contain thresholds for interpreting 
	 * the values contained in the swatch record into a small ordered list of values 
	 * which are then interpreted into colors by templates.  
	 * 
	 * @param swatch
	 * @param updateNumber 
	 * @return
	 */
    protected boolean addSwatchData(CellSwatchRecord swatch, long updateNumber) {
        if (this.updateNumber != updateNumber) beginSwatchUpdate(updateNumber);
        DataStore ds = lrd.datastore;
        final String inputChannel = swatch.getInputChannel();
        if (inputChannel.equals(CellSwatchRecord.ALARMSWATCH)) {
            Strip alarmStrip = ds.getAlarmStrip();
            if (alarmStrip == null) return false;
            swatch.setSigFunction(alarmStrip.getSigFunction());
            Cell cell = getCreateCell(alarmStrip);
            cell.setSwatch(swatch);
            cell.checkAddToCritGroup();
            return true;
        }
        List<Strip> stripSet = ds.getChannelStripList(inputChannel);
        final int numStrips = stripSet.size();
        Iterator<Strip> it = stripSet.iterator();
        while (it.hasNext()) {
            final Strip strip = it.next();
            swatch.setSigFunction(strip.getSigFunction());
            Cell cell = getCreateCell(strip);
            cell.setSwatch(swatch);
            cell.checkAddToCritGroup();
        }
        if (numStrips == 0) System.err.println("Source.addSwatchData: unable to locate strip for swatch (" + swatch + ")");
        return numStrips > 0;
    }

    public String toString() {
        try {
            final String splitIndex = (lrd == null || lrd.getSourceAxis() == null || minLine == null) ? "" : Integer.toString(lrd.sourceAxis.getSplitIndex(minLine));
            final String resultString = sourceID + "(" + getNumCells() + ")@" + splitIndex;
            return resultString;
        } catch (NullPointerException e) {
            return sourceName;
        }
    }

    /**
	 * 
	 * @param x World space position
	 * @param stripAxis 
	 * @param pixelSize
	 * @return
	 */
    public Cell pickCell(double x, SplitAxis stripAxis, double pixelSize) {
        SplitLine pickedLine = stripAxis.getSplitFromAbsolute(x, pixelSize, getLRD().getFrameNum());
        if (pickedLine == null) return null;
        int stripPosition = stripAxis.getSplitIndex(pickedLine) + 1;
        Cell result = getCell(stripPosition);
        if (result == null) {
            phonyCell.setSource(this);
            phonyCell.setStrip(lrd.getDataStore().getStripByIndex(stripPosition));
            result = phonyCell;
        }
        return result;
    }

    public boolean match(String searchString, int field) throws PatternSyntaxException {
        searchString = searchString.toUpperCase();
        if (field == SOURCEINDEX) {
            return sourceName.toUpperCase().matches(".*" + searchString + ".*");
        } else if (field == BUSINESSINDEX) {
            return business.toUpperCase().matches(".*" + searchString + ".*");
        } else if (field == CUSTOMERINDEX) {
            return customer.toUpperCase().matches(".*" + searchString + ".*");
        } else if (field == GROUPINDEX) {
            return group.toUpperCase().matches(".*" + searchString + ".*");
        } else if (field == LOCATIONINDEX) {
            return location.toUpperCase().matches(".*" + searchString + ".*");
        } else if (field == SITEINDEX) {
            return site.toUpperCase().matches(".*" + searchString + ".*");
        }
        final String[] fields = { sourceName, business, customer, group, location, site };
        boolean result = false;
        for (int i = 0; i < fields.length; i++) {
            result = fields[i].toUpperCase().matches(".*" + searchString + ".*");
            if (result) return true;
        }
        return false;
    }

    public Hashtable<Strip, Cell> getCells() {
        return cells;
    }

    /**
	 * @deprecated
	 * @return
	 */
    public AccordionLRACDrawer getAd() {
        return getLRD();
    }

    public void setAd(AccordionLRACDrawer lrd) {
        this.lrd = lrd;
    }

    /**
	 * Returns the number of metrics this device is monitoring. 
	 * @return
	 */
    public int getNumCells() {
        return cells.size();
    }

    /**
	 * Returns the name of this device.
	 * @return
	 */
    public String getName() {
        return sourceID;
    }

    public int getHeight() {
        return 1;
    }

    public int getKey() {
        if (minLine != null) return lrd.sourceAxis.getSplitIndex(minLine);
        System.err.println("Source.getKey(): source not in split line tree!");
        return -2;
    }

    /**
	 * @return Returns the topLine.
	 */
    public SplitLine getMinLine() {
        return minLine;
    }

    public SplitLine getMaxLine() {
        return lrd.getSourceAxis().getNextSplit(getMinLine());
    }

    /**
	 * @param minLine The topLine to set.
	 */
    public void setMinLine(SplitLine minLine) {
        this.minLine = minLine;
    }

    public String getBusiness() {
        return business;
    }

    public void setBusiness(String biz) {
        this.business = biz;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String cust) {
        this.customer = cust;
    }

    /**
	 * Returns the swatch significant value for the cell specified by the given strip.
	 * 
	 * 0 is returned if the cell has no data. 
	 * 
	 * @param s
	 * @return
	 */
    public float getSwatchSigValue(Strip s) {
        Cell cell = getCell(s);
        if (cell == null) return -1;
        CellSwatchRecord swatch = cell.getSwatch();
        if (swatch == null) return -1;
        return swatch.getSigValue();
    }

    /**
	 * 
	 * @param strip
	 * @return
	 */
    public int getSwatchNumValues(Strip strip) {
        Cell cell = getCell(strip);
        if (cell == null) return -1;
        CellSwatchRecord swatch = cell.getSwatch();
        if (swatch == null) return -1;
        return swatch.getNumValues();
    }

    public int compareTo(Source source) {
        if (source == null) return -1;
        if (getSortKey().equals(SORTOPTIONS[MANUALINDEX]) && sortStrip != null) {
            int result = 0;
            if (ascend) result = (new Float(source.getSwatchSigValue(sortStrip)).compareTo(getSwatchSigValue(sortStrip))); else result = (new Float(getSwatchSigValue(sortStrip)).compareTo(source.getSwatchSigValue(sortStrip)));
            if (result != 0) return result;
            if (ascend) result = (new Integer(source.getSwatchNumValues(sortStrip)).compareTo(getSwatchNumValues(sortStrip))); else result = (new Integer(getSwatchNumValues(sortStrip)).compareTo(source.getSwatchNumValues(sortStrip)));
            if (result != 0) return result;
            return sourceID.compareTo(source.sourceID);
        }
        final String mySortKeyField = this.getSortKeyField();
        final int compareResult = mySortKeyField.compareTo(source.getSortKeyField());
        if (compareResult != 0) return compareResult;
        return sourceID.compareTo(source.sourceID);
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public AccordionLRACDrawer getLRD() {
        return lrd;
    }

    public static Source sourceFromString(String buffer) {
        String[] tokenString = Util.myTokenize(buffer, TOKENIZER);
        return sourceFromArray(tokenString);
    }

    static Source sourceFromArray(String[] inputArray) {
        if (inputArray.length <= 1) {
            return null;
        }
        Source s = new Source(inputArray[0], null);
        for (int i = 1; i < inputArray.length; ) {
            String key = inputArray[i];
            i++;
            if (key.equals("o")) {
                s.sourceName = inputArray[i];
            } else if (key.equals("b")) {
                s.business = inputArray[i];
            } else if (key.equals("c")) {
                s.customer = inputArray[i];
            } else if (key.equals("g")) {
                s.group = inputArray[i];
            } else if (key.equals("l")) {
                s.location = inputArray[i];
            } else if (key.equals("s")) {
                s.site = inputArray[i];
            } else if (key.equals("t")) {
                s.type = inputArray[i];
            } else {
                System.err.println("unrecognized key: " + key);
            }
            i++;
        }
        return s;
    }

    public static Cell getPhonyCell() {
        return phonyCell;
    }

    public String getLogString() {
        return sourceID + "(" + getNumCells() + ")@" + lrd.sourceAxis.getSplitIndex(minLine);
    }
}

package net.sf.mzmine.project.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.Range;

/**
 * RawDataFile implementation. It provides storage of data points for scans and
 * mass lists using the storeDataPoints() and readDataPoints() methods. The data
 * points are stored in a temporary file (dataPointsFile) and the structure of
 * the file is stored in two TreeMaps. The dataPointsOffsets maps storage ID to
 * the offset in the dataPointsFile. The dataPointsLength maps the storage ID to
 * the number of data points stored under this ID. When stored data points are
 * deleted using removeStoredDataPoints(), the dataPointsFile is not modified,
 * the storage ID is just deleted from the two TreeMaps. When the project is
 * saved, the contents of the dataPointsFile are consolidated - only data points
 * referenced by the TreeMaps are saved (see the RawDataFileSaveHandler class).
 */
public class RawDataFileImpl implements RawDataFile, RawDataFileWriter {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private String dataFileName;

    private final Hashtable<Integer, Range> dataMZRange, dataRTRange;

    private final Hashtable<Integer, Double> dataMaxBasePeakIntensity, dataMaxTIC;

    private final Hashtable<Integer, int[]> scanNumbersCache;

    private ByteBuffer buffer = ByteBuffer.allocate(20000);

    private final TreeMap<Integer, Long> dataPointsOffsets;

    private final TreeMap<Integer, Integer> dataPointsLengths;

    private File dataPointsFileName;

    private RandomAccessFile dataPointsFile;

    /**
     * Scans
     */
    private final Hashtable<Integer, StorableScan> scans;

    public RawDataFileImpl(String dataFileName) throws IOException {
        this.dataFileName = dataFileName;
        scanNumbersCache = new Hashtable<Integer, int[]>();
        dataMZRange = new Hashtable<Integer, Range>();
        dataRTRange = new Hashtable<Integer, Range>();
        dataMaxBasePeakIntensity = new Hashtable<Integer, Double>();
        dataMaxTIC = new Hashtable<Integer, Double>();
        scans = new Hashtable<Integer, StorableScan>();
        dataPointsOffsets = new TreeMap<Integer, Long>();
        dataPointsLengths = new TreeMap<Integer, Integer>();
    }

    /**
     * Create a new temporary data points file
     */
    public static File createNewDataPointsFile() throws IOException {
        return File.createTempFile("mzmine", ".scans");
    }

    /**
     * Returns the (already opened) data points file. Warning: may return null
     * in case no scans have been added yet to this RawDataFileImpl instance
     */
    public RandomAccessFile getDataPointsFile() {
        return dataPointsFile;
    }

    /**
     * Opens the given file as a data points file for this RawDataFileImpl
     * instance. If the file is not empty, the TreeMaps supplied as parameters
     * have to describe the mapping of storage IDs to data points in the file.
     */
    public synchronized void openDataPointsFile(File dataPointsFileName) throws IOException {
        if (this.dataPointsFile != null) {
            throw new IOException("Cannot open another data points file, because one is already open");
        }
        this.dataPointsFileName = dataPointsFileName;
        this.dataPointsFile = new RandomAccessFile(dataPointsFileName, "rw");
        FileChannel fileChannel = dataPointsFile.getChannel();
        fileChannel.lock();
        dataPointsFileName.deleteOnExit();
    }

    /**
     * @see net.sf.mzmine.data.RawDataFile#getNumOfScans()
     */
    public int getNumOfScans() {
        return scans.size();
    }

    /**
     * @see net.sf.mzmine.data.RawDataFile#getScan(int)
     */
    public Scan getScan(int scanNumber) {
        return scans.get(scanNumber);
    }

    /**
     * @see net.sf.mzmine.data.RawDataFile#getScanNumbers(int)
     */
    public int[] getScanNumbers(int msLevel) {
        if (scanNumbersCache.containsKey(msLevel)) return scanNumbersCache.get(msLevel);
        int scanNumbers[] = getScanNumbers(msLevel, new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        scanNumbersCache.put(msLevel, scanNumbers);
        return scanNumbers;
    }

    /**
     * @see net.sf.mzmine.data.RawDataFile#getScanNumbers(int, double, double)
     */
    public int[] getScanNumbers(int msLevel, Range rtRange) {
        assert rtRange != null;
        ArrayList<Integer> eligibleScanNumbers = new ArrayList<Integer>();
        Enumeration<StorableScan> scansEnum = scans.elements();
        while (scansEnum.hasMoreElements()) {
            Scan scan = scansEnum.nextElement();
            if ((scan.getMSLevel() == msLevel) && (rtRange.contains(scan.getRetentionTime()))) eligibleScanNumbers.add(scan.getScanNumber());
        }
        int[] numbersArray = CollectionUtils.toIntArray(eligibleScanNumbers);
        Arrays.sort(numbersArray);
        return numbersArray;
    }

    /**
     * @see net.sf.mzmine.data.RawDataFile#getScanNumbers()
     */
    public int[] getScanNumbers() {
        if (scanNumbersCache.containsKey(0)) return scanNumbersCache.get(0);
        Set<Integer> allScanNumbers = scans.keySet();
        int[] numbersArray = CollectionUtils.toIntArray(allScanNumbers);
        Arrays.sort(numbersArray);
        scanNumbersCache.put(0, numbersArray);
        return numbersArray;
    }

    /**
     * @see net.sf.mzmine.data.RawDataFile#getMSLevels()
     */
    public int[] getMSLevels() {
        Set<Integer> msLevelsSet = new HashSet<Integer>();
        Enumeration<StorableScan> scansEnum = scans.elements();
        while (scansEnum.hasMoreElements()) {
            Scan scan = scansEnum.nextElement();
            msLevelsSet.add(scan.getMSLevel());
        }
        int[] msLevels = CollectionUtils.toIntArray(msLevelsSet);
        Arrays.sort(msLevels);
        return msLevels;
    }

    /**
     * @see net.sf.mzmine.data.RawDataFile#getDataMaxBasePeakIntensity()
     */
    public double getDataMaxBasePeakIntensity(int msLevel) {
        Double maxBasePeak = dataMaxBasePeakIntensity.get(msLevel);
        if (maxBasePeak != null) return maxBasePeak;
        Enumeration<StorableScan> scansEnum = scans.elements();
        while (scansEnum.hasMoreElements()) {
            Scan scan = scansEnum.nextElement();
            if (scan.getMSLevel() != msLevel) continue;
            DataPoint scanBasePeak = scan.getBasePeak();
            if (scanBasePeak == null) continue;
            if ((maxBasePeak == null) || (scanBasePeak.getIntensity() > maxBasePeak)) maxBasePeak = scanBasePeak.getIntensity();
        }
        if (maxBasePeak == null) maxBasePeak = -1d;
        dataMaxBasePeakIntensity.put(msLevel, maxBasePeak);
        return maxBasePeak;
    }

    /**
     * @see net.sf.mzmine.data.RawDataFile#getDataMaxTotalIonCurrent()
     */
    public double getDataMaxTotalIonCurrent(int msLevel) {
        Double maxTIC = dataMaxTIC.get(msLevel);
        if (maxTIC != null) return maxTIC.doubleValue();
        Enumeration<StorableScan> scansEnum = scans.elements();
        while (scansEnum.hasMoreElements()) {
            Scan scan = scansEnum.nextElement();
            if (scan.getMSLevel() != msLevel) continue;
            if ((maxTIC == null) || (scan.getTIC() > maxTIC)) maxTIC = scan.getTIC();
        }
        if (maxTIC == null) maxTIC = -1d;
        dataMaxTIC.put(msLevel, maxTIC);
        return maxTIC;
    }

    public synchronized int storeDataPoints(DataPoint dataPoints[]) throws IOException {
        if (dataPointsFile == null) {
            File newFile = RawDataFileImpl.createNewDataPointsFile();
            openDataPointsFile(newFile);
        }
        final long currentOffset = dataPointsFile.length();
        final int currentID;
        if (!dataPointsOffsets.isEmpty()) currentID = dataPointsOffsets.lastKey() + 1; else currentID = 1;
        final int numOfDataPoints = dataPoints.length;
        final int numOfBytes = numOfDataPoints * 2 * 4;
        if (buffer.capacity() < numOfBytes) {
            buffer = ByteBuffer.allocate(numOfBytes * 2);
        } else {
            buffer.clear();
        }
        FloatBuffer floatBuffer = buffer.asFloatBuffer();
        for (DataPoint dp : dataPoints) {
            floatBuffer.put((float) dp.getMZ());
            floatBuffer.put((float) dp.getIntensity());
        }
        dataPointsFile.seek(currentOffset);
        dataPointsFile.write(buffer.array(), 0, numOfBytes);
        dataPointsOffsets.put(currentID, currentOffset);
        dataPointsLengths.put(currentID, numOfDataPoints);
        return currentID;
    }

    public synchronized DataPoint[] readDataPoints(int ID) throws IOException {
        final Long currentOffset = dataPointsOffsets.get(ID);
        final Integer numOfDataPoints = dataPointsLengths.get(ID);
        if ((currentOffset == null) || (numOfDataPoints == null)) {
            throw new IllegalArgumentException("Unknown storage ID " + ID);
        }
        final int numOfBytes = numOfDataPoints * 2 * 4;
        if (buffer.capacity() < numOfBytes) {
            buffer = ByteBuffer.allocate(numOfBytes * 2);
        } else {
            buffer.clear();
        }
        dataPointsFile.seek(currentOffset);
        dataPointsFile.read(buffer.array(), 0, numOfBytes);
        FloatBuffer floatBuffer = buffer.asFloatBuffer();
        DataPoint dataPoints[] = new DataPoint[numOfDataPoints];
        for (int i = 0; i < numOfDataPoints; i++) {
            float mz = floatBuffer.get();
            float intensity = floatBuffer.get();
            dataPoints[i] = new SimpleDataPoint(mz, intensity);
        }
        return dataPoints;
    }

    public synchronized void removeStoredDataPoints(int ID) throws IOException {
        dataPointsOffsets.remove(ID);
        dataPointsLengths.remove(ID);
    }

    public synchronized void addScan(Scan newScan) throws IOException {
        if (newScan instanceof StorableScan) {
            scans.put(newScan.getScanNumber(), (StorableScan) newScan);
            return;
        }
        DataPoint dataPoints[] = newScan.getDataPoints();
        final int storageID = storeDataPoints(dataPoints);
        StorableScan storedScan = new StorableScan(newScan, this, dataPoints.length, storageID);
        scans.put(newScan.getScanNumber(), storedScan);
    }

    /**
     * @see net.sf.mzmine.data.RawDataFileWriter#finishWriting()
     */
    public synchronized RawDataFile finishWriting() throws IOException {
        for (StorableScan scan : scans.values()) {
            scan.updateValues();
        }
        logger.finest("Writing of scans to file " + dataPointsFileName + " finished");
        return this;
    }

    public Range getDataMZRange() {
        return getDataMZRange(0);
    }

    public Range getDataMZRange(int msLevel) {
        Range mzRange = dataMZRange.get(msLevel);
        if (mzRange != null) return mzRange;
        for (Scan scan : scans.values()) {
            if ((msLevel != 0) && (scan.getMSLevel() != msLevel)) continue;
            if (mzRange == null) mzRange = scan.getMZRange(); else mzRange.extendRange(scan.getMZRange());
        }
        if (mzRange != null) dataMZRange.put(msLevel, mzRange);
        return mzRange;
    }

    public Range getDataRTRange() {
        return getDataRTRange(0);
    }

    public Range getDataRTRange(int msLevel) {
        Range rtRange = dataRTRange.get(msLevel);
        if (rtRange != null) return rtRange;
        for (Scan scan : scans.values()) {
            if ((msLevel != 0) && (scan.getMSLevel() != msLevel)) continue;
            if (rtRange == null) rtRange = new Range(scan.getRetentionTime()); else rtRange.extendRange(scan.getRetentionTime());
        }
        if (rtRange != null) dataRTRange.put(msLevel, rtRange);
        return rtRange;
    }

    public void setRTRange(int msLevel, Range rtRange) {
        dataRTRange.put(msLevel, rtRange);
    }

    public void setMZRange(int msLevel, Range mzRange) {
        dataMZRange.put(msLevel, mzRange);
    }

    public int getNumOfScans(int msLevel) {
        return getScanNumbers(msLevel).length;
    }

    public synchronized TreeMap<Integer, Long> getDataPointsOffsets() {
        return dataPointsOffsets;
    }

    public synchronized TreeMap<Integer, Integer> getDataPointsLengths() {
        return dataPointsLengths;
    }

    public synchronized void close() {
        try {
            dataPointsFile.close();
            dataPointsFileName.delete();
        } catch (IOException e) {
            logger.warning("Could not close file " + dataPointsFileName + ": " + e.toString());
        }
    }

    public String getName() {
        return dataFileName;
    }

    public void setName(String name) {
        this.dataFileName = name;
    }

    public String toString() {
        return dataFileName;
    }
}

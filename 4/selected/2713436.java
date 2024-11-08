package loci.formats;

import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Logic to stitch together files with similar names.
 * Assumes that all files have the same dimensions.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/loci/formats/FileStitcher.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/loci/formats/FileStitcher.java">SVN</a></dd></dl>
 */
public class FileStitcher implements IFormatReader {

    /** FormatReader to use as a template for constituent readers. */
    private IFormatReader reader;

    /**
   * Whether string ids given should be treated
   * as file patterns rather than single file paths.
   */
    private boolean patternIds = false;

    /** Current file pattern string. */
    private String currentId;

    /** File pattern object used to build the list of files. */
    private FilePattern fp;

    /** Axis guesser object used to guess which dimensional axes are which. */
    private AxisGuesser[] ag;

    /** The matching files. */
    private String[] files;

    /** Used files list. */
    private String[] usedFiles;

    /** Reader used for each file. */
    private IFormatReader[] readers;

    /** Blank buffered image, for use when image counts vary between files. */
    private BufferedImage[] blankImage;

    /** Blank image bytes, for use when image counts vary between files. */
    private byte[][] blankBytes;

    /** Blank buffered thumbnail, for use when image counts vary between files. */
    private BufferedImage[] blankThumb;

    /** Blank thumbnail bytes, for use when image counts vary between files. */
    private byte[][] blankThumbBytes;

    /** Number of images per file. */
    private int[] imagesPerFile;

    /** Dimensional axis lengths per file. */
    private int[] sizeZ, sizeC, sizeT;

    /** Component lengths for each axis type. */
    private int[][] lenZ, lenC, lenT;

    /** Core metadata. */
    private CoreMetadata core;

    /** Constructs a FileStitcher around a new image reader. */
    public FileStitcher() {
        this(new ImageReader());
    }

    /**
   * Constructs a FileStitcher around a new image reader.
   * @param patternIds Whether string ids given should be treated as file
   *    patterns rather than single file paths.
   */
    public FileStitcher(boolean patternIds) {
        this(new ImageReader(), patternIds);
    }

    /**
   * Constructs a FileStitcher with the given reader.
   * @param r The reader to use for reading stitched files.
   */
    public FileStitcher(IFormatReader r) {
        this(r, false);
    }

    /**
   * Constructs a FileStitcher with the given reader.
   * @param r The reader to use for reading stitched files.
   * @param patternIds Whether string ids given should be treated as file
   *   patterns rather than single file paths.
   */
    public FileStitcher(IFormatReader r, boolean patternIds) {
        reader = r;
        this.patternIds = patternIds;
    }

    /** Gets the wrapped reader prototype. */
    public IFormatReader getReader() {
        return reader;
    }

    /**
   * Gets the axis type for each dimensional block.
   * @return An array containing values from the enumeration:
   *   <ul>
   *     <li>AxisGuesser.Z_AXIS: focal planes</li>
   *     <li>AxisGuesser.T_AXIS: time points</li>
   *     <li>AxisGuesser.C_AXIS: channels</li>
   *   </ul>
   */
    public int[] getAxisTypes() {
        FormatTools.assertId(currentId, true, 2);
        return ag[getSeries()].getAxisTypes();
    }

    /**
   * Sets the axis type for each dimensional block.
   * @param axes An array containing values from the enumeration:
   *   <ul>
   *     <li>AxisGuesser.Z_AXIS: focal planes</li>
   *     <li>AxisGuesser.T_AXIS: time points</li>
   *     <li>AxisGuesser.C_AXIS: channels</li>
   *   </ul>
   */
    public void setAxisTypes(int[] axes) throws FormatException {
        FormatTools.assertId(currentId, true, 2);
        ag[getSeries()].setAxisTypes(axes);
        computeAxisLengths();
    }

    /** Gets the file pattern object used to build the list of files. */
    public FilePattern getFilePattern() {
        FormatTools.assertId(currentId, true, 2);
        return fp;
    }

    /**
   * Gets the axis guesser object used to guess
   * which dimensional axes are which.
   */
    public AxisGuesser getAxisGuesser() {
        FormatTools.assertId(currentId, true, 2);
        return ag[getSeries()];
    }

    /**
   * Finds the file pattern for the given ID, based on the state of the file
   * stitcher. Takes both ID map entries and the patternIds flag into account.
   */
    public FilePattern findPattern(String id) {
        FormatTools.assertId(currentId, true, 2);
        if (!patternIds) {
            Hashtable map = Location.getIdMap();
            String pattern = null;
            if (map.containsKey(id)) {
                String[] idList = new String[map.size()];
                Enumeration en = map.keys();
                for (int i = 0; i < idList.length; i++) {
                    idList[i] = (String) en.nextElement();
                }
                pattern = FilePattern.findPattern(id, null, idList);
            } else {
                pattern = FilePattern.findPattern(new Location(id));
            }
            if (pattern != null) id = pattern;
        }
        return new FilePattern(id);
    }

    public boolean isThisType(byte[] block) {
        return reader.isThisType(block);
    }

    public void setId(String id) throws FormatException, IOException {
        if (!id.equals(currentId)) initFile(id);
    }

    public void setId(String id, boolean force) throws FormatException, IOException {
        if (!id.equals(currentId) || force) initFile(id);
    }

    public int getImageCount() {
        FormatTools.assertId(currentId, true, 2);
        return core.imageCount[getSeries()];
    }

    public boolean isRGB() {
        FormatTools.assertId(currentId, true, 2);
        return core.rgb[getSeries()];
    }

    public int getSizeX() {
        FormatTools.assertId(currentId, true, 2);
        return core.sizeX[getSeries()];
    }

    public int getSizeY() {
        FormatTools.assertId(currentId, true, 2);
        return core.sizeY[getSeries()];
    }

    public int getSizeZ() {
        FormatTools.assertId(currentId, true, 2);
        return core.sizeZ[getSeries()];
    }

    public int getSizeC() {
        FormatTools.assertId(currentId, true, 2);
        return core.sizeC[getSeries()];
    }

    public int getSizeT() {
        FormatTools.assertId(currentId, true, 2);
        return core.sizeT[getSeries()];
    }

    public int getPixelType() {
        FormatTools.assertId(currentId, true, 2);
        return core.pixelType[getSeries()];
    }

    public int getEffectiveSizeC() {
        FormatTools.assertId(currentId, true, 2);
        return getImageCount() / (getSizeZ() * getSizeT());
    }

    public int getRGBChannelCount() {
        FormatTools.assertId(currentId, true, 2);
        return getSizeC() / getEffectiveSizeC();
    }

    public boolean isIndexed() {
        FormatTools.assertId(currentId, true, 2);
        return reader.isIndexed();
    }

    public boolean isFalseColor() {
        FormatTools.assertId(currentId, true, 2);
        return reader.isFalseColor();
    }

    public byte[][] get8BitLookupTable() throws FormatException, IOException {
        FormatTools.assertId(currentId, true, 2);
        return reader.get8BitLookupTable();
    }

    public short[][] get16BitLookupTable() throws FormatException, IOException {
        FormatTools.assertId(currentId, true, 2);
        return reader.get16BitLookupTable();
    }

    public int[] getChannelDimLengths() {
        FormatTools.assertId(currentId, true, 1);
        return core.cLengths[getSeries()];
    }

    public String[] getChannelDimTypes() {
        FormatTools.assertId(currentId, true, 1);
        return core.cTypes[getSeries()];
    }

    public int getThumbSizeX() {
        FormatTools.assertId(currentId, true, 2);
        return reader.getThumbSizeX();
    }

    public int getThumbSizeY() {
        FormatTools.assertId(currentId, true, 2);
        return reader.getThumbSizeY();
    }

    public boolean isLittleEndian() {
        FormatTools.assertId(currentId, true, 2);
        return reader.isLittleEndian();
    }

    public String getDimensionOrder() {
        FormatTools.assertId(currentId, true, 2);
        return core.currentOrder[getSeries()];
    }

    public boolean isOrderCertain() {
        FormatTools.assertId(currentId, true, 2);
        return core.orderCertain[getSeries()];
    }

    public boolean isInterleaved() {
        FormatTools.assertId(currentId, true, 2);
        return reader.isInterleaved();
    }

    public boolean isInterleaved(int subC) {
        FormatTools.assertId(currentId, true, 2);
        return reader.isInterleaved(subC);
    }

    public BufferedImage openImage(int no) throws FormatException, IOException {
        FormatTools.assertId(currentId, true, 2);
        int[] q = computeIndices(no);
        int fno = q[0], ino = q[1];
        if (ino < readers[fno].getImageCount()) {
            return readers[fno].openImage(ino);
        }
        int sno = getSeries();
        if (blankImage[sno] == null) {
            blankImage[sno] = ImageTools.blankImage(core.sizeX[sno], core.sizeY[sno], sizeC[sno], getPixelType());
        }
        return blankImage[sno];
    }

    public byte[] openBytes(int no) throws FormatException, IOException {
        FormatTools.assertId(currentId, true, 2);
        int[] q = computeIndices(no);
        int fno = q[0], ino = q[1];
        if (ino < readers[fno].getImageCount()) {
            return readers[fno].openBytes(ino);
        }
        int sno = getSeries();
        if (blankBytes[sno] == null) {
            int bytes = FormatTools.getBytesPerPixel(getPixelType());
            blankBytes[sno] = new byte[core.sizeX[sno] * core.sizeY[sno] * bytes * getRGBChannelCount()];
        }
        return blankBytes[sno];
    }

    public byte[] openBytes(int no, byte[] buf) throws FormatException, IOException {
        FormatTools.assertId(currentId, true, 2);
        int[] q = computeIndices(no);
        int fno = q[0], ino = q[1];
        if (ino < readers[fno].getImageCount()) {
            return readers[fno].openBytes(ino, buf);
        }
        Arrays.fill(buf, (byte) 0);
        return buf;
    }

    public BufferedImage openThumbImage(int no) throws FormatException, IOException {
        FormatTools.assertId(currentId, true, 2);
        int[] q = computeIndices(no);
        int fno = q[0], ino = q[1];
        if (ino < readers[fno].getImageCount()) {
            return readers[fno].openThumbImage(ino);
        }
        int sno = getSeries();
        if (blankThumb[sno] == null) {
            blankThumb[sno] = ImageTools.blankImage(getThumbSizeX(), getThumbSizeY(), sizeC[sno], getPixelType());
        }
        return blankThumb[sno];
    }

    public byte[] openThumbBytes(int no) throws FormatException, IOException {
        FormatTools.assertId(currentId, true, 2);
        int[] q = computeIndices(no);
        int fno = q[0], ino = q[1];
        if (ino < readers[fno].getImageCount()) {
            return readers[fno].openThumbBytes(ino);
        }
        int sno = getSeries();
        if (blankThumbBytes[sno] == null) {
            int bytes = FormatTools.getBytesPerPixel(getPixelType());
            blankThumbBytes[sno] = new byte[getThumbSizeX() * getThumbSizeY() * bytes * getRGBChannelCount()];
        }
        return blankThumbBytes[sno];
    }

    public void close(boolean fileOnly) throws IOException {
        if (readers == null) reader.close(fileOnly); else {
            for (int i = 0; i < readers.length; i++) readers[i].close(fileOnly);
        }
        if (!fileOnly) {
            readers = null;
            blankImage = null;
            blankBytes = null;
            currentId = null;
        }
    }

    public void close() throws IOException {
        if (readers == null) reader.close(); else {
            for (int i = 0; i < readers.length; i++) readers[i].close();
        }
        readers = null;
        blankImage = null;
        blankBytes = null;
        currentId = null;
    }

    public int getSeriesCount() {
        FormatTools.assertId(currentId, true, 2);
        return reader.getSeriesCount();
    }

    public void setSeries(int no) {
        FormatTools.assertId(currentId, true, 2);
        reader.setSeries(no);
    }

    public int getSeries() {
        FormatTools.assertId(currentId, true, 2);
        return reader.getSeries();
    }

    public void setGroupFiles(boolean group) {
        for (int i = 0; i < readers.length; i++) readers[i].setGroupFiles(group);
    }

    public boolean isGroupFiles() {
        return readers[0].isGroupFiles();
    }

    public int fileGroupOption(String id) throws FormatException, IOException {
        return readers[0].fileGroupOption(id);
    }

    public boolean isMetadataComplete() {
        return readers[0].isMetadataComplete();
    }

    public void setNormalized(boolean normalize) {
        FormatTools.assertId(currentId, false, 2);
        if (readers == null) reader.setNormalized(normalize); else {
            for (int i = 0; i < readers.length; i++) {
                readers[i].setNormalized(normalize);
            }
        }
    }

    public boolean isNormalized() {
        return reader.isNormalized();
    }

    public void setMetadataCollected(boolean collect) {
        FormatTools.assertId(currentId, false, 2);
        if (readers == null) reader.setMetadataCollected(collect); else {
            for (int i = 0; i < readers.length; i++) {
                readers[i].setMetadataCollected(collect);
            }
        }
    }

    public boolean isMetadataCollected() {
        return reader.isMetadataCollected();
    }

    public void setOriginalMetadataPopulated(boolean populate) {
        FormatTools.assertId(currentId, false, 1);
        if (readers == null) reader.setOriginalMetadataPopulated(populate); else {
            for (int i = 0; i < readers.length; i++) {
                readers[i].setOriginalMetadataPopulated(populate);
            }
        }
    }

    public boolean isOriginalMetadataPopulated() {
        return reader.isOriginalMetadataPopulated();
    }

    public String[] getUsedFiles() {
        FormatTools.assertId(currentId, true, 2);
        if (reader.getUsedFiles().length > 1) {
            if (usedFiles == null) {
                String[][] used = new String[files.length][];
                int total = 0;
                for (int i = 0; i < files.length; i++) {
                    try {
                        readers[i].setId(files[i]);
                    } catch (FormatException exc) {
                        LogTools.trace(exc);
                        return null;
                    } catch (IOException exc) {
                        LogTools.trace(exc);
                        return null;
                    }
                    used[i] = readers[i].getUsedFiles();
                    total += used[i].length;
                }
                usedFiles = new String[total];
                for (int i = 0, off = 0; i < used.length; i++) {
                    System.arraycopy(used[i], 0, usedFiles, off, used[i].length);
                    off += used[i].length;
                }
            }
            return usedFiles;
        }
        return files;
    }

    public String getCurrentFile() {
        return currentId;
    }

    public int getIndex(int z, int c, int t) {
        return FormatTools.getIndex(this, z, c, t);
    }

    public int[] getZCTCoords(int index) {
        return FormatTools.getZCTCoords(this, index);
    }

    public Object getMetadataValue(String field) {
        FormatTools.assertId(currentId, true, 2);
        return reader.getMetadataValue(field);
    }

    public Hashtable getMetadata() {
        FormatTools.assertId(currentId, true, 2);
        return reader.getMetadata();
    }

    public CoreMetadata getCoreMetadata() {
        FormatTools.assertId(currentId, true, 2);
        return core;
    }

    public void setMetadataFiltered(boolean filter) {
        FormatTools.assertId(currentId, false, 2);
        reader.setMetadataFiltered(filter);
    }

    public boolean isMetadataFiltered() {
        return reader.isMetadataFiltered();
    }

    public void setMetadataStore(MetadataStore store) {
        FormatTools.assertId(currentId, false, 2);
        reader.setMetadataStore(store);
    }

    public MetadataStore getMetadataStore() {
        FormatTools.assertId(currentId, true, 2);
        return reader.getMetadataStore();
    }

    public Object getMetadataStoreRoot() {
        FormatTools.assertId(currentId, true, 2);
        return reader.getMetadataStoreRoot();
    }

    public boolean isThisType(String name) {
        return reader.isThisType(name);
    }

    public boolean isThisType(String name, boolean open) {
        return reader.isThisType(name, open);
    }

    public String getFormat() {
        FormatTools.assertId(currentId, true, 2);
        return reader.getFormat();
    }

    public String[] getSuffixes() {
        return reader.getSuffixes();
    }

    public void addStatusListener(StatusListener l) {
        if (readers == null) reader.addStatusListener(l); else {
            for (int i = 0; i < readers.length; i++) readers[i].addStatusListener(l);
        }
    }

    public void removeStatusListener(StatusListener l) {
        if (readers == null) reader.removeStatusListener(l); else {
            for (int i = 0; i < readers.length; i++) readers[i].removeStatusListener(l);
        }
    }

    public StatusListener[] getStatusListeners() {
        return reader.getStatusListeners();
    }

    /** Initializes the given file. */
    protected void initFile(String id) throws FormatException, IOException {
        if (FormatHandler.debug) {
            LogTools.println("calling FileStitcher.initFile(" + id + ")");
        }
        currentId = id;
        fp = findPattern(id);
        String msg = " Please rename your files or disable file stitching.";
        if (!fp.isValid()) {
            throw new FormatException("Invalid " + (patternIds ? "file pattern" : "filename") + " (" + currentId + "): " + fp.getErrorMessage() + msg);
        }
        files = fp.getFiles();
        if (files == null) {
            throw new FormatException("No files matching pattern (" + fp.getPattern() + "). " + msg);
        }
        for (int i = 0; i < files.length; i++) {
            if (!new Location(files[i]).exists()) {
                throw new FormatException("File #" + i + " (" + files[i] + ") does not exist.");
            }
        }
        Vector classes = new Vector();
        IFormatReader r = reader;
        while (r instanceof ReaderWrapper) {
            classes.add(r.getClass());
            r = ((ReaderWrapper) r).getReader();
        }
        if (r instanceof ImageReader) r = ((ImageReader) r).getReader(files[0]);
        classes.add(r.getClass());
        readers = new IFormatReader[files.length];
        readers[0] = reader;
        for (int i = 1; i < readers.length; i++) {
            try {
                r = null;
                for (int j = classes.size() - 1; j >= 0; j--) {
                    Class c = (Class) classes.elementAt(j);
                    if (r == null) r = (IFormatReader) c.newInstance(); else {
                        r = (IFormatReader) c.getConstructor(new Class[] { IFormatReader.class }).newInstance(new Object[] { r });
                    }
                }
                readers[i] = (IFormatReader) r;
            } catch (InstantiationException exc) {
                LogTools.trace(exc);
            } catch (IllegalAccessException exc) {
                LogTools.trace(exc);
            } catch (NoSuchMethodException exc) {
                LogTools.trace(exc);
            } catch (InvocationTargetException exc) {
                LogTools.trace(exc);
            }
        }
        boolean normalized = reader.isNormalized();
        boolean metadataFiltered = reader.isMetadataFiltered();
        boolean metadataCollected = reader.isMetadataCollected();
        StatusListener[] statusListeners = reader.getStatusListeners();
        for (int i = 1; i < readers.length; i++) {
            readers[i].setNormalized(normalized);
            readers[i].setMetadataFiltered(metadataFiltered);
            readers[i].setMetadataCollected(metadataCollected);
            for (int j = 0; j < statusListeners.length; j++) {
                readers[i].addStatusListener(statusListeners[j]);
            }
        }
        reader.setId(files[0]);
        int seriesCount = reader.getSeriesCount();
        ag = new AxisGuesser[seriesCount];
        blankImage = new BufferedImage[seriesCount];
        blankBytes = new byte[seriesCount][];
        blankThumb = new BufferedImage[seriesCount];
        blankThumbBytes = new byte[seriesCount][];
        imagesPerFile = new int[seriesCount];
        sizeZ = new int[seriesCount];
        sizeC = new int[seriesCount];
        sizeT = new int[seriesCount];
        boolean[] certain = new boolean[seriesCount];
        lenZ = new int[seriesCount][];
        lenC = new int[seriesCount][];
        lenT = new int[seriesCount][];
        core = new CoreMetadata(seriesCount);
        int oldSeries = reader.getSeries();
        for (int i = 0; i < seriesCount; i++) {
            reader.setSeries(i);
            core.sizeX[i] = reader.getSizeX();
            core.sizeY[i] = reader.getSizeY();
            core.pixelType[i] = reader.getPixelType();
            imagesPerFile[i] = reader.getImageCount();
            core.imageCount[i] = files.length * imagesPerFile[i];
            core.thumbSizeX[i] = reader.getThumbSizeX();
            core.thumbSizeY[i] = reader.getThumbSizeY();
            core.currentOrder[i] = reader.getDimensionOrder();
            core.rgb[i] = reader.isRGB();
            core.littleEndian[i] = reader.isLittleEndian();
            core.interleaved[i] = reader.isInterleaved();
            core.seriesMetadata[i] = reader.getMetadata();
            sizeZ[i] = reader.getSizeZ();
            sizeC[i] = reader.getSizeC();
            sizeT[i] = reader.getSizeT();
            certain[i] = reader.isOrderCertain();
        }
        reader.setSeries(oldSeries);
        for (int i = 0; i < seriesCount; i++) {
            ag[i] = new AxisGuesser(fp, core.currentOrder[i], sizeZ[i], sizeT[i], sizeC[i], certain[i]);
        }
        for (int i = 0; i < seriesCount; i++) {
            setSeries(i);
            core.currentOrder[i] = ag[i].getAdjustedOrder();
            core.orderCertain[i] = ag[i].isCertain();
            computeAxisLengths();
        }
        setSeries(oldSeries);
        usedFiles = null;
    }

    /** Computes axis length arrays, and total axis lengths. */
    protected void computeAxisLengths() throws FormatException {
        int sno = getSeries();
        int[] count = fp.getCount();
        int[] axes = ag[sno].getAxisTypes();
        int numZ = ag[sno].getAxisCountZ();
        int numC = ag[sno].getAxisCountC();
        int numT = ag[sno].getAxisCountT();
        core.sizeZ[sno] = sizeZ[sno];
        core.sizeC[sno] = sizeC[sno];
        core.sizeT[sno] = sizeT[sno];
        lenZ[sno] = new int[numZ + 1];
        lenC[sno] = new int[numC + 1];
        lenT[sno] = new int[numT + 1];
        lenZ[sno][0] = sizeZ[sno];
        lenC[sno][0] = sizeC[sno];
        lenT[sno][0] = sizeT[sno];
        for (int i = 0, z = 1, c = 1, t = 1; i < axes.length; i++) {
            switch(axes[i]) {
                case AxisGuesser.Z_AXIS:
                    core.sizeZ[sno] *= count[i];
                    lenZ[sno][z++] = count[i];
                    break;
                case AxisGuesser.C_AXIS:
                    core.sizeC[sno] *= count[i];
                    lenC[sno][c++] = count[i];
                    break;
                case AxisGuesser.T_AXIS:
                    core.sizeT[sno] *= count[i];
                    lenT[sno][t++] = count[i];
                    break;
                default:
                    throw new FormatException("Unknown axis type for axis #" + i + ": " + axes[i]);
            }
        }
        int[] cLengths = reader.getChannelDimLengths();
        String[] cTypes = reader.getChannelDimTypes();
        int cCount = 0;
        for (int i = 0; i < cLengths.length; i++) {
            if (cLengths[i] > 1) cCount++;
        }
        for (int i = 1; i < lenC[sno].length; i++) {
            if (lenC[sno][i] > 1) cCount++;
        }
        if (cCount == 0) {
            core.cLengths[sno] = new int[] { 1 };
            core.cTypes[sno] = new String[] { FormatTools.CHANNEL };
        } else {
            core.cLengths[sno] = new int[cCount];
            core.cTypes[sno] = new String[cCount];
        }
        int c = 0;
        for (int i = 0; i < cLengths.length; i++) {
            if (cLengths[i] == 1) continue;
            core.cLengths[sno][c] = cLengths[i];
            core.cTypes[sno][c] = cTypes[i];
            c++;
        }
        for (int i = 1; i < lenC[sno].length; i++) {
            if (lenC[sno][i] == 1) continue;
            core.cLengths[sno][c] = lenC[sno][i];
            core.cTypes[sno][c] = FormatTools.CHANNEL;
        }
        int pixelType = getPixelType();
        boolean little = reader.isLittleEndian();
        MetadataStore s = reader.getMetadataStore();
        s.setPixels(new Integer(core.sizeX[sno]), new Integer(core.sizeY[sno]), new Integer(core.sizeZ[sno]), new Integer(core.sizeC[sno]), new Integer(core.sizeT[sno]), new Integer(pixelType), new Boolean(!little), core.currentOrder[sno], new Integer(sno), null);
    }

    /**
   * Gets the file index, and image index into that file,
   * corresponding to the given global image index.
   *
   * @return An array of size 2, dimensioned {file index, image index}.
   */
    protected int[] computeIndices(int no) throws FormatException, IOException {
        int sno = getSeries();
        int[] axes = ag[sno].getAxisTypes();
        int[] count = fp.getCount();
        int[] zct = getZCTCoords(no);
        zct[1] *= getRGBChannelCount();
        int[] posZ = FormatTools.rasterToPosition(lenZ[sno], zct[0]);
        int[] posC = FormatTools.rasterToPosition(lenC[sno], zct[1]);
        int[] posT = FormatTools.rasterToPosition(lenT[sno], zct[2]);
        int[] pos = new int[axes.length];
        int z = 1, c = 1, t = 1;
        for (int i = 0; i < axes.length; i++) {
            if (axes[i] == AxisGuesser.Z_AXIS) pos[i] = posZ[z++]; else if (axes[i] == AxisGuesser.C_AXIS) pos[i] = posC[c++]; else if (axes[i] == AxisGuesser.T_AXIS) pos[i] = posT[t++]; else {
                throw new FormatException("Unknown axis type for axis #" + i + ": " + axes[i]);
            }
        }
        int fno = FormatTools.positionToRaster(count, pos);
        readers[fno].setId(files[fno]);
        readers[fno].setSeries(reader.getSeries());
        int ino;
        if (posZ[0] < readers[fno].getSizeZ() && posC[0] < readers[fno].getSizeC() && posT[0] < readers[fno].getSizeT()) {
            ino = FormatTools.getIndex(readers[fno], posZ[0], posC[0], posT[0]);
        } else ino = Integer.MAX_VALUE;
        return new int[] { fno, ino };
    }

    /**
   * Gets a list of readers to include in relation to the given C position.
   * @return Array with indices corresponding to the list of readers, and
   *   values indicating the internal channel index to use for that reader.
   */
    protected int[] getIncludeList(int theC) throws FormatException, IOException {
        int[] include = new int[readers.length];
        Arrays.fill(include, -1);
        for (int t = 0; t < sizeT[getSeries()]; t++) {
            for (int z = 0; z < sizeZ[getSeries()]; z++) {
                int no = getIndex(z, theC, t);
                int[] q = computeIndices(no);
                int fno = q[0], ino = q[1];
                include[fno] = ino;
            }
        }
        return include;
    }

    /** @deprecated Replaced by {@link #getAxisTypes()} */
    public int[] getAxisTypes(String id) throws FormatException, IOException {
        setId(id);
        return getAxisTypes();
    }

    /** @deprecated Replaced by {@link #setAxisTypes(int[])} */
    public void setAxisTypes(String id, int[] axes) throws FormatException, IOException {
        setId(id);
        setAxisTypes(axes);
    }

    /** @deprecated Replaced by {@link #getFilePattern()} */
    public FilePattern getFilePattern(String id) throws FormatException, IOException {
        setId(id);
        return getFilePattern();
    }

    /** @deprecated Replaced by {@link #getAxisGuesser()} */
    public AxisGuesser getAxisGuesser(String id) throws FormatException, IOException {
        setId(id);
        return getAxisGuesser();
    }

    /** @deprecated Replaced by {@link #getImageCount()} */
    public int getImageCount(String id) throws FormatException, IOException {
        setId(id);
        return getImageCount();
    }

    /** @deprecated Replaced by {@link #isRGB()} */
    public boolean isRGB(String id) throws FormatException, IOException {
        setId(id);
        return isRGB();
    }

    /** @deprecated Replaced by {@link #getSizeX()} */
    public int getSizeX(String id) throws FormatException, IOException {
        setId(id);
        return getSizeX();
    }

    /** @deprecated Replaced by {@link #getSizeY()} */
    public int getSizeY(String id) throws FormatException, IOException {
        setId(id);
        return getSizeY();
    }

    /** @deprecated Replaced by {@link #getSizeZ()} */
    public int getSizeZ(String id) throws FormatException, IOException {
        setId(id);
        return getSizeZ();
    }

    /** @deprecated Replaced by {@link #getSizeC()} */
    public int getSizeC(String id) throws FormatException, IOException {
        setId(id);
        return getSizeC();
    }

    /** @deprecated Replaced by {@link #getSizeT()} */
    public int getSizeT(String id) throws FormatException, IOException {
        setId(id);
        return getSizeT();
    }

    /** @deprecated Replaced by {@link #getPixelType()} */
    public int getPixelType(String id) throws FormatException, IOException {
        setId(id);
        return getPixelType();
    }

    /** @deprecated Replaced by {@link #getEffectiveSizeC()} */
    public int getEffectiveSizeC(String id) throws FormatException, IOException {
        setId(id);
        return getEffectiveSizeC();
    }

    /** @deprecated Replaced by {@link #getRGBChannelCount()} */
    public int getRGBChannelCount(String id) throws FormatException, IOException {
        setId(id);
        return getSizeC() / getEffectiveSizeC();
    }

    /** @deprecated Replaced by {@link #getChannelDimLengths()} */
    public int[] getChannelDimLengths(String id) throws FormatException, IOException {
        setId(id);
        return getChannelDimLengths();
    }

    /** @deprecated Replaced by {@link #getChannelDimTypes()} */
    public String[] getChannelDimTypes(String id) throws FormatException, IOException {
        setId(id);
        return getChannelDimTypes();
    }

    /** @deprecated Replaced by {@link #getThumbSizeX()} */
    public int getThumbSizeX(String id) throws FormatException, IOException {
        setId(id);
        return getThumbSizeX();
    }

    /** @deprecated Replaced by {@link #getThumbSizeY()} */
    public int getThumbSizeY(String id) throws FormatException, IOException {
        setId(id);
        return getThumbSizeY();
    }

    /** @deprecated Replaced by {@link #isLittleEndian()} */
    public boolean isLittleEndian(String id) throws FormatException, IOException {
        setId(id);
        return isLittleEndian();
    }

    /** @deprecated Replaced by {@link #getDimensionOrder()} */
    public String getDimensionOrder(String id) throws FormatException, IOException {
        setId(id);
        return getDimensionOrder();
    }

    /** @deprecated Replaced by {@link #isOrderCertain()} */
    public boolean isOrderCertain(String id) throws FormatException, IOException {
        setId(id);
        return isOrderCertain();
    }

    /** @deprecated Replaced by {@link #isInterleaved()} */
    public boolean isInterleaved(String id) throws FormatException, IOException {
        setId(id);
        return isInterleaved();
    }

    /** @deprecated Replaced by {@link #isInterleaved(int)} */
    public boolean isInterleaved(String id, int subC) throws FormatException, IOException {
        setId(id);
        return isInterleaved(subC);
    }

    /** @deprecated Replaced by {@link #openImage(int)} */
    public BufferedImage openImage(String id, int no) throws FormatException, IOException {
        setId(id);
        return openImage(no);
    }

    /** @deprecated Replaced by {@link #openBytes(int)} */
    public byte[] openBytes(String id, int no) throws FormatException, IOException {
        setId(id);
        return openBytes(no);
    }

    /** @deprecated Replaced by {@link #openBytes(int, byte[])} */
    public byte[] openBytes(String id, int no, byte[] buf) throws FormatException, IOException {
        setId(id);
        return openBytes(no, buf);
    }

    /** @deprecated Replaced by {@link #openThumbImage(int)} */
    public BufferedImage openThumbImage(String id, int no) throws FormatException, IOException {
        setId(id);
        return openThumbImage(no);
    }

    /** @deprecated Replaced by {@link #openThumbImage(int)} */
    public byte[] openThumbBytes(String id, int no) throws FormatException, IOException {
        setId(id);
        return openThumbBytes(no);
    }

    /** @deprecated Replaced by {@link #getSeriesCount()} */
    public int getSeriesCount(String id) throws FormatException, IOException {
        setId(id);
        return getSeriesCount();
    }

    /** @deprecated Replaced by {@link #setSeries(int)} */
    public void setSeries(String id, int no) throws FormatException, IOException {
        setId(id);
        setSeries(no);
    }

    /** @deprecated Replaced by {@link #getSeries()} */
    public int getSeries(String id) throws FormatException, IOException {
        setId(id);
        return getSeries();
    }

    /** @deprecated Replaced by {@link #getUsedFiles()} */
    public String[] getUsedFiles(String id) throws FormatException, IOException {
        setId(id);
        return getUsedFiles();
    }

    /** @deprecated Replaced by {@link #getIndex(int, int, int)} */
    public int getIndex(String id, int z, int c, int t) throws FormatException, IOException {
        setId(id);
        return getIndex(z, c, t);
    }

    /** @deprecated Replaced by {@link #getZCTCoords(int)} */
    public int[] getZCTCoords(String id, int index) throws FormatException, IOException {
        setId(id);
        return getZCTCoords(index);
    }

    /** @deprecated Replaced by {@link #getMetadataValue(String)} */
    public Object getMetadataValue(String id, String field) throws FormatException, IOException {
        setId(id);
        return getMetadataValue(field);
    }

    /** @deprecated Replaced by {@link #getMetadata()} */
    public Hashtable getMetadata(String id) throws FormatException, IOException {
        setId(id);
        return getMetadata();
    }

    /** @deprecated Replaced by {@link #getCoreMetadata()} */
    public CoreMetadata getCoreMetadata(String id) throws FormatException, IOException {
        setId(id);
        return getCoreMetadata();
    }

    /** @deprecated Replaced by {@link #getMetadataStore()} */
    public MetadataStore getMetadataStore(String id) throws FormatException, IOException {
        setId(id);
        return getMetadataStore();
    }

    /** @deprecated Replaced by {@link #getMetadataStoreRoot()} */
    public Object getMetadataStoreRoot(String id) throws FormatException, IOException {
        setId(id);
        return getMetadataStoreRoot();
    }
}

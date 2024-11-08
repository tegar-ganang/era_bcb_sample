package loci.formats;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.*;

/**
 * Abstract superclass of all biological file format readers.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/loci/formats/FormatReader.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/loci/formats/FormatReader.java">SVN</a></dd></dl>
 */
public abstract class FormatReader extends FormatHandler implements IFormatReader {

    /** Default thumbnail width and height. */
    protected static final int THUMBNAIL_DIMENSION = 128;

    /** Current file. */
    protected RandomAccessStream in;

    /** Hashtable containing metadata key/value pairs. */
    protected Hashtable metadata;

    /** The number of the current series. */
    protected int series = 0;

    /** Core metadata values. */
    protected CoreMetadata core;

    /** Whether or not to normalize float data. */
    protected boolean normalizeData;

    /** Whether or not to filter out invalid metadata. */
    protected boolean filterMetadata;

    /** Whether or not to collect metadata. */
    protected boolean collectMetadata = true;

    /** Whether or not to save proprietary metadata in the MetadataStore. */
    protected boolean saveOriginalMetadata = false;

    /** Whether or not to group multi-file formats. */
    protected boolean group = true;

    /**
   * Current metadata store. Should <b>never</b> be accessed directly as the
   * semantics of {@link #getMetadataStore()} prevent "null" access.
   */
    protected MetadataStore metadataStore = new DummyMetadata();

    /** Constructs a format reader with the given name and default suffix. */
    public FormatReader(String format, String suffix) {
        super(format, suffix);
    }

    /** Constructs a format reader with the given name and default suffixes. */
    public FormatReader(String format, String[] suffixes) {
        super(format, suffixes);
    }

    /**
   * Initializes the given file (parsing header information, etc.).
   * Most subclasses should override this method to perform
   * initialization operations such as parsing metadata.
   */
    protected void initFile(String id) throws FormatException, IOException {
        if (currentId != null) {
            String[] s = getUsedFiles();
            for (int i = 0; i < s.length; i++) {
                if (id.equals(s[i])) return;
            }
        }
        series = 0;
        close();
        currentId = id;
        metadata = new Hashtable();
        core = new CoreMetadata(1);
        Arrays.fill(core.orderCertain, true);
        getMetadataStore().createRoot();
    }

    /**
   * Opens the given file, reads in the first few KB and calls
   * isThisType(byte[]) to check whether it matches this format.
   */
    protected boolean checkBytes(String name, int maxLen) {
        try {
            RandomAccessStream ras = new RandomAccessStream(name);
            long len = ras.length();
            byte[] buf = new byte[len < maxLen ? (int) len : maxLen];
            ras.readFully(buf);
            ras.close();
            return isThisType(buf);
        } catch (IOException exc) {
            return false;
        }
    }

    /** Returns true if the given file name is in the used files list. */
    protected boolean isUsedFile(String file) {
        String[] usedFiles = getUsedFiles();
        for (int i = 0; i < usedFiles.length; i++) {
            if (usedFiles[i].equals(file) || usedFiles[i].equals(new Location(file).getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }

    /** Adds an entry to the metadata table. */
    protected void addMeta(String key, Object value) {
        if (key == null || value == null || !collectMetadata) return;
        if (filterMetadata) {
            if (key.length() == 0) return;
            String val = value.toString();
            if (val.length() == 0) return;
            if (key.charAt(0) < 32) return;
            if (val.charAt(0) < 32) return;
            if (!key.matches(".*[a-zA-Z].*")) return;
        }
        if (saveOriginalMetadata) {
            MetadataStore store = getMetadataStore();
            if (MetadataTools.isOMEXMLMetadata(store)) {
                MetadataTools.populateOriginalMetadata(store, key, value.toString());
            }
        }
        metadata.put(key, value);
    }

    /** Gets a value from the metadata table. */
    protected Object getMeta(String key) {
        return metadata.get(key);
    }

    public int getImageCount() {
        FormatTools.assertId(currentId, true, 1);
        return core.imageCount[series];
    }

    public boolean isRGB() {
        FormatTools.assertId(currentId, true, 1);
        return core.rgb[series];
    }

    public int getSizeX() {
        FormatTools.assertId(currentId, true, 1);
        return core.sizeX[series];
    }

    public int getSizeY() {
        FormatTools.assertId(currentId, true, 1);
        return core.sizeY[series];
    }

    public int getSizeZ() {
        FormatTools.assertId(currentId, true, 1);
        return core.sizeZ[series];
    }

    public int getSizeC() {
        FormatTools.assertId(currentId, true, 1);
        return core.sizeC[series];
    }

    public int getSizeT() {
        FormatTools.assertId(currentId, true, 1);
        return core.sizeT[series];
    }

    public int getPixelType() {
        FormatTools.assertId(currentId, true, 1);
        return core.pixelType[series];
    }

    public int getEffectiveSizeC() {
        return getImageCount() / (getSizeZ() * getSizeT());
    }

    public int getRGBChannelCount() {
        return getSizeC() / getEffectiveSizeC();
    }

    public boolean isIndexed() {
        FormatTools.assertId(currentId, true, 1);
        return core.indexed[series];
    }

    public boolean isFalseColor() {
        FormatTools.assertId(currentId, true, 1);
        return core.falseColor[series];
    }

    public byte[][] get8BitLookupTable() throws FormatException, IOException {
        return null;
    }

    public short[][] get16BitLookupTable() throws FormatException, IOException {
        return null;
    }

    public int[] getChannelDimLengths() {
        FormatTools.assertId(currentId, true, 1);
        if (core.cLengths[series] == null) {
            core.cLengths[series] = new int[] { core.sizeC[series] };
        }
        return core.cLengths[series];
    }

    public String[] getChannelDimTypes() {
        FormatTools.assertId(currentId, true, 1);
        if (core.cTypes[series] == null) {
            core.cTypes[series] = new String[] { FormatTools.CHANNEL };
        }
        return core.cTypes[series];
    }

    public int getThumbSizeX() {
        FormatTools.assertId(currentId, true, 1);
        if (core.thumbSizeX[series] == 0) {
            int sx = getSizeX();
            int sy = getSizeY();
            core.thumbSizeX[series] = sx > sy ? THUMBNAIL_DIMENSION : sx * THUMBNAIL_DIMENSION / sy;
        }
        return core.thumbSizeX[series];
    }

    public int getThumbSizeY() {
        FormatTools.assertId(currentId, true, 1);
        if (core.thumbSizeY[series] == 0) {
            int sx = getSizeX();
            int sy = getSizeY();
            core.thumbSizeY[series] = sy > sx ? THUMBNAIL_DIMENSION : sy * THUMBNAIL_DIMENSION / sx;
        }
        return core.thumbSizeY[series];
    }

    public boolean isLittleEndian() {
        FormatTools.assertId(currentId, true, 1);
        return core.littleEndian[series];
    }

    public String getDimensionOrder() {
        FormatTools.assertId(currentId, true, 1);
        return core.currentOrder[series];
    }

    public boolean isOrderCertain() {
        FormatTools.assertId(currentId, true, 1);
        return core.orderCertain[series];
    }

    public boolean isInterleaved() {
        return isInterleaved(0);
    }

    public boolean isInterleaved(int subC) {
        FormatTools.assertId(currentId, true, 1);
        return core.interleaved[series];
    }

    public byte[] openBytes(int no) throws FormatException, IOException {
        byte[] buf = new byte[getSizeX() * getSizeY() * getRGBChannelCount() * FormatTools.getBytesPerPixel(getPixelType())];
        return openBytes(no, buf);
    }

    public BufferedImage openImage(int no) throws FormatException, IOException {
        byte[] buf = openBytes(no);
        if (getPixelType() == FormatTools.FLOAT) {
            float[] f = (float[]) DataTools.makeDataArray(buf, 4, true, isLittleEndian());
            if (normalizeData) f = DataTools.normalizeFloats(f);
            return ImageTools.makeImage(f, core.sizeX[series], core.sizeY[series], getRGBChannelCount(), true);
        }
        BufferedImage b = ImageTools.makeImage(buf, core.sizeX[series], core.sizeY[series], isIndexed() ? 1 : getRGBChannelCount(), core.interleaved[0], FormatTools.getBytesPerPixel(core.pixelType[series]), core.littleEndian[series]);
        if (isIndexed()) {
            IndexedColorModel model = null;
            if (core.pixelType[series] == FormatTools.UINT8 || core.pixelType[series] == FormatTools.INT8) {
                byte[][] table = get8BitLookupTable();
                model = new IndexedColorModel(8, table[0].length, table);
            } else if (core.pixelType[series] == FormatTools.UINT16 || core.pixelType[series] == FormatTools.INT16) {
                short[][] table = get16BitLookupTable();
                model = new IndexedColorModel(16, table[0].length, table);
            }
            if (model != null) {
                WritableRaster raster = Raster.createWritableRaster(b.getSampleModel(), b.getData().getDataBuffer(), null);
                b = new BufferedImage(model, raster, false, null);
            }
        }
        return b;
    }

    public BufferedImage openThumbImage(int no) throws FormatException, IOException {
        FormatTools.assertId(currentId, true, 1);
        return ImageTools.scale(openImage(no), getThumbSizeX(), getThumbSizeY(), false);
    }

    public byte[] openThumbBytes(int no) throws FormatException, IOException {
        FormatTools.assertId(currentId, true, 1);
        BufferedImage img = openThumbImage(no);
        byte[][] bytes = ImageTools.getPixelBytes(img, core.littleEndian[series]);
        if (bytes.length == 1) return bytes[0];
        byte[] rtn = new byte[getRGBChannelCount() * bytes[0].length];
        for (int i = 0; i < getRGBChannelCount(); i++) {
            System.arraycopy(bytes[i], 0, rtn, bytes[0].length * i, bytes[i].length);
        }
        return rtn;
    }

    public void close(boolean fileOnly) throws IOException {
        if (fileOnly) {
            if (in != null) in.close();
        } else close();
    }

    public int getSeriesCount() {
        FormatTools.assertId(currentId, true, 1);
        return core.sizeX.length;
    }

    public void setSeries(int no) {
        if (no < 0 || no >= getSeriesCount()) {
            throw new IllegalArgumentException("Invalid series: " + no);
        }
        series = no;
    }

    public int getSeries() {
        return series;
    }

    public void setGroupFiles(boolean groupFiles) {
        FormatTools.assertId(currentId, false, 1);
        group = groupFiles;
    }

    public boolean isGroupFiles() {
        return group;
    }

    public int fileGroupOption(String id) throws FormatException, IOException {
        return FormatTools.CANNOT_GROUP;
    }

    public boolean isMetadataComplete() {
        FormatTools.assertId(currentId, true, 1);
        return core.metadataComplete[series];
    }

    public void setNormalized(boolean normalize) {
        FormatTools.assertId(currentId, false, 1);
        normalizeData = normalize;
    }

    public boolean isNormalized() {
        return normalizeData;
    }

    public void setMetadataCollected(boolean collect) {
        FormatTools.assertId(currentId, false, 1);
        collectMetadata = collect;
    }

    public boolean isMetadataCollected() {
        return collectMetadata;
    }

    public void setOriginalMetadataPopulated(boolean populate) {
        FormatTools.assertId(currentId, false, 1);
        saveOriginalMetadata = populate;
    }

    public boolean isOriginalMetadataPopulated() {
        return saveOriginalMetadata;
    }

    public String[] getUsedFiles() {
        FormatTools.assertId(currentId, true, 1);
        return new String[] { currentId };
    }

    public String getCurrentFile() {
        return currentId == null ? "" : currentId;
    }

    public int getIndex(int z, int c, int t) {
        FormatTools.assertId(currentId, true, 1);
        return FormatTools.getIndex(this, z, c, t);
    }

    public int[] getZCTCoords(int index) {
        FormatTools.assertId(currentId, true, 1);
        return FormatTools.getZCTCoords(this, index);
    }

    public Object getMetadataValue(String field) {
        FormatTools.assertId(currentId, true, 1);
        return getMeta(field);
    }

    public Hashtable getMetadata() {
        FormatTools.assertId(currentId, true, 1);
        return metadata;
    }

    public CoreMetadata getCoreMetadata() {
        FormatTools.assertId(currentId, true, 1);
        return core;
    }

    public void setMetadataFiltered(boolean filter) {
        FormatTools.assertId(currentId, false, 1);
        filterMetadata = filter;
    }

    public boolean isMetadataFiltered() {
        return filterMetadata;
    }

    public void setMetadataStore(MetadataStore store) {
        FormatTools.assertId(currentId, false, 1);
        if (store == null) {
            throw new IllegalArgumentException("Metadata object is null");
        }
        metadataStore = store;
    }

    public MetadataStore getMetadataStore() {
        return metadataStore;
    }

    public Object getMetadataStoreRoot() {
        FormatTools.assertId(currentId, true, 1);
        return getMetadataStore().getRoot();
    }

    public void setId(String id, boolean force) throws FormatException, IOException {
        if (!id.equals(currentId) || force) initFile(id);
    }

    public void close() throws IOException {
        if (in != null) in.close();
        in = null;
        currentId = null;
    }

    /** @deprecated Replaced by {@link #getImageCount()} */
    public int getImageCount(String id) throws FormatException, IOException {
        setId(id);
        return getImageCount();
    }

    /** @deprecated Replaced by {@link #isRGB()} */
    public boolean isRGB(String id) throws FormatException, IOException {
        return getRGBChannelCount(id) > 1;
    }

    /** @deprecated Replaced by {@link #getSizeX()} */
    public int getSizeX(String id) throws FormatException, IOException {
        setId(id);
        return core.sizeX[series];
    }

    /** @deprecated Replaced by {@link #getSizeY()} */
    public int getSizeY(String id) throws FormatException, IOException {
        setId(id);
        return core.sizeY[series];
    }

    /** @deprecated Replaced by {@link #getSizeZ()} */
    public int getSizeZ(String id) throws FormatException, IOException {
        setId(id);
        return core.sizeZ[series];
    }

    /** @deprecated Replaced by {@link #getSizeC()} */
    public int getSizeC(String id) throws FormatException, IOException {
        setId(id);
        return core.sizeC[series];
    }

    /** @deprecated Replaced by {@link #getSizeT()} */
    public int getSizeT(String id) throws FormatException, IOException {
        setId(id);
        return core.sizeT[series];
    }

    /** @deprecated Replaced by {@link #getPixelType()} */
    public int getPixelType(String id) throws FormatException, IOException {
        setId(id);
        return core.pixelType[series];
    }

    /** @deprecated Replaced by {@link #getEffectiveSizeC()} */
    public int getEffectiveSizeC(String id) throws FormatException, IOException {
        return getImageCount(id) / (getSizeZ(id) * getSizeT(id));
    }

    /** @deprecated Replaced by {@link #getRGBChannelCount()} */
    public int getRGBChannelCount(String id) throws FormatException, IOException {
        return getSizeC(id) / getEffectiveSizeC(id);
    }

    /** @deprecated Replaced by {@link #getChannelDimLengths()} */
    public int[] getChannelDimLengths(String id) throws FormatException, IOException {
        setId(id);
        if (core.cLengths[series] == null) {
            core.cLengths[series] = new int[] { core.sizeC[series] };
        }
        return core.cLengths[series];
    }

    /** @deprecated Replaced by {@link #getChannelDimTypes()} */
    public String[] getChannelDimTypes(String id) throws FormatException, IOException {
        setId(id);
        if (core.cTypes[series] == null) {
            core.cTypes[series] = new String[] { FormatTools.CHANNEL };
        }
        return core.cTypes[series];
    }

    /** @deprecated Replaced by {@link #getThumbSizeX()} */
    public int getThumbSizeX(String id) throws FormatException, IOException {
        int sx = getSizeX(id);
        int sy = getSizeY(id);
        return sx > sy ? THUMBNAIL_DIMENSION : sx * THUMBNAIL_DIMENSION / sy;
    }

    /** @deprecated Replaced by {@link #getThumbSizeY()} */
    public int getThumbSizeY(String id) throws FormatException, IOException {
        int sx = getSizeX(id);
        int sy = getSizeY(id);
        return sy > sx ? THUMBNAIL_DIMENSION : sy * THUMBNAIL_DIMENSION / sx;
    }

    /** @deprecated Replaced by {@link #isLittleEndian()} */
    public boolean isLittleEndian(String id) throws FormatException, IOException {
        setId(id);
        return isLittleEndian();
    }

    /** @deprecated Replaced by {@link #getDimensionOrder()} */
    public String getDimensionOrder(String id) throws FormatException, IOException {
        setId(id);
        return core.currentOrder[series];
    }

    /** @deprecated Replaced by {@link #isOrderCertain()} */
    public boolean isOrderCertain(String id) throws FormatException, IOException {
        setId(id);
        return core.orderCertain[series];
    }

    /** @deprecated Replaced by {@link #isInterleaved()} */
    public boolean isInterleaved(String id) throws FormatException, IOException {
        return isInterleaved(id, 0);
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
        return openBytes(id, no);
    }

    /** @deprecated Replaced by {@link #openThumbImage(int)} */
    public BufferedImage openThumbImage(String id, int no) throws FormatException, IOException {
        return ImageTools.scale(openImage(id, no), getThumbSizeX(id), getThumbSizeY(id), false);
    }

    /** @deprecated Replaced by {@link #openThumbBytes(int)} */
    public byte[] openThumbBytes(String id, int no) throws FormatException, IOException {
        BufferedImage img = openThumbImage(id, no);
        byte[][] bytes = ImageTools.getBytes(img);
        if (bytes.length == 1) return bytes[0];
        byte[] rtn = new byte[getRGBChannelCount(id) * bytes[0].length];
        for (int i = 0; i < getRGBChannelCount(id); i++) {
            System.arraycopy(bytes[i], 0, rtn, bytes[0].length * i, bytes[i].length);
        }
        return rtn;
    }

    /** @deprecated Replaced by {@link #getSeriesCount()} */
    public int getSeriesCount(String id) throws FormatException, IOException {
        setId(id);
        return 1;
    }

    /** @deprecated Replaced by {@link #setSeries(int)} */
    public void setSeries(String id, int no) throws FormatException, IOException {
        if (no < 0 || no >= getSeriesCount(id)) {
            throw new FormatException("Invalid series: " + no);
        }
        series = no;
    }

    /** @deprecated Replaced by {@link #getSeries()} */
    public int getSeries(String id) throws FormatException, IOException {
        setId(id);
        return series;
    }

    /** @deprecated Replaced by {@link #getUsedFiles()} */
    public String[] getUsedFiles(String id) throws FormatException, IOException {
        setId(id);
        return new String[] { id };
    }

    /** @deprecated Replaced by {@link #getIndex(int, int, int)} */
    public int getIndex(String id, int z, int c, int t) throws FormatException, IOException {
        setId(id);
        return FormatTools.getIndex(this, z, c, t);
    }

    /** @deprecated Replaced by {@link #getZCTCoords(int)} */
    public int[] getZCTCoords(String id, int index) throws FormatException, IOException {
        setId(id);
        return FormatTools.getZCTCoords(this, index);
    }

    /** @deprecated Replaced by {@link #getMetadataValue(String)} */
    public Object getMetadataValue(String id, String field) throws FormatException, IOException {
        setId(id);
        return getMeta(field);
    }

    /** @deprecated Replaced by {@link #getMetadata()} */
    public Hashtable getMetadata(String id) throws FormatException, IOException {
        setId(id);
        return metadata;
    }

    /** @deprecated Replaced by {@link #getCoreMetadata()} */
    public CoreMetadata getCoreMetadata(String id) throws FormatException, IOException {
        setId(id);
        return core;
    }

    /** @deprecated Replaced by {@link #getMetadataStore()} */
    public MetadataStore getMetadataStore(String id) throws FormatException, IOException {
        setId(id);
        return metadataStore;
    }

    /** @deprecated Replaced by {@link #getMetadataStoreRoot()} */
    public Object getMetadataStoreRoot(String id) throws FormatException, IOException {
        setId(id);
        return getMetadataStore().getRoot();
    }
}

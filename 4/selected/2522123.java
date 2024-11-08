package gov.nasa.worldwind.formats.tiff;

import gov.nasa.worldwind.Version;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.data.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.*;
import java.awt.color.*;
import java.awt.image.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * @author Lado Garakanidze
 * @version $Id: GeotiffWriter.java 1 2011-07-16 23:22:47Z dcollins $
 */
public class GeotiffWriter {

    private RandomAccessFile targetFile;

    private FileChannel theChannel;

    private static final int INTEGER_SIZEOF = Integer.SIZE / Byte.SIZE;

    private static final int BufferedImage_TYPE_ELEVATION_SHORT16 = 9001;

    private static final int BufferedImage_TYPE_ELEVATION_FLOAT32 = 9002;

    public GeotiffWriter(String filename) throws IOException {
        if (null == filename || 0 == filename.trim().length()) {
            String msg = Logging.getMessage("generic.FileNameIsMissing");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        commonInitializer(new File(filename));
    }

    public GeotiffWriter(File file) throws IOException {
        if (null == file) {
            String msg = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        commonInitializer(file);
    }

    private void commonInitializer(File file) throws IOException {
        if (!file.getParentFile().canWrite()) {
            String msg = Logging.getMessage("generic.FolderNoWritePermission", file.getParentFile().getAbsolutePath());
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        this.targetFile = new RandomAccessFile(file, "rw");
        this.theChannel = this.targetFile.getChannel();
    }

    public void close() {
        try {
            this.targetFile.close();
        } catch (Exception ex) {
        }
    }

    public void write(BufferedImage image) throws IOException {
        this.write(image, null);
    }

    public void write(DataRaster raster) throws IOException, IllegalArgumentException {
        if (null == raster) {
            String msg = Logging.getMessage("nullValue.RasterIsNull");
            Logging.logger().finest(msg);
            throw new IllegalArgumentException(msg);
        }
        if (!(raster.getWidth() > 0)) {
            String msg = Logging.getMessage("generic.InvalidWidth", raster.getWidth());
            Logging.logger().finest(msg);
            throw new IllegalArgumentException(msg);
        }
        if (!(raster.getHeight() > 0)) {
            String msg = Logging.getMessage("generic.InvalidHeight", raster.getHeight());
            Logging.logger().finest(msg);
            throw new IllegalArgumentException(msg);
        }
        if (raster instanceof BufferedImageRaster) {
            this.write(((BufferedImageRaster) raster).getBufferedImage(), raster);
        } else if (raster instanceof BufferWrapperRaster) {
            this.writeRaster((BufferWrapperRaster) raster);
        }
    }

    public void write(BufferedImage image, AVList params) throws IOException {
        if (image == null) {
            String msg = Logging.getMessage("nullValue.ImageSource");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (0 == image.getWidth() || 0 == image.getHeight()) {
            String msg = Logging.getMessage("generic.InvalidImageSize", image.getWidth(), image.getHeight());
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (null == params || 0 == params.getValues().size()) {
            String reason = Logging.getMessage("nullValue.AVListIsNull");
            Logging.logger().finest(Logging.getMessage("GeotiffWriter.GeoKeysMissing", reason));
            params = new AVListImpl();
        } else {
            this.validateParameters(params, image.getWidth(), image.getHeight());
        }
        int type = image.getType();
        if (BufferedImage.TYPE_CUSTOM == type) {
            int numColorComponents = 0, numComponents = 0, pixelSize = 0, dataType = 0, csType = 0;
            boolean hasAlpha = false;
            if (null != image.getColorModel()) {
                ColorModel cm = image.getColorModel();
                numColorComponents = cm.getNumColorComponents();
                numComponents = cm.getNumComponents();
                pixelSize = cm.getPixelSize();
                hasAlpha = cm.hasAlpha();
                ColorSpace cs = cm.getColorSpace();
                if (null != cs) csType = cs.getType();
            }
            if (null != image.getSampleModel()) {
                SampleModel sm = image.getSampleModel();
                dataType = sm.getDataType();
            }
            if (dataType == DataBuffer.TYPE_FLOAT && pixelSize == Float.SIZE && numComponents == 1) {
                type = BufferedImage_TYPE_ELEVATION_FLOAT32;
            } else if (dataType == DataBuffer.TYPE_SHORT && pixelSize == Short.SIZE && numComponents == 1) {
                type = BufferedImage_TYPE_ELEVATION_SHORT16;
            } else if (ColorSpace.CS_GRAY == csType && pixelSize == Byte.SIZE) {
                type = BufferedImage.TYPE_BYTE_GRAY;
            } else if (dataType == DataBuffer.TYPE_USHORT && ColorSpace.CS_GRAY == csType && pixelSize == Short.SIZE) {
                type = BufferedImage.TYPE_USHORT_GRAY;
            } else if (ColorSpace.TYPE_RGB == csType && pixelSize == 3 * Byte.SIZE && numColorComponents == 3) {
                type = BufferedImage.TYPE_3BYTE_BGR;
            } else if (ColorSpace.TYPE_RGB == csType && hasAlpha && pixelSize == 4 * Byte.SIZE && numComponents == 4) {
                type = BufferedImage.TYPE_4BYTE_ABGR;
            }
        }
        switch(type) {
            case BufferedImage.TYPE_3BYTE_BGR:
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_BGR:
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
                {
                    this.writeColorImage(image, params);
                }
                break;
            case BufferedImage.TYPE_USHORT_GRAY:
            case BufferedImage.TYPE_BYTE_GRAY:
                {
                    this.writeGrayscaleImage(image, params);
                }
                break;
            case BufferedImage_TYPE_ELEVATION_SHORT16:
            case BufferedImage_TYPE_ELEVATION_FLOAT32:
                {
                    String msg = Logging.getMessage("GeotiffWriter.FeatureNotImplementedd", type);
                    Logging.logger().severe(msg);
                    throw new IllegalArgumentException(msg);
                }
            case BufferedImage.TYPE_CUSTOM:
            default:
                {
                    ColorModel cm = image.getColorModel();
                    SampleModel sm = image.getSampleModel();
                    StringBuffer sb = new StringBuffer(Logging.getMessage("GeotiffWriter.UnsupportedType", type));
                    sb.append("\n");
                    sb.append("NumBands=").append(sm.getNumBands()).append("\n");
                    sb.append("NumDataElements=").append(sm.getNumDataElements()).append("\n");
                    sb.append("NumColorComponents=").append(cm.getNumColorComponents()).append("\n");
                    sb.append("NumComponents=").append(cm.getNumComponents()).append("\n");
                    sb.append("PixelSize=").append(cm.getPixelSize()).append("\n");
                    sb.append("hasAlpha=").append(cm.hasAlpha());
                    String msg = sb.toString();
                    Logging.logger().severe(msg);
                    throw new IllegalArgumentException(msg);
                }
        }
    }

    private void writeColorImage(BufferedImage image, AVList params) throws IOException {
        int numBands = image.getRaster().getNumBands();
        long offset;
        this.writeTiffHeader();
        int numRows = image.getHeight();
        int numCols = image.getWidth();
        int[] stripCounts = new int[numRows];
        int[] stripOffsets = new int[numRows];
        ByteBuffer dataBuff = ByteBuffer.allocateDirect(numCols * numBands);
        Raster rast = image.getRaster();
        for (int i = 0; i < numRows; i++) {
            stripOffsets[i] = (int) this.theChannel.position();
            stripCounts[i] = numCols * numBands;
            int[] rowData = rast.getPixels(0, i, image.getWidth(), 1, (int[]) null);
            dataBuff.clear();
            for (int j = 0; j < numCols * numBands; j++) {
                putUnsignedByte(dataBuff, rowData[j]);
            }
            dataBuff.flip();
            this.theChannel.write(dataBuff);
        }
        ArrayList<TiffIFDEntry> ifds = new ArrayList<TiffIFDEntry>(10);
        ifds.add(new TiffIFDEntry(Tiff.Tag.IMAGE_WIDTH, Tiff.Type.LONG, 1, numCols));
        ifds.add(new TiffIFDEntry(Tiff.Tag.IMAGE_LENGTH, Tiff.Type.LONG, 1, numRows));
        ifds.add(new TiffIFDEntry(Tiff.Tag.PLANAR_CONFIGURATION, Tiff.Type.SHORT, 1, Tiff.PlanarConfiguration.CHUNKY));
        ifds.add(new TiffIFDEntry(Tiff.Tag.SAMPLES_PER_PIXEL, Tiff.Type.SHORT, 1, numBands));
        ifds.add(new TiffIFDEntry(Tiff.Tag.COMPRESSION, Tiff.Type.LONG, 1, Tiff.Compression.NONE));
        ifds.add(new TiffIFDEntry(Tiff.Tag.PHOTO_INTERPRETATION, Tiff.Type.SHORT, 1, Tiff.Photometric.Color_RGB));
        ifds.add(new TiffIFDEntry(Tiff.Tag.ORIENTATION, Tiff.Type.SHORT, 1, Tiff.Orientation.DEFAULT));
        offset = this.theChannel.position();
        short[] bps = new short[numBands];
        for (int i = 0; i < numBands; i++) {
            bps[i] = Tiff.BitsPerSample.MONOCHROME_BYTE;
        }
        this.theChannel.write(ByteBuffer.wrap(this.getBytes(bps)));
        ifds.add(new TiffIFDEntry(Tiff.Tag.BITS_PER_SAMPLE, Tiff.Type.SHORT, numBands, offset));
        offset = this.theChannel.position();
        dataBuff = ByteBuffer.allocateDirect(stripOffsets.length * INTEGER_SIZEOF);
        for (int stripOffset : stripOffsets) {
            dataBuff.putInt(stripOffset);
        }
        dataBuff.flip();
        this.theChannel.write(dataBuff);
        ifds.add(new TiffIFDEntry(Tiff.Tag.STRIP_OFFSETS, Tiff.Type.LONG, stripOffsets.length, offset));
        ifds.add(new TiffIFDEntry(Tiff.Tag.ROWS_PER_STRIP, Tiff.Type.LONG, 1, 1));
        offset = this.theChannel.position();
        dataBuff.clear();
        for (int stripCount : stripCounts) {
            dataBuff.putInt(stripCount);
        }
        dataBuff.flip();
        this.theChannel.write(dataBuff);
        ifds.add(new TiffIFDEntry(Tiff.Tag.STRIP_BYTE_COUNTS, Tiff.Type.LONG, stripCounts.length, offset));
        this.appendGeoTiff(ifds, params);
        this.writeIFDs(ifds);
    }

    private void writeGrayscaleImage(BufferedImage image, AVList params) throws IOException {
        int type = image.getType();
        int bitsPerSample = (BufferedImage.TYPE_USHORT_GRAY == type) ? Tiff.BitsPerSample.MONOCHROME_UINT16 : Tiff.BitsPerSample.MONOCHROME_UINT8;
        int numBands = image.getSampleModel().getNumBands();
        int bytesPerSample = numBands * bitsPerSample / Byte.SIZE;
        this.writeTiffHeader();
        int numRows = image.getHeight();
        int numCols = image.getWidth();
        int[] stripCounts = new int[numRows];
        int[] stripOffsets = new int[numRows];
        ByteBuffer dataBuff = ByteBuffer.allocateDirect(numCols * bytesPerSample);
        Raster rast = image.getRaster();
        for (int i = 0; i < numRows; i++) {
            stripOffsets[i] = (int) this.theChannel.position();
            stripCounts[i] = numCols * bytesPerSample;
            int[] rowData = rast.getPixels(0, i, image.getWidth(), 1, (int[]) null);
            dataBuff.clear();
            if (BufferedImage.TYPE_USHORT_GRAY == type) {
                for (int j = 0; j < numCols * numBands; j++) {
                    this.putUnsignedShort(dataBuff, rowData[j]);
                }
            } else if (BufferedImage.TYPE_BYTE_GRAY == type) {
                for (int j = 0; j < numCols * numBands; j++) {
                    this.putUnsignedByte(dataBuff, rowData[j]);
                }
            }
            dataBuff.flip();
            this.theChannel.write(dataBuff);
        }
        ArrayList<TiffIFDEntry> ifds = new ArrayList<TiffIFDEntry>(10);
        ifds.add(new TiffIFDEntry(Tiff.Tag.IMAGE_WIDTH, Tiff.Type.LONG, 1, numCols));
        ifds.add(new TiffIFDEntry(Tiff.Tag.IMAGE_LENGTH, Tiff.Type.LONG, 1, numRows));
        ifds.add(new TiffIFDEntry(Tiff.Tag.BITS_PER_SAMPLE, Tiff.Type.SHORT, 1, bitsPerSample));
        ifds.add(new TiffIFDEntry(Tiff.Tag.COMPRESSION, Tiff.Type.LONG, 1, Tiff.Compression.NONE));
        ifds.add(new TiffIFDEntry(Tiff.Tag.PHOTO_INTERPRETATION, Tiff.Type.SHORT, 1, Tiff.Photometric.Grayscale_BlackIsZero));
        ifds.add(new TiffIFDEntry(Tiff.Tag.SAMPLE_FORMAT, Tiff.Type.SHORT, 1, Tiff.SampleFormat.UNSIGNED));
        long offset = this.theChannel.position();
        dataBuff = ByteBuffer.allocateDirect(stripOffsets.length * INTEGER_SIZEOF);
        for (int stripOffset : stripOffsets) {
            dataBuff.putInt(stripOffset);
        }
        dataBuff.flip();
        this.theChannel.write(dataBuff);
        ifds.add(new TiffIFDEntry(Tiff.Tag.STRIP_OFFSETS, Tiff.Type.LONG, stripOffsets.length, offset));
        ifds.add(new TiffIFDEntry(Tiff.Tag.SAMPLES_PER_PIXEL, Tiff.Type.SHORT, 1, numBands));
        ifds.add(new TiffIFDEntry(Tiff.Tag.ROWS_PER_STRIP, Tiff.Type.LONG, 1, 1));
        offset = this.theChannel.position();
        dataBuff.clear();
        for (int stripCount : stripCounts) {
            dataBuff.putInt(stripCount);
        }
        dataBuff.flip();
        this.theChannel.write(dataBuff);
        ifds.add(new TiffIFDEntry(Tiff.Tag.STRIP_BYTE_COUNTS, Tiff.Type.LONG, stripCounts.length, offset));
        this.appendGeoTiff(ifds, params);
        this.writeIFDs(ifds);
    }

    private void writeTiffHeader() throws IOException {
        byte[] tiffHeader = new byte[] { 0x4D, 0x4D, 0, 42, 0, 0, 0, 0 };
        this.theChannel.write(ByteBuffer.wrap(tiffHeader));
    }

    private void appendGeoTiff(ArrayList<TiffIFDEntry> ifds, AVList params) throws IOException, IllegalArgumentException {
        if (null == params || 0 == params.getEntries().size()) {
            String reason = Logging.getMessage("nullValue.AVListIsNull");
            Logging.logger().finest(Logging.getMessage("GeotiffWriter.GeoKeysMissing", reason));
            return;
        }
        long offset = this.theChannel.position();
        if (params.hasKey(AVKey.DISPLAY_NAME)) {
            String value = params.getStringValue(AVKey.DISPLAY_NAME);
            if (null != value && 0 < value.trim().length()) {
                offset = this.theChannel.position();
                byte[] bytes = value.trim().getBytes();
                this.theChannel.write(ByteBuffer.wrap(bytes));
                ifds.add(new TiffIFDEntry(Tiff.Tag.DOCUMENT_NAME, Tiff.Type.ASCII, bytes.length, offset));
            }
        }
        if (params.hasKey(AVKey.DESCRIPTION)) {
            String value = params.getStringValue(AVKey.DESCRIPTION);
            if (null != value && 0 < value.trim().length()) {
                offset = this.theChannel.position();
                byte[] bytes = value.trim().getBytes();
                this.theChannel.write(ByteBuffer.wrap(bytes));
                ifds.add(new TiffIFDEntry(Tiff.Tag.IMAGE_DESCRIPTION, Tiff.Type.ASCII, bytes.length, offset));
            }
        }
        if (params.hasKey(AVKey.VERSION)) {
            String value = params.getStringValue(AVKey.VERSION);
            if (null != value && 0 < value.trim().length()) {
                offset = this.theChannel.position();
                byte[] bytes = value.trim().getBytes();
                this.theChannel.write(ByteBuffer.wrap(bytes));
                ifds.add(new TiffIFDEntry(Tiff.Tag.SOFTWARE_VERSION, Tiff.Type.ASCII, bytes.length, offset));
            }
        }
        if (params.hasKey(AVKey.DATE_TIME)) {
            String value = params.getStringValue(AVKey.DATE_TIME);
            if (null != value && 0 < value.trim().length()) {
                offset = this.theChannel.position();
                byte[] bytes = value.getBytes();
                this.theChannel.write(ByteBuffer.wrap(bytes));
                ifds.add(new TiffIFDEntry(Tiff.Tag.DATE_TIME, Tiff.Type.ASCII, bytes.length, offset));
            }
        }
        if (params.hasKey(AVKey.SECTOR)) {
            if (params.hasKey(AVKey.PIXEL_WIDTH) && params.hasKey(AVKey.PIXEL_HEIGHT)) {
                offset = this.theChannel.position();
                double[] values = new double[] { (Double) params.getValue(AVKey.PIXEL_WIDTH), (Double) params.getValue(AVKey.PIXEL_HEIGHT), isElevation(params) ? 1d : 0d };
                byte[] bytes = this.getBytes(values);
                this.theChannel.write(ByteBuffer.wrap(bytes));
                ifds.add(new TiffIFDEntry(GeoTiff.Tag.MODEL_PIXELSCALE, Tiff.Type.DOUBLE, values.length, offset));
            }
            if (params.hasKey(AVKey.WIDTH) && params.hasKey(AVKey.HEIGHT)) {
                offset = this.theChannel.position();
                double w = (Integer) params.getValue(AVKey.WIDTH);
                double h = (Integer) params.getValue(AVKey.HEIGHT);
                Sector sec = (Sector) params.getValue(AVKey.SECTOR);
                double[] values = new double[] { 0d, 0d, 0d, sec.getMinLongitude().degrees, sec.getMaxLatitude().degrees, 0d, w - 1, 0d, 0d, sec.getMaxLongitude().degrees, sec.getMaxLatitude().degrees, 0d, w - 1, h - 1, 0d, sec.getMaxLongitude().degrees, sec.getMinLatitude().degrees, 0d, 0d, h - 1, 0d, sec.getMinLongitude().degrees, sec.getMinLatitude().degrees, 0d };
                byte[] bytes = this.getBytes(values);
                this.theChannel.write(ByteBuffer.wrap(bytes));
                ifds.add(new TiffIFDEntry(GeoTiff.Tag.MODEL_TIEPOINT, Tiff.Type.DOUBLE, values.length, offset));
            }
            if (params.hasKey(AVKey.MISSING_DATA_SIGNAL) || params.hasKey(AVKey.MISSING_DATA_REPLACEMENT)) {
                offset = this.theChannel.position();
                Object nodata = params.hasKey(AVKey.MISSING_DATA_SIGNAL) ? params.getValue(AVKey.MISSING_DATA_SIGNAL) : params.getValue(AVKey.MISSING_DATA_REPLACEMENT);
                String value = "" + nodata + "\0";
                byte[] bytes = value.getBytes();
                this.theChannel.write(ByteBuffer.wrap(bytes));
                ifds.add(new TiffIFDEntry(GeoTiff.Tag.GDAL_NODATA, Tiff.Type.ASCII, bytes.length, offset));
            }
            if (params.hasKey(AVKey.COORDINATE_SYSTEM)) {
                String cs = params.getStringValue(AVKey.COORDINATE_SYSTEM);
                if (AVKey.COORDINATE_SYSTEM_GEOGRAPHIC.equals(cs)) {
                    if (isElevation(params)) this.writeGeographicElevationGeoKeys(ifds, params); else this.writeGeographicImageGeoKeys(ifds, params);
                } else if (AVKey.COORDINATE_SYSTEM_PROJECTED.equals(cs)) {
                    String msg = Logging.getMessage("GeotiffWriter.FeatureNotImplementedd", cs);
                    Logging.logger().severe(msg);
                    throw new IllegalArgumentException(msg);
                } else {
                    String msg = Logging.getMessage("GeotiffWriter.UnknownCoordinateSystem", cs);
                    Logging.logger().severe(msg);
                    throw new IllegalArgumentException(msg);
                }
            }
        }
    }

    protected void validateParameters(AVList list, int srcWidth, int srcHeight) throws IllegalArgumentException {
        if (null == list || 0 == list.getValues().size()) {
            String reason = Logging.getMessage("nullValue.AVListIsNull");
            String msg = Logging.getMessage("GeotiffWriter.GeoKeysMissing", reason);
            Logging.logger().finest(msg);
            throw new IllegalArgumentException(msg);
        }
        if (!(srcWidth > 0 && srcHeight > 0)) {
            String msg = Logging.getMessage("generic.InvalidImageSize", srcWidth, srcHeight);
            Logging.logger().finest(msg);
            throw new IllegalArgumentException(msg);
        }
        if (list.hasKey(AVKey.WIDTH)) {
            int width = (Integer) list.getValue(AVKey.WIDTH);
            if (width != srcWidth) {
                String msg = Logging.getMessage("GeotiffWriter.ImageWidthMismatch", width, srcWidth);
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }
        } else list.setValue(AVKey.WIDTH, srcWidth);
        if (list.hasKey(AVKey.HEIGHT)) {
            int height = (Integer) list.getValue(AVKey.HEIGHT);
            if (height != srcHeight) {
                String msg = Logging.getMessage("GeotiffWriter.ImageHeightMismatch", height, srcHeight);
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }
        } else list.setValue(AVKey.HEIGHT, srcHeight);
        Sector sector = null;
        if (list.hasKey(AVKey.SECTOR)) sector = (Sector) list.getValue(AVKey.SECTOR);
        if (null == sector) {
            String msg = Logging.getMessage("GeotiffWriter.NoSectorSpecified");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (!list.hasKey(AVKey.COORDINATE_SYSTEM)) {
            String msg = Logging.getMessage("GeotiffWriter.GeoKeysMissing", AVKey.COORDINATE_SYSTEM);
            Logging.logger().finest(msg);
            list.setValue(AVKey.COORDINATE_SYSTEM, AVKey.COORDINATE_SYSTEM_GEOGRAPHIC);
        }
        if (!list.hasKey(AVKey.PROJECTION_EPSG_CODE)) {
            if (isGeographic(list)) {
                list.setValue(AVKey.PROJECTION_EPSG_CODE, GeoTiff.GCS.WGS_84);
            } else {
                String msg = Logging.getMessage("GeotiffWriter.GeoKeysMissing", AVKey.PROJECTION_EPSG_CODE);
                Logging.logger().finest(msg);
                throw new IllegalArgumentException(msg);
            }
        }
        if (!list.hasKey(AVKey.PIXEL_WIDTH)) {
            if (isGeographic(list)) {
                double pixelWidth = sector.getDeltaLonDegrees() / (double) srcWidth;
                list.setValue(AVKey.PIXEL_WIDTH, pixelWidth);
            } else {
                String msg = Logging.getMessage("GeotiffWriter.GeoKeysMissing", AVKey.PIXEL_WIDTH);
                Logging.logger().finest(msg);
                throw new IllegalArgumentException(msg);
            }
        }
        if (!list.hasKey(AVKey.PIXEL_HEIGHT)) {
            if (isGeographic(list)) {
                double pixelHeight = sector.getDeltaLatDegrees() / (double) srcHeight;
                list.setValue(AVKey.PIXEL_HEIGHT, pixelHeight);
            } else {
                String msg = Logging.getMessage("GeotiffWriter.GeoKeysMissing", AVKey.PIXEL_HEIGHT);
                Logging.logger().finest(msg);
                throw new IllegalArgumentException(msg);
            }
        }
        if (!list.hasKey(AVKey.PIXEL_FORMAT)) {
            String msg = Logging.getMessage("GeotiffWriter.GeoKeysMissing", AVKey.PIXEL_FORMAT);
            Logging.logger().finest(msg);
            throw new IllegalArgumentException(msg);
        } else {
            String pixelFormat = list.getStringValue(AVKey.PIXEL_FORMAT);
            if (!AVKey.ELEVATION.equals(pixelFormat) && !AVKey.IMAGE.equals(pixelFormat)) {
                String msg = Logging.getMessage("Geotiff.UnknownGeoKeyValue", pixelFormat, AVKey.PIXEL_FORMAT);
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }
        }
        if (AVKey.ELEVATION.equals(list.getValue(AVKey.PIXEL_FORMAT))) {
            if (!list.hasKey(AVKey.DATA_TYPE)) {
                String msg = Logging.getMessage("GeotiffWriter.GeoKeysMissing", AVKey.DATA_TYPE);
                Logging.logger().finest(msg);
                throw new IllegalArgumentException(msg);
            }
            String type = list.getStringValue(AVKey.DATA_TYPE);
            if (!AVKey.FLOAT32.equals(type) && !AVKey.INT16.equals(type)) {
                String msg = Logging.getMessage("Geotiff.UnknownGeoKeyValue", type, AVKey.DATA_TYPE);
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }
        }
        if (!list.hasKey(AVKey.ORIGIN)) {
            LatLon origin = new LatLon(sector.getMaxLatitude(), sector.getMinLongitude());
            list.setValue(AVKey.ORIGIN, origin);
        }
        if (list.hasKey(AVKey.BYTE_ORDER) && !AVKey.BIG_ENDIAN.equals(list.getStringValue(AVKey.BYTE_ORDER))) {
            String msg = Logging.getMessage("generic.UnrecognizedByteOrder", list.getStringValue(AVKey.BYTE_ORDER));
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (!list.hasKey(AVKey.DATE_TIME)) {
            String timestamp = String.format("%1$tY:%1$tm:%1$td %tT\0", Calendar.getInstance());
            list.setValue(AVKey.DATE_TIME, timestamp);
        }
        if (!list.hasKey(AVKey.VERSION)) {
            list.setValue(AVKey.VERSION, Version.getVersion());
        }
    }

    private static boolean isElevation(AVList params) {
        return (null != params && params.hasKey(AVKey.PIXEL_FORMAT) && AVKey.ELEVATION.equals(params.getValue(AVKey.PIXEL_FORMAT)));
    }

    private static boolean isImage(AVList params) {
        return (null != params && params.hasKey(AVKey.PIXEL_FORMAT) && AVKey.IMAGE.equals(params.getValue(AVKey.PIXEL_FORMAT)));
    }

    private static boolean isGeographic(AVList params) {
        return (null != params && params.hasKey(AVKey.COORDINATE_SYSTEM) && AVKey.COORDINATE_SYSTEM_GEOGRAPHIC.equals(params.getValue(AVKey.COORDINATE_SYSTEM)));
    }

    private static boolean isProjected(AVList params) {
        return (null != params && params.hasKey(AVKey.COORDINATE_SYSTEM) && AVKey.COORDINATE_SYSTEM_PROJECTED.equals(params.getValue(AVKey.COORDINATE_SYSTEM)));
    }

    private void writeGeographicImageGeoKeys(ArrayList<TiffIFDEntry> ifds, AVList params) throws IOException {
        long offset = this.theChannel.position();
        if (isImage(params) && isGeographic(params)) {
            int epsg = GeoTiff.GCS.WGS_84;
            if (params.hasKey(AVKey.PROJECTION_EPSG_CODE)) epsg = (Integer) params.getValue(AVKey.PROJECTION_EPSG_CODE);
            short[] values = new short[] { GeoTiff.GeoKeyHeader.KeyDirectoryVersion, GeoTiff.GeoKeyHeader.KeyRevision, GeoTiff.GeoKeyHeader.MinorRevision, 0, GeoTiff.GeoKey.ModelType, 0, 1, GeoTiff.ModelType.Geographic, GeoTiff.GeoKey.RasterType, 0, 1, (short) (0xFFFF & GeoTiff.RasterType.RasterPixelIsArea), GeoTiff.GeoKey.GeographicType, 0, 1, (short) (0xFFFF & epsg), GeoTiff.GeoKey.GeogAngularUnits, 0, 1, GeoTiff.Unit.Angular.Angular_Degree };
            values[3] = (short) (values.length / 4);
            byte[] bytes = this.getBytes(values);
            this.theChannel.write(ByteBuffer.wrap(bytes));
            ifds.add(new TiffIFDEntry(GeoTiff.Tag.GEO_KEY_DIRECTORY, Tiff.Type.SHORT, values.length, offset));
        }
    }

    private void writeGeographicElevationGeoKeys(ArrayList<TiffIFDEntry> ifds, AVList params) throws IOException {
        long offset = this.theChannel.position();
        if (isElevation(params) && isGeographic(params)) {
            int epsg = GeoTiff.GCS.WGS_84;
            if (params.hasKey(AVKey.PROJECTION_EPSG_CODE)) epsg = (Integer) params.getValue(AVKey.PROJECTION_EPSG_CODE);
            int elevUnits = GeoTiff.Unit.Linear.Meter;
            if (params.hasKey(AVKey.ELEVATION_UNIT)) {
                if (AVKey.UNIT_FOOT.equals(params.getValue(AVKey.ELEVATION_UNIT))) elevUnits = GeoTiff.Unit.Linear.Foot;
            }
            int rasterType = GeoTiff.RasterType.RasterPixelIsArea;
            if (params.hasKey(AVKey.RASTER_PIXEL) && AVKey.RASTER_PIXEL_IS_POINT.equals(params.getValue(AVKey.RASTER_PIXEL))) rasterType = GeoTiff.RasterType.RasterPixelIsPoint;
            short[] values = new short[] { GeoTiff.GeoKeyHeader.KeyDirectoryVersion, GeoTiff.GeoKeyHeader.KeyRevision, GeoTiff.GeoKeyHeader.MinorRevision, 0, GeoTiff.GeoKey.ModelType, 0, 1, GeoTiff.ModelType.Geographic, GeoTiff.GeoKey.RasterType, 0, 1, (short) (0xFFFF & rasterType), GeoTiff.GeoKey.GeographicType, 0, 1, (short) (0xFFFF & epsg), GeoTiff.GeoKey.GeogAngularUnits, 0, 1, GeoTiff.Unit.Angular.Angular_Degree, GeoTiff.GeoKey.VerticalCSType, 0, 1, GeoTiff.VCS.WGS_84_ellipsoid, GeoTiff.GeoKey.VerticalUnits, 0, 1, (short) (0xFFFF & elevUnits) };
            values[3] = (short) (values.length / 4);
            byte[] bytes = this.getBytes(values);
            this.theChannel.write(ByteBuffer.wrap(bytes));
            ifds.add(new TiffIFDEntry(GeoTiff.Tag.GEO_KEY_DIRECTORY, Tiff.Type.SHORT, values.length, offset));
        }
    }

    private void writeIFDs(List<TiffIFDEntry> ifds) throws IOException {
        long offset = this.theChannel.position();
        long adjust = offset % 4L;
        offset += (adjust == 0) ? 0 : (4L - adjust);
        this.theChannel.position(offset);
        Collections.sort(ifds);
        ByteBuffer dataBuff = ByteBuffer.allocateDirect(ifds.size() * 12);
        putUnsignedShort(dataBuff, ifds.size());
        dataBuff.flip();
        this.theChannel.write(dataBuff);
        dataBuff.clear();
        for (TiffIFDEntry ifd : ifds) {
            putUnsignedShort(dataBuff, ifd.tag);
            putUnsignedShort(dataBuff, ifd.type);
            putUnsignedInt(dataBuff, ifd.count);
            if (ifd.type == Tiff.Type.SHORT && ifd.count == 1) {
                putUnsignedShort(dataBuff, (int) ifd.valOffset);
                dataBuff.putShort((short) 0);
            } else putUnsignedInt(dataBuff, ifd.valOffset);
        }
        dataBuff.flip();
        this.theChannel.write(dataBuff);
        dataBuff.clear();
        dataBuff.putInt(0);
        dataBuff.flip();
        this.theChannel.write(dataBuff);
        this.theChannel.position(4);
        dataBuff.clear();
        putUnsignedInt(dataBuff, offset);
        dataBuff.flip();
        this.theChannel.write(dataBuff);
    }

    private void putUnsignedByte(ByteBuffer buff, int value) {
        buff.put((byte) (value & 0xff));
    }

    private void putUnsignedShort(ByteBuffer buff, int value) {
        buff.putShort((short) (value & 0xffff));
    }

    private void putUnsignedInt(ByteBuffer buff, long value) {
        buff.putInt((int) (value & 0xffffffffL));
    }

    private byte[] getBytes(double[] array) {
        try {
            ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
            DataOutputStream datastream = new DataOutputStream(bytestream);
            for (double n : array) {
                datastream.writeDouble(n);
            }
            datastream.flush();
            return bytestream.toByteArray();
        } catch (IOException ioe) {
            Logging.logger().finest(ioe.getMessage());
        }
        return null;
    }

    private byte[] getBytes(short[] array) {
        try {
            ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
            DataOutputStream datastream = new DataOutputStream(bytestream);
            for (short n : array) {
                datastream.writeShort(n);
            }
            datastream.flush();
            return bytestream.toByteArray();
        } catch (IOException ioe) {
            Logging.logger().finest(ioe.getMessage());
        }
        return null;
    }

    public void writeRaster(BufferWrapperRaster raster) throws IOException, IllegalArgumentException {
        if (raster == null) {
            String msg = Logging.getMessage("nullValue.RasterIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (0 == raster.getWidth() || 0 == raster.getHeight()) {
            String msg = Logging.getMessage("generic.InvalidImageSize", raster.getWidth(), raster.getHeight());
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        this.validateParameters(raster, raster.getWidth(), raster.getHeight());
        int bitsPerSample, samplesPerPixel, sampleFormat, photometric, numBands;
        if (AVKey.ELEVATION.equals(raster.getValue(AVKey.PIXEL_FORMAT))) {
            if (AVKey.FLOAT32.equals(raster.getValue(AVKey.DATA_TYPE))) {
                numBands = 1;
                samplesPerPixel = Tiff.SamplesPerPixel.MONOCHROME;
                sampleFormat = Tiff.SampleFormat.IEEEFLOAT;
                photometric = Tiff.Photometric.Grayscale_BlackIsZero;
                bitsPerSample = Tiff.BitsPerSample.ELEVATIONS_FLOAT32;
            } else if (AVKey.INT16.equals(raster.getValue(AVKey.DATA_TYPE))) {
                numBands = 1;
                samplesPerPixel = Tiff.SamplesPerPixel.MONOCHROME;
                sampleFormat = Tiff.SampleFormat.SIGNED;
                photometric = Tiff.Photometric.Grayscale_BlackIsZero;
                bitsPerSample = Tiff.BitsPerSample.ELEVATIONS_INT16;
            } else {
                String msg = Logging.getMessage("GeotiffWriter.UnsupportedType", raster.getValue(AVKey.DATA_TYPE));
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }
        } else if (AVKey.IMAGE.equals(raster.getValue(AVKey.PIXEL_FORMAT))) {
            if (AVKey.INT8.equals(raster.getValue(AVKey.DATA_TYPE))) {
                numBands = 1;
                samplesPerPixel = Tiff.SamplesPerPixel.MONOCHROME;
                sampleFormat = Tiff.SampleFormat.UNSIGNED;
                photometric = Tiff.Photometric.Grayscale_BlackIsZero;
                bitsPerSample = Tiff.BitsPerSample.MONOCHROME_UINT8;
            } else if (AVKey.INT16.equals(raster.getValue(AVKey.DATA_TYPE))) {
                numBands = 1;
                samplesPerPixel = Tiff.SamplesPerPixel.MONOCHROME;
                sampleFormat = Tiff.SampleFormat.UNSIGNED;
                photometric = Tiff.Photometric.Grayscale_BlackIsZero;
                bitsPerSample = Tiff.BitsPerSample.MONOCHROME_UINT16;
            } else if (AVKey.INT32.equals(raster.getValue(AVKey.DATA_TYPE))) {
                numBands = 3;
                samplesPerPixel = Tiff.SamplesPerPixel.RGB;
                sampleFormat = Tiff.SampleFormat.UNSIGNED;
                photometric = Tiff.Photometric.Color_RGB;
                bitsPerSample = Tiff.BitsPerSample.RGB;
            } else {
                String msg = Logging.getMessage("GeotiffWriter.UnsupportedType", raster.getValue(AVKey.DATA_TYPE));
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }
        } else {
            String msg = Logging.getMessage("GeotiffWriter.UnsupportedType", raster.getValue(AVKey.PIXEL_FORMAT));
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        int bytesPerSample = numBands * bitsPerSample / Byte.SIZE;
        this.writeTiffHeader();
        int numRows = raster.getHeight();
        int numCols = raster.getWidth();
        int[] stripCounts = new int[numRows];
        int[] stripOffsets = new int[numRows];
        BufferWrapper srcBuffer = raster.getBuffer();
        ByteBuffer dataBuff = ByteBuffer.allocateDirect(numCols * bytesPerSample);
        switch(bitsPerSample) {
            case Tiff.BitsPerSample.MONOCHROME_UINT8:
                {
                    for (int y = 0; y < numRows; y++) {
                        stripOffsets[y] = (int) this.theChannel.position();
                        stripCounts[y] = numCols * bytesPerSample;
                        dataBuff.clear();
                        for (int x = 0; x < numCols * numBands; x++) {
                            dataBuff.put(srcBuffer.getByte(x + y * numCols));
                        }
                        dataBuff.flip();
                        this.theChannel.write(dataBuff);
                    }
                }
                break;
            case Tiff.BitsPerSample.ELEVATIONS_INT16:
                {
                    for (int y = 0; y < numRows; y++) {
                        stripOffsets[y] = (int) this.theChannel.position();
                        stripCounts[y] = numCols * bytesPerSample;
                        dataBuff.clear();
                        for (int x = 0; x < numCols * numBands; x++) {
                            dataBuff.putShort(srcBuffer.getShort(x + y * numCols));
                        }
                        dataBuff.flip();
                        this.theChannel.write(dataBuff);
                    }
                }
                break;
            case Tiff.BitsPerSample.ELEVATIONS_FLOAT32:
                {
                    for (int y = 0; y < numRows; y++) {
                        stripOffsets[y] = (int) this.theChannel.position();
                        stripCounts[y] = numCols * bytesPerSample;
                        dataBuff.clear();
                        for (int x = 0; x < numCols * numBands; x++) {
                            dataBuff.putFloat(srcBuffer.getFloat(x + y * numCols));
                        }
                        dataBuff.flip();
                        this.theChannel.write(dataBuff);
                    }
                }
                break;
            case Tiff.BitsPerSample.RGB:
                {
                    for (int y = 0; y < numRows; y++) {
                        stripOffsets[y] = (int) this.theChannel.position();
                        stripCounts[y] = numCols * bytesPerSample;
                        dataBuff.clear();
                        for (int x = 0; x < numCols; x++) {
                            int color = srcBuffer.getInt(x + y * numCols);
                            byte red = (byte) (0xFF & (color >> 16));
                            byte green = (byte) (0xFF & (color >> 8));
                            byte blue = (byte) (0xFF & color);
                            dataBuff.put(red).put(green).put(blue);
                        }
                        dataBuff.flip();
                        this.theChannel.write(dataBuff);
                    }
                }
                break;
        }
        ArrayList<TiffIFDEntry> ifds = new ArrayList<TiffIFDEntry>(10);
        ifds.add(new TiffIFDEntry(Tiff.Tag.IMAGE_WIDTH, Tiff.Type.LONG, 1, numCols));
        ifds.add(new TiffIFDEntry(Tiff.Tag.IMAGE_LENGTH, Tiff.Type.LONG, 1, numRows));
        long offset = this.theChannel.position();
        if (Tiff.BitsPerSample.RGB == bitsPerSample) {
            short[] bps = new short[numBands];
            for (int i = 0; i < numBands; i++) {
                bps[i] = Tiff.BitsPerSample.MONOCHROME_BYTE;
            }
            this.theChannel.write(ByteBuffer.wrap(this.getBytes(bps)));
            ifds.add(new TiffIFDEntry(Tiff.Tag.BITS_PER_SAMPLE, Tiff.Type.SHORT, numBands, offset));
        } else ifds.add(new TiffIFDEntry(Tiff.Tag.BITS_PER_SAMPLE, Tiff.Type.SHORT, 1, bitsPerSample));
        ifds.add(new TiffIFDEntry(Tiff.Tag.COMPRESSION, Tiff.Type.LONG, 1, Tiff.Compression.NONE));
        ifds.add(new TiffIFDEntry(Tiff.Tag.PHOTO_INTERPRETATION, Tiff.Type.SHORT, 1, photometric));
        ifds.add(new TiffIFDEntry(Tiff.Tag.SAMPLES_PER_PIXEL, Tiff.Type.SHORT, 1, samplesPerPixel));
        ifds.add(new TiffIFDEntry(Tiff.Tag.ORIENTATION, Tiff.Type.SHORT, 1, Tiff.Orientation.DEFAULT));
        ifds.add(new TiffIFDEntry(Tiff.Tag.PLANAR_CONFIGURATION, Tiff.Type.SHORT, 1, Tiff.PlanarConfiguration.CHUNKY));
        ifds.add(new TiffIFDEntry(Tiff.Tag.SAMPLE_FORMAT, Tiff.Type.SHORT, 1, sampleFormat));
        offset = this.theChannel.position();
        dataBuff = ByteBuffer.allocateDirect(stripOffsets.length * INTEGER_SIZEOF);
        for (int stripOffset : stripOffsets) {
            dataBuff.putInt(stripOffset);
        }
        dataBuff.flip();
        this.theChannel.write(dataBuff);
        ifds.add(new TiffIFDEntry(Tiff.Tag.STRIP_OFFSETS, Tiff.Type.LONG, stripOffsets.length, offset));
        ifds.add(new TiffIFDEntry(Tiff.Tag.ROWS_PER_STRIP, Tiff.Type.LONG, 1, 1));
        offset = this.theChannel.position();
        dataBuff.clear();
        for (int stripCount : stripCounts) {
            dataBuff.putInt(stripCount);
        }
        dataBuff.flip();
        this.theChannel.write(dataBuff);
        ifds.add(new TiffIFDEntry(Tiff.Tag.STRIP_BYTE_COUNTS, Tiff.Type.LONG, stripCounts.length, offset));
        this.appendGeoTiff(ifds, raster);
        this.writeIFDs(ifds);
    }
}

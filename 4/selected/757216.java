package com.tamanderic.smupload;

import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.lang.reflect.*;

public class ExifInfo {

    /***********************************************************************
     * PRIVATE DATA
     **********************************************************************/
    private static final String PROG_NAME = ExifInfo.class.getName();

    private boolean exifDataPopulated;

    private String filename;

    private String errorMsg;

    private String debugMsg;

    private boolean debugMode;

    private int tiffOffset;

    private String exifHeaderString;

    private ByteOrder byteOrder;

    private IfdData ifd0Data;

    private IfdData subIfdData;

    /***********************************************************************
     * Static Utility Functions
     **********************************************************************/
    public static String getYear(String dateTime) {
        return dateTime.substring(0, 4);
    }

    public static String getMonth(String dateTime) {
        return dateTime.substring(5, 7);
    }

    public static String getDay(String dateTime) {
        return dateTime.substring(8, 10);
    }

    public static String getHour(String dateTime) {
        return dateTime.substring(11, 13);
    }

    public static String getMinute(String dateTime) {
        return dateTime.substring(14, 16);
    }

    public static String getSecond(String dateTime) {
        return dateTime.substring(17, 19);
    }

    /***********************************************************************
     * Constructors
     **********************************************************************/
    public ExifInfo(String filename) {
        this(filename, false);
    }

    public ExifInfo(String filename, boolean debugMode) {
        this.filename = filename;
        this.exifDataPopulated = false;
        this.errorMsg = null;
        this.debugMsg = null;
        this.debugMode = debugMode;
        this.tiffOffset = 0;
        this.ifd0Data = new IfdData("IFD0", 16, debugMode);
        this.subIfdData = new IfdData("SubIFD", 16, debugMode);
        this.populateExifData();
    }

    /***********************************************************************
     * Error Handling Functions (and supporting private functions)
     **********************************************************************/
    public String getErrorMsg() {
        if (this.debugMode && this.debugMsg != null) {
            return this.errorMsg + " (" + this.debugMsg + ")";
        }
        return this.errorMsg;
    }

    private void setErrorMsg(String msg) {
        this.setErrorMsg(msg, null);
    }

    private void setErrorMsg(String msg, String debugMsg) {
        if (this.errorMsg == null) {
            this.errorMsg = msg;
            this.debugMsg = debugMsg;
        }
    }

    /***********************************************************************
     * EXIF Data Access Methods
     **********************************************************************/
    public String getImageDescription() {
        return this.ifd0Data.getString(Exif.IFD0Tag.IMAGE_DESCRIPTION);
    }

    public String getMake() {
        return this.ifd0Data.getString(Exif.IFD0Tag.MAKE);
    }

    public String getModel() {
        return this.ifd0Data.getString(Exif.IFD0Tag.MODEL);
    }

    public Long getOrientation() {
        return this.ifd0Data.getValue(Exif.IFD0Tag.ORIENTATION);
    }

    public Long[] getXResolution() {
        return this.ifd0Data.getRational(Exif.IFD0Tag.X_RESOLUTION);
    }

    public Long[] getYResolution() {
        return this.ifd0Data.getRational(Exif.IFD0Tag.Y_RESOLUTION);
    }

    public Long getResolutionUnit() {
        return this.ifd0Data.getValue(Exif.IFD0Tag.RESOLUTION_UNIT);
    }

    public String getSoftware() {
        return this.ifd0Data.getString(Exif.IFD0Tag.SOFTWARE);
    }

    public String getDateTime() {
        return this.ifd0Data.getString(Exif.IFD0Tag.DATE_TIME);
    }

    public Long[][] getWhitePoint() {
        return this.ifd0Data.getRationalList(Exif.IFD0Tag.WHITE_POINT);
    }

    public Long[][] getPrimaryChromaticities() {
        return this.ifd0Data.getRationalList(Exif.IFD0Tag.PRIMARY_CHROMATICITIES);
    }

    public Long[][] getYCbCrCoefficients() {
        return this.ifd0Data.getRationalList(Exif.IFD0Tag.Y_CB_CR_COEFFICIENTS);
    }

    public Long getYCbCrPositioning() {
        return this.ifd0Data.getValue(Exif.IFD0Tag.Y_CB_CR_POSITIONING);
    }

    public Long[][] getReferenceBlackWhite() {
        return this.ifd0Data.getRationalList(Exif.IFD0Tag.REFERENCE_BLACK_WHITE);
    }

    public String getCopyright() {
        return this.ifd0Data.getString(Exif.IFD0Tag.COPYRIGHT);
    }

    public Long[] getExposureTime() {
        return this.subIfdData.getRational(Exif.SubIFDTag.EXPOSURE_TIME);
    }

    public Long[] getFNumber() {
        return this.subIfdData.getRational(Exif.SubIFDTag.F_NUMBER);
    }

    public Long getExposureProgram() {
        return this.subIfdData.getValue(Exif.SubIFDTag.EXPOSURE_PROGRAM);
    }

    public Long[] getISOSpeedRatings() {
        return this.subIfdData.getValueList(Exif.SubIFDTag.ISO_SPEED_RATINGS);
    }

    public String getDateTimeOriginal() {
        return this.subIfdData.getString(Exif.SubIFDTag.DATE_TIME_ORIGINAL);
    }

    public String getDateTimeDigitized() {
        return this.subIfdData.getString(Exif.SubIFDTag.DATE_TIME_DIGITIZED);
    }

    public Long[] getCompressedBitsPerPixel() {
        return this.subIfdData.getRational(Exif.SubIFDTag.COMPRESSED_BITS_PER_PIXEL);
    }

    public Long[] getShutterSpeedValue() {
        return this.subIfdData.getRational(Exif.SubIFDTag.SHUTTER_SPEED_VALUE);
    }

    public Long[] getApertureValue() {
        return this.subIfdData.getRational(Exif.SubIFDTag.APERTURE_VALUE);
    }

    public Long[] getBrightnessValue() {
        return this.subIfdData.getRational(Exif.SubIFDTag.BRIGHTNESS_VALUE);
    }

    public Long[] getExposureBiasValue() {
        return this.subIfdData.getRational(Exif.SubIFDTag.EXPOSURE_BIAS_VALUE);
    }

    public Long[] getMaxApertureValue() {
        return this.subIfdData.getRational(Exif.SubIFDTag.MAX_APERTURE_VALUE);
    }

    public Long[] getSubjectDistance() {
        return this.subIfdData.getRational(Exif.SubIFDTag.SUBJECT_DISTANCE);
    }

    public Long getMeteringMode() {
        return this.subIfdData.getValue(Exif.SubIFDTag.METERING_MODE);
    }

    public Long getLightSource() {
        return this.subIfdData.getValue(Exif.SubIFDTag.LIGHT_SOURCE);
    }

    public Long getFlash() {
        return this.subIfdData.getValue(Exif.SubIFDTag.FLASH);
    }

    public Long[] getFocalLength() {
        return this.subIfdData.getRational(Exif.SubIFDTag.FOCAL_LENGTH);
    }

    public String getSubsecTime() {
        return this.subIfdData.getString(Exif.SubIFDTag.SUBSEC_TIME);
    }

    public String getSubsecTimeOriginal() {
        return this.subIfdData.getString(Exif.SubIFDTag.SUBSEC_TIME_ORIGINAL);
    }

    public String getSubsecTimeDigitized() {
        return this.subIfdData.getString(Exif.SubIFDTag.SUBSEC_TIME_DIGITIZED);
    }

    public Long getColorSpace() {
        return this.subIfdData.getValue(Exif.SubIFDTag.COLOR_SPACE);
    }

    public Long getExifImageWidth() {
        return this.subIfdData.getValue(Exif.SubIFDTag.EXIF_IMAGE_WIDTH);
    }

    public Long getExifImageHeight() {
        return this.subIfdData.getValue(Exif.SubIFDTag.EXIF_IMAGE_HEIGHT);
    }

    public String getRelatedSoundFile() {
        return this.subIfdData.getString(Exif.SubIFDTag.RELATED_SOUND_FILE);
    }

    public Long getExifInteroperabilityOffset() {
        return this.subIfdData.getValue(Exif.SubIFDTag.EXIF_INTEROPERABILITY_OFFSET);
    }

    public Long[] getFocalPlaneXResolution() {
        return this.subIfdData.getRational(Exif.SubIFDTag.FOCAL_PLANE_X_RESOLUTION);
    }

    public Long[] getFocalPlaneYResolution() {
        return this.subIfdData.getRational(Exif.SubIFDTag.FOCAL_PLANE_Y_RESOLUTION);
    }

    public Long getFocalPlaneResolutionUnit() {
        return this.subIfdData.getValue(Exif.SubIFDTag.FOCAL_PLANE_RESOLUTION_UNIT);
    }

    public Long[] getExposureIndex() {
        return this.subIfdData.getRational(Exif.SubIFDTag.EXPOSURE_INDEX);
    }

    public Long getSensingMethod() {
        return this.subIfdData.getValue(Exif.SubIFDTag.SENSING_METHOD);
    }

    public String getFilename() {
        return this.filename;
    }

    /***********************************************************************
     * Driving function for reading all EXIF data
     **********************************************************************/
    private void populateExifData() {
        if (this.exifDataPopulated) {
            return;
        }
        this.exifDataPopulated = true;
        File exifFile = new File(this.filename);
        FileInputStream exifInputStream;
        try {
            exifInputStream = new FileInputStream(exifFile);
        } catch (FileNotFoundException e) {
            this.setErrorMsg("file " + this.filename + " does not exist");
            return;
        }
        if (!exifFile.isFile()) {
            this.setErrorMsg("file " + this.filename + " is not a exif file");
            return;
        }
        ByteBuffer exifBuffer = this.getExifDataBuffer(exifInputStream.getChannel());
        if (exifBuffer == null) {
            return;
        }
        if (this.debugMode) {
            System.out.println("Have EXIF buffer of size " + (exifBuffer.limit() - exifBuffer.position()) + ", limit " + exifBuffer.limit());
        }
        this.parseExifBuffer(exifBuffer);
    }

    /***********************************************************************
     * Helper function to get a buffer with the raw EXIF data
     **********************************************************************/
    private ByteBuffer getExifDataBuffer(FileChannel exifInputChannel) {
        int bufferSize = (this.debugMode) ? 1 : 16384;
        final int BUFFER_SIZE_MAX = 1024 * 1024;
        boolean tryBiggerBuffer = false;
        int int32;
        for (; ; ) {
            try {
                exifInputChannel.position(0);
                ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
                if (exifInputChannel.read(buffer) == -1) {
                    this.setErrorMsg("failed to read file " + this.filename);
                    return null;
                }
                buffer.order(ByteOrder.BIG_ENDIAN);
                buffer.flip();
                if (buffer.limit() < bufferSize) {
                    this.setErrorMsg("short file read of " + this.filename + ", read " + buffer.limit() + " of " + bufferSize + " bytes");
                    return null;
                }
                HeaderInfo header = new HeaderInfo();
                header.readFromBufferMarkerOnly(buffer);
                if (header.getMarker() != Exif.Marker.START_OF_IMAGE) {
                    this.setErrorMsg("not a supported image file", "marker was " + Integer.toHexString(header.getMarker()) + ", expected " + Integer.toHexString(Exif.Marker.START_OF_IMAGE));
                    return null;
                }
                for (; ; ) {
                    header.readFromBuffer(buffer);
                    if (header.getMarker() == Exif.Marker.START_OF_SCAN) {
                        this.setErrorMsg("cannot find EXIF data section", "found StartOfScan marker");
                        return null;
                    }
                    if (!header.isValidMarker()) {
                        this.setErrorMsg("cannot find EXIF data section", "invalid marker was " + Integer.toHexString(header.getMarker()));
                        return null;
                    }
                    if (this.debugMode) {
                        System.out.println("Found marker " + Integer.toHexString(header.getMarker()) + " size " + header.getSize());
                    }
                    int nextPosition = buffer.position() + header.getSize() - 2;
                    if (nextPosition > buffer.limit()) {
                        tryBiggerBuffer = true;
                        break;
                    }
                    if (header.getMarker() == Exif.Marker.EXIF_DATA_APP1) {
                        buffer.limit(buffer.position() + header.getSize() - 2);
                        return buffer;
                    }
                    if (this.debugMode) {
                        System.out.println("Set buffer position to " + nextPosition + ", current limit is " + buffer.limit());
                    }
                    buffer.position(nextPosition);
                }
            } catch (IOException e) {
                this.setErrorMsg("I/O error reading " + this.getFilename());
                return null;
            } catch (BufferUnderflowException e) {
                tryBiggerBuffer = true;
            }
            if (tryBiggerBuffer) {
                if (bufferSize > BUFFER_SIZE_MAX) {
                    this.setErrorMsg("Failed to find EXIF data in first " + bufferSize + " bytes, giving up");
                    return null;
                }
                bufferSize *= 2;
                tryBiggerBuffer = false;
                if (this.debugMode) {
                    System.out.println("Resize buffer to " + bufferSize);
                }
            }
        }
    }

    /***********************************************************************
     * Driving function for parsing the EXIF data out of the raw buffer
     **********************************************************************/
    private void parseExifBuffer(ByteBuffer exifBuffer) {
        try {
            int int32;
            byte[] twobytes = new byte[2];
            byte[] sixbytes = new byte[6];
            exifBuffer.get(sixbytes);
            this.exifHeaderString = new String(sixbytes);
            if (this.debugMode) {
                System.out.println("Exif header string: " + this.exifHeaderString);
            }
            this.tiffOffset = exifBuffer.position();
            exifBuffer.get(twobytes);
            String endianString = new String(twobytes);
            if (endianString.equals("II")) {
                this.byteOrder = ByteOrder.LITTLE_ENDIAN;
            } else if (endianString.equals("MM")) {
                this.byteOrder = ByteOrder.BIG_ENDIAN;
            } else {
                this.setErrorMsg("malformed EXIF data", "endian marker was " + endianString);
                return;
            }
            exifBuffer.order(this.byteOrder);
            int32 = Unsigned.get16(exifBuffer);
            if (int32 != 0x002a) {
                this.setErrorMsg("malformed EXIF data", "expected header marker 0x002a, got " + Integer.toHexString(int32) + " instead");
                return;
            }
            int ifdOffset = exifBuffer.getInt();
            if (ifdOffset < 8) {
                this.setErrorMsg("malformed EXIF data", "ifdoffset is " + ifdOffset);
                return;
            }
            exifBuffer.position(this.tiffOffset + ifdOffset);
            this.ifd0Data.parseIfd(exifBuffer, this.tiffOffset);
            Long subIfdOffset = this.ifd0Data.getValue(Exif.IFD0Tag.EXIF_OFFSET);
            if (subIfdOffset != null) {
                exifBuffer.position(this.tiffOffset + subIfdOffset.intValue());
                this.subIfdData.parseIfd(exifBuffer, this.tiffOffset);
            }
        } catch (BufferUnderflowException e) {
            this.setErrorMsg("malformed EXIF data");
            return;
        }
        return;
    }

    /***********************************************************************
     * Run the named method and print the output
     **********************************************************************/
    private void printItem(String itemName) {
        String methodName = "get" + itemName;
        Object retObject;
        try {
            String str;
            Method method = this.getClass().getMethod(methodName);
            retObject = method.invoke(this);
            if (retObject instanceof String) {
                System.out.println("\t" + itemName + ": " + retObject);
            } else if (retObject instanceof Long) {
                System.out.println("\t" + itemName + ": " + retObject);
            }
        } catch (Exception e) {
            System.out.println(itemName + ": " + e.getMessage());
        }
    }

    /***********************************************************************
     * Unit test of module
     *
     * Initialize an object and try all the data access methods
     **********************************************************************/
    public static void main(String[] args) {
        Long[][] rational;
        if (args.length < 1) {
            usage();
            System.exit(1);
        }
        ExifInfo ei1 = new ExifInfo(args[0], true);
        System.out.println(ei1.getFilename() + ":");
        ei1.printItem("ImageDescription");
        ei1.printItem("Make");
        ei1.printItem("Model");
        ei1.printItem("Orientation");
        ei1.printItem("XResolution");
        ei1.printItem("YResolution");
        ei1.printItem("ResolutionUnit");
        ei1.printItem("Software");
        ei1.printItem("DateTime");
        ei1.printItem("PrimaryChromaticities");
        ei1.printItem("YCbCrCoefficients");
        ei1.printItem("YCbCrPositioning");
        ei1.printItem("ReferenceBlackWhite");
        ei1.printItem("Copyright");
        ei1.printItem("ExposureTime");
        ei1.printItem("FNumber");
        ei1.printItem("ExposureProgram");
        ei1.printItem("ISOSpeedRatings");
        ei1.printItem("DateTimeOriginal");
        ei1.printItem("DateTimeDigitized");
        ei1.printItem("CompressedBitsPerPixel");
        ei1.printItem("ShutterSpeedValue");
        ei1.printItem("ApertureValue");
        ei1.printItem("BrightnessValue");
        ei1.printItem("ExposureBiasValue");
        ei1.printItem("MaxApertureValue");
        ei1.printItem("SubjectDistance");
        ei1.printItem("MeteringMode");
        ei1.printItem("LightSource");
        ei1.printItem("Flash");
        ei1.printItem("FocalLength");
        ei1.printItem("SubsecTime");
        ei1.printItem("SubsecTimeOriginal");
        ei1.printItem("SubsecTimeDigitized");
        ei1.printItem("ColorSpace");
        ei1.printItem("ExifImageWidth");
        ei1.printItem("ExifImageHeight");
        ei1.printItem("RelatedSoundFile");
        ei1.printItem("ExifInteroperabilityOffset");
        ei1.printItem("FocalPlaneXResolution");
        ei1.printItem("FocalPlaneYResolution");
        ei1.printItem("FocalPlaneResolutionUnit");
        ei1.printItem("ExposureIndex");
        ei1.printItem("SensingMethod");
        String errStr = ei1.getErrorMsg();
        if (errStr != null) {
            System.out.println("Error: " + errStr);
        }
        String dateTime = ei1.getDateTimeOriginal();
        System.out.println("Year( " + dateTime + ") = " + ExifInfo.getYear(dateTime));
        System.out.println("Month( " + dateTime + ") = " + ExifInfo.getMonth(dateTime));
        System.out.println("Day( " + dateTime + ") = " + ExifInfo.getDay(dateTime));
        System.out.println("Hour( " + dateTime + ") = " + ExifInfo.getHour(dateTime));
        System.out.println("Minute( " + dateTime + ") = " + ExifInfo.getMinute(dateTime));
        System.out.println("Second( " + dateTime + ") = " + ExifInfo.getSecond(dateTime));
        System.exit(0);
    }

    private static void usage() {
        System.out.println("Usage: " + PROG_NAME + " filename");
    }
}

/***********************************************************************
 * Helper class to read EXIF file headers
 **********************************************************************/
class HeaderInfo {

    private int marker;

    private int size;

    public HeaderInfo() {
        this.marker = 0;
        this.size = 0;
    }

    public int getMarker() {
        return this.marker;
    }

    public int getSize() {
        return this.size;
    }

    public boolean isValidMarker() {
        return ((this.marker & 0xff00) == 0xff00);
    }

    public void readFromBuffer(ByteBuffer buffer) throws BufferUnderflowException {
        this.marker = buffer.getShort() & 0xffff;
        this.size = buffer.getShort() & 0xffff;
    }

    public void readFromBufferMarkerOnly(ByteBuffer buffer) throws BufferUnderflowException {
        this.marker = buffer.getShort() & 0xffff;
    }
}

/***********************************************************************
 * Utility functions to get an unsigned values from a buffer
 **********************************************************************/
class Unsigned {

    public static int get16(ByteBuffer buffer) {
        return buffer.getShort() & 0xffff;
    }

    public static long get32(ByteBuffer buffer) {
        return buffer.getInt() & 0xffffffff;
    }
}

/***********************************************************************
 * Helper class for parsing and holding per-IFD data
 **********************************************************************/
class IfdData {

    private String ifdName;

    private HashMap<Integer, String> stringMap;

    private HashMap<Integer, Long[]> longMap;

    private HashMap<Integer, Long[][]> rationalMap;

    private HashMap<Integer, Integer> componentSize;

    boolean debugMode;

    public IfdData(String ifdName, int entryCount, boolean debugMode) {
        this.ifdName = ifdName;
        this.debugMode = debugMode;
        this.stringMap = new HashMap<Integer, String>(entryCount);
        this.longMap = new HashMap<Integer, Long[]>(entryCount);
        this.rationalMap = new HashMap<Integer, Long[][]>(entryCount);
        this.componentSize = new HashMap<Integer, Integer>(12);
        this.componentSize.put(Exif.IfdEntryFormat.UNSIGNED_BYTE, 1);
        this.componentSize.put(Exif.IfdEntryFormat.ASCII_STRINGS, 1);
        this.componentSize.put(Exif.IfdEntryFormat.UNSIGNED_SHORT, 2);
        this.componentSize.put(Exif.IfdEntryFormat.UNSIGNED_LONG, 4);
        this.componentSize.put(Exif.IfdEntryFormat.UNSIGNED_RATIONAL, 8);
        this.componentSize.put(Exif.IfdEntryFormat.SIGNED_BYTE, 1);
        this.componentSize.put(Exif.IfdEntryFormat.UNDEFINED, 1);
        this.componentSize.put(Exif.IfdEntryFormat.SIGNED_SHORT, 2);
        this.componentSize.put(Exif.IfdEntryFormat.SIGNED_LONG, 4);
        this.componentSize.put(Exif.IfdEntryFormat.SIGNED_RATIONAL, 8);
        this.componentSize.put(Exif.IfdEntryFormat.SINGLE_FLOAT, 4);
        this.componentSize.put(Exif.IfdEntryFormat.DOUBLE_FLOAT, 8);
    }

    /********************************************************************
     * Value Access Methods
     *******************************************************************/
    public String getString(int tag) {
        return stringMap.get(tag);
    }

    public Long getValue(int tag) {
        Long[] longList = this.longMap.get(tag);
        if (longList == null || longList.length != 1) {
            return null;
        }
        return longList[0];
    }

    public Long[] getValueList(int tag) {
        return this.longMap.get(tag);
    }

    public Long[] getRational(int tag) {
        Long[][] rationalList = this.rationalMap.get(tag);
        if (rationalList == null || rationalList.length != 1) {
            return null;
        }
        return rationalList[0];
    }

    public Long[][] getRationalList(int tag) {
        return this.rationalMap.get(tag);
    }

    /********************************************************************
     * Parse the entries in an IFD into tag-value hashmaps
     *******************************************************************/
    public void parseIfd(ByteBuffer exifBuffer, int baseOffset) {
        int ifdEntryCount = Unsigned.get16(exifBuffer);
        if (this.debugMode) {
            System.out.println(ifdEntryCount + " entries in " + this.ifdName);
        }
        for (int i = 0; i < ifdEntryCount; i++) {
            int tag = Unsigned.get16(exifBuffer);
            int format = Unsigned.get16(exifBuffer);
            int componentCount = exifBuffer.getInt();
            int startPosition = exifBuffer.position();
            int entryData = exifBuffer.getInt();
            int endPosition = exifBuffer.position();
            if (this.debugMode) {
                System.out.println(this.ifdName + "[" + i + "] = " + Integer.toHexString(tag) + "," + format + "," + componentCount + "," + Integer.toHexString(entryData));
            }
            exifBuffer.position(startPosition);
            parseIfdEntry(tag, format, componentCount, entryData, exifBuffer, baseOffset);
            exifBuffer.position(endPosition);
        }
        int offsetToNextIfd = exifBuffer.getInt();
        if (this.debugMode) {
            System.out.println("Offset to next IFD: " + offsetToNextIfd);
        }
    }

    /********************************************************************
     * Helper function to parse an individual IFD entry
     *******************************************************************/
    public void parseIfdEntry(int tag, int format, int componentCount, int entryData, ByteBuffer exifBuffer, int baseOffset) {
        boolean inlineData;
        if (this.componentSize.get(format) * componentCount <= 4) {
            inlineData = true;
        } else {
            inlineData = false;
        }
        if (!inlineData) {
            exifBuffer.position(baseOffset + entryData);
        }
        switch(format) {
            case Exif.IfdEntryFormat.ASCII_STRINGS:
                byte[] asVal = new byte[componentCount];
                exifBuffer.get(asVal);
                this.stringMap.put(tag, new String(asVal));
                break;
            case Exif.IfdEntryFormat.UNSIGNED_SHORT:
                Long[] usVal = new Long[componentCount];
                for (int i = 0; i < componentCount; i++) {
                    usVal[i] = new Long(Unsigned.get16(exifBuffer));
                }
                this.longMap.put(tag, usVal);
                break;
            case Exif.IfdEntryFormat.SIGNED_SHORT:
                Long[] ssVal = new Long[componentCount];
                for (int i = 0; i < componentCount; i++) {
                    ssVal[i] = new Long(exifBuffer.getShort());
                }
                this.longMap.put(tag, ssVal);
                break;
            case Exif.IfdEntryFormat.UNSIGNED_LONG:
                Long[] ulVal = new Long[componentCount];
                for (int i = 0; i < componentCount; i++) {
                    ulVal[i] = new Long(Unsigned.get32(exifBuffer));
                }
                this.longMap.put(tag, ulVal);
                break;
            case Exif.IfdEntryFormat.SIGNED_LONG:
                Long[] slVal = new Long[componentCount];
                for (int i = 0; i < componentCount; i++) {
                    slVal[i] = new Long(exifBuffer.getInt());
                }
                this.longMap.put(tag, slVal);
                break;
            case Exif.IfdEntryFormat.UNSIGNED_RATIONAL:
                Long[][] urVal = new Long[2][componentCount];
                for (int i = 0; i < componentCount; i++) {
                    urVal[0][i] = new Long(Unsigned.get32(exifBuffer));
                    urVal[1][i] = new Long(Unsigned.get32(exifBuffer));
                }
                this.rationalMap.put(tag, urVal);
                break;
            case Exif.IfdEntryFormat.SIGNED_RATIONAL:
                Long[][] srVal = new Long[2][componentCount];
                for (int i = 0; i < componentCount; i++) {
                    srVal[0][i] = new Long(exifBuffer.getInt());
                    srVal[1][i] = new Long(Unsigned.get32(exifBuffer));
                }
                this.rationalMap.put(tag, srVal);
                break;
            default:
                break;
        }
    }
}

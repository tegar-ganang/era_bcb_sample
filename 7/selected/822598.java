package loci.formats.in;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.Vector;
import loci.formats.*;
import loci.formats.codec.LZOCodec;

/**
 * OpenlabReader is the file format reader for Openlab LIFF files.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/loci/formats/in/OpenlabReader.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/loci/formats/in/OpenlabReader.java">SVN</a></dd></dl>
 *
 * @author Melissa Linkert linkert at wisc.edu
 * @author Eric Kjellman egkjellman at wisc.edu
 * @author Curtis Rueden ctrueden at wisc.edu
 */
public class OpenlabReader extends FormatReader {

    /** Image types. */
    private static final int MAC_1_BIT = 1;

    private static final int MAC_256_GREYS = 5;

    private static final int MAC_256_COLORS = 6;

    private static final int MAC_24_BIT = 8;

    private static final int GREY_16_BIT = 16;

    /** Helper reader to read PICT data. */
    private static PictReader pict = new PictReader();

    /** LIFF version (should be 2 or 5). */
    private int version;

    /** Number of series. */
    private int numSeries;

    private Vector[] layerInfoList;

    private float xCal, yCal, zCal;

    private int bytesPerPixel;

    private int tag = 0, subTag = 0;

    private String fmt = "";

    /** Constructs a new OpenlabReader. */
    public OpenlabReader() {
        super("Openlab LIFF", "liff");
    }

    public boolean isThisType(byte[] block) {
        return block.length >= 8 && block[0] == 0 && block[1] == 0 && block[2] == -1 && block[3] == -1 && block[4] == 105 && block[5] == 109 && block[6] == 112 && block[7] == 114;
    }

    public byte[] openBytes(int no, byte[] buf) throws FormatException, IOException {
        FormatTools.assertId(currentId, true, 1);
        FormatTools.checkPlaneNumber(this, no);
        FormatTools.checkBufferSize(this, buf.length);
        buf = openBytes(no);
        return buf;
    }

    public byte[] openBytes(int no) throws FormatException, IOException {
        FormatTools.assertId(currentId, true, 1);
        FormatTools.checkPlaneNumber(this, no);
        LayerInfo info = (LayerInfo) layerInfoList[series].get(no);
        in.seek(info.layerStart);
        readTagHeader();
        if ((tag != 67 && tag != 68) || (!fmt.equals("PICT") && !fmt.equals("RAWi"))) {
            throw new FormatException("Corrupt LIFF file.");
        }
        in.skipBytes(24);
        int volumeType = in.readShort();
        in.skipBytes(272);
        int top, left, bottom, right;
        if (version == 2) {
            in.skipBytes(2);
            top = in.readShort();
            left = in.readShort();
            bottom = in.readShort();
            right = in.readShort();
            if (core.sizeX[series] == 0) core.sizeX[series] = right - left;
            if (core.sizeY[series] == 0) core.sizeY[series] = bottom - top;
        } else {
            core.sizeX[series] = in.readInt();
            core.sizeY[series] = in.readInt();
        }
        in.seek(info.layerStart);
        byte[] b = new byte[0];
        if (version == 2) {
            long nextTag = readTagHeader();
            if ((tag != 67 && tag != 68) || !fmt.equals("PICT")) {
                throw new FormatException("Corrupt LIFF file.");
            }
            in.skipBytes(298);
            Exception exception = null;
            try {
                b = new byte[(int) (nextTag - in.getFilePointer())];
                in.read(b);
                BufferedImage img = pict.open(b);
                byte[][] tmp = ImageTools.getBytes(img);
                b = new byte[tmp.length * tmp[0].length];
                int pt = 0;
                for (int i = 0; i < tmp[0].length; i++) {
                    for (int j = 0; j < tmp.length; j++) {
                        b[pt++] = tmp[j][i];
                    }
                }
            } catch (FormatException exc) {
                exception = exc;
            } catch (IOException exc) {
                exception = exc;
            }
            if (exception != null) {
                if (debug) trace(exception);
                b = null;
                in.seek(info.layerStart + 12);
                int blockSize = DataTools.read4SignedBytes(in, false);
                byte toRead = (byte) in.read();
                if (toRead == 1) in.skipBytes(128);
                in.skipBytes(169);
                byte[] q = new byte[blockSize];
                in.read(q);
                byte[] pixelData = new byte[blockSize];
                int pixPos = 0;
                int length = q.length;
                int num, size;
                int totalBlocks = -1;
                int expectedBlock = 0;
                int pos = 0;
                while (expectedBlock != totalBlocks) {
                    while (pos + 7 < length && (q[pos] != 73 || q[pos + 1] != 86 || q[pos + 2] != 69 || q[pos + 3] != 65 || q[pos + 4] != 100 || q[pos + 5] != 98 || q[pos + 6] != 112 || q[pos + 7] != 113)) {
                        pos++;
                    }
                    pos += 8;
                    num = DataTools.bytesToInt(q, pos, 4, false);
                    if (num != expectedBlock) {
                        throw new FormatException("Expected iPic block not found");
                    }
                    expectedBlock++;
                    if (totalBlocks == -1) {
                        totalBlocks = DataTools.bytesToInt(q, pos + 4, 4, false);
                    } else {
                        if (DataTools.bytesToInt(q, pos + 4, 4, false) != totalBlocks) {
                            throw new FormatException("Unexpected totalBlocks numbein.read");
                        }
                    }
                    pos += 16;
                    size = DataTools.bytesToInt(q, pos, 4, false);
                    pos += 8;
                    System.arraycopy(q, pos, pixelData, pixPos, size);
                    pixPos += size;
                }
                System.gc();
                b = new byte[pixPos];
                System.arraycopy(pixelData, 0, b, 0, b.length);
            }
        } else {
            readTagHeader();
            if (tag != 68 || !fmt.equals("RAWi")) {
                throw new FormatException("Corrupt LIFF file.");
            }
            if (subTag != 0) {
                throw new FormatException("Wrong compression type.");
            }
            in.skipBytes(24);
            volumeType = in.readShort();
            in.skipBytes(280);
            int size = in.readInt();
            int compressedSize = in.readInt();
            b = new byte[size];
            byte[] c = new byte[compressedSize];
            in.read(c);
            LZOCodec lzoc = new LZOCodec();
            b = lzoc.decompress(c);
            if (b.length != size) {
                LogTools.println("LZOCodec failed to predict image size");
                LogTools.println(size + " expected, got " + b.length + ". The image displayed may not be correct.");
            }
            if (volumeType == MAC_24_BIT) {
                bytesPerPixel = b.length >= core.sizeX[series] * core.sizeY[series] * 4 ? 4 : 3;
                int destRowBytes = core.sizeX[series] * bytesPerPixel;
                int srcRowBytes = b.length / core.sizeY[series];
                byte[] tmp = new byte[destRowBytes * core.sizeY[series]];
                int src = 0;
                int dest = 0;
                for (int y = 0; y < core.sizeY[series]; y++) {
                    System.arraycopy(b, src, tmp, dest, destRowBytes);
                    src += srcRowBytes;
                    dest += destRowBytes;
                }
                if (bytesPerPixel == 4) {
                    b = new byte[(3 * tmp.length) / 4];
                    dest = 0;
                    for (int i = 0; i < tmp.length; i += 4) {
                        b[dest] = tmp[i + 1];
                        b[dest + (b.length / 3)] = tmp[i + 2];
                        b[dest + ((2 * b.length) / 3)] = tmp[i + 3];
                        dest++;
                    }
                    bytesPerPixel = 3;
                }
            } else if (volumeType == MAC_256_GREYS) {
                byte[] tmp = b;
                b = new byte[core.sizeX[series] * core.sizeY[series]];
                for (int y = 0; y < core.sizeY[series]; y++) {
                    System.arraycopy(tmp, y * (core.sizeX[series] + 16), b, y * core.sizeX[series], core.sizeX[series]);
                }
            } else if (volumeType < MAC_24_BIT) {
                throw new FormatException("Unsupported image type : " + volumeType);
            }
        }
        int bpp = b.length / (core.sizeX[series] * core.sizeY[series]);
        int expected = core.sizeX[series] * core.sizeY[series] * bpp;
        if (b.length > expected) {
            byte[] tmp = b;
            b = new byte[expected];
            System.arraycopy(tmp, 0, b, 0, b.length);
        }
        return b;
    }

    public void close(boolean fileOnly) throws IOException {
        if (fileOnly) {
            if (in != null) in.close();
            if (pict != null) pict.close(fileOnly);
        } else close();
    }

    public boolean isThisType(String name, boolean open) {
        if (super.isThisType(name, open)) return true;
        if (open) {
            byte[] b = new byte[8];
            try {
                in = new RandomAccessStream(name);
                in.read(b);
            } catch (IOException exc) {
                if (debug) trace(exc);
                return false;
            }
            return isThisType(b);
        } else {
            return name.indexOf(".") < 0;
        }
    }

    public void close() throws IOException {
        super.close();
        if (pict != null) pict.close();
        layerInfoList = null;
    }

    protected void initFile(String id) throws FormatException, IOException {
        if (debug) debug("OpenlabReader.initFile(" + id + ")");
        super.initFile(id);
        in = new RandomAccessStream(id);
        status("Verifying Openlab LIFF format");
        in.order(false);
        in.skipBytes(4);
        if (!in.readString(4).equals("impr")) {
            throw new FormatException("Invalid LIFF file.");
        }
        version = in.readInt();
        if (version != 2 && version != 5) {
            throw new FormatException("Invalid version : " + version);
        }
        in.skipBytes(4);
        int offset = in.readInt();
        in.seek(offset);
        status("Finding image offsets");
        layerInfoList = new Vector[2];
        for (int i = 0; i < layerInfoList.length; i++) layerInfoList[i] = new Vector();
        xCal = yCal = zCal = (float) 0.0;
        while (in.getFilePointer() < in.length()) {
            long nextTag, startPos;
            subTag = tag = 0;
            try {
                startPos = in.getFilePointer();
                nextTag = readTagHeader();
            } catch (IOException exc) {
                if (debug) trace(exc);
                if (in.getFilePointer() >= in.length()) break; else throw new FormatException(exc.getMessage());
            }
            try {
                if (tag == 67 || tag == 68 || fmt.equals("PICT") || fmt.equals("RAWi")) {
                    LayerInfo info = new LayerInfo();
                    info.layerStart = (int) startPos;
                    info.zPosition = -1;
                    info.wavelength = -1;
                    in.skipBytes(24);
                    int volumeType = in.readShort();
                    if (volumeType == MAC_1_BIT || volumeType == MAC_256_GREYS || volumeType == MAC_256_COLORS || (volumeType >= MAC_24_BIT && volumeType <= GREY_16_BIT)) {
                        in.skipBytes(16);
                        info.layerName = in.readString(128);
                        if (!info.layerName.trim().equals("Original Image")) {
                            info.timestamp = in.readLong();
                            layerInfoList[0].add(info);
                        }
                    }
                } else if (tag == 69) {
                    in.skipBytes(18);
                    xCal = in.readFloat();
                    yCal = in.readFloat();
                } else if (tag == 72 || fmt.equals("USER")) {
                    char aChar = (char) in.read();
                    StringBuffer sb = new StringBuffer();
                    while (aChar != 0) {
                        sb = sb.append(aChar);
                        aChar = (char) in.read();
                    }
                    String className = sb.toString();
                    if (className.equals("CVariableList")) {
                        aChar = (char) in.read();
                        if (aChar == 1) {
                            int numVars = in.readShort();
                            while (numVars > 0) {
                                aChar = (char) in.read();
                                sb = new StringBuffer();
                                while (aChar != 0) {
                                    sb = sb.append(aChar);
                                    aChar = (char) in.read();
                                }
                                String varName = "";
                                String varStringValue = "";
                                double varNumValue = 0.0;
                                className = sb.toString();
                                int derivedClassVersion = in.read();
                                if (derivedClassVersion != 1) {
                                    throw new FormatException("Invalid revision.");
                                }
                                if (className.equals("CStringVariable")) {
                                    int strSize = in.readInt();
                                    varStringValue = in.readString(strSize);
                                    varNumValue = Float.parseFloat(varStringValue);
                                    in.skipBytes(1);
                                } else if (className.equals("CFloatVariable")) {
                                    varNumValue = in.readDouble();
                                    varStringValue = "" + varNumValue;
                                }
                                int baseClassVersion = in.read();
                                if (baseClassVersion == 1 || baseClassVersion == 2) {
                                    int strSize = in.readInt();
                                    varName = in.readString(strSize);
                                    in.skipBytes(baseClassVersion == 1 ? 3 : 2);
                                } else {
                                    throw new FormatException("Invalid revision.");
                                }
                                addMeta(varName, varStringValue);
                                numVars--;
                            }
                        }
                    }
                }
                in.seek(nextTag);
            } catch (Exception exc) {
                if (debug) trace(exc);
                in.seek(nextTag);
            }
        }
        Vector tmp = new Vector();
        for (int i = 0; i < layerInfoList[0].size(); i++) {
            tmp.add(layerInfoList[0].get(i));
        }
        core = new CoreMetadata(2);
        core.imageCount[0] = tmp.size();
        status("Determining series count");
        int oldChannels = openBytes(0).length / (core.sizeX[0] * core.sizeY[0] * 3);
        int oldWidth = core.sizeX[0];
        for (int i = 0; i < tmp.size(); i++) {
            LayerInfo layer = (LayerInfo) tmp.get(i);
            in.seek(layer.layerStart);
            long nextTag = readTagHeader();
            if (fmt.equals("PICT")) {
                in.skipBytes(298);
                int top, left, bottom, right;
                if (version == 2) {
                    in.skipBytes(2);
                    top = in.readShort();
                    left = in.readShort();
                    bottom = in.readShort();
                    right = in.readShort();
                    if (core.sizeX[series] == 0) core.sizeX[series] = right - left;
                    if (core.sizeY[series] == 0) core.sizeY[series] = bottom - top;
                } else {
                    core.sizeX[series] = in.readInt();
                    core.sizeY[series] = in.readInt();
                }
                in.seek(layer.layerStart);
                if (version == 2) {
                    nextTag = readTagHeader();
                    if ((tag != 67 && tag != 68) || !fmt.equals("PICT")) {
                        throw new FormatException("Corrupt LIFF file.");
                    }
                    in.skipBytes(298);
                    try {
                        byte[] b = new byte[(int) (nextTag - in.getFilePointer())];
                        in.read(b);
                        BufferedImage img = pict.open(b);
                        if (img.getRaster().getNumBands() != oldChannels || img.getWidth() != oldWidth) {
                            layerInfoList[1].add(tmp.get(i));
                            layerInfoList[0].remove(tmp.get(i));
                        }
                    } catch (FormatException e) {
                    }
                }
            } else {
                in.skipBytes(24);
                int type = DataTools.read2SignedBytes(in, false);
                if (type == MAC_24_BIT) {
                    layerInfoList[1].add(tmp.get(i));
                    layerInfoList[0].remove(tmp.get(i));
                }
            }
        }
        if (layerInfoList[1].size() == 0 || layerInfoList[0].size() == 0) {
            core.sizeC = new int[1];
            core.sizeC[0] = layerInfoList[1].size() == 0 ? 1 : 3;
            if (core.sizeC[0] == 1 && oldChannels == 1) core.sizeC[0] = 3;
            int oldImages = core.imageCount[0];
            core.imageCount = new int[1];
            core.imageCount[0] = oldImages;
            if (layerInfoList[0].size() == 0) layerInfoList[0] = layerInfoList[1];
            int x = core.sizeX[0];
            core.sizeX = new int[1];
            core.sizeX[0] = x;
        } else {
            core.imageCount[0] = layerInfoList[0].size();
            core.imageCount[1] = layerInfoList[1].size();
            core.sizeC[0] = 1;
            core.sizeC[1] = 3;
            int oldW = core.sizeX[0];
            int oldH = core.sizeY[0];
            core.sizeX = new int[2];
            core.sizeY = new int[2];
            core.sizeX[0] = oldW;
            core.sizeX[1] = oldW;
            core.sizeY[0] = oldH;
            core.sizeY[1] = oldH;
        }
        Arrays.fill(core.metadataComplete, true);
        status("Populating metadata");
        numSeries = core.imageCount.length;
        int[] bpp = new int[numSeries];
        Arrays.fill(core.orderCertain, true);
        int oldSeries = getSeries();
        for (int i = 0; i < bpp.length; i++) {
            setSeries(i);
            if (core.sizeC[i] == 0) core.sizeC[i] = 1;
            bpp[i] = openBytes(0).length / (core.sizeX[i] * core.sizeY[i]);
        }
        setSeries(oldSeries);
        if (bytesPerPixel == 3) bytesPerPixel = 1;
        if (bytesPerPixel == 0) bytesPerPixel++;
        addMeta("Version", new Integer(version));
        addMeta("Number of Series", new Integer(numSeries));
        for (int i = 0; i < numSeries; i++) {
            addMeta("Width (Series " + i + ")", new Integer(core.sizeX[i]));
            addMeta("Height (Series " + i + ")", new Integer(core.sizeY[i]));
            addMeta("Bit depth (Series " + i + ")", new Integer(bpp[i] * 8));
            addMeta("Number of channels (Series " + i + ")", new Integer(core.sizeC[i]));
            addMeta("Number of images (Series " + i + ")", new Integer(core.imageCount[i]));
        }
        MetadataStore store = getMetadataStore();
        for (int i = 0; i < numSeries; i++) {
            core.sizeT[i] += 1;
            core.sizeZ[i] = core.imageCount[i] / core.sizeT[i];
            core.currentOrder[i] = isRGB() ? "XYCZT" : "XYZCT";
            core.rgb[i] = core.sizeC[i] > 1;
            core.interleaved[i] = true;
            core.littleEndian[i] = false;
            try {
                if (i != 0) {
                    if (bpp[i] == bpp[0]) bpp[i] = bpp[i + 1];
                }
            } catch (ArrayIndexOutOfBoundsException a) {
            }
            switch(bpp[i]) {
                case 1:
                case 3:
                    core.pixelType[i] = FormatTools.UINT8;
                    break;
                case 2:
                case 6:
                    core.pixelType[i] = FormatTools.UINT16;
                    break;
                case 4:
                    core.pixelType[i] = FormatTools.UINT32;
                    break;
            }
            store.setImage("Series " + i, null, null, new Integer(i));
            store.setDimensions(new Float(xCal), new Float(yCal), new Float(zCal), null, null, new Integer(i));
        }
        FormatTools.populatePixels(store, this);
        for (int i = 0; i < numSeries; i++) {
            for (int j = 0; j < core.sizeC[i]; j++) {
                store.setLogicalChannel(j, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, new Integer(i));
            }
        }
    }

    /** Read the next tag. */
    private long readTagHeader() throws IOException {
        tag = in.readShort();
        subTag = in.readShort();
        long nextTag = (version == 2 ? in.readInt() : in.readLong());
        byte[] b = new byte[4];
        in.read(b);
        fmt = new String(b);
        if (version == 2) in.skipBytes(4); else in.skipBytes(8);
        return nextTag;
    }

    /** Helper class for storing layer info. */
    protected class LayerInfo {

        protected int layerStart;

        protected int zPosition;

        protected int wavelength;

        protected String layerName;

        protected long timestamp;
    }
}

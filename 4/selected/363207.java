package org.apache.sanselan.formats.psd;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.sanselan.ImageFormat;
import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.ImageParser;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.common.byteSources.ByteSource;
import org.apache.sanselan.formats.psd.dataparsers.DataParser;
import org.apache.sanselan.formats.psd.dataparsers.DataParserBitmap;
import org.apache.sanselan.formats.psd.dataparsers.DataParserCMYK;
import org.apache.sanselan.formats.psd.dataparsers.DataParserGrayscale;
import org.apache.sanselan.formats.psd.dataparsers.DataParserIndexed;
import org.apache.sanselan.formats.psd.dataparsers.DataParserLab;
import org.apache.sanselan.formats.psd.dataparsers.DataParserRGB;
import org.apache.sanselan.formats.psd.datareaders.CompressedDataReader;
import org.apache.sanselan.formats.psd.datareaders.DataReader;
import org.apache.sanselan.formats.psd.datareaders.UncompressedDataReader;
import org.apache.sanselan.util.Debug;
import com.google.code.appengine.awt.Dimension;
import com.google.code.appengine.awt.image.BufferedImage;

public class PsdImageParser extends ImageParser {

    public PsdImageParser() {
        super.setByteOrder(BYTE_ORDER_MSB);
    }

    public String getName() {
        return "PSD-Custom";
    }

    public String getDefaultExtension() {
        return DEFAULT_EXTENSION;
    }

    private static final String DEFAULT_EXTENSION = ".psd";

    private static final String ACCEPTED_EXTENSIONS[] = { DEFAULT_EXTENSION };

    protected String[] getAcceptedExtensions() {
        return ACCEPTED_EXTENSIONS;
    }

    protected ImageFormat[] getAcceptedTypes() {
        return new ImageFormat[] { ImageFormat.IMAGE_FORMAT_PSD };
    }

    private PSDHeaderInfo readHeader(ByteSource byteSource) throws ImageReadException, IOException {
        InputStream is = null;
        try {
            is = byteSource.getInputStream();
            return readHeader(is);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception e) {
                Debug.debug(e);
            }
        }
    }

    private PSDHeaderInfo readHeader(InputStream is) throws ImageReadException, IOException {
        readAndVerifyBytes(is, new byte[] { 56, 66, 80, 83 }, "Not a Valid PSD File");
        int Version = read2Bytes("Version", is, "Not a Valid PSD File");
        byte Reserved[] = readByteArray("Reserved", 6, is, "Not a Valid PSD File");
        int Channels = read2Bytes("Channels", is, "Not a Valid PSD File");
        int Rows = read4Bytes("Rows", is, "Not a Valid PSD File");
        int Columns = read4Bytes("Columns", is, "Not a Valid PSD File");
        int Depth = read2Bytes("Depth", is, "Not a Valid PSD File");
        int Mode = read2Bytes("Mode", is, "Not a Valid PSD File");
        PSDHeaderInfo result = new PSDHeaderInfo(Version, Reserved, Channels, Rows, Columns, Depth, Mode);
        return result;
    }

    private ImageContents readImageContents(InputStream is) throws ImageReadException, IOException {
        PSDHeaderInfo header = readHeader(is);
        int ColorModeDataLength = read4Bytes("ColorModeDataLength", is, "Not a Valid PSD File");
        skipBytes(is, ColorModeDataLength);
        int ImageResourcesLength = read4Bytes("ImageResourcesLength", is, "Not a Valid PSD File");
        skipBytes(is, ImageResourcesLength);
        int LayerAndMaskDataLength = read4Bytes("LayerAndMaskDataLength", is, "Not a Valid PSD File");
        skipBytes(is, LayerAndMaskDataLength);
        int Compression = read2Bytes("Compression", is, "Not a Valid PSD File");
        ImageContents result = new ImageContents(header, ColorModeDataLength, ImageResourcesLength, LayerAndMaskDataLength, Compression);
        return result;
    }

    private ArrayList readImageResourceBlocks(byte bytes[], int imageResourceIDs[], int maxBlocksToRead) throws ImageReadException, IOException {
        return readImageResourceBlocks(new ByteArrayInputStream(bytes), imageResourceIDs, maxBlocksToRead, bytes.length);
    }

    private boolean keepImageResourceBlock(int ID, int imageResourceIDs[]) {
        if (imageResourceIDs == null) return true;
        for (int i = 0; i < imageResourceIDs.length; i++) if (ID == imageResourceIDs[i]) return true;
        return false;
    }

    private ArrayList readImageResourceBlocks(InputStream is, int imageResourceIDs[], int maxBlocksToRead, int available) throws ImageReadException, IOException {
        ArrayList result = new ArrayList();
        while (available > 0) {
            readAndVerifyBytes(is, new byte[] { 56, 66, 73, 77 }, "Not a Valid PSD File");
            available -= 4;
            int id = read2Bytes("ID", is, "Not a Valid PSD File");
            available -= 2;
            int nameLength = readByte("NameLength", is, "Not a Valid PSD File");
            available -= 1;
            byte nameBytes[] = readByteArray("NameData", nameLength, is, "Not a Valid PSD File");
            available -= nameLength;
            if (((nameLength + 1) % 2) != 0) {
                int NameDiscard = readByte("NameDiscard", is, "Not a Valid PSD File");
                available -= 1;
            }
            int DataSize = read4Bytes("Size", is, "Not a Valid PSD File");
            available -= 4;
            byte Data[] = readByteArray("Data", DataSize, is, "Not a Valid PSD File");
            available -= DataSize;
            if ((DataSize % 2) != 0) {
                int DataDiscard = readByte("DataDiscard", is, "Not a Valid PSD File");
                available -= 1;
            }
            if (keepImageResourceBlock(id, imageResourceIDs)) {
                result.add(new ImageResourceBlock(id, nameBytes, Data));
                if ((maxBlocksToRead >= 0) && (result.size() >= maxBlocksToRead)) return result;
            }
        }
        return result;
    }

    private ArrayList readImageResourceBlocks(ByteSource byteSource, int imageResourceIDs[], int maxBlocksToRead) throws ImageReadException, IOException {
        InputStream is = null;
        try {
            is = byteSource.getInputStream();
            ImageContents imageContents = readImageContents(is);
            is.close();
            is = this.getInputStream(byteSource, PSD_SECTION_IMAGE_RESOURCES);
            byte ImageResources[] = readByteArray("ImageResources", imageContents.ImageResourcesLength, is, "Not a Valid PSD File");
            return readImageResourceBlocks(ImageResources, imageResourceIDs, maxBlocksToRead);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception e) {
                Debug.debug(e);
            }
        }
    }

    private static final int PSD_SECTION_HEADER = 0;

    private static final int PSD_SECTION_COLOR_MODE = 1;

    private static final int PSD_SECTION_IMAGE_RESOURCES = 2;

    private static final int PSD_SECTION_LAYER_AND_MASK_DATA = 3;

    private static final int PSD_SECTION_IMAGE_DATA = 4;

    private static final int PSD_HEADER_LENGTH = 26;

    private InputStream getInputStream(ByteSource byteSource, int section) throws ImageReadException, IOException {
        InputStream is = byteSource.getInputStream();
        if (section == PSD_SECTION_HEADER) return is;
        skipBytes(is, PSD_HEADER_LENGTH);
        int ColorModeDataLength = read4Bytes("ColorModeDataLength", is, "Not a Valid PSD File");
        if (section == PSD_SECTION_COLOR_MODE) return is;
        skipBytes(is, ColorModeDataLength);
        int ImageResourcesLength = read4Bytes("ImageResourcesLength", is, "Not a Valid PSD File");
        if (section == PSD_SECTION_IMAGE_RESOURCES) return is;
        skipBytes(is, ImageResourcesLength);
        int LayerAndMaskDataLength = read4Bytes("LayerAndMaskDataLength", is, "Not a Valid PSD File");
        if (section == PSD_SECTION_LAYER_AND_MASK_DATA) return is;
        skipBytes(is, LayerAndMaskDataLength);
        int Compression = read2Bytes("Compression", is, "Not a Valid PSD File");
        if (section == PSD_SECTION_IMAGE_DATA) return is;
        throw new ImageReadException("getInputStream: Unknown Section: " + section);
    }

    private byte[] getData(ByteSource byteSource, int section) throws ImageReadException, IOException {
        InputStream is = null;
        try {
            is = byteSource.getInputStream();
            if (section == PSD_SECTION_HEADER) return readByteArray("Header", PSD_HEADER_LENGTH, is, "Not a Valid PSD File");
            skipBytes(is, PSD_HEADER_LENGTH);
            int ColorModeDataLength = read4Bytes("ColorModeDataLength", is, "Not a Valid PSD File");
            if (section == PSD_SECTION_COLOR_MODE) return readByteArray("ColorModeData", ColorModeDataLength, is, "Not a Valid PSD File");
            skipBytes(is, ColorModeDataLength);
            int ImageResourcesLength = read4Bytes("ImageResourcesLength", is, "Not a Valid PSD File");
            if (section == PSD_SECTION_IMAGE_RESOURCES) return readByteArray("ImageResources", ImageResourcesLength, is, "Not a Valid PSD File");
            skipBytes(is, ImageResourcesLength);
            int LayerAndMaskDataLength = read4Bytes("LayerAndMaskDataLength", is, "Not a Valid PSD File");
            if (section == PSD_SECTION_LAYER_AND_MASK_DATA) return readByteArray("LayerAndMaskData", LayerAndMaskDataLength, is, "Not a Valid PSD File");
            skipBytes(is, LayerAndMaskDataLength);
            int Compression = read2Bytes("Compression", is, "Not a Valid PSD File");
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception e) {
                Debug.debug(e);
            }
        }
        throw new ImageReadException("getInputStream: Unknown Section: " + section);
    }

    private ImageContents readImageContents(ByteSource byteSource) throws ImageReadException, IOException {
        InputStream is = null;
        try {
            is = byteSource.getInputStream();
            ImageContents imageContents = readImageContents(is);
            return imageContents;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception e) {
                Debug.debug(e);
            }
        }
    }

    public static final int IMAGE_RESOURCE_ID_ICC_PROFILE = 0x040F;

    public byte[] getICCProfileBytes(ByteSource byteSource, Map params) throws ImageReadException, IOException {
        ArrayList blocks = readImageResourceBlocks(byteSource, new int[] { IMAGE_RESOURCE_ID_ICC_PROFILE }, 1);
        if ((blocks == null) || (blocks.size() < 1)) return null;
        ImageResourceBlock irb = (ImageResourceBlock) blocks.get(0);
        byte bytes[] = irb.data;
        if ((bytes == null) || (bytes.length < 1)) return null;
        return bytes;
    }

    public Dimension getImageSize(ByteSource byteSource, Map params) throws ImageReadException, IOException {
        PSDHeaderInfo bhi = readHeader(byteSource);
        if (bhi == null) throw new ImageReadException("PSD: couldn't read header");
        return new Dimension(bhi.Columns, bhi.Rows);
    }

    public byte[] embedICCProfile(byte image[], byte profile[]) {
        return null;
    }

    public boolean embedICCProfile(File src, File dst, byte profile[]) {
        return false;
    }

    public IImageMetadata getMetadata(ByteSource byteSource, Map params) throws ImageReadException, IOException {
        return null;
    }

    private int getChannelsPerMode(int mode) {
        switch(mode) {
            case 0:
                return 1;
            case 1:
                return 1;
            case 2:
                return -1;
            case 3:
                return 3;
            case 4:
                return 4;
            case 7:
                return -1;
            case 8:
                return -1;
            case 9:
                return 4;
            default:
                return -1;
        }
    }

    public ImageInfo getImageInfo(ByteSource byteSource, Map params) throws ImageReadException, IOException {
        ImageContents imageContents = readImageContents(byteSource);
        if (imageContents == null) throw new ImageReadException("PSD: Couldn't read blocks");
        PSDHeaderInfo header = imageContents.header;
        if (header == null) throw new ImageReadException("PSD: Couldn't read Header");
        int Width = header.Columns;
        int Height = header.Rows;
        ArrayList Comments = new ArrayList();
        int BitsPerPixel = header.Depth * getChannelsPerMode(header.Mode);
        if (BitsPerPixel < 0) BitsPerPixel = 0;
        ImageFormat Format = ImageFormat.IMAGE_FORMAT_PSD;
        String FormatName = "Photoshop";
        String MimeType = "image/x-photoshop";
        int NumberOfImages = -1;
        boolean isProgressive = false;
        int PhysicalWidthDpi = 72;
        float PhysicalWidthInch = (float) ((double) Width / (double) PhysicalWidthDpi);
        int PhysicalHeightDpi = 72;
        float PhysicalHeightInch = (float) ((double) Height / (double) PhysicalHeightDpi);
        String FormatDetails = "Psd";
        boolean isTransparent = false;
        boolean usesPalette = header.Mode == COLOR_MODE_INDEXED;
        int ColorType = ImageInfo.COLOR_TYPE_UNKNOWN;
        String compressionAlgorithm;
        switch(imageContents.Compression) {
            case 0:
                compressionAlgorithm = ImageInfo.COMPRESSION_ALGORITHM_NONE;
                break;
            case 1:
                compressionAlgorithm = ImageInfo.COMPRESSION_ALGORITHM_PSD;
                break;
            default:
                compressionAlgorithm = ImageInfo.COMPRESSION_ALGORITHM_UNKNOWN;
        }
        ImageInfo result = new ImageInfo(FormatDetails, BitsPerPixel, Comments, Format, FormatName, Height, MimeType, NumberOfImages, PhysicalHeightDpi, PhysicalHeightInch, PhysicalWidthDpi, PhysicalWidthInch, Width, isProgressive, isTransparent, usesPalette, ColorType, compressionAlgorithm);
        return result;
    }

    private ImageResourceBlock findImageResourceBlock(ArrayList blocks, int ID) {
        for (int i = 0; i < blocks.size(); i++) {
            ImageResourceBlock block = (ImageResourceBlock) blocks.get(i);
            if (block.id == ID) return block;
        }
        return null;
    }

    public boolean dumpImageFile(PrintWriter pw, ByteSource byteSource) throws ImageReadException, IOException {
        pw.println("gif.dumpImageFile");
        {
            ImageInfo fImageData = getImageInfo(byteSource);
            if (fImageData == null) return false;
            fImageData.toString(pw, "");
        }
        {
            ImageContents imageContents = readImageContents(byteSource);
            imageContents.dump(pw);
            imageContents.header.dump(pw);
            ArrayList blocks = readImageResourceBlocks(byteSource, null, -1);
            pw.println("blocks.size(): " + blocks.size());
            for (int i = 0; i < blocks.size(); i++) {
                ImageResourceBlock block = (ImageResourceBlock) blocks.get(i);
                pw.println("\t" + i + " (" + Integer.toHexString(block.id) + ", " + "'" + new String(block.nameData) + "' (" + block.nameData.length + "), " + " data: " + block.data.length + " type: '" + new PSDConstants().getDescription(block.id) + "' " + ")");
            }
        }
        pw.println("");
        return true;
    }

    private static final int COLOR_MODE_INDEXED = 2;

    public BufferedImage getBufferedImage(ByteSource byteSource, Map params) throws ImageReadException, IOException {
        ImageContents imageContents = readImageContents(byteSource);
        if (imageContents == null) throw new ImageReadException("PSD: Couldn't read blocks");
        PSDHeaderInfo header = imageContents.header;
        if (header == null) throw new ImageReadException("PSD: Couldn't read Header");
        ArrayList blocks = readImageResourceBlocks(byteSource, null, -1);
        int width = header.Columns;
        int height = header.Rows;
        boolean hasAlpha = false;
        BufferedImage result = getBufferedImageFactory(params).getColorBufferedImage(width, height, hasAlpha);
        DataParser dataParser;
        switch(imageContents.header.Mode) {
            case 0:
                dataParser = new DataParserBitmap();
                break;
            case 1:
            case 8:
                dataParser = new DataParserGrayscale();
                break;
            case 3:
                dataParser = new DataParserRGB();
                break;
            case 4:
                dataParser = new DataParserCMYK();
                break;
            case 9:
                dataParser = new DataParserLab();
                break;
            case COLOR_MODE_INDEXED:
                {
                    byte ColorModeData[] = getData(byteSource, PSD_SECTION_COLOR_MODE);
                    dataParser = new DataParserIndexed(ColorModeData);
                    break;
                }
            case 7:
            default:
                throw new ImageReadException("Unknown Mode: " + imageContents.header.Mode);
        }
        DataReader fDataReader;
        switch(imageContents.Compression) {
            case 0:
                fDataReader = new UncompressedDataReader(dataParser);
                break;
            case 1:
                fDataReader = new CompressedDataReader(dataParser);
                break;
            default:
                throw new ImageReadException("Unknown Compression: " + imageContents.Compression);
        }
        {
            InputStream is = null;
            try {
                is = getInputStream(byteSource, PSD_SECTION_IMAGE_DATA);
                fDataReader.readData(is, result, imageContents, this);
                fDataReader.dump();
            } finally {
                try {
                    if (is != null) is.close();
                } catch (Exception e) {
                    Debug.debug(e);
                }
            }
        }
        return result;
    }

    public static final int IMAGE_RESOURCE_ID_XMP = 0x0424;

    public static final String BLOCK_NAME_XMP = "XMP";

    /**
     * Extracts embedded XML metadata as XML string.
     * <p>
     *
     * @param byteSource
     *            File containing image data.
     * @param params
     *            Map of optional parameters, defined in SanselanConstants.
     * @return Xmp Xml as String, if present. Otherwise, returns null.
     */
    public String getXmpXml(ByteSource byteSource, Map params) throws ImageReadException, IOException {
        ImageContents imageContents = readImageContents(byteSource);
        if (imageContents == null) throw new ImageReadException("PSD: Couldn't read blocks");
        PSDHeaderInfo header = imageContents.header;
        if (header == null) throw new ImageReadException("PSD: Couldn't read Header");
        ArrayList blocks = readImageResourceBlocks(byteSource, new int[] { IMAGE_RESOURCE_ID_XMP }, -1);
        if ((blocks == null) || (blocks.size() < 1)) return null;
        List xmpBlocks = new ArrayList();
        if (false) {
            for (int i = 0; i < blocks.size(); i++) {
                ImageResourceBlock block = (ImageResourceBlock) blocks.get(i);
                if (!block.getName().equals(BLOCK_NAME_XMP)) continue;
                xmpBlocks.add(block);
            }
        } else xmpBlocks.addAll(blocks);
        if (xmpBlocks.size() < 1) return null;
        if (xmpBlocks.size() > 1) throw new ImageReadException("PSD contains more than one XMP block.");
        ImageResourceBlock block = (ImageResourceBlock) xmpBlocks.get(0);
        try {
            String xml = new String(block.data, 0, block.data.length, "utf-8");
            return xml;
        } catch (UnsupportedEncodingException e) {
            throw new ImageReadException("Invalid JPEG XMP Segment.");
        }
    }
}

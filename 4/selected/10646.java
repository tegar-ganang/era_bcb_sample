package com.lepidllama.packageeditor.utility;

import gr.zdimensions.jsquish.Squish;
import gr.zdimensions.jsquish.Squish.CompressionType;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import com.lepidllama.packageeditor.fileio.ByteArrayDataReader;
import com.lepidllama.packageeditor.fileio.ByteArrayDataWriter;
import com.lepidllama.packageeditor.fileio.DataReader;
import com.lepidllama.packageeditor.fileio.DataWriter;

/** A reader and writer for DirectDraw Surface (.dds) files, which are
 used to describe textures. These files can contain multiple mipmap
 levels in one file. This class is currently minimal and does not
 support all of the possible file formats. */
public class DDSImage {

    private DataReader buf;

    private Header header;

    int bufSize = -1;

    private long bufStart = 0;

    public static final int DDSD_CAPS = 0x00000001;

    public static final int DDSD_HEIGHT = 0x00000002;

    public static final int DDSD_WIDTH = 0x00000004;

    public static final int DDSD_PITCH = 0x00000008;

    public static final int DDSD_BACKBUFFERCOUNT = 0x00000020;

    public static final int DDSD_ZBUFFERBITDEPTH = 0x00000040;

    public static final int DDSD_ALPHABITDEPTH = 0x00000080;

    public static final int DDSD_LPSURFACE = 0x00000800;

    public static final int DDSD_PIXELFORMAT = 0x00001000;

    public static final int DDSD_MIPMAPCOUNT = 0x00020000;

    public static final int DDSD_LINEARSIZE = 0x00080000;

    public static final int DDSD_DEPTH = 0x00800000;

    public static final int DDPF_ALPHAPIXELS = 0x00000001;

    public static final int DDPF_ALPHA = 0x00000002;

    public static final int DDPF_FOURCC = 0x00000004;

    public static final int DDPF_PALETTEINDEXED4 = 0x00000008;

    public static final int DDPF_PALETTEINDEXEDTO8 = 0x00000010;

    public static final int DDPF_PALETTEINDEXED8 = 0x00000020;

    public static final int DDPF_RGB = 0x00000040;

    public static final int DDPF_COMPRESSED = 0x00000080;

    public static final int DDPF_RGBTOYUV = 0x00000100;

    public static final int DDPF_YUV = 0x00000200;

    public static final int DDPF_ZBUFFER = 0x00000400;

    public static final int DDPF_PALETTEINDEXED1 = 0x00000800;

    public static final int DDPF_PALETTEINDEXED2 = 0x00001000;

    public static final int DDPF_ZPIXELS = 0x00002000;

    public static final int DDSCAPS_TEXTURE = 0x00001000;

    public static final int DDSCAPS_MIPMAP = 0x00400000;

    public static final int DDSCAPS_COMPLEX = 0x00000008;

    public static final int DDSCAPS2_CUBEMAP = 0x00000200;

    public static final int DDSCAPS2_CUBEMAP_POSITIVEX = 0x00000400;

    public static final int DDSCAPS2_CUBEMAP_NEGATIVEX = 0x00000800;

    public static final int DDSCAPS2_CUBEMAP_POSITIVEY = 0x00001000;

    public static final int DDSCAPS2_CUBEMAP_NEGATIVEY = 0x00002000;

    public static final int DDSCAPS2_CUBEMAP_POSITIVEZ = 0x00004000;

    public static final int DDSCAPS2_CUBEMAP_NEGATIVEZ = 0x00008000;

    public static final int D3DFMT_UNKNOWN = 0;

    public static final int D3DFMT_R8G8B8 = 20;

    public static final int D3DFMT_A8R8G8B8 = 21;

    public static final int D3DFMT_X8R8G8B8 = 22;

    public static final int D3DFMT_DXT1 = 0x31545844;

    public static final int D3DFMT_DXT2 = 0x32545844;

    public static final int D3DFMT_DXT3 = 0x33545844;

    public static final int D3DFMT_DXT4 = 0x34545844;

    public static final int D3DFMT_DXT5 = 0x35545844;

    /** Reads a DirectDraw surface from the specified ByteBuffer, returning
	    the resulting DDSImage.

	    @param buf Input data
	    @return DDS image object
	    @throws java.io.IOException if an I/O exception occurred
	 */
    public static DDSImage read(DataReader buf) {
        DDSImage image = new DDSImage();
        image.bufStart = buf.getFilePointer();
        image.buf = buf;
        image.header = new Header();
        image.header.read(buf);
        image.fixupHeader();
        return image;
    }

    /**
	 * Writes this DDSImage to the specified file name. WARNING: WILL ONLY WORK IF FILE HAS BEEN CHANGED!
	 * @param file File object to write to
	 * @throws java.io.IOException if an I/O exception occurred
	 */
    public void write(DataWriter hdr) {
        buf.seek(bufStart);
        hdr.writeChunk(buf.readChunk(bufSize));
    }

    public void setImage(BufferedImage imageOrig) {
        bufStart = 0;
        WritableRaster raster = imageOrig.getRaster();
        byte[] rgba = new byte[imageOrig.getHeight() * imageOrig.getWidth() * 4];
        int offset = 0;
        for (int i = 0; i < imageOrig.getWidth(); i++) {
            for (int j = 0; j < imageOrig.getHeight(); j++) {
                int[] pixel = raster.getPixel(j, i, new int[4]);
                rgba[offset] = (byte) (0xFF & pixel[0]);
                rgba[offset + 1] = (byte) (0xFF & pixel[1]);
                rgba[offset + 2] = (byte) (0xFF & pixel[2]);
                rgba[offset + 3] = (byte) (0xFF & pixel[3]);
                offset += 4;
            }
        }
        CompressionType type;
        switch(getCompressionFormat()) {
            case DDSImage.D3DFMT_DXT1:
                type = Squish.CompressionType.DXT1;
                break;
            case DDSImage.D3DFMT_DXT3:
                type = Squish.CompressionType.DXT3;
                break;
            case DDSImage.D3DFMT_DXT5:
                type = Squish.CompressionType.DXT5;
                break;
            default:
                type = Squish.CompressionType.DXT5;
                break;
        }
        int thingy = ((imageOrig.getWidth() + 3) / 4) * ((imageOrig.getHeight() + 3) / 4) * ((header.backBufferCountOrDepth + 3) / 4);
        byte[] compressed = Squish.compressImage(rgba, imageOrig.getWidth(), imageOrig.getHeight(), new byte[thingy], type);
        header.width = imageOrig.getWidth();
        header.height = imageOrig.getHeight();
        header.mipMapCountOrAux = 1;
        header.pfRGBBitCount = 32;
        ByteArrayDataWriter badw = new ByteArrayDataWriter(compressed.length + Header.writtenSize());
        header.write(badw);
        badw.writeChunk(compressed);
        bufSize = badw.getContent().length;
        buf = new ByteArrayDataReader(badw.getContent());
    }

    /**
	 * Indicates whether this texture is cubemap
	 * @return true if cubemap or false otherwise
	 */
    public boolean isCubemap() {
        return ((header.ddsCaps1 & DDSCAPS_COMPLEX) != 0) && ((header.ddsCaps2 & DDSCAPS2_CUBEMAP) != 0);
    }

    /**
	 * Indicates whether this cubemap side present
	 * @param side Side to test
	 * @return true if side present or false otherwise
	 */
    public boolean isCubemapSidePresent(int side) {
        return isCubemap() && (header.ddsCaps2 & side) != 0;
    }

    /** Indicates whether this texture is compressed. */
    public boolean isCompressed() {
        return (((header.pfFlags & DDPF_FOURCC) != 0));
    }

    /** If this surface is compressed, returns the kind of compression
	    used (DXT1..DXT5). */
    public int getCompressionFormat() {
        return header.pfFourCC;
    }

    /** Width of the texture (or the top-most mipmap if mipmaps are
	    present) */
    public int getWidth() {
        return header.width;
    }

    /** Height of the texture (or the top-most mipmap if mipmaps are
	    present) */
    public int getHeight() {
        return header.height;
    }

    /** Total number of bits per pixel. Only valid if DDPF_RGB is
	    present. For A8R8G8B8, would be 32. */
    public int getDepth() {
        return header.pfRGBBitCount;
    }

    /** Number of mip maps in the texture */
    public int getNumMipMaps() {
        if (!((header.flags & DDSD_MIPMAPCOUNT) != 0)) {
            return 0;
        }
        return header.mipMapCountOrAux;
    }

    /** Gets the <i>i</i>th mipmap data (0..getNumMipMaps() - 1)
	 * @param map Mipmap index
	 * @return Image object
	 * @throws IOException 
	 */
    public byte[] getMipMap(int map) throws IOException {
        return getMipMap(0, map);
    }

    /**
	 * Gets the <i>i</i>th mipmap data (0..getNumMipMaps() - 1)
	 * @param side Cubemap side or 0 for 2D texture
	 * @param map Mipmap index
	 * @return Image object
	 * @throws IOException 
	 */
    public byte[] getMipMap(int side, int map) throws IOException {
        if (!isCubemap() && (side != 0)) {
            throw new IOException("Illegal side for 2D texture: " + side);
        }
        if (isCubemap() && !isCubemapSidePresent(side)) {
            throw new IOException("Illegal side, side not present: " + side);
        }
        if (getNumMipMaps() > 0 && ((map < 0) || (map >= getNumMipMaps()))) {
            throw new IOException("Illegal mipmap number " + map + " (0.." + (getNumMipMaps() - 1) + ")");
        }
        int seek = Header.writtenSize();
        if (isCubemap()) {
            seek += sideShiftInBytes(side);
        }
        for (int i = 0; i < map; i++) {
            seek += mipMapSizeInBytes(i);
        }
        buf.seek(seek + bufStart);
        byte[] imageData = buf.readChunk(mipMapSizeInBytes(map));
        return imageData;
    }

    /** Returns an array of ImageInfos corresponding to all mipmap
	    levels of this DDS file.
	    @return Mipmap image objects set
	 * @throws IOException 
	 */
    public byte[][] getAllMipMaps() throws IOException {
        return getAllMipMaps(0);
    }

    /**
	 * Returns an array of ImageInfos corresponding to all mipmap
	 * levels of this DDS file.
	 * @param side Cubemap side or 0 for 2D texture
	 * @return Mipmap image objects set
	 * @throws IOException 
	 */
    public byte[][] getAllMipMaps(int side) throws IOException {
        int numLevels = getNumMipMaps();
        if (numLevels == 0) {
            numLevels = 1;
        }
        byte[][] result = new byte[numLevels][];
        for (int i = 0; i < numLevels; i++) {
            result[i] = getMipMap(side, i);
        }
        return result;
    }

    /** Converts e.g. DXT1 compression format constant (see {@link
	    #getCompressionFormat}) into "DXT1".
	    @param compressionFormat Compression format constant
	    @return String format code
	 */
    public static String getCompressionFormatName(int compressionFormat) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < 4; i++) {
            char c = (char) (compressionFormat & 0xFF);
            buf.append(c);
            compressionFormat = compressionFormat >> 8;
        }
        return buf.toString();
    }

    private static final int MAGIC = 0x20534444;

    static class Header {

        int size;

        int flags;

        int height;

        int width;

        int pitchOrLinearSize;

        int backBufferCountOrDepth;

        int mipMapCountOrAux;

        int alphaBitDepth;

        int reserved1;

        int surface;

        int colorSpaceLowValue;

        int colorSpaceHighValue;

        int destBltColorSpaceLowValue;

        int destBltColorSpaceHighValue;

        int srcOverlayColorSpaceLowValue;

        int srcOverlayColorSpaceHighValue;

        int srcBltColorSpaceLowValue;

        int srcBltColorSpaceHighValue;

        int pfSize;

        int pfFlags;

        int pfFourCC;

        int pfRGBBitCount;

        int pfRBitMask;

        int pfGBitMask;

        int pfBBitMask;

        int pfABitMask;

        int ddsCaps1;

        int ddsCaps2;

        int ddsCapsReserved1;

        int ddsCapsReserved2;

        int textureStage;

        void read(DataReader buf) {
            int magic = buf.readDwordInt();
            if (magic != MAGIC) {
                System.err.println("Incorrect magic number 0x" + Integer.toHexString(magic) + " (expected  0x" + Integer.toHexString(MAGIC) + ")");
                return;
            }
            size = buf.readDwordInt();
            flags = buf.readDwordInt();
            height = buf.readDwordInt();
            width = buf.readDwordInt();
            pitchOrLinearSize = buf.readDwordInt();
            backBufferCountOrDepth = buf.readDwordInt();
            mipMapCountOrAux = buf.readDwordInt();
            alphaBitDepth = buf.readDwordInt();
            reserved1 = buf.readDwordInt();
            surface = buf.readDwordInt();
            colorSpaceLowValue = buf.readDwordInt();
            colorSpaceHighValue = buf.readDwordInt();
            destBltColorSpaceLowValue = buf.readDwordInt();
            destBltColorSpaceHighValue = buf.readDwordInt();
            srcOverlayColorSpaceLowValue = buf.readDwordInt();
            srcOverlayColorSpaceHighValue = buf.readDwordInt();
            srcBltColorSpaceLowValue = buf.readDwordInt();
            srcBltColorSpaceHighValue = buf.readDwordInt();
            pfSize = buf.readDwordInt();
            pfFlags = buf.readDwordInt();
            pfFourCC = buf.readDwordInt();
            pfRGBBitCount = buf.readDwordInt();
            pfRBitMask = buf.readDwordInt();
            pfGBitMask = buf.readDwordInt();
            pfBBitMask = buf.readDwordInt();
            pfABitMask = buf.readDwordInt();
            ddsCaps1 = buf.readDwordInt();
            ddsCaps2 = buf.readDwordInt();
            ddsCapsReserved1 = buf.readDwordInt();
            ddsCapsReserved2 = buf.readDwordInt();
            textureStage = buf.readDwordInt();
        }

        void write(DataWriter buf) {
            buf.writeDwordInt(MAGIC);
            buf.writeDwordInt(size);
            buf.writeDwordInt(flags);
            buf.writeDwordInt(height);
            buf.writeDwordInt(width);
            buf.writeDwordInt(pitchOrLinearSize);
            buf.writeDwordInt(backBufferCountOrDepth);
            buf.writeDwordInt(mipMapCountOrAux);
            buf.writeDwordInt(alphaBitDepth);
            buf.writeDwordInt(reserved1);
            buf.writeDwordInt(surface);
            buf.writeDwordInt(colorSpaceLowValue);
            buf.writeDwordInt(colorSpaceHighValue);
            buf.writeDwordInt(destBltColorSpaceLowValue);
            buf.writeDwordInt(destBltColorSpaceHighValue);
            buf.writeDwordInt(srcOverlayColorSpaceLowValue);
            buf.writeDwordInt(srcOverlayColorSpaceHighValue);
            buf.writeDwordInt(srcBltColorSpaceLowValue);
            buf.writeDwordInt(srcBltColorSpaceHighValue);
            buf.writeDwordInt(pfSize);
            buf.writeDwordInt(pfFlags);
            buf.writeDwordInt(pfFourCC);
            buf.writeDwordInt(pfRGBBitCount);
            buf.writeDwordInt(pfRBitMask);
            buf.writeDwordInt(pfGBitMask);
            buf.writeDwordInt(pfBBitMask);
            buf.writeDwordInt(pfABitMask);
            buf.writeDwordInt(ddsCaps1);
            buf.writeDwordInt(ddsCaps2);
            buf.writeDwordInt(ddsCapsReserved1);
            buf.writeDwordInt(ddsCapsReserved2);
            buf.writeDwordInt(textureStage);
        }

        private static int writtenSize() {
            return 128;
        }
    }

    private DDSImage() {
    }

    private void fixupHeader() {
        if (isCompressed() && !((header.flags & DDSD_LINEARSIZE) != 0)) {
            int depth = header.backBufferCountOrDepth;
            if (depth == 0) {
                depth = 1;
            }
            header.pitchOrLinearSize = computeCompressedBlockSize(getWidth(), getHeight(), depth, getCompressionFormat());
            header.flags |= DDSD_LINEARSIZE;
        }
    }

    private static int computeCompressedBlockSize(int width, int height, int depth, int compressionFormat) {
        int blockSize = ((width + 3) / 4) * ((height + 3) / 4) * ((depth + 3) / 4);
        switch(compressionFormat) {
            case D3DFMT_DXT1:
                blockSize *= 8;
                break;
            default:
                blockSize *= 16;
                break;
        }
        return blockSize;
    }

    private int mipMapWidth(int map) {
        int width = getWidth();
        for (int i = 0; i < map; i++) {
            width >>= 1;
        }
        return Math.max(width, 1);
    }

    private int mipMapHeight(int map) {
        int height = getHeight();
        for (int i = 0; i < map; i++) {
            height >>= 1;
        }
        return Math.max(height, 1);
    }

    private int mipMapSizeInBytes(int map) {
        int width = mipMapWidth(map);
        int height = mipMapHeight(map);
        if (isCompressed()) {
            int blockSize = (getCompressionFormat() == D3DFMT_DXT1 ? 8 : 16);
            return ((width + 3) / 4) * ((height + 3) / 4) * blockSize;
        } else {
            return width * height * (getDepth() / 8);
        }
    }

    private int sideSizeInBytes() {
        int numLevels = getNumMipMaps();
        if (numLevels == 0) {
            numLevels = 1;
        }
        int size = 0;
        for (int i = 0; i < numLevels; i++) {
            size += mipMapSizeInBytes(i);
        }
        return size;
    }

    private int sideShiftInBytes(int side) throws IOException {
        int[] sides = { DDSCAPS2_CUBEMAP_POSITIVEX, DDSCAPS2_CUBEMAP_NEGATIVEX, DDSCAPS2_CUBEMAP_POSITIVEY, DDSCAPS2_CUBEMAP_NEGATIVEY, DDSCAPS2_CUBEMAP_POSITIVEZ, DDSCAPS2_CUBEMAP_NEGATIVEZ };
        int shift = 0;
        int sideSize = sideSizeInBytes();
        for (int i = 0; i < sides.length; i++) {
            int temp = sides[i];
            if ((temp & side) != 0) {
                return shift;
            }
            shift += sideSize;
        }
        throw new IOException("Illegal side: " + side);
    }

    public boolean isChanged() {
        return bufSize > -1;
    }
}

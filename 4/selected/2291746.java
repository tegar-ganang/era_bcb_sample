package com.jmbaai.bombsight.graphic.image;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import javax.media.opengl.GL;
import com.sun.opengl.util.BufferUtil;

/**
 * A reader and writer for DirectDraw Surface (.dds) files, which are used to
 * describe textures. These files can contain multiple mipmap levels in one
 * file. This class is currently minimal and does not support all of the
 * possible file formats.
 * <p>
 * Derived from com.sun.opengl.util.texture.spi.DDSImage.
 * @author jonb
 */
public class DdsImage {

    /**
	 * Simple class describing images and data; does not encapsulate image
	 * format information. User is responsible for transmitting that information
	 * in another way.
	 */
    public static class ImageInfo {

        private ByteBuffer data;

        private int width;

        private int height;

        private boolean isCompressed;

        private int compressionFormat;

        public ImageInfo(ByteBuffer data, int width, int height, boolean compressed, int compressionFormat) {
            this.data = data;
            this.width = width;
            this.height = height;
            this.isCompressed = compressed;
            this.compressionFormat = compressionFormat;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public ByteBuffer getData() {
            return data;
        }

        public boolean isCompressed() {
            return isCompressed;
        }

        public int getCompressionFormat() {
            if (!isCompressed()) throw new RuntimeException("Should not call unless compressed");
            return compressionFormat;
        }
    }

    private FileInputStream _fis;

    private FileChannel _chan;

    private ByteBuffer _buf;

    private DdsHeader _header;

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

    /**
	 * Reads a DirectDraw surface from the specified file name, returning the
	 * resulting DDSImage.
	 * @param filename File name
	 * @return DDS image object
	 * @throws java.io.IOException if an I/O exception occurred
	 */
    public static DdsImage read(String filename) throws IOException {
        return read(new File(filename));
    }

    /**
	 * Reads a DirectDraw surface from the specified file, returning the
	 * resulting DDSImage.
	 * @param file File object
	 * @return DDS image object
	 * @throws java.io.IOException if an I/O exception occurred
	 */
    public static DdsImage read(File file) throws IOException {
        DdsImage image = new DdsImage();
        image.readFromFile(file);
        return image;
    }

    /**
	 * Reads a DirectDraw surface from the specified ByteBuffer, returning the
	 * resulting DDSImage.
	 * @param buf Input data
	 * @return DDS image object
	 * @throws java.io.IOException if an I/O exception occurred
	 */
    public static DdsImage read(ByteBuffer buf) throws IOException {
        DdsImage image = new DdsImage();
        image.readFromBuffer(buf);
        return image;
    }

    /**
	 * Closes open files and resources associated with the open DDSImage. No
	 * other methods may be called on this object once this is called.
	 */
    public void close() {
        try {
            if (_chan != null) {
                _chan.close();
                _chan = null;
            }
            if (_fis != null) {
                _fis.close();
                _fis = null;
            }
            _buf = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Creates a new DDSImage from data supplied by the user. The resulting
	 * DDSImage can be written to disk using the write() method.
	 * 
	 * @param d3dFormat the D3DFMT_ constant describing the data; it is assumed
	 * that it is packed tightly
	 * @param width the width in pixels of the topmost mipmap image
	 * @param height the height in pixels of the topmost mipmap image
	 * @param mipmapData the data for each mipmap level of the resulting
	 * DDSImage; either only one mipmap level should be specified, or they all
	 * must be
	 * @throws IllegalArgumentException if the data does not match the specified
	 * arguments
	 * @return DDS image object
	 */
    public static DdsImage createFromData(int d3dFormat, int width, int height, ByteBuffer[] mipmapData) throws IllegalArgumentException {
        DdsImage image = new DdsImage();
        image.initFromData(d3dFormat, width, height, mipmapData);
        return image;
    }

    /**
	 * Determines from the magic number whether the given InputStream points to
	 * a DDS image. The given InputStream must return true from markSupported()
	 * and support a minimum of four bytes of read-ahead.
	 * @param in Stream to check
	 * @return true if input stream is DDS image or false otherwise
	 * @throws java.io.IOException if an I/O exception occurred
	 */
    public static boolean isDDSImage(InputStream in) throws IOException {
        if (!(in instanceof BufferedInputStream)) {
            in = new BufferedInputStream(in);
        }
        if (!in.markSupported()) {
            throw new IOException("Can not test non-destructively whether given InputStream is a DDS image");
        }
        in.mark(4);
        int magic = 0;
        for (int i = 0; i < 4; i++) {
            int tmp = in.read();
            if (tmp < 0) {
                in.reset();
                return false;
            }
            magic = ((magic >>> 8) | (tmp << 24));
        }
        in.reset();
        return (magic == DdsHeader.MAGIC);
    }

    /**
	 * Writes this DDSImage to the specified file name.
	 * @param filename File name to write to
	 * @throws java.io.IOException if an I/O exception occurred
	 */
    public void write(String filename) throws IOException {
        write(new File(filename));
    }

    /**
	 * Writes this DDSImage to the specified file name.
	 * @param file File object to write to
	 * @throws java.io.IOException if an I/O exception occurred
	 */
    public void write(File file) throws IOException {
        FileOutputStream stream = new FileOutputStream(file);
        FileChannel chan = stream.getChannel();
        ByteBuffer hdr = ByteBuffer.allocate(DdsHeader.writtenSize());
        hdr.order(ByteOrder.LITTLE_ENDIAN);
        _header.write(hdr);
        hdr.rewind();
        chan.write(hdr);
        _buf.position(DdsHeader.writtenSize());
        chan.write(_buf);
        chan.force(true);
        chan.close();
        stream.close();
    }

    /**
	 * Gets the data buffer, which contains both header and pixel data. This is
	 * the raw data used to create this image. For the cooked data, call
	 * writeHeader() followed by writePixels().
	 * @param buf Shared exposed data buffer. Never null.
	 */
    public ByteBuffer getBuffer() {
        _buf.rewind();
        return _buf;
    }

    /**
	 * Writes the header of this image to the specified byte buffer.
	 * @param buf Buffer object to write to
	 */
    public void writeHeader(ByteBuffer buf) {
        _header.write(buf);
    }

    /**
	 * Writes the pixels of this image to the specified byte buffer.
	 * @param buf Buffer object to write to
	 */
    public void writePixels(ByteBuffer buf) {
        _buf.position(DdsHeader.writtenSize());
        buf.put(_buf);
    }

    /**
	 * Test for presence/absence of surface description flags (DDSD_*)
	 * @param flag DDSD_* flags set to test
	 * @return true if flag present or false otherwise
	 */
    public boolean isSurfaceDescFlagSet(int flag) {
        return ((_header.flags & flag) != 0);
    }

    /** Test for presence/absence of pixel format flags (DDPF_*) */
    public boolean isPixelFormatFlagSet(int flag) {
        return ((_header.pfFlags & flag) != 0);
    }

    /**
	 * Gets the pixel format of this texture (D3DFMT_*) based on some
	 * heuristics. Returns D3DFMT_UNKNOWN if could not recognize the pixel
	 * format.
	 */
    public int getPixelFormat() {
        if (isCompressed()) {
            return getCompressionFormat();
        } else if (isPixelFormatFlagSet(DDPF_RGB)) {
            if (isPixelFormatFlagSet(DDPF_ALPHAPIXELS)) {
                if (getDepth() == 32 && _header.pfRBitMask == 0x00FF0000 && _header.pfGBitMask == 0x0000FF00 && _header.pfBBitMask == 0x000000FF && _header.pfABitMask == 0xFF000000) {
                    return D3DFMT_A8R8G8B8;
                }
            } else {
                if (getDepth() == 24 && _header.pfRBitMask == 0x00FF0000 && _header.pfGBitMask == 0x0000FF00 && _header.pfBBitMask == 0x000000FF) {
                    return D3DFMT_R8G8B8;
                } else if (getDepth() == 32 && _header.pfRBitMask == 0x00FF0000 && _header.pfGBitMask == 0x0000FF00 && _header.pfBBitMask == 0x000000FF) {
                    return D3DFMT_X8R8G8B8;
                }
            }
        }
        return D3DFMT_UNKNOWN;
    }

    /**
	 * Indicates whether this texture is cubemap
	 * @return true if cubemap or false otherwise
	 */
    public boolean isCubemap() {
        return ((_header.ddsCaps1 & DDSCAPS_COMPLEX) != 0) && ((_header.ddsCaps2 & DDSCAPS2_CUBEMAP) != 0);
    }

    /**
	 * Indicates whethe this cubemap side present
	 * @param side Side to test
	 * @return true if side present or false otherwise
	 */
    public boolean isCubemapSidePresent(int side) {
        return isCubemap() && (_header.ddsCaps2 & side) != 0;
    }

    /** Indicates whether this texture is compressed. */
    public boolean isCompressed() {
        return (isPixelFormatFlagSet(DDPF_FOURCC));
    }

    /**
	 * If this surface is compressed, returns the kind of compression used
	 * (DXT1..DXT5).
	 */
    public int getCompressionFormat() {
        return _header.pfFourCC;
    }

    /**
	 * Width of the texture (or the top-most mipmap if mipmaps are present)
	 */
    public int getWidth() {
        return _header.width;
    }

    /**
	 * Height of the texture (or the top-most mipmap if mipmaps are present)
	 */
    public int getHeight() {
        return _header.height;
    }

    /**
	 * Total number of bits per pixel. Only valid if DDPF_RGB is present. For
	 * A8R8G8B8, would be 32.
	 */
    public int getDepth() {
        return _header.pfRGBBitCount;
    }

    /** Number of mip maps in the texture */
    public int getNumMipMaps() {
        if (!isSurfaceDescFlagSet(DDSD_MIPMAPCOUNT)) {
            return 0;
        }
        return _header.mipMapCountOrAux;
    }

    /**
	 * Gets the <i>i</i>th mipmap data (0..getNumMipMaps() - 1)
	 * @param map Mipmap index
	 * @return Image object
	 */
    public ImageInfo getMipMap(int map) {
        return getMipMap(0, map);
    }

    /**
	 * Gets the <i>i</i>th mipmap data (0..getNumMipMaps() - 1)
	 * @param side Cubemap side or 0 for 2D texture
	 * @param map Mipmap index
	 * @return Image object
	 */
    public ImageInfo getMipMap(int side, int map) {
        if (!isCubemap() && (side != 0)) {
            throw new RuntimeException("Illegal side for 2D texture: " + side);
        }
        if (isCubemap() && !isCubemapSidePresent(side)) {
            throw new RuntimeException("Illegal side, side not present: " + side);
        }
        if (getNumMipMaps() > 0 && ((map < 0) || (map >= getNumMipMaps()))) {
            throw new RuntimeException("Illegal mipmap number " + map + " (0.." + (getNumMipMaps() - 1) + ")");
        }
        int seek = DdsHeader.writtenSize();
        if (isCubemap()) {
            seek += sideShiftInBytes(side);
        }
        for (int i = 0; i < map; i++) {
            seek += mipMapSizeInBytes(i);
        }
        _buf.limit(seek + mipMapSizeInBytes(map));
        _buf.position(seek);
        ByteBuffer next = _buf.slice();
        _buf.position(0);
        _buf.limit(_buf.capacity());
        return new ImageInfo(next, mipMapWidth(map), mipMapHeight(map), isCompressed(), getCompressionFormat());
    }

    /**
	 * Returns an array of ImageInfos corresponding to all mipmap levels of this
	 * DDS file.
	 * @return Mipmap image objects set
	 */
    public ImageInfo[] getAllMipMaps() {
        return getAllMipMaps(0);
    }

    /**
	 * Returns an array of ImageInfos corresponding to all mipmap levels of this
	 * DDS file.
	 * @param side Cubemap side or 0 for 2D texture
	 * @return Mipmap image objects set
	 */
    public ImageInfo[] getAllMipMaps(int side) {
        int numLevels = getNumMipMaps();
        if (numLevels == 0) {
            numLevels = 1;
        }
        ImageInfo[] result = new ImageInfo[numLevels];
        for (int i = 0; i < numLevels; i++) {
            result[i] = getMipMap(side, i);
        }
        return result;
    }

    /**
	 * Converts e.g. DXT1 compression format constant (see
	 * {@link #getCompressionFormat}) into "DXT1".
	 * @param compressionFormat Compression format constant
	 * @return String format code
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

    /**
	 * Allocates a temporary, empty ByteBuffer suitable for use in a call to
	 * glCompressedTexImage2D. This is used by the Texture class to expand
	 * non-power-of-two DDS compressed textures to power-of-two sizes on
	 * hardware not supporting OpenGL 2.0 and the NPOT texture extension. The
	 * specified OpenGL internal format must be one of
	 * GL_COMPRESSED_RGB_S3TC_DXT1_EXT, GL_COMPRESSED_RGBA_S3TC_DXT1_EXT,
	 * GL_COMPRESSED_RGBA_S3TC_DXT3_EXT, or GL_COMPRESSED_RGBA_S3TC_DXT5_EXT.
	 */
    public static ByteBuffer allocateBlankBuffer(int width, int height, int openGLInternalFormat) {
        int size = width * height;
        switch(openGLInternalFormat) {
            case GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT:
            case GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT:
                size /= 2;
                break;
            case GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT:
            case GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT:
                break;
            default:
                throw new IllegalArgumentException("Illegal OpenGL texture internal format " + openGLInternalFormat);
        }
        if (size == 0) size = 1;
        return BufferUtil.newByteBuffer(size);
    }

    public void debugPrint() {
        PrintStream tty = System.err;
        tty.println("Compressed texture: " + isCompressed());
        if (isCompressed()) {
            int fmt = getCompressionFormat();
            String name = getCompressionFormatName(fmt);
            tty.println("Compression format: 0x" + Integer.toHexString(fmt) + " (" + name + ")");
        }
        tty.println("Width: " + _header.width + " Height: " + _header.height);
        tty.println("header.pitchOrLinearSize: " + _header.pitchOrLinearSize);
        tty.println("header.pfRBitMask: 0x" + Integer.toHexString(_header.pfRBitMask));
        tty.println("header.pfGBitMask: 0x" + Integer.toHexString(_header.pfGBitMask));
        tty.println("header.pfBBitMask: 0x" + Integer.toHexString(_header.pfBBitMask));
        tty.println("SurfaceDesc flags:");
        boolean recognizedAny = false;
        recognizedAny |= printIfRecognized(tty, _header.flags, DDSD_CAPS, "DDSD_CAPS");
        recognizedAny |= printIfRecognized(tty, _header.flags, DDSD_HEIGHT, "DDSD_HEIGHT");
        recognizedAny |= printIfRecognized(tty, _header.flags, DDSD_WIDTH, "DDSD_WIDTH");
        recognizedAny |= printIfRecognized(tty, _header.flags, DDSD_PITCH, "DDSD_PITCH");
        recognizedAny |= printIfRecognized(tty, _header.flags, DDSD_BACKBUFFERCOUNT, "DDSD_BACKBUFFERCOUNT");
        recognizedAny |= printIfRecognized(tty, _header.flags, DDSD_ZBUFFERBITDEPTH, "DDSD_ZBUFFERBITDEPTH");
        recognizedAny |= printIfRecognized(tty, _header.flags, DDSD_ALPHABITDEPTH, "DDSD_ALPHABITDEPTH");
        recognizedAny |= printIfRecognized(tty, _header.flags, DDSD_LPSURFACE, "DDSD_LPSURFACE");
        recognizedAny |= printIfRecognized(tty, _header.flags, DDSD_PIXELFORMAT, "DDSD_PIXELFORMAT");
        recognizedAny |= printIfRecognized(tty, _header.flags, DDSD_MIPMAPCOUNT, "DDSD_MIPMAPCOUNT");
        recognizedAny |= printIfRecognized(tty, _header.flags, DDSD_LINEARSIZE, "DDSD_LINEARSIZE");
        recognizedAny |= printIfRecognized(tty, _header.flags, DDSD_DEPTH, "DDSD_DEPTH");
        if (!recognizedAny) {
            tty.println("(none)");
        }
        tty.println("Raw SurfaceDesc flags: 0x" + Integer.toHexString(_header.flags));
        tty.println("Pixel format flags:");
        recognizedAny = false;
        recognizedAny |= printIfRecognized(tty, _header.pfFlags, DDPF_ALPHAPIXELS, "DDPF_ALPHAPIXELS");
        recognizedAny |= printIfRecognized(tty, _header.pfFlags, DDPF_ALPHA, "DDPF_ALPHA");
        recognizedAny |= printIfRecognized(tty, _header.pfFlags, DDPF_FOURCC, "DDPF_FOURCC");
        recognizedAny |= printIfRecognized(tty, _header.pfFlags, DDPF_PALETTEINDEXED4, "DDPF_PALETTEINDEXED4");
        recognizedAny |= printIfRecognized(tty, _header.pfFlags, DDPF_PALETTEINDEXEDTO8, "DDPF_PALETTEINDEXEDTO8");
        recognizedAny |= printIfRecognized(tty, _header.pfFlags, DDPF_PALETTEINDEXED8, "DDPF_PALETTEINDEXED8");
        recognizedAny |= printIfRecognized(tty, _header.pfFlags, DDPF_RGB, "DDPF_RGB");
        recognizedAny |= printIfRecognized(tty, _header.pfFlags, DDPF_COMPRESSED, "DDPF_COMPRESSED");
        recognizedAny |= printIfRecognized(tty, _header.pfFlags, DDPF_RGBTOYUV, "DDPF_RGBTOYUV");
        recognizedAny |= printIfRecognized(tty, _header.pfFlags, DDPF_YUV, "DDPF_YUV");
        recognizedAny |= printIfRecognized(tty, _header.pfFlags, DDPF_ZBUFFER, "DDPF_ZBUFFER");
        recognizedAny |= printIfRecognized(tty, _header.pfFlags, DDPF_PALETTEINDEXED1, "DDPF_PALETTEINDEXED1");
        recognizedAny |= printIfRecognized(tty, _header.pfFlags, DDPF_PALETTEINDEXED2, "DDPF_PALETTEINDEXED2");
        recognizedAny |= printIfRecognized(tty, _header.pfFlags, DDPF_ZPIXELS, "DDPF_ZPIXELS");
        if (!recognizedAny) {
            tty.println("(none)");
        }
        tty.println("Raw pixel format flags: 0x" + Integer.toHexString(_header.pfFlags));
        tty.println("Depth: " + getDepth());
        tty.println("Number of mip maps: " + getNumMipMaps());
        int fmt = getPixelFormat();
        tty.print("Pixel format: ");
        switch(fmt) {
            case D3DFMT_R8G8B8:
                tty.println("D3DFMT_R8G8B8");
                break;
            case D3DFMT_A8R8G8B8:
                tty.println("D3DFMT_A8R8G8B8");
                break;
            case D3DFMT_X8R8G8B8:
                tty.println("D3DFMT_X8R8G8B8");
                break;
            case D3DFMT_DXT1:
                tty.println("D3DFMT_DXT1");
                break;
            case D3DFMT_DXT2:
                tty.println("D3DFMT_DXT2");
                break;
            case D3DFMT_DXT3:
                tty.println("D3DFMT_DXT3");
                break;
            case D3DFMT_DXT4:
                tty.println("D3DFMT_DXT4");
                break;
            case D3DFMT_DXT5:
                tty.println("D3DFMT_DXT5");
                break;
            case D3DFMT_UNKNOWN:
                tty.println("D3DFMT_UNKNOWN");
                break;
            default:
                tty.println("(unknown pixel format " + fmt + ")");
                break;
        }
    }

    private DdsImage() {
    }

    private void readFromFile(File file) throws IOException {
        _fis = new FileInputStream(file);
        _chan = _fis.getChannel();
        ByteBuffer buf = _chan.map(FileChannel.MapMode.READ_ONLY, 0, (int) file.length());
        readFromBuffer(buf);
    }

    private void readFromBuffer(ByteBuffer buf) throws IOException {
        this._buf = buf;
        buf.order(ByteOrder.LITTLE_ENDIAN);
        _header = new DdsHeader();
        _header.read(buf);
        fixupHeader();
    }

    private void initFromData(int d3dFormat, int width, int height, ByteBuffer[] mipmapData) throws IllegalArgumentException {
        int topmostMipmapSize = width * height;
        int pitchOrLinearSize = width;
        boolean isCompressed = false;
        switch(d3dFormat) {
            case D3DFMT_R8G8B8:
                topmostMipmapSize *= 3;
                pitchOrLinearSize *= 3;
                break;
            case D3DFMT_A8R8G8B8:
                topmostMipmapSize *= 4;
                pitchOrLinearSize *= 4;
                break;
            case D3DFMT_X8R8G8B8:
                topmostMipmapSize *= 4;
                pitchOrLinearSize *= 4;
                break;
            case D3DFMT_DXT1:
            case D3DFMT_DXT2:
            case D3DFMT_DXT3:
            case D3DFMT_DXT4:
            case D3DFMT_DXT5:
                topmostMipmapSize = computeCompressedBlockSize(width, height, 1, d3dFormat);
                pitchOrLinearSize = topmostMipmapSize;
                isCompressed = true;
                break;
            default:
                throw new IllegalArgumentException("d3dFormat must be one of the known formats");
        }
        int curSize = topmostMipmapSize;
        int totalSize = 0;
        for (int i = 0; i < mipmapData.length; i++) {
            if (mipmapData[i].remaining() != curSize) {
                throw new IllegalArgumentException("Mipmap level " + i + " didn't match expected data size (expected " + curSize + ", got " + mipmapData[i].remaining() + ")");
            }
            curSize /= 4;
            totalSize += mipmapData[i].remaining();
        }
        totalSize += DdsHeader.writtenSize();
        ByteBuffer buf = ByteBuffer.allocate(totalSize);
        buf.position(DdsHeader.writtenSize());
        for (int i = 0; i < mipmapData.length; i++) {
            buf.put(mipmapData[i]);
        }
        this._buf = buf;
        _header = new DdsHeader();
        _header.size = DdsHeader.size();
        _header.flags = DDSD_CAPS | DDSD_HEIGHT | DDSD_WIDTH | DDSD_PIXELFORMAT;
        if (mipmapData.length > 1) {
            _header.flags |= DDSD_MIPMAPCOUNT;
            _header.mipMapCountOrAux = mipmapData.length;
        }
        _header.width = width;
        _header.height = height;
        if (isCompressed) {
            _header.flags |= DDSD_LINEARSIZE;
            _header.pfFlags |= DDPF_FOURCC;
            _header.pfFourCC = d3dFormat;
        } else {
            _header.flags |= DDSD_PITCH;
            _header.pfFlags |= DDPF_RGB;
            switch(d3dFormat) {
                case D3DFMT_R8G8B8:
                    _header.pfRGBBitCount = 24;
                    break;
                case D3DFMT_A8R8G8B8:
                    _header.pfRGBBitCount = 32;
                    _header.pfFlags |= DDPF_ALPHAPIXELS;
                    break;
                case D3DFMT_X8R8G8B8:
                    _header.pfRGBBitCount = 32;
                    break;
            }
            _header.pfRBitMask = 0x00FF0000;
            _header.pfGBitMask = 0x0000FF00;
            _header.pfBBitMask = 0x000000FF;
            if (d3dFormat == D3DFMT_A8R8G8B8) {
                _header.pfABitMask = 0xFF000000;
            }
        }
        _header.pitchOrLinearSize = pitchOrLinearSize;
        _header.pfSize = DdsHeader.pfSize();
    }

    private void fixupHeader() {
        if (isCompressed() && !isSurfaceDescFlagSet(DDSD_LINEARSIZE)) {
            int depth = _header.backBufferCountOrDepth;
            if (depth == 0) {
                depth = 1;
            }
            _header.pitchOrLinearSize = computeCompressedBlockSize(getWidth(), getHeight(), depth, getCompressionFormat());
            _header.flags |= DDSD_LINEARSIZE;
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

    private int sideShiftInBytes(int side) {
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
        throw new RuntimeException("Illegal side: " + side);
    }

    private boolean printIfRecognized(PrintStream tty, int flags, int flag, String what) {
        if ((flags & flag) != 0) {
            tty.println(what);
            return true;
        }
        return false;
    }
}

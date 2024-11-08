package com.sun.opengl.util.texture.spi;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import javax.media.opengl.*;
import com.sun.opengl.util.*;

/**
 * Targa image reader and writer adapted from sources of the <a href =
 * "http://java.sun.com/products/jimi/">Jimi</a> image I/O class library.
 *
 * <P>
 *
 * Image decoder for image data stored in TGA file format.
 * Currently only the original TGA file format is supported. This is
 * because the new TGA format has data at the end of the file, getting
 * to the end of a file in an InputStream orient environment presents
 * several difficulties which are avoided at the moment.
 *
 * <P>
 *
 * This is a simple decoder and is only setup to load a single image
 * from the input stream
 *
 * <P>
 *
 * @author    Robin Luiten
 * @author    Kenneth Russell
 * @version    $Revision: 1.1 $
 */
public class TGAImage {

    private Header header;

    private int format;

    private ByteBuffer data;

    private TGAImage(Header header) {
        this.header = header;
    }

    /**
   * This class reads in all of the TGA image header in addition it also
   * reads in the imageID field as it is convenient to handle that here.
   *
   * @author    Robin Luiten
   * @version   1.1
   */
    public static class Header {

        /** Set of possible file format TGA types */
        public static final int TYPE_NEW = 0;

        public static final int TYPE_OLD = 1;

        public static final int TYPE_UNK = 2;

        /**  Set of possible image types in TGA file */
        public static final int NO_IMAGE = 0;

        public static final int UCOLORMAPPED = 1;

        public static final int UTRUECOLOR = 2;

        public static final int UBLACKWHITE = 3;

        public static final int COLORMAPPED = 9;

        public static final int TRUECOLOR = 10;

        public static final int BLACKWHITE = 11;

        /** Field image descriptor bitfield values definitions */
        public static final int ID_ATTRIBPERPIXEL = 0xF;

        public static final int ID_RIGHTTOLEFT = 0x10;

        public static final int ID_TOPTOBOTTOM = 0x20;

        public static final int ID_INTERLEAVE = 0xC0;

        /** Field image descriptor / interleave values */
        public static final int I_NOTINTERLEAVED = 0;

        public static final int I_TWOWAY = 1;

        public static final int I_FOURWAY = 2;

        /** Type of this TGA file format */
        private int tgaType;

        /** initial TGA image data fields */
        private int idLength;

        private int colorMapType;

        private int imageType;

        /** TGA image colour map fields */
        private int firstEntryIndex;

        private int colorMapLength;

        private byte colorMapEntrySize;

        /** TGA image specification fields */
        private int xOrigin;

        private int yOrigin;

        private int width;

        private int height;

        private byte pixelDepth;

        private byte imageDescriptor;

        private byte[] imageIDbuf;

        private String imageID;

        Header() {
            tgaType = TYPE_OLD;
        }

        Header(LEDataInputStream in) throws IOException {
            int ret;
            tgaType = TYPE_OLD;
            idLength = in.readUnsignedByte();
            colorMapType = in.readUnsignedByte();
            imageType = in.readUnsignedByte();
            firstEntryIndex = in.readUnsignedShort();
            colorMapLength = in.readUnsignedShort();
            colorMapEntrySize = in.readByte();
            xOrigin = in.readUnsignedShort();
            yOrigin = in.readUnsignedShort();
            width = in.readUnsignedShort();
            height = in.readUnsignedShort();
            pixelDepth = in.readByte();
            imageDescriptor = in.readByte();
            if (idLength > 0) {
                imageIDbuf = new byte[idLength];
                in.read(imageIDbuf, 0, idLength);
                imageID = new String(imageIDbuf, "US-ASCII");
            }
        }

        public int tgaType() {
            return tgaType;
        }

        /** initial TGA image data fields */
        public int idLength() {
            return idLength;
        }

        public int colorMapType() {
            return colorMapType;
        }

        public int imageType() {
            return imageType;
        }

        /** TGA image colour map fields */
        public int firstEntryIndex() {
            return firstEntryIndex;
        }

        public int colorMapLength() {
            return colorMapLength;
        }

        public byte colorMapEntrySize() {
            return colorMapEntrySize;
        }

        /** TGA image specification fields */
        public int xOrigin() {
            return xOrigin;
        }

        public int yOrigin() {
            return yOrigin;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public byte pixelDepth() {
            return pixelDepth;
        }

        public byte imageDescriptor() {
            return imageDescriptor;
        }

        /** bitfields in imageDescriptor */
        public byte attribPerPixel() {
            return (byte) (imageDescriptor & ID_ATTRIBPERPIXEL);
        }

        public boolean rightToLeft() {
            return ((imageDescriptor & ID_RIGHTTOLEFT) != 0);
        }

        public boolean topToBottom() {
            return ((imageDescriptor & ID_TOPTOBOTTOM) != 0);
        }

        public byte interleave() {
            return (byte) ((imageDescriptor & ID_INTERLEAVE) >> 6);
        }

        public byte[] imageIDbuf() {
            return imageIDbuf;
        }

        public String imageID() {
            return imageID;
        }

        public String toString() {
            return "TGA Header " + " id length: " + idLength + " color map type: " + colorMapType + " image type: " + imageType + " first entry index: " + firstEntryIndex + " color map length: " + colorMapLength + " color map entry size: " + colorMapEntrySize + " x Origin: " + xOrigin + " y Origin: " + yOrigin + " width: " + width + " height: " + height + " pixel depth: " + pixelDepth + " image descriptor: " + imageDescriptor + (imageIDbuf == null ? "" : (" ID String: " + imageID));
        }

        public int size() {
            return 18 + idLength;
        }

        private void write(ByteBuffer buf) {
            buf.put((byte) idLength);
            buf.put((byte) colorMapType);
            buf.put((byte) imageType);
            buf.putShort((short) firstEntryIndex);
            buf.putShort((short) colorMapLength);
            buf.put((byte) colorMapEntrySize);
            buf.putShort((short) xOrigin);
            buf.putShort((short) yOrigin);
            buf.putShort((short) width);
            buf.putShort((short) height);
            buf.put((byte) pixelDepth);
            buf.put((byte) imageDescriptor);
            if (idLength > 0) {
                try {
                    byte[] chars = imageID.getBytes("US-ASCII");
                    buf.put(chars);
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
   * Identifies the image type of the tga image data and loads
   * it into the JimiImage structure. This was taken from the
   * prototype and modified for the new Jimi structure
   */
    private void decodeImage(LEDataInputStream dIn) throws IOException {
        switch(header.imageType()) {
            case Header.UCOLORMAPPED:
                throw new IOException("TGADecoder Uncompressed Colormapped images not supported");
            case Header.UTRUECOLOR:
                switch(header.pixelDepth) {
                    case 16:
                        throw new IOException("TGADecoder Compressed 16-bit True Color images not supported");
                    case 24:
                    case 32:
                        decodeRGBImageU24_32(dIn);
                        break;
                }
                break;
            case Header.UBLACKWHITE:
                throw new IOException("TGADecoder Uncompressed Grayscale images not supported");
            case Header.COLORMAPPED:
                throw new IOException("TGADecoder Compressed Colormapped images not supported");
            case Header.TRUECOLOR:
                throw new IOException("TGADecoder Compressed True Color images not supported");
            case Header.BLACKWHITE:
                throw new IOException("TGADecoder Compressed Grayscale images not supported");
        }
    }

    /**
   * This assumes that the body is for a 24 bit or 32 bit for a
   * RGB or ARGB image respectively.
   */
    private void decodeRGBImageU24_32(LEDataInputStream dIn) throws IOException {
        int i;
        int j;
        int y;
        int raw;
        int rawWidth = header.width() * (header.pixelDepth() / 8);
        byte[] rawBuf = new byte[rawWidth];
        byte[] tmpData = new byte[rawWidth * header.height()];
        if (header.pixelDepth() == 24) {
            format = GL.GL_BGR;
        } else {
            assert header.pixelDepth() == 32;
            format = GL.GL_BGRA;
        }
        for (i = 0; i < header.height(); ++i) {
            dIn.readFully(rawBuf, 0, rawWidth);
            if (header.topToBottom()) y = header.height - i - 1; else y = i;
            System.arraycopy(rawBuf, 0, tmpData, y * rawWidth, rawBuf.length);
        }
        data = ByteBuffer.wrap(tmpData);
    }

    /** Returns the width of the image. */
    public int getWidth() {
        return header.width();
    }

    /** Returns the height of the image. */
    public int getHeight() {
        return header.height();
    }

    /** Returns the OpenGL format for this texture; e.g. GL.GL_BGR or GL.GL_BGRA. */
    public int getGLFormat() {
        return format;
    }

    /** Returns the raw data for this texture in the correct
      (bottom-to-top) order for calls to glTexImage2D. */
    public ByteBuffer getData() {
        return data;
    }

    /** Reads a Targa image from the specified file. */
    public static TGAImage read(String filename) throws IOException {
        return read(new FileInputStream(filename));
    }

    /** Reads a Targa image from the specified InputStream. */
    public static TGAImage read(InputStream in) throws IOException {
        LEDataInputStream dIn = new LEDataInputStream(new BufferedInputStream(in));
        Header header = new Header(dIn);
        TGAImage res = new TGAImage(header);
        res.decodeImage(dIn);
        return res;
    }

    /** Writes the image in Targa format to the specified file name. */
    public void write(String filename) throws IOException {
        write(new File(filename));
    }

    /** Writes the image in Targa format to the specified file. */
    public void write(File file) throws IOException {
        FileOutputStream stream = new FileOutputStream(file);
        FileChannel chan = stream.getChannel();
        ByteBuffer buf = ByteBuffer.allocate(header.size());
        buf.order(ByteOrder.LITTLE_ENDIAN);
        header.write(buf);
        buf.rewind();
        chan.write(buf);
        chan.write(data);
        data.rewind();
        chan.force(true);
        chan.close();
        stream.close();
    }

    /** Creates a TGAImage from data supplied by the end user. Shares
      data with the passed ByteBuffer. Assumes the data is already in
      the correct byte order for writing to disk, i.e., BGR or
      BGRA. */
    public static TGAImage createFromData(int width, int height, boolean hasAlpha, boolean topToBottom, ByteBuffer data) {
        Header header = new Header();
        header.imageType = Header.UTRUECOLOR;
        header.width = width;
        header.height = height;
        header.pixelDepth = (byte) (hasAlpha ? 32 : 24);
        header.imageDescriptor = (byte) (topToBottom ? Header.ID_TOPTOBOTTOM : 0);
        TGAImage ret = new TGAImage(header);
        ret.data = data;
        return ret;
    }
}

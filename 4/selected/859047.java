package com.bix.util.blizfiles.blp;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.apache.log4j.Logger;
import com.bix.util.blizfiles.BufferUtils;
import com.bix.util.blizfiles.dds.DXTCFormat;

/**
 * This class encapsulates information about a BLP file (Blizzard's graphic
 * image format). The decode logic was pulled from another piece of code, though
 * I've cleaned it up immensely.
 * 
 * BLP2 Format 
 * 	Type 1 Encoding 1 AlphaDepth 0 (uncompressed paletted image with no alpha) 
 * 	Type 1 Encoding 1 AlphaDepth 1 (uncompressed paletted image with 1-bit alpha) 
 * 	Type 1 Encoding 1 AlphaDepth 8 (uncompressed paletted image with 8-bit alpha) 
 * 	Type 1 Encoding 2 AlphaDepth 0 (DXT1 no alpha) 
 * 	Type 1 Encoding 2 AlphaDepth 1 (DXT1 one bit alpha) 
 * 	Type 1 Encoding 2 AlphaDepth 8 AlphaEncoding 1(DXT3) 
 * 	Type 1 Encoding 2 AlphaDepth 8 AlphaEncoding 7(DXT5)
 * 
 * Not all of the image formats have been implemented and, unfortunately, some
 * of the images formats are completely untested.  I'm fairly sure DXT1 and
 * DXT2 probably work.  Since DXT2 is based on DXT3, it's likely that DXT3
 * doesn't work because of the lack of handling of the "premultiplication" of
 * the alpha channels.
 * 
 * @author squid
 * 
 */
public class BLPFile {

    private static final Logger log = Logger.getLogger(BLPFile.class.getName());

    /**
	 * This is the header of the BLP file.
	 * 
	 * @author squid
	 */
    public class BLPHeader {

        private byte[] magic;

        private int type;

        private byte compression;

        private byte alphaChannelBitDepth;

        private byte alphaCompression;

        private byte mipMapLevel;

        private int xResolution;

        private int yResolution;

        private int[] mipMapOffset;

        private int[] mipMapSizes;

        /**
		 * Instantiate the BLP header from a byte buffer.  The byte buffer should
		 * have been set to the correct endian format prior to calling this.
		 * 
		 * @param bb	The byte buffer to read the header from.
		 */
        public BLPHeader(ByteBuffer bb) {
            read(bb);
        }

        /**
		 * Read the BLP header from a byte buffer.  The byte buffer should have
		 * been set to the correct endian format prior to calling this.
		 * 
		 * @param bb	The byte buffer to read the header from.
		 */
        public void read(ByteBuffer bb) {
            this.magic = BufferUtils.getByteArray(bb, 4);
            this.type = bb.getInt();
            this.compression = bb.get();
            this.alphaChannelBitDepth = bb.get();
            this.alphaCompression = bb.get();
            this.mipMapLevel = bb.get();
            this.xResolution = bb.getInt();
            this.yResolution = bb.getInt();
            this.mipMapOffset = BufferUtils.getIntArray(bb, 16);
            this.mipMapSizes = BufferUtils.getIntArray(bb, 16);
        }

        /**
		 * @return the magic
		 */
        public byte[] getMagic() {
            return magic;
        }

        /**
		 * @return the version
		 */
        public int getType() {
            return type;
        }

        /**
		 * @return the compression
		 */
        public byte getCompression() {
            return compression;
        }

        /**
		 * @return the alphaChannelBitDepth
		 */
        public byte getAlphaChannelBitDepth() {
            return alphaChannelBitDepth;
        }

        /**
		 * @return the alphaCompression
		 */
        public byte getAlphaCompression() {
            return alphaCompression;
        }

        /**
		 * @return the mipMapLevel
		 */
        public byte getMipMapLevel() {
            return mipMapLevel;
        }

        /**
		 * @return the xResolution
		 */
        public int getXResolution() {
            return xResolution;
        }

        /**
		 * @return the yResolution
		 */
        public int getYResolution() {
            return yResolution;
        }

        /**
		 * @return the mipMapOffset
		 */
        public int[] getMipMapOffset() {
            return mipMapOffset;
        }

        /**
		 * @return the mipMapSizes
		 */
        public int[] getMipMapSizes() {
            return mipMapSizes;
        }
    }

    public final int ENCODING_TYPE_PALETTE = 1;

    public final int ENCODING_TYPE_RGB = 2;

    private BLPHeader header;

    private int[] bitmap;

    private byte[] alphaChannel;

    private boolean bitmapHasAlpha = false;

    private byte[] palette;

    public BLPFile() {
    }

    /**
	 * Instantiates the BLPFile object from a File.
	 * 
	 * @param f	The file to instnatiate the BLPFile from.
	 * 
	 * @throws IOException
	 * @throws BLPFileException
	 */
    public BLPFile(File f) throws IOException, BLPFileException {
        FileChannel fc = new FileInputStream(f).getChannel();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        bb.order(ByteOrder.LITTLE_ENDIAN);
        read(bb);
    }

    /**
	 * Loads a BLP2 image from a stream.
	 * 
	 * @param is	Loads a BLP file from an input stream.
	 * 
	 * @throws IOException
	 * @throws BLPFileException
	 */
    public void read(InputStream is) throws IOException, BLPFileException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        byte[] chunk = new byte[1024];
        int bytesRead;
        while ((bytesRead = is.read(chunk)) != -1) {
            baos.write(chunk, 0, bytesRead);
        }
        chunk = baos.toByteArray();
        log.debug("Read " + chunk.length + " bytes from the stream.");
        read(ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN));
    }

    /**
	 * Load the object from a ByteBuffer.
	 * 
	 * @param bb
	 *          The byte buffer to load the object from.
	 * 
	 * @throws IOException
	 * @throws BLPFileException
	 */
    public void read(ByteBuffer bb) throws IOException, BLPFileException {
        this.header = new BLPHeader(bb);
        verifyEncoding();
        int imageDataStart = this.header.getMipMapOffset()[0];
        int imageDataSize = this.header.getMipMapSizes()[0];
        int width = this.header.getXResolution();
        int height = this.header.getYResolution();
        log.debug("  compression: [0x" + Integer.toHexString(this.header.getCompression()) + "]");
        log.debug("  alpha channel bit depth[" + this.header.getAlphaChannelBitDepth() + "]");
        if (this.header.getCompression() == ENCODING_TYPE_PALETTE) {
            if (this.header.getMipMapLevel() == 0) {
                throw new BLPFileException("Critical Error: dataFormat == 1 && mipMapLevel = " + this.header.getMipMapLevel());
            }
            bb.position(148);
            palette = new byte[1024];
            bb.get(palette);
            if (this.header.getAlphaChannelBitDepth() == 0) {
                if (width * height != imageDataSize) {
                    throw new BLPFileException("Critical Error: imageSize = " + width * height + ", mipmapSize = " + imageDataSize);
                }
                bb.position(imageDataStart);
                byte abyte1[] = new byte[imageDataSize];
                bb.get(abyte1);
                bitmap = new int[width * height];
                System.arraycopy(abyte1, 0, bitmap, 0, width * height);
            } else if (this.header.getCompression() == 8) {
                bitmapHasAlpha = true;
                if (width * height * 2 != imageDataSize) {
                    throw new BLPFileException("Critical Error: alpha imageSize = " + width * height + ", mipmapSize = " + imageDataSize);
                }
                bb.position(imageDataStart);
                byte abyte2[] = new byte[imageDataSize / 2];
                bb.get(abyte2);
                bitmap = new int[width * height];
                System.arraycopy(abyte2, 0, bitmap, 0, width * height);
                bb.position(imageDataStart + imageDataSize / 2);
                alphaChannel = new byte[imageDataSize / 2];
                bb.get(alphaChannel);
            }
        } else if (this.header.getCompression() == ENCODING_TYPE_RGB) {
            bb.position(imageDataStart);
            if (this.header.getAlphaChannelBitDepth() == 0 || this.header.getAlphaChannelBitDepth() == 1) {
                bitmapHasAlpha = false;
                if ((width * height) / 2 != imageDataSize) {
                    throw new BLPFileException("Critical Error: imageSize = " + width * height + ", mipmapSize = " + imageDataSize + ", maybe not DXT1");
                }
                log.debug("Decoding DXT1");
                bitmap = DXTCFormat.decodeDXT1(bb, width, height);
            } else if (this.header.getAlphaChannelBitDepth() == 8) {
                bitmapHasAlpha = true;
                if (width * height != imageDataSize) {
                    throw new BLPFileException("Critical Error: imageSize = " + this.header.getXResolution() * this.header.getYResolution() + ", mipmapSize = " + imageDataSize + ", maybe not DXT1");
                }
                log.debug("Decoding DXT2");
                bitmap = DXTCFormat.decodeDXT2(bb, width, height);
            } else {
                bitmapHasAlpha = true;
                if (width * height != imageDataSize) {
                    throw new BLPFileException("Critical Error: imageSize = " + this.header.getXResolution() * this.header.getYResolution() + ", mipmapSize = " + imageDataSize + ", maybe not DXT1");
                }
                log.debug("Decoding DXT5");
                bitmap = DXTCFormat.decodeDXT5(bb, width, height);
            }
        }
    }

    /**
	 * Verify that the encoding is valid and that we understand how to process the
	 * type of file.
	 * 
	 * @throws BLPFileException
	 */
    private void verifyEncoding() throws BLPFileException {
        if (this.header.getCompression() == ENCODING_TYPE_PALETTE) {
            if (this.header.getAlphaCompression() != 8) throw new BLPFileException("Indexed bitmaps with " + this.header.getAlphaCompression() + " bpp are not supported");
        }
        if (this.header.getMipMapLevel() != 1 && this.header.getMipMapLevel() != 2 && this.header.getMipMapLevel() != 0) {
            throw new BLPFileException("Critical Error: unknown4 = " + this.header.getMipMapLevel() + " dataformat = " + this.header.getCompression());
        }
    }

    /**
	 * 
	 * 
	 * @return
	 * 
	 * @throws IOException
	 * @throws BLPFileException
	 */
    public BufferedImage getImage() throws IOException, BLPFileException {
        int width = this.header.getXResolution();
        int height = this.header.getYResolution();
        BufferedImage bufferedimage = null;
        log.debug("getImage():  size[" + this.header.getXResolution() + "," + this.header.getYResolution() + "]");
        if (this.header.getCompression() == this.ENCODING_TYPE_PALETTE) {
            byte reds[] = new byte[256];
            byte greens[] = new byte[256];
            byte blues[] = new byte[256];
            for (int l = 0; l < 256; l++) {
                blues[l] = palette[l * 4];
                greens[l] = palette[l * 4 + 1];
                reds[l] = palette[l * 4 + 2];
            }
            if (bitmapHasAlpha) {
                int i = -1;
                int j = -1;
                int i1 = 0;
                while (i1 < 256) {
                    int k1 = i1 + 1;
                    while (k1 < 256) {
                        if (reds[i1] - reds[k1] == 0 && greens[i1] - greens[k1] == 0 && blues[i1] - blues[k1] == 0) {
                            j = i1;
                            i = k1;
                            break;
                        }
                        i1++;
                    }
                    if (i != -1) {
                        break;
                    }
                    i1++;
                }
                if (i == -1) {
                    throw new BLPFileException("Couldn't find transparency color");
                }
                IndexColorModel indexcolormodel1 = new IndexColorModel(8, 256, reds, greens, blues, i);
                for (int j1 = 0; j1 < width * height; j1++) {
                    if (bitmap[j1] == i) {
                        bitmap[j1] = j;
                    }
                    if (alphaChannel[j1] >= 0 && alphaChannel[j1] < 112) {
                        bitmap[j1] = i;
                    }
                }
                bufferedimage = new BufferedImage(width, height, 13, indexcolormodel1);
            } else {
                IndexColorModel indexcolormodel = new IndexColorModel(8, 256, reds, greens, blues);
                bufferedimage = new BufferedImage(width, height, 13, indexcolormodel);
            }
        } else if (this.header.getCompression() == ENCODING_TYPE_RGB) {
            bufferedimage = new BufferedImage(width, height, bitmapHasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        }
        bufferedimage.getRaster().setPixels(0, 0, width, height, bitmap);
        return bufferedimage;
    }

    /**
	 * @return the header
	 */
    public BLPHeader getHeader() {
        return header;
    }
}

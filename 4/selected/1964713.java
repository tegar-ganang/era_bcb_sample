package org.miv.jism.tools.reader;

import org.miv.jism.core.Console;
import org.miv.jism.core.JismError;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * Reader for the analyse image file format.
 *
 * @author Guilhelm Savin
 **/
public class AnalyseReader implements JismReader {

    public static final int DT_UNKNOWN = 0x00;

    public static final int DT_BINARY = 0x01;

    public static final int DT_UNSIGNED_CHAR = 0x02;

    public static final int DT_SIGNED_SHORT = 0x04;

    public static final int DT_SIGNED_INT = 0x08;

    public static final int DT_FLOAT = 0x10;

    public static final int DT_COMPLEX = 0x20;

    public static final int DT_DOUBLE = 0x40;

    public static final int DT_RGB = 0x80;

    public static final int DT_ALL = 0xFF;

    /**
	 * Header description in analyse format.
	 *
	 * @author Guilhelm Savin
	 **/
    public static class Header {

        public int sizeOfHDR;

        public char regular;

        public int dim;

        public int width;

        public int height;

        public int depth;

        public int datatype;

        public int bitsPerPixel;

        public float[] pixdim = new float[3];

        public Header() {
        }

        public Header(String path) {
            try {
                RandomAccessFile in = new RandomAccessFile(path, "r");
                byte[] tmp = new byte[(int) in.length()];
                in.readFully(tmp);
                in.close();
                ByteBuffer buffer = ByteBuffer.wrap(tmp);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                sizeOfHDR = buffer.getInt(0);
                regular = (char) buffer.get(38);
                dim = buffer.getShort(40);
                width = buffer.getShort(42);
                height = buffer.getShort(44);
                depth = buffer.getShort(46);
                datatype = buffer.getShort(70);
                bitsPerPixel = buffer.getShort(72);
                pixdim[0] = buffer.getFloat(80);
                pixdim[1] = buffer.getFloat(84);
                pixdim[2] = buffer.getFloat(88);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Header of the image.
	 **/
    private Header hdr;

    /**
	 * File containing image.
	 **/
    private RandomAccessFile in;

    private FileChannel channel;

    private ByteBuffer buffer;

    /**
	 * Current slice.
	 **/
    private int x, y, z;

    /**
	 * Create a new reader for analyse format.
	 * Need the header file has the same prefix that the image.
	 *
	 * @param path
	 * 	image path
	 **/
    public AnalyseReader(String path) {
        this.hdr = new Header(path.substring(0, path.lastIndexOf(".")) + ".hdr");
        init(path);
    }

    /**
	 * Init the reader.
	 **/
    protected void init(String path) {
        in = null;
        try {
            in = new RandomAccessFile(path, "r");
            channel = in.getChannel();
            channel.position(0);
        } catch (Exception e) {
            Console.printError("unable to read \"%s\"", path);
            return;
        }
        x = y = z = 0;
        switch(hdr.datatype) {
            case DT_SIGNED_SHORT:
                buffer = ByteBuffer.allocate(2);
                break;
            case DT_SIGNED_INT:
                buffer = ByteBuffer.allocate(4);
                break;
            case DT_UNSIGNED_CHAR:
                buffer = ByteBuffer.allocate(1);
                break;
            default:
                throw new JismError("unsupported data type");
        }
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(0);
    }

    /**
	 * End of file.
	 *
	 * @return true is there is no pixel anymore
	 **/
    public boolean eof() {
        return z >= hdr.depth;
    }

    /**
	 * Read another pixel.
	 *
	 * @return new pixel or null if there is no pixel anymore
	 **/
    public void read(int[] pdata) throws IOException {
        if (eof()) {
            return;
        }
        pdata[0] = x;
        pdata[1] = y;
        pdata[2] = z;
        buffer.position(0);
        channel.read(buffer);
        switch(hdr.datatype) {
            case DT_SIGNED_SHORT:
                pdata[3] = buffer.getShort(0);
                break;
            case DT_SIGNED_INT:
                pdata[3] = buffer.getInt(0);
                break;
            case DT_UNSIGNED_CHAR:
                pdata[3] = buffer.get(0);
                if (pdata[3] < 0) pdata[3] = 255 - pdata[3];
                break;
            default:
                throw new JismError("unsupported data type");
        }
        if (pdata[3] < 0) Console.printWarning("negative intensity (%d)\n", pdata[3]);
        y++;
        if (y >= hdr.height) {
            x++;
            y = 0;
        }
        if (x >= hdr.width) {
            z++;
            x = 0;
        }
    }

    public void close() {
        channel = null;
        try {
            in.close();
        } catch (Exception e) {
            Console.printError("error while closing file\n");
        }
        in = null;
        buffer = null;
    }

    /**
	 * Get image width.
	 *
	 * @return width
	 **/
    public int getWidth() {
        return hdr.width;
    }

    /**
	 * Get image height.
	 *
	 * @return height
	 **/
    public int getHeight() {
        return hdr.height;
    }

    /**
	 * Get image depth.
	 *
	 * @return depth
	 **/
    public int getDepth() {
        return hdr.depth;
    }
}

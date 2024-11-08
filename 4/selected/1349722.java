package org.miv.jism.tools.writer;

import org.miv.jism.core.JismContext;
import org.miv.jism.core.JismError;
import org.miv.jism.tools.reader.AnalyseReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * Writer for the analyse image file format.
 *
 * @author Guilhelm Savin
 **/
public class AnalyseWriter implements JismWriter {

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

    public AnalyseWriter() {
    }

    public void write(JismContext ctx, String path) throws IOException {
        AnalyseReader.Header hdr = new AnalyseReader.Header();
        hdr.sizeOfHDR = 348;
        hdr.dim = ctx.getDepth() > 1 ? 3 : 2;
        hdr.width = ctx.getWidth();
        hdr.height = ctx.getHeight();
        hdr.depth = ctx.getDepth();
        if (ctx.getMaxIntensity() > Short.MAX_VALUE) {
            hdr.datatype = DT_SIGNED_INT;
            hdr.bitsPerPixel = 32;
        } else {
            hdr.datatype = DT_SIGNED_SHORT;
            hdr.bitsPerPixel = 16;
        }
        hdr.pixdim[0] = 1;
        hdr.pixdim[1] = 1;
        hdr.pixdim[2] = 1;
        writeHeader(hdr, path.substring(0, path.lastIndexOf(".")) + ".hdr");
        writeData(hdr, ctx, path.substring(0, path.lastIndexOf(".")) + ".img");
    }

    protected void writeHeader(AnalyseReader.Header hdr, String path) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(348);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 348; i++) buffer.put((byte) 0x0);
        buffer.putInt(0, 0x15C);
        buffer.putShort(40, (short) hdr.dim);
        buffer.putShort(42, (short) hdr.width);
        buffer.putShort(44, (short) hdr.height);
        buffer.putShort(46, (short) hdr.depth);
        buffer.putShort(70, (short) hdr.datatype);
        buffer.putShort(72, (short) hdr.bitsPerPixel);
        buffer.putFloat(76, hdr.dim);
        buffer.putFloat(80, hdr.pixdim[0]);
        buffer.putFloat(84, hdr.pixdim[1]);
        buffer.putFloat(88, hdr.pixdim[2]);
        buffer.position(0);
        RandomAccessFile out = new RandomAccessFile(path, "rw");
        out.setLength(0);
        if (out.getChannel().write(buffer) != 348) {
            throw new IOException("invalid written bytes size for header");
        }
        out.close();
    }

    protected void writeData(AnalyseReader.Header hdr, JismContext ctx, String path) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(hdr.bitsPerPixel / 8);
        RandomAccessFile out = new RandomAccessFile(path, "rw");
        FileChannel channel = out.getChannel();
        out.setLength(0);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int z = 0; z < ctx.getDepth(); z++) for (int x = 0; x < ctx.getWidth(); x++) for (int y = 0; y < ctx.getHeight(); y++) {
            switch(hdr.datatype) {
                case DT_SIGNED_SHORT:
                    buffer.putShort(0, (short) ctx.getPixelIntensity(ctx.pixelId(x, y, z)));
                    break;
                case DT_SIGNED_INT:
                    buffer.putInt(0, ctx.getPixelIntensity(ctx.pixelId(x, y, z)));
                    break;
                case DT_FLOAT:
                    buffer.putFloat(0, (float) ctx.getPixelIntensity(ctx.pixelId(x, y, z)));
                    break;
                case DT_DOUBLE:
                    buffer.putDouble(0, (double) ctx.getPixelIntensity(ctx.pixelId(x, y, z)));
                    break;
                default:
                    throw new JismError("unsupported data type");
            }
            buffer.position(0);
            channel.write(buffer);
        }
        out.close();
    }
}

package jaxlib.io.stream;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import jaxlib.jaxlib_private.CheckArg;
import jaxlib.jaxlib_private.io.CheckIOArg;
import jaxlib.lang.ReturnValueException;

/**
 * TODO: comment
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: DataIOImpl.java,v 1.2 2004/09/14 19:59:39 joerg_wassmer Exp $
 */
class DataIOImpl extends Object {

    protected DataIOImpl() throws InstantiationException {
        throw new InstantiationException();
    }

    public static long transferBytes(InputStream in, DataOutput out, long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) return 0;
        long count = 0;
        for (int b; (count < maxCount) && ((b = in.read()) >= 0); ) {
            out.write(b);
            count++;
        }
        return count;
    }

    public static void transferBytesFully(DataInput in, DataOutput out, long count) throws IOException {
        CheckIOArg.count(count);
        if (count == 0) return;
        long transferred = 0;
        while (transferred < count) {
            out.write(in.readByte());
            transferred++;
        }
    }

    public static void transferBytesFullyToStream(DataInput in, OutputStream out, long count) throws IOException {
        CheckIOArg.count(count);
        if (count == 0) return;
        long transferred = 0;
        while (transferred < count) {
            out.write(in.readByte());
            transferred++;
        }
    }

    /**
   * Reads bytes from specified stream via <tt>readByte</tt> and puts them 
   * to specified buffer, until the buffer has no more <tt>remaining</tt> elements.
   *
   * @param in        the stream to read from.
   * @param dest      the destination buffer.
   *
   * @throws IOException                if an I/O error occurs.
   * @throws ReadOnlyBufferException    if the buffer is <tt>readOnly</tt>.
   * @throws NullPointerException       if <tt>(in == null) || (dest == null)</tt>.
   *
   * @see DataInput#readByte()
   * @see ByteBuffer#isReadOnly()
   *
   * @since JaXLib 1.0
   */
    public static void read(DataInput in, ByteBuffer dest) throws IOException {
        int remaining = dest.remaining();
        while (remaining-- > 0) dest.put(in.readByte());
    }

    public static void write(DataOutput out, boolean[] data) throws IOException {
        write(out, data, 0, data.length);
    }

    public static void write(DataOutput out, boolean[] data, int off, int len) throws IOException {
        CheckIOArg.range(data.length, off, len);
        while (len-- > 0) out.writeBoolean(data[off++]);
    }

    public static void write(DataOutput out, byte[] data) throws IOException {
        write(out, data, 0, data.length);
    }

    public static void write(DataOutput out, byte[] data, int off, int len) throws IOException {
        CheckIOArg.range(data.length, off, len);
        out.write(data, off, len);
    }

    public static void write(DataOutput out, char[] data) throws IOException {
        write(out, data, 0, data.length);
    }

    public static void write(DataOutput out, char[] data, int off, int len) throws IOException {
        CheckIOArg.range(data.length, off, len);
        while (len-- > 0) out.writeChar(data[off++]);
    }

    public static void write(DataOutput out, double[] data) throws IOException {
        write(out, data, 0, data.length);
    }

    public static void write(DataOutput out, double[] data, int off, int len) throws IOException {
        CheckIOArg.range(data.length, off, len);
        while (len-- > 0) out.writeDouble(data[off++]);
    }

    public static void write(DataOutput out, float[] data) throws IOException {
        write(out, data, 0, data.length);
    }

    public static void write(DataOutput out, float[] data, int off, int len) throws IOException {
        CheckIOArg.range(data.length, off, len);
        while (len-- > 0) out.writeFloat(data[off++]);
    }

    public static void write(DataOutput out, int[] data) throws IOException {
        write(out, data, 0, data.length);
    }

    public static void write(DataOutput out, int[] data, int off, int len) throws IOException {
        CheckIOArg.range(data.length, off, len);
        while (len-- > 0) out.writeInt(data[off++]);
    }

    public static void write(DataOutput out, long[] data) throws IOException {
        write(out, data, 0, data.length);
    }

    public static void write(DataOutput out, long[] data, int off, int len) throws IOException {
        CheckIOArg.range(data.length, off, len);
        while (len-- > 0) out.writeLong(data[off++]);
    }

    public static void write(DataOutput out, short[] data) throws IOException {
        write(out, data, 0, data.length);
    }

    public static void write(DataOutput out, short[] data, int off, int len) throws IOException {
        CheckIOArg.range(data.length, off, len);
        while (len-- > 0) out.writeShort(data[off++]);
    }

    /**
   * Writes all remaining elements in specified buffer to specified stream.
   * When this method returns the buffer has no more remaining elements.
   * <p>
   * The buffer's position is undefined if an I/O error occurs.
   * </p>
   *
   * @param out       the stream to write elements to.
   * @param data      the buffer containing the elements to write.
   *
   * @throws IOException                if an I/O error occurs.
   * @throws NullPointerException       if <tt>(out == null) || (data == null)</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(DataOutput out, ByteBuffer data) throws IOException {
        final int count = data.remaining();
        if (data.hasArray()) {
            out.write(data.array(), data.arrayOffset() + data.position(), count);
            data.position(data.limit());
        } else {
            int remaining = count;
            while (remaining-- > 0) out.writeByte(data.get());
        }
        return count;
    }

    /**
   * Writes all remaining elements in specified buffer to specified stream.
   * When this method returns the buffer has no more remaining elements.
   * <p>
   * The buffer's position is undefined if an I/O error occurs.
   * </p>
   *
   * @param out       the stream to write elements to.
   * @param data      the buffer containing the elements to write.
   *
   * @throws IOException                if an I/O error occurs.
   * @throws NullPointerException       if <tt>(out == null) || (data == null)</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(DataOutput out, CharBuffer data) throws IOException {
        final int count = data.remaining();
        for (int rem = count; --rem >= 0; ) out.writeChar(data.get());
        return count;
    }

    /**
   * Writes all remaining elements in specified buffer to specified stream.
   * When this method returns the buffer has no more remaining elements.
   * <p>
   * The buffer's position is undefined if an I/O error occurs.
   * </p>
   *
   * @param out       the stream to write elements to.
   * @param data      the buffer containing the elements to write.
   *
   * @throws IOException                if an I/O error occurs.
   * @throws NullPointerException       if <tt>(out == null) || (data == null)</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(DataOutput out, DoubleBuffer data) throws IOException {
        final int count = data.remaining();
        for (int rem = count; --rem >= 0; ) out.writeDouble(data.get());
        return count;
    }

    /**
   * Writes all remaining elements in specified buffer to specified stream.
   * When this method returns the buffer has no more remaining elements.
   * <p>
   * The buffer's position is undefined if an I/O error occurs.
   * </p>
   *
   * @param out       the stream to write elements to.
   * @param data      the buffer containing the elements to write.
   *
   * @throws IOException                if an I/O error occurs.
   * @throws NullPointerException       if <tt>(out == null) || (data == null)</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(DataOutput out, FloatBuffer data) throws IOException {
        final int count = data.remaining();
        for (int rem = count; --rem >= 0; ) out.writeFloat(data.get());
        return count;
    }

    /**
   * Writes all remaining elements in specified buffer to specified stream.
   * When this method returns the buffer has no more remaining elements.
   * <p>
   * The buffer's position is undefined if an I/O error occurs.
   * </p>
   *
   * @param out       the stream to write elements to.
   * @param data      the buffer containing the elements to write.
   *
   * @throws IOException                if an I/O error occurs.
   * @throws NullPointerException       if <tt>(out == null) || (data == null)</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(DataOutput out, IntBuffer data) throws IOException {
        final int count = data.remaining();
        for (int rem = count; --rem >= 0; ) out.writeInt(data.get());
        return count;
    }

    /**
   * Writes all remaining elements in specified buffer to specified stream.
   * When this method returns the buffer has no more remaining elements.
   * <p>
   * The buffer's position is undefined if an I/O error occurs.
   * </p>
   *
   * @param out       the stream to write elements to.
   * @param data      the buffer containing the elements to write.
   *
   * @throws IOException                if an I/O error occurs.
   * @throws NullPointerException       if <tt>(out == null) || (data == null)</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(DataOutput out, LongBuffer data) throws IOException {
        final int count = data.remaining();
        for (int rem = count; --rem >= 0; ) out.writeLong(data.get());
        return count;
    }

    /**
   * Writes all remaining elements in specified buffer to specified stream.
   * When this method returns the buffer has no more remaining elements.
   * <p>
   * The buffer's position is undefined if an I/O error occurs.
   * </p>
   *
   * @param out       the stream to write elements to.
   * @param data      the buffer containing the elements to write.
   *
   * @throws IOException                if an I/O error occurs.
   * @throws NullPointerException       if <tt>(out == null) || (data == null)</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(DataOutput out, ShortBuffer data) throws IOException {
        final int count = data.remaining();
        for (int rem = count; --rem >= 0; ) out.writeShort(data.get());
        return count;
    }

    static int writeUTFImpl(OutputStream out, CharSequence str) throws IOException {
        return ByteOrders.BIG_ENDIAN.writeUTF(out, str);
    }
}

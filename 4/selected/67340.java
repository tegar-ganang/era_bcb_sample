package jaxlib.io.stream;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.WritableByteChannel;
import jaxlib.jaxlib_private.io.CheckIOArg;

/**
 * Common data I/O stream utilities.
 * <p>
 * <b>Stream implementations should never call methods of this class with itself as argument.</b>
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: DataIO.java,v 1.2 2004/09/14 19:59:39 joerg_wassmer Exp $
 */
public class DataIO extends DataIOImpl {

    protected DataIO() throws InstantiationException {
        throw new InstantiationException();
    }

    public static void readFully(DataInput in, ByteBuffer dest) throws IOException {
        if (in instanceof XDataInput) {
            ((XDataInput) in).readFully(dest);
        } else {
            for (int remaining = dest.remaining(); --remaining >= 0; ) dest.put(in.readByte());
        }
    }

    public static void readFully(DataInput in, CharBuffer dest) throws IOException {
        if (in instanceof XDataInput) {
            ((XDataInput) in).readFully(dest);
        } else {
            for (int remaining = dest.remaining(); --remaining >= 0; ) dest.put(in.readChar());
        }
    }

    public static void readFully(DataInput in, DoubleBuffer dest) throws IOException {
        if (in instanceof XDataInput) {
            ((XDataInput) in).readFully(dest);
        } else {
            for (int remaining = dest.remaining(); --remaining >= 0; ) dest.put(in.readDouble());
        }
    }

    public static void readFully(DataInput in, FloatBuffer dest) throws IOException {
        if (in instanceof XDataInput) {
            ((XDataInput) in).readFully(dest);
        } else {
            for (int remaining = dest.remaining(); --remaining >= 0; ) dest.put(in.readFloat());
        }
    }

    public static void readFully(DataInput in, IntBuffer dest) throws IOException {
        if (in instanceof XDataInput) {
            ((XDataInput) in).readFully(dest);
        } else {
            for (int remaining = dest.remaining(); --remaining >= 0; ) dest.put(in.readInt());
        }
    }

    public static void readFully(DataInput in, LongBuffer dest) throws IOException {
        if (in instanceof XDataInput) {
            ((XDataInput) in).readFully(dest);
        } else {
            for (int remaining = dest.remaining(); --remaining >= 0; ) dest.put(in.readLong());
        }
    }

    public static void readFully(DataInput in, ShortBuffer dest) throws IOException {
        if (in instanceof XDataInput) {
            ((XDataInput) in).readFully(dest);
        } else {
            for (int remaining = dest.remaining(); --remaining >= 0; ) dest.put(in.readShort());
        }
    }

    public static void write(DataOutput out, boolean[] data) throws IOException {
        write(out, data, 0, data.length);
    }

    public static void write(DataOutput out, boolean[] data, int off, int len) throws IOException {
        CheckIOArg.range(data.length, off, len);
        while (len-- > 0) out.writeBoolean(data[off++]);
    }

    public static void write(DataOutput out, char[] data) throws IOException {
        write(out, data, 0, data.length);
    }

    public static void write(DataOutput out, char[] data, int off, int len) throws IOException {
        if ((len > 32) && (out instanceof XDataOutput)) ((XDataOutput) out).write(CharBuffer.wrap(data, off, len)); else DataIOImpl.write(out, data, off, len);
    }

    public static void write(DataOutput out, double[] data) throws IOException {
        write(out, data, 0, data.length);
    }

    public static void write(DataOutput out, double[] data, int off, int len) throws IOException {
        if ((len > 32) && (out instanceof XDataOutput)) ((XDataOutput) out).write(DoubleBuffer.wrap(data, off, len)); else DataIOImpl.write(out, data, off, len);
    }

    public static void write(DataOutput out, float[] data) throws IOException {
        write(out, data, 0, data.length);
    }

    public static void write(DataOutput out, float[] data, int off, int len) throws IOException {
        if ((len > 32) && (out instanceof XDataOutput)) ((XDataOutput) out).write(FloatBuffer.wrap(data, off, len)); else DataIOImpl.write(out, data, off, len);
    }

    public static void write(DataOutput out, int[] data) throws IOException {
        write(out, data, 0, data.length);
    }

    public static void write(DataOutput out, int[] data, int off, int len) throws IOException {
        if ((len > 32) && (out instanceof XDataOutput)) ((XDataOutput) out).write(IntBuffer.wrap(data, off, len)); else DataIOImpl.write(out, data, off, len);
    }

    public static void write(DataOutput out, long[] data) throws IOException {
        write(out, data, 0, data.length);
    }

    public static void write(DataOutput out, long[] data, int off, int len) throws IOException {
        if ((len > 32) && (out instanceof XDataOutput)) ((XDataOutput) out).write(LongBuffer.wrap(data, off, len)); else DataIOImpl.write(out, data, off, len);
    }

    public static void write(DataOutput out, short[] data) throws IOException {
        write(out, data, 0, data.length);
    }

    public static void write(DataOutput out, short[] data, int off, int len) throws IOException {
        if ((len > 32) && (out instanceof XDataOutput)) ((XDataOutput) out).write(ShortBuffer.wrap(data, off, len)); else DataIOImpl.write(out, data, off, len);
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
        WritableByteChannel ch;
        if (out instanceof WritableByteChannel) ch = (WritableByteChannel) out; else if (out instanceof RandomAccessFile) ch = ((RandomAccessFile) out).getChannel(); else ch = null;
        if (ch != null) {
            int count = data.remaining();
            while (data.hasRemaining()) ch.write(data);
            return count;
        } else return DataIOImpl.write(out, data);
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
        if (out instanceof XDataOutput) return ((XDataOutput) out).write(data); else return DataIOImpl.write(out, data);
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
        if (out instanceof XDataOutput) return ((XDataOutput) out).write(data); else return DataIOImpl.write(out, data);
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
        if (out instanceof XDataOutput) return ((XDataOutput) out).write(data); else return DataIOImpl.write(out, data);
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
        if (out instanceof XDataOutput) return ((XDataOutput) out).write(data); else return DataIOImpl.write(out, data);
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
        if (out instanceof XDataOutput) return ((XDataOutput) out).write(data); else return DataIOImpl.write(out, data);
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
        if (out instanceof XDataOutput) return ((XDataOutput) out).write(data); else return DataIOImpl.write(out, data);
    }

    /**
   * Returns the number of bytes which would be written to stream if the specified string would
   * be written via {@link DataOutput#writeUTF(String)}.
   *
   * @since JaXLib 1.0
   */
    public static int countUTFBytes(CharSequence s) {
        int utflen = 2;
        for (int i = s.length(); --i >= 0; ) {
            char c = s.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) utflen++; else if (c > 0x07FF) utflen += 3; else utflen += 2;
        }
        return utflen;
    }

    public static void writeUTF(OutputStream out, CharSequence str) throws IOException {
        if ((out instanceof DataOutput) && (str instanceof String)) ((DataOutput) out).writeUTF(str.toString()); else ByteOrders.BIG_ENDIAN.writeUTF(out, str);
    }
}

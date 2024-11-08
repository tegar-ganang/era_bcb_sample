package jaxlib.io.stream;

import java.io.DataOutput;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.WritableByteChannel;
import jaxlib.col.ImmutableCompactHashSet;
import jaxlib.util.CheckBounds;

/**
 * Provides methods to read and write from and to untrusted streams.
 * All methods in this class do guarantee that datastructures given as arguments will not be manipulated by
 * bogus stream implementations.
 * <p>
 * The purpose of this class is to transfer private data securely to untrusted stream instances, without e.g.
 * making the array visible. Although, normally you should use another stream instance for such tasks.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: SecureIO.java 3034 2012-01-02 22:25:52Z joerg_wassmer $
 */
public class SecureIO extends Object {

    protected SecureIO() throws InstantiationException {
        throw new InstantiationException();
    }

    private static final int WRAP_LIMIT = 32;

    /**
   * Use classnames instead of classes to avoid classloading.
   * We check the classes in an extra step.
   *
   * Do only add stream classes which do not write to other streams,
   * e.g. do not add subclasses of FilterOutputStream!
   */
    private static final ImmutableCompactHashSet<String> trustedClassNames = ImmutableCompactHashSet.of(String.class, "java.io.ByteArrayOutputStream", "java.io.CharArrayWriter", "java.io.FileOutputStream", "java.io.FileWriter", "java.io.RandomAccessFile", "java.io.StringWriter", "jaxlib.io.streams.XRandomAccessFile");

    private static boolean trustClass(Class c) {
        String cn = c.getName();
        if (!trustedClassNames.contains(cn)) return false;
        try {
            return Class.forName(cn) == c;
        } catch (final ClassNotFoundException ex) {
            throw new AssertionError(ex);
        }
    }

    public static void append(final Appendable out, final char[] data, int off, int len) throws IOException {
        if (out instanceof XWriter) {
            ((XWriter) out).writeSecurely(data, off, len);
        } else if (len < WRAP_LIMIT) {
            CheckBounds.offset(data, off, len);
            while (len-- > 0) out.append(data[off++]);
        } else if (trustClass(out.getClass())) {
            if (out instanceof Writer) ((Writer) out).write(data, off, len); else out.append(CharBuffer.wrap(data, off, len));
        } else out.append(CharBuffer.wrap(data, off, len).asReadOnlyBuffer());
    }

    public static void append(Appendable out, CharSequence data, int fromIndex, int toIndex) throws IOException {
        if (toIndex - fromIndex < WRAP_LIMIT) {
            CheckBounds.offset(data.length(), fromIndex, toIndex);
            while (fromIndex < toIndex) out.append(data.charAt(fromIndex++));
        } else if (((fromIndex == 0) && (toIndex == data.length())) && (trustClass(out.getClass()) || (data instanceof String) || ((data instanceof CharBuffer) && ((CharBuffer) data).isReadOnly()))) {
            out.append(data);
        } else out.append(CharBuffer.wrap(data, fromIndex, toIndex));
    }

    public static void print(PrintStream out, char[] data, int off, int len) {
        try {
            append(out, data, off, len);
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    public static void print(PrintWriter out, char[] data, int off, int len) {
        try {
            append(out, data, off, len);
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    public static void write(OutputStream out, byte[] data, int off, int len) throws IOException {
        CheckBounds.offset(data, off, len);
        if (out instanceof XOutputStream) {
            ((XOutputStream) out).writeSecurely(data, off, len);
        } else if (len <= WRAP_LIMIT) {
            for (int hi = off + len; off < hi; off++) out.write(data[off]);
        } else if (trustClass(out.getClass())) {
            out.write(data, off, len);
        } else if (out instanceof FileOutputStream) {
            ByteBuffer buf = ByteBuffer.wrap(data, off, len).asReadOnlyBuffer();
            WritableByteChannel ch = ((FileOutputStream) out).getChannel();
            while (buf.hasRemaining()) ch.write(buf);
        } else if (out instanceof WritableByteChannel) {
            ByteBuffer buf = ByteBuffer.wrap(data, off, len).asReadOnlyBuffer();
            WritableByteChannel ch = (WritableByteChannel) out;
            while (buf.hasRemaining()) ch.write(buf);
        } else {
            int capacity = Math.min(2048, len);
            byte[] a = new byte[capacity];
            while (len > 0) {
                int step = Math.min(capacity, len);
                System.arraycopy(data, off, a, 0, step);
                out.write(a, 0, step);
                len -= step;
                off += step;
            }
        }
    }

    public static void writeData(DataOutput out, byte[] data, int off, int len) throws IOException {
        CheckBounds.offset(data, off, len);
        if (out instanceof XOutputStream) {
            ((XOutputStream) out).writeSecurely(data, off, len);
        } else if (len <= WRAP_LIMIT) {
            for (int hi = off + len; off < hi; off++) out.write(data[off]);
        } else if (trustClass(out.getClass())) out.write(data, off, len); else if (out instanceof FileOutputStream) {
            ByteBuffer buf = ByteBuffer.wrap(data, off, len).asReadOnlyBuffer();
            WritableByteChannel ch = ((FileOutputStream) out).getChannel();
            while (buf.hasRemaining()) ch.write(buf);
        } else if (out instanceof WritableByteChannel) {
            ByteBuffer buf = ByteBuffer.wrap(data, off, len).asReadOnlyBuffer();
            WritableByteChannel ch = (WritableByteChannel) out;
            while (buf.hasRemaining()) ch.write(buf);
        } else {
            int capacity = Math.min(2048, len);
            byte[] a = new byte[capacity];
            while (len > 0) {
                int step = Math.min(capacity, len);
                System.arraycopy(data, off, a, 0, step);
                out.write(a, 0, step);
                len -= step;
                off += step;
            }
        }
    }

    public static void writeData(DataOutput out, boolean[] data, int off, int len) throws IOException {
        CheckBounds.offset(data, off, len);
        for (int hi = off + len; off < hi; off++) out.writeBoolean(data[off]);
    }

    public static void writeData(DataOutput out, char[] data, int off, int len) throws IOException {
        CheckBounds.offset(data, off, len);
        if ((len <= WRAP_LIMIT) || !(out instanceof XDataOutput)) {
            for (int hi = off + len; off < hi; off++) out.writeChar(data[off]);
        } else {
            CharBuffer buf = CharBuffer.wrap(data, off, len).asReadOnlyBuffer();
            ((XDataOutput) out).write(buf);
        }
    }

    public static void writeData(DataOutput out, double[] data, int off, int len) throws IOException {
        CheckBounds.offset(data, off, len);
        if ((len <= WRAP_LIMIT) || !(out instanceof XDataOutput)) {
            for (int hi = off + len; off < hi; off++) out.writeDouble(data[off]);
        } else {
            DoubleBuffer buf = DoubleBuffer.wrap(data, off, len).asReadOnlyBuffer();
            ((XDataOutput) out).write(buf);
        }
    }

    public static void writeData(DataOutput out, float[] data, int off, int len) throws IOException {
        CheckBounds.offset(data, off, len);
        if ((len <= WRAP_LIMIT) || !(out instanceof XDataOutput)) {
            for (int hi = off + len; off < hi; off++) out.writeFloat(data[off]);
        } else {
            FloatBuffer buf = FloatBuffer.wrap(data, off, len).asReadOnlyBuffer();
            ((XDataOutput) out).write(buf);
        }
    }

    public static void writeData(DataOutput out, long[] data, int off, int len) throws IOException {
        CheckBounds.offset(data, off, len);
        if ((len <= WRAP_LIMIT) || !(out instanceof XDataOutput)) {
            for (int hi = off + len; off < hi; off++) out.writeLong(data[off]);
        } else {
            LongBuffer buf = LongBuffer.wrap(data, off, len).asReadOnlyBuffer();
            ((XDataOutput) out).write(buf);
        }
    }

    public static void writeData(DataOutput out, int[] data, int off, int len) throws IOException {
        CheckBounds.offset(data, off, len);
        if ((len <= WRAP_LIMIT) || !(out instanceof XDataOutput)) {
            for (int hi = off + len; off < hi; off++) out.writeInt(data[off]);
        } else {
            IntBuffer buf = IntBuffer.wrap(data, off, len).asReadOnlyBuffer();
            ((XDataOutput) out).write(buf);
        }
    }

    public static void writeData(DataOutput out, short[] data, int off, int len) throws IOException {
        CheckBounds.offset(data, off, len);
        if ((len <= WRAP_LIMIT) || !(out instanceof XDataOutput)) {
            for (int hi = off + len; off < hi; off++) out.writeShort(data[off]);
        } else {
            ShortBuffer buf = ShortBuffer.wrap(data, off, len).asReadOnlyBuffer();
            ((XDataOutput) out).write(buf);
        }
    }

    public static void writeData(DataOutput out, ByteBuffer data) throws IOException {
        if ((out instanceof WritableByteChannel) && (data.isReadOnly() || (data.remaining() >= WRAP_LIMIT))) {
            ByteBuffer ro = (data.isReadOnly() || trustClass(out.getClass())) ? data : data.asReadOnlyBuffer();
            WritableByteChannel ch = (WritableByteChannel) out;
            try {
                while (data.hasRemaining()) ch.write(data);
            } finally {
                data.position(ro.position());
            }
        } else if (out instanceof RandomAccessFile) {
            WritableByteChannel ch = ((RandomAccessFile) out).getChannel();
            while (data.hasRemaining()) ch.write(data);
        } else {
            for (int remaining = data.remaining(); --remaining > 0; ) out.writeByte(data.get());
        }
    }

    public static void writeData(DataOutput out, CharBuffer data) throws IOException {
        if ((out instanceof XDataOutput) && (data.isReadOnly() || (data.remaining() >= WRAP_LIMIT))) {
            CharBuffer ro = (data.isReadOnly() || trustClass(out.getClass())) ? data : data.asReadOnlyBuffer();
            try {
                ((XDataOutput) out).write(data);
            } finally {
                data.position(ro.position());
            }
        } else {
            for (int remaining = data.remaining(); --remaining > 0; ) out.writeChar(data.get());
        }
    }

    public static void writeData(DataOutput out, DoubleBuffer data) throws IOException {
        if ((out instanceof XDataOutput) && (data.isReadOnly() || (data.remaining() >= WRAP_LIMIT))) {
            DoubleBuffer ro = (data.isReadOnly() || trustClass(out.getClass())) ? data : data.asReadOnlyBuffer();
            try {
                ((XDataOutput) out).write(data);
            } finally {
                data.position(ro.position());
            }
        } else {
            for (int remaining = data.remaining(); --remaining > 0; ) out.writeDouble(data.get());
        }
    }

    public static void writeData(DataOutput out, FloatBuffer data) throws IOException {
        if ((out instanceof XDataOutput) && (data.isReadOnly() || (data.remaining() >= WRAP_LIMIT))) {
            FloatBuffer ro = (data.isReadOnly() || trustClass(out.getClass())) ? data : data.asReadOnlyBuffer();
            try {
                ((XDataOutput) out).write(data);
            } finally {
                data.position(ro.position());
            }
        } else {
            for (int remaining = data.remaining(); --remaining > 0; ) out.writeFloat(data.get());
        }
    }

    public static void writeData(DataOutput out, IntBuffer data) throws IOException {
        if ((out instanceof XDataOutput) && (data.isReadOnly() || (data.remaining() >= WRAP_LIMIT))) {
            IntBuffer ro = (data.isReadOnly() || trustClass(out.getClass())) ? data : data.asReadOnlyBuffer();
            try {
                ((XDataOutput) out).write(data);
            } finally {
                data.position(ro.position());
            }
        } else {
            for (int remaining = data.remaining(); --remaining > 0; ) out.writeInt(data.get());
        }
    }

    public static void writeData(DataOutput out, LongBuffer data) throws IOException {
        if ((out instanceof XDataOutput) && (data.isReadOnly() || (data.remaining() >= WRAP_LIMIT))) {
            LongBuffer ro = (data.isReadOnly() || trustClass(out.getClass())) ? data : data.asReadOnlyBuffer();
            try {
                ((XDataOutput) out).write(data);
            } finally {
                data.position(ro.position());
            }
        } else {
            for (int remaining = data.remaining(); --remaining > 0; ) out.writeLong(data.get());
        }
    }

    public static void writeData(DataOutput out, ShortBuffer data) throws IOException {
        if ((out instanceof XDataOutput) && (data.isReadOnly() || (data.remaining() >= WRAP_LIMIT))) {
            ShortBuffer ro = (data.isReadOnly() || trustClass(out.getClass())) ? data : data.asReadOnlyBuffer();
            try {
                ((XDataOutput) out).write(data);
            } finally {
                data.position(ro.position());
            }
        } else {
            for (int remaining = data.remaining(); --remaining > 0; ) out.writeShort(data.get());
        }
    }

    public static int writeToChannel(WritableByteChannel out, ByteBuffer data) throws IOException {
        ByteBuffer ro = (data.isReadOnly() || trustClass(out.getClass())) ? data : data.asReadOnlyBuffer();
        try {
            return out.write(data);
        } finally {
            data.position(ro.position());
        }
    }

    public static void writeToChannelFully(WritableByteChannel out, ByteBuffer data) throws IOException {
        ByteBuffer ro = (data.isReadOnly() || trustClass(out.getClass())) ? data : data.asReadOnlyBuffer();
        try {
            while (data.hasRemaining()) out.write(data);
        } finally {
            data.position(ro.position());
        }
    }
}

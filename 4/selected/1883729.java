package jaxlib.io.stream.pds;

import java.io.Closeable;
import java.io.EOFException;
import java.io.Externalizable;
import java.io.Flushable;
import java.io.InputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.zip.GZIPOutputStream;
import javax.swing.table.TableModel;
import jaxlib.beans.BeanMapSpec;
import jaxlib.beans.BeanProperties;
import jaxlib.beans.BeanProperty;
import jaxlib.beans.BeanPropertyList;
import jaxlib.col.ObjectArray;
import jaxlib.jaxlib_private.io.CheckIOArg;
import jaxlib.io.CloseableInstance;
import jaxlib.io.stream.BufferedXOutputStream;
import jaxlib.io.stream.ByteOrders;
import jaxlib.io.stream.IOStreams;
import jaxlib.io.stream.XOutputStream;
import jaxlib.io.stream.adapter.AdapterOutputStream;
import jaxlib.io.stream.embedded.BlockOutputStream;
import jaxlib.lang.Bytes;
import jaxlib.lang.ClassRegistry;
import jaxlib.lang.Objects;
import jaxlib.lang.ReturnValueException;
import jaxlib.util.CheckArg;
import jaxlib.util.Strings;

/**
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: PDSOutputStream.java 1607 2006-02-12 12:22:54Z joerg_wassmer $
 */
public class PDSOutputStream extends XOutputStream implements ObjectOutput {

    private static final int STATE_ROOT = 0;

    private static final int STATE_IN_SUBSTREAM = 1;

    private static final int STATE_IN_TABLE = 2;

    private static final int STATE_TABLE_WRITER = 3;

    private static final int STATE_END = 4;

    private static final ClassRegistry<Object, PDSTableHeader> beanTableHeaders = new ClassRegistry<Object, PDSTableHeader>(false);

    private static final ClassRegistry<Object, PDSTableHeader> entityBeanTableHeaders = new ClassRegistry<Object, PDSTableHeader>(false);

    final PDSCompressionType compression;

    final boolean ownOutput;

    PDSOutputStream.Output out;

    int state;

    int columnIndex;

    final int columnCount;

    final PDSTableHeader tableHeader;

    IdentityHashMap<Class, Integer> classes;

    IdentityHashMap<Object, Integer> objects;

    ByteBuffer buffer;

    public PDSOutputStream(PDSOutputStream.Output out) throws IOException {
        super();
        CheckArg.notNull(out, "out");
        this.out = out;
        this.ownOutput = false;
        this.columnCount = -1;
        this.compression = PDSCompressionType.NONE;
        this.state = STATE_ROOT;
        this.tableHeader = null;
        out.beginPacket();
    }

    /**
   * Constructs a new {@code PDSOutputStream} and writes the {@code PDS} stream header.
   *
   * @param out
   *  the destination stream.
   * @param byteOrder
   *  the byte order for multibyte values.
   * @param compression
   *  the type of compression to apply to the bytes written to the specified stream.
   *
   * @throws IOException
   *  if an I/O error occurs.
   * @throws NullPointerException 
   *  if {@code (out == null) || (byteOrder == null) || (compression == null)}.
   *
   * @since JaXLib 1.0
   */
    @Deprecated
    public PDSOutputStream(OutputStream out, final PDSCompressionType compression, int bufferSize) throws IOException {
        super();
        CheckArg.notNull(out, "out");
        CheckArg.notNull(compression, "compression");
        CheckArg.notNegative(bufferSize, "bufferSize");
        bufferSize = Math.max(bufferSize, 128);
        this.out = new Output(out, bufferSize);
        this.ownOutput = true;
        this.columnCount = -1;
        this.compression = compression;
        this.state = STATE_ROOT;
        this.tableHeader = null;
        this.out.beginPacket();
    }

    /**
   * Constructor for PDSOutputStream.Table
   */
    private PDSOutputStream(PDSOutputStream parent, PDSTableHeader header) {
        super();
        this.buffer = parent.buffer;
        this.classes = parent.classes;
        this.columnCount = header.getColumnCount();
        this.columnIndex = 0;
        this.compression = parent.compression;
        this.objects = parent.objects;
        this.out = parent.out;
        this.ownOutput = false;
        this.state = STATE_TABLE_WRITER;
        this.tableHeader = header;
    }

    final Output checkWriteRaw() throws IOException {
        final Output out = this.out;
        if (out != null) {
            switch(this.state) {
                case STATE_ROOT:
                    return out;
                case STATE_IN_SUBSTREAM:
                    {
                        throw new IllegalStateException("currently writing substream, you must close the stream first");
                    }
                case STATE_IN_TABLE:
                    {
                        throw new IllegalStateException("currently writing table, you must close the table first");
                    }
                case STATE_TABLE_WRITER:
                    return out;
                case STATE_END:
                    {
                        if (this.tableHeader == null) throw new IOException("closed"); else throw new IOException("table closed");
                    }
                default:
                    throw new AssertionError(this.state);
            }
        } else if (this.tableHeader == null) throw new IOException("closed"); else throw new IOException("table closed");
    }

    final Output ensureOpen() throws IOException {
        final Output out = this.out;
        if (out != null) return out; else if (this.tableHeader == null) throw new IOException("closed"); else throw new IOException("table closed");
    }

    final Output writeType(int type) throws IOException {
        switch(this.state) {
            case STATE_ROOT:
                {
                    final Output out = ensureOpen();
                    out.write(type);
                    return out;
                }
            case STATE_IN_SUBSTREAM:
                {
                    throw new IllegalStateException("currently writing substream, you must close the stream first");
                }
            case STATE_IN_TABLE:
                {
                    throw new IllegalStateException("currently writing table, you must close the table first");
                }
            case STATE_TABLE_WRITER:
                {
                    final int columnIndex = this.columnIndex;
                    if (columnIndex == this.columnCount) {
                        throw new IllegalStateException("at end of table row, you must call nextRow() or close()");
                    } else {
                        final PDSType columnType = this.tableHeader.getColumnType(columnIndex);
                        if ((columnType != PDSType.VARIANT) && !columnType.isEquivalent(type)) {
                            throw new PDSTypeMismatchException("expecting type " + columnType + " for column index " + columnIndex);
                        } else {
                            final Output out = ensureOpen();
                            if (columnType == PDSType.VARIANT) out.write(type);
                            this.columnIndex = columnIndex + 1;
                            return out;
                        }
                    }
                }
            case STATE_END:
                {
                    if (this.tableHeader == null) throw new IOException("closed"); else throw new IOException("table closed");
                }
            default:
                throw new AssertionError(this.state);
        }
    }

    private void writeArrayObject(final Object v) throws IOException {
        final PDSType pdsType = PDSType.forJavaType(v.getClass().getComponentType());
        final int length = Array.getLength(v);
        final Output out = ensureOpen();
        out.writeInt(length);
        switch(pdsType) {
            case BOOL:
                out.write((boolean[]) v, 0, length);
                break;
            case FLOAT32:
                out.write((float[]) v, 0, length);
                break;
            case FLOAT64:
                out.write((double[]) v, 0, length);
                break;
            case INT8:
                out.write((byte[]) v);
                break;
            case INT16:
                out.write((short[]) v, 0, length);
                break;
            case INT32:
                out.write((int[]) v, 0, length);
                break;
            case INT64:
                out.write((long[]) v, 0, length);
                break;
            case UTF:
                out.writeCharsUTF((char[]) v, 0, length);
                break;
            default:
                {
                    Object[] a = (Object[]) v;
                    for (int i = 0; i < a.length; i++) writeObject(a[i]);
                }
                break;
        }
    }

    private <T> void writeClassDescriptor(final Class<T> c) throws IOException {
        if (this.classes == null) this.classes = new IdentityHashMap<Class, Integer>();
        Integer cid = this.classes.get(c);
        if (cid != null) {
            ensureOpen().writeInt32Bits(cid.intValue());
        } else {
            cid = this.classes.size() + 1;
            final Output out = ensureOpen();
            out.writeInt32Bits(-cid);
            writeUTFArray(c.getName());
            if (c.isArray()) {
                out.writeInt8Bits(PDS.CLASS_FORMAT_ARRAY);
            } else if (Externalizable.class.isAssignableFrom(c) || PDSData.class.isAssignableFrom(c)) {
                out.writeInt8Bits(PDS.CLASS_FORMAT_EXTERNALIZABLE);
            } else {
                out.writeInt8Bits(PDS.CLASS_FORMAT_BEAN);
                final BeanProperties<T> beanProperties = BeanProperties.getInstance(c);
                final boolean isEntity = false;
                final int propertyCount = beanProperties.getPropertyList().size();
                out.writeInt32Bits(propertyCount);
                if (propertyCount > 0) {
                    for (BeanProperty p : beanProperties.getPropertyList()) {
                        writeUTFArray(p.getName());
                    }
                }
            }
        }
    }

    static void writeUTFArrayBody(final Output out, final String s) throws IOException {
        if (s == null) {
            out.write(PDS.NULL);
            out.write(0);
        } else {
            final int length = s.length();
            out.writeInt32Bits(length);
            out.writeCharsUTF(s, 0, s.length());
        }
    }

    static void writeUTFArrayBody(final Output out, final CharSequence s) throws IOException {
        if (s instanceof String) {
            writeUTFArrayBody(out, (String) s);
        } else if (s == null) {
            out.write(PDS.NULL);
            out.write(0);
        } else {
            final int length = s.length();
            out.writeInt32Bits(length);
            out.writeCharsUTF(s, 0, length);
        }
    }

    /**
   * @return
   *   A substream, never {@code null}.
   *
   * @since JaXLib 1.0
   */
    public XOutputStream beginBlob() throws IOException {
        writeType(PDS.TYPE_BLOB);
        return new PDSOutputStream.SubStream(this);
    }

    /**
   * @return
   *   A substream for writing a table; {@code null} if {@code header == null}.
   *
   * @since JaXLib 1.0
   */
    public PDSOutputStream.Table beginTable(PDSTableHeader header) throws IOException {
        return beginTable(header, -1);
    }

    /**
   * @return
   *   A substream for writing a table; {@code null} if {@code header == null}.
   *
   * @since JaXLib 1.0
   */
    public PDSOutputStream.Table beginTable(PDSTableHeader header, int rowCount) throws IOException {
        final Output out = writeType(PDS.TYPE_TABLE);
        if (header == null) {
            writeUTFArray(header.name);
            out.writeNull();
            return null;
        } else {
            writeUTFArray(header.name);
            final int columnCount = header.getColumnCount();
            writeTypeArray(header.columnTypes);
            for (int i = 0; i < columnCount; i++) writeUTFArray(header.getColumnName(i));
            writeInt32Array(header.columnLengths);
            if (rowCount >= 0) writeInt32(rowCount); else writeInt32(null);
            final int initialState = this.state;
            this.state = STATE_IN_TABLE;
            return new PDSOutputStream.Table(this, header, initialState);
        }
    }

    @Override
    public void close() throws IOException {
        final Output out = this.out;
        if (out != null) {
            try {
                closeInstance();
                if (this.ownOutput) out.close();
            } catch (IOException ex) {
                try {
                    if (this.ownOutput) out.close();
                } finally {
                    throw ex;
                }
            }
        }
    }

    @Override
    public void closeInstance() throws IOException {
        final Output out = this.out;
        if (out != null) {
            this.out = null;
            try {
                out.write(PDS.EOF);
                out.flush();
            } finally {
                this.classes = null;
                this.objects = null;
                this.state = STATE_END;
                this.out = null;
            }
        }
    }

    @Override
    public void flush() throws IOException {
        ensureOpen().flush();
    }

    public final PDSCompressionType getCompressionType() {
        return this.compression;
    }

    @Override
    public final boolean isOpen() {
        return this.out != null;
    }

    @Override
    public void write(final int b) throws IOException {
        checkWriteRaw().write(b);
    }

    @Override
    public void write(final byte[] src, final int offs, final int len) throws IOException {
        checkWriteRaw().write(src, offs, len);
    }

    public final void writeBigDecimal(BigDecimal v) throws IOException {
        final Output out = writeType(PDS.TYPE_BIGDECIMAL);
        if (v != null) {
            out.writeInt32Bits(v.scale());
            byte[] a = v.unscaledValue().toByteArray();
            out.writeInt(a.length);
            out.write(a);
        } else {
            out.writeNull();
        }
    }

    public final void writeBigInteger(BigInteger v) throws IOException {
        final Output out = writeType(PDS.TYPE_BIGINTEGER);
        if (v != null) {
            byte[] a = v.toByteArray();
            out.writeInt32Bits(a.length);
            out.write(a);
        } else {
            out.writeNull();
        }
    }

    public final long writeBlob(final InputStream in) throws IOException {
        if (in == null) {
            writeInt8Array(null);
            return 0;
        } else {
            Output out = writeType(PDS.TYPE_BLOB);
            out.writeInt64Bits(-1);
            out.beginBlock();
            try {
                long count = out.transferFrom(in, -1);
                out.endBlock();
                return count;
            } catch (IOException ex) {
                try {
                    out.endBlock();
                } finally {
                    throw ex;
                }
            }
        }
    }

    public final long writeBlob(final InputStream in, long length) throws IOException {
        if (length < 0) {
            return writeBlob(in);
        } else if (in == null) {
            if (length > 0) throw new IllegalArgumentException("in == null but length > 0: " + length);
            writeInt8Array(null);
            return 0;
        } else if (length <= Integer.MAX_VALUE) {
            writeInt8Array(in, (int) length);
            return length;
        } else {
            Output out = writeType(PDS.TYPE_BLOB);
            out.writeInt64Bits(length);
            long count = out.transferFrom(in, length);
            if (count == length) return count; else if (count < length) throw new EOFException(); else throw new AssertionError();
        }
    }

    public final long writeBlob(Blob blob) throws IOException, SQLException {
        if (blob == null) {
            writeInt8Array(null);
            return 0;
        } else {
            long length = blob.length();
            if (length == 0) {
                writeInt8Array(Bytes.EMPTY_ARRAY);
                return 0;
            } else {
                InputStream in = blob.getBinaryStream();
                try {
                    long count = writeBlob(in, length);
                    in.close();
                    return count;
                } catch (IOException ex) {
                    try {
                        in.close();
                    } finally {
                        throw ex;
                    }
                }
            }
        }
    }

    @Override
    public final void writeBoolean(final boolean v) throws IOException {
        writeType(PDS.TYPE_BOOL).write(v ? 1 : 0);
    }

    public final void writeBoolean(final Boolean v) throws IOException {
        writeType(PDS.TYPE_BOOL).write((v == null) ? PDS.NULL : v ? 1 : 0);
    }

    public final void writeBooleanArray(final boolean[] a) throws IOException {
        if (a != null) writeBooleanArray(a, 0, a.length); else writeType(PDS.TYPE_ARRAY_BOOL).writeNull();
    }

    public final void writeBooleanArray(final boolean[] a, final int offs, final int len) throws IOException {
        CheckIOArg.range(a.length, offs, len);
        final Output out = writeType(PDS.TYPE_ARRAY_BOOL);
        out.writeInt32Bits(len);
        out.write(a, offs, len);
    }

    @Override
    public final void writeByte(final int v) throws IOException {
        writeInt8(v);
    }

    @Override
    public final void writeBytes(final String s) throws IOException {
        for (int i = 0, hi = s.length(); i < hi; i++) writeByte((byte) s.charAt(i));
    }

    @Override
    public final void writeChar(final int v) throws IOException {
        writeCharUTF(v);
    }

    @Override
    public final void writeChars(final String s) throws IOException {
        for (int i = 0, hi = s.length(); i < hi; i++) writeChar(s.charAt(0));
    }

    public final void writeChar8(final int v) throws IOException {
        writeType(PDS.TYPE_CHAR8).writeInt8Bits(v);
    }

    public final void writeChar8(final Character v) throws IOException {
        final Output out = writeType(PDS.TYPE_CHAR8);
        if (v != null) writeType(PDS.TYPE_CHAR8).writeInt16Bits(v.charValue()); else writeType(PDS.TYPE_CHAR8).writeNull();
    }

    public final void writeChar16(final int v) throws IOException {
        CheckArg.between(v, 0, 65535, "v");
        writeType(PDS.TYPE_CHAR16).writeInt16Bits(v);
    }

    public final void writeChar16(final Character v) throws IOException {
        final Output out = writeType(PDS.TYPE_CHAR16);
        if (v != null) out.writeInt16Bits(v.charValue()); else out.writeNull();
    }

    public final void writeChar16Array(CharSequence s) throws IOException {
        if (s == null) writeVoid(); else writeChar16Array(s, 0, s.length());
    }

    public final void writeChar16Array(CharSequence s, int offs, int len) throws IOException {
        CheckIOArg.range(s.length(), offs, len);
        final Output out = writeType(PDS.TYPE_ARRAY_CHAR16);
        out.writeInt32Bits(len);
        out.writeChars(s, offs, len);
    }

    public final void writeChar16Array(String s) throws IOException {
        if (s == null) writeVoid(); else writeChar16Array(s, 0, s.length());
    }

    public final void writeChar16Array(String s, int offs, int len) throws IOException {
        CheckIOArg.range(s.length(), offs, len);
        final Output out = writeType(PDS.TYPE_ARRAY_CHAR16);
        out.writeInt32Bits(len);
        out.writeChars(s, offs, len);
    }

    public final void writeCharUTF(final int c) throws IOException {
        CheckArg.between(c, 0, 65535, "c");
        writeType(PDS.TYPE_UTF).writeCharUTF(c);
    }

    public void writeClob(Reader in) throws IOException {
        writeClob(in, -1);
    }

    public void writeClob(Reader in, long charCount) throws IOException {
        final Output out = writeType(PDS.TYPE_CLOB);
        if (in == null) {
            out.write(PDS.NULL);
            out.write(0);
        } else if (charCount < 0) {
            out.writeInt64Bits(-1);
            char[] buf = new char[PDS.DEFAULT_SUBSTREAM_BUFFER_SIZE];
            int offs = 0;
            while (true) {
                final int step = in.read(buf, offs, PDS.DEFAULT_SUBSTREAM_BUFFER_SIZE - offs);
                if (step > 0) {
                    offs += step;
                    if (offs == PDS.DEFAULT_SUBSTREAM_BUFFER_SIZE) {
                        ByteOrders.BIG_ENDIAN.writeShort(out, PDS.DEFAULT_SUBSTREAM_BUFFER_SIZE);
                        out.writeCharsUTF(buf, 0, PDS.DEFAULT_SUBSTREAM_BUFFER_SIZE);
                        offs = 0;
                    }
                    continue;
                } else if (step < 0) {
                    break;
                } else {
                    buf = null;
                    throw new ReturnValueException(in, "read(byte[])", 0, "!= 0");
                }
            }
            if (offs > 0) {
                ByteOrders.BIG_ENDIAN.writeShort(out, offs);
                out.writeCharsUTF(buf, 0, offs);
            }
            buf = null;
            ByteOrders.BIG_ENDIAN.writeShort(out, 0);
        } else {
            out.writeInt64Bits(charCount);
            char[] buf = new char[(int) Math.min(charCount, PDS.DEFAULT_SUBSTREAM_BUFFER_SIZE)];
            while (true) {
                final int maxStep = (int) Math.min(charCount, buf.length);
                if (maxStep > 0) {
                    final int step = in.read(buf, 0, maxStep);
                    if (step > 0) {
                        out.writeCharsUTF(buf, 0, step);
                        charCount -= step;
                        continue;
                    } else if (step < 0) {
                        break;
                    } else {
                        buf = null;
                        throw new ReturnValueException(in, "read(char[])", 0, "!= 0");
                    }
                } else {
                    break;
                }
            }
            if (charCount > 0) throw new EOFException();
        }
    }

    @Override
    public final void writeDouble(final double v) throws IOException {
        writeFloat64(v);
    }

    @Override
    public final void writeFloat(final float v) throws IOException {
        writeFloat32(v);
    }

    @Override
    public final void writeInt(final int v) throws IOException {
        writeInt32(v);
    }

    public final void writeInt8(final int v) throws IOException {
        writeType(PDS.TYPE_INT8).writeInt8Bits(v);
    }

    public final void writeInt8(final Byte v) throws IOException {
        if (v != null) writeType(PDS.TYPE_INT8).writeInt8Bits(v.byteValue()); else writeType(PDS.TYPE_INT8).writeNull();
    }

    public final void writeInt8Array(final byte[] a) throws IOException {
        if (a != null) writeInt8Array(a, 0, a.length); else writeType(PDS.TYPE_ARRAY_INT8).writeNull();
    }

    public final void writeInt8Array(final byte[] a, final int offs, final int len) throws IOException {
        CheckIOArg.range(a, offs, len);
        final Output out = writeType(PDS.TYPE_ARRAY_INT8);
        out.writeInt32Bits(len);
        out.write(a, offs, len);
    }

    public final void writeInt8Array(final InputStream in, final int length) throws IOException {
        CheckArg.notNegative(length, "length");
        final Output out = writeType(PDS.TYPE_ARRAY_INT8);
        if (in == null) {
            out.writeNull();
        } else {
            out.writeInt32Bits(length);
            if (out.transferFrom(in, length) < length) throw new EOFException();
        }
    }

    public final void writeDate(final long millis) throws IOException {
        writeType(PDS.TYPE_DATE).writeInt64Bits(millis);
    }

    public final void writeDate(final java.sql.Date v) throws IOException {
        if (v != null) writeType(PDS.TYPE_DATE).writeInt64Bits(v.getTime()); else writeType(PDS.TYPE_DATE).writeNull();
    }

    public final void writeDateTime(final long millis) throws IOException {
        writeType(PDS.TYPE_DATETIME).writeDateTimeBits(millis);
    }

    public final void writeDateTime(final Date v) throws IOException {
        if (v != null) writeType(PDS.TYPE_DATETIME).writeDateTimeBits(v); else writeType(PDS.TYPE_DATETIME).writeNull();
    }

    public final void writeTime(final long millis) throws IOException {
        writeType(PDS.TYPE_TIME).writeInt64Bits(millis);
    }

    public final void writeTime(final java.sql.Time v) throws IOException {
        if (v != null) writeType(PDS.TYPE_TIME).writeInt64Bits(v.getTime()); else writeType(PDS.TYPE_TIME).writeNull();
    }

    public final void writeFloat32(final float v) throws IOException {
        writeType(PDS.TYPE_FLOAT32).writeInt32Bits(Float.floatToIntBits(v));
    }

    public final void writeFloat32(final Float v) throws IOException {
        if (v != null) writeType(PDS.TYPE_FLOAT32).writeInt32Bits(Float.floatToIntBits(v.floatValue())); else writeType(PDS.TYPE_FLOAT32).writeNull();
    }

    public final void writeFloat32Array(final float[] a) throws IOException {
        if (a != null) writeFloat32Array(a, 0, a.length); else writeType(PDS.TYPE_ARRAY_FLOAT32).writeNull();
    }

    public final void writeFloat32Array(float[] a, int offs, int len) throws IOException {
        CheckIOArg.range(a.length, offs, len);
        final Output out = writeType(PDS.TYPE_ARRAY_FLOAT32);
        out.writeInt32Bits(len);
        out.write(a, offs, len);
    }

    public final void writeFloat64(final double v) throws IOException {
        writeType(PDS.TYPE_FLOAT64).writeInt64Bits(Double.doubleToLongBits(v));
    }

    public final void writeFloat64(final Double v) throws IOException {
        if (v != null) writeType(PDS.TYPE_FLOAT64).writeInt64Bits(Double.doubleToLongBits(v.doubleValue())); else writeType(PDS.TYPE_FLOAT64).writeNull();
    }

    public final void writeFloat64Array(final double[] a) throws IOException {
        if (a != null) writeFloat64Array(a, 0, a.length); else writeType(PDS.TYPE_ARRAY_FLOAT64).writeNull();
    }

    public final void writeFloat64Array(double[] a, int offs, int len) throws IOException {
        CheckIOArg.range(a.length, offs, len);
        final Output out = writeType(PDS.TYPE_ARRAY_FLOAT64);
        out.writeInt32Bits(len);
        out.write(a, offs, len);
    }

    public final void writeInt16(final int v) throws IOException {
        writeType(PDS.TYPE_INT16).writeInt16Bits(v);
    }

    public final void writeInt16(final Short v) throws IOException {
        if (v != null) writeType(PDS.TYPE_INT16).writeInt16Bits(v.intValue()); else writeType(PDS.TYPE_INT16).writeNull();
    }

    public final void writeInt16Array(final short[] a) throws IOException {
        if (a != null) writeInt16Array(a, 0, a.length); else writeType(PDS.TYPE_ARRAY_INT16).writeNull();
    }

    public final void writeInt16Array(short[] a, int offs, int len) throws IOException {
        CheckIOArg.range(a.length, offs, len);
        final Output out = writeType(PDS.TYPE_ARRAY_INT16);
        out.writeInt32Bits(len);
        out.write(a, offs, len);
    }

    public final void writeInt32(final int v) throws IOException {
        writeType(PDS.TYPE_INT32).writeInt32Bits(v);
    }

    public final void writeInt32(final Integer v) throws IOException {
        if (v != null) writeType(PDS.TYPE_INT32).writeInt32Bits(v.intValue()); else writeType(PDS.TYPE_INT32).writeNull();
    }

    public final void writeInt32Array(final int[] a) throws IOException {
        if (a != null) writeInt32Array(a, 0, a.length); else writeType(PDS.TYPE_ARRAY_INT32).writeNull();
    }

    public final void writeInt32Array(int[] a, int offs, int len) throws IOException {
        CheckIOArg.range(a.length, offs, len);
        final Output out = writeType(PDS.TYPE_ARRAY_INT32);
        out.writeInt32Bits(len);
        out.write(a, offs, len);
    }

    public final void writeInt64(final long v) throws IOException {
        writeType(PDS.TYPE_INT64).writeInt64Bits(v);
    }

    public final void writeInt64(final Long v) throws IOException {
        if (v != null) writeType(PDS.TYPE_INT64).writeInt64Bits(v.longValue()); else writeType(PDS.TYPE_INT64).writeNull();
    }

    public final void writeInt64Array(final long[] a) throws IOException {
        if (a != null) writeInt64Array(a, 0, a.length); else writeType(PDS.TYPE_ARRAY_INT64).writeNull();
    }

    public final void writeInt64Array(long[] a, int offs, int len) throws IOException {
        CheckIOArg.range(a.length, offs, len);
        final Output out = writeType(PDS.TYPE_ARRAY_INT64);
        out.writeInt32Bits(len);
        out.write(a, offs, len);
    }

    @Override
    public final void writeLong(final long v) throws IOException {
        writeInt64(v);
    }

    @Override
    public final void writeShort(final int v) throws IOException {
        writeInt16(v);
    }

    private void writeObject(final Object v, final boolean shared) throws IOException {
        if (v == null) {
            writeVoid();
        } else {
            final PDSType pdsType = PDSType.forJavaObject(v);
            switch(pdsType) {
                case BOOL:
                    writeBoolean(((Boolean) v).booleanValue());
                    break;
                case CHAR8:
                case CHAR16:
                case UTF:
                    writeChar(((Character) v).charValue());
                    break;
                case INT8:
                    writeInt8(((Number) v).byteValue());
                    break;
                case INT16:
                    writeInt16(((Number) v).shortValue());
                    break;
                case INT32:
                    writeInt32(((Number) v).intValue());
                    break;
                case INT64:
                    writeInt64(((Number) v).longValue());
                    break;
                case FLOAT32:
                    writeFloat32(((Number) v).floatValue());
                    break;
                case FLOAT64:
                    writeFloat64(((Number) v).doubleValue());
                    break;
                case DATETIME:
                    writeDateTime(((Date) v).getTime());
                    break;
                case DATE:
                    writeDate(((Date) v).getTime());
                    break;
                case TIME:
                    writeTime(((Date) v).getTime());
                    break;
                case ARRAY_UTF:
                    {
                        if (v instanceof CharSequence) writeUTFArray((CharSequence) v); else writeUTFArray((char[]) v);
                    }
                    break;
                default:
                    {
                        final Output out = writeType(PDS.TYPE_OBJECT);
                        if (shared) {
                            if (this.objects == null) this.objects = new IdentityHashMap<Object, Integer>();
                            Integer oid = this.objects.get(v);
                            if (oid != null) {
                                out.writeInt(oid.intValue());
                                return;
                            } else {
                                oid = Integer.valueOf(this.objects.size() + 2);
                                this.objects.put(v, oid);
                                out.writeInt(-oid.intValue());
                            }
                        } else {
                            out.writeInt(1);
                        }
                        switch(pdsType) {
                            case ARRAY_BOOL:
                                out.writeInt32Bits(0);
                                writeBooleanArray((boolean[]) v);
                                break;
                            case ARRAY_UTF:
                                out.writeInt32Bits(0);
                                if (v instanceof String) writeUTFArray((String) v); else if (v instanceof CharSequence) writeUTFArray((CharSequence) v); else if (v instanceof char[]) writeUTFArray((char[]) v); else throw new AssertionError(v.getClass());
                                break;
                            case ARRAY_INT8:
                                out.writeInt32Bits(0);
                                writeInt8Array((byte[]) v);
                                break;
                            case ARRAY_INT16:
                                out.writeInt32Bits(0);
                                writeInt16Array((short[]) v);
                                break;
                            case ARRAY_INT32:
                                out.writeInt32Bits(0);
                                writeInt32Array((int[]) v);
                                break;
                            case ARRAY_INT64:
                                out.writeInt32Bits(0);
                                writeInt64Array((long[]) v);
                                break;
                            case ARRAY_FLOAT32:
                                out.writeInt32Bits(0);
                                writeFloat32Array((float[]) v);
                                break;
                            case ARRAY_FLOAT64:
                                out.writeInt32Bits(0);
                                writeFloat64Array((double[]) v);
                                break;
                            case OBJECT_ARRAY:
                                {
                                    out.writeInt32Bits(0);
                                    writeObjectArray((Object[]) v);
                                }
                                break;
                            default:
                                writeClassDescriptor(v.getClass());
                                out.write(PDS.TYPE_OBJECT);
                                if (v instanceof PDSData) {
                                    ((PDSData) v).writePDSData(this);
                                } else if (v instanceof Externalizable) {
                                    ((Externalizable) v).writeExternal(this);
                                } else if (v instanceof Serializable) {
                                    final BeanMapSpec<?> beanMapSpec = BeanMapSpec.getInstance(v.getClass());
                                    final boolean isEntity = beanMapSpec.isEntity();
                                    for (BeanMapSpec.Handler h : beanMapSpec.getHandlers().values()) {
                                        if (!isEntity || h.isPersistent()) writeObject(h.get(v));
                                    }
                                } else {
                                    throw new NotSerializableException("Object is not instance of java.io.Serializable: ".concat(Objects.toIdentityString(v)));
                                }
                                break;
                        }
                        break;
                    }
            }
        }
    }

    public final void writeType(PDSType type) throws IOException {
        if (type != null) writeType(PDS.TYPE_TYPE).writeInt8Bits(type.ordinal()); else writeType(PDS.TYPE_TYPE).writeNull();
    }

    public final void writeTypeArray(PDSType[] v) throws IOException {
        final Output out = writeType(PDS.TYPE_ARRAY_TYPE);
        if (v != null) {
            out.writeInt32Bits(v.length);
            for (PDSType t : v) out.write(t.ordinal);
        } else {
            out.writeNull();
        }
    }

    @Override
    public final void writeUTF(String s) throws IOException {
        writeUTFArray(s);
    }

    public final void writeUTFArray(final char[] s) throws IOException {
        if (s != null) writeUTFArray(s, 0, s.length); else writeType(PDS.TYPE_ARRAY_UTF).writeNull();
    }

    public final void writeUTFArray(final char[] s, int offs, final int len) throws IOException {
        CheckIOArg.range(s, offs, len);
        final Output out = writeType(PDS.TYPE_ARRAY_UTF);
        out.writeInt32Bits(len);
        out.writeCharsUTF(s, offs, offs + len);
    }

    public final void writeUTFArray(final String s) throws IOException {
        writeUTFArrayBody(writeType(PDS.TYPE_ARRAY_UTF), s);
    }

    public void writeUTFArray(final CharSequence s) throws IOException {
        writeUTFArrayBody(writeType(PDS.TYPE_ARRAY_UTF), s);
    }

    public final void writeVoid() throws IOException {
        writeType(PDS.TYPE_VOID);
    }

    public final void writeNull(PDSType type) throws IOException {
        switch(type) {
            case VOID:
                writeVoid();
                return;
            case BLOB:
                writeBlob((InputStream) null);
                return;
            case CLOB:
                writeClob(null);
                return;
            case TABLE:
                beginTable(null);
                return;
            case VARIANT:
                throw new IllegalArgumentException(type.name());
            case TYPE:
                writeType(null);
                return;
            case BOOL:
                writeBoolean(null);
                return;
            case INT8:
                writeInt8(null);
                return;
            case CHAR8:
                writeChar8(null);
                return;
            case CHAR16:
                writeChar16(null);
                return;
            case UTF:
                throw new IllegalArgumentException(type.name());
            case INT16:
                writeInt16(null);
                return;
            case INT32:
                writeInt32(null);
                return;
            case INT64:
                writeInt64(null);
                return;
            case FLOAT32:
                writeFloat32(null);
                return;
            case FLOAT64:
                writeFloat64(null);
                return;
            case BIGINTEGER:
                writeBigInteger(null);
                return;
            case BIGDECIMAL:
                writeBigDecimal(null);
                return;
            case DATETIME:
                writeDateTime(null);
                return;
            case DATE:
                writeDate(null);
                return;
            case TIME:
                writeTime(null);
                return;
            case ARRAY_TYPE:
                writeTypeArray(null);
                return;
            case ARRAY_BOOL:
                writeBooleanArray(null);
                return;
            case ARRAY_INT8:
                writeInt8Array(null);
                return;
            case ARRAY_CHAR8:
            case ARRAY_CHAR16:
            case ARRAY_UTF:
                writeUTFArray((char[]) null);
                return;
            case ARRAY_INT16:
                writeInt16Array(null);
                return;
            case ARRAY_INT32:
                writeInt32Array(null);
                return;
            case ARRAY_INT64:
                writeInt64Array(null);
                return;
            case ARRAY_FLOAT32:
                writeFloat32Array(null);
                return;
            case ARRAY_FLOAT64:
                writeFloat64Array(null);
                return;
            case OBJECT:
                writeObject(null);
                return;
            default:
                throw new AssertionError(type);
        }
    }

    public void writeObject(final Object v) throws IOException {
        writeObject(v, true);
    }

    public void writeObjectUnshared(final Object v) throws IOException {
        writeObject(v, false);
    }

    public void writeObjectArray(Object[] src) throws IOException {
        if (src == null) {
            writeVoid();
        } else {
            Output out = writeType(PDS.TYPE_OBJECT_ARRAY);
            out.writeInt(src.length);
            for (Object e : src) writeObject(e);
        }
    }

    /**
   * Write an 
   */
    public void writeObjectArray(Iterator<?> src) throws IOException {
        if (src == null) {
            writeVoid();
        } else {
            Output out = writeType(PDS.TYPE_OBJECT_ARRAY);
            out.writeInt(-1);
            while (src.hasNext()) writeObject(src.next());
        }
    }

    /**
   * @see BeanProperty.Persistence#isPersistentColumn()
   */
    public <B> int writePersistentColumnsAsTable(final String tableName, final BeanProperties<B> properties, Iterator<? extends B> src) throws IOException {
        if (src == null) {
            beginTable(null);
            return 0;
        } else {
            CheckArg.notNull(properties, "properties");
            final ObjectArray<BeanProperty<B, ?>> propertyList = properties.getPropertyList();
            final int columnCount = properties.getPersistentColumnCount();
            PDSTableHeader header = PDSOutputStream.entityBeanTableHeaders.get(properties.getBeanClass());
            if (header == null) {
                final int[] columnLengths = new int[columnCount];
                final String[] columnNames = new String[columnCount];
                final PDSType[] columnTypes = new PDSType[columnCount];
                for (int i = propertyList.size(), col = columnCount; --i >= 0; ) {
                    final BeanProperty<B, ?> p = propertyList.get(i);
                    final BeanProperty.Persistence pp = p.getPersistence();
                    if (pp.isPersistentColumn()) {
                        col--;
                        columnLengths[col] = pp.getColumnLength();
                        columnTypes[col] = PDSType.forJavaType(p.getType());
                        final String columnName = pp.getColumnName();
                        columnNames[col] = (columnName == null) ? p.getName() : columnName;
                    }
                }
                header = new PDSTableHeader(false, null, columnTypes, columnLengths, columnNames);
                PDSOutputStream.entityBeanTableHeaders.putIfAbsent(properties.getBeanClass(), header);
            }
            if (tableName != null) header = new PDSTableHeader(tableName, header, (String[]) null);
            final PDSOutputStream.Table table = beginTable(header);
            try {
                int rowCount = 0;
                while (src.hasNext()) {
                    rowCount++;
                    table.nextRow();
                    final B bean = src.next();
                    if (bean == null) {
                        for (int col = 0; col < columnCount; col++) table.writeValue(null);
                    } else {
                        for (int i = 0, hi = propertyList.size(); i < hi; i++) {
                            final BeanProperty<B, ?> p = propertyList.get(i);
                            final BeanProperty.Persistence pp = p.getPersistence();
                            if (pp.isPersistentColumn()) table.writeValue(propertyList.get(i).get(bean));
                        }
                    }
                }
                return rowCount;
            } finally {
                src = null;
                table.closeInstance();
            }
        }
    }

    public <B> int writePersistentPropertiesAsTable(final BeanProperties<B> properties, final Iterator<? extends B> src) throws IOException {
        return writePersistentPropertiesAsTable(null, properties, src);
    }

    /**
   * @see BeanProperty.Persistence#isPersistentColumn()
   */
    public <B> int writePersistentPropertiesAsTable(final String tableName, final BeanProperties<B> properties, Iterator<? extends B> src) throws IOException {
        if (src == null) {
            beginTable(null);
            return 0;
        } else {
            CheckArg.notNull(properties, "properties");
            final ObjectArray<BeanProperty<B, ?>> propertyList = properties.getPropertyList();
            final int columnCount = properties.getPersistentPropertyCount();
            PDSTableHeader header = PDSOutputStream.beanTableHeaders.get(properties.getBeanClass());
            if (header == null) {
                final int[] columnLengths = new int[columnCount];
                final String[] columnNames = new String[columnCount];
                final PDSType[] columnTypes = new PDSType[columnCount];
                for (int col = columnCount; --col >= 0; ) {
                    final BeanProperty<B, ?> p = propertyList.get(col);
                    final BeanProperty.Persistence pp = p.getPersistence();
                    if (!pp.isTransient()) {
                        columnLengths[col] = pp.getColumnLength();
                        columnTypes[col] = PDSType.forJavaType(p.getType());
                        columnNames[col] = p.getName();
                    }
                }
                header = new PDSTableHeader(false, null, columnTypes, columnLengths, columnNames);
                PDSOutputStream.beanTableHeaders.putIfAbsent(properties.getBeanClass(), header);
            }
            if (tableName != null) header = new PDSTableHeader(tableName, header, (String[]) null);
            final PDSOutputStream.Table table = beginTable(header);
            try {
                int rowCount = 0;
                while (src.hasNext()) {
                    rowCount++;
                    table.nextRow();
                    final B bean = src.next();
                    if (bean == null) {
                        for (int col = 0; col < columnCount; col++) table.writeValue(null);
                    } else {
                        for (int col = 0; col < columnCount; col++) table.writeValue(propertyList.get(col).get(bean));
                    }
                }
                return rowCount;
            } finally {
                src = null;
                table.closeInstance();
            }
        }
    }

    /**
   * Writes the specified collection of collections as rows of a table.
   * Each inner collection represents one row in the table.
   * All rows must be of same size as the specified arrays of column names and types.
   *
   * @param rows 
   *  the iterator delivering the rows to write.
   * @param rowCount
   *  the exact number of rows to write, or negative to write all rows remaining in the iteration.
   *
   * @return
   *  the number of rows written.
   */
    public int writeTable(final String tableName, final String[] columnNames, final PDSType[] columnTypes, Iterator<? extends Iterable<?>> rows, int rowCount) throws IOException {
        PDSTableHeader header = new PDSTableHeader(tableName, columnTypes, null, columnNames);
        PDSOutputStream.Table table = beginTable(header, rowCount);
        int rowsWritten = 0;
        if (rowCount != 0) {
            while (rows.hasNext()) {
                Iterable<?> row = rows.next();
                table.nextRow();
                Iterator<?> columns = row.iterator();
                for (int i = columnTypes.length; --i >= 0; ) table.writeValue(columns.next());
                if (++rowsWritten == rowCount) break;
            }
            table.closeInstance();
        }
        if (rowsWritten < rowCount) {
            throw new IllegalArgumentException(Strings.concat("Less than the specified number of rows were remaining in the iterator:" + "\n  specified = ", Integer.toString(rowCount), "\n  actual    = ", Integer.toString(rowsWritten)));
        }
        return rowsWritten;
    }

    /**
   * Writes the specified collection of collections as rows of a table.
   * Each inner collection represents one row in the table.
   * All rows must be of same size as the specified arrays of column names and types.
   *
   * @param rows 
   *  the iterator delivering the rows to write.
   * @param rowCount
   *  the exact number of rows to write, or negative to write all rows remaining in the iteration.
   *
   * @return
   *  the number of rows written.
   */
    public int writeDenseTable(PDSTableHeader header, List<?> data, boolean nullifyData, int tempMaximumBufferCapacity) throws IOException {
        CheckArg.notNull(header, "header");
        CheckArg.notNull(data, "data");
        final int columnCount = header.getColumnCount();
        final int rowCount = data.size() / columnCount;
        PDSOutputStream.Output out = this.out;
        if (out == null) throw new IOException("closed");
        final int oldMaxBufferCapacity = out.getMaxBufferCapacity();
        if (tempMaximumBufferCapacity > oldMaxBufferCapacity) out.setMaxBufferCapacity(tempMaximumBufferCapacity);
        boolean ok = false;
        try {
            final PDSOutputStream.Table table = beginTable(header, rowCount);
            int rowsWritten = 0;
            if (rowCount > 0) {
                ListIterator<?> it = data.listIterator(0);
                while (rowsWritten < rowCount) {
                    rowsWritten++;
                    table.nextRow();
                    for (int i = columnCount; --i >= 0; ) {
                        final Object v = it.next();
                        table.writeValue(v);
                        if (nullifyData && (v != null)) it.set(null);
                    }
                }
            }
            table.closeInstance();
            ok = true;
            return rowsWritten;
        } finally {
            if (ok) {
                out.setMaxBufferCapacity(oldMaxBufferCapacity);
            } else {
                try {
                    out.setMaxBufferCapacity(oldMaxBufferCapacity);
                } catch (IOException ex) {
                }
            }
        }
    }

    public void writeTable(final ResultSet rs) throws IOException, SQLException {
        writeTable(null, rs);
    }

    public void writeTable(final String tableName, final ResultSet rs) throws IOException, SQLException {
        if (rs == null) {
            beginTable(null);
        } else {
            ResultSetMetaData rsmeta = rs.getMetaData();
            final int columnCount = rsmeta.getColumnCount();
            PDSType[] columnTypes = new PDSType[columnCount];
            String[] columnNames = new String[columnCount];
            int[] columnLengths = new int[columnCount];
            for (int i = 0; i < columnCount; i++) {
                columnNames[i] = rsmeta.getColumnName(i + 1);
                columnLengths[i] = rsmeta.getColumnDisplaySize(i + 1);
                String classname = rsmeta.getColumnClassName(i + 1);
                Class clazz;
                try {
                    clazz = Class.forName(classname);
                } catch (ClassNotFoundException ex) {
                    throw (SQLException) new SQLException("class not found").initCause(ex);
                }
                columnTypes[i] = PDSType.forJavaType(clazz);
            }
            rsmeta = null;
            PDSTableHeader header = new PDSTableHeader(false, tableName, columnTypes, columnLengths, columnNames);
            columnNames = null;
            columnLengths = null;
            PDSOutputStream.Table table = beginTable(header);
            header = null;
            while (rs.next()) {
                table.nextRow();
                for (int i = 1; i <= columnCount; i++) table.writeValue(rs.getObject(i), columnTypes[i - 1]);
            }
            table.closeInstance();
        }
    }

    public void writeTable(String tableName, jaxlib.col.table.Table<?> tab) throws IOException {
        if (tab == null) {
            beginTable(null);
        } else {
            final int columnCount = tab.getColumnCount();
            PDSType[] columnTypes = new PDSType[columnCount];
            String[] columnNames = new String[columnCount];
            for (int col = 0; col < columnCount; col++) {
                columnNames[col] = tab.getColumnName(col);
                columnTypes[col] = PDSType.forJavaType(tab.getColumnClass(col));
            }
            PDSTableHeader header = new PDSTableHeader(false, tableName, columnTypes, null, columnNames);
            columnTypes = null;
            columnNames = null;
            PDSOutputStream.Table table = beginTable(header);
            header = null;
            for (int row = 0, rowCount = tab.getRowCount(); row < rowCount; row++) {
                table.nextRow();
                for (int col = 0; col < columnCount; col++) table.writeValue(tab.get(row, col));
            }
            table.closeInstance();
        }
    }

    public void writeTable(String tableName, TableModel model) throws IOException {
        if (model == null) {
            beginTable(null);
        } else {
            final int columnCount = model.getColumnCount();
            PDSType[] columnTypes = new PDSType[columnCount];
            String[] columnNames = new String[columnCount];
            for (int col = 0; col < columnCount; col++) {
                columnNames[col] = model.getColumnName(col);
                columnTypes[col] = PDSType.forJavaType(model.getColumnClass(col));
            }
            PDSTableHeader header = new PDSTableHeader(false, tableName, columnTypes, null, columnNames);
            columnTypes = null;
            columnNames = null;
            PDSOutputStream.Table table = beginTable(header);
            header = null;
            for (int row = 0, rowCount = model.getRowCount(); row < rowCount; row++) {
                table.nextRow();
                for (int col = 0; col < columnCount; col++) table.writeValue(model.getValueAt(row, col));
            }
            table.closeInstance();
        }
    }

    public void writeValue(final Object v) throws IOException {
        writeValue(v, PDSType.forJavaObject(v));
    }

    final void writeValue(final Object v, final PDSType type) throws IOException {
        switch(type) {
            case VOID:
                writeVoid();
                return;
            case BLOB:
                {
                    if ((v == null) || (v instanceof Blob)) {
                        try {
                            writeBlob((Blob) v);
                        } catch (SQLException ex) {
                            throw (IOException) new IOException(ex.getMessage()).initCause(ex);
                        }
                    } else if (v instanceof InputStream) {
                        writeBlob((InputStream) v);
                    } else {
                        throw new AssertionError(v);
                    }
                }
                return;
            case CLOB:
                {
                    if (v == null) {
                        writeClob(null);
                    } else if (v instanceof Clob) {
                        Clob clob = (Clob) v;
                        try {
                            writeClob(clob.getCharacterStream(), clob.length());
                        } catch (SQLException ex) {
                            throw (IOException) new IOException(ex.getMessage()).initCause(ex);
                        }
                    } else if (v instanceof Reader) {
                        writeClob((Reader) v);
                    } else {
                        throw new AssertionError(v);
                    }
                }
                return;
            case TABLE:
                {
                    if (v instanceof ResultSet) {
                        try {
                            writeTable((ResultSet) v);
                        } catch (SQLException ex) {
                            throw (IOException) new IOException().initCause(ex);
                        }
                    } else if (v instanceof jaxlib.col.table.Table) {
                        writeTable(null, (jaxlib.col.table.Table) v);
                    } else {
                        writeTable(null, (TableModel) v);
                    }
                }
                return;
            case VARIANT:
                throw new AssertionError(type);
            case TYPE:
                writeType((PDSType) v);
                return;
            case BOOL:
                writeBoolean((Boolean) v);
                return;
            case INT8:
                if (v == null) writeInt8(null); else writeInt8(((Number) v).intValue());
                return;
            case CHAR8:
                throw new AssertionError();
            case CHAR16:
                writeChar16((Character) v);
                return;
            case UTF:
                writeCharUTF((Character) v);
                return;
            case INT16:
                if (v == null) writeInt16(null); else writeInt16(((Number) v).intValue());
                return;
            case INT32:
                if (v == null) writeInt32(null); else writeInt32(((Number) v).intValue());
                return;
            case INT64:
                if (v == null) writeInt64(null); else writeInt64(((Number) v).longValue());
                return;
            case FLOAT32:
                if (v == null) writeFloat32(null); else writeFloat32(((Number) v).floatValue());
                return;
            case FLOAT64:
                if (v == null) writeFloat64(null); else writeFloat64(((Number) v).doubleValue());
                return;
            case BIGINTEGER:
                writeBigInteger((BigInteger) v);
                return;
            case BIGDECIMAL:
                writeBigDecimal((BigDecimal) v);
                return;
            case DATETIME:
                writeDateTime((Date) v);
                return;
            case DATE:
                writeDate((java.sql.Date) v);
                return;
            case TIME:
                writeTime((java.sql.Time) v);
                return;
            case ARRAY_TYPE:
                writeTypeArray((PDSType[]) v);
                return;
            case ARRAY_BOOL:
                writeBooleanArray((boolean[]) v);
                return;
            case ARRAY_INT8:
                {
                    if ((v == null) || (v instanceof byte[])) {
                        writeInt8Array((byte[]) v);
                    } else if (v instanceof Blob) {
                        try {
                            Blob blob = (Blob) v;
                            long length = blob.length();
                            if (length <= Integer.MAX_VALUE) writeInt8Array(blob.getBinaryStream(), (int) length); else throw new IOException("Blob too big for array: " + length);
                        } catch (SQLException ex) {
                            throw (IOException) new IOException(ex.getMessage()).initCause(ex);
                        }
                    } else {
                        throw new AssertionError(v.getClass());
                    }
                }
                return;
            case ARRAY_CHAR8:
            case ARRAY_CHAR16:
                throw new AssertionError(type);
            case ARRAY_UTF:
                if ((v == null) || (v instanceof CharSequence)) writeUTFArray((CharSequence) v); else if (v instanceof char[]) writeUTFArray((char[]) v); else {
                    Clob clob = (Clob) v;
                    try {
                        int len = (int) clob.length();
                        if (len == 0) {
                            writeUTFArray("");
                        } else {
                            writeUTFArray(clob.getSubString(1, (int) len));
                        }
                    } catch (SQLException ex) {
                        throw (IOException) new IOException().initCause(ex);
                    }
                }
                return;
            case ARRAY_INT16:
                writeInt16Array((short[]) v);
                return;
            case ARRAY_INT32:
                writeInt32Array((int[]) v);
                return;
            case ARRAY_INT64:
                writeInt64Array((long[]) v);
                return;
            case ARRAY_FLOAT32:
                writeFloat32Array((float[]) v);
                return;
            case ARRAY_FLOAT64:
                writeFloat64Array((double[]) v);
                return;
            case DATA:
                ((PDSData) v).writePDSData(this);
                return;
            case OBJECT:
                writeObject(v);
                return;
            default:
                throw new AssertionError(type);
        }
    }

    public static final class Output extends BufferedXOutputStream {

        static final int MAX_BLOCK_SIZE = 0xffff;

        static final int NULL_BYTES = PDS.NULL << 8;

        private int blockPosition = -1;

        private int flags;

        private OutputStream out;

        private java.util.Date tempDate;

        public Output(OutputStream out, int bufferSize) {
            super(out, Math.max(bufferSize, 64));
            this.flags = 1;
            this.out = out;
        }

        public Output(OutputStream out, int bufferSize, int maximumBufferSize) {
            super(out, Math.max(bufferSize, 64), (maximumBufferSize >= 0) ? Math.max(maximumBufferSize, 64) : maximumBufferSize);
            this.flags = 1;
            this.out = out;
        }

        final void beginBlock() throws IOException {
            ByteBuffer buffer = getBuffer();
            if (buffer != null) {
                if (buffer.hasRemaining()) {
                    flushBuffer();
                    buffer = getBuffer();
                }
                if (buffer != null) {
                    this.blockPosition = buffer.position();
                    buffer.putShort((short) 0);
                    return;
                }
            }
            throw new IOException("closed");
        }

        final void endBlock() throws IOException {
            int blockPosition = this.blockPosition;
            if (blockPosition >= 0) {
                this.blockPosition = -1;
                ByteBuffer buffer = getBuffer();
                if (buffer != null) {
                    buffer.putShort(blockPosition, (short) ((buffer.position() - blockPosition) - 2));
                    buffer = null;
                    writeShort(0);
                } else {
                    throw new IOException("closed");
                }
            }
        }

        final void beginPacket() throws IOException {
            write('P');
            write('D');
            write('S');
            write(' ');
            write(1);
            write(0);
            write(this.flags);
            write(0);
        }

        final void writeDateTimeBits(long millis) throws IOException {
            if ((this.flags & PDS.FLAG_SHIFT_TIMEZONE) != 0) {
                if (this.tempDate == null) this.tempDate = new java.util.Date(millis); else this.tempDate.setTime(millis);
                millis -= this.tempDate.getTimezoneOffset() * 60 * 1000;
            }
            writeInt64Bits(millis);
        }

        final void writeDateTimeBits(final java.util.Date v) throws IOException {
            long millis = v.getTime();
            if ((this.flags & PDS.FLAG_SHIFT_TIMEZONE) != 0) millis -= v.getTimezoneOffset() * 60 * 1000;
            writeInt64Bits(millis);
        }

        final void writeInt8Bits(final int v) throws IOException {
            write(v);
            if ((v & 0xff) == PDS.NULL) write(0x01);
        }

        final void writeInt16Bits(final int v) throws IOException {
            if (((v >>> 8) & 0xff) != PDS.NULL) {
                writeShort(v);
            } else {
                final int b0 = ((v >>> 0) & 0xff);
                if ((b0 != 0) && (b0 != 1)) {
                    writeShort(v);
                } else {
                    write(PDS.NULL);
                    write(0x01);
                    write(b0);
                }
            }
        }

        final void writeInt32Bits(final int v) throws IOException {
            if (((v >>> 24) & 0xff) != PDS.NULL) {
                writeInt(v);
            } else {
                final int b2 = ((v >>> 16) & 0xff);
                if ((b2 != 0) && (b2 != 1)) {
                    writeInt(v);
                } else {
                    write(PDS.NULL);
                    write(0x01);
                    write(b2);
                    write((v >>> 8) & 0xff);
                    write((v >>> 0) & 0xff);
                }
            }
        }

        final void writeInt64Bits(final long v) throws IOException {
            if (((int) ((v >>> 56) & 0xff)) != PDS.NULL) {
                writeLong(v);
            } else {
                final int b6 = (int) ((v >>> 48) & 0xff);
                if ((b6 != 0) && (b6 != 1)) {
                    writeLong(v);
                } else {
                    write(PDS.NULL);
                    write(0x01);
                    write(b6);
                    write((int) ((v >>> 40) & 0xff));
                    write((int) ((v >>> 32) & 0xff));
                    write((int) ((v >>> 24) & 0xff));
                    write((int) ((v >>> 16) & 0xff));
                    write((int) ((v >>> 8) & 0xff));
                    write((int) ((v >>> 0) & 0xff));
                }
            }
        }

        final void writeNull() throws IOException {
            writeShort(Output.NULL_BYTES);
        }

        @Override
        protected void flushBuffer(ByteBuffer buffer, int minFree) throws IOException {
            if (this.blockPosition < 0) {
                flushBuffer0(buffer, minFree);
            } else {
                if (minFree < 0) minFree = 1;
                if (buffer.remaining() < minFree) {
                    final int capacity = buffer.capacity();
                    if (minFree <= capacity) {
                        OutputStream out = this.out;
                        if (out != null) {
                            do {
                                int position = buffer.position();
                                final int step = Math.min((position - this.blockPosition) - 2, MAX_BLOCK_SIZE);
                                buffer.putShort(this.blockPosition, (short) step);
                                flushBuffer0(buffer, (capacity - position) + step);
                                this.blockPosition = buffer.position();
                            } while (capacity - buffer.position() < minFree);
                        } else {
                            throw new IOException("closed");
                        }
                    } else {
                        throw new IllegalArgumentException("minFree(" + minFree + ") > capacity(" + capacity + ")");
                    }
                }
            }
        }

        private void flushBuffer0(ByteBuffer buffer, int minFree) throws IOException {
            super.flushBuffer(buffer, minFree);
        }

        @Override
        public void closeInstance() throws IOException {
            OutputStream out = this.out;
            if (out != null) {
                endBlock();
                super.closeInstance();
            }
        }

        /**
     * Unsupported.
     *
     * @throws UnsupportedOperationException
     *  Always.
     */
        @Override
        public void setByteOrder(ByteOrder order) {
            throw new UnsupportedOperationException();
        }

        public void setShiftTimeZone(boolean v) {
            if (v) this.flags |= PDS.FLAG_SHIFT_TIMEZONE; else this.flags &= ~PDS.FLAG_SHIFT_TIMEZONE;
        }

        @Override
        public void write(final byte[] source, int off, int len) throws IOException {
            if (this.blockPosition < 0) {
                super.write(source, off, len);
            } else {
                CheckIOArg.range(source, off, len);
                while (len > 0) {
                    ByteBuffer buffer = getBuffer();
                    if (buffer == null) throw new IOException("closed");
                    int remaining = buffer.remaining();
                    if (remaining > 0) {
                        final int step = Math.min(len, remaining);
                        buffer.put(source, off, step);
                        off += step;
                        len -= step;
                    } else {
                        flushBuffer(buffer, 1);
                    }
                }
            }
        }

        @Override
        public int write(ByteBuffer source) throws IOException {
            if (this.blockPosition < 0) {
                return super.write(source);
            } else {
                final int count = source.remaining();
                for (int len = count; len > 0; ) {
                    ByteBuffer buffer = getBuffer();
                    if (buffer == null) throw new IOException("closed");
                    int remaining = buffer.remaining();
                    if (remaining > 0) {
                        final int step = Math.min(len, remaining);
                        final int lim = source.limit();
                        source.limit(source.position() + step);
                        buffer.put(source);
                        source.limit(lim);
                        len -= step;
                    } else {
                        flushBuffer(buffer, 1);
                    }
                }
                return count;
            }
        }
    }

    private static final class SubStream extends AdapterOutputStream {

        private PDSOutputStream parent;

        private final int initialParentState;

        protected SubStream(final PDSOutputStream parent) throws IOException {
            super(parent.out);
            this.parent = parent;
            this.initialParentState = parent.state;
            parent.state = PDSOutputStream.STATE_IN_SUBSTREAM;
            parent.out.write(0);
            parent.out.beginBlock();
        }

        @Override
        public void close() throws IOException {
            closeInstance();
        }

        @Override
        public void closeInstance() throws IOException {
            PDSOutputStream parent = this.parent;
            if (parent != null) {
                setOut(null);
                this.parent = null;
                parent.out.endBlock();
                if (parent.state == PDSOutputStream.STATE_IN_SUBSTREAM) parent.state = this.initialParentState;
            }
        }
    }

    /**
   * An output stream for {@code PDS} tables.
   * <p>
   * The {@link #close()} method of this stream does not close the parent {@link PDSOutputStream}.
   * </p>
   *
   * @see PDSOutputStream#beginTable(PDSTableHeader)
   *
   * @since JaXLib 1.0
   */
    public static final class Table extends PDSOutputStream {

        private PDSOutputStream parent;

        private final int initialParentState;

        private int rowIndex;

        private Table(PDSOutputStream parent, PDSTableHeader header, int initialParentState) {
            super(parent, header);
            this.parent = parent;
            this.initialParentState = initialParentState;
            this.columnIndex = this.columnCount;
        }

        /**
     * Closes this table, but the parent {@code PDSOutputStream} and its underlying stream only if 
     * {@code exitCode} is not zero.
     *
     * @throws IOException
     *  if an I/O error occurs.
     * @throws IllegalStateException
     *  if there are columns remaining in the current row.
     */
        @Override
        public final void close() throws IOException {
            closeInstance();
        }

        /**
     * Closes this table, but the parent {@code PDSOutputStream} only if {@code exitCode} is not zero.
     * However, never closes the parent's underlying stream.
     *
     * @throws IOException
     *  if an I/O error occurs.
     * @throws IllegalStateException
     *  if (exitCode != 0) there are columns remaining in the current row.
     */
        @Override
        public final void closeInstance() throws IOException {
            final BufferedXOutputStream out = this.out;
            if (out != null) {
                PDSOutputStream parent = this.parent;
                if (parent == null) throw new ConcurrentModificationException();
                parent.buffer = this.buffer;
                parent.classes = this.classes;
                parent.objects = this.objects;
                this.out = null;
                this.parent = null;
                if (this.columnIndex < this.columnCount) {
                    out.close();
                    throw new IOException("columnIndex (" + this.columnIndex + ") < columnCount (" + this.columnCount + ")");
                } else {
                    this.state = STATE_END;
                    out.write(0);
                    parent.state = this.initialParentState;
                }
            }
        }

        @Override
        public void flush() throws IOException {
            final PDSOutputStream parent = this.parent;
            if (parent != null) parent.flush(); else throw new IOException("table closed");
        }

        /**
     * Returns the index of the next column to be written.
     */
        public final int getColumnIndex() {
            return this.columnIndex;
        }

        /**
     * Returns the index of the current row.
     */
        public final int getRowIndex() {
            return this.rowIndex;
        }

        /**
     * Returns the header of this table.
     */
        public final PDSTableHeader getTableHeader() {
            return this.tableHeader;
        }

        /**
     * Begins the next row.
     *
     * @return
     *  this object.
     *
     * @throws IOException
     *  if an I/O error occurs.
     * @throws IllegalStateException
     *  if there are columns remaining in the current row.
     *
     * @since JaXLib 1.0
     */
        public Table nextRow() throws IOException {
            final BufferedXOutputStream out = ensureOpen();
            if (this.columnIndex == this.columnCount) {
                out.write(1);
                this.columnIndex = 0;
                this.rowIndex++;
                return this;
            } else {
                throw new IllegalStateException("columnIndex (" + this.columnIndex + ") < columnCount (" + this.columnCount + ")");
            }
        }

        @Override
        public void writeValue(final Object v) throws IOException {
            if ((v != null) || (this.columnIndex >= this.columnCount) || (this.columnIndex < 0)) super.writeValue(v); else writeNull(this.tableHeader.getColumnType(this.columnIndex));
        }
    }
}

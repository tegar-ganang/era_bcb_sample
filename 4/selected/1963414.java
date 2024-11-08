package jaxlib.jdbc.tds;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.BufferOverflowException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.RowId;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import javax.annotation.Nullable;
import jaxlib.io.stream.ByteBufferInputStream;
import jaxlib.io.stream.InputStreamXReader;
import jaxlib.io.stream.IOStreams;
import jaxlib.io.stream.OutputStreamXWriter;
import jaxlib.io.stream.ReaderInputStream;
import jaxlib.jaxlib_private.util.UnsafeStringConstructor;
import jaxlib.lang.Bytes;
import jaxlib.lang.Chars;
import jaxlib.lang.Doubles;
import jaxlib.lang.Floats;
import jaxlib.sql.JdbcType;
import jaxlib.sql.SQLTypes;

/**
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: TdsTypes.java 2984 2011-09-13 05:27:36Z joerg_wassmer $
 */
final class TdsTypes extends Object {

    private TdsTypes() {
        super();
    }

    static final int SYBVOID = 31;

    static final int SYBIMAGE = 34;

    static final int SYBTEXT = 35;

    static final int SYBUNIQUE = 36;

    static final int SYBVARBINARY = 37;

    static final int SYBINTN = 38;

    static final int SYBVARCHAR = 39;

    static final int MSDATEN = 40;

    static final int MSTIMEN = 41;

    static final int MSDATETIME2N = 42;

    static final int MSDATETIMEOFFSETN = 43;

    static final int SYBBINARY = 45;

    static final int SYBCHAR = 47;

    static final int SYBINT1 = 48;

    static final int SYBDATE = 49;

    static final int SYBBIT = 50;

    static final int SYBTIME = 51;

    static final int SYBINT2 = 52;

    static final int SYBINT4 = 56;

    static final int SYBDATETIME4 = 58;

    static final int SYBREAL = 59;

    static final int SYBMONEY = 60;

    static final int SYBDATETIME = 61;

    static final int SYBFLT8 = 62;

    static final int SYBSINT1 = 64;

    static final int SYBUINT2 = 65;

    static final int SYBUINT4 = 66;

    static final int SYBUINT8 = 67;

    static final int SYBUINTN = 68;

    static final int SYBVARIANT = 98;

    static final int SYBNTEXT = 99;

    static final int SYBNVARCHAR = 103;

    static final int SYBBITN = 104;

    static final int SYBDECIMAL = 106;

    static final int SYBNUMERIC = 108;

    static final int SYBFLTN = 109;

    static final int SYBMONEYN = 110;

    static final int SYBDATETIMN = 111;

    static final int SYBMONEY4 = 122;

    static final int SYBDATEN = 123;

    static final int SYBINT8 = 127;

    static final int SYBTIMEN = 147;

    static final int XSYBVARBINARY = 165;

    static final int XSYBVARCHAR = 167;

    static final int XSYBBINARY = 173;

    static final int SYBUNITEXT = 174;

    static final int XSYBCHAR = 175;

    static final int SYBSINT8 = 191;

    static final int SYBLONGBINARY = 225;

    static final int XSYBNVARCHAR = 231;

    static final int XSYBNCHAR = 239;

    static final int MSUDT = 240;

    static final int MSXML = 241;

    static final int VAR_MAX = 255;

    static final int MS_LONGVAR_MAX = 8000;

    static final int SYB_CHUNK_SIZE = 8192;

    static final int SYB_LONGVAR_MAX = 16384;

    static final int SYBLONGDATA = 36;

    static final int UDT_CHAR = 1;

    static final int UDT_VARCHAR = 2;

    static final int UDT_BINARY = 3;

    static final int UDT_VARBINARY = 4;

    static final int UDT_NCHAR = 18;

    static final int UDT_NVARCHAR = 19;

    static final int UDT_UNICHAR = 34;

    static final int UDT_UNIVARCHAR = 35;

    static final int UDT_UNITEXT = 36;

    /**
   * Not really a timestamp but a number encoded as binary or varbinary.
   */
    static final int UDT_TIMESTAMP = 80;

    static final int UDT_SYSNAME = 18;

    static final int UDT_NEWSYSNAME = 256;

    static final Float FLOAT_ZERO = Floats.ZERO;

    static final Double DOUBLE_ZERO = Doubles.ZERO;

    private static final UnsafeStringConstructor unsafeStringConstructor = UnsafeStringConstructor.getInstance();

    private static Charset getCharset(final TdsConnectionObject caller) throws SQLException {
        try {
            final Charset cs = caller.tdsConnection().getCharset();
            return (cs != null) ? cs : Charset.defaultCharset();
        } catch (final IOException ex) {
            throw new SQLNonTransientException(Messages.get("error.generic.ioerror", ex.getMessage()), "HY000");
        }
    }

    static Object convert(@Nullable final Object x, final int targetType, final TdsConnectionObject caller) throws SQLException {
        if (x == null) return null;
        switch(targetType) {
            case Types.LONGNVARCHAR:
                return toNClob(x, targetType, caller);
            case Types.NCHAR:
                return toString(x, targetType, caller);
            case Types.NVARCHAR:
                return toString(x, targetType, caller);
            case Types.ROWID:
                return toRowId(x, targetType);
            case Types.BIT:
                return toBoolean(x);
            case Types.TINYINT:
                return toInteger(x);
            case Types.BIGINT:
                return toLong(x);
            case Types.LONGVARBINARY:
                return toBlob(x, targetType, caller);
            case Types.VARBINARY:
            case Types.BINARY:
                return toByteArray(x, targetType, caller);
            case Types.LONGVARCHAR:
                return toClob(x, targetType, caller);
            case Types.NULL:
                throw convertBadType(x, targetType);
            case Types.CHAR:
                return toString(x, targetType, caller);
            case Types.NUMERIC:
            case Types.DECIMAL:
                return toBigDecimal(x);
            case Types.INTEGER:
            case Types.SMALLINT:
                return toInteger(x);
            case Types.FLOAT:
                return toDouble(x);
            case Types.REAL:
                return toFloat(x);
            case Types.DOUBLE:
                return toDouble(x);
            case Types.VARCHAR:
                return toString(x, targetType, caller);
            case Types.BOOLEAN:
                return toBoolean(x);
            case Types.DATALINK:
                throw convertBadType(x, targetType);
            case Types.DATE:
                return toDate(x);
            case Types.TIME:
                return toTime(x);
            case Types.TIMESTAMP:
                return toTimestamp(x);
            case Types.OTHER:
                return x;
            case Types.JAVA_OBJECT:
            case Types.DISTINCT:
            case Types.STRUCT:
            case Types.ARRAY:
                throw convertBadType(x, targetType);
            case Types.BLOB:
                return toBlob(x, targetType, caller);
            case Types.CLOB:
                return toClob(x, targetType, caller);
            case Types.REF:
                throw convertBadType(x, targetType);
            case Types.SQLXML:
                throw convertBadType(x, targetType);
            case Types.NCLOB:
                return toNClob(x, targetType, caller);
            default:
                throw new SQLDataException(Messages.get("error.convert.badtypeconst", getJdbcTypeName(targetType)), "HY004");
        }
    }

    private static SQLException convertBadType(@Nullable final Object x, final int targetType) throws SQLException {
        return new SQLDataException(Messages.get("error.convert.badtypes", (x == null) ? "null" : x.getClass().getName(), getJdbcTypeName(targetType)), "22005");
    }

    static String getJdbcTypeName(final int jdbcType) throws SQLException {
        try {
            return JdbcType.ofJdbc(jdbcType).name();
        } catch (final SQLException ex) {
            return "ERROR";
        }
    }

    static boolean isLobOrArray(final int type) {
        switch(type) {
            case SYBIMAGE:
            case SYBTEXT:
            case SYBVARBINARY:
            case SYBNTEXT:
            case SYBLONGBINARY:
            case XSYBVARBINARY:
            case XSYBBINARY:
            case MSXML:
                return true;
            default:
                return false;
        }
    }

    static BigDecimal toBigDecimal(@Nullable final Object x) throws SQLException {
        return SQLTypes.toBigDecimal(x);
    }

    static Blob toBlob(@Nullable final Object x, final int jdbcType, final TdsConnectionObject caller) throws SQLException {
        if (x == null) return null; else if (x instanceof Blob) return (Blob) x; else if (x instanceof byte[]) return new TdsBlob(caller.tdsConnection(), (byte[]) x); else if (x instanceof Clob) {
            final Clob clob = (Clob) x;
            try {
                final Reader in = clob.getCharacterStream();
                final TdsBlob blob = new TdsBlob(caller.tdsConnection());
                final long length = clob.length();
                if (length > 0) {
                    OutputStreamXWriter out = new OutputStreamXWriter(blob.setBinaryStream(1), caller.tdsConnection().getCharset());
                    out.setCharBufferCapacity((int) Math.min(length, 8192));
                    out.transferFrom(in, length);
                    out.close();
                    out = null;
                    in.close();
                }
                return blob;
            } catch (final IOException ex) {
                throw new SQLDataException(Messages.get("error.generic.ioerror", ex.getMessage()), "HY000", ex);
            }
        } else if (x instanceof CharSequence) {
            final CharBuffer in = unsafeStringConstructor.asMaybeWritableCharBuffer((CharSequence) x);
            try {
                final TdsBlob blob = new TdsBlob(caller.tdsConnection());
                final int length = in.length();
                if (length > 0) {
                    final OutputStreamXWriter out = new OutputStreamXWriter(blob.setBinaryStream(1), caller.tdsConnection().getCharset());
                    out.transferFrom(in, length);
                    out.close();
                }
                return blob;
            } catch (final IOException ex) {
                throw new SQLNonTransientException(Messages.get("error.generic.ioerror", ex.getMessage()), "HY000", ex);
            }
        } else {
            throw new SQLDataException(Messages.get("error.convert.badtypes", x.getClass().getName(), getJdbcTypeName(jdbcType)), "22005");
        }
    }

    static Boolean toBoolean(@Nullable final Object x) throws SQLException {
        return SQLTypes.toBoolean(x);
    }

    static boolean toBooleanValue(@Nullable final Object x) throws SQLException {
        return SQLTypes.toBooleanValue(x);
    }

    static InputStream toBinaryStream(@Nullable final Object x, final int jdbcType, final TdsConnectionObject con) throws SQLException {
        if (x == null) return null;
        if (x instanceof InputStream) return (InputStream) x;
        if (x instanceof Blob) return ((Blob) x).getBinaryStream();
        if (x instanceof byte[]) return new ByteArrayInputStream((byte[]) x);
        if (x instanceof Clob) {
            if (x instanceof TdsClob) {
                final BlobBuffer buffer = ((TdsClob) x).blobBuffer;
                long length = buffer.getLength();
                return buffer.getBinaryStream(1, ((length & 1) == 0) ? length : (length - 1));
            }
            return new ReaderInputStream(((Clob) x).getCharacterStream(), getCharset(con));
        }
        if (x instanceof Reader) return new ReaderInputStream((Reader) x, getCharset(con));
        if (x instanceof String) return new ReaderInputStream(CharBuffer.wrap((String) x), getCharset(con));
        if (x instanceof Boolean) return new ByteArrayInputStream(new byte[] { ((Boolean) x) ? (byte) 1 : 0 });
        final Blob blob = toBlob(x, jdbcType, con);
        return (blob == null) ? null : blob.getBinaryStream();
    }

    static Byte toByte(@Nullable final Object x) throws SQLException {
        return SQLTypes.toByte(x);
    }

    static byte toByteValue(@Nullable final Object x) throws SQLException {
        return SQLTypes.toByteValue(x);
    }

    static byte[] toByteArray(@Nullable Object x, final int jdbcType, final TdsConnectionObject caller) throws SQLException {
        if (x == null) return null; else if (x instanceof byte[]) return (byte[]) x; else if (x instanceof RowId) return ((RowId) x).getBytes(); else if (x instanceof Blob) {
            final Blob blob = (Blob) x;
            final long length = blob.length();
            if (length > Integer.MAX_VALUE) throw new SQLDataException(Messages.get("error.normalize.lobtoobig"), "22000");
            return blob.getBytes(1, (int) length);
        } else if (x instanceof Clob) {
            final Clob clob = (Clob) x;
            final long length = clob.length();
            if (length == 0) return Bytes.EMPTY_ARRAY; else if (length <= Integer.MAX_VALUE) x = clob.getSubString(1, (int) length);
            throw new SQLDataException(Messages.get("error.normalize.lobtoobig"), "22000");
        } else if (x instanceof Character) {
            x = Chars.toString(((Character) x).charValue());
        }
        if (x instanceof CharSequence) {
            CharSequence cs = (CharSequence) x;
            if (cs.length() == 0) return Bytes.EMPTY_ARRAY;
            final String s = cs.toString();
            cs = null;
            x = null;
            try {
                return s.getBytes(getCharset(caller).name());
            } catch (UnsupportedEncodingException ex) {
                ex = null;
                return s.getBytes();
            }
        } else {
            throw new SQLDataException(Messages.get("error.convert.badtypes", x.getClass().getName(), getJdbcTypeName(jdbcType)), "22005");
        }
    }

    static Clob toClob(@Nullable final Object x, final int jdbcType, final TdsConnectionObject caller) throws SQLException {
        if (x == null) return null; else if (x instanceof Clob) return (Clob) x; else return toNClob(x, jdbcType, caller);
    }

    static java.sql.Date toDate(@Nullable final Object x) throws SQLException {
        return SQLTypes.toDate(x);
    }

    static Double toDouble(@Nullable final Object x) throws SQLException {
        return SQLTypes.toDouble(x);
    }

    static double toDoubleValue(@Nullable final Object x) throws SQLException {
        return SQLTypes.toDoubleValue(x);
    }

    static Float toFloat(@Nullable final Object x) throws SQLException {
        return SQLTypes.toFloat(x);
    }

    static float toFloatValue(@Nullable final Object x) throws SQLException {
        return SQLTypes.toFloatValue(x);
    }

    static Integer toInteger(@Nullable final Object x) throws SQLException {
        return SQLTypes.toInteger(x);
    }

    static int toIntValue(@Nullable final Object x) throws SQLException {
        return SQLTypes.toIntValue(x);
    }

    static Long toLong(@Nullable final Object x) throws SQLException {
        return SQLTypes.toLong(x);
    }

    static long toLongValue(@Nullable final Object x) throws SQLException {
        return SQLTypes.toLongValue(x);
    }

    static NClob toNClob(@Nullable final Object x, final int jdbcType, final TdsConnectionObject caller) throws SQLException {
        if (x == null) return null; else if (x instanceof NClob) return (NClob) x; else if (x instanceof Blob) {
            final Blob blob = (Blob) x;
            try {
                final InputStreamXReader in = new InputStreamXReader(blob.getBinaryStream(), getCharset(caller));
                final TdsClob clob = new TdsClob(caller.tdsConnection());
                Writer out = clob.setCharacterStream(1);
                try {
                    in.transferTo(out, -1);
                } finally {
                    out.close();
                    out = null;
                    in.close();
                }
                return clob;
            } catch (final IOException ex) {
                throw new SQLNonTransientException(Messages.get("error.generic.ioerror", ex.getMessage()), "HY000", ex);
            }
        } else if (x instanceof byte[]) {
            try {
                final InputStreamXReader in = new InputStreamXReader(new ByteBufferInputStream((byte[]) x), getCharset(caller));
                final TdsClob clob = new TdsClob(caller.tdsConnection());
                Writer out = clob.setCharacterStream(1);
                in.transferTo(out, -1);
                out.close();
                out = null;
                in.close();
                return clob;
            } catch (final IOException ex) {
                throw new SQLNonTransientException(Messages.get("error.generic.ioerror", ex.getMessage()), "HY000", ex);
            }
        } else {
            return new TdsClob(caller.tdsConnection(), toString(x, jdbcType, caller));
        }
    }

    static Reader toReader(@Nullable final Object x, final int jdbcType, final TdsConnectionObject con) throws SQLException {
        if (x == null) return null;
        if (x instanceof Reader) return (Reader) x;
        if (x instanceof Clob) return ((Clob) x).getCharacterStream();
        if (x instanceof char[]) return new CharArrayReader((char[]) x);
        if (x instanceof Blob) return new InputStreamXReader(((Blob) x).getBinaryStream(), getCharset(con));
        if ((x instanceof String) || (x instanceof Number) || (x instanceof java.util.Date) || (x instanceof Character)) {
            return new StringReader(x.toString());
        }
        if (x instanceof Boolean) return new StringReader(((Boolean) x) ? "1" : "0");
        final Clob clob = toClob(x, jdbcType, con);
        return (clob == null) ? null : clob.getCharacterStream();
    }

    static RowId toRowId(@Nullable final Object x, final int jdbcType) throws SQLException {
        return SQLTypes.toRowId(x);
    }

    static Short toShort(@Nullable final Object x) throws SQLException {
        return SQLTypes.toShort(x);
    }

    static short toShortValue(@Nullable final Object x) throws SQLException {
        return SQLTypes.toShortValue(x);
    }

    static String toString(@Nullable final Object x, final int jdbcType, final TdsConnectionObject caller) throws SQLException {
        if (x == null) return null; else if ((x instanceof CharSequence) || (x instanceof Number)) return x.toString(); else if (x instanceof Character) return Chars.toString(((Character) x).charValue()); else if (x instanceof Boolean) return ((Boolean) x).booleanValue() ? "1" : "0"; else if (x instanceof Clob) {
            final Clob clob = (Clob) x;
            final long length = clob.length();
            if (length <= Integer.MAX_VALUE) return clob.getSubString(1, (int) length); else throw new SQLDataException(Messages.get("error.normalize.lobtoobig"), "22000");
        } else if (x instanceof char[]) {
            final char[] a = (char[]) x;
            return (a.length == 0) ? "" : (a.length == 1) ? Chars.toString(a[0]) : new String(a);
        } else if (x instanceof byte[]) {
            final byte[] a = (byte[]) x;
            if (a.length == 0) return "";
            try {
                return new String(a, getCharset(caller).name());
            } catch (final UnsupportedEncodingException ex) {
                throw new SQLNonTransientException(Messages.get("error.generic.ioerror", ex.getMessage()), "HY000", ex);
            }
        } else if (x instanceof Blob) {
            final Blob blob = (Blob) x;
            final InputStreamXReader in = new InputStreamXReader(blob.getBinaryStream(), getCharset(caller));
            try {
                try {
                    return IOStreams.readString(in, -1);
                } finally {
                    in.close();
                }
            } catch (final BufferOverflowException ex) {
                throw new SQLDataException(Messages.get("error.normalize.lobtoobig"), "22000", ex);
            } catch (final IOException ex) {
                throw new SQLNonTransientException(Messages.get("error.generic.ioerror", ex.getMessage()), "HY000", ex);
            }
        } else {
            return x.toString();
        }
    }

    static Time toTime(@Nullable final Object x) throws SQLException {
        return SQLTypes.toTime(x);
    }

    static Timestamp toTimestamp(@Nullable final Object x) throws SQLException {
        return SQLTypes.toTimestamp(x);
    }

    static URL toURL(@Nullable final Object x) throws SQLException {
        return SQLTypes.toURL(x);
    }
}

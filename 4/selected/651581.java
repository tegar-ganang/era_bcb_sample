package jaxlib.jdbc.tds;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.sql.Types;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jaxlib.io.IO;
import jaxlib.io.stream.ByteOrders;
import jaxlib.io.stream.IOStreams;
import jaxlib.io.stream.InputStreamXReader;
import jaxlib.jaxlib_private.util.UnsafeStringConstructor;
import jaxlib.util.CheckArg;
import jaxlib.util.Strings;

/**
 * Class to implement an output stream for the server request.
 * <p>
 * Implementation note:
 * <ol>
 * <li>This class contains methods to writeByte different types of data to the
 *     server request stream in TDS format.
 * <li>Character translation of String items is carried out.
 * </ol>
 *
 * @author  Mike Hutchinson.
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: TdsOutputStream.java 3047 2012-02-01 04:51:30Z joerg_wassmer $
 */
final class TdsOutputStream extends OutputStream {

    private static final UnsafeStringConstructor unsafeStringConstructor = UnsafeStringConstructor.getInstance();

    private final TdsCalendar calendar;

    private int packetCounter;

    /** The shared network socket. */
    private TdsSocket socket;

    private ByteBuffer buffer;

    /** The request packet type. */
    private int pktType;

    /** The unique stream id. */
    final int streamId;

    /** The maximum decimal precision. */
    private int maxPrecision;

    private CharsetEncoder charsetEncoder;

    @CheckForNull
    private CharBuffer singleCharBuffer;

    /**
   * Construct a TdsOutputStream object.
   *
   * @param socket     the shared socket object to writeByte to
   * @param streamId   the unique id for this stream
   * @param bufferSize the initial buffer size to use (the current network  packet size)
   */
    TdsOutputStream(final TdsSocket socket, final int streamId, final int bufferSize, final int maxPrecision, final TdsCalendar calendar) {
        super();
        assert (socket != null);
        assert (bufferSize > 0) : bufferSize;
        assert (streamId != -1);
        this.buffer = Support.allocateMaybeDirectBuffer(Math.max(TdsCore.PKT_HDR_LEN << 1, bufferSize));
        this.calendar = calendar;
        this.maxPrecision = maxPrecision;
        this.socket = socket;
        this.streamId = streamId;
        this.buffer.position(TdsCore.PKT_HDR_LEN);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
   * Close the output stream.
   */
    @Override
    public final void close() {
        this.buffer = null;
        this.charsetEncoder = null;
        this.socket = null;
        this.singleCharBuffer = null;
    }

    /**
   * Flush the packet to the output stream setting the last packet flag.
   *
   * @throws IOException
   */
    @Override
    public final void flush() throws IOException {
        putPacket(1);
    }

    @Override
    public final void write(final int b) throws IOException {
        getPacket().put((byte) b);
    }

    /**
   * Write an array of bytes to the output stream.
   *
   * @param b The byte array to writeByte.
   * @throws IOException
   */
    @Override
    public final void write(final byte[] src) throws IOException {
        write(src, 0, src.length);
    }

    /**
   * Write a partial byte buffer to the output stream.
   *
   * @param b The byte array buffer.
   * @param off The offset into the byte array.
   * @param len The number of bytes to writeByte.
   * @throws IOException
   */
    @Override
    public final void write(final byte[] src, int offs, final int len) throws IOException {
        final int limit = Math.min(src.length, offs + len);
        final int overhead = len - (limit - offs);
        while (offs < limit) {
            final ByteBuffer buffer = getPacket();
            final int step = Math.min(buffer.remaining(), limit - offs);
            buffer.put(src, offs, step);
            offs += step;
        }
        if (overhead > 0) writeZeros(overhead);
    }

    private void closeAfterError() {
        final TdsSocket socket = this.socket;
        IO.tryClose(this);
        IO.tryClose(socket);
    }

    private CharsetEncoder getCharsetEncoder() throws IOException {
        final Charset charset = socket().tdsConnection().getCharset();
        CharsetEncoder encoder = this.charsetEncoder;
        if ((encoder == null) || (encoder.charset() != charset)) this.charsetEncoder = encoder = charset.newEncoder().onMalformedInput(CodingErrorAction.REPORT); else encoder.reset();
        return encoder;
    }

    @Nonnull
    private ByteBuffer getPacket() throws IOException {
        ByteBuffer buffer = this.buffer;
        if (buffer == null) {
            close();
            throw new IOException("Socket output stream closed.");
        }
        if (buffer.hasRemaining()) return buffer;
        buffer = null;
        return putPacket(0);
    }

    @Nonnull
    private CharBuffer getSingleCharBuffer() {
        CharBuffer b = this.singleCharBuffer;
        if (b == null) this.singleCharBuffer = b = CharBuffer.allocate(1);
        b.clear();
        return b;
    }

    /**
   * Write the TDS packet to the network.
   *
   * @param last Set to 1 if this is the last packet else 0.
   * @throws IOException
   */
    @Nonnull
    private ByteBuffer putPacket(final int last) throws IOException {
        ByteBuffer buffer = this.buffer;
        if (buffer == null) throw new IOException("Socket output stream closed");
        final TdsSocket socket = socket();
        final TdsVersion tdsVersion = socket.getTdsVersion();
        int packetCounter = this.packetCounter + 1;
        if (packetCounter >= 256) packetCounter = 0;
        this.packetCounter = packetCounter;
        buffer.put(0, (byte) this.pktType);
        buffer.put(1, (byte) last);
        buffer.put(2, (byte) (buffer.position() >> 8));
        buffer.put(3, (byte) buffer.position());
        buffer.put(4, (byte) 0);
        buffer.put(5, (byte) 0);
        buffer.put(6, (byte) ((tdsVersion != null) && (tdsVersion.major >= 7) ? packetCounter : 0));
        buffer.put(7, (byte) 0);
        try {
            socket.writePacket(streamId, buffer);
        } catch (final IOException ex) {
            buffer = null;
            closeAfterError();
            throw ex;
        }
        buffer.limit(buffer.capacity()).position(TdsCore.PKT_HDR_LEN);
        return buffer;
    }

    private void writeReverse(final byte[] src, final int offs, final int len) throws IOException {
        for (int i = offs + len; i > offs; ) {
            final ByteBuffer buffer = getPacket();
            final int lo = i - Math.min(buffer.remaining(), i - offs);
            while (i > lo) buffer.put(src[--i]);
        }
    }

    final TdsSocket socket() throws IOException {
        final TdsSocket socket = this.socket;
        if (socket == null) throw new IOException("Socket output stream closed");
        return socket;
    }

    final ByteBuffer encodeStringToSharedBuffer(final CharSequence s, final Charset charset) throws IOException {
        final CharsetEncoder encoder = ((charset == null) || charset.equals(socket().tdsConnection().getCharset())) ? getCharsetEncoder() : charset.newEncoder();
        final CharBuffer in = unsafeStringConstructor.asMaybeWritableCharBuffer(s);
        ByteBuffer out = socket().getSharedBuffer(in.remaining());
        while (in.hasRemaining()) {
            final CoderResult cr = encoder.encode(in, out, true);
            if (cr == CoderResult.OVERFLOW) {
                final ByteBuffer newOut = socket().getSharedBuffer(out.capacity() + in.remaining());
                newOut.put(out);
                socket().releaseSharedBuffer(out);
                out = newOut;
                continue;
            } else if (cr.isError()) {
                cr.throwException();
            }
        }
        out.flip();
        return out;
    }

    /**
   * Returns the maximum number of bytes required to output a decimal
   * given the current {@link #maxPrecision}.
   *
   * @return the maximum number of bytes required to output a decimal.
   */
    final byte getMaxDecimalBytes() {
        return (byte) ((this.maxPrecision <= TdsData.DEFAULT_PRECISION_28) ? 13 : 17);
    }

    /**
   * Set the output buffer size
   *
   * @param size The new buffer size (>= {@link TdsCore#MIN_PKT_SIZE} <= {@link TdsCore#MAX_PKT_SIZE}).
   */
    final void setBlockSize(final int size) {
        final ByteBuffer oldBuffer = this.buffer;
        if ((size < oldBuffer.position()) || (size == oldBuffer.capacity())) return;
        if ((size < TdsCore.MIN_PKT_SIZE) || (size > TdsCore.MAX_PKT_SIZE)) throw new IllegalArgumentException("Invalid buffer size parameter " + size);
        oldBuffer.flip();
        final ByteBuffer newBuffer = ByteBuffer.allocate(size);
        newBuffer.put(oldBuffer);
        newBuffer.order(ByteOrder.LITTLE_ENDIAN);
        this.buffer = newBuffer;
    }

    /**
   * Retrieve the current output packet size.
   *
   * @return the packet size as an <code>int</code>.
   */
    final int getBufferSize() {
        final ByteBuffer buffer = this.buffer;
        return (buffer != null) ? buffer.capacity() : 0;
    }

    /**
   * Retrieve the TDS version number.
   *
   * @return The TDS version as an <code>int</code>.
   */
    final TdsVersion getTdsVersion() throws IOException {
        return socket().getTdsVersion();
    }

    /**
   * Set the maximum decimal precision.
   *
   * @param v The precision either 28 or 38.
   */
    final void setMaxPrecision(final int v) {
        this.maxPrecision = v;
    }

    /**
   * Set the current output packet type.
   *
   * @param v The packet type eg TdsCore.QUERY_PKT.
   */
    final void setPacketType(final int v) throws IOException, SQLException {
        this.pktType = v;
        switch(v) {
            case TdsCore.QUERY_PKT:
            case TdsCore.RPC_PKT:
            case TdsCore.MSDTC_PKT:
                if (getTdsVersion().major >= 9) {
                    final ByteBuffer packet = getPacket();
                    if (packet.position() == TdsCore.PKT_HDR_LEN) {
                        packet.putInt(22);
                        packet.putInt(18);
                        packet.putShort((short) 2);
                        packet.putLong(socket().tdsConnection().getTransactionDescriptor());
                        packet.putInt(1);
                    }
                }
                break;
            default:
                break;
        }
    }

    final void transferCharsFromEncoded(final Readable in, final int length) throws IOException {
        CheckArg.notNegative(length, "length");
        if (length > 0) {
            ByteBuffer bb = socket().getSharedBuffer(Math.max(Math.min(8192, length), 256));
            CharBuffer cb = bb.asCharBuffer();
            final CharsetEncoder encoder = getCharsetEncoder();
            int count = 0;
            while (true) {
                cb.compact();
                final int cbPos = cb.position();
                cb.limit(Math.min(cb.capacity(), (int) Math.min(Integer.MAX_VALUE, (long) cbPos + length)));
                in.read(cb);
                final int step = cb.position() - cbPos;
                if (step <= 0) break;
                count += step;
                cb.flip();
                while (encoder.encode(cb, getPacket(), count == length) == CoderResult.OVERFLOW) putPacket(0);
                if (count < length) continue;
                break;
            }
            while (cb.hasRemaining()) {
                if (encoder.encode(cb, getPacket(), true) == CoderResult.OVERFLOW) putPacket(0);
            }
            cb = null;
            socket.releaseSharedBuffer(bb);
            bb = null;
            while (encoder.flush(getPacket()) == CoderResult.OVERFLOW) putPacket(0);
            if (count != length) {
                throw new IOException(Strings.concat("Data in stream less than specified by length.\n", "  expected = ", Integer.toString(length), "  actual   = ", Integer.toString(count)));
            }
        }
    }

    /**
   * Copy the contents of an InputStream to the server.
   *
   * @param in The InputStream to read.
   * @param length The length of the stream.
   * @throws IOException
   */
    final void transferFrom(final InputStream in, final int length) throws IOException {
        CheckArg.notNegative(length, "length");
        for (int transferred = 0; transferred < length; ) {
            final ByteBuffer buffer = getPacket();
            final int maxStep = Math.min(buffer.remaining(), length - transferred);
            final int pos = buffer.position();
            final int step = in.read(buffer.array(), pos, maxStep);
            if (step > maxStep) {
                throw new IOException("the source stream reported more bytes than requested:" + "\nrequested    = " + maxStep + "\nreturned     = " + step + "\nstream class = " + in.getClass().getName());
            } else if (step <= 0) {
                throw new EOFException("data in source stream less than specified length:" + "\nexpected     = " + length + "\nactual       = " + transferred + "\nstream class = " + in.getClass().getName());
            } else {
                buffer.position(pos + step);
                transferred += step;
            }
        }
    }

    final int write(final ByteBuffer src) throws IOException {
        final int count = src.remaining();
        if (src.hasArray()) {
            write(src.array(), src.arrayOffset() + src.position(), count);
            src.position(src.position() + count);
        } else {
            for (int i = count; --i >= 0; ) write(src.get());
        }
        return count;
    }

    /**
   * Write a byte to the output stream.
   *
   * @param b The byte value to writeByte.
   * @throws IOException
   */
    final void writeByte(final int b) throws IOException {
        getPacket().put((byte) b);
    }

    /**
   * Write a short value to the output stream.
   *
   * @param s The short value to writeByte.
   * @throws IOException
   */
    final void writeChar(final char v) throws IOException {
        final ByteBuffer buffer = getPacket();
        if (buffer.remaining() >= 2) buffer.putChar(v); else ByteOrders.LITTLE_ENDIAN.writeChar(this, v);
    }

    /**
   * Write a double value to the output stream.
   *
   * @param f The double value to writeByte.
   * @throws IOException
   */
    final void writeDouble(final double v) throws IOException {
        final ByteBuffer buffer = getPacket();
        if (buffer.remaining() >= 8) buffer.putDouble(v); else ByteOrders.LITTLE_ENDIAN.writeDouble(this, v);
    }

    /**
   * Write a float value to the output stream.
   *
   * @param f The float value to writeByte.
   * @throws IOException
   */
    final void writeFloat(final float v) throws IOException {
        final ByteBuffer buffer = getPacket();
        if (buffer.remaining() >= 4) buffer.putFloat(v); else ByteOrders.LITTLE_ENDIAN.writeFloat(this, v);
    }

    /**
   * Write an int value to the output stream.
   *
   * @param i The int value to writeByte.
   * @throws IOException
   */
    final void writeInt(final int v) throws IOException {
        final ByteBuffer buffer = getPacket();
        if (buffer.remaining() >= 4) buffer.putInt(v); else ByteOrders.LITTLE_ENDIAN.writeInt(this, v);
    }

    /**
   * Write a short value to the output stream.
   *
   * @param s The short value to writeByte.
   * @throws IOException
   */
    final void writeShort(final int v) throws IOException {
        final ByteBuffer buffer = getPacket();
        if (buffer.remaining() >= 2) buffer.putShort((short) v); else ByteOrders.LITTLE_ENDIAN.writeShort(this, v);
    }

    /**
   * Write a long value to the output stream.
   *
   * @param l The long value to writeByte.
   * @throws IOException
   */
    final void writeLong(final long v) throws IOException {
        final ByteBuffer buffer = getPacket();
        if (buffer.remaining() >= 8) buffer.putLong(v); else ByteOrders.LITTLE_ENDIAN.writeLong(this, v);
    }

    /**
   * Write a char array object to the output stream.
   *
   * @param s The char[] to writeByte.
   * @throws IOException
   */
    final void writeChars(final char[] s, final int off, final int len) throws IOException {
        int fromIndex = off;
        final int toIndex = off + len;
        while (fromIndex < toIndex) {
            ByteBuffer buffer = getPacket();
            int remaining = buffer.remaining();
            if ((remaining & 1) != 0) remaining--;
            if (remaining > 1) {
                final int hi = fromIndex + Math.min(remaining >> 1, toIndex - fromIndex);
                int pos = buffer.position();
                while (fromIndex < hi) {
                    buffer.putChar(pos, s[fromIndex++]);
                    pos += 2;
                }
                buffer.position(pos);
            } else {
                buffer = null;
                writeChar(s[fromIndex++]);
            }
        }
    }

    final void writeLengthByteAndStringEncoded(final CharSequence s) throws IOException {
        final ByteBuffer buf = encodeStringToSharedBuffer(s, null);
        writeByte(buf.remaining());
        write(buf);
        socket().releaseSharedBuffer(buf);
    }

    /**
   * Write a String to the output stream as translated bytes.
   *
   * @param s The String to writeByte.
   * @throws IOException
   */
    final void writeStringEncoded(CharSequence s) throws IOException {
        final int slen = s.length();
        if (slen > 0) {
            CharBuffer cb = unsafeStringConstructor.asMaybeWritableCharBuffer(s);
            s = null;
            final CharsetEncoder encoder = getCharsetEncoder();
            while (cb.hasRemaining()) {
                final CoderResult cr = encoder.encode(cb, getPacket(), true);
                if (cr == CoderResult.OVERFLOW) {
                    putPacket(0);
                    continue;
                } else if (cr.isError()) {
                    cr.throwException();
                }
            }
            cb = null;
            while (true) {
                final CoderResult cr = encoder.flush(getPacket());
                if (cr == CoderResult.OVERFLOW) {
                    putPacket(0);
                    continue;
                } else if (cr.isError()) cr.throwException(); else break;
            }
        }
    }

    /**
   * Write a String object to the output stream.
   * If the TDS version is >= 7.0 write a unicode string, otherwise a translated byte stream.
   *
   * @param s The String to writeByte.
   * @throws IOException
   */
    final void writeStringPlain(final String s) throws IOException {
        if (socket().getTdsVersion().major >= 7) writeStringUTF16LE(s); else writeStringEncoded(s);
    }

    /**
   * Write a String object to the output stream.
   * If the TDS version is >= 7.0 write a unicode string, otherwise a translated byte stream.
   *
   * @param s The String to writeByte.
   * @throws IOException
   */
    final void writeStringPlain(final StringBuilder s) throws IOException {
        if (socket().getTdsVersion().major >= 7) writeStringUTF16LE(s); else writeStringEncoded(s);
    }

    private void writeStringUTF16LE(final String s) throws IOException {
        int fromIndex = 0;
        final int toIndex = s.length();
        while (fromIndex < toIndex) {
            ByteBuffer buffer = getPacket();
            int remaining = buffer.remaining();
            if ((remaining & 1) != 0) remaining--;
            if (remaining > 1) {
                final int hi = fromIndex + Math.min(remaining >> 1, toIndex - fromIndex);
                int pos = buffer.position();
                while (fromIndex < hi) {
                    buffer.putChar(pos, s.charAt(fromIndex++));
                    pos += 2;
                }
                buffer.position(pos);
            } else {
                buffer = null;
                writeChar(s.charAt(fromIndex++));
            }
        }
    }

    /**
   * Same as for string, using StringBuilder instead of CharSequence for micro-optimization purposes.
   */
    private void writeStringUTF16LE(final StringBuilder s) throws IOException {
        int fromIndex = 0;
        final int toIndex = s.length();
        while (fromIndex < toIndex) {
            ByteBuffer buffer = getPacket();
            int remaining = buffer.remaining();
            if ((remaining & 1) != 0) remaining--;
            if (remaining > 1) {
                final int hi = fromIndex + Math.min(remaining >> 1, toIndex - fromIndex);
                int pos = buffer.position();
                while (fromIndex < hi) {
                    buffer.putChar(pos, s.charAt(fromIndex++));
                    pos += 2;
                }
                buffer.position(pos);
            } else {
                buffer = null;
                writeChar(s.charAt(fromIndex++));
            }
        }
    }

    /**
   * Write a BigDecimal value to the output stream.
   *
   * @param value The BigDecimal value to writeByte.
   * @throws IOException
   */
    final void writeBigDecimal(BigDecimal value) throws IOException {
        if (value == null) writeByte(0); else {
            final int signum = (value.signum() < 0) ? 0 : 1;
            final byte[] mantisse = value.unscaledValue().abs().toByteArray();
            value = null;
            final int len = mantisse.length + 1;
            final int maxLen = getMaxDecimalBytes();
            if (len > maxLen) {
                throw new IOException("BigDecimal too big to send (len = " + len + "; maxLen=" + maxLen + ")");
            }
            writeByte(len);
            writeByte(signum);
            writeReverse(mantisse, 0, mantisse.length);
        }
    }

    final void writeTdsCollation(final TdsParamInfo pi) throws IOException {
        if (TdsSQLType.get(pi.tdsType).collation) writeTdsCollation(pi.collation);
    }

    final void writeTdsCollation(final long collation) throws IOException {
        if (collation == 0) writeZeros(5); else {
            writeInt((int) (collation & 0x00ffffffffL));
            write((int) ((collation & 0xff00000000L) >> 32));
        }
    }

    final void writeZeros(int count) throws IOException {
        while (count > 0) {
            final ByteBuffer buffer = getPacket();
            int i = Math.min(count, buffer.remaining());
            count -= i;
            while (--i >= 0) buffer.put((byte) 0);
        }
    }

    /**
   * Write a parameter to the server request stream.
   *
   * @param out         the server request stream
   * @param charsetInfo the default character set
   * @param collation   the default SQL Server 2000 collation
   * @param pi          the parameter descriptor
   */
    final void writeTdsParam(final TdsParamInfo pi, final long collation) throws IOException, SQLException {
        if ((getTdsVersion().major >= 8) && (pi.collation == 0)) pi.collation = collation;
        if (pi.charsetInfo == null) pi.charsetInfo = socket().tdsConnection().getCharsetInfo();
        switch(pi.tdsType) {
            case TdsTypes.SYBIMAGE:
                writeTdsIMAGE(pi);
                return;
            case TdsTypes.SYBTEXT:
                writeTdsTEXT(pi);
                return;
            case TdsTypes.SYBVARBINARY:
                writeTdsVARBINARY(pi);
                return;
            case TdsTypes.SYBINTN:
                writeTdsINTN(pi);
                return;
            case TdsTypes.SYBVARCHAR:
                writeTdsVARCHAR(pi);
                return;
            case TdsTypes.MSDATEN:
                writeTdsMSDATEN(pi);
                return;
            case TdsTypes.MSTIMEN:
                writeTdsMSTIMEN(pi);
                return;
            case TdsTypes.MSDATETIME2N:
                writeTdsMSDATETIME2N(pi);
                return;
            case TdsTypes.SYBBIT:
                writeTdsBIT(pi);
                return;
            case TdsTypes.UDT_TIMESTAMP:
                writeTdsUDTTIMESTAMP(pi);
                return;
            case TdsTypes.SYBNTEXT:
                writeTdsNTEXT(pi);
                return;
            case TdsTypes.SYBBITN:
                writeTdsBITN(pi);
                return;
            case TdsTypes.SYBDECIMAL:
            case TdsTypes.SYBNUMERIC:
                writeTdsNUMERIC(pi);
                return;
            case TdsTypes.SYBFLTN:
                writeTdsFLTN(pi);
                return;
            case TdsTypes.SYBDATETIMN:
                writeTdsDATETIMN(pi);
                return;
            case TdsTypes.SYBDATEN:
                writeTdsDATEN(pi);
                return;
            case TdsTypes.SYBTIMEN:
                writeTdsTIMEN(pi);
                return;
            case TdsTypes.XSYBVARBINARY:
                writeTdsXVARBINARY(pi);
                return;
            case TdsTypes.XSYBVARCHAR:
                writeTdsXVARCHAR(pi);
                return;
            case TdsTypes.XSYBNVARCHAR:
                writeTdsXNVARCHAR(pi);
                return;
            default:
                throw new SQLNonTransientException("internal error: unsupported output TDS type " + Integer.toHexString(pi.tdsType));
        }
    }

    /**
   * Copy the contents of the specified character stream to the server, encoded as UTF-16LE.
   *
   * Note: SQL Server 2010 is making problems when sending a buffer that is not completely filled while the string
   * has not been completely written. In that case the server will close the socket without further notice.
   *
   * @param in
   *  the source stream.
   * @param length
   *  the length of the data in characters.
   */
    private void transferCharsFrom(final Readable in, final int length) throws IOException {
        CheckArg.notNegative(length, "length");
        for (int transferred = 0; transferred < length; ) {
            final ByteBuffer bb = getPacket();
            final CharBuffer cb;
            final int limit;
            final boolean wrapped;
            if (bb.remaining() < 2) {
                cb = getSingleCharBuffer();
                limit = 1;
                wrapped = false;
            } else {
                cb = bb.asCharBuffer();
                limit = Math.min(cb.limit(), length - transferred);
                cb.limit(limit);
                wrapped = true;
            }
            final int step = in.read(cb);
            if (step > limit) {
                throw new IOException("the source stream reported more chars than requested:" + "\nrequested    = " + limit + "\nreturned     = " + step + "\nstream class = " + in.getClass().getName());
            } else if (step != cb.position()) {
                throw new IOException("return value/position mismatch:" + "\nreturn       = " + step + "\nposition     = " + cb.position() + "\nstream class = " + in.getClass().getName());
            } else if (step <= 0) {
                throw new EOFException("data in source stream less than specified length:" + "\nexpected     = " + length + "\nactual       = " + transferred + "\nstream class = " + in.getClass().getName());
            } else {
                if (wrapped) bb.position(bb.position() + (step << 1)); else writeChar(cb.get(0));
                transferred += step;
            }
        }
    }

    private void writeTdsBIT(final TdsParamInfo pi) throws IOException {
        writeByte(TdsTypes.SYBBIT);
        writeByte(((pi.value != null) && ((Boolean) pi.value).booleanValue()) ? 1 : 0);
    }

    private void writeTdsBITN(final TdsParamInfo pi) throws IOException {
        writeByte(TdsTypes.SYBBITN);
        writeByte(1);
        if (pi.value == null) writeByte(0); else {
            writeByte(1);
            writeByte((((Boolean) pi.value).booleanValue() ? 1 : 0));
        }
    }

    private void writeTdsDATEN(final TdsParamInfo pi) throws IOException, SQLDataException {
        writeByte(TdsTypes.SYBDATEN);
        writeByte(4);
        writeDATENValue((java.util.Date) pi.value);
    }

    final void writeDATENValue(@Nullable final java.util.Date value) throws IOException, SQLDataException {
        if (value == null) writeByte(0); else {
            writeByte(4);
            this.calendar.calendar.setTime(value);
            writeInt(this.calendar.encodeDate());
        }
    }

    private void writeTdsDATETIMN(final TdsParamInfo pi) throws IOException, SQLDataException {
        writeByte(TdsTypes.SYBDATETIMN);
        writeByte(8);
        writeDateTimeValue((java.util.Date) pi.value);
    }

    final void writeDateTimeValue(@Nullable final java.util.Date value) throws IOException, SQLDataException {
        if (value == null) writeByte(0); else {
            this.calendar.calendar.setTime(value);
            final int time = this.calendar.encodeTime();
            final int date = this.calendar.encodeDate();
            writeByte(8);
            writeInt(date);
            writeInt(time);
        }
    }

    private void writeTdsFLTN(final TdsParamInfo pi) throws IOException {
        writeByte(TdsTypes.SYBFLTN);
        if (pi.value instanceof Float) {
            writeByte(4);
            writeByte(4);
            writeFloat(((Number) pi.value).floatValue());
        } else {
            writeByte(8);
            if (pi.value == null) writeByte(0); else {
                writeByte(8);
                writeDouble(((Number) pi.value).doubleValue());
            }
        }
    }

    private void writeTdsIMAGE(final TdsParamInfo pi) throws IOException, SQLException {
        final int len = (pi.value == null) ? 0 : pi.length;
        writeByte(TdsTypes.SYBIMAGE);
        if (len > 0) {
            InputStream in;
            if (pi.value instanceof InputStream) in = (InputStream) pi.value; else if (pi.value instanceof Blob) in = ((Blob) pi.value).getBinaryStream(); else in = null;
            if (in != null) {
                pi.value = null;
                writeInt(len);
                writeInt(len);
                try {
                    transferFrom(in, len);
                    in.close();
                    in = null;
                } finally {
                    IO.tryClose(in);
                }
            } else {
                final ByteBuffer buf = pi.getOutputBytes(this);
                pi.value = null;
                writeInt(buf.remaining());
                writeInt(buf.remaining());
                write(buf);
            }
        } else if (getTdsVersion().major < 7) {
            writeInt(1);
            writeInt(1);
            writeByte(0);
        } else {
            writeInt(0);
            writeInt(0);
        }
    }

    private void writeTdsINTN(final TdsParamInfo pi) throws IOException {
        writeByte(TdsTypes.SYBINTN);
        if (pi.value == null) {
            writeByte((pi.sqlType.equals("bigint")) ? 8 : 4);
            writeByte(0);
        } else if (pi.sqlType.equals("bigint")) {
            writeByte(8);
            writeByte(8);
            writeLong(((Number) pi.value).longValue());
        } else {
            writeByte(4);
            writeByte(4);
            writeInt(((Number) pi.value).intValue());
        }
    }

    private void writeTdsMSDATEN(final TdsParamInfo pi) throws IOException, SQLDataException {
        writeByte(TdsTypes.MSDATEN);
        if (pi.value == null) writeByte(0); else {
            final GregorianCalendar calendar = this.calendar.calendar;
            calendar.setTime((java.util.Date) pi.value);
            final int date = DateTime2.daysSinceBaseDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.DAY_OF_YEAR));
            if ((date < 0) || (date > DateTime2.DAYS_SINCE_BASE_DATE_MAX)) throw new SQLDataException("date overflow, value is out of supported range: " + pi.value);
            writeByte(3);
            writeByte(date & 0xff);
            writeByte((date >> 8) & 0xff);
            writeByte((date >> 16) & 0xff);
        }
    }

    private void writeTdsMSDATETIME2N(final TdsParamInfo pi) throws IOException, SQLException {
        writeByte(TdsTypes.MSDATETIME2N);
        writeByte(pi.scale);
        if (pi.value == null) writeByte(0); else {
            final GregorianCalendar calendar = this.calendar.calendar;
            calendar.setTime((java.util.Date) pi.value);
            final int date = DateTime2.daysSinceBaseDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.DAY_OF_YEAR));
            if ((date < 0) || (date > DateTime2.DAYS_SINCE_BASE_DATE_MAX)) throw new SQLDataException("date overflow, value is out of supported range: " + pi.value);
            final long time = DateTime2.getTimeBytes(calendar, pi.scale);
            final int timeLen = DateTime2.nanosSinceMidnightLengthForScale(pi.scale);
            writeByte(3 + timeLen);
            for (int i = 0; i < timeLen; i++) writeByte((int) ((time >> 8 * i) & 0xff));
            writeByte(date & 0xff);
            writeByte((date >> 8) & 0xff);
            writeByte((date >> 16) & 0xff);
        }
    }

    private void writeTdsMSTIMEN(final TdsParamInfo pi) throws IOException, SQLException {
        writeByte(TdsTypes.MSTIMEN);
        writeByte(pi.scale);
        if (pi.value == null) writeByte(0); else {
            final GregorianCalendar calendar = this.calendar.calendar;
            calendar.setTime((java.util.Date) pi.value);
            final long time = DateTime2.getTimeBytes(calendar, pi.scale);
            final int len = DateTime2.nanosSinceMidnightLengthForScale(pi.scale);
            writeByte(len);
            for (int i = 0; i < len; i++) writeByte((int) (time >> 8 * i & 0xff));
        }
    }

    private void writeTdsNTEXT(final TdsParamInfo pi) throws IOException, SQLException {
        writeByte(TdsTypes.SYBNTEXT);
        if (pi.value == null) {
            writeInt(-1);
            if (getTdsVersion().major >= 8) writeTdsCollation(pi);
            writeInt(0);
        } else {
            int len = (pi.value == null) ? 0 : pi.length;
            if (len == 0) {
                writeInt(len);
                if (getTdsVersion().major >= 8) writeTdsCollation(pi);
                writeInt(len);
            } else {
                Readable in;
                if (pi.value instanceof Readable) in = (Readable) pi.value; else if (pi.value instanceof Clob) in = ((Clob) pi.value).getCharacterStream(); else if ((pi.value instanceof InputStream) && !pi.charsetInfo.wideChars) in = new InputStreamXReader((InputStream) pi.value, pi.charsetInfo.getCharset()); else in = null;
                if (in != null) {
                    pi.value = null;
                    try {
                        writeInt(len);
                        if (getTdsVersion().major >= 8) writeTdsCollation(pi);
                        writeInt(len * 2);
                        transferCharsFrom(in, len);
                        IO.closeIfCloseable(in);
                        in = null;
                    } finally {
                        IO.tryCloseIfCloseable(in);
                    }
                } else {
                    final String tmp = pi.getString(pi.charsetInfo.name);
                    pi.value = null;
                    len = tmp.length();
                    writeInt(len);
                    if (getTdsVersion().major >= 8) writeTdsCollation(pi);
                    writeInt(len * 2);
                    writeStringPlain(tmp);
                }
            }
        }
    }

    private void writeTdsNUMERIC(final TdsParamInfo pi) throws IOException {
        writeByte(pi.checkTdsType(TdsTypes.SYBDECIMAL, TdsTypes.SYBNUMERIC));
        writeByte(getMaxDecimalBytes());
        writeByte(this.maxPrecision);
        if (pi.value == null) {
            if (pi.jdbcType == Types.BIGINT) writeByte(0); else writeByte(((pi.scale >= 0) && (pi.scale <= this.maxPrecision)) ? pi.scale : TdsData.DEFAULT_SCALE);
            writeBigDecimal(null);
        } else if (pi.value instanceof Long) {
            writeByte(0);
            writeBigDecimal(BigDecimal.valueOf(((Long) pi.value).longValue()));
        } else {
            final BigDecimal value = (BigDecimal) pi.value;
            writeByte(value.scale());
            writeBigDecimal(value);
        }
    }

    private void writeTdsTEXT(final TdsParamInfo pi) throws IOException, SQLException {
        int len;
        if (pi.value == null) len = 0; else {
            len = pi.length;
            if ((len == 0) && (getTdsVersion().major < 7)) {
                pi.value = " ";
                len = 1;
            }
        }
        writeByte(TdsTypes.SYBTEXT);
        if (len > 0) {
            InputStream in;
            if (pi.value instanceof InputStream) in = (InputStream) pi.value; else if (pi.value instanceof Blob) in = ((Blob) pi.value).getBinaryStream(); else in = null;
            if (in != null) {
                pi.value = null;
                try {
                    writeInt(len);
                    if (getTdsVersion().major >= 8) writeTdsCollation(pi);
                    writeInt(len);
                    transferFrom(in, len);
                    in.close();
                    in = null;
                } finally {
                    IO.tryClose(in);
                }
            } else if ((pi.value instanceof Readable) && !pi.charsetInfo.wideChars) {
                Readable reader = (Reader) pi.value;
                try {
                    pi.value = null;
                    writeInt(len);
                    if (getTdsVersion().major >= 8) writeTdsCollation(pi);
                    writeInt(len);
                    transferCharsFromEncoded(reader, len);
                    IO.closeIfCloseable(reader);
                    reader = null;
                } finally {
                    IO.tryCloseIfCloseable(reader);
                }
            } else {
                final ByteBuffer buf = pi.getOutputBytes(this);
                pi.value = null;
                writeInt(buf.remaining());
                if (getTdsVersion().major >= 8) writeTdsCollation(pi);
                writeInt(buf.remaining());
                write(buf);
            }
        } else {
            writeInt(len);
            if (getTdsVersion().major >= 8) writeTdsCollation(pi);
            writeInt(len);
        }
    }

    private void writeTdsVARBINARY(final TdsParamInfo pi) throws IOException {
        writeByte(TdsTypes.SYBVARBINARY);
        writeByte(TdsTypes.VAR_MAX);
        if (pi.value == null) writeByte(0); else {
            final ByteBuffer buf = pi.getOutputBytes(this);
            if (!buf.hasRemaining() && (getTdsVersion().major < 7)) {
                writeByte(1);
                writeByte(0);
            } else {
                writeByte(buf.remaining());
                write(buf);
            }
        }
    }

    private void writeTdsTIMEN(final TdsParamInfo pi) throws IOException, SQLDataException {
        writeByte(pi.checkTdsType(TdsTypes.MSTIMEN, TdsTypes.SYBTIMEN));
        writeByte(4);
        writeTIMENValue((java.util.Date) pi.value);
    }

    final void writeTIMENValue(@Nullable final java.util.Date value) throws IOException {
        if (value == null) writeByte(0); else {
            this.calendar.calendar.setTime(value);
            writeByte(4);
            writeInt(this.calendar.encodeTime());
        }
    }

    private void writeTdsVARCHAR(final TdsParamInfo pi) throws IOException {
        if (getTdsVersion().major < 8) {
            assert false;
            writeTdsXVARCHAR(pi);
        } else if (pi.value == null) {
            writeByte(TdsTypes.SYBVARCHAR);
            writeByte(TdsTypes.VAR_MAX);
            writeByte(0);
        } else {
            final ByteBuffer buf = pi.getOutputBytes(this);
            if (buf.remaining() > TdsTypes.VAR_MAX) {
                final int tdsVersion = getTdsVersion().major;
                if ((buf.remaining() <= TdsTypes.MS_LONGVAR_MAX) && (tdsVersion >= 7)) {
                    writeByte(TdsTypes.XSYBVARCHAR);
                    writeShort(TdsTypes.MS_LONGVAR_MAX);
                    if (tdsVersion >= 8) writeTdsCollation(pi);
                    writeShort(buf.remaining());
                    write(buf);
                } else {
                    writeByte(TdsTypes.SYBTEXT);
                    writeInt(buf.remaining());
                    if (tdsVersion >= 8) writeTdsCollation(pi);
                    writeInt(buf.remaining());
                    write(buf);
                }
            } else {
                writeByte(TdsTypes.SYBVARCHAR);
                writeByte(TdsTypes.VAR_MAX);
                if (!buf.hasRemaining()) {
                    writeByte(1);
                    writeByte(' ');
                } else {
                    writeByte(buf.remaining());
                    write(buf);
                }
            }
        }
    }

    private void writeTdsUDTTIMESTAMP(final TdsParamInfo pi) throws IOException {
        writeByte(TdsTypes.UDT_TIMESTAMP);
        writeByte(8);
        if (pi.value == null) writeByte(0); else writeLong(((Long) pi.value).longValue());
    }

    private void writeTdsXNVARCHAR(final TdsParamInfo pi) throws IOException, SQLException {
        writeByte(TdsTypes.XSYBNVARCHAR);
        if (pi.value == null) {
            writeShort(TdsTypes.MS_LONGVAR_MAX);
            if (getTdsVersion().major >= 8) writeTdsCollation(pi);
            writeShort(0xffff);
        } else if (pi.value instanceof String) {
            writeTdsXNVARCHARString(pi, (String) pi.value);
        } else {
            Readable in;
            if (pi.value instanceof Readable) in = (Readable) pi.value; else if (pi.value instanceof Clob) in = ((Clob) pi.value).getCharacterStream(); else in = null;
            if (in == null) writeTdsXNVARCHARString(pi, pi.getString(pi.charsetInfo.name)); else {
                try {
                    if (IOStreams.providesReadFromStringInstanceFor(in)) writeTdsXNVARCHAROptimized(pi, in); else if (pi.length < Integer.MAX_VALUE) writeTdsXNVARCHARWithKnownLength(pi, in); else if (getTdsVersion().isYukonOrLater()) writeTdsXNVARCHARWithUnknownLength(pi, in); else writeTdsXNVARCHARString(pi, pi.getString(pi.charsetInfo.name));
                    IO.closeIfCloseable(in);
                    in = null;
                } finally {
                    pi.value = null;
                    IO.tryCloseIfCloseable(in);
                }
            }
        }
    }

    private void writeTdsXNVARCHARWithKnownLength(final TdsParamInfo pi, final Readable in) throws IOException, SQLException {
        pi.value = null;
        final int byteCount = pi.length * 2;
        if (byteCount <= TdsTypes.MS_LONGVAR_MAX) {
            writeShort(TdsTypes.MS_LONGVAR_MAX);
            if (getTdsVersion().major >= 8) writeTdsCollation(pi);
            writeShort(byteCount);
            transferCharsFrom(in, pi.length);
        } else {
            assert getTdsVersion().isYukonOrLater();
            writeShort(-1);
            writeTdsCollation(pi);
            writeLong(byteCount);
            writeInt(byteCount);
            transferCharsFrom(in, pi.length);
            writeInt(0);
        }
    }

    /**
   * Special optimization for java.io.StringReader.
   * E.g. Hibernate is using StringReader to wrap strings of bean properties annotated with javax.persistence.Lob.
   */
    private void writeTdsXNVARCHAROptimized(final TdsParamInfo pi, final Readable in) throws IOException, SQLException {
        String s = IOStreams.readStringMayReuseStringInstance(in, pi.length);
        if (s.length() != pi.length) {
            final int len = s.length();
            s = null;
            throw new IOException("number of characters in stream is less than the specified length, expected " + pi.length + ", got " + len);
        }
        writeTdsXNVARCHARString(pi, s);
    }

    private void writeTdsXNVARCHARString(final TdsParamInfo pi, final String s) throws IOException, SQLException {
        final long byteCount = s.length() * 2L;
        if (byteCount <= TdsTypes.MS_LONGVAR_MAX) {
            writeShort(TdsTypes.MS_LONGVAR_MAX);
            if (getTdsVersion().major >= 8) writeTdsCollation(pi);
            writeShort((int) byteCount);
            writeStringPlain(s);
        } else {
            assert getTdsVersion().isYukonOrLater();
            writeShort(-1);
            writeTdsCollation(pi);
            writeLong(byteCount);
            if (byteCount <= Integer.MAX_VALUE) {
                writeInt((int) byteCount);
                writeStringPlain(s);
            } else {
                writeInt(Integer.MAX_VALUE);
                writeStringPlain(s.substring(0, Integer.MAX_VALUE));
                writeInt(s.length() - Integer.MAX_VALUE);
                writeStringPlain(s.substring(Integer.MAX_VALUE));
            }
            writeInt(0);
        }
    }

    private void writeTdsXNVARCHARWithUnknownLength(final TdsParamInfo pi, final Readable in) throws IOException, SQLException {
        assert getTdsVersion().isYukonOrLater();
        pi.value = null;
        writeShort(-1);
        writeTdsCollation(pi);
        writeLong(-2);
        final ByteBuffer sharedBuffer = socket().getSharedBuffer(512);
        final CharBuffer buf = sharedBuffer.asCharBuffer();
        while (true) {
            buf.clear();
            final int step = in.read(buf);
            if (step <= 0) {
                break;
            } else if (step != buf.position()) {
                throw new IOException("return value/position mismatch:" + "\nreturn       = " + step + "\nposition     = " + buf.position() + "\nstream class = " + in.getClass().getName());
            } else {
                writeInt(step << 1);
                buf.flip();
                transferCharsFrom(buf, step);
            }
        }
        socket().releaseSharedBuffer(sharedBuffer);
        writeInt(0);
    }

    private void writeTdsXVARBINARY(final TdsParamInfo pi) throws IOException, SQLException {
        writeByte(TdsTypes.XSYBVARBINARY);
        if (pi.value == null) {
            writeShort(TdsTypes.MS_LONGVAR_MAX);
            writeShort(0xFFFF);
        } else if (pi.value instanceof byte[]) {
            writeTdsXVARBINARYArray((byte[]) pi.value);
        } else {
            InputStream in;
            if (pi.value instanceof InputStream) in = (InputStream) pi.value; else if (pi.value instanceof Blob) in = ((Blob) pi.value).getBinaryStream(); else in = null;
            if (in == null) writeTdsXVARBINARYBuffer(pi.getOutputBytes(this)); else {
                pi.value = null;
                try {
                    if (pi.length <= TdsTypes.MS_LONGVAR_MAX) writeTdsXVARBINARYSmall(pi, in); else if (pi.length < Integer.MAX_VALUE) writeTdsXVARBINARYWithKnownLength(pi, in); else writeTdsXVARBINARYWithUnknownLength(in);
                    in.close();
                    in = null;
                } finally {
                    IO.tryClose(in);
                }
            }
        }
    }

    private void writeTdsXVARBINARYArray(final byte[] a) throws IOException, SQLException {
        writeTdsXVARBINARYArray(a, 0, a.length);
    }

    private void writeTdsXVARBINARYArray(final byte[] a, final int offset, final int length) throws IOException, SQLException {
        if (length <= TdsTypes.MS_LONGVAR_MAX) {
            writeShort(TdsTypes.MS_LONGVAR_MAX);
            writeShort(length);
            write(a, offset, length);
        } else {
            assert getTdsVersion().isYukonOrLater();
            writeShort(-1);
            writeLong(length);
            writeInt(length);
            write(a, offset, length);
            writeInt(0);
        }
    }

    private void writeTdsXVARBINARYBuffer(final ByteBuffer buf) throws IOException, SQLException {
        if (buf.remaining() <= TdsTypes.MS_LONGVAR_MAX) {
            writeShort(TdsTypes.MS_LONGVAR_MAX);
            writeShort(buf.remaining());
            write(buf);
        } else {
            assert getTdsVersion().isYukonOrLater();
            writeShort(-1);
            writeLong(buf.remaining());
            writeInt(buf.remaining());
            write(buf);
            writeInt(0);
        }
    }

    private void writeTdsXVARBINARYSmall(final TdsParamInfo pi, final InputStream in) throws IOException, SQLException {
        writeShort(TdsTypes.MS_LONGVAR_MAX);
        writeShort(pi.length);
        transferFrom(in, pi.length);
    }

    private void writeTdsXVARBINARYWithKnownLength(final TdsParamInfo pi, final InputStream in) throws IOException, SQLException {
        assert getTdsVersion().isYukonOrLater();
        writeShort(-1);
        writeLong(pi.length);
        writeInt(pi.length);
        transferFrom(in, pi.length);
        writeInt(0);
    }

    private void writeTdsXVARBINARYWithUnknownLength(final InputStream in) throws IOException, SQLException {
        assert getTdsVersion().isYukonOrLater();
        writeShort(-1);
        writeLong(-2);
        final ByteBuffer buf = socket().getSharedBuffer(1024);
        final byte[] a = buf.array();
        while (true) {
            buf.clear();
            final int step = in.read(a, 0, a.length);
            if (step <= 0) {
                break;
            } else if (step > a.length) {
                throw new IOException("the source stream reported more bytes than requested:" + "\nrequested    = " + a.length + "\nreturned     = " + step + "\nstream class = " + in.getClass().getName());
            } else {
                writeInt(step);
                write(a, 0, step);
            }
        }
        socket().releaseSharedBuffer(buf);
        writeInt(0);
    }

    private void writeTdsXVARCHAR(final TdsParamInfo pi) throws IOException {
        if (pi.value == null) {
            writeByte(TdsTypes.XSYBVARCHAR);
            writeShort(TdsTypes.MS_LONGVAR_MAX);
            if (getTdsVersion().major >= 8) writeTdsCollation(pi);
            writeShort(0xFFFF);
        } else {
            final ByteBuffer buf = pi.getOutputBytes(this);
            if (buf.remaining() <= TdsTypes.MS_LONGVAR_MAX) {
                writeByte(TdsTypes.XSYBVARCHAR);
                writeShort(TdsTypes.MS_LONGVAR_MAX);
                if (getTdsVersion().major >= 8) writeTdsCollation(pi);
                writeShort(buf.remaining());
                write(buf);
            } else if (getTdsVersion().isYukonOrLater()) {
                writeByte(TdsTypes.XSYBVARCHAR);
                writeShort(-1);
                writeTdsCollation(pi);
                writeLong(buf.remaining());
                writeInt(buf.remaining());
                write(buf);
                writeInt(0);
            } else {
                writeByte(TdsTypes.SYBTEXT);
                writeInt(buf.remaining());
                if (getTdsVersion().major >= 8) writeTdsCollation(pi);
                writeInt(buf.remaining());
                write(buf);
            }
        }
    }
}

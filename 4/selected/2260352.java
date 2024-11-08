package jaxlib.io.stream;

import java.io.DataInput;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import jaxlib.jaxlib_private.CheckArg;
import jaxlib.jaxlib_private.io.CheckIOArg;
import jaxlib.lang.ReturnValueException;

/**
 * Common I/O stream utilities.
 * <p>
 * <b>Stream implementations should never call methods of this class with themselves as argument.</b>
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: IOStreams.java,v 1.3 2004/09/14 19:59:39 joerg_wassmer Exp $
 */
public class IOStreams {

    protected IOStreams() throws InstantiationException {
        throw new InstantiationException();
    }

    public static int read(InputStream in, ByteBuffer dest) throws IOException {
        if (in instanceof ReadableByteChannel) {
            return ((ReadableByteChannel) in).read(dest);
        } else if (dest.hasArray()) {
            int count = in.read(dest.array(), dest.arrayOffset() + dest.position(), dest.remaining());
            if (count > 0) dest.position(dest.position() + count);
            return count;
        } else {
            int count = 0;
            for (int max = dest.remaining(); count < max; count++) {
                int b = in.read();
                if (b >= 0) {
                    dest.put((byte) b);
                } else {
                    break;
                }
            }
            return (count == 0) ? -1 : count;
        }
    }

    /**
   * Works the same way than <code>DataInput.readFully(dest, off, len)</code>.
   *
   * @see XDataInput#readFully(byte[],int,int)
   *
   * @since JaXLib 1.0
   */
    public static void readFully(InputStream in, byte[] dest, int off, int len) throws IOException {
        if (in instanceof DataInput) {
            ((DataInput) in).readFully(dest, off, len);
        } else {
            while (len > 0) {
                int step = in.read(dest, off, len);
                if (step < 0) throw new EOFException();
                off += step;
                len -= step;
            }
        }
    }

    public static void readFully(InputStream in, ByteBuffer dest) throws IOException {
        if (in instanceof XInputStream) {
            ((XInputStream) in).readFully(dest);
        } else {
            int remaining = dest.remaining();
            if (remaining > 0) {
                if (dest.hasArray()) {
                    byte[] a = dest.array();
                    int offs = dest.arrayOffset() + dest.position();
                    while (remaining > 0) {
                        int step = in.read(a, offs, remaining);
                        if (step < 0) {
                            throw new EOFException();
                        } else {
                            offs += step;
                            remaining -= step;
                            dest.position(dest.position() + step);
                        }
                    }
                } else {
                    while (--remaining >= 0) {
                        int b = in.read();
                        if (b < 0) throw new EOFException(); else dest.put((byte) b);
                    }
                }
            }
        }
    }

    /**
   * Reads up to the specified count of characters in specified stream.
   * <p>
   * This method reads characters from specified stream until either the specified maximum count or the end
   * of the stream has been reached.
   * </p><p>
   * You should avoid specifying a count which is much greater than the count of remaining characters in 
   * the stream. This method will allocate a buffer with capacity equal to <tt>maxCount</tt>, if not
   * negative.
   * </p>
   *
   * @param in       the source stream.
   * @param maxCount the maximum count of characters to read; <tt>-1</tt> for no limit.
   *
   * @throws IOException              if an I/O error occurs.
   * @throws IllegalArgumentException if <code>maxCount < -1</code>.
   *
   * @since JaXLib 1.0
   */
    public static String readString(Readable in, int maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) return "";
        if (in instanceof CharBuffer) {
            CharBuffer buf = (CharBuffer) in;
            if ((maxCount < 0) || (maxCount >= buf.remaining())) {
                String s = buf.toString();
                buf.position(buf.limit());
                return s;
            } else {
                CharBuffer b = buf.duplicate();
                b.limit(b.position() + maxCount);
                String s = b.toString();
                buf.position(b.limit());
                return s;
            }
        } else if (maxCount < 0) {
            CharBuffer buf = CharBuffer.allocate(8192);
            while (in.read(buf) >= 0) {
                if (!buf.hasRemaining()) {
                    CharBuffer b = CharBuffer.allocate(buf.capacity() * 2);
                    buf.flip();
                    b.put(buf);
                    buf = b;
                }
            }
            buf.flip();
            return buf.toString();
        } else {
            CharBuffer buf = CharBuffer.allocate(maxCount);
            while (buf.hasRemaining() && (in.read(buf) >= 0)) ;
            buf.flip();
            return buf.toString();
        }
    }

    /**
   * Skips bytes in the stream until either the specified number of bytes have been skipped or the end
   * of the stream has been reached.
   *
   * @return the number of bytes skipped.
   *
   * @param in        the stream
   * @param maxCount  the maximum number of bytes to skip, <tt>-1</tt> for no limit.
   *
   * @throws IOException              if an I/O error occurs.
   * @throws IllegalArgumentException if <code>maxCount < -1</code>.
   *
   * @since JaXLib 1.0
   */
    public static long skip(InputStream in, long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        long skipped = 0;
        while (true) {
            long remaining = (maxCount < 0) ? Long.MAX_VALUE : (maxCount - skipped);
            if (remaining <= 0) break;
            long step = in.skip(remaining);
            if (step > remaining) throw new ReturnValueException(in, "skip" + step + "<=" + remaining);
            if (step <= 0) break;
            skipped += step;
        }
        return skipped;
    }

    /**
   * Skips bytes in the stream until either the specified number of characters have been skipped or the end
   * of the stream has been reached.
   *
   * @return the number of characters skipped.
   *
   * @param in        the stream
   * @param maxCount  the maximum number of characters to skip, <tt>-1</tt> for no limit.
   *
   * @throws IOException              if an I/O error occurs.
   * @throws IllegalArgumentException if <code>maxCount < -1</code>.
   *
   * @since JaXLib 1.0
   */
    public static long skip(Reader in, long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        long skipped = 0;
        while (true) {
            long remaining = (maxCount < 0) ? Long.MAX_VALUE : (maxCount - skipped);
            if (remaining <= 0) break;
            long step = in.skip(remaining);
            if (step > remaining) throw new ReturnValueException(in, "skip" + step + "<=" + remaining);
            if (step <= 0) break;
            skipped += step;
        }
        return skipped;
    }

    /**
   * Skips exactly the specified number of bytes in the specified stream.
   *
   * @param in        the stream.
   * @param count     the number of bytes to skip.
   *
   * @throws IOException              
   *         if an I/O error occurs.
   * @throws EOFException             
   *         if there are less than the specified number of bytes are remaining in the stream.
   * @throws IllegalArgumentException 
   *         if <code>count < 0</code>.
   *
   * @since JaXLib 1.0
   */
    public static void skipFully(InputStream in, long count) throws IOException, EOFException {
        CheckIOArg.count(count);
        while (count > 0) {
            long step = in.skip(count);
            if (step <= 0) throw new EOFException();
            count -= step;
        }
    }

    /**
   * Skips exactly the specified number of characters in the specified stream.
   *
   * @param in        the stream.
   * @param count     the number of characters to skip.
   *
   * @throws IOException              
   *         if an I/O error occurs.
   * @throws EOFException             
   *         if there are less than the specified number of characters are remaining in the stream.
   * @throws IllegalArgumentException 
   *         if <code>count < 0</code>.
   *
   * @since JaXLib 1.0
   */
    public static void skipFully(Reader in, long count) throws IOException, EOFException {
        CheckIOArg.count(count);
        while (count > 0) {
            long step = in.skip(count);
            if (step <= 0) throw new EOFException();
            count -= step;
        }
    }

    /**
   * Transfers up to the specified number of bytes from specified input to specified output.
   * <p>
   * This method tries to avoid an intermediate buffer for the transfer.
   * </p>
   *
   * @param in        the input stream.
   * @param out       the output stream.
   * @param maxCount  the maximum number of bytes to transfer, or <tt>-1</tt> for no limit.
   *
   * @throws IOException
   *         if an I/O error occurs.
   * @throws IllegalArgumentException
   *         if <code>maxCount < -1</code>
   * @throws NullPointerException 
   *         if <code>(in == null) || (out == null)</code>.
   *
   * @see XInputStream#transferTo(OutputStream,long)
   *
   * @since JaXLib 1.0
   */
    public static long transfer(InputStream in, OutputStream out, long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) return 0; else if (in instanceof XDataInput) return ((XDataInput) in).transferTo(out, maxCount); else if (out instanceof XDataOutput) return ((XDataOutput) out).transferFrom(in, maxCount); else {
            long count = 0;
            for (int b; (count != maxCount) && ((b = in.read()) >= 0); ) {
                out.write(b);
                count++;
            }
            return count;
        }
    }

    /**
   * Transfers up to the specified number of characters from specified input to specified output.
   * <p>
   * This method tries to avoid an intermediate buffer for the transfer.
   * </p>
   *
   * @param in        the character input stream.
   * @param out       the character output stream.
   * @param maxCount  the maximum number of characters to transfer, or <tt>-1</tt> for no limit.
   *
   * @throws IOException
   *         if an I/O error occurs.
   * @throws IllegalArgumentException
   *         if <code>maxCount < -1</code>
   * @throws NullPointerException 
   *         if <code>(in == null) || (out == null)</code>.
   *
   * @see XReader#transferTo(Appendable,long)
   *
   * @since JaXLib 1.0
   */
    public static long transfer(Readable in, Appendable out, long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) return 0; else if (in instanceof XReader) return ((XReader) in).transferTo(out, maxCount); else if (out instanceof XWriter) return ((XWriter) out).transferFrom(in, maxCount); else if (in instanceof Reader) {
            Reader rin = (Reader) in;
            long count = 0;
            for (int b; (count != maxCount) && ((b = rin.read()) >= 0); ) {
                out.append((char) b);
                count++;
            }
            return count;
        } else if (in instanceof CharBuffer) {
            CharBuffer b = (CharBuffer) in;
            long count = (maxCount < 0) ? b.remaining() : Math.min(maxCount, b.remaining());
            if (count == b.remaining()) {
                out.append(b);
                b.position(b.limit());
            } else {
                CharBuffer c = b.slice();
                c.limit((int) count);
                out.append(c);
                b.position(b.position() + (int) count);
            }
            return count;
        } else {
            int bufSize = (maxCount < 0) ? 256 : (int) Math.min(256, maxCount);
            CharBuffer buf = CharBuffer.allocate(bufSize);
            long count = 0;
            while (count != maxCount) {
                int step = (maxCount < 0) ? bufSize : (int) Math.min(maxCount - count, bufSize);
                buf.position(0);
                buf.limit(step);
                int a = in.read(buf);
                if (a < 0) break; else if (a != buf.position()) throw new ReturnValueException(in, "read(CharBuffer)" + a + "== buf.position()"); else if (a > step) throw new ReturnValueException(in, "read(CharBuffer)" + a + "<=" + step); else {
                    buf.flip();
                    out.append(buf);
                    count += a;
                }
            }
            return count;
        }
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
   * @throws IOException                if one occurs in the <tt>writeLong</tt> method of the stream.
   * @throws NullPointerException       if <tt>(out == null) || (data == null)</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(OutputStream out, ByteBuffer data) throws IOException {
        final int count = data.remaining();
        if (count == 0) {
            return 0;
        } else if (data.hasArray()) {
            out.write(data.array(), data.arrayOffset() + data.position(), count);
            data.position(data.limit());
        } else {
            WritableByteChannel ch;
            if (out instanceof WritableByteChannel) ch = (WritableByteChannel) out; else if (out instanceof FileOutputStream) ch = ((FileOutputStream) out).getChannel(); else ch = null;
            if (ch == null) {
                for (int r = count; --r >= 0; ) out.write(data.get());
            } else {
                for (int r = count; r > 0; ) {
                    int step = ch.write(data);
                    if ((step < 0) || (step > r)) throw new ReturnValueException(ch, "write(ByteBuffer)" + step, ">= 0 && <=" + r);
                    r -= step;
                }
            }
        }
        return count;
    }
}

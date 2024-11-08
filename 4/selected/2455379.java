package jaxlib.io.channel;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import javax.annotation.Nullable;
import jaxlib.buffer.ByteBuffers;
import jaxlib.jaxlib_private.CheckArg;
import jaxlib.lang.JavaTypeId;
import jaxlib.util.CheckBounds;

/**
 * Provides utilities to work with <tt>FileChannel</tt> instances.
 * <p>
 * All methods are implemented in a way such as almost no intermediate buffering takes place when transfering
 * bytes from or to a file channel. Instead the read and write operations are using either the
 * {@link FileChannel#transferFrom(ReadableByteChannel,long,long) transferFrom} or
 * {@link FileChannel#transferTo(long,long,WritableByteChannel) transferTo} methods of
 * <tt>FileChannel</tt> instances werever applicable.
 * </p><p>
 * The specification of a {@link FileChannel} allows methods returning normally from incomplete
 * operations.<br>
 * The methods in this class are implemented in a way such they are blocking until their operation is complete.
 * The operations can be interrupted by closing the file channel or by interrupting the thread, as specified by
 * the {@link java.nio.channels.InterruptibleChannel} interface, which is implemented by the <tt>FileChannel</tt>
 * class.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: FileChannels.java 3029 2011-12-29 00:36:48Z joerg_wassmer $
 */
public class FileChannels extends SeekableByteChannels {

    protected FileChannels() throws InstantiationException {
        throw new InstantiationException();
    }

    private static void checkCopyRange(final long size, final long sourceIndex, final long destSize, final long destIndex, final long length) {
        if (sourceIndex < 0) throw new IndexOutOfBoundsException("sourceIndex(" + sourceIndex + ") < 0");
        if (destIndex < 0) throw new IndexOutOfBoundsException("destIndex(" + destIndex + ") < 0");
        if (length < 0) throw new IndexOutOfBoundsException("length(" + length + ") < 0");
        if ((sourceIndex + length) > size) throw new IndexOutOfBoundsException("sourceIndex(" + sourceIndex + ") + length(" + length + ") > file size(" + size + ")");
        if (destIndex > destSize) throw new IndexOutOfBoundsException("destination index(" + destIndex + ") > destination file size(" + destSize + ")");
    }

    /**
   * Removes from specified file specified count of bytes beginning at the file's current position.
   * After this call the length of the file is its initial length decremented by the specified count
   * and the file's position is equal to the specified index.
   *
   * @param f         the file to remove bytes from.
   * @param fromIndex the index of the first byte to remove.
   * @param count     the number of bytes to remove.
   *
   * @throws IOException               if an I/O error occurs.
   * @throws IllegalArgumentException  if <tt>count < 0</tt>.
   * @throws IndexOutOfBoundsException if <code>(fromIndex < 0) || (fromIndex + count > f.size()</code>.
   * @throws NullPointerException      if <tt>f == null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static void clear(final FileChannel f, final long fromIndex, final long count) throws IOException {
        CheckArg.notNegative(count, "count");
        final long size = f.size();
        CheckBounds.offset(size, fromIndex, count);
        if (count == 0) return;
        if (fromIndex + count == size) {
            f.position(fromIndex);
            f.truncate(fromIndex);
        } else {
            copy(f, fromIndex + count, f, fromIndex, size - (fromIndex + count));
            f.truncate(size - count);
            f.position(fromIndex);
        }
    }

    /**
   * Copies specified region of the specified file to specified destination index in the same file.
   * The behaviour of this method is similar to the
   * {@link System#arraycopy(Object,int,Object,int,int) System.arraycopy} method.
   * <p>
   * The copying is performed as if the source region would be copied to a temporary array and then the array
   * would be written at the destination index.
   * </p><p>
   * If the file is not big enough to copy the source range to, then the file grows
   * exactly for as many bytes as required.
   * </p><p>
   * After this call the position of the file pointer is unspecified.
   * </p>
   *
   * @param f           the file.
   * @param sourceIndex the index in the file of the first byte to copy.
   * @param destIndex   the index in the file where to copy the first source byte to.
   * @param length      the number of bytes to copy.
   *
   * @throws IOException
   *         if an I/O error occurs.
   * @throws IndexOutOfBoundsException
   *         if <code>(sourceIndex < 0)
   *               || (destIndex < 0)
   *               || (length < 0)
   *               || (sourceIndex + length > f.size())
   *               || (destIndex > f.size())
   *            </code>.
   * @throws NullPointerException
   *         if <code>f == null</code>.
   *
   * @since JaXLib 1.0
   */
    public static void copy(final FileChannel f, final long sourceIndex, final long destIndex, final long length) throws IOException {
        final long size = f.size();
        checkCopyRange(size, sourceIndex, size, destIndex, length);
        copy0(f, size, sourceIndex, destIndex, length);
    }

    private static void copy0(FileChannel f, long size, long sourceIndex, long destIndex, long length) throws IOException {
        if ((length == 0) || (sourceIndex == destIndex)) return; else if ((destIndex < sourceIndex) || (destIndex >= sourceIndex + length)) {
            f.position(sourceIndex);
            while (length > 0) {
                final long step = f.transferFrom(f, destIndex, length);
                if ((step < length) && (f.size() != size)) throw new IOException("file has been modified concurrently by another process");
                assert (step >= 0);
                length -= step;
                sourceIndex += step;
                destIndex += step;
            }
        } else {
            final long overlap = (sourceIndex + length) - destIndex;
            copy0(f, size, destIndex, destIndex + length - overlap, overlap);
            copy0(f, size, sourceIndex, destIndex, destIndex - sourceIndex);
        }
    }

    /**
   * Copies specified region of the specified source file to specified destination file at specified index.
   * The behaviour of this method is similar to the
   * {@link System#arraycopy(Object,int,Object,int,int) System.arraycopy} method.
   * <p>
   * If the source <tt>RandomAccessFile</tt> is identical to the destination instance, then the copying is
   * performed as if the source region would be copied to a temporary array and than the array would be written
   * at the destination index.
   * </p><p>
   * If the destination file is not big enough to copy the source range to, then the destination file grows
   * exactly for as many bytes as required. The destination index may be bigger than its initial
   * length, in which case the values of the bytes between the initial length and the index are undefined.
   * </p><p>
   * After this call the position of the file pointers are unspecified.
   * The behaviour of this method is undefined if the two <tt>RandomAccessFile</tt> instances are not
   * identical but pointing to the same file on the filesystem.
   * </p>
   *
   * @param source      the file to copy bytes from.
   * @param sourceIndex the index in the source file of the first byte to copy.
   * @param dest        the file to copy bytes to.
   * @param destIndex   the index in the destination file where to copy the first byte to.
   * @param length      the number of bytes to copy.
   *
   * @throws IOException                if an I/O error occurs.
   * @throws IndexOutOfBoundsException
   *         if <code>(sourceIndex < 0)
   *               || (destIndex < 0)
   *               || (length < 0)
   *               || (sourceIndex + length > source.length())
   *               || (destIndex > dest.size())
   *            </code>.
   * @throws NullPointerException       if a <tt>null</tt> file was specified.
   *
   * @since JaXLib 1.0
   */
    public static void copy(final FileChannel source, long sourceIndex, final FileChannel dest, long destIndex, long length) throws IOException {
        if (source == dest) copy(source, sourceIndex, destIndex, length); else {
            final long sourceSize = source.size();
            checkCopyRange(sourceSize, sourceIndex, dest.size(), destIndex, length);
            if (length != 0) {
                source.position(sourceIndex);
                while (length > 0) {
                    final long step = dest.transferFrom(source, destIndex, length);
                    if ((step == 0) && (source.size() != sourceSize)) throw new IOException("file has been modified concurrently by another process");
                    length -= step;
                    sourceIndex += step;
                    destIndex += step;
                }
            }
        }
    }

    /**
   * Fills specified file beginning at its current position with specified count of specified byte.
   * In other words, replaces specified count of bytes in the file by the specified byte, and grows the
   * file if necessary.
   * <p>
   * After this call the file's position is its initial position incremented by specified count.
   * </p>
   *
   * @param e     the byte to fill the file with.
   * @param f     the file to fill.
   * @param count the number of bytes to fill the file with.
   *
   * @throws IOException              if an I/O error occurs.
   * @throws IllegalArgumentException if <tt>count < 0</tt>.
   * @throws NullPointerException     if <tt>f == null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static void fill(final FileChannel f, final byte e, final long count) throws IOException {
        if (count == 0) return;
        CheckArg.notNegative(count, "count");
        final class ByteCopiesChannel extends Object implements ReadableByteChannel {

            long remaining = count;

            ByteCopiesChannel() {
                super();
            }

            @Override
            public final boolean isOpen() {
                return true;
            }

            @Override
            public final void close() {
            }

            @Override
            public final int read(final ByteBuffer dest) {
                return ByteBuffers.fill(dest, e);
            }
        }
        transferFromByteChannel(new ByteCopiesChannel(), f, count);
    }

    /**
   * Find the position of a text line.
   * Search begins at the channels actual position. After this call the channel will we positioned to the beginning of
   * a line if at least one has been found. If the argument number of lines is negative then this method searches
   * backwards. If the argument number is zero then this call has no effect.
   *
   * @param f
   *  the file to search in.
   * @param lineCount
   *  the number of lines to find, negative to search backwards.
   * @param buf
   *  optional temporary buffer to use while searching.
   *
   * @return
   *  the number of lines found.
   *
   * @throws IOException
   *  on any I/O error.
   *
   * @since JaXLib 1.0
   */
    public static long findLine(final FileChannel f, final long lineCount, @Nullable final byte[] buf) throws IOException {
        if (lineCount > 0) return findLineForward(f, lineCount, buf);
        if (lineCount < 0) return findLineBackward(f, (lineCount == Long.MIN_VALUE) ? Long.MAX_VALUE : -lineCount, buf);
        return 0;
    }

    private static long findLineBackward(final FileChannel f, final long lineCount, @Nullable final byte[] aBuf) throws IOException {
        long pos = Math.min(f.position(), f.size());
        if (pos <= 0) return 0;
        final ByteBuffer buf = ((aBuf != null) && (aBuf.length > 0)) ? ByteBuffer.wrap(aBuf) : ByteBuffer.allocate(Math.min(8192, (int) Math.min(pos, Math.max(8192, lineCount * 128))));
        final byte[] a = buf.array();
        long foundPos = -1;
        long linesFound = 0;
        boolean n = false;
        READ: while (pos > 0) {
            buf.limit((a.length > pos) ? (int) pos : a.length).position(0);
            int i = f.read(buf, pos - buf.limit());
            if (i <= 0) break READ;
            pos -= i;
            while (--i >= 0) {
                final byte b = a[i];
                if (b == '\n') {
                    foundPos = pos + i + 1;
                    if (++linesFound >= lineCount) break READ;
                    n = true;
                } else if (b == '\r') {
                    if (!n) {
                        foundPos = pos + i + 1;
                        if (++linesFound >= lineCount) break READ;
                    }
                    n = false;
                } else {
                    n = false;
                }
            }
        }
        if (foundPos >= 0) f.position(foundPos);
        return linesFound;
    }

    private static long findLineForward(final FileChannel f, final long lineCount, @Nullable final byte[] aBuf) throws IOException {
        final long size = f.size();
        long pos = f.position();
        if (pos >= size) return 0;
        final ByteBuffer buf = ((aBuf != null) && (aBuf.length > 0)) ? ByteBuffer.wrap(aBuf) : ByteBuffer.allocate(Math.min(8192, (int) Math.min(size - pos, Math.max(8192, lineCount * 128))));
        final byte[] a = buf.array();
        long foundPos = -1;
        long linesFound = 0;
        boolean r = false;
        READ: while (pos < size) {
            buf.clear();
            final int count = f.read(buf, pos);
            if (count <= 0) break READ;
            for (int i = 0; i < count; i++) {
                final byte b = a[i];
                if (b == '\n') {
                    foundPos = pos + i + 1;
                    if (++linesFound >= lineCount) break READ;
                    r = false;
                } else if (b == '\r') {
                    if (r) {
                        foundPos = pos + i + 1;
                        if (++linesFound >= lineCount) break READ;
                    }
                    r = true;
                } else if (r) {
                    foundPos = pos + i + 1;
                    if (++linesFound >= lineCount) break READ;
                    r = false;
                }
            }
            pos += count;
        }
        if (foundPos >= 0) f.position(foundPos);
        return linesFound;
    }

    /**
   * Moves specified sequence of the specified file to specified destination index in the same file.
   * The source sequence is copied and than removed from the file.
   * <p>
   * Copying is performed as if the source region would be copied to a temporary array and then the array would
   * be written at the destination index.
   * </p><p>
   * The position of the file pointer after this call is unspecified.
   * </p>
   *
   * @param f           the file.
   * @param sourceIndex the index in the file of the first byte to move.
   * @param destIndex   the index in the file where to move the first source byte to.
   * @param count       the number of bytes to move.
   *
   * @throws IOException
   *         if an I/O error occurs.
   * @throws IndexOutOfBoundsException
   *         if <code>(sourceIndex < 0)
   *               || (destIndex < 0)
   *               || (length < 0)
   *               || (sourceIndex + length > f.size())
   *               || (destIndex > f.size())
   *            </code>.
   * @throws NullPointerException
   *         if <code>f == null</code>
   *
   * @see #copy(FileChannel,long,FileChannel,long,long)
   * @see #clear(FileChannel,long,long)
   *
   * @since JaXLib 1.0
   */
    public static void move(final FileChannel f, final long sourceIndex, final long destIndex, final long count) throws IOException {
        if (sourceIndex == destIndex) {
            final long size = f.size();
            checkCopyRange(size, sourceIndex, size, destIndex, count);
        } else if ((destIndex < sourceIndex) && (destIndex + count >= sourceIndex)) {
            final long size = f.size();
            checkCopyRange(size, sourceIndex, size, destIndex, count);
            clear(f, destIndex, sourceIndex - destIndex);
        } else if ((destIndex > sourceIndex) && (destIndex < sourceIndex + count)) {
            final long size = f.size();
            checkCopyRange(size, sourceIndex, size, destIndex, count);
            clear(f, destIndex + count, destIndex - sourceIndex);
        } else {
            copy(f, sourceIndex, destIndex, count);
            clear(f, sourceIndex, count);
        }
    }

    /**
   * Moves specified sequence of the specified source file to specified destination file at specified index.
   * The source sequence is copied to the destination file and than removed from the source file.
   * <p>
   * If the source <tt>RandomAccessFile</tt> is identical to the destination file instance, then the copying
   * is performed as if the source region would be copied to a temporary array and then the array would be
   * written at the destination index.
   * </p><p>
   * If the destination file is not big enough to copy the source range to, then the destination file grows
   * exactly for as many bytes as required.
   * </p><p>
   * After this call the positions of the file pointers are unspecified.
   * The behaviour of this method is undefined if the two <tt>RandomAccessFile</tt> instances are not
   * identical but pointing to the same file on the filesystem.
   * </p>
   *
   * @param source      the file to move bytes from.
   * @param sourceIndex the index in the source file of the first byte to move.
   * @param dest        the file to move bytes to.
   * @param destIndex   the index in the destination file where to move the first byte to.
   * @param count       the number of bytes to move.
   *
   * @throws IOException
   *         if an I/O error occurs.
   * @throws IndexOutOfBoundsException
   *         if <code>(sourceIndex < 0)
   *               || (destIndex < 0)
   *               || (length < 0)
   *               || (sourceIndex + length > source.size())
   *               || (destIndex > dest.size())
   *            </code>.
   * @throws NullPointerException
   *         if a <tt>null</tt> file was specified.
   *
   * @see #copy(FileChannel,long,FileChannel,long,long)
   * @see #clear(FileChannel,long,long)
   *
   * @since JaXLib 1.0
   */
    public static void move(final FileChannel source, final long sourceIndex, final FileChannel dest, final long destIndex, final long count) throws IOException {
        if (source == dest) move(source, sourceIndex, destIndex, count); else {
            copy(source, sourceIndex, dest, destIndex, count);
            clear(source, sourceIndex, count);
        }
    }

    /**
   * Reads from the specified file starting at its current position as many bytes as remaining in the specified
   * buffer.
   * <p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes readed.
   * </p>
   *
   * @param src    the file to read from.
   * @param dest   the buffer to read into.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws EOFException         if there are not enough remaining bytes in the file at its initial position.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static void readFully(final FileChannel src, final ByteBuffer dest) throws IOException {
        while (dest.hasRemaining()) {
            if (src.read(dest) < 0) throw new EOFException();
        }
    }

    /**
   * Reads from the specified file starting at its current position as many values as remaining in the specified
   * buffer.
   * <p>
   * The bytes of the values are decoded in {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN} order.
   * </p><p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes readed.
   * </p>
   *
   * @param src    the file to read from.
   * @param dest   the buffer to read into.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws EOFException         if there are not enough remaining bytes in the file at its initial position.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static void readFully(final FileChannel src, final CharBuffer dest) throws IOException {
        readFully(src, dest, ByteOrder.BIG_ENDIAN);
    }

    /**
   * Reads from the specified file starting at its current position as many values as remaining in the specified
   * buffer, decoding the bytes in the file according to the specified byte order.
   * <p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes readed.
   * </p>
   *
   * @param src    the file to read from.
   * @param dest   the buffer to read into.
   * @param order  the byte order of the values in the file.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws EOFException         if there are not enough remaining bytes in the file at its initial position.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static void readFully(final FileChannel src, final CharBuffer dest, final ByteOrder order) throws IOException {
        readFullyImpl(src, dest, order, JavaTypeId.CHAR, 1);
    }

    /**
   * Reads from the specified file starting at its current position as many values as remaining in the specified
   * buffer.
   * <p>
   * The bytes of the values are decoded in {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN} order.
   * </p><p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes readed.
   * </p>
   *
   * @param src    the file to read from.
   * @param dest   the buffer to read into.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws EOFException         if there are not enough remaining bytes in the file at its initial position.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static void readFully(final FileChannel src, final DoubleBuffer dest) throws IOException {
        readFully(src, dest, ByteOrder.BIG_ENDIAN);
    }

    /**
   * Reads from the specified file starting at its current position as many values as remaining in the specified
   * buffer, decoding the bytes in the file according to the specified byte order.
   * <p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes readed.
   * </p>
   *
   * @param src    the file to read from.
   * @param dest   the buffer to read into.
   * @param order  the byte order of the values in the file.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws EOFException         if there are not enough remaining bytes in the file at its initial position.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static void readFully(final FileChannel src, final DoubleBuffer dest, final ByteOrder order) throws IOException {
        readFullyImpl(src, dest, order, JavaTypeId.DOUBLE, 3);
    }

    /**
   * Reads from the specified file starting at its current position as many values as remaining in the specified
   * buffer.
   * <p>
   * The bytes of the values are decoded in {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN} order.
   * </p><p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes readed.
   * </p>
   *
   * @param src    the file to read from.
   * @param dest   the buffer to read into.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws EOFException         if there are not enough remaining bytes in the file at its initial position.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static void readFully(final FileChannel src, final FloatBuffer dest) throws IOException {
        readFully(src, dest, ByteOrder.BIG_ENDIAN);
    }

    /**
   * Reads from the specified file starting at its current position as many values as remaining in the specified
   * buffer, decoding the bytes in the file according to the specified byte order.
   * <p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes readed.
   * </p>
   *
   * @param src    the file to read from.
   * @param dest   the buffer to read into.
   * @param order  the byte order of the values in the file.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws EOFException         if there are not enough remaining bytes in the file at its initial position.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static void readFully(final FileChannel src, final FloatBuffer dest, final ByteOrder order) throws IOException {
        readFullyImpl(src, dest, order, JavaTypeId.FLOAT, 2);
    }

    /**
   * Reads from the specified file starting at its current position as many values as remaining in the specified
   * buffer.
   * <p>
   * The bytes of the values are decoded in {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN} order.
   * </p><p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes readed.
   * </p>
   *
   * @param src    the file to read from.
   * @param dest   the buffer to read into.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws EOFException         if there are not enough remaining bytes in the file at its initial position.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static void readFully(final FileChannel src, final IntBuffer dest) throws IOException {
        readFully(src, dest, ByteOrder.BIG_ENDIAN);
    }

    /**
   * Reads from the specified file starting at its current position as many values as remaining in the specified
   * buffer, decoding the bytes in the file according to the specified byte order.
   * <p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes readed.
   * </p>
   *
   * @param src    the file to read from.
   * @param dest   the buffer to read into.
   * @param order  the byte order of the values in the file.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws EOFException         if there are not enough remaining bytes in the file at its initial position.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static void readFully(final FileChannel src, final IntBuffer dest, final ByteOrder order) throws IOException {
        readFullyImpl(src, dest, order, JavaTypeId.INT, 2);
    }

    /**
   * Reads from the specified file starting at its current position as many values as remaining in the specified
   * buffer.
   * <p>
   * The bytes of the values are decoded in {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN} order.
   * </p><p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes readed.
   * </p>
   *
   * @param src    the file to read from.
   * @param dest   the buffer to read into.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws EOFException         if there are not enough remaining bytes in the file at its initial position.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static void readFully(final FileChannel src, final LongBuffer dest) throws IOException {
        readFully(src, dest, ByteOrder.BIG_ENDIAN);
    }

    /**
   * Reads from the specified file starting at its current position as many values as remaining in the specified
   * buffer, decoding the bytes in the file according to the specified byte order.
   * <p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes readed.
   * </p>
   *
   * @param src    the file to read from.
   * @param dest   the buffer to read into.
   * @param order  the byte order of the values in the file.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws EOFException         if there are not enough remaining bytes in the file at its initial position.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static void readFully(final FileChannel src, final LongBuffer dest, final ByteOrder order) throws IOException {
        readFullyImpl(src, dest, order, JavaTypeId.LONG, 3);
    }

    /**
   * Reads from the specified file starting at its current position as many values as remaining in the specified
   * buffer.
   * <p>
   * The bytes of the values are decoded in {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN} order.
   * </p><p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes readed.
   * </p>
   *
   * @param src    the file to read from.
   * @param dest   the buffer to read into.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws EOFException         if there are not enough remaining bytes in the file at its initial position.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static void readFully(final FileChannel src, final ShortBuffer dest) throws IOException {
        readFully(src, dest, ByteOrder.BIG_ENDIAN);
    }

    /**
   * Reads from the specified file starting at its current position as many values as remaining in the specified
   * buffer, decoding the bytes in the file according to the specified byte order.
   * <p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes readed.
   * </p>
   *
   * @param src    the file to read from.
   * @param dest   the buffer to read into.
   * @param order  the byte order of the values in the file.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws EOFException         if there are not enough remaining bytes in the file at its initial position.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static void readFully(final FileChannel src, final ShortBuffer dest, final ByteOrder order) throws IOException {
        readFullyImpl(src, dest, order, JavaTypeId.SHORT, 1);
    }

    private static void readFullyImpl(final FileChannel src, final Buffer dest, final ByteOrder order, final JavaTypeId type, final int shift) throws IOException {
        final WritableByteChannel destChannel = new FileChannels.BufferConverterChannel(dest, order, type);
        long pos = src.position();
        while (dest.hasRemaining()) {
            final long step = src.transferTo(pos, ((long) dest.remaining()) << shift, destChannel);
            if (step == 0) {
                final long size = src.size();
                if (pos + (((long) dest.remaining()) << shift) > size) {
                    if (src.position() < size) src.position(size);
                    throw new EOFException();
                }
            }
            assert (step >= 0);
            pos += step;
        }
        src.position(pos);
    }

    /**
   * Transfers bytes from specified stream to specified destination file until either the end of the source
   * stream has been reached or the specified maximum number of bytes have been transferred.
   * <p>
   * This call increments the position of the specified file by the number of bytes transferred.
   * </p>
   *
   * @return the number of bytes transferred.
   *
   * @param src       the stream to read the bytes from.
   * @param dest      the file to write the bytes to.
   * @param maxCount  the maximum number of bytes to transfer, or <tt>-1</tt> to transfer all bytes until
   *                  the end of the source stream has been reached.
   *
   * @throws IOException              if an I/O error occurs.
   * @throws IllegalArgumentException if <code>maxCount < -1</code>.
   * @throws NullPointerException     if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static long transferFrom(final InputStream src, final FileChannel dest, final long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) return 0;
        return transferFromByteChannel(IOChannels.asReadableByteChannel(src), dest, maxCount);
    }

    /**
   * Transfers from specified source stream exactly the specified number of bytes to the specified destination
   * file.
   * <p>
   * This call increments the position of the specified file by the number of bytes transferred.
   * </p>
   *
   * @param src       the stream to read the bytes from.
   * @param dest      the file to write the bytes to.
   * @param count     the number of bytes to transfer.
   *
   * @throws IOException              if an I/O error occurs.
   * @throws EOFException             if less than the specified count of bytes are remaining in the source
   *                                  stream.
   * @throws IllegalArgumentException if <code>count < 0</code>.
   * @throws NullPointerException     if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static void transferBytesFullyFrom(final DataInput src, final FileChannel dest, long count) throws IOException {
        CheckArg.count(count);
        if (count == 0) return;
        if (src == dest) {
            final long fp = dest.position();
            final long size = dest.size();
            dest.position(Math.min(fp + count, size));
            if (fp + count > size) throw new EOFException();
            return;
        }
        final ReadableByteChannel srcChannel;
        if (src instanceof ReadableByteChannel) srcChannel = (ReadableByteChannel) src; else if (src instanceof RandomAccessFile) srcChannel = ((RandomAccessFile) src).getChannel(); else if (src == null) throw new NullPointerException("source"); else {
            srcChannel = new ReadableByteChannel() {

                @Override
                public final void close() {
                }

                @Override
                public final boolean isOpen() {
                    return true;
                }

                @Override
                public final int read(final ByteBuffer dest) throws IOException {
                    final int count = dest.remaining();
                    if (dest.hasArray()) {
                        src.readFully(dest.array(), dest.arrayOffset() + dest.position(), count);
                        dest.position(dest.limit());
                    } else {
                        for (int r = count; --r >= 0; ) dest.put(src.readByte());
                    }
                    return count;
                }
            };
        }
        if (transferFromByteChannel(srcChannel, dest, count) != count) {
            throw new EOFException();
        }
    }

    /**
   * Transfers bytes from specified channel to specified destination file until either the source
   * channel has no more remaining bytes or the specified maximum number of bytes have been transferred.
   * <p>
   * This call increments the position of the specified file by the number of bytes transferred.
   * </p>
   *
   * @return the number of bytes transferred.
   *
   * @param src       the channel to read the bytes from.
   * @param dest      the file to write the bytes to.
   * @param maxCount  the maximum number of bytes to transfer, or <tt>-1</tt> to transfer all bytes until
   *                  the end of the source stream has been reached.
   *
   * @throws IOException              if an I/O error occurs.
   * @throws IllegalArgumentException if <code>maxCount < -1</code>.
   * @throws NullPointerException     if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static long transferFromByteChannel(final ReadableByteChannel src, final FileChannel dest, final long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) return 0;
        if (src == dest) {
            final long pos = dest.position();
            final long count = (maxCount < 0) ? (dest.size() - pos) : Math.min(dest.size() - pos, maxCount);
            dest.position(pos + count);
            return count;
        }
        long count = 0;
        long pos = dest.position();
        ByteBuffer testBuffer = null;
        while ((count < maxCount) || (maxCount < 0)) {
            final long step = dest.transferFrom(src, pos, (maxCount < 0) ? Integer.MAX_VALUE : (maxCount - count));
            count += step;
            if (step > 0) {
                pos += step;
                dest.position(pos);
            }
            if (step <= 0) {
                if (testBuffer == null) testBuffer = ByteBuffer.allocate((maxCount < 0) ? 32 : (int) Math.min(32, maxCount - count)); else testBuffer.clear();
                final int testRead = src.read(testBuffer);
                if (testRead < 0) break;
                if (testRead > 0) {
                    testBuffer.flip();
                    dest.write(testBuffer);
                    count += testRead;
                    pos += testRead;
                    dest.position(pos);
                }
            }
        }
        return count;
    }

    /**
   * Transfers bytes from specified file starting at its current position to specified destination stream until
   * either the end of the source file has been reached or the specified maximum number of bytes have been
   * transferred.
   * <p>
   * This call increments the position of the specified file by the number of bytes transferred.
   * </p>
   *
   * @return the number of bytes transferred.
   *
   * @param src       the file to read the bytes from.
   * @param dest      the stream to write the bytes to.
   * @param maxCount  the maximum number of bytes to transfer, or <tt>-1</tt> to transfer all bytes until
   *                  the end of the source file has been reached.
   *
   * @throws IOException              if an I/O error occurs.
   * @throws IllegalArgumentException if <code>maxCount < -1</code>.
   * @throws NullPointerException     if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static long transferTo(final FileChannel src, final OutputStream dest, final long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) return 0;
        return transferToByteChannel(src, IOChannels.asWritableByteChannel(dest), maxCount);
    }

    /**
   * Transfers bytes from specified file starting at its current position to specified destination stream until
   * either the end of the source file has been reached or the specified maximum number of bytes have been
   * transferred.
   * <p>
   * This call increments the position of the specified file by the number of bytes transferred.
   * </p>
   *
   * @return the number of bytes transferred.
   *
   * @param src       the file to read the bytes from.
   * @param dest      the stream to write the bytes to.
   * @param maxCount  the maximum number of bytes to transfer, or <tt>-1</tt> to transfer all bytes until
   *                  the end of the source file has been reached.
   *
   * @throws IOException              if an I/O error occurs.
   * @throws IllegalArgumentException if <code>maxCount < -1</code>.
   * @throws NullPointerException     if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static long transferBytesTo(final FileChannel src, final DataOutput dest, final long maxCount) throws IOException {
        CheckArg.count(maxCount);
        if (maxCount == 0) return 0;
        return transferToByteChannel(src, IOChannels.asWritableByteChannel(dest), maxCount);
    }

    /**
   * Transfers bytes from specified file starting at its current position to specified destination channel until
   * either the end of the source file has been reached or the specified maximum number of bytes have been
   * transferred.
   * <p>
   * This call increments the position of the specified file by the number of bytes transferred.
   * </p>
   *
   * @return the number of bytes transferred.
   *
   * @param src       the file to read the bytes from.
   * @param dest      the stream to write the bytes to.
   * @param maxCount  the maximum number of bytes to transfer, or <tt>-1</tt> to transfer all bytes until
   *                  the end of the source file has been reached.
   *
   * @throws IOException              if an I/O error occurs.
   * @throws IllegalArgumentException if <code>maxCount < -1</code>.
   * @throws NullPointerException     if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static long transferToByteChannel(final FileChannel src, final WritableByteChannel dest, long maxCount) throws IOException {
        CheckArg.maxCount(maxCount);
        if (maxCount == 0) return 0;
        long pos = src.position();
        maxCount = (maxCount < 0) ? (src.size() - pos) : Math.min(maxCount, src.size() - pos);
        if (maxCount <= 0) return 0;
        if (src == dest) {
            src.position(pos + maxCount);
            return maxCount;
        }
        long count = 0;
        while ((count < maxCount) || (maxCount < 0)) {
            final long step = src.transferTo(pos, maxCount - count, dest);
            count += step;
            if (step > 0) {
                pos += step;
                src.position(pos);
            }
            if ((step <= 0) && (pos == src.size())) break;
        }
        return count;
    }

    /**
   * Writes all remaining elements in the specified buffer to the specified file at the file's current position.
   * <p>
   * The values are written encoded in {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN} order.
   * </p><p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes written.
   * </p>
   *
   * @return the initial count of elements in the buffer.
   *
   * @param dest   the file to write to.
   * @param src    the buffer containing the values to write to the file.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(final FileChannel dest, final CharBuffer src) throws IOException {
        return write(dest, src, ByteOrder.BIG_ENDIAN);
    }

    /**
   * Writes all remaining elements in the specified buffer to the specified file at the file's current position,
   * encoding the values according to the specified byte order.
   * <p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes written.
   * </p>
   *
   * @return the initial count of elements in the buffer.
   *
   * @param dest   the file to write to.
   * @param src    the buffer containing the values to write to the file.
   * @param order  specifies how the values in the file will be encoded.
   *
   *
   * @throws IOException          if an I/O error occurs.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(final FileChannel dest, final CharBuffer src, final ByteOrder order) throws IOException {
        return writeImpl(dest, src, order, JavaTypeId.CHAR, 1);
    }

    /**
   * Writes all remaining elements in the specified buffer to the specified file at the file's current position.
   * <p>
   * The values are written encoded in {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN} order.
   * </p><p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes written.
   * </p>
   *
   * @return the initial count of elements in the buffer.
   *
   * @param dest   the file to write to.
   * @param src    the buffer containing the values to write to the file.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(final FileChannel dest, final DoubleBuffer src) throws IOException {
        return write(dest, src, ByteOrder.BIG_ENDIAN);
    }

    /**
   * Writes all remaining elements in the specified buffer to the specified file at the file's current position,
   * encoding the values according to the specified byte order.
   * <p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes written.
   * </p>
   *
   * @return the initial count of elements in the buffer.
   *
   * @param dest   the file to write to.
   * @param src    the buffer containing the values to write to the file.
   * @param order  specifies how the values in the file will be encoded.
   *
   *
   * @throws IOException          if an I/O error occurs.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(final FileChannel dest, final DoubleBuffer src, final ByteOrder order) throws IOException {
        return writeImpl(dest, src, order, JavaTypeId.DOUBLE, 3);
    }

    /**
   * Writes all remaining elements in the specified buffer to the specified file at the file's current position.
   * <p>
   * The values are written encoded in {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN} order.
   * </p><p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes written.
   * </p>
   *
   * @return the initial count of elements in the buffer.
   *
   * @param dest   the file to write to.
   * @param src    the buffer containing the values to write to the file.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(final FileChannel dest, final FloatBuffer src) throws IOException {
        return write(dest, src, ByteOrder.BIG_ENDIAN);
    }

    /**
   * Writes all remaining elements in the specified buffer to the specified file at the file's current position,
   * encoding the values according to the specified byte order.
   * <p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes written.
   * </p>
   *
   * @return the initial count of elements in the buffer.
   *
   * @param dest   the file to write to.
   * @param src    the buffer containing the values to write to the file.
   * @param order  specifies how the values in the file will be encoded.
   *
   *
   * @throws IOException          if an I/O error occurs.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(final FileChannel dest, final FloatBuffer src, final ByteOrder order) throws IOException {
        return writeImpl(dest, src, order, JavaTypeId.FLOAT, 2);
    }

    /**
   * Writes all remaining elements in the specified buffer to the specified file at the file's current position.
   * <p>
   * The values are written encoded in {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN} order.
   * </p><p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes written.
   * </p>
   *
   * @return the initial count of elements in the buffer.
   *
   * @param dest   the file to write to.
   * @param src    the buffer containing the values to write to the file.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(final FileChannel dest, final IntBuffer src) throws IOException {
        return write(dest, src, ByteOrder.BIG_ENDIAN);
    }

    /**
   * Writes all remaining elements in the specified buffer to the specified file at the file's current position,
   * encoding the values according to the specified byte order.
   * <p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes written.
   * </p>
   *
   * @return the initial count of elements in the buffer.
   *
   * @param dest   the file to write to.
   * @param src    the buffer containing the values to write to the file.
   * @param order  specifies how the values in the file will be encoded.
   *
   *
   * @throws IOException          if an I/O error occurs.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(final FileChannel dest, final IntBuffer src, final ByteOrder order) throws IOException {
        return writeImpl(dest, src, order, JavaTypeId.INT, 2);
    }

    /**
   * Writes all remaining elements in the specified buffer to the specified file at the file's current position.
   * <p>
   * The values are written encoded in {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN} order.
   * </p><p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes written.
   * </p>
   *
   * @return the initial count of elements in the buffer.
   *
   * @param dest   the file to write to.
   * @param src    the buffer containing the values to write to the file.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(final FileChannel dest, final LongBuffer src) throws IOException {
        return write(dest, src, ByteOrder.BIG_ENDIAN);
    }

    /**
   * Writes all remaining elements in the specified buffer to the specified file at the file's current position,
   * encoding the values according to the specified byte order.
   * <p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes written.
   * </p>
   *
   * @return the initial count of elements in the buffer.
   *
   * @param dest   the file to write to.
   * @param src    the buffer containing the values to write to the file.
   * @param order  specifies how the values in the file will be encoded.
   *
   *
   * @throws IOException          if an I/O error occurs.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(final FileChannel dest, final LongBuffer src, final ByteOrder order) throws IOException {
        return writeImpl(dest, src, order, JavaTypeId.LONG, 3);
    }

    /**
   * Writes all remaining elements in the specified buffer to the specified file at the file's current position.
   * <p>
   * The values are written encoded in {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN} order.
   * </p><p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes written.
   * </p>
   *
   * @return the initial count of elements in the buffer.
   *
   * @param dest   the file to write to.
   * @param src    the buffer containing the values to write to the file.
   *
   * @throws IOException          if an I/O error occurs.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(final FileChannel dest, final ShortBuffer src) throws IOException {
        return write(dest, src, ByteOrder.BIG_ENDIAN);
    }

    /**
   * Writes all remaining elements in the specified buffer to the specified file at the file's current position,
   * encoding the values according to the specified byte order.
   * <p>
   * If this call returns normally then the specified buffer has no more remaining elements.
   * The position of the file gets incremented by the count of bytes written.
   * </p>
   *
   * @return the initial count of elements in the buffer.
   *
   * @param dest   the file to write to.
   * @param src    the buffer containing the values to write to the file.
   * @param order  specifies how the values in the file will be encoded.
   *
   *
   * @throws IOException          if an I/O error occurs.
   * @throws NullPointerException if any argument is <tt>null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(final FileChannel dest, final ShortBuffer src, final ByteOrder order) throws IOException {
        return writeImpl(dest, src, order, JavaTypeId.SHORT, 1);
    }

    private static int writeImpl(final FileChannel dest, final Buffer src, final ByteOrder order, final JavaTypeId type, final int shift) throws IOException {
        final int count = src.remaining();
        if (count == 0) return 0;
        final ReadableByteChannel srcChannel = new FileChannels.BufferConverterChannel(src, order, type);
        long pos = dest.position();
        while (src.hasRemaining()) pos += dest.transferFrom(srcChannel, pos, ((long) src.remaining()) << shift);
        dest.position(pos);
        return count;
    }

    /**
   * Bogus implementation of a byte channel reading/writing from/to a buffer of another type.
   * Bogus, but works for the purposes of this class.
   * <p>
   * Precondition: nobody tries to write more bytes to the channel than fitting into the buffer.
   * </p><p>
   * Precondition: nobody tries to read more bytes from the channel than remaining in the buffer.
   * </p><p>
   * Precondition: either reading or writing but not both on the same instance.
   * </p>
   */
    private static final class BufferConverterChannel extends Object implements ReadableByteChannel, WritableByteChannel {

        private boolean closed;

        private final Buffer buffer;

        private final ByteOrder order;

        private final JavaTypeId type;

        BufferConverterChannel(final Buffer buffer, final ByteOrder order, final JavaTypeId type) {
            super();
            if (order == null) throw new NullPointerException("order");
            this.buffer = buffer;
            this.order = order;
            this.type = type;
        }

        @Override
        public final void close() {
            this.closed = true;
        }

        @Override
        public final boolean isOpen() {
            return !this.closed;
        }

        @Override
        public final int read(final ByteBuffer dest) {
            final int pos = this.buffer.position();
            final int lim = this.buffer.limit();
            if (pos == lim) return -1;
            final int countBytes;
            switch(this.type) {
                case CHAR:
                    {
                        final CharBuffer b = dest.order(this.order).asCharBuffer();
                        final int count = b.remaining();
                        this.buffer.limit(pos + count);
                        b.put((CharBuffer) this.buffer);
                        countBytes = count << 1;
                    }
                    break;
                case DOUBLE:
                    {
                        final DoubleBuffer b = dest.order(this.order).asDoubleBuffer();
                        final int count = b.remaining();
                        this.buffer.limit(pos + count);
                        b.put((DoubleBuffer) this.buffer);
                        countBytes = count << 3;
                    }
                    break;
                case FLOAT:
                    {
                        final FloatBuffer b = dest.order(this.order).asFloatBuffer();
                        final int count = b.remaining();
                        this.buffer.limit(pos + count);
                        b.put((FloatBuffer) this.buffer);
                        countBytes = count << 2;
                    }
                    break;
                case INT:
                    {
                        final IntBuffer b = dest.order(this.order).asIntBuffer();
                        final int count = b.remaining();
                        this.buffer.limit(pos + count);
                        b.put((IntBuffer) this.buffer);
                        countBytes = count << 2;
                    }
                    break;
                case LONG:
                    {
                        final LongBuffer b = dest.order(this.order).asLongBuffer();
                        final int count = b.remaining();
                        this.buffer.limit(pos + count);
                        b.put((LongBuffer) this.buffer);
                        countBytes = count << 3;
                    }
                    break;
                case SHORT:
                    {
                        final ShortBuffer b = dest.order(this.order).asShortBuffer();
                        final int count = b.remaining();
                        this.buffer.limit(pos + count);
                        b.put((ShortBuffer) this.buffer);
                        countBytes = count << 1;
                    }
                    break;
                default:
                    throw new AssertionError(this.type);
            }
            dest.position(dest.position() + countBytes);
            this.buffer.limit(lim);
            return countBytes;
        }

        @Override
        public final int write(final ByteBuffer src) {
            final int countBytes;
            switch(this.type) {
                case CHAR:
                    {
                        final CharBuffer b = src.order(this.order).asCharBuffer();
                        countBytes = b.remaining() << 1;
                        ((CharBuffer) this.buffer).put(b);
                    }
                    break;
                case DOUBLE:
                    {
                        final DoubleBuffer b = src.order(this.order).asDoubleBuffer();
                        countBytes = b.remaining() << 3;
                        ((DoubleBuffer) this.buffer).put(b);
                    }
                    break;
                case FLOAT:
                    {
                        final FloatBuffer b = src.order(this.order).asFloatBuffer();
                        countBytes = b.remaining() << 2;
                        ((FloatBuffer) this.buffer).put(b);
                    }
                    break;
                case INT:
                    {
                        final IntBuffer b = src.order(this.order).asIntBuffer();
                        countBytes = b.remaining() << 2;
                        ((IntBuffer) this.buffer).put(b);
                    }
                    break;
                case LONG:
                    {
                        final LongBuffer b = src.order(this.order).asLongBuffer();
                        countBytes = b.remaining() << 3;
                        ((LongBuffer) this.buffer).put(b);
                    }
                    break;
                case SHORT:
                    {
                        final ShortBuffer b = src.order(this.order).asShortBuffer();
                        countBytes = b.remaining() << 1;
                        ((ShortBuffer) this.buffer).put(b);
                    }
                    break;
                default:
                    throw new AssertionError(this.type);
            }
            src.position(src.position() + countBytes);
            return countBytes;
        }
    }
}

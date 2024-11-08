package jaxlib.io.stream;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import jaxlib.io.channel.FileChannels;

/**
 * Provides utilities to work with instances of {@link RandomAccessFile}.
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: RandomAccessFiles.java 3018 2011-11-30 04:45:29Z joerg_wassmer $
 */
public class RandomAccessFiles extends Object {

    protected RandomAccessFiles() throws InstantiationException {
        throw new InstantiationException();
    }

    /**
   * Removes from specified file specified count of bytes beginning at the specified position.
   * After this call the length of the file is its initial length decremented by the specified count
   * and the file's position is equal to the specified index.
   *
   * @param f         the file to remove bytes from.
   * @param fromIndex the index of the first byte to remove.
   * @param count     the number of bytes to remove.
   *
   * @throws IOException               if an I/O error occurs.
   * @throws IllegalArgumentException  if <tt>count < 0</tt>.
   * @throws IndexOutOfBoundsException if <code>(fromIndex < 0) || (fromIndex + count > f.length()</code>.
   * @throws NullPointerException      if <tt>f == null</tt>.
   *
   * @since JaXLib 1.0
   */
    public static void clear(RandomAccessFile f, long fromIndex, long count) throws IOException {
        FileChannels.clear(f.getChannel(), fromIndex, count);
    }

    /**
   * Copies specified region of the specified file to specified destination index in the same file.
   * The behaviour of this method is similar to the {@link System#arraycopy(Object,int,Object,int,int) System.arraycopy} method.
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
    public static void copy(RandomAccessFile f, long sourceIndex, long destIndex, long length) throws IOException {
        FileChannels.copy(f.getChannel(), sourceIndex, destIndex, length);
    }

    /**
   * Copies specified region of the specified source file to specified destination file at specified index.
   * The behaviour of this method is similar to the {@link System#arraycopy(Object,int,Object,int,int) System.arraycopy} method.
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
    public static void copy(RandomAccessFile source, long sourceIndex, RandomAccessFile dest, long destIndex, long length) throws IOException {
        FileChannels.copy(source.getChannel(), sourceIndex, dest.getChannel(), destIndex, length);
    }

    /**
   * Fills specified file beginning at its current position with specified count of specified byte.
   * In other words, replaces specified count of bytes in the file by the specified byte.
   * The file's position may be bigger than its current length, in which case the values of the bytes between
   * the files initial length and initial position are undefined.
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
    public static void fill(RandomAccessFile f, byte e, long count) throws IOException {
        FileChannels.fill(f.getChannel(), e, count);
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
   * @see #copy(RandomAccessFile,long,RandomAccessFile,long,long)
   * @see #clear(RandomAccessFile,long,long)
   *
   * @since JaXLib 1.0
   */
    public static void move(RandomAccessFile f, long sourceIndex, long destIndex, long count) throws IOException {
        FileChannels.move(f.getChannel(), sourceIndex, destIndex, count);
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
   * @see #copy(RandomAccessFile,long,RandomAccessFile,long,long)
   * @see #clear(RandomAccessFile,long,long)
   *
   * @since JaXLib 1.0
   */
    public static void move(RandomAccessFile source, long sourceIndex, RandomAccessFile dest, long destIndex, long count) throws IOException {
        FileChannels.move(source.getChannel(), sourceIndex, dest.getChannel(), destIndex, count);
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
    public static void readFully(RandomAccessFile src, ByteBuffer dest) throws IOException {
        FileChannels.readFully(src.getChannel(), dest);
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
    public static void readFully(RandomAccessFile src, CharBuffer dest) throws IOException {
        FileChannels.readFully(src.getChannel(), dest);
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
    public static void readFully(RandomAccessFile src, CharBuffer dest, ByteOrder order) throws IOException {
        FileChannels.readFully(src.getChannel(), dest, order);
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
    public static void readFully(RandomAccessFile src, DoubleBuffer dest) throws IOException {
        FileChannels.readFully(src.getChannel(), dest);
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
    public static void readFully(RandomAccessFile src, DoubleBuffer dest, ByteOrder order) throws IOException {
        FileChannels.readFully(src.getChannel(), dest, order);
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
    public static void readFully(RandomAccessFile src, FloatBuffer dest) throws IOException {
        FileChannels.readFully(src.getChannel(), dest);
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
    public static void readFully(RandomAccessFile src, FloatBuffer dest, ByteOrder order) throws IOException {
        FileChannels.readFully(src.getChannel(), dest, order);
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
    public static void readFully(RandomAccessFile src, IntBuffer dest) throws IOException {
        FileChannels.readFully(src.getChannel(), dest);
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
    public static void readFully(RandomAccessFile src, IntBuffer dest, ByteOrder order) throws IOException {
        FileChannels.readFully(src.getChannel(), dest, order);
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
    public static void readFully(RandomAccessFile src, LongBuffer dest) throws IOException {
        FileChannels.readFully(src.getChannel(), dest);
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
    public static void readFully(RandomAccessFile src, LongBuffer dest, ByteOrder order) throws IOException {
        FileChannels.readFully(src.getChannel(), dest, order);
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
    public static void readFully(RandomAccessFile src, ShortBuffer dest) throws IOException {
        FileChannels.readFully(src.getChannel(), dest);
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
    public static void readFully(RandomAccessFile src, ShortBuffer dest, ByteOrder order) throws IOException {
        FileChannels.readFully(src.getChannel(), dest, order);
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
    public static long transferFrom(InputStream src, RandomAccessFile dest, long maxCount) throws IOException {
        return FileChannels.transferFrom(src, dest.getChannel(), maxCount);
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
    public static long transferFromByteChannel(ReadableByteChannel src, RandomAccessFile dest, long maxCount) throws IOException {
        return FileChannels.transferFromByteChannel(src, dest.getChannel(), maxCount);
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
    public static void transferBytesFullyFrom(final DataInput src, RandomAccessFile dest, long count) throws IOException {
        FileChannels.transferBytesFullyFrom(src, dest.getChannel(), count);
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
    public static long transferTo(RandomAccessFile src, OutputStream dest, long maxCount) throws IOException {
        return FileChannels.transferTo(src.getChannel(), dest, maxCount);
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
    public static long transferBytesTo(RandomAccessFile src, DataOutput dest, long maxCount) throws IOException {
        return FileChannels.transferBytesTo(src.getChannel(), dest, maxCount);
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
    public static long transferToByteChannel(final RandomAccessFile src, final WritableByteChannel dest, final long maxCount) throws IOException {
        return FileChannels.transferToByteChannel(src.getChannel(), dest, maxCount);
    }

    /**
   * Writes all remaining elements in the specified buffer to the specified file at the file's current position.
   * <p>
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
    public static int write(final RandomAccessFile dest, final ByteBuffer src) throws IOException {
        return FileChannels.write(dest.getChannel(), src);
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
    public static int write(final RandomAccessFile dest, final CharBuffer src) throws IOException {
        return FileChannels.write(dest.getChannel(), src);
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
    public static int write(RandomAccessFile dest, CharBuffer src, ByteOrder order) throws IOException {
        return FileChannels.write(dest.getChannel(), src, order);
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
    public static int write(RandomAccessFile dest, DoubleBuffer src) throws IOException {
        return FileChannels.write(dest.getChannel(), src);
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
    public static int write(RandomAccessFile dest, DoubleBuffer src, ByteOrder order) throws IOException {
        return FileChannels.write(dest.getChannel(), src, order);
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
    public static int write(RandomAccessFile dest, FloatBuffer src) throws IOException {
        return FileChannels.write(dest.getChannel(), src);
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
    public static int write(RandomAccessFile dest, FloatBuffer src, ByteOrder order) throws IOException {
        return FileChannels.write(dest.getChannel(), src, order);
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
    public static int write(RandomAccessFile dest, IntBuffer src) throws IOException {
        return FileChannels.write(dest.getChannel(), src);
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
    public static int write(RandomAccessFile dest, IntBuffer src, ByteOrder order) throws IOException {
        return FileChannels.write(dest.getChannel(), src, order);
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
    public static int write(RandomAccessFile dest, LongBuffer src) throws IOException {
        return FileChannels.write(dest.getChannel(), src);
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
    public static int write(RandomAccessFile dest, LongBuffer src, ByteOrder order) throws IOException {
        return FileChannels.write(dest.getChannel(), src, order);
    }

    /**
   * Writes all remaining elements in the specified buffer to the specified file at the file's current position.
   * <p>
   * The bytes of the values are encoded in {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN} order.
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
    public static int write(RandomAccessFile dest, ShortBuffer src) throws IOException {
        return FileChannels.write(dest.getChannel(), src);
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
    public static int write(RandomAccessFile dest, ShortBuffer src, ByteOrder order) throws IOException {
        return FileChannels.write(dest.getChannel(), src, order);
    }
}

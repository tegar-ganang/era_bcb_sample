package jaxlib.jdbc.tds;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.util.Arrays;
import jaxlib.io.IO;
import jaxlib.io.stream.IOStreams;
import jaxlib.io.stream.embedded.FixedLengthInputStream;
import jaxlib.lang.Bytes;

/**
 * Manages a buffer (backed by optional disk storage) for use as a data store
 * by the CLOB and BLOB objects.
 * <p/>
 * The data can be purely memory based until the size exceeds the value
 * dictated by the <code>lobBuffer</code> URL property after which it will be
 * written to disk. The disk array is accessed randomly one page (1024 bytes)
 * at a time.
 * <p/>
 * This class is not synchronized and concurrent open input and output
 * streams can conflict.
 * <p/>
 * Tuning hints:
 * <ol>
 *   <li>The <code>PAGE_SIZE</code> governs how much data is buffered when
 *     reading or writing data a byte at a time. 1024 bytes seems to work well
 *     but if very large objects are being written a byte at a time 4096 may be
 *     better. <b>NB.</b> ensure that the <code>PAGE_MASK</code> and
 *     <code>BYTE_MASK</code> fields are also adjusted to match.
 *   <li>Reading or writing byte arrays that are greater than or equal to the
 *     page size will go directly to or from the random access file cutting out
 *     an ArrayCopy operation.
 *   <li>If BLOBs are being buffered exclusively in memory you may wish to
 *     adjust the <code>MAX_BUF_INC</code> value. Every time the buffer is
 *     expanded the existing contents are copied and this may get expensive
 *     with very large BLOBs.
 *   <li>The BLOB file will be kept open for as long as there are open input or
 *     output streams. Therefore BLOB streams should be explicitly closed as
 *     soon as they are finished with.
 * </ol>
 *
 * @author  Mike Hutchinson
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: BlobBuffer.java 3041 2012-01-25 07:59:46Z joerg_wassmer $
 */
final class BlobBuffer extends Object {

    /**
   * Default page size (must be power of 2).
   */
    private static final int PAGE_SIZE = 1024;

    /**
   * Mask for page component of read/write pointer.
   */
    private static final int PAGE_MASK = 0xFFFFFC00;

    /**
   * Mask for page offset component of R/W pointer.
   */
    private static final int BYTE_MASK = 0x000003FF;

    /**
   * Maximum buffer increment.
   */
    private static final int MAX_BUF_INC = 16384;

    /**
   * Invalid page marker.
   */
    private static final int INVALID_PAGE = -1;

    /**
   * The BLOB buffer or the current page buffer.
   */
    private byte[] buffer;

    /**
   * The total length of the valid data in buffer.
   */
    private int length;

    /**
   * The number of the current page in memory.
   */
    private int currentPage;

    /**
   * Indicates page in memory must be saved.
   */
    private boolean bufferDirty;

    /**
   * Count of callers that have opened the BLOB file.
   */
    private int openCount;

    /**
   * True if attempts to create a BLOB file have failed.
   */
    private boolean isMemOnly;

    /**
   * The maximum size of an in memory buffer.
   */
    private final int maxMemSize;

    private final BlobBuffer.FileHolder fileHolder;

    /**
   * Creates a blob buffer.
   *
   * @param maxMemSize the maximum size of the in memory buffer
   */
    BlobBuffer(final TdsConnection connection) throws SQLException {
        super();
        this.fileHolder = new BlobBuffer.FileHolder(connection.getCloser());
        this.maxMemSize = (int) connection.lobBuffer;
        this.buffer = Bytes.EMPTY_ARRAY;
    }

    /**
   * Creates a random access disk file to use as backing storage for the LOB
   * data.
   * <p/>
   * This method may fail due to security exceptions or local disk problems,
   * in which case the blob storage will remain entirely in memory.
   */
    private void createBlobFileIfNecessary() throws SQLException {
        synchronized (this.fileHolder) {
            ensureOpen();
            if (!this.isMemOnly && (this.fileHolder.file == null)) {
                try {
                    this.fileHolder.closer.purge();
                    if (this.fileHolder.path == null) {
                        this.fileHolder.path = File.createTempFile("tdsblob", ".tmp");
                        this.fileHolder.closer.info.incLobFilesCreated();
                    }
                    this.fileHolder.file = new RandomAccessFile(this.fileHolder.path, "rw");
                    if (this.length > 0) this.fileHolder.file.write(this.buffer, 0, this.length);
                    this.buffer = new byte[PAGE_SIZE];
                    this.currentPage = INVALID_PAGE;
                    this.openCount = 0;
                } catch (final SecurityException ex) {
                    this.fileHolder.tryClose();
                    this.isMemOnly = true;
                } catch (final IOException ex) {
                    this.fileHolder.tryClose();
                    this.isMemOnly = true;
                }
                if (this.fileHolder.close == null) this.fileHolder.close = this.fileHolder.closer.add(this, this.fileHolder);
            }
        }
    }

    private void ensureOpen() throws SQLException {
        if (this.fileHolder.closed) throw new SQLNonTransientException(Messages.get("error.generic.closed", "Blob"), "57014");
    }

    final void close() throws IOException {
        this.fileHolder.closed = true;
        this.buffer = Bytes.EMPTY_ARRAY;
        this.fileHolder.close();
    }

    /**
   * Logically closes the file or physically closeOne it if the open count is
   * now zero.
   * <p/>
   * Any updated buffer in memory is flushed to disk before the file is
   * closed.
   *
   * @throws IOException if an I/O error occurs
   */
    final void closeOne() throws IOException {
        synchronized (this.fileHolder) {
            if ((this.openCount > 0) && (--this.openCount == 0) && (this.fileHolder.file != null) && !this.fileHolder.closed) {
                if (this.bufferDirty) writePage(this.currentPage);
                this.fileHolder.closeFile();
                this.buffer = Bytes.EMPTY_ARRAY;
                this.currentPage = INVALID_PAGE;
            }
        }
    }

    final boolean isOpen() {
        return !this.fileHolder.closed;
    }

    /**
   * Opens the BLOB disk file.
   * <p/>
   * A count of open and closeOne requests is kept so that the file may be
   * closed when no longer required thus keeping the number of open files to
   * a minimum.
   *
   * @throws IOException if an I/O error occurs
   */
    final void open() throws IOException {
        synchronized (this.fileHolder) {
            this.fileHolder.ensureOpen();
            if ((this.fileHolder.file == null) && (this.fileHolder.path != null)) {
                this.fileHolder.file = new RandomAccessFile(this.fileHolder.path, "rw");
                this.openCount = 1;
                this.currentPage = INVALID_PAGE;
                this.buffer = new byte[PAGE_SIZE];
            } else if (this.fileHolder.file != null) this.openCount++;
        }
    }

    /**
   * Reads byte from the BLOB buffer at the specified location.
   * <p/>
   * The read pointer is partitioned into a page number and an offset within
   * the page. This routine will read new pages as required. The page size
   * must be a power of 2 and is currently set to 1024 bytes.
   *
   * @param readPtr the offset in the buffer of the required byte
   * @return the byte value as an <code>int</code> or -1 if at EOF
   * @throws IOException if an I/O error occurs
   */
    final int read(final int readPtr) throws IOException {
        this.fileHolder.ensureOpen();
        if (readPtr >= this.length) return -1;
        if (this.fileHolder.file != null) {
            if (this.currentPage != (readPtr & PAGE_MASK)) readPage(readPtr);
            return this.buffer[readPtr & BYTE_MASK] & 0xFF;
        } else {
            return this.buffer[readPtr] & 0xFF;
        }
    }

    /**
   * Reads bytes from the BLOB buffer at the specified location.
   *
   * @param readPtr the offset in the buffer of the required byte
   * @param bytes   the byte array to fill
   * @param offset  the start position in the byte array
   * @param len     the number of bytes to read
   * @return the number of bytes read or -1 if at end of file
   * @throws IOException if an I/O error occurs
   */
    final int read(int readPtr, final byte[] bytes, int offset, int len) throws IOException {
        this.fileHolder.ensureOpen();
        if (bytes == null) throw new NullPointerException(); else if ((offset < 0) || (offset > bytes.length) || (len < 0) || ((offset + len) > bytes.length) || ((offset + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) return 0; else if (readPtr >= length) return -1; else {
            len = Math.min(this.length - readPtr, len);
            if (this.fileHolder.file == null) System.arraycopy(this.buffer, readPtr, bytes, offset, len); else if (len >= PAGE_SIZE) {
                if (this.bufferDirty) writePage(this.currentPage);
                this.currentPage = INVALID_PAGE;
                this.fileHolder.file.seek(readPtr);
                this.fileHolder.file.readFully(bytes, offset, len);
            } else {
                int count = len;
                while (count > 0) {
                    if (this.currentPage != (readPtr & PAGE_MASK)) readPage(readPtr);
                    final int inBuffer = Math.min(PAGE_SIZE - (readPtr & BYTE_MASK), count);
                    System.arraycopy(this.buffer, readPtr & BYTE_MASK, bytes, offset, inBuffer);
                    offset += inBuffer;
                    readPtr += inBuffer;
                    count -= inBuffer;
                }
            }
            return len;
        }
    }

    /**
   * Inserts a byte into the buffer at the specified location.
   * <p/>
   * The write pointer is partitioned into a page number and an offset within
   * the page. This routine will write new pages as required. The page size
   * must be a power of 2 and is currently set to 1024 bytes.
   *
   * @param writePtr the offset in the buffer of the required byte
   * @param b        the byte value to write
   * @throws IOException if an I/O error occurs
   */
    final void write(final int writePtr, final int b) throws IOException {
        this.fileHolder.ensureOpen();
        if (writePtr >= this.length) {
            if (writePtr > this.length) {
                throw new IOException("BLOB buffer has been truncated");
            }
            if (++this.length < 0) {
                throw new IOException("BLOB may not exceed 2GB in size");
            }
        }
        if (this.fileHolder.file != null) {
            if (this.currentPage != (writePtr & PAGE_MASK)) readPage(writePtr);
            this.buffer[writePtr & BYTE_MASK] = (byte) b;
            this.bufferDirty = true;
        } else {
            if (writePtr >= this.buffer.length) growBuffer(writePtr + 1);
            this.buffer[writePtr] = (byte) b;
        }
    }

    /**
   * Inserts bytes into the buffer at the specified location.
   *
   * @param writePtr the offset in the buffer of the required byte
   * @param bytes    the byte array value to write
   * @param offset   the start position in the byte array
   * @param len      the number of bytes to write
   * @throws IOException if an I/O error occurs
   */
    final void write(int writePtr, final byte[] bytes, int offset, final int len) throws IOException {
        this.fileHolder.ensureOpen();
        if (bytes == null) throw new NullPointerException(); else if ((offset < 0) || (offset > bytes.length) || (len < 0) || ((offset + len) > bytes.length) || ((offset + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        if ((long) writePtr + len > (long) Integer.MAX_VALUE) throw new IOException("BLOB may not exceed 2GB in size");
        if (writePtr > this.length) {
            throw new IOException("BLOB buffer has been truncated");
        }
        if (this.fileHolder.file != null) {
            if (len >= PAGE_SIZE) {
                if (bufferDirty) writePage(this.currentPage);
                this.currentPage = INVALID_PAGE;
                this.fileHolder.file.seek(writePtr);
                this.fileHolder.file.write(bytes, offset, len);
                writePtr += len;
            } else {
                int count = len;
                while (count > 0) {
                    if (this.currentPage != (writePtr & PAGE_MASK)) readPage(writePtr);
                    final int inBuffer = Math.min(PAGE_SIZE - (writePtr & BYTE_MASK), count);
                    System.arraycopy(bytes, offset, this.buffer, writePtr & BYTE_MASK, inBuffer);
                    this.bufferDirty = true;
                    offset += inBuffer;
                    writePtr += inBuffer;
                    count -= inBuffer;
                }
            }
        } else {
            if (writePtr + len > this.buffer.length) growBuffer(writePtr + len);
            System.arraycopy(bytes, offset, this.buffer, writePtr, len);
            writePtr += len;
        }
        if (writePtr > this.length) this.length = writePtr;
    }

    final void write(int writePtr, final ByteBuffer src) throws IOException {
        this.fileHolder.ensureOpen();
        if (src == null) throw new NullPointerException();
        final int remaining = src.remaining();
        if (remaining == 0) return;
        if ((long) writePtr + remaining > Integer.MAX_VALUE) throw new IOException("BLOB may not exceed 2GB in size");
        if (writePtr > this.length) {
            throw new IOException("BLOB buffer has been truncated");
        }
        if (this.fileHolder.file != null) {
            if (remaining >= PAGE_SIZE) {
                if (this.bufferDirty) writePage(this.currentPage);
                this.currentPage = INVALID_PAGE;
                this.fileHolder.file.seek(writePtr);
                while (src.hasRemaining()) this.fileHolder.file.getChannel().write(src);
                writePtr += remaining;
            } else {
                while (src.hasRemaining()) {
                    if (this.currentPage != (writePtr & PAGE_MASK)) readPage(writePtr);
                    final int inBuffer = Math.min(PAGE_SIZE - (writePtr & BYTE_MASK), src.remaining());
                    src.get(this.buffer, writePtr & BYTE_MASK, inBuffer);
                    this.bufferDirty = true;
                    writePtr += inBuffer;
                }
            }
        } else {
            if (writePtr + remaining > this.buffer.length) growBuffer(writePtr + remaining);
            src.get(this.buffer, writePtr, remaining);
            writePtr += remaining;
        }
        if (writePtr > this.length) this.length = writePtr;
    }

    /**
   * Reads in the specified page from the disk buffer.
   * <p/>
   * Any existing dirty page is first saved to disk.
   *
   * @param page the page number
   * @throws IOException if an I/O error occurs
   */
    final void readPage(int page) throws IOException {
        this.fileHolder.ensureOpen();
        page = page & PAGE_MASK;
        if (bufferDirty) writePage(currentPage);
        if (page > this.fileHolder.file.length()) throw new IOException("readPage: Invalid page number " + page);
        this.currentPage = page;
        this.fileHolder.file.seek(currentPage);
        int count = 0, res;
        do {
            res = this.fileHolder.file.read(buffer, count, buffer.length - count);
            count += (res == -1) ? 0 : res;
        } while (count < PAGE_SIZE && res != -1);
    }

    /**
   * Writes the specified page to the disk buffer.
   *
   * @param page the page number
   * @throws IOException if an I/O error occurs
   */
    final void writePage(int page) throws IOException {
        this.fileHolder.ensureOpen();
        page &= PAGE_MASK;
        if (page > this.fileHolder.file.length()) throw new IOException("writePage: Invalid page number " + page);
        if (buffer.length != PAGE_SIZE) throw new IllegalStateException("writePage: buffer size invalid");
        this.fileHolder.file.seek(page);
        this.fileHolder.file.write(buffer);
        this.bufferDirty = false;
    }

    /**
   * Increases the size of the in memory buffer for situations where disk
   * storage of BLOB is not possible.
   *
   * @param minSize the minimum size of buffer required
   */
    final void growBuffer(final int minSize) throws IOException {
        this.fileHolder.ensureOpen();
        if (this.buffer.length == 0) this.buffer = new byte[Math.max(PAGE_SIZE, minSize)]; else {
            final byte[] tmp;
            if ((this.buffer.length * 2 > minSize) && (this.buffer.length <= MAX_BUF_INC)) tmp = new byte[this.buffer.length * 2]; else tmp = new byte[minSize + MAX_BUF_INC];
            System.arraycopy(this.buffer, 0, tmp, 0, this.buffer.length);
            this.buffer = tmp;
        }
    }

    /**
   * Sets the initial buffer to an existing byte array.
   *
   * @param bytes the byte array containing the BLOB data
   * @param copy  true if a local copy of the data is required
   */
    final void setBuffer(final byte[] bytes, final boolean copy) throws SQLException {
        ensureOpen();
        this.buffer = (bytes.length == 0) ? Bytes.EMPTY_ARRAY : copy ? bytes.clone() : bytes;
        this.length = bytes.length;
    }

    /**
   * Returns the BLOB data as a byte array.
   *
   * @param pos the start position in the BLOB buffer (from 1)
   * @param len the number of bytes to copy
   * @return the requested data as a <code>byte[]</code>
   */
    final byte[] getBytes(long pos, int len) throws SQLException {
        ensureOpen();
        pos--;
        if (pos < 0) throw new SQLException(Messages.get("error.blobclob.badpos"), "HY090");
        if (pos > this.length) throw new SQLException(Messages.get("error.blobclob.badposlen"), "HY090");
        if (len < 0) throw new SQLException(Messages.get("error.blobclob.badlen"), "HY090");
        if (pos + len > this.length) len = (int) (this.length - pos);
        try {
            if (BlobBuffer.this.fileHolder.path == null) {
                final byte[] data = new byte[len];
                System.arraycopy(buffer, (int) (pos), data, 0, len);
                return data;
            } else {
                BlobInputStream is = new BlobInputStream(this, pos);
                try {
                    final byte[] data = new byte[len];
                    IOStreams.readFully(is, data);
                    is.close();
                    is = null;
                    return data;
                } finally {
                    IO.tryClose(is);
                }
            }
        } catch (final IOException e) {
            throw new SQLException(Messages.get("error.generic.ioerror", e.getMessage()), "HY000", e);
        }
    }

    final InputStream getAsciiStream() throws SQLException {
        ensureOpen();
        try {
            return new AsciiInputStream(this, 0);
        } catch (final IOException e) {
            throw new SQLException(Messages.get("error.generic.ioerror", e.getMessage()), "HY000", e);
        }
    }

    /**
   * Retrieve the BLOB data as an <code>InputStream</code>.
   *
   * @param ascii true if an ASCII input stream should be returned
   * @return the <code>InputStream</code> built over the BLOB data
   * @throws SQLException if an error occurs
   */
    final InputStream getBinaryStream() throws SQLException {
        ensureOpen();
        try {
            return new BlobInputStream(this, 0);
        } catch (final IOException e) {
            throw new SQLException(Messages.get("error.generic.ioerror", e.getMessage()), "HY000", e);
        }
    }

    final InputStream getBinaryStream(long pos, final long length) throws SQLException {
        ensureOpen();
        if (pos < 1) throw new SQLDataException(Messages.get("error.blobclob.badoffset"), "22003");
        --pos;
        final long totalLength = getLength();
        if ((length < 0) || (pos + length > totalLength)) throw new SQLDataException(Messages.get("error.blobclob.badlen"), "22003");
        InputStream in;
        try {
            in = new BlobInputStream(this, pos);
        } catch (final IOException e) {
            throw new SQLException(Messages.get("error.generic.ioerror", e.getMessage()), "HY000", e);
        }
        if (pos + length < totalLength) in = new FixedLengthInputStream(in, length, true);
        return in;
    }

    /**
   * Retrieve the BLOB data as an Big Endian Unicode
   * <code>InputStream</code>.
   *
   * @return the <code>InputStream</code> built over the BLOB data
   * @throws SQLException if an error occurs
   */
    final InputStream getUnicodeStream() throws SQLException {
        ensureOpen();
        try {
            return new UnicodeInputStream(this, 0);
        } catch (final IOException e) {
            throw new SQLException(Messages.get("error.generic.ioerror", e.getMessage()), "HY000", e);
        }
    }

    /**
   * Creates an <code>OutputStream</code> that can be used to update the
   * BLOB.
   * <p/>
   * Given that we cannot know the final size of a BLOB created by the caller
   * of this method, we assume the worst and create a disk BLOB by default.
   *
   * @param pos   the start position in the buffer (from 1)
   * @param ascii true if an ASCII output stream is required
   * @return the <code>OutputStream</code> to be used to update the BLOB
   * @throws SQLException if an error occurs
   */
    final OutputStream setBinaryStream(long pos, final boolean ascii) throws SQLException {
        ensureOpen();
        pos--;
        if (pos < 0) throw new SQLException(Messages.get("error.blobclob.badpos"), "HY090");
        if (pos > this.length) throw new SQLException(Messages.get("error.blobclob.badposlen"), "HY090");
        try {
            createBlobFileIfNecessary();
            if (ascii) return new AsciiOutputStream(this, pos); else return new BlobOutputStream(this, pos);
        } catch (final IOException ex) {
            throw new SQLException(Messages.get("error.generic.ioerror", ex.getMessage()), "HY000", ex);
        }
    }

    /**
   * Sets the content of the BLOB to the supplied byte array value.
   * <p/>
   * If the following conditions are met:
   * <ol>
   *   <li>The start position is 1
   *   <li>The existing BLOB length is smaller or the same as the length of
   *     the new data
   *   <li>The new data length does not exceed the in memory limit
   * </ol>
   * then the new data is buffered entirely in memory, otherwise a disk file
   * is created.
   *
   * @param pos    the start position in the buffer (from 1)
   * @param bytes  the byte array containing the data to copy
   * @param offset the start position in the byte array (from 0)
   * @param len    the number of bytes to copy
   * @param copy   true if a local copy of the byte array is required
   * @return the number of bytes copied
   * @throws SQLException if an error occurs
   */
    final int setBytes(long pos, final byte[] bytes, final int offset, final int len, final boolean copy) throws SQLException {
        ensureOpen();
        pos--;
        if (pos < 0) throw new SQLException(Messages.get("error.blobclob.badpos"), "HY090");
        if (pos > this.length) throw new SQLException(Messages.get("error.blobclob.badposlen"), "HY090");
        if (bytes == null) throw new SQLException(Messages.get("error.blob.bytesnull"), "HY009");
        if (offset < 0 || offset > bytes.length) throw new SQLException(Messages.get("error.blobclob.badoffset"), "HY090");
        if (len < 0 || pos + len > (long) Integer.MAX_VALUE || offset + len > bytes.length) throw new SQLException(Messages.get("error.blobclob.badlen"), "HY090");
        if ((this.fileHolder.path == null) && (pos == 0) && (len >= this.length) && (len <= maxMemSize)) {
            if (!copy) this.buffer = bytes; else this.buffer = Arrays.copyOfRange(bytes, offset, offset + len);
            this.length = len;
            return len;
        } else {
            try {
                createBlobFileIfNecessary();
                open();
                try {
                    write((int) pos, bytes, offset, len);
                } finally {
                    closeOne();
                }
                return len;
            } catch (final IOException e) {
                throw new SQLException(Messages.get("error.generic.ioerror", e.getMessage()), "HY000", e);
            }
        }
    }

    final int setBytes(long pos, final ByteBuffer bytes, final boolean copy) throws SQLException {
        ensureOpen();
        if (bytes == null) throw new SQLDataException(Messages.get("error.blob.bytesnull"), "HY009");
        final int remaining = bytes.remaining();
        if (remaining == 0) return 0; else if (copy) {
            final byte[] a = new byte[remaining];
            bytes.get(a);
            return setBytes(pos, a, 0, remaining, false);
        } else {
            pos--;
            if (pos < 0) throw new SQLDataException(Messages.get("error.blobclob.badpos"), "HY090");
            if (pos > this.length) throw new SQLDataException(Messages.get("error.blobclob.badposlen"), "HY090");
            if (pos + remaining > Integer.MAX_VALUE) throw new SQLDataException(Messages.get("error.blobclob.badlen"), "HY090");
            try {
                createBlobFileIfNecessary();
                open();
                try {
                    write((int) pos, bytes);
                } finally {
                    closeOne();
                }
                return remaining;
            } catch (final IOException ex) {
                throw new SQLNonTransientException(Messages.get("error.generic.ioerror", ex.getMessage()), "HY000", ex);
            }
        }
    }

    /**
   * Retrieves the length of this BLOB buffer in bytes.
   *
   * @return the length of the BLOB data in bytes
   */
    final long getLength() {
        return this.length;
    }

    /**
   * Retrieves the length of the BLOB buffer (in memory version only).
   *
   * @param length the length of the valid data in the buffer
   */
    final void setLength(final long length) {
        this.length = (int) length;
    }

    /**
   * Truncates the BLOB buffer to the specified size.
   *
   * @param len the required length
   * @throws SQLException if an error occurs
   */
    final void truncate(final long len) throws SQLException {
        ensureOpen();
        if (len < 0) throw new SQLDataException(Messages.get("error.blobclob.badlen"), "HY090");
        if (len > this.length) throw new SQLDataException(Messages.get("error.blobclob.lentoolong"), "HY090");
        length = (int) len;
        if (len == 0) {
            try {
                this.fileHolder.close();
            } catch (final IOException ex) {
                throw new SQLNonTransientException(Messages.get("error.generic.ioerror", ex.getMessage()), "HY000", ex);
            } finally {
                this.buffer = Bytes.EMPTY_ARRAY;
                this.openCount = 0;
                this.currentPage = INVALID_PAGE;
            }
        }
    }

    /**
   * Provides support for pattern searching methods.
   *
   * @param pattern the byte array containg the search pattern
   * @param start   the start position in the BLOB (from 1)
   * @return the <code>int</code> start index for the pattern (from 1) or -1
   *         if the pattern is not found.
   * @throws SQLException if an error occurs
   */
    final int position(final byte[] pattern, long start) throws SQLException {
        ensureOpen();
        try {
            start--;
            if (start < 0) throw new SQLDataException(Messages.get("error.blobclob.badpos"), "HY090");
            if (start >= this.length) throw new SQLDataException(Messages.get("error.blobclob.badposlen"), "HY090");
            if (pattern == null) throw new SQLDataException(Messages.get("error.blob.badpattern"), "HY009");
            if ((pattern.length == 0) || (length == 0) || (pattern.length > length)) return -1;
            final int limit = length - pattern.length;
            if (this.fileHolder.path == null) {
                for (int i = (int) start; i <= limit; i++) {
                    int p;
                    for (p = 0; p < pattern.length && buffer[i + p] == pattern[p]; p++) continue;
                    if (p == pattern.length) return i + 1;
                }
                return -1;
            } else {
                int result = -1;
                open();
                try {
                    for (int i = (int) start; i <= limit; i++) {
                        int p;
                        for (p = 0; p < pattern.length && read(i + p) == (pattern[p] & 0xFF); p++) continue;
                        if (p == pattern.length) {
                            result = i + 1;
                            break;
                        }
                    }
                } finally {
                    closeOne();
                }
                return result;
            }
        } catch (final IOException ex) {
            throw new SQLNonTransientException(Messages.get("error.generic.ioerror", ex.getMessage()), "HY000", ex);
        }
    }

    private static final class FileHolder extends Object implements Closeable {

        boolean closed;

        /**
     * The RA file object reference or null if closed.
     */
        RandomAccessFile file;

        /**
     * The name of the temporary BLOB disk file.
     */
        private File path;

        private Closer.CloseableReference close;

        private final Closer closer;

        FileHolder(final Closer closer) {
            super();
            this.closer = closer;
        }

        @Override
        public final void close() throws IOException {
            IOException ex = null;
            Closer.CloseableReference close;
            RandomAccessFile file;
            final File path;
            synchronized (this) {
                this.closed = true;
                close = this.close;
                file = this.file;
                path = this.path;
                this.file = null;
                this.path = null;
                this.close = null;
            }
            if (close != null) {
                close.clear();
                close = null;
            }
            if (file != null) {
                try {
                    file.close();
                } catch (final IOException x) {
                    ex = x;
                }
                file = null;
            }
            if (path != null) {
                try {
                    path.delete();
                } catch (final RuntimeException x) {
                    if (ex == null) throw x;
                }
            }
            if (ex != null) throw ex;
        }

        final void closeFile() throws IOException {
            final RandomAccessFile f;
            synchronized (this) {
                f = this.file;
                this.file = null;
            }
            if (f != null) f.close();
        }

        final void ensureOpen() throws IOException {
            if (this.closed) throw new IOException("closed");
        }

        final void tryClose() {
            try {
                close();
            } catch (final IOException ex) {
            } catch (final SecurityException ex) {
            }
        }
    }

    private abstract static class AbstractInputStream extends InputStream {

        int readPtr;

        BlobBuffer buffer;

        AbstractInputStream(final BlobBuffer buffer, final long pos) throws IOException {
            super();
            buffer.open();
            this.buffer = buffer;
            this.readPtr = (int) pos;
        }

        final BlobBuffer ensureOpen() throws IOException {
            final BlobBuffer buffer = this.buffer;
            if (buffer == null) throw new IOException("closed");
            return buffer;
        }

        @Override
        public final synchronized void close() throws IOException {
            final BlobBuffer buffer = this.buffer;
            if (buffer != null) {
                this.buffer = null;
                buffer.closeOne();
            }
        }
    }

    private abstract static class AbstractOutputStream extends OutputStream {

        int writePtr;

        BlobBuffer buffer;

        AbstractOutputStream(final BlobBuffer buffer, final long pos) throws IOException {
            super();
            buffer.open();
            this.buffer = buffer;
            this.writePtr = (int) pos;
        }

        final BlobBuffer ensureOpen() throws IOException {
            final BlobBuffer buffer = this.buffer;
            if (buffer == null) throw new IOException("closed");
            return buffer;
        }

        @Override
        public final synchronized void close() throws IOException {
            final BlobBuffer buffer = this.buffer;
            if (buffer != null) {
                this.buffer = null;
                buffer.closeOne();
            }
        }
    }

    /**
   * An <code>InputStream</code> over the BLOB buffer.
   */
    private static final class BlobInputStream extends AbstractInputStream {

        /**
     * Costructs an <code>InputStream</code> object over the BLOB buffer.
     *
     * @param pos  the starting position (from 0)
     * @throws IOException if an I/O error occurs
     */
        BlobInputStream(final BlobBuffer buffer, final long pos) throws IOException {
            super(buffer, pos);
        }

        /**
     * Returns the number of bytes available to read.
     *
     * @throws IOException if an I/O error occurs
     */
        @Override
        public final int available() throws IOException {
            return (int) ensureOpen().getLength() - this.readPtr;
        }

        /**
     * Reads the next byte from the stream.
     *
     * @return the next byte as an <code>int</code> or -1 if at EOF
     * @throws IOException if an I/O error occurs
     */
        @Override
        public final int read() throws IOException {
            final int b = ensureOpen().read(this.readPtr);
            if (b >= 0) this.readPtr++;
            return b;
        }

        /**
     * Reads a bytes from the stream.
     *
     * @param bytes  the byte array to fill
     * @param offset the start position in the byte array
     * @param len    the number of bytes to read
     * @return the number of bytes read or -1 if at end of file
     * @throws IOException if an I/O error occurs
     */
        @Override
        public final int read(final byte[] bytes, final int offset, final int len) throws IOException {
            final int count = ensureOpen().read(readPtr, bytes, offset, len);
            if (count > 0) this.readPtr += count;
            return count;
        }

        @Override
        public final long skip(long n) throws IOException {
            final BlobBuffer buffer = ensureOpen();
            if (n <= 0) return 0;
            final int pos = this.readPtr;
            n = Math.max(0, Math.min(n, buffer.getLength() - pos));
            this.readPtr = (int) (pos + n);
            return n;
        }
    }

    /**
   * A Big Endian Unicode <code>InputStream</code> over the CLOB buffer.
   */
    private static final class UnicodeInputStream extends AbstractInputStream {

        /**
     * Costructs an InputStream object over the BLOB buffer.
     *
     * @param pos  the starting position (from 0)
     * @throws IOException if an I/O error occurs
     */
        UnicodeInputStream(final BlobBuffer buffer, final long pos) throws IOException {
            super(buffer, pos);
        }

        /**
     * Returns the number of bytes available to read.
     *
     * @throws IOException if an I/O error occurs
     */
        @Override
        public final int available() throws IOException {
            return (int) ensureOpen().getLength() - this.readPtr;
        }

        /**
     * Reads the next byte from the stream.
     *
     * @return the next byte as an <code>int</code> or -1 if at EOF
     * @throws IOException if an I/O error occurs
     */
        @Override
        public final int read() throws IOException {
            final int b = ensureOpen().read(readPtr ^ 1);
            if (b >= 0) this.readPtr++;
            return b;
        }
    }

    /**
   * An ASCII <code>InputStream</code> over the CLOB buffer.
   * <p/>
   * This class interprets ASCII as anything which has a value below 0x80.
   * This is more rigid than other drivers which allow any character below
   * 0x100 to be converted to returned. The more relaxed coding is useful
   * when dealing with most single byte character sets and if this behaviour
   * is desired, comment out the line indicated in the read method.
   */
    private static final class AsciiInputStream extends AbstractInputStream {

        /**
     * Costructs an InputStream object over the BLOB buffer.
     *
     * @param pos  the starting position (from 0)
     * @throws IOException if an I/O error occurs
     */
        AsciiInputStream(final BlobBuffer buffer, final long pos) throws IOException {
            super(buffer, pos);
        }

        /**
     * Returns the number of bytes available to read.
     *
     * @throws IOException if an I/O error occurs
     */
        @Override
        public final int available() throws IOException {
            return ((int) ensureOpen().getLength() - this.readPtr) >> 1;
        }

        /**
     * Read the next byte from the stream.
     *
     * @return the next byte as an <code>int</code> or -1 if at EOF
     * @throws IOException if an I/O error occurs
     */
        @Override
        public final int read() throws IOException {
            final BlobBuffer buffer = ensureOpen();
            int b1 = buffer.read(readPtr);
            if (b1 >= 0) {
                readPtr++;
                final int b2 = buffer.read(readPtr);
                if (b2 >= 0) {
                    readPtr++;
                    if (b2 != 0 || b1 > 0x7F) b1 = '?';
                    return b1;
                }
            }
            return -1;
        }
    }

    /**
   * Implements an <code>OutputStream</code> for BLOB data.
   */
    private static final class BlobOutputStream extends AbstractOutputStream {

        /**
     * Costructs an OutputStream object over the BLOB buffer.
     *
     * @param pos  the starting position (from 0)
     * @throws IOException if an I/O error occurs
     */
        BlobOutputStream(final BlobBuffer buffer, final long pos) throws IOException {
            super(buffer, pos);
        }

        /**
     * Write a byte to the BLOB buffer.
     *
     * @param b the byte value to write
     * @throws IOException if an I/O error occurs
     */
        @Override
        public final void write(final int b) throws IOException {
            ensureOpen().write(writePtr++, b);
        }

        /**
     * Write bytes to the BLOB buffer.
     *
     * @param bytes  the byte array value to write
     * @param offset the start position in the byte array
     * @param len    the number of bytes to write
     * @throws IOException if an I/O error occurs
     */
        @Override
        public final void write(final byte[] bytes, final int offset, final int len) throws IOException {
            ensureOpen().write(this.writePtr, bytes, offset, len);
            this.writePtr += len;
        }
    }

    /**
   * Implements an ASCII <code>OutputStream</code> for CLOB data.
   */
    private static final class AsciiOutputStream extends AbstractOutputStream {

        /**
     * Costructs an ASCII <code>OutputStream</code> object over the BLOB
     * buffer.
     *
     * @param pos  the starting position (from 0)
     * @throws IOException if an I/O error occurs
     */
        AsciiOutputStream(final BlobBuffer buffer, final long pos) throws IOException {
            super(buffer, pos);
        }

        /**
     * Writes a byte to the BLOB buffer.
     *
     * @param b the byte value to write
     * @throws IOException if an I/O error occurs
     */
        @Override
        public final void write(final int b) throws IOException {
            final BlobBuffer buffer = ensureOpen();
            buffer.write(writePtr++, b);
            buffer.write(writePtr++, 0);
        }
    }
}

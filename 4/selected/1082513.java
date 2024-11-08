package jaxlib.tcol.tbyte;

import java.io.DataOutput;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ConcurrentModificationException;
import java.util.RandomAccess;
import jaxlib.util.sorting.SortAlgorithm;
import jaxlib.buffer.ByteBuffers;
import jaxlib.util.AccessTypeSet;
import jaxlib.io.stream.ByteBufferInputStream;
import jaxlib.io.stream.XInputStream;
import jaxlib.jaxlib_private.CheckArg;

/**
 * A list of bytes which delegates all operations to a {@link RandomAccessFile}.
 * <p>
 * <tt>ByteFileList</tt> supports all methods of the <tt>ByteList</tt> interface. However, modifying operations 
 * may throw an exception if the underlying file was opened in a restricted mode (read-only).
 * </p><p>
 * <tt>ByteFileList</tt> never automatically closes the underlying file. You have to do that manually using
 * the {@link #close()} method of this class. You could close the file directly, but then the <tt>ByteFileList</tt>
 * instance will keep references to buffers used to access the file. Thus you should always use the <tt>close</tt>
 * method of the <tt>ByteFileList</tt>.
 * </p><p>
 * The behaviour of a <tt>ByteFileList</tt> is unspecified if the underlying file gets modified while the list
 * is in use. Thus you should lock the file if you are not sure whether it could get modified concurrently.
 * </p><p>
 * All {@link IOException}s are rethrown as cause of a new {@link RuntimeException}.
 * <tt>ByteFileList</tt> throws an {@link IllegalStateException} if the size of the underlying file exceeds 
 * {@link Integer#MAX_VALUE} bytes. 
 * </p><p>
 * Please note that inserting elements into a <tt>ByteFileList</tt> is as slower as lower the insertion index
 * is. Also typically slow are remove operations when not done at the end of the file. The overall performance 
 * depends on the storage device. Let's hope near future will bring us a payable alternative to the outaged hard
 * disks.
 * </p><p>
 * Like most collection classes this class is not synchronized. If a <tt>ByteFileList</tt> gets modified 
 * structurally by more than one thread, it has to be synchronized externally. Structural modifications are all 
 * operations which are modifying the size of an <tt>ByteFileList</tt> instance.
 * </p><p>
 * The iterator returned by this class's is <i>fail-fast</i>: if the list is structurally modified at any time 
 * after the iterator is created, in any way except through the iterator's own <tt>add</tt> or <tt>remove</tt> 
 * method, then the iterator will throw a {@link ConcurrentModificationException}. Thus, in the face of concurrent 
 * modification, the iterator fails quickly and cleanly, rather than risking arbitrary, non-deterministic behavior
 * at an undetermined time in the future.<br>
 * </p><p>
 * Note that the fail-fast behavior of an iterator cannot be guaranteed as it is, generally speaking, impossible 
 * to make any hard guarantees in the presence of unsynchronized concurrent modification. Fail-fast iterators 
 * throw <tt>ConcurrentModificationException</tt> on a best-effort basis. Therefore, it would be wrong to write a
 * program that depended on this exception for its correctness: <i>the fail-fast behavior of iterators should be 
 * used only to detect bugs.</i>
 * </p><p>
 * The iterator does not detect concurrent modifications if they were not done via the <tt>ByteFileList</tt>
 * instance the iterator was created by.
 * </p>
 *
 * @see Throwable#getCause()
 * @see FileChannel#lock()
 * @see java.nio.channels.FileLock
 * @see RandomAccessFile
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: ByteFileList.java 1069 2004-04-09 15:48:50Z joerg_wassmer $
 */
public class ByteFileList extends AbstractByteList implements ByteList, RandomAccess {

    private static final int MAX_BUFFER = 2048;

    private static final int MAP_LIMIT = 128000;

    final FileChannel channel;

    final RandomAccessFile file;

    ByteBuffer heapBuffer;

    MappedByteBuffer mappedBuf;

    CharBuffer charBuf;

    AccessTypeSet accessTypes;

    /**
   * Constructs a new ByteFileList which delegates to specified <tt>RandomAccessFile</tt>.
   * <p>
   * This constructor does not aquire a lock on the file. Callers have to lock the file for themselves if desired.
   * Callers <i>should</i> lock the file if it could get modified while the <tt>ByteFileList</tt> is in use.
   * </p>
   *
   * @see FileChannel#lock()
   * @see java.nio.channels.FileLock
   *
   * @throws IOException          if an I/O error occurs.
   * @throws NullPointerException if <tt>file == null</tt>.
   */
    public ByteFileList(RandomAccessFile file) {
        super();
        this.file = file;
        this.channel = file.getChannel();
        size();
    }

    final ByteBuffer heapBuffer(int minLength) {
        ByteBuffer heapBuffer = this.heapBuffer;
        if ((heapBuffer == null) || (heapBuffer.capacity() < minLength)) this.heapBuffer = heapBuffer = ByteBuffer.allocate(Math.min(MAX_BUFFER, minLength + (minLength >> 1))); else heapBuffer.clear();
        return heapBuffer;
    }

    protected final ByteBuffer directBuffer() {
        return mappedBuffer(-1).duplicate();
    }

    protected final void releaseDirectBuffer() {
        releaseMappedBuffer();
    }

    final CharBuffer charBuffer(ByteOrder order, int size) {
        ByteBuffer buf = mappedBuffer(size);
        CharBuffer charBuf = this.charBuf;
        if ((charBuf == null) || (charBuf.order() != order)) {
            synchronized (buf) {
                return buf.order(order).asCharBuffer();
            }
        } else {
            return charBuf;
        }
    }

    /**
   * @return the whole file mapped to a buffer.
   *
   * @param size the actual size of the file or -1 if unknown.
   */
    final MappedByteBuffer mappedBuffer(int size) {
        if (size < 0) size = size();
        try {
            MappedByteBuffer mappedBuf = this.mappedBuf;
            if (mappedBuf == null) {
                this.mappedBuf = mappedBuf = this.channel.map(FileChannel.MapMode.READ_WRITE, 0, size);
            } else if (mappedBuf.remaining() != size) {
                this.charBuf = null;
                mappedBuf.force();
                this.mappedBuf = mappedBuf = this.channel.map(FileChannel.MapMode.READ_WRITE, 0, size);
            }
            return mappedBuf;
        } catch (IOException ex) {
            throw ioException(ex);
        }
    }

    final void releaseMappedBuffer() {
        if (this.mappedBuf != null) {
            this.mappedBuf.force();
            this.mappedBuf = null;
            this.charBuf = null;
        }
    }

    /**
   * This method is called if an {@link IOException} occured.
   * The default implementation simply throws a new {@link RuntimeException} with its cause set to the specified exception.
   * <p>
   * <b>Subclasses overwriting this method have to ensure that this method never exits normally!</b>
   * </p>
   *
   * @param ex the io exception (never null).
   *
   * @since JaXLib 1.0
   */
    protected RuntimeException ioException(IOException ex) {
        throw new RuntimeException(ex);
    }

    /**
   * Closes the underlying file.
   *
   * @see RandomAccessFile#close()
   *
   * @since JaXLib 1.0
   */
    public synchronized void close() {
        try {
            this.modCount++;
            flush();
            this.channel.close();
            this.file.close();
        } catch (IOException ex) {
            ioException(ex);
        } finally {
            this.mappedBuf = null;
            this.charBuf = null;
            this.heapBuffer = null;
        }
    }

    /**
   * Forces any buffered data to be written to the underlying file. 
   *
   * @since JaXLib 1.0
   */
    public synchronized void flush() {
        if (this.mappedBuf != null) this.mappedBuf.force();
        try {
            this.channel.force(true);
        } catch (IOException ex) {
            throw ioException(ex);
        }
    }

    /**
   * Returns true if and only if the underlying file of this <tt>ByteFileList</tt> is not closed.
   *
   * @see RandomAccessFile#getChannel()
   * @see java.nio.channels.Channel#isOpen()
   *
   * @since JaXLib 1.0
   */
    public boolean isOpen() {
        return this.channel.isOpen();
    }

    @Overrides
    public AccessTypeSet accessTypes() {
        if (this.accessTypes == null) {
            if (mappedBuffer(-1).isReadOnly()) this.accessTypes = AccessTypeSet.READ_ONLY; else this.accessTypes = AccessTypeSet.ALL;
        }
        return this.accessTypes;
    }

    @Overrides
    public final boolean add(byte e) {
        int size = size();
        addImpl(size, size, e);
        return true;
    }

    @Overrides
    public final void add(int index, byte e) {
        addImpl(size(), index, e);
    }

    final void addImpl(int size, int index, byte e) {
        CheckArg.rangeForAdding(size, index);
        if (size >= Integer.MAX_VALUE) throw new IllegalStateException("File is too big.");
        this.modCount++;
        RandomAccessFile file = this.file;
        try {
            int move = size - index;
            if (move == 0) {
                releaseMappedBuffer();
                file.seek(index);
                file.write(e);
            } else if (move > MAP_LIMIT) {
                file.setLength(size + 1);
                MappedByteBuffer mb = mappedBuffer(size + 1);
                for (int i = size; i > index; ) mb.put(i, mb.get(i - 1));
                mb.put(index, e);
            } else {
                prepareInsert(size, index, 1);
                file.seek(index);
                file.write(e);
            }
        } catch (IOException ex) {
            throw ioException(ex);
        }
    }

    final void prepareInsert(int size, int index, int count) throws IOException {
        CheckArg.rangeForAdding(size, index);
        if (count == 0) return;
        this.modCount++;
        releaseMappedBuffer();
        int move = size - index;
        if (move == 0) {
            this.file.setLength(size + count);
        } else if (move > MAP_LIMIT) {
            this.file.setLength(size + count);
            MappedByteBuffer mb = mappedBuffer(size + count);
            for (int i = size; i > index; i--) mb.put(i, mb.get(i - 1));
        } else {
            this.file.setLength(size + count);
            ByteBuffer buf = heapBuffer(move);
            int moveFrom = size;
            while (move > 0) {
                int step = Math.min(buf.capacity(), move);
                buf.position(0).limit(step);
                moveFrom -= step;
                this.channel.position(moveFrom);
                while (buf.hasRemaining()) {
                    if (this.channel.read(buf) < 0) throw new ConcurrentModificationException("Either this ByteFileList or the underlying file was modified by another thread.");
                }
                buf.flip();
                this.channel.position(moveFrom + count).write(buf);
                move -= step;
            }
        }
    }

    @Overrides
    public int addAll(int index, ByteList src, int fromIndex, int toIndex) {
        if (src == this) return addSelf(index, fromIndex, toIndex);
        int size = size();
        CheckArg.rangeForAdding(size, index);
        CheckArg.range(src.size(), fromIndex, toIndex);
        int count = toIndex - fromIndex;
        if (count == 0) return 0;
        if ((((long) size) + (long) count) > Integer.MAX_VALUE) throw new IllegalStateException("Capacity would be exhausted.");
        try {
            if (index == size) {
                this.modCount++;
                releaseMappedBuffer();
            } else {
                prepareInsert(size, index, count);
            }
            this.file.seek(index);
            src.toStream(fromIndex, toIndex, this.file);
        } catch (IOException ex) {
            throw ioException(ex);
        }
        return count;
    }

    @Overrides
    public final int addAll(int index, byte[] src, int fromIndex, int toIndex) {
        int size = size();
        CheckArg.rangeForAdding(size, index);
        CheckArg.range(src.length, fromIndex, toIndex);
        int count = toIndex - fromIndex;
        if (count == 0) return 0;
        if ((((long) size) + (long) count) > Integer.MAX_VALUE) throw new IllegalStateException("Capacity would be exhausted.");
        try {
            if (index == size) {
                this.modCount++;
                releaseMappedBuffer();
            } else {
                prepareInsert(size, index, count);
            }
            this.file.seek(index);
            this.file.write(src, fromIndex, count);
        } catch (IOException ex) {
            throw ioException(ex);
        }
        return count;
    }

    @Overrides
    public final int addRemaining(int index, ByteBuffer src) {
        int size = size();
        CheckArg.rangeForAdding(size, index);
        int count = src.remaining();
        if (count == 0) return 0;
        if ((((long) size) + (long) count) > Integer.MAX_VALUE) throw new IllegalStateException("Capacity would be exhausted.");
        try {
            if (index == size) {
                this.modCount++;
                releaseMappedBuffer();
            } else {
                prepareInsert(size, index, count);
            }
            this.channel.position(index).write(src);
        } catch (IOException ex) {
            throw ioException(ex);
        }
        return count;
    }

    final int addSelf(int index, int fromIndex, int toIndex) {
        int size = size();
        CheckArg.rangeForAdding(size, index);
        CheckArg.range(size, fromIndex, toIndex);
        final int count = toIndex - fromIndex;
        if (count == 0) return 0;
        this.modCount++;
        int newSize = size + count;
        try {
            prepareInsert(size, index, count);
        } catch (IOException ex) {
            throw ioException(ex);
        }
        if (index == size) copy(fromIndex, toIndex, index); else if (index >= toIndex) {
            copy(index, index + size, index + count);
            ByteBuffer dest = mappedBuffer(newSize).duplicate();
            ByteBuffer source = dest.duplicate();
            source.position(fromIndex).limit(toIndex);
            dest.position(index);
            dest.put(source);
        } else if (index <= fromIndex) {
            copy(index, index + size, index + count);
            fromIndex += count;
            toIndex += count;
            ByteBuffer dest = mappedBuffer(newSize).duplicate();
            ByteBuffer source = dest.duplicate();
            source.position(fromIndex).limit(toIndex);
            dest.position(index);
            dest.put(source);
        } else {
            copy(index, size, index + count);
            copy(index, toIndex, toIndex);
            copy(fromIndex, index, index);
        }
        return count;
    }

    @Overrides
    public final void at(int index, byte e) {
        mappedBuffer(-1).put(index, e);
    }

    @Overrides
    public final int binarySearch(int fromIndex, int toIndex, byte e) {
        int size = size();
        CheckArg.range(size, fromIndex, toIndex);
        return ByteBuffers.binarySearch(mappedBuffer(size), fromIndex, toIndex, e);
    }

    @Overrides
    public final int binarySearchFirst(int fromIndex, int toIndex, byte e) {
        int size = size();
        CheckArg.range(size, fromIndex, toIndex);
        return ByteBuffers.binarySearchFirst(mappedBuffer(size), fromIndex, toIndex, e);
    }

    @Overrides
    public final int binarySearchLast(int fromIndex, int toIndex, byte e) {
        int size = size();
        CheckArg.range(size, fromIndex, toIndex);
        return ByteBuffers.binarySearchLast(mappedBuffer(size), fromIndex, toIndex, e);
    }

    @Overrides
    public final void clear() {
        int size = size();
        clearImpl(size, 0, size);
    }

    @Overrides
    public final void clear(int index) {
        clearImpl(size(), index, index + 1);
    }

    @Overrides
    public final void clear(int fromIndex, int toIndex) {
        clearImpl(size(), fromIndex, toIndex);
    }

    final void clearImpl(int size, int fromIndex, int toIndex) {
        CheckArg.range(size, fromIndex, toIndex);
        int count = toIndex - fromIndex;
        if (count == 0) return;
        this.modCount++;
        try {
            MappedByteBuffer mb = this.mappedBuf;
            this.mappedBuf = null;
            this.charBuf = null;
            int move = size - toIndex;
            if (move == 0) {
                if (mb != null) mb.force();
            } else if ((move > 128000) || (mb != null)) {
                if (mb == null) mb = this.channel.map(FileChannel.MapMode.READ_WRITE, 0, size);
                ByteBuffer moveRegion = mb.duplicate();
                moveRegion.position(toIndex);
                mb.position(fromIndex);
                mb.put(moveRegion);
                mb.force();
            } else {
                int moveFrom = toIndex;
                int moveTo = fromIndex;
                ByteBuffer buf = heapBuffer(move);
                while (move > 0) {
                    buf.position(0).limit(Math.min(buf.capacity(), move));
                    int step = this.channel.read(buf, moveFrom);
                    if (step < 0) throw new ConcurrentModificationException("Either this ByteFileList or the underlying file was modified by another thread.");
                    buf.flip();
                    this.channel.write(buf, moveTo);
                    move -= step;
                    moveFrom += step;
                    moveTo += step;
                }
            }
            this.file.setLength(size - count);
        } catch (IOException ex) {
            throw ioException(ex);
        }
    }

    @Overrides
    public int copy(int fromIndex, int toIndex, int destIndex) {
        int size = size();
        CheckArg.copyListRangeIntern(size, fromIndex, toIndex, destIndex);
        if (fromIndex == toIndex || fromIndex == destIndex) return 0;
        int add = Math.max(0, (destIndex + (toIndex - fromIndex)) - size);
        if (add > 0) {
            addSelf(size, toIndex - add, toIndex);
            toIndex -= add;
            size += add;
        }
        ByteBuffers.copy(mappedBuffer(size).duplicate(), fromIndex, toIndex, destIndex);
        return add;
    }

    @Overrides
    public final byte cut(int index) {
        int size = size();
        CheckArg.range(size, index);
        byte e = mappedBuffer(size).get(index);
        clearImpl(size, index, index + 1);
        return e;
    }

    @Overrides
    public final int fill(int fromIndex, int toIndex, byte e) {
        int size = size();
        CheckArg.fill(size, fromIndex, toIndex);
        if (fromIndex == toIndex) return 0;
        int add = Math.max(0, toIndex - size);
        if (add > 0) {
            if ((long) size + (long) add > Integer.MAX_VALUE) throw new IllegalStateException("File too big.");
            this.modCount++;
        }
        try {
            if (add > 0) {
                releaseMappedBuffer();
                this.file.setLength(size + add);
            }
        } catch (IOException ex) {
            throw ioException(ex);
        }
        ByteBuffers.fill(mappedBuffer(size + add), fromIndex, toIndex, e);
        return add;
    }

    @Overrides
    public final byte get(int index) {
        return mappedBuffer(-1).get(index);
    }

    @Overrides
    public final int indexOf(int fromIndex, int toIndex, byte e) {
        int size = size();
        CheckArg.range(size, fromIndex, toIndex);
        if (fromIndex == toIndex) return -1;
        MappedByteBuffer mb = mappedBuffer(size);
        while (fromIndex < toIndex) {
            if (e == mb.get(fromIndex)) return fromIndex;
            fromIndex++;
        }
        return -1;
    }

    @Overrides
    public final int lastIndexOf(int fromIndex, int toIndex, byte e) {
        int size = size();
        CheckArg.range(size, fromIndex, toIndex);
        if (fromIndex == toIndex) return -1;
        MappedByteBuffer mb = mappedBuffer(size);
        while (fromIndex <= --toIndex) {
            if (e == mb.get(toIndex)) return toIndex;
        }
        return -1;
    }

    @Overrides
    public XInputStream inputStream(int nextIndex) {
        return new InputStreamImpl(this, nextIndex);
    }

    @Overrides
    public final void reverse(int fromIndex, int toIndex) {
        int size = size();
        CheckArg.range(size, fromIndex, toIndex);
        if (toIndex - fromIndex < 2) return;
        ByteBuffers.reverse(mappedBuffer(size), fromIndex, toIndex);
    }

    @Overrides
    public final void rotate(int fromIndex, int toIndex, int distance) {
        int size = size();
        CheckArg.range(size, fromIndex, toIndex);
        if ((toIndex - fromIndex < 2) || (distance == 0)) return;
        ByteBuffers.rotate(mappedBuffer(size), fromIndex, toIndex, distance);
    }

    @Overrides
    public final byte set(int index, byte e) {
        MappedByteBuffer mb = mappedBuffer(-1);
        byte old = mb.get(index);
        mb.put(index, e);
        return old;
    }

    /**
   * Returns the length of the underlying file.
   *
   * @throws IllegalStateException if the file is bigger than {@link Integer#MAX_VALUE}.
   *
   * @see RandomAccessFile#length()
   */
    @Overrides
    public final int size() {
        long size;
        try {
            size = this.file.length();
        } catch (IOException ex) {
            throw ioException(ex);
        }
        if (size > Integer.MAX_VALUE) throw new IllegalStateException("File too big.");
        return (int) size;
    }

    @Overrides
    public final void sort(int fromIndex, int toIndex, SortAlgorithm algo) {
        int size = size();
        CheckArg.range(size, fromIndex, toIndex);
        if (toIndex - fromIndex < 2) return;
        if (algo == null) algo = SortAlgorithm.getDefault();
        algo.apply(mappedBuffer(size), fromIndex, toIndex);
    }

    @Overrides
    public final void swap(int index1, int index2) {
        int size = size();
        CheckArg.swap(size, index1, index2);
        if (index1 == index2) return;
        MappedByteBuffer mb = mappedBuffer(size);
        byte t = mb.get(index1);
        mb.put(index1, mb.get(index2));
        mb.put(index2, t);
    }

    @Overrides
    public final void toArray(int fromIndex, int toIndex, byte[] dest, int destIndex) {
        int size = size();
        CheckArg.copyRangeTo(size, fromIndex, toIndex, dest.length, destIndex);
        if (fromIndex == toIndex) return;
        MappedByteBuffer mb = mappedBuffer(size);
        if (toIndex - fromIndex <= 16) {
            while (fromIndex < toIndex) dest[destIndex++] = mb.get(fromIndex++);
        } else {
            ByteBuffer buf = mb.duplicate();
            buf.position(fromIndex).limit(toIndex);
            buf.get(dest, destIndex, toIndex - fromIndex);
        }
    }

    @Overrides
    public final void toBuffer(int fromIndex, int toIndex, ByteBuffer dest) {
        int size = size();
        CheckArg.range(size, fromIndex, toIndex);
        if (fromIndex == toIndex) return;
        ByteBuffer buf = mappedBuffer(size).asReadOnlyBuffer();
        buf.position(fromIndex).limit(toIndex);
        dest.put(buf);
    }

    @Overrides
    public void toByteChannel(int fromIndex, int toIndex, WritableByteChannel dest) throws IOException {
        if (this.channel == dest) super.toByteChannel(fromIndex, toIndex, dest); else {
            int size = size();
            CheckArg.range(size, fromIndex, toIndex);
            if (fromIndex == toIndex) return; else if (dest instanceof FileChannel) {
                for (int remaining = toIndex - fromIndex; remaining > 0; ) {
                    int step = (int) this.channel.transferTo(fromIndex, remaining, (FileChannel) dest);
                    fromIndex += step;
                    remaining -= step;
                }
            } else {
                ByteBuffer buf = mappedBuffer(size).asReadOnlyBuffer();
                buf.position(fromIndex).limit(toIndex);
                while (buf.remaining() > 0) dest.write(buf);
            }
        }
    }

    @Overrides
    public void toOutputStream(int fromIndex, int toIndex, OutputStream dest) throws IOException {
        if ((toIndex - fromIndex > 10) && (dest instanceof FileOutputStream)) {
            toByteChannel(fromIndex, toIndex, ((FileOutputStream) dest).getChannel());
        } else {
            CheckArg.range(size(), fromIndex, toIndex);
            int remaining = toIndex - fromIndex;
            if (remaining <= 16) {
                while (fromIndex < toIndex) dest.write(get(fromIndex++));
            } else {
                int bufLen = Math.min(1024, remaining);
                byte[] buf = new byte[bufLen];
                while (fromIndex < toIndex) {
                    int step = Math.min(bufLen, remaining);
                    toArray(fromIndex, fromIndex + step, buf, 0);
                    dest.write(buf);
                    fromIndex += step;
                }
            }
        }
    }

    @Overrides
    public void toStream(int fromIndex, int toIndex, DataOutput dest) throws IOException {
        if ((dest == this.file) || (toIndex - fromIndex <= 10) || !(dest instanceof RandomAccessFile)) super.toStream(fromIndex, toIndex, dest); else toByteChannel(fromIndex, toIndex, ((RandomAccessFile) dest).getChannel());
    }

    private static final class InputStreamImpl extends ByteBufferInputStream {

        private ByteFileList delegate;

        private final int modCount;

        InputStreamImpl(ByteFileList delegate, int nextIndex) {
            super(null);
            int size = delegate.size();
            CheckArg.rangeForIterator(size, nextIndex);
            ByteBuffer b = delegate.mappedBuffer(size).duplicate();
            b.position(nextIndex);
            super.in = b;
            this.delegate = delegate;
            this.modCount = delegate.modCount();
        }

        @Overrides
        protected ByteBuffer ensureOpen() throws IOException {
            ByteBuffer b = super.in;
            ByteFileList delegate = this.delegate;
            if ((b == null) || (delegate == null) || (!delegate.isOpen())) {
                super.in = null;
                this.delegate = null;
                throw new ClosedChannelException();
            }
            if (this.modCount != delegate.modCount()) throw (IOException) new IOException().initCause(new ConcurrentModificationException());
            return b;
        }

        @Overrides
        public void close() throws IOException {
            this.delegate = null;
            super.close();
        }
    }
}

package gnu.java.nio.channels;

import gnu.classpath.Configuration;
import gnu.java.nio.FileLockImpl;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * This file is not user visible !
 * But alas, Java does not have a concept of friendly packages
 * so this class is public. 
 * Instances of this class are created by invoking getChannel
 * Upon a Input/Output/RandomAccessFile object.
 */
public final class FileChannelImpl extends FileChannel {

    public static final int READ = 1;

    public static final int WRITE = 2;

    public static final int APPEND = 4;

    public static final int EXCL = 8;

    public static final int SYNC = 16;

    public static final int DSYNC = 32;

    public static FileChannelImpl in;

    public static FileChannelImpl out;

    public static FileChannelImpl err;

    private static native void init();

    static {
        if (Configuration.INIT_LOAD_LIBRARY) {
            System.loadLibrary("javanio");
        }
        init();
        in = new FileChannelImpl(0, READ);
        out = new FileChannelImpl(1, WRITE);
        err = new FileChannelImpl(2, WRITE);
    }

    private int fd = -1;

    private int mode;

    final String description;

    public static FileChannelImpl create(File file, int mode) throws FileNotFoundException {
        return new FileChannelImpl(file, mode);
    }

    private FileChannelImpl(File file, int mode) throws FileNotFoundException {
        String path = file.getPath();
        description = path;
        fd = open(path, mode);
        this.mode = mode;
        if (file.isDirectory()) {
            try {
                close();
            } catch (IOException e) {
            }
            throw new FileNotFoundException(description + " is a directory");
        }
    }

    /**
   * Constructor for default channels in, out and err.
   *
   * Used by init() (native code).
   *
   * @param fd the file descriptor (0, 1, 2 for stdin, stdout, stderr).
   *
   * @param mode READ or WRITE
   */
    FileChannelImpl(int fd, int mode) {
        this.fd = fd;
        this.mode = mode;
        this.description = "descriptor(" + fd + ")";
    }

    private native int open(String path, int mode) throws FileNotFoundException;

    public native int available() throws IOException;

    private native long implPosition() throws IOException;

    private native void seek(long newPosition) throws IOException;

    private native void implTruncate(long size) throws IOException;

    public native void unlock(long pos, long len) throws IOException;

    public native long size() throws IOException;

    protected native void implCloseChannel() throws IOException;

    /**
   * Makes sure the Channel is properly closed.
   */
    protected void finalize() throws IOException {
        if (fd != -1) close();
    }

    public int read(ByteBuffer dst) throws IOException {
        int result;
        byte[] buffer = new byte[dst.remaining()];
        result = read___3BII(buffer, 0, buffer.length);
        if (result > 0) dst.put(buffer, 0, result);
        return result;
    }

    public int read(ByteBuffer dst, long position) throws IOException {
        if (position < 0) throw new IllegalArgumentException("position: " + position);
        long oldPosition = implPosition();
        position(position);
        int result = read(dst);
        position(oldPosition);
        return result;
    }

    public int read() throws IOException {
        return read__();
    }

    public native int read__() throws IOException;

    public int read(byte[] buffer, int offset, int length) throws IOException {
        return read___3BII(buffer, offset, length);
    }

    public native int read___3BII(byte[] buffer, int offset, int length) throws IOException;

    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        long result = 0;
        for (int i = offset; i < offset + length; i++) {
            result += read(dsts[i]);
        }
        return result;
    }

    public int write(ByteBuffer src) throws IOException {
        int len = src.remaining();
        if (src.hasArray()) {
            byte[] buffer = src.array();
            write___3BII(buffer, src.arrayOffset() + src.position(), len);
            src.position(src.position() + len);
        } else {
            byte[] buffer = new byte[len];
            src.get(buffer, 0, len);
            write___3BII(buffer, 0, len);
        }
        return len;
    }

    public int write(ByteBuffer src, long position) throws IOException {
        if (position < 0) throw new IllegalArgumentException("position: " + position);
        if (!isOpen()) throw new ClosedChannelException();
        if ((mode & WRITE) == 0) throw new NonWritableChannelException();
        int result;
        long oldPosition;
        oldPosition = implPosition();
        seek(position);
        result = write(src);
        seek(oldPosition);
        return result;
    }

    public void write(byte[] buffer, int offset, int length) throws IOException {
        write___3BII(buffer, offset, length);
    }

    public native void write___3BII(byte[] buffer, int offset, int length) throws IOException;

    public void write(int b) throws IOException {
        write__I(b);
    }

    public native void write__I(int b) throws IOException;

    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        long result = 0;
        for (int i = offset; i < offset + length; i++) {
            result += write(srcs[i]);
        }
        return result;
    }

    public native MappedByteBuffer mapImpl(char mode, long position, int size) throws IOException;

    public MappedByteBuffer map(FileChannel.MapMode mode, long position, long size) throws IOException {
        char nmode = 0;
        if (mode == MapMode.READ_ONLY) {
            nmode = 'r';
            if ((this.mode & READ) == 0) throw new NonReadableChannelException();
        } else if (mode == MapMode.READ_WRITE || mode == MapMode.PRIVATE) {
            nmode = mode == MapMode.READ_WRITE ? '+' : 'c';
            if ((this.mode & WRITE) != WRITE) throw new NonWritableChannelException();
            if ((this.mode & READ) != READ) throw new NonReadableChannelException();
        } else throw new IllegalArgumentException("mode: " + mode);
        if (position < 0 || size < 0 || size > Integer.MAX_VALUE) throw new IllegalArgumentException("position: " + position + ", size: " + size);
        return mapImpl(nmode, position, (int) size);
    }

    /**
   * msync with the disk
   */
    public void force(boolean metaData) throws IOException {
        if (!isOpen()) throw new ClosedChannelException();
        force();
    }

    private native void force();

    private int smallTransferTo(long position, int count, WritableByteChannel target) throws IOException {
        ByteBuffer buffer;
        try {
            buffer = map(MapMode.READ_ONLY, position, count);
        } catch (IOException e) {
            buffer = ByteBuffer.allocate(count);
            read(buffer, position);
            buffer.flip();
        }
        return target.write(buffer);
    }

    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        if (position < 0 || count < 0) throw new IllegalArgumentException("position: " + position + ", count: " + count);
        if (!isOpen()) throw new ClosedChannelException();
        if ((mode & READ) == 0) throw new NonReadableChannelException();
        final int pageSize = 65536;
        long total = 0;
        while (count > 0) {
            int transferred = smallTransferTo(position, (int) Math.min(count, pageSize), target);
            if (transferred < 0) break;
            total += transferred;
            position += transferred;
            count -= transferred;
        }
        return total;
    }

    private int smallTransferFrom(ReadableByteChannel src, long position, int count) throws IOException {
        ByteBuffer buffer = null;
        if (src instanceof FileChannel) {
            try {
                buffer = ((FileChannel) src).map(MapMode.READ_ONLY, position, count);
            } catch (IOException e) {
            }
        }
        if (buffer == null) {
            buffer = ByteBuffer.allocate((int) count);
            src.read(buffer);
            buffer.flip();
        }
        return write(buffer, position);
    }

    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        if (position < 0 || count < 0) throw new IllegalArgumentException("position: " + position + ", count: " + count);
        if (!isOpen()) throw new ClosedChannelException();
        if ((mode & WRITE) == 0) throw new NonWritableChannelException();
        final int pageSize = 65536;
        long total = 0;
        while (count > 0) {
            int transferred = smallTransferFrom(src, position, (int) Math.min(count, pageSize));
            if (transferred < 0) break;
            total += transferred;
            position += transferred;
            count -= transferred;
        }
        return total;
    }

    private void lockCheck(long position, long size, boolean shared) throws IOException {
        if (position < 0 || size < 0) throw new IllegalArgumentException("position: " + position + ", size: " + size);
        if (!isOpen()) throw new ClosedChannelException();
        if (shared && ((mode & READ) == 0)) throw new NonReadableChannelException();
        if (!shared && ((mode & WRITE) == 0)) throw new NonWritableChannelException();
    }

    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        lockCheck(position, size, shared);
        boolean completed = false;
        try {
            begin();
            boolean lockable = lock(position, size, shared, false);
            completed = true;
            return (lockable ? new FileLockImpl(this, position, size, shared) : null);
        } finally {
            end(completed);
        }
    }

    /** Try to acquire a lock at the given position and size.
   * On success return true.
   * If wait as specified, block until we can get it.
   * Otherwise return false.
   */
    private native boolean lock(long position, long size, boolean shared, boolean wait) throws IOException;

    public FileLock lock(long position, long size, boolean shared) throws IOException {
        lockCheck(position, size, shared);
        boolean completed = false;
        try {
            boolean lockable = lock(position, size, shared, true);
            completed = true;
            return (lockable ? new FileLockImpl(this, position, size, shared) : null);
        } finally {
            end(completed);
        }
    }

    public long position() throws IOException {
        if (!isOpen()) throw new ClosedChannelException();
        return implPosition();
    }

    public FileChannel position(long newPosition) throws IOException {
        if (newPosition < 0) throw new IllegalArgumentException("newPostition: " + newPosition);
        if (!isOpen()) throw new ClosedChannelException();
        seek(newPosition);
        return this;
    }

    public FileChannel truncate(long size) throws IOException {
        if (size < 0) throw new IllegalArgumentException("size: " + size);
        if (!isOpen()) throw new ClosedChannelException();
        if ((mode & WRITE) == 0) throw new NonWritableChannelException();
        if (size < size()) implTruncate(size);
        return this;
    }

    public String toString() {
        return (this.getClass() + "[fd=" + fd + ",mode=" + mode + "," + description + "]");
    }
}

package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.List;
import java.security.AccessController;
import sun.misc.Cleaner;
import sun.security.action.GetPropertyAction;

public class FileChannelImpl extends FileChannel {

    private static final FileDispatcher nd;

    private static final long allocationGranularity;

    private final FileDescriptor fd;

    private final boolean writable;

    private final boolean readable;

    private final Object parent;

    private final NativeThreadSet threads = new NativeThreadSet(2);

    private final Object positionLock = new Object();

    private FileChannelImpl(FileDescriptor fd, boolean readable, boolean writable, Object parent) {
        this.fd = fd;
        this.readable = readable;
        this.writable = writable;
        this.parent = parent;
    }

    public static FileChannel open(FileDescriptor fd, boolean readable, boolean writable, Object parent) {
        return new FileChannelImpl(fd, readable, writable, parent);
    }

    private void ensureOpen() throws IOException {
        if (!isOpen()) throw new ClosedChannelException();
    }

    protected void implCloseChannel() throws IOException {
        if (fileLockTable != null) {
            for (FileLock fl : fileLockTable.removeAll()) {
                synchronized (fl) {
                    if (fl.isValid()) {
                        nd.release(fd, fl.position(), fl.size());
                        ((FileLockImpl) fl).invalidate();
                    }
                }
            }
        }
        nd.preClose(fd);
        threads.signalAndWait();
        if (parent != null) {
            ((java.io.Closeable) parent).close();
        } else {
            nd.close(fd);
        }
    }

    public int read(ByteBuffer dst) throws IOException {
        ensureOpen();
        if (!readable) throw new NonReadableChannelException();
        synchronized (positionLock) {
            int n = 0;
            int ti = -1;
            try {
                begin();
                ti = threads.add();
                if (!isOpen()) return 0;
                do {
                    n = IOUtil.read(fd, dst, -1, nd, positionLock);
                } while ((n == IOStatus.INTERRUPTED) && isOpen());
                return IOStatus.normalize(n);
            } finally {
                threads.remove(ti);
                end(n > 0);
                assert IOStatus.check(n);
            }
        }
    }

    private long read0(ByteBuffer[] dsts) throws IOException {
        ensureOpen();
        if (!readable) throw new NonReadableChannelException();
        synchronized (positionLock) {
            long n = 0;
            int ti = -1;
            try {
                begin();
                ti = threads.add();
                if (!isOpen()) return 0;
                do {
                    n = IOUtil.read(fd, dsts, nd);
                } while ((n == IOStatus.INTERRUPTED) && isOpen());
                return IOStatus.normalize(n);
            } finally {
                threads.remove(ti);
                end(n > 0);
                assert IOStatus.check(n);
            }
        }
    }

    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        if ((offset < 0) || (length < 0) || (offset > dsts.length - length)) throw new IndexOutOfBoundsException();
        return read0(Util.subsequence(dsts, offset, length));
    }

    public int write(ByteBuffer src) throws IOException {
        ensureOpen();
        if (!writable) throw new NonWritableChannelException();
        synchronized (positionLock) {
            int n = 0;
            int ti = -1;
            try {
                begin();
                ti = threads.add();
                if (!isOpen()) return 0;
                do {
                    n = IOUtil.write(fd, src, -1, nd, positionLock);
                } while ((n == IOStatus.INTERRUPTED) && isOpen());
                return IOStatus.normalize(n);
            } finally {
                threads.remove(ti);
                end(n > 0);
                assert IOStatus.check(n);
            }
        }
    }

    private long write0(ByteBuffer[] srcs) throws IOException {
        ensureOpen();
        if (!writable) throw new NonWritableChannelException();
        synchronized (positionLock) {
            long n = 0;
            int ti = -1;
            try {
                begin();
                ti = threads.add();
                if (!isOpen()) return 0;
                do {
                    n = IOUtil.write(fd, srcs, nd);
                } while ((n == IOStatus.INTERRUPTED) && isOpen());
                return IOStatus.normalize(n);
            } finally {
                threads.remove(ti);
                end(n > 0);
                assert IOStatus.check(n);
            }
        }
    }

    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        if ((offset < 0) || (length < 0) || (offset > srcs.length - length)) throw new IndexOutOfBoundsException();
        return write0(Util.subsequence(srcs, offset, length));
    }

    public long position() throws IOException {
        ensureOpen();
        synchronized (positionLock) {
            long p = -1;
            int ti = -1;
            try {
                begin();
                ti = threads.add();
                if (!isOpen()) return 0;
                do {
                    p = position0(fd, -1);
                } while ((p == IOStatus.INTERRUPTED) && isOpen());
                return IOStatus.normalize(p);
            } finally {
                threads.remove(ti);
                end(p > -1);
                assert IOStatus.check(p);
            }
        }
    }

    public FileChannel position(long newPosition) throws IOException {
        ensureOpen();
        if (newPosition < 0) throw new IllegalArgumentException();
        synchronized (positionLock) {
            long p = -1;
            int ti = -1;
            try {
                begin();
                ti = threads.add();
                if (!isOpen()) return null;
                do {
                    p = position0(fd, newPosition);
                } while ((p == IOStatus.INTERRUPTED) && isOpen());
                return this;
            } finally {
                threads.remove(ti);
                end(p > -1);
                assert IOStatus.check(p);
            }
        }
    }

    public long size() throws IOException {
        ensureOpen();
        synchronized (positionLock) {
            long s = -1;
            int ti = -1;
            try {
                begin();
                ti = threads.add();
                if (!isOpen()) return -1;
                do {
                    s = nd.size(fd);
                } while ((s == IOStatus.INTERRUPTED) && isOpen());
                return IOStatus.normalize(s);
            } finally {
                threads.remove(ti);
                end(s > -1);
                assert IOStatus.check(s);
            }
        }
    }

    public FileChannel truncate(long size) throws IOException {
        ensureOpen();
        if (size < 0) throw new IllegalArgumentException();
        if (size > size()) return this;
        if (!writable) throw new NonWritableChannelException();
        synchronized (positionLock) {
            int rv = -1;
            long p = -1;
            int ti = -1;
            try {
                begin();
                ti = threads.add();
                if (!isOpen()) return null;
                do {
                    p = position0(fd, -1);
                } while ((p == IOStatus.INTERRUPTED) && isOpen());
                if (!isOpen()) return null;
                assert p >= 0;
                do {
                    rv = nd.truncate(fd, size);
                } while ((rv == IOStatus.INTERRUPTED) && isOpen());
                if (!isOpen()) return null;
                if (p > size) p = size;
                do {
                    rv = (int) position0(fd, p);
                } while ((rv == IOStatus.INTERRUPTED) && isOpen());
                return this;
            } finally {
                threads.remove(ti);
                end(rv > -1);
                assert IOStatus.check(rv);
            }
        }
    }

    public void force(boolean metaData) throws IOException {
        ensureOpen();
        int rv = -1;
        int ti = -1;
        try {
            begin();
            ti = threads.add();
            if (!isOpen()) return;
            do {
                rv = nd.force(fd, metaData);
            } while ((rv == IOStatus.INTERRUPTED) && isOpen());
        } finally {
            threads.remove(ti);
            end(rv > -1);
            assert IOStatus.check(rv);
        }
    }

    private static volatile boolean transferSupported = true;

    private static volatile boolean pipeSupported = true;

    private static volatile boolean fileSupported = true;

    private long transferToDirectly(long position, int icount, WritableByteChannel target) throws IOException {
        if (!transferSupported) return IOStatus.UNSUPPORTED;
        FileDescriptor targetFD = null;
        if (target instanceof FileChannelImpl) {
            if (!fileSupported) return IOStatus.UNSUPPORTED_CASE;
            targetFD = ((FileChannelImpl) target).fd;
        } else if (target instanceof SelChImpl) {
            if ((target instanceof SinkChannelImpl) && !pipeSupported) return IOStatus.UNSUPPORTED_CASE;
            targetFD = ((SelChImpl) target).getFD();
        }
        if (targetFD == null) return IOStatus.UNSUPPORTED;
        int thisFDVal = IOUtil.fdVal(fd);
        int targetFDVal = IOUtil.fdVal(targetFD);
        if (thisFDVal == targetFDVal) return IOStatus.UNSUPPORTED;
        long n = -1;
        int ti = -1;
        try {
            begin();
            ti = threads.add();
            if (!isOpen()) return -1;
            do {
                n = transferTo0(thisFDVal, position, icount, targetFDVal);
            } while ((n == IOStatus.INTERRUPTED) && isOpen());
            if (n == IOStatus.UNSUPPORTED_CASE) {
                if (target instanceof SinkChannelImpl) pipeSupported = false;
                if (target instanceof FileChannelImpl) fileSupported = false;
                return IOStatus.UNSUPPORTED_CASE;
            }
            if (n == IOStatus.UNSUPPORTED) {
                transferSupported = false;
                return IOStatus.UNSUPPORTED;
            }
            return IOStatus.normalize(n);
        } finally {
            threads.remove(ti);
            end(n > -1);
        }
    }

    private long transferToTrustedChannel(long position, int icount, WritableByteChannel target) throws IOException {
        if (!((target instanceof FileChannelImpl) || (target instanceof SelChImpl))) return IOStatus.UNSUPPORTED;
        MappedByteBuffer dbb = null;
        try {
            dbb = map(MapMode.READ_ONLY, position, icount);
            return target.write(dbb);
        } finally {
            if (dbb != null) unmap(dbb);
        }
    }

    private long transferToArbitraryChannel(long position, int icount, WritableByteChannel target) throws IOException {
        int c = Math.min(icount, TRANSFER_SIZE);
        ByteBuffer bb = Util.getTemporaryDirectBuffer(c);
        long tw = 0;
        long pos = position;
        try {
            Util.erase(bb);
            while (tw < icount) {
                bb.limit(Math.min((int) (icount - tw), TRANSFER_SIZE));
                int nr = read(bb, pos);
                if (nr <= 0) break;
                bb.flip();
                int nw = target.write(bb);
                tw += nw;
                if (nw != nr) break;
                pos += nw;
                bb.clear();
            }
            return tw;
        } catch (IOException x) {
            if (tw > 0) return tw;
            throw x;
        } finally {
            Util.releaseTemporaryDirectBuffer(bb);
        }
    }

    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        ensureOpen();
        if (!target.isOpen()) throw new ClosedChannelException();
        if (!readable) throw new NonReadableChannelException();
        if (target instanceof FileChannelImpl && !((FileChannelImpl) target).writable) throw new NonWritableChannelException();
        if ((position < 0) || (count < 0)) throw new IllegalArgumentException();
        long sz = size();
        if (position > sz) return 0;
        int icount = (int) Math.min(count, Integer.MAX_VALUE);
        if ((sz - position) < icount) icount = (int) (sz - position);
        long n;
        if ((n = transferToDirectly(position, icount, target)) >= 0) return n;
        if ((n = transferToTrustedChannel(position, icount, target)) >= 0) return n;
        return transferToArbitraryChannel(position, icount, target);
    }

    private long transferFromFileChannel(FileChannelImpl src, long position, long count) throws IOException {
        synchronized (src.positionLock) {
            long p = src.position();
            int icount = (int) Math.min(Math.min(count, Integer.MAX_VALUE), src.size() - p);
            MappedByteBuffer bb = src.map(MapMode.READ_ONLY, p, icount);
            try {
                long n = write(bb, position);
                src.position(p + n);
                return n;
            } finally {
                unmap(bb);
            }
        }
    }

    private static final int TRANSFER_SIZE = 8192;

    private long transferFromArbitraryChannel(ReadableByteChannel src, long position, long count) throws IOException {
        int c = (int) Math.min(count, TRANSFER_SIZE);
        ByteBuffer bb = Util.getTemporaryDirectBuffer(c);
        long tw = 0;
        long pos = position;
        try {
            Util.erase(bb);
            while (tw < count) {
                bb.limit((int) Math.min((count - tw), (long) TRANSFER_SIZE));
                int nr = src.read(bb);
                if (nr <= 0) break;
                bb.flip();
                int nw = write(bb, pos);
                tw += nw;
                if (nw != nr) break;
                pos += nw;
                bb.clear();
            }
            return tw;
        } catch (IOException x) {
            if (tw > 0) return tw;
            throw x;
        } finally {
            Util.releaseTemporaryDirectBuffer(bb);
        }
    }

    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        ensureOpen();
        if (!src.isOpen()) throw new ClosedChannelException();
        if (!writable) throw new NonWritableChannelException();
        if ((position < 0) || (count < 0)) throw new IllegalArgumentException();
        if (position > size()) return 0;
        if (src instanceof FileChannelImpl) return transferFromFileChannel((FileChannelImpl) src, position, count);
        return transferFromArbitraryChannel(src, position, count);
    }

    public int read(ByteBuffer dst, long position) throws IOException {
        if (dst == null) throw new NullPointerException();
        if (position < 0) throw new IllegalArgumentException("Negative position");
        if (!readable) throw new NonReadableChannelException();
        ensureOpen();
        int n = 0;
        int ti = -1;
        try {
            begin();
            ti = threads.add();
            if (!isOpen()) return -1;
            do {
                n = IOUtil.read(fd, dst, position, nd, positionLock);
            } while ((n == IOStatus.INTERRUPTED) && isOpen());
            return IOStatus.normalize(n);
        } finally {
            threads.remove(ti);
            end(n > 0);
            assert IOStatus.check(n);
        }
    }

    public int write(ByteBuffer src, long position) throws IOException {
        if (src == null) throw new NullPointerException();
        if (position < 0) throw new IllegalArgumentException("Negative position");
        if (!writable) throw new NonWritableChannelException();
        ensureOpen();
        int n = 0;
        int ti = -1;
        try {
            begin();
            ti = threads.add();
            if (!isOpen()) return -1;
            do {
                n = IOUtil.write(fd, src, position, nd, positionLock);
            } while ((n == IOStatus.INTERRUPTED) && isOpen());
            return IOStatus.normalize(n);
        } finally {
            threads.remove(ti);
            end(n > 0);
            assert IOStatus.check(n);
        }
    }

    private static class Unmapper implements Runnable {

        static volatile int count;

        static volatile long totalSize;

        static volatile long totalCapacity;

        private long address;

        private long size;

        private int cap;

        private Unmapper(long address, long size, int cap) {
            assert (address != 0);
            this.address = address;
            this.size = size;
            this.cap = cap;
            synchronized (Unmapper.class) {
                count++;
                totalSize += size;
                totalCapacity += cap;
            }
        }

        public void run() {
            if (address == 0) return;
            unmap0(address, size);
            address = 0;
            synchronized (Unmapper.class) {
                count--;
                totalSize -= size;
                totalCapacity -= cap;
            }
        }
    }

    private static void unmap(MappedByteBuffer bb) {
        Cleaner cl = ((DirectBuffer) bb).cleaner();
        if (cl != null) cl.clean();
    }

    private static final int MAP_RO = 0;

    private static final int MAP_RW = 1;

    private static final int MAP_PV = 2;

    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        ensureOpen();
        if (position < 0L) throw new IllegalArgumentException("Negative position");
        if (size < 0L) throw new IllegalArgumentException("Negative size");
        if (position + size < 0) throw new IllegalArgumentException("Position + size overflow");
        if (size > Integer.MAX_VALUE) throw new IllegalArgumentException("Size exceeds Integer.MAX_VALUE");
        int imode = -1;
        if (mode == MapMode.READ_ONLY) imode = MAP_RO; else if (mode == MapMode.READ_WRITE) imode = MAP_RW; else if (mode == MapMode.PRIVATE) imode = MAP_PV;
        assert (imode >= 0);
        if ((mode != MapMode.READ_ONLY) && !writable) throw new NonWritableChannelException();
        if (!readable) throw new NonReadableChannelException();
        long addr = -1;
        int ti = -1;
        try {
            begin();
            ti = threads.add();
            if (!isOpen()) return null;
            if (size() < position + size) {
                if (!writable) {
                    throw new IOException("Channel not open for writing " + "- cannot extend file to required size");
                }
                int rv;
                do {
                    rv = nd.truncate(fd, position + size);
                } while ((rv == IOStatus.INTERRUPTED) && isOpen());
            }
            if (size == 0) {
                addr = 0;
                if ((!writable) || (imode == MAP_RO)) return Util.newMappedByteBufferR(0, 0, null); else return Util.newMappedByteBuffer(0, 0, null);
            }
            int pagePosition = (int) (position % allocationGranularity);
            long mapPosition = position - pagePosition;
            long mapSize = size + pagePosition;
            try {
                addr = map0(imode, mapPosition, mapSize);
            } catch (OutOfMemoryError x) {
                System.gc();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException y) {
                    Thread.currentThread().interrupt();
                }
                try {
                    addr = map0(imode, mapPosition, mapSize);
                } catch (OutOfMemoryError y) {
                    throw new IOException("Map failed", y);
                }
            }
            assert (IOStatus.checkAll(addr));
            assert (addr % allocationGranularity == 0);
            int isize = (int) size;
            Unmapper um = new Unmapper(addr, size + pagePosition, isize);
            if ((!writable) || (imode == MAP_RO)) return Util.newMappedByteBufferR(isize, addr + pagePosition, um); else return Util.newMappedByteBuffer(isize, addr + pagePosition, um);
        } finally {
            threads.remove(ti);
            end(IOStatus.checkAll(addr));
        }
    }

    /**
     * Invoked by sun.management.ManagementFactoryHelper to create the management
     * interface for mapped buffers.
     */
    public static sun.misc.JavaNioAccess.BufferPool getMappedBufferPool() {
        return new sun.misc.JavaNioAccess.BufferPool() {

            @Override
            public String getName() {
                return "mapped";
            }

            @Override
            public long getCount() {
                return Unmapper.count;
            }

            @Override
            public long getTotalCapacity() {
                return Unmapper.totalCapacity;
            }

            @Override
            public long getMemoryUsed() {
                return Unmapper.totalSize;
            }
        };
    }

    private volatile FileLockTable fileLockTable;

    private static boolean isSharedFileLockTable;

    private static volatile boolean propertyChecked;

    private static boolean isSharedFileLockTable() {
        if (!propertyChecked) {
            synchronized (FileChannelImpl.class) {
                if (!propertyChecked) {
                    String value = AccessController.doPrivileged(new GetPropertyAction("sun.nio.ch.disableSystemWideOverlappingFileLockCheck"));
                    isSharedFileLockTable = ((value == null) || value.equals("false"));
                    propertyChecked = true;
                }
            }
        }
        return isSharedFileLockTable;
    }

    private FileLockTable fileLockTable() throws IOException {
        if (fileLockTable == null) {
            synchronized (this) {
                if (fileLockTable == null) {
                    if (isSharedFileLockTable()) {
                        int ti = threads.add();
                        try {
                            ensureOpen();
                            fileLockTable = FileLockTable.newSharedFileLockTable(this, fd);
                        } finally {
                            threads.remove(ti);
                        }
                    } else {
                        fileLockTable = new SimpleFileLockTable();
                    }
                }
            }
        }
        return fileLockTable;
    }

    public FileLock lock(long position, long size, boolean shared) throws IOException {
        ensureOpen();
        if (shared && !readable) throw new NonReadableChannelException();
        if (!shared && !writable) throw new NonWritableChannelException();
        FileLockImpl fli = new FileLockImpl(this, position, size, shared);
        FileLockTable flt = fileLockTable();
        flt.add(fli);
        boolean completed = false;
        int ti = -1;
        try {
            begin();
            ti = threads.add();
            if (!isOpen()) return null;
            int n;
            do {
                n = nd.lock(fd, true, position, size, shared);
            } while ((n == FileDispatcher.INTERRUPTED) && isOpen());
            if (isOpen()) {
                if (n == FileDispatcher.RET_EX_LOCK) {
                    assert shared;
                    FileLockImpl fli2 = new FileLockImpl(this, position, size, false);
                    flt.replace(fli, fli2);
                    fli = fli2;
                }
                completed = true;
            }
        } finally {
            if (!completed) flt.remove(fli);
            threads.remove(ti);
            try {
                end(completed);
            } catch (ClosedByInterruptException e) {
                throw new FileLockInterruptionException();
            }
        }
        return fli;
    }

    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        ensureOpen();
        if (shared && !readable) throw new NonReadableChannelException();
        if (!shared && !writable) throw new NonWritableChannelException();
        FileLockImpl fli = new FileLockImpl(this, position, size, shared);
        FileLockTable flt = fileLockTable();
        flt.add(fli);
        int result;
        int ti = threads.add();
        try {
            try {
                ensureOpen();
                result = nd.lock(fd, false, position, size, shared);
            } catch (IOException e) {
                flt.remove(fli);
                throw e;
            }
            if (result == FileDispatcher.NO_LOCK) {
                flt.remove(fli);
                return null;
            }
            if (result == FileDispatcher.RET_EX_LOCK) {
                assert shared;
                FileLockImpl fli2 = new FileLockImpl(this, position, size, false);
                flt.replace(fli, fli2);
                return fli2;
            }
            return fli;
        } finally {
            threads.remove(ti);
        }
    }

    void release(FileLockImpl fli) throws IOException {
        int ti = threads.add();
        try {
            ensureOpen();
            nd.release(fd, fli.position(), fli.size());
        } finally {
            threads.remove(ti);
        }
        assert fileLockTable != null;
        fileLockTable.remove(fli);
    }

    /**
     * A simple file lock table that maintains a list of FileLocks obtained by a
     * FileChannel. Use to get 1.4/5.0 behaviour.
     */
    private static class SimpleFileLockTable extends FileLockTable {

        private final List<FileLock> lockList = new ArrayList<FileLock>(2);

        public SimpleFileLockTable() {
        }

        private void checkList(long position, long size) throws OverlappingFileLockException {
            assert Thread.holdsLock(lockList);
            for (FileLock fl : lockList) {
                if (fl.overlaps(position, size)) {
                    throw new OverlappingFileLockException();
                }
            }
        }

        public void add(FileLock fl) throws OverlappingFileLockException {
            synchronized (lockList) {
                checkList(fl.position(), fl.size());
                lockList.add(fl);
            }
        }

        public void remove(FileLock fl) {
            synchronized (lockList) {
                lockList.remove(fl);
            }
        }

        public List<FileLock> removeAll() {
            synchronized (lockList) {
                List<FileLock> result = new ArrayList<FileLock>(lockList);
                lockList.clear();
                return result;
            }
        }

        public void replace(FileLock fl1, FileLock fl2) {
            synchronized (lockList) {
                lockList.remove(fl1);
                lockList.add(fl2);
            }
        }
    }

    private native long map0(int prot, long position, long length) throws IOException;

    private static native int unmap0(long address, long length);

    private native long transferTo0(int src, long position, long count, int dst);

    private native long position0(FileDescriptor fd, long offset);

    private static native long initIDs();

    static {
        Util.load();
        allocationGranularity = initIDs();
        nd = new FileDispatcherImpl();
    }
}

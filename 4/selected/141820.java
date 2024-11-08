package xbird.util.nio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Map;
import xbird.config.Settings;
import xbird.server.services.RemotePagingService;
import xbird.util.collections.ObservableLRUMap;
import xbird.util.collections.ObservableLRUMap.Cleaner;
import xbird.util.concurrent.AtomicUtils;
import xbird.util.io.IOUtils;
import xbird.util.struct.MutableLongPair;

/**
 * 
 * <DIV lang="en"></DIV> <DIV lang="ja"></DIV>
 * 
 * @author Makoto YUI (yuin405+xbird@gmail.com)
 */
public final class MemoryMappedFile implements IMemoryMappedFile {

    private static final int MAX_MAPPED_BUFS = Integer.parseInt(Settings.get("xbird.database.mmap.max_mapped", "2048"));

    private final boolean _readOnly;

    private final boolean _setAsLittleEndian;

    private final int _pageSize;

    private final long _oid;

    private final transient String _filepath;

    private FileChannel _channel;

    private static final Map<MutableLongPair, CloseableMappedByteBuffer> _pool;

    static {
        final Cleaner<MutableLongPair, CloseableMappedByteBuffer> cleaner = new Cleaner<MutableLongPair, CloseableMappedByteBuffer>() {

            public void cleanup(MutableLongPair key, CloseableMappedByteBuffer reclaimed) {
                try {
                    reclaimed.close();
                } catch (IOException e) {
                    ;
                }
            }
        };
        _pool = new ObservableLRUMap<MutableLongPair, CloseableMappedByteBuffer>(MAX_MAPPED_BUFS, cleaner);
    }

    /**
     * @param pageSize should be greater than or equals to 64k.
     */
    public MemoryMappedFile(final File file, final int pageShift, final boolean readOnly, final boolean nativeByteOrder) throws FileNotFoundException {
        this._filepath = file.getAbsolutePath();
        RandomAccessFile raf = new RandomAccessFile(file, readOnly ? "r" : "rw");
        this._channel = raf.getChannel();
        this._readOnly = readOnly;
        this._setAsLittleEndian = nativeByteOrder && (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN);
        this._pageSize = 1 << pageShift;
        this._oid = System.identityHashCode(this);
    }

    public CloseableMappedByteBuffer allocateBuffer(final long pageOffset) {
        final MutableLongPair pair = new MutableLongPair(_oid, pageOffset);
        final Map<MutableLongPair, CloseableMappedByteBuffer> pool = _pool;
        CloseableMappedByteBuffer buf;
        synchronized (pool) {
            buf = pool.get(pair);
            if (buf == null || !AtomicUtils.tryIncrementIfGreaterThan(buf.referenceCount(), 0)) {
                MappedByteBuffer bufimpl = map(pageOffset);
                buf = new CloseableMappedByteBuffer(bufimpl);
                pool.put(pair, buf);
            }
        }
        return buf;
    }

    /**
     * @link http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4724038
     */
    private MappedByteBuffer map(final long page) {
        if (_channel == null) {
            ensureOpen();
        }
        MappedByteBuffer buf;
        try {
            buf = _channel.map(_readOnly ? MapMode.READ_ONLY : MapMode.READ_WRITE, page, _pageSize);
        } catch (IOException e) {
            System.gc();
            System.runFinalization();
            try {
                buf = _channel.map(_readOnly ? MapMode.READ_ONLY : MapMode.READ_WRITE, page, _pageSize);
            } catch (IOException ioe) {
                throw new IllegalStateException("map failed for page#" + page + " of file: " + _filepath, ioe);
            }
        }
        if (_setAsLittleEndian) {
            buf.order(ByteOrder.LITTLE_ENDIAN);
        }
        return buf;
    }

    public int[] transferBuffer(final long pageOffset, final int aryLength) {
        final int[] newPage = new int[aryLength];
        final CloseableMappedByteBuffer buf = allocateBuffer(pageOffset);
        final IntBuffer ibuf = buf.getBuffer().asIntBuffer();
        ibuf.get(newPage, 0, aryLength);
        IOUtils.closeQuietly(buf);
        return newPage;
    }

    public synchronized void flush() {
        _pool.clear();
    }

    public synchronized void close() throws IOException {
        _channel.close();
        this._channel = null;
    }

    public synchronized void ensureOpen() {
        if (_channel == null) {
            final RandomAccessFile raf;
            try {
                raf = new RandomAccessFile(_filepath, _readOnly ? "r" : "rw");
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("File not found: " + _filepath, e);
            }
            this._channel = raf.getChannel();
        }
    }

    public RemoteMemoryMappedFile externalize() {
        return new RemoteMemoryMappedFile(RemotePagingService.PORT, _filepath, _pageSize, false, !_setAsLittleEndian);
    }
}

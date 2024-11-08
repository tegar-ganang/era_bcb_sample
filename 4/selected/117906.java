package edu.ucsd.ncmir.spl.io;

import edu.sdsc.grid.io.local.LocalRandomAccessFile;
import edu.ucsd.ncmir.spl.core.SoftHashMap;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class RandomFileAccessor extends ChunkedAccessor {

    private FileChannel _channel;

    private long _file_size;

    private LocalRandomAccessFile _file;

    public RandomFileAccessor(LocalRandomAccessFile file) throws IOException {
        super(file.getFile());
        this._file = file;
        this._file_size = file.getFile().length();
        RandomAccessFile raf = file.getRandomAccessFile();
        this._channel = raf.getChannel();
    }

    private long _origin = 0;

    private int _length = 0;

    private SoftHashMap<Long, BufferCache> _buffer_table = new SoftHashMap<Long, BufferCache>();

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        ByteBuffer[] bb = { ByteBuffer.wrap(bytes) };
        this._channel.write(bb, offset, 1);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        this.write(bytes, 0, bytes.length);
    }

    public void seekReal(long offset) throws IOException {
        this._channel.position(offset);
    }

    private class BufferCache {

        private long _origin;

        private ByteBuffer _byte_buffer;

        BufferCache(long origin, ByteBuffer byte_buffer) {
            this._origin = origin;
            this._byte_buffer = byte_buffer;
        }

        @Override
        public boolean equals(Object o) {
            boolean equals = false;
            if (o instanceof BufferCache) {
                BufferCache bc = (BufferCache) o;
                equals = bc._origin == this._origin;
            }
            return equals;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + (int) (this._origin ^ (this._origin >>> 32));
            return hash;
        }

        Long getKey() {
            return new Long(this._origin);
        }

        ByteBuffer getBuffer() {
            return this._byte_buffer;
        }
    }

    private static final int MAX_BUFFER_SIZE = 1024 * 1024;

    private ByteBuffer _byte_buffer = null;

    @Override
    protected ByteBuffer getByteBuffer(long position) throws IOException {
        this._origin = position - (position % MAX_BUFFER_SIZE);
        if ((this._origin + MAX_BUFFER_SIZE) > this._file_size) this._length = (int) (this._file_size - this._origin); else this._length = MAX_BUFFER_SIZE;
        if (this._length > 0) {
            Long key = new Long(this._origin);
            BufferCache buffer_cache = this._buffer_table.get(key);
            if (buffer_cache == null) {
                this._byte_buffer = null;
                do {
                    try {
                        ByteBuffer bb = ByteBuffer.allocate(this._length);
                        this._channel.position(this._origin);
                        this._channel.read(bb);
                        bb.position(0);
                        buffer_cache = new BufferCache(this._origin, bb);
                        this._buffer_table.put(key, buffer_cache);
                    } catch (OutOfMemoryError e) {
                        this._buffer_table = new SoftHashMap<Long, BufferCache>();
                    }
                } while (buffer_cache == null);
            }
            if (buffer_cache != null) this._byte_buffer = buffer_cache.getBuffer();
        } else this._byte_buffer = null;
        return this._byte_buffer;
    }

    @Override
    protected long origin() {
        return this._origin;
    }

    @Override
    protected long size() {
        return this._length;
    }
}

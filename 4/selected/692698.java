package org.exist.util.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Cache implementation for CachingFilterInputStream
 * Backed by a Memory Mapped File
 *
 * @version 1.0
 *
 * @author Adam Retter <adam.retter@googlemail.com>
 */
public class MemoryMappedFileFilterInputStreamCache implements FilterInputStreamCache {

    private static final long DEFAULT_MEMORY_MAP_SIZE = 64 * 1024 * 1024;

    private final RandomAccessFile raf;

    private final FileChannel channel;

    private MappedByteBuffer buf;

    private File tempFile = null;

    private final long memoryMapSize = DEFAULT_MEMORY_MAP_SIZE;

    public MemoryMappedFileFilterInputStreamCache() throws IOException {
        this(null);
    }

    public MemoryMappedFileFilterInputStreamCache(File f) throws FileNotFoundException, IOException {
        if (f == null) {
            tempFile = File.createTempFile("MemoryMappedFileFilterInputStreamCache" + "_" + System.currentTimeMillis(), ".tmp");
            tempFile.deleteOnExit();
            f = tempFile;
        }
        this.raf = new RandomAccessFile(f, "rw");
        this.channel = raf.getChannel();
        this.buf = channel.map(FileChannel.MapMode.READ_WRITE, 0, getMemoryMapSize());
    }

    private long getMemoryMapSize() {
        return memoryMapSize;
    }

    private void increaseSize(long bytes) throws IOException {
        long factor = (bytes / getMemoryMapSize());
        if (factor == 0 || bytes % getMemoryMapSize() > 0) {
            factor++;
        }
        buf.force();
        int position = buf.position();
        buf = channel.map(FileChannel.MapMode.READ_WRITE, 0, buf.capacity() + (getMemoryMapSize() * factor));
        buf.position(position);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (buf.remaining() < len) {
            increaseSize(len - buf.remaining());
        }
        buf.put(b, off, len);
    }

    @Override
    public void write(int i) throws IOException {
        if (buf.remaining() < 1) {
            increaseSize(1);
        }
        buf.put((byte) i);
    }

    @Override
    public byte get(int off) throws IOException {
        if (off > buf.capacity()) {
            increaseSize(off - buf.capacity());
        }
        return buf.get(off);
    }

    @Override
    public int getLength() {
        return buf.capacity() - buf.remaining();
    }

    @Override
    public void copyTo(int cacheOffset, byte[] b, int off, int len) throws IOException {
        if (off + len > buf.capacity()) {
            increaseSize(off + len - buf.capacity());
        }
        final int position = buf.position();
        try {
            buf.position(cacheOffset);
            byte data[] = new byte[len];
            buf.get(data, 0, len);
            System.arraycopy(data, 0, b, off, len);
        } finally {
            buf.position(position);
        }
    }

    @Override
    public void invalidate() throws IOException {
        buf.force();
        channel.close();
        raf.close();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (tempFile != null) {
            boolean deletedFile = tempFile.delete();
        }
    }
}

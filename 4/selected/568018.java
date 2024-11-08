package org.bintrotter.afiles;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;

public class File {

    public final long streamAddress;

    public final long size;

    private RandomAccessFile raf;

    private java.io.File file;

    public File(java.io.File file, long streamAddress, long size) throws IOException {
        this.file = file;
        this.streamAddress = streamAddress;
        this.size = size;
        this.open();
    }

    public MappedByteBuffer mmap(long addr, long len) throws IOException {
        return raf.getChannel().map(MapMode.READ_WRITE, addr, len);
    }

    public void open() throws IOException {
        if (raf == null) raf = new RandomAccessFile(this.file, "rw");
        if (raf.length() != size) raf.setLength(size);
    }

    public void close() throws IOException {
        raf.close();
        raf = null;
    }
}

package vanilla.java.collections.impl;

import sun.nio.ch.DirectBuffer;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class MappedFileChannel {

    private final RandomAccessFile raf;

    private final ByteBuffer buffer;

    public MappedFileChannel(RandomAccessFile raf, int length) throws IOException {
        this.raf = raf;
        buffer = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, length);
    }

    public void flush() throws IOException {
        raf.getChannel().force(false);
    }

    public void close() throws IOException {
        ((DirectBuffer) buffer).cleaner().clean();
        raf.close();
    }

    public ByteBuffer acquire(int size) {
        buffer.limit(buffer.position() + size);
        ByteBuffer bb = buffer.slice();
        buffer.position(buffer.limit());
        assert bb.remaining() == size;
        return bb;
    }
}

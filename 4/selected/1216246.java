package gg.de.sbmp3.common;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created: 15.07.2004  19:55:42
 */
public class MemoryRandomAccessFile extends RandomAccessFile {

    private MappedByteBuffer buf;

    public MemoryRandomAccessFile(File file, String mode) throws IOException {
        super(file, "r");
        if (!mode.equals("r")) throw new RuntimeException("only read-only mode implemented");
        init();
    }

    public MemoryRandomAccessFile(String file, String mode) throws IOException {
        super(file, "r");
        if (!mode.equals("r")) throw new RuntimeException("only read-only mode implemented");
        init();
    }

    public void seek(long position) {
        buf.position((int) position);
    }

    public int skipBytes(int count) throws IOException {
        int wished = buf.position() + count;
        buf.position(buf.position() + count);
        if (buf.position() < wished) {
            return (int) (wished - this.getChannel().size());
        } else {
            return count;
        }
    }

    public int read(byte[] b) {
        int count = b.length;
        if (count >= buf.remaining()) {
            count = buf.remaining();
        }
        if (count == 0) return 0;
        buf.get(b, 0, (count - 1));
        return count;
    }

    private void init() throws IOException {
        buf = this.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, this.getChannel().size());
        if (!buf.isLoaded()) buf.load();
    }
}

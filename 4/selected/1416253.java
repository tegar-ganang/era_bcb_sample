package neembuu.vfs.readmanager.test;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import neembuu.vfs.readmanager.DownloadDataChannel;

/**
 *
 * @author Shashank Tulsyan
 */
public final class TestDownloadChannel implements DownloadDataChannel {

    private final FileChannel destFile;

    private volatile long write_position = 0;

    public TestDownloadChannel(String dest) throws IOException {
        destFile = new RandomAccessFile(dest, "rw").getChannel();
    }

    public final long amountDownloaded() {
        return write_position;
    }

    public void readDestFile(long read_position, ByteBuffer dest) throws IOException {
        synchronized (this) {
            destFile.position(read_position);
            destFile.read(dest);
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        synchronized (this) {
            destFile.position(write_position);
            int written = destFile.write(src);
            write_position += written;
            return written;
        }
    }

    @Override
    public boolean isOpen() {
        return destFile.isOpen();
    }

    @Override
    public void close() throws IOException {
        destFile.close();
    }
}

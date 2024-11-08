package net.sf.moviekebab.toolset;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Generates a file of given size on local filesystem.
 * Useful for load testing.
 *
 * @author Laurent Caillette
 */
public class BigFileGenerator {

    private final FileChannel fileChannel;

    private final FileOutputStream fileOutputStream;

    private final ByteBuffer byteBuffer;

    private final long crapSize;

    public BigFileGenerator(String filename, int bufferSize, long crapSize) throws IOException {
        this.crapSize = crapSize;
        final File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        fileOutputStream = new FileOutputStream(file);
        fileChannel = fileOutputStream.getChannel();
        byteBuffer = ByteBuffer.allocateDirect(bufferSize);
        for (int i = 0; i < byteBuffer.capacity(); i++) {
            byteBuffer.put(CRAP);
        }
        byteBuffer.rewind();
    }

    public long write() throws IOException {
        if (!fileChannel.isOpen()) {
            throw new IllegalStateException("Already written, can write only once");
        }
        final long bytesWritten = writeCrap();
        close();
        return bytesWritten;
    }

    private static final byte CRAP = 60;

    private long writeCrap() throws IOException {
        long bytesWritten = 0;
        for (long i = 0; i < crapSize / byteBuffer.limit(); i++) {
            byteBuffer.position(byteBuffer.limit());
            bytesWritten += flush();
        }
        byteBuffer.position((int) (crapSize % byteBuffer.limit()));
        bytesWritten += flush();
        return bytesWritten;
    }

    private long flush() throws IOException {
        byteBuffer.flip();
        long bytesWritten = fileChannel.write(byteBuffer);
        byteBuffer.clear();
        return bytesWritten;
    }

    private void close() throws IOException {
        fileChannel.force(true);
        fileChannel.close();
        fileOutputStream.close();
    }

    private static final int KILOBYTE = 1024;

    private static final int MEGABYTE = KILOBYTE * KILOBYTE;

    public static void main(String[] args) throws IOException {
        new BigFileGenerator("crap.bin", 1 * KILOBYTE, 2 * MEGABYTE).write();
    }
}

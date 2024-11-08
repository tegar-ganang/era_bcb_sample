package sk.tuke.ess.lib.generator.utilities;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Bc. Tomáš Sarlós
 */
public class RawFileNumberProvider implements INumberProvider {

    public static String RAW_FILE_PATH = "random-org-raw";

    private static final long MAX_MAPPED_BUFFER_SIZE = 8000;

    private long fileSize;

    private FileChannel fch;

    private MappedByteBuffer buffer;

    private long position = 0;

    public RawFileNumberProvider() throws FileNotFoundException, Exception {
        if (MAX_MAPPED_BUFFER_SIZE % 4 != 0) throw new InternalError("Veľkosť buffra musí byť deliteľná 4");
        File f = new File(RAW_FILE_PATH);
        if ((fileSize = f.length()) < 5) throw new Exception(String.format("Raw súbor '%s' musí mať aspoň 5 bajtov", RAW_FILE_PATH));
        fch = new FileInputStream(f.getAbsolutePath()).getChannel();
        mapNextBytes();
    }

    private void mapNextBytes() {
        long nextReadSize = fileSize - position;
        if (nextReadSize < 4) {
            position = 0;
            nextReadSize = MAX_MAPPED_BUFFER_SIZE;
        } else if (nextReadSize > MAX_MAPPED_BUFFER_SIZE) nextReadSize = MAX_MAPPED_BUFFER_SIZE; else nextReadSize -= nextReadSize % 4;
        buffer = null;
        try {
            buffer = fch.map(FileChannel.MapMode.READ_ONLY, position, nextReadSize);
            position += nextReadSize;
        } catch (IOException ex) {
            throw new InternalError(ex.getMessage());
        }
    }

    public int nextInt() throws NoRandomNumberException {
        if (!buffer.hasRemaining()) mapNextBytes();
        return buffer.getInt();
    }

    @Override
    protected void finalize() {
        if (this.fch != null) try {
            this.fch.close();
            this.fch = null;
        } catch (IOException iOException) {
        }
        buffer = null;
    }

    public void close() {
        finalize();
    }
}

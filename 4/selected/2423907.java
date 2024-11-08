package it.crs4.seal.recab;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.*;
import java.nio.MappedByteBuffer;
import java.nio.ByteBuffer;

/**
 * Access a packed sequence file.
 * This class implements memory-mapped access to a packed sequence
 * as in the .pac file produced by "bwa index".
 */
public class PackedSequence {

    protected File path;

    protected FileChannel channel;

    protected MappedByteBuffer mmap;

    public static final char[] BASE_INDEX = { 'A', 'C', 'G', 'T', 'N' };

    public PackedSequence() {
    }

    /**
	 * Load a packed sequence file.
	 * This method actually creates the memory map for the sequence file to
	 * be accessed.
	 */
    public void load(File pac) throws java.io.FileNotFoundException, java.io.IOException {
        path = pac;
        RandomAccessFile fileObject = null;
        try {
            fileObject = new RandomAccessFile(path, "r");
            channel = fileObject.getChannel();
            mmap = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileObject.length());
        } catch (NonReadableChannelException e) {
            System.err.println("BUG!! used unreadable channel to create mmap. Message: " + e);
            throw e;
        } catch (NonWritableChannelException e) {
            System.err.println("BUG!! Tried to create a writable mmap. Message: " + e);
            throw e;
        } catch (IllegalArgumentException e) {
            System.err.println("BUG!! Tried to create mmap with bad parameters. Message: " + e);
            throw e;
        } finally {
            if (fileObject != null) fileObject.close();
        }
    }

    /**
	 * Try to load the packed sequence fully into memory.
	 */
    public void preload() {
        mmap.load();
    }

    /**
	 * Close the sequence file.
	 */
    public void close() throws java.io.IOException {
        if (channel != null) channel.close();
    }

    /**
	 * Read and unpack a section of sequence len bases long and starting at start (0-based).
	 * The unpacked sequence (in byte values 0-3).
	 */
    public void readSection(ByteBuffer dest, long start, int len) {
        if (mmap == null) throw new IllegalStateException("PacReference.readSection called before loading a reference");
        if (start < 0) throw new IllegalArgumentException("start value must be positive");
        if (len <= 0) throw new IllegalArgumentException("len must be > 0");
        mmap.position((int) (start / 4));
        int offset = (int) (start % 4);
        if (offset > 0) {
            BytePacking.unpackByte(dest, mmap.get(), offset, 3);
            len -= (4 - offset);
        }
        int bytesToRead = len / 4;
        for (int i = bytesToRead; i > 0; --i) BytePacking.unpackByte(dest, mmap.get(), 0, 3);
        len -= bytesToRead * 4;
        if (len > 0) {
            BytePacking.unpackByte(dest, mmap.get(), 0, len - 1);
        }
    }

    /**
	 * Convert a ByteBuffer of DNA base bytes (in the range 0-4) to an ASCII string.
	 * Reads from bytes.position() to bytes.limit() (not inclusive)
	 */
    public static String bytesToBases(ByteBuffer bytes) {
        StringBuilder builder = new StringBuilder(bytes.limit());
        for (int i = bytes.limit(); i > 0; --i) builder.append(BASE_INDEX[bytes.get()]);
        return builder.toString();
    }
}

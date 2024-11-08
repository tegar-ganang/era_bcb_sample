package dovetaildb.fileaccessor;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

public final class FileOfLongs {

    RandomAccessFile raf;

    FileChannel channel;

    MappedByteBuffer bb;

    LongBuffer lb;

    long numRecs, fileLength;

    private long minLongIncrement = 10;

    private long maxLongIncrement = 1000;

    /***
	 * Write access must be synchronized externally
	 * @param f
	 */
    public FileOfLongs(File f) {
        try {
            raf = new RandomAccessFile(f, "rw");
            fileLength = raf.length();
            fileLength = fileLength - 8;
            raf.seek(fileLength);
            numRecs = raf.readLong();
            reopen();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setNumRecs(long numLongs) {
        try {
            if (numLongs < fileLength) {
                long delta = (long) (numLongs * 8 * 0.10);
                if (delta > maxLongIncrement) delta = maxLongIncrement; else if (delta < minLongIncrement) delta = minLongIncrement;
                fileLength = numLongs + delta;
                raf.setLength(fileLength * 8 + 8);
                raf.seek(fileLength);
                raf.writeLong(fileLength);
            }
            numRecs = numLongs;
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    private void reopen() throws IOException {
        channel = raf.getChannel();
        bb = channel.map(MapMode.READ_WRITE, 0, fileLength);
        lb = bb.asLongBuffer();
    }

    public LongBuffer getLongBuffer() {
        return lb;
    }

    public long get(int i) {
        return lb.get(i);
    }

    public void set(int i, long val) {
        lb.put(i, val);
    }

    public void insertAt(int insertionIndex, long val) {
        numRecs++;
        for (int i = (int) numRecs; i > insertionIndex; i--) {
            lb.put(i, lb.get(i - 1));
        }
        lb.put(insertionIndex, val);
    }

    public void close() {
        try {
            bb.force();
            channel.close();
            bb = null;
            lb = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public long getNumRecs() {
        return numRecs;
    }
}

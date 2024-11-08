package dovetaildb.fileaccessor;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

public class MappedByteBufferAccessor implements FileAccessor {

    RandomAccessFile raf;

    FileChannel channel;

    MappedByteBuffer buffer;

    LongBuffer lb;

    IntBuffer ib;

    public MappedByteBufferAccessor(File f) {
        try {
            raf = new RandomAccessFile(f, "rw");
            channel = raf.getChannel();
            reopen();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reopen() {
        try {
            buffer = channel.map(MapMode.READ_WRITE, 0, raf.length());
            lb = buffer.asLongBuffer();
            ib = buffer.asIntBuffer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public long[] getLongs(long startPositionAsLong, long endPositionAsLong) {
        int startPosition = (int) startPositionAsLong;
        int endPosition = (int) endPositionAsLong;
        int byteSize = endPosition - startPosition;
        int longSize = (byteSize % 8 == 0) ? byteSize / 8 : ((byteSize / 8) + 1);
        long[] data = new long[longSize];
        for (int i = longSize - 1; i >= 0; i--) {
            data[i] = buffer.getLong(startPosition + i * 8);
        }
        return data;
    }

    public int cmp(long[] a, int aLength, long startPositionAsLong, long endPositionAsLong) {
        int startPosition = (int) startPositionAsLong;
        int endPosition = (int) endPositionAsLong;
        int bLength = endPosition - startPosition;
        int minLength = (aLength > bLength) ? bLength : aLength;
        int minLengthInLongsMinusOne = (minLength / 8) - 1;
        for (int i = 0; i < minLengthInLongsMinusOne; i++) {
            long av = a[i];
            long bv = buffer.getLong(startPosition + i * 8);
            if (av < bv) return -1; else if (av > bv) return 1;
        }
        int numBytesToZero = 8 - (minLength % 8);
        long mask = -1L << numBytesToZero;
        long av = a[minLengthInLongsMinusOne] & mask;
        long bv = buffer.getLong(startPosition + minLengthInLongsMinusOne * 8) & mask;
        if (av < bv) return -1; else if (av > bv) return 1; else return aLength - bLength;
    }

    public long getLong(long longPos) {
        return lb.get((int) longPos);
    }

    public long getUint(long longPos) {
        return ib.get((int) longPos) & 0xFFFFFFFF;
    }

    public void setLength(long newLength) {
        try {
            raf.setLength(newLength);
            reopen();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

package dovetaildb.fileaccessor;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

public final class BitFieldFile {

    RandomAccessFile raf;

    FileChannel channel;

    MappedByteBuffer bb;

    int fileLength;

    /***
	 * Write access must be synchronized externally
	 * @param f
	 */
    public BitFieldFile(File f) {
        try {
            raf = new RandomAccessFile(f, "rw");
            fileLength = (int) raf.length();
            channel = raf.getChannel();
            reopen();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean get(int i) {
        int index = i >> 3;
        if (index >= fileLength) return false;
        byte b = bb.get(index);
        int mask = 1 << (i & 0x07);
        return (b & mask) != 0;
    }

    public void setOn(long i) {
        try {
            int index = (int) (i >> 3);
            if (index >= fileLength) {
                int newFileLength = index + 1;
                raf.setLength(newFileLength);
                reopen();
                for (int pos = fileLength; pos < newFileLength; pos++) {
                    bb.put(pos, (byte) 0);
                }
                fileLength = newFileLength;
            }
            byte b = bb.get(index);
            int mask = 1 << (i & 0x07);
            b |= mask;
            bb.put(index, b);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setOff(long i) {
        try {
            int index = (int) (i >> 3);
            if (index >= fileLength) {
                fileLength = index + 1;
                raf.setLength(fileLength);
                reopen();
            }
            byte b = bb.get(index);
            int mask = 1 << (i & 0x07);
            b &= ~mask;
            bb.put(index, b);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getNextSet(int i) {
        while (!get(i)) {
            i++;
            if (i >= fileLength) return -1;
        }
        return i;
    }

    private void reopen() throws IOException {
        channel.position(0);
        if (bb != null) bb.force();
        bb = channel.map(MapMode.READ_WRITE, 0, fileLength);
    }

    public void close() {
        try {
            bb.force();
            channel.close();
            bb = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

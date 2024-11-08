package dovetaildb.fileaccessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

public class MappedFile {

    File file;

    RandomAccessFile raf;

    FileChannel channel;

    ByteBuffer bb;

    IntBuffer ib;

    LongBuffer lb;

    long applicationFileLength;

    float autoExtendPercent = 0.10f;

    int minExtendBytes = 1024;

    int maxExtendBytes = 50 * 1024 * 1024;

    public MappedFile(File file) {
        this.file = file;
        try {
            boolean existing = file.exists();
            raf = new RandomAccessFile(file, "rw");
            channel = raf.getChannel();
            if ((!existing) || raf.length() == 0) {
                raf.setLength(minExtendBytes + 8);
                raf.seek(minExtendBytes);
                applicationFileLength = 0;
                raf.writeLong(applicationFileLength);
            } else {
                raf.seek(raf.length() - 8);
                applicationFileLength = raf.readLong();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setLength(long length) {
        try {
            if (length > raf.length() - 8) {
                int incr = (int) (length * autoExtendPercent);
                if (incr < minExtendBytes) incr = minExtendBytes; else if (incr > maxExtendBytes) incr = maxExtendBytes;
                raf.setLength(length + incr + 8);
            }
            raf.seek(raf.length() - 8);
            raf.writeLong(length);
            openFromRaf();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public long getLength() {
        return applicationFileLength;
    }

    public void openFromRaf() {
        try {
            channel.position(0);
            bb = channel.map(MapMode.READ_WRITE, 0, raf.length());
            ib = bb.asIntBuffer();
            lb = bb.asLongBuffer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public long getUint(long position) {
        return ib.get((int) position) & 0xffffffffl;
    }

    public void putUint(long position, long data) {
        ib.put((int) position, (int) data);
    }

    public void putBytes(long position, byte[] data) {
        bb.put(data, (int) position, data.length);
    }

    public void putBytes(long position, byte[] data, int offset, int length) {
        ByteBuffer bbc = bb.slice();
        bbc.position((int) position);
        bbc.put(data, offset, length);
    }

    public void getBytes(long start, byte[] bytes) {
        bb.get(bytes, (int) start, bytes.length);
    }

    public void close() {
        try {
            ib = null;
            lb = null;
            bb = null;
            channel = null;
            raf.close();
            raf = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void write(long docId, int pageNum, int pageSize) {
    }

    public static final class VintReader {

        int bytePosition, topPosition;

        long curValue;

        int pageSize, nextPage;

        ByteBuffer bb;

        public VintReader(int pageNum, int pageSize, ByteBuffer bb) {
            this.bb = bb;
            this.pageSize = pageSize;
            startOnPage(pageNum);
        }

        public void startOnPage(int page) {
            int p = page * pageSize;
            nextPage = bb.getInt(p);
            bytePosition = p + bb.getInt(p + 4);
            topPosition = p + 8;
        }

        private long readVint() {
            byte b = bb.get(bytePosition--);
            long cur = 0;
            while (b > 0) {
                cur = (cur << 7) | b;
                b = bb.get(bytePosition--);
            }
            cur = (cur << 7) | (b & 0x7f);
            return cur;
        }

        public boolean next() {
            if (bytePosition <= topPosition) {
                if (nextPage == 0) return false;
                startOnPage(nextPage);
                return next();
            }
            curValue = readVint();
            return true;
        }
    }

    public int cmp(byte[] data, long start, long end) {
        int len = (int) (end - start);
        len = (data.length < len) ? data.length : len;
        for (int i = 0; i < len; i++) {
            int cmp = data[i] - bb.get(i);
            if (cmp != 0) return cmp;
        }
        return data.length - len;
    }
}

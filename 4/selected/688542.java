package dovetaildb.fileaccessor;

import gnu.trove.TLongLongHashMap;
import gnu.trove.TLongLongProcedure;
import gnu.trove.TLongProcedure;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import dovetaildb.store.BytesInterface;
import dovetaildb.store.ChunkedMemoryMappedFile;
import dovetaildb.store.VarPosition;

public final class PagedFile {

    public static final int PAGE_SIZE = 2048;

    RandomAccessFile raf;

    FileChannel channel;

    BytesInterface bi;

    int numPages, usedPages;

    float growPercent = 0.1f;

    /***
	 * Write access must be synchronized externally
	 * @param f
	 */
    public PagedFile(File f) {
        try {
            boolean exists = f.exists();
            raf = new RandomAccessFile(f, "rw");
            if (!exists) {
                raf.setLength(PAGE_SIZE + 4);
                raf.seek(PAGE_SIZE);
                raf.writeInt(0);
            }
            int fileLength = (int) raf.length() - 4;
            raf.seek(fileLength);
            usedPages = raf.readInt();
            numPages = (int) (fileLength / PAGE_SIZE);
            reopen();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void reopen() throws IOException {
        channel = raf.getChannel();
        bi = ChunkedMemoryMappedFile.mapFile(channel);
    }

    public void close() {
        try {
            channel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void freeLastPages(int numPagesToFree) {
        try {
            usedPages -= numPagesToFree;
            raf.seek(numPages * PAGE_SIZE);
            raf.writeInt(usedPages);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void copyPage(int from, int to) {
        long offsetFrom = getByteOffestForPage(from);
        long offsetTo = getByteOffestForPage(to);
        byte[] bytes = new byte[PAGE_SIZE];
        bi.getBytes(offsetFrom, PAGE_SIZE, bytes, 0);
        bi.putBytes(offsetTo, PAGE_SIZE, bytes, 0);
    }

    public int newPageIndex() {
        try {
            if (usedPages >= numPages) {
                channel.close();
                numPages += 10 + (int) (numPages * growPercent);
                raf.setLength(numPages * PAGE_SIZE + 4);
                reopen();
            }
            usedPages++;
            raf.seek(numPages * PAGE_SIZE);
            raf.writeInt(usedPages);
            return usedPages - 1;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public long getIntOffestForPage(int page) {
        return page * (PAGE_SIZE / 4);
    }

    public long getByteOffestForPage(int page) {
        return page * PAGE_SIZE;
    }

    public long getLongOffestForPage(int page) {
        return page * (PAGE_SIZE / 8);
    }

    public BytesInterface getBytesInterface() {
        return bi;
    }

    public int cmp(long position, int length, byte[] compareTo) {
        return bi.cmp(position, length, compareTo);
    }
}

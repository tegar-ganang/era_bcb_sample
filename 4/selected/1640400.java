package ao.util.persist;

import ao.util.math.Calc;
import java.io.*;
import java.nio.channels.FileChannel;

/**
 *
 */
public class PersistentInts {

    private static final int N_BYTE = Integer.SIZE / 8;

    public static void main(String[] args) {
    }

    private PersistentInts() {
    }

    public static int[] retrieve(String fromFile) {
        return retrieve(new File(fromFile));
    }

    public static int[] retrieve(File fromFile) {
        try {
            if (fromFile == null || !fromFile.canRead()) return null;
            return (fromFile.length() < 1024 * 1024) ? doRetrieve(fromFile) : doRetrieveMmap(fromFile);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private static int[] doRetrieveMmap(File cacheFile) throws Exception {
        int[] cached = new int[(int) (cacheFile.length() / N_BYTE)];
        FileInputStream f = new FileInputStream(cacheFile);
        try {
            FileChannel ch = f.getChannel();
            int offset = Mmap.ints(cached, 0, ch);
            while (offset < cached.length) {
                offset = Mmap.ints(cached, offset, ch);
            }
            ch.close();
        } finally {
            f.close();
        }
        return cached;
    }

    private static int[] doRetrieve(File cacheFile) throws Exception {
        int[] cached = new int[(int) (cacheFile.length() / N_BYTE)];
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(cacheFile));
        try {
            byte[] buffer = new byte[(int) cacheFile.length()];
            if (in.read(buffer) != buffer.length) {
                throw new Error("PersistentInts did not read expected size");
            }
            for (int i = 0; i < cached.length; i++) {
                cached[i] = Calc.toInt(buffer, i * N_BYTE);
            }
        } finally {
            in.close();
        }
        return cached;
    }

    public static void persist(int vals[], String fileName) {
        persist(vals, new File(fileName));
    }

    public static void persist(int vals[], File toFile) {
        try {
            doPersist(vals, toFile);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private static void doPersist(int vals[], File cacheFile) throws Exception {
        cacheFile.createNewFile();
        DataOutputStream cache = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(cacheFile)));
        for (int val : vals) {
            cache.writeInt(val);
        }
        cache.close();
    }
}

package ao.util.persist;

import ao.util.math.rand.Rand;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.Arrays;

/**
 * User: alex
 * Date: 31-Mar-2009
 * Time: 1:21:29 PM
 */
public class PersistentDoubles {

    private PersistentDoubles() {
    }

    public static void main(String[] args) {
        String file = "test/grid-5x5.double";
        double[][] grid = new double[5][5];
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                grid[i][j] = Rand.nextDouble(Long.MIN_VALUE, Long.MAX_VALUE);
                System.out.println(Long.toBinaryString(Double.doubleToLongBits(grid[i][j])));
            }
        }
        persist(grid, file);
        System.out.println(Arrays.deepToString(grid));
        double[][] gridB = new double[5][5];
        retrieve(file, gridB);
        System.out.println(Arrays.deepEquals(grid, gridB));
    }

    public static void retrieve(String fromFile, double rowsColumns[][]) {
        retrieve(new File(fromFile), rowsColumns);
    }

    public static void retrieve(File fromFile, double rowsColumns[][]) {
        try {
            doRetrieve(fromFile, rowsColumns);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void doRetrieve(File fromFile, double rowsColumns[][]) throws IOException {
        if (!fromFile.canRead()) return;
        FileInputStream f = new FileInputStream(fromFile);
        try {
            FileChannel ch = f.getChannel();
            long offset = 0;
            for (double[] row : rowsColumns) {
                Mmap.doubles(row, 0, offset, row.length, ch);
                offset += row.length * 8;
            }
        } finally {
            f.close();
        }
    }

    public static double[] retrieve(String fromFile) {
        return retrieve(new File(fromFile));
    }

    public static double[] retrieve(File fromFile) {
        try {
            return doRetrieve(fromFile);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private static double[] doRetrieve(File cacheFile) throws Exception {
        if (!cacheFile.canRead()) return null;
        double[] cached = new double[(int) (cacheFile.length() / 8)];
        FileInputStream f = new FileInputStream(cacheFile);
        try {
            FileChannel ch = f.getChannel();
            int offset = Mmap.doubles(cached, 0, ch);
            while (offset < cached.length) {
                offset = Mmap.doubles(cached, offset, ch);
            }
        } finally {
            f.close();
        }
        return cached;
    }

    public static void persist(double vals[][], String fileName) {
        persist(vals, new File(fileName));
    }

    public static void persist(double vals[][], File toFile) {
        try {
            doPersist(vals, toFile);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private static void doPersist(double vals[][], File cacheFile) throws Exception {
        cacheFile.createNewFile();
        DataOutputStream cache = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(cacheFile)));
        for (double[] col : vals) {
            for (double val : col) {
                cache.writeDouble(val);
            }
        }
        cache.close();
    }

    public static void persist(double vals[], String fileName) {
        persist(vals, new File(fileName));
    }

    public static void persist(double vals[], File toFile) {
        try {
            doPersist(vals, toFile);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private static void doPersist(double vals[], File cacheFile) throws Exception {
        cacheFile.createNewFile();
        DataOutputStream cache = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(cacheFile)));
        for (double val : vals) {
            cache.writeDouble(val);
        }
        cache.close();
    }
}

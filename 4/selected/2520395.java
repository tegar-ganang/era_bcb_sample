package streamcruncher.test.misc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.annotations.Test;
import streamcruncher.test.TestGroupNames;
import streamcruncher.util.AppendOnlyPrimitiveLongList;

public class AppendOnlyListTest extends Thread {

    private static enum THREAD_WORK_TYPE {

        read, readAvailable, write
    }

    private static final int TOTAL_ITEMS = (64 * 800) + 12;

    private AppendOnlyPrimitiveLongList list;

    private int totalItems;

    private THREAD_WORK_TYPE threadWorkType;

    private long[] results = new long[TOTAL_ITEMS];

    private PrintWriter writer;

    public AppendOnlyListTest() {
    }

    public AppendOnlyListTest(AppendOnlyPrimitiveLongList list, THREAD_WORK_TYPE threadWorkType, File out) throws IOException {
        this.list = list;
        this.threadWorkType = threadWorkType;
        writer = new PrintWriter(new BufferedOutputStream(new FileOutputStream(out)));
    }

    /**
     * @return Returns the results.
     */
    public long[] getResults() {
        return results;
    }

    @Override
    public void run() {
        switch(threadWorkType) {
            case read:
                read();
                break;
            case readAvailable:
                readAvailable();
                break;
            case write:
            default:
                writeAll();
                break;
        }
    }

    public void discard() {
        results = null;
        writer.flush();
        writer.close();
    }

    public void read() {
        while (totalItems < TOTAL_ITEMS) {
            long item = list.remove();
            writer.println(item);
            results[totalItems++] = item;
        }
        System.out.println("list size: " + list.getSize());
        System.out.println("Reading done!");
    }

    public void readAvailable() {
        while (totalItems < TOTAL_ITEMS) {
            long[] items = list.removeAvailable();
            for (int i = 0; i < items.length; i++) {
                if (i == items.length - 1) {
                    writer.print(items[i]);
                    writer.print(" -->");
                } else {
                    writer.println(items[i]);
                }
                results[totalItems++] = items[i];
            }
            if (items.length > 0) {
                writer.println(items.length);
            }
            if (TOTAL_ITEMS - totalItems <= 90) {
                try {
                    Thread.sleep(9000);
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
            }
        }
        System.out.println("Reading done!");
    }

    public void write() {
        Random random = new Random(System.currentTimeMillis());
        while (totalItems < TOTAL_ITEMS) {
            long item = random.nextLong();
            list.add(item);
            writer.println(item);
            results[totalItems++] = item;
            if (totalItems % 35 == 0) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
            }
        }
        System.out.println("Writing done!");
    }

    public void writeAll() {
        Random random = new Random(System.currentTimeMillis());
        while (totalItems < TOTAL_ITEMS) {
            long item = random.nextLong();
            writer.println(item);
            results[totalItems++] = item;
        }
        list.add(results);
        System.out.println("Writing done!");
    }

    public static void performTest(THREAD_WORK_TYPE readType) throws Exception {
        AppendOnlyPrimitiveLongList list = new AppendOnlyPrimitiveLongList();
        AppendOnlyListTest readerThread = new AppendOnlyListTest(list, readType, new File("tmp/read.txt"));
        AppendOnlyListTest writerThread = new AppendOnlyListTest(list, THREAD_WORK_TYPE.write, new File("tmp/write.txt"));
        readerThread.start();
        writerThread.start();
        readerThread.join();
        writerThread.join();
        long[] readResults = readerThread.getResults();
        long[] writeResults = writerThread.getResults();
        for (int i = 0; i < writeResults.length; i++) {
            long diff = readResults[i] - writeResults[i];
            Assert.assertEquals(diff, 0);
        }
        readerThread.discard();
        writerThread.discard();
    }

    @Test(groups = { TestGroupNames.SC_TEST })
    public static void testRead() throws Exception {
        double totalDiff = 0;
        int runs = 100;
        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            performTest(THREAD_WORK_TYPE.read);
            totalDiff = totalDiff + System.nanoTime() - start;
        }
        totalDiff = (totalDiff / runs);
        long time = TimeUnit.MILLISECONDS.convert((long) totalDiff, TimeUnit.NANOSECONDS);
        System.out.println("Read::::     Avg: " + time + " msecs over: " + runs + " each iteration: " + TOTAL_ITEMS);
    }

    @Test(groups = { TestGroupNames.SC_TEST })
    public static void testReadAvailable() throws Exception {
        double totalDiff = 0;
        int runs = 100;
        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            performTest(THREAD_WORK_TYPE.readAvailable);
            totalDiff = totalDiff + System.nanoTime() - start;
        }
        totalDiff = (totalDiff / runs);
        long time = TimeUnit.MILLISECONDS.convert((long) totalDiff, TimeUnit.NANOSECONDS);
        System.out.println("ReadAvailable::::     Avg: " + time + " msecs over: " + runs + " each iteration: " + TOTAL_ITEMS);
    }
}

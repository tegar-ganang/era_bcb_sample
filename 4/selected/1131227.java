package com.googlecode.jue.test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import com.googlecode.jue.util.ConcurrentLRUCache;
import com.googlecode.jue.util.ConcurrentLRUHashMap;
import com.googlecode.jue.util.LRUCache;

public class Performance {

    public static final int readCount = 10000;

    public static final int writeCount = 10000;

    public static final int readThreadCount = 10;

    public static final int writeThreadCount = 10;

    static ExecutorService readExec;

    static ExecutorService writeExec;

    static AtomicLong totalReadTime;

    static AtomicLong totalWriteTime;

    static AtomicLong totalReadCount;

    static AtomicLong totalWriteCount;

    static final long waitSecond = 20;

    static List<Long> timePerReadThread;

    static List<Long> timePerWriteThread;

    static ConcurrentLRUCache<String, String> cache;

    public static void main(String[] args) throws InterruptedException {
        init();
        start();
    }

    private static void init() {
        cache = new ConcurrentLRUCache<String, String>(1000);
        readExec = Executors.newCachedThreadPool();
        writeExec = Executors.newCachedThreadPool();
        totalReadTime = new AtomicLong();
        totalWriteTime = new AtomicLong();
        totalReadCount = new AtomicLong();
        totalWriteCount = new AtomicLong();
        timePerReadThread = new CopyOnWriteArrayList<Long>();
        timePerWriteThread = new CopyOnWriteArrayList<Long>();
    }

    private static void start() throws InterruptedException {
        loops();
        printResult();
    }

    private static void printResult() throws InterruptedException {
        readExec.shutdown();
        writeExec.shutdown();
        System.out.println("readThreadCount:" + readThreadCount);
        System.out.println("writeThreadCount:" + writeThreadCount);
        System.out.println();
        readExec.awaitTermination(waitSecond, TimeUnit.SECONDS);
        if (readExec.isShutdown()) {
            System.out.println("readCount:" + readCount);
        } else {
            System.out.println("readExec not shutDown");
        }
        System.out.println("totalReadTime:" + totalReadTime);
        double avgTimePerReadThread = (double) totalReadTime.longValue() / readThreadCount;
        System.out.println("avgTimePerReadThread:" + avgTimePerReadThread);
        System.out.println("readPerSecond:" + readCount / (avgTimePerReadThread / 1000));
        System.out.println("==============================");
        writeExec.awaitTermination(waitSecond, TimeUnit.SECONDS);
        if (writeExec.isShutdown()) {
            System.out.println("writeCount:" + writeCount);
        } else {
            System.out.println("writeExec not shutDown");
        }
        System.out.println("totalWriteTime:" + totalWriteTime);
        double avgTimePerWriteThread = (double) totalWriteTime.longValue() / writeThreadCount;
        System.out.println("avgTimePerWriteThread:" + avgTimePerWriteThread);
        System.out.println("writePerSecond:" + writeCount / (avgTimePerWriteThread / 1000));
        System.out.println("==============================");
        System.out.println("cache size:" + cache.size());
    }

    private static void loops() {
        for (int i = 0; i < readThreadCount; i++) {
            readExec.execute(new Runnable() {

                @Override
                public void run() {
                    final long start = System.currentTimeMillis();
                    Random r = new Random();
                    int c = 0;
                    for (int i = 0; i < readCount; ++i) {
                        int n = r.nextInt();
                        cache.get(String.valueOf(n));
                        ++c;
                    }
                    totalReadCount.addAndGet(c);
                    long escaped = System.currentTimeMillis() - start;
                    totalReadTime.addAndGet(escaped);
                    timePerReadThread.add(escaped);
                }
            });
        }
        for (int i = 0; i < writeThreadCount; i++) {
            writeExec.execute(new Runnable() {

                @Override
                public void run() {
                    final long start = System.currentTimeMillis();
                    Random r = new Random();
                    int c = 0;
                    for (int i = 0; i < writeCount; ++i) {
                        int n = r.nextInt();
                        String s = String.valueOf(n);
                        cache.put(s, s);
                        ++c;
                    }
                    totalWriteCount.addAndGet(c);
                    long escaped = System.currentTimeMillis() - start;
                    totalWriteTime.addAndGet(escaped);
                    timePerWriteThread.add(escaped);
                }
            });
        }
    }
}

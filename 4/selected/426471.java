package com.dynomedia.esearch.util.groupkeycache.demo;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.dynomedia.esearch.util.groupkeycache.GroupkeyCache;
import com.dynomedia.esearch.util.groupkeycache.KeyBuffer;
import com.dynomedia.esearch.util.groupkeycache.util.GkcConfig;

public class GkcMultithreadTest implements Runnable {

    private static final Log logger = LogFactory.getLog(GkcMultithreadTest.class);

    private static ExecutorService writeService = null;

    private static long requestSize = 10000000;

    private static AtomicLong requestCount = new AtomicLong(0);

    private static int printCount = 100000;

    private static int threadCount;

    private static long startTime;

    private static List<String> gkeys = null;

    private static List<String> gidstr = null;

    private static List<Long> gids = new ArrayList<Long>();

    static {
    }

    private static void init(int threadCount) {
        try {
            GkcConfig.getInstance().setGroupkeySize(1000);
            GroupkeyCache.start();
            logger.info("GkcMultithreadTest start load keys....");
            gkeys = FileUtils.readLines(new File("resources" + File.separatorChar + "gkeys.txt"), "UTF-8");
            gidstr = FileUtils.readLines(new File("resources" + File.separatorChar + "gids.txt"), "UTF-8");
            for (String id : gidstr) gids.add(Long.parseLong(id));
            logger.info("GkcMultithreadTest keys load complete,gids.size:" + gids.size() + " gkeys.size:" + gkeys.size());
            GkcMultithreadTest.threadCount = threadCount;
            writeService = Executors.newFixedThreadPool(threadCount);
            for (int i = 0; i < threadCount; i++) {
                writeService.submit(new GkcMultithreadTest());
            }
            startTime = System.currentTimeMillis();
        } catch (Exception e) {
            logger.error("GkcMultithreadTest init error!", e);
        }
    }

    public static void main(String[] args) {
        GkcMultithreadTest.init(10);
    }

    public void run() {
        logger.info("GkcMultithreadTest start send data to KeyBuffer by Thread:" + Thread.currentThread().getName());
        Random rand = new Random();
        for (int i = 1; i < requestSize + 1; i++) {
            if (i % printCount == 0) {
                System.out.println("[request by thread:" + Thread.currentThread().getName() + "]:" + NumberFormat.getInstance().format(i) + "\t[total request]:" + NumberFormat.getInstance().format(requestCount.addAndGet(printCount)) + "\t[freeMemory]:" + (((Runtime.getRuntime().freeMemory()) / 1024 / 1024)) + "M\t[KeyBuffer.size]:" + KeyBuffer.size());
            }
            KeyBuffer.add(gids.get(rand.nextInt(gids.size())), gkeys.get(rand.nextInt(gkeys.size())));
        }
        logger.info("GkcMultithreadTest request complete,requestSize:" + requestSize + " by " + Thread.currentThread().getName());
        if (requestCount.get() == requestSize * threadCount) {
            System.out.println("GkcMultithreadTest request complete,[timeElapse]:" + ((System.currentTimeMillis() - startTime) / 1000f) + " s\t[requestCount]:" + requestCount);
            System.out.println("please check samples.log");
        }
    }
}

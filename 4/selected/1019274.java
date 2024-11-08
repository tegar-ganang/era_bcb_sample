package com.continuent.tungsten.replicator.util;

import org.apache.log4j.Logger;
import junit.framework.TestCase;

/**
 * Test for AtomicIntervalGuard class. In additional to catching bugs in the
 * queue structure (this test found several) the last case gives an indication
 * of performance by simulating the usage of the AtomicIntervalGuard in the
 * replicator. We should be able to handle 30 threads simultaneously reporting
 * on 1M independent transactions within 10 seconds. Recent runs on Mac OS X
 * laptop with Intel Core 2 processor complete in about 9.7s. Any result over
 * 10s should be investigated.
 * 
 * @author <a href="mailto:robert.hodges@continuent.com">Robert Hodges</a>
 * @version 1.0
 */
public class TestAtomicIntervalGuard extends TestCase {

    private static Logger logger = Logger.getLogger(TestAtomicIntervalGuard.class);

    /**
     * Show that single threaded operation correctly inserts values.
     */
    public void testInitialInsert() throws Exception {
        AtomicIntervalGuard<String> ati = new AtomicIntervalGuard<String>(4);
        Integer[] t = { new Integer(0), new Integer(1), new Integer(2), new Integer(3) };
        ati.report(t[0], 2, 20, "2");
        assertEquals("Head #1", 2, ati.getLowSeqno());
        assertEquals("Tail #1", 2, ati.getHiSeqno());
        assertEquals("Head #1 -time", 20, ati.getLowTime());
        assertEquals("Tail #1 -time", 20, ati.getHiTime());
        assertEquals("Head #1 -datum", "2", ati.getLowDatum());
        assertEquals("Tail #1 -datum", "2", ati.getHiDatum());
        ati.validate();
        assertEquals("Interval #1", 0, ati.getInterval());
        ati.report(t[1], 1, 10, "1");
        assertEquals("Head #2", 1, ati.getLowSeqno());
        assertEquals("Tail #2", 2, ati.getHiSeqno());
        assertEquals("Head #2 -time", 10, ati.getLowTime());
        assertEquals("Tail #2 -time", 20, ati.getHiTime());
        assertEquals("Head #2 -datum", "1", ati.getLowDatum());
        assertEquals("Tail #2 -datum", "2", ati.getHiDatum());
        assertEquals("Interval #2", 10, ati.getInterval());
        ati.validate();
        ati.report(t[2], 4, 40);
        assertEquals("Head #3", 1, ati.getLowSeqno());
        assertEquals("Tail #3", 4, ati.getHiSeqno());
        assertEquals("Head #3 -time", 10, ati.getLowTime());
        assertEquals("Tail #3 -time", 40, ati.getHiTime());
        assertEquals("Head #3 -datum", "1", ati.getLowDatum());
        assertNull("Tail #3 -datum", ati.getHiDatum());
        assertEquals("Interval #3", 30, ati.getInterval());
        ati.validate();
        ati.report(t[3], 3, 30, "3");
        assertEquals("Head #4", 1, ati.getLowSeqno());
        assertEquals("Tail #4", 4, ati.getHiSeqno());
        assertEquals("Head #4 -time", 10, ati.getLowTime());
        assertEquals("Tail #4 -time", 40, ati.getHiTime());
        assertEquals("Head #4 -datum", "1", ati.getLowDatum());
        assertNull("Tail #4 -datum", ati.getHiDatum());
        assertEquals("Interval #4", 30, ati.getInterval());
        ati.validate();
    }

    /**
     * Show that single threaded operation correctly reorders values.
     */
    public void testReordering() throws Exception {
        AtomicIntervalGuard<String> ati = new AtomicIntervalGuard<String>(4);
        Integer[] t = { new Integer(0), new Integer(1), new Integer(2) };
        ati.report(t[0], 1, 10);
        ati.report(t[1], 2, 20);
        ati.report(t[2], 3, 30);
        ati.validate();
        ati.report(t[0], 8, 80);
        assertEquals("Head #1", 2, ati.getLowSeqno());
        assertEquals("Head #1 -time", 20, ati.getLowTime());
        assertEquals("Tail #1", 8, ati.getHiSeqno());
        assertEquals("Tail #1 -time", 80, ati.getHiTime());
        assertEquals("Interval #1", 60, ati.getInterval());
        ati.validate();
        ati.report(t[2], 7, 70);
        assertEquals("Head #2", 2, ati.getLowSeqno());
        assertEquals("Head #2 -time", 20, ati.getLowTime());
        assertEquals("Tail #2", 8, ati.getHiSeqno());
        assertEquals("Tail #2 -time", 80, ati.getHiTime());
        assertEquals("Interval #2", 60, ati.getInterval());
        ati.validate();
        ati.report(t[1], 6, 60);
        assertEquals("Head #3", 6, ati.getLowSeqno());
        assertEquals("Head #3 -time", 60, ati.getLowTime());
        assertEquals("Tail #3", 8, ati.getHiSeqno());
        assertEquals("Tail #3 -time", 80, ati.getHiTime());
        assertEquals("Interval #3", 20, ati.getInterval());
        ati.report(t[0], 9, 90);
        assertEquals("Head #4", 6, ati.getLowSeqno());
        assertEquals("Head #4 -time", 60, ati.getLowTime());
        assertEquals("Tail #4", 9, ati.getHiSeqno());
        assertEquals("Tail #4 -time", 90, ati.getHiTime());
        assertEquals("Interval #4", 30, ati.getInterval());
        ati.validate();
        ati.report(t[2], 10, 100);
        assertEquals("Head #5", 6, ati.getLowSeqno());
        assertEquals("Head #5 -time", 60, ati.getLowTime());
        assertEquals("Tail #5", 10, ati.getHiSeqno());
        assertEquals("Tail #5 -time", 100, ati.getHiTime());
        assertEquals("Interval #4", 40, ati.getInterval());
        ati.validate();
    }

    /**
     * Show that time intervals are correctly calculated and that we can wait
     * for them.
     */
    public void testSingleThreadedInterval() throws Exception {
        AtomicIntervalGuard<String> ati = new AtomicIntervalGuard<String>(3);
        Integer[] t = { new Integer(0), new Integer(1), new Integer(2) };
        long seqno = 0;
        long time = 0;
        for (int i = 0; i < 100000; i++) {
            for (int j = 0; j < 3; j++) {
                seqno++;
                time += 10;
                ati.report(t[j], seqno, time);
            }
            long minimumTime = Math.max(time - 20, 0);
            long tailTime = ati.waitMinTime(minimumTime);
            assertEquals("Tail time at iteration: " + i, minimumTime, tailTime);
        }
    }

    /**
     * Show that time intervals work correctly when a large number of threads
     * whose progress is gated by an atomic counter are simultaneously updating
     * the thread interval array.
     */
    public void testMultiThreadedInterval() throws Exception {
        AtomicIntervalGuard<String> ati = new AtomicIntervalGuard<String>(15);
        AtomicCounter counter = new AtomicCounter(0);
        SampleThreadIntervalWriter[] writer = new SampleThreadIntervalWriter[15];
        for (int i = 0; i < writer.length; i++) {
            writer[i] = new SampleThreadIntervalWriter(i, counter, ati, 500000);
            ati.report(i, 0, 0);
            writer[i].start();
        }
        long startMillis = System.currentTimeMillis();
        for (; ; ) {
            long seqno = counter.incrAndGetSeqno();
            if (seqno >= 500000) break;
            ati.waitMinTime(Math.max(seqno - 5000, 0));
            if (seqno % 50000 == 0) {
                double elapsed = (System.currentTimeMillis() - startMillis) / 1000.0;
                logger.info("Processed seqno=" + seqno + " elapsed=" + elapsed);
            }
        }
        double elapsed = (System.currentTimeMillis() - startMillis) / 1000.0;
        logger.info("Processed seqno=" + counter.getSeqno() + " elapsed=" + elapsed);
        for (int i = 0; i < writer.length; i++) {
            writer[i].join(60000);
            if (writer[i].throwable != null) {
                throw new Exception("Writer terminated abnormally: writer=" + i + " seqno=" + writer[i].seqno, writer[i].throwable);
            }
            if (!writer[i].done) {
                throw new Exception("Writer did not terminate: writer=" + i + " seqno=" + writer[i].seqno);
            }
            assertEquals("Checking writer[" + i + "] seqno", 500000, writer[i].seqno);
        }
    }
}

class SampleThreadIntervalWriter extends Thread {

    private int id;

    private AtomicCounter counter;

    private AtomicIntervalGuard<?> threadInterval;

    private long maxSeqno;

    volatile Throwable throwable;

    volatile long seqno;

    volatile boolean done;

    /**
     * Create new write with counter and maximum sequence number to execute to.
     * (Start at 0.)
     */
    SampleThreadIntervalWriter(int id, AtomicCounter counter, AtomicIntervalGuard<?> threadInterval, long maxSeqno) {
        this.id = id;
        this.counter = counter;
        this.maxSeqno = maxSeqno;
        this.threadInterval = threadInterval;
    }

    /**
     * Execute a loop to wait and report sequence numbers.
     */
    public void run() {
        seqno = 0;
        try {
            for (; ; ) {
                if (seqno >= maxSeqno) break;
                seqno++;
                counter.waitSeqnoGreaterEqual(seqno);
                threadInterval.report(id, seqno, seqno);
            }
        } catch (InterruptedException e) {
        } catch (Throwable t) {
            throwable = t;
        } finally {
            done = true;
        }
    }
}

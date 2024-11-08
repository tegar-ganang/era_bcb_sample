package de.snipworks.queue.persistentqueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;

/**
 * Unit test for LinkedBlockingPersistentQueue
 * @author Mario Hahnemann
 *
 */
public class LinkedBlockingPersistentQueueTest extends TestCase {

    private LinkedBlockingPersistentQueue<TestEntry> queue;

    private static final String TEST_FILENAME = "persist_queue.dat";

    private static final String FIRST = "first entry from offer";

    private static final int QUEUE_ELEMENTS = 100;

    /**
     * generate the queue file
     * @throws Exception on error in queue generation
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        queue = new LinkedBlockingPersistentQueue<TestEntry>(TEST_FILENAME);
    }

    private void putOne() {
        TestEntry entry = new TestEntry(FIRST);
        queue.offer(entry);
    }

    /**
     * clears the queue after each unit test
     * @throws Exception if something goes wrong while clearing
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        queue.clear();
    }

    /**
     * inserts one element only
     */
    public void testOfferQueueType() {
        putOne();
        TestEntry entry = new TestEntry(FIRST);
        assertTrue(queue.offer(entry));
    }

    /**
     * tests to get alle elements from queue into one collection
     * returns 2 elements with correct content
     *
     */
    public void testDrainToCollectionOfQsuperQueueType() {
        putOne();
        putOne();
        Collection<TestEntry> c = new ArrayList<TestEntry>();
        boolean result = false;
        int count = queue.drainTo(c, 4);
        if (c.iterator().next().getEntry().equals(FIRST)) {
            result = true;
        }
        assertTrue((count == 2) && result);
    }

    /**
     * this test must fail, beause it is impossible to store alle elements
     * from file into one queue
     */
    public void testDrainToCollection() {
        boolean exeptionThrown = false;
        Collection<TestEntry> c = new ArrayList<TestEntry>();
        try {
            queue.drainTo(c);
        } catch (UnsupportedOperationException e) {
            exeptionThrown = true;
        }
        assertTrue("Operation must not be supported", exeptionThrown);
    }

    /**
     * <li>one writer writes 100 entries to queue </li>
     * <li>one reader will read 100 entries from same queue </li>
     * <li>the reader will block if the queue is empty </li>
     * <li> the reader will never return with <b><i> null </i></b> </li>
     * <li> the reader has to read exactly 100 entries </li> 
     *
     */
    public void testReadWriteWithBlocks() {
        final AtomicBoolean result = new AtomicBoolean(true);
        class Writer implements Runnable {

            private TestEntry entry = new TestEntry(FIRST);

            private int count = QUEUE_ELEMENTS;

            public void run() {
                do {
                    try {
                        queue.put(entry);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    count--;
                } while (count > 0);
            }
        }
        class Reader implements Runnable {

            private TestEntry entry = null;

            private int count = QUEUE_ELEMENTS;

            public void run() {
                do {
                    try {
                        entry = queue.take();
                        if (entry == null) {
                            result.set(false);
                        }
                        if (!entry.getEntry().equals(FIRST)) {
                            result.set(false);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    count--;
                } while (count > 0);
            }
        }
        Thread writerThread = new Thread(new Writer());
        Thread readerThread = new Thread(new Reader());
        writerThread.start();
        readerThread.start();
        try {
            readerThread.join();
            writerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!queue.isEmpty()) {
            result.set(false);
        }
        assertTrue(result.get());
    }

    /**
     * <li>one writer writes 100 entries to queue </li>
     * <li>one reader will read 100 entries from same queue </li>
     * <li>the reader will block if the queue is empty </li>
     * <li> the reader will never return with <b><i> null </i></b> </li>
     * <li> the reader has to read exactly 100 entries </li> 
     * <li> reader and writer will handle timeouts </li>
     **/
    public void testReadWriteWithTimeout() {
        final AtomicBoolean result = new AtomicBoolean(true);
        class Writer implements Runnable {

            private TestEntry entry = new TestEntry(FIRST);

            private int count = QUEUE_ELEMENTS;

            public void run() {
                do {
                    try {
                        queue.offer(entry, 10, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    count--;
                } while (count > 0);
            }
        }
        class Reader implements Runnable {

            private TestEntry entry = null;

            private int count = QUEUE_ELEMENTS;

            public void run() {
                do {
                    try {
                        entry = queue.poll(10, TimeUnit.SECONDS);
                        if (entry == null) {
                            result.set(false);
                        }
                        if (!entry.getEntry().equals(FIRST)) {
                            result.set(false);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    count--;
                } while (count > 0);
            }
        }
        Thread writerThread = new Thread(new Writer());
        Thread readerThread = new Thread(new Reader());
        writerThread.start();
        readerThread.start();
        try {
            readerThread.join();
            writerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!queue.isEmpty()) {
            result.set(false);
        }
        assertTrue(result.get());
    }

    /**
     * test the if correct nimber of entries will return
     *
     */
    public void testRemainingCapacity() {
        boolean exeptionThrown = false;
        try {
            queue.remainingCapacity();
        } catch (UnsupportedOperationException e) {
            exeptionThrown = true;
        }
        assertTrue("Operation must not be supported", exeptionThrown);
    }
}

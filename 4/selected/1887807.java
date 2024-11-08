package org.crappydbms.dbfiles.locking;

import java.util.ArrayList;
import java.util.Iterator;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author Facundo Manuel Quiroga Dec 23, 2008
 * 
 */
public class PageLockTest extends TestCase {

    static int waitTime = 5;

    static int value;

    static int finishedThreads;

    public static synchronized void finished() {
        finishedThreads++;
    }

    static int failed;

    public static synchronized void failed() {
        failed++;
    }

    volatile PageLock generalPageLock;

    protected void setUp() throws Exception {
        super.setUp();
        generalPageLock = new PageLock();
        PageLockTest.value = 0;
        PageLockTest.failed = 0;
        PageLockTest.finishedThreads = 0;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testLockReaders() {
        this.testLock(100, 0, 0, false);
    }

    public void testLockWriters() {
        this.testLock(0, 100, 0, false);
    }

    public void testLockReadersWriters() {
        this.testLock(1, 1, 1, false);
    }

    public void testLockReadersWriters2() {
        this.testLock(100, 100, 1, false);
    }

    public void testLockReadersWriters3() {
        this.testLock(0, 0, 4, true);
    }

    public void testLockReadersAndWriters() {
        this.testLock(100, 100, 0, false);
    }

    public void testLock(int readersCount, int writersCount, int readerWritersCount, boolean expectingDeadlock) {
        int finalvalue = writersCount + readerWritersCount;
        int totalThreads = writersCount + readerWritersCount + readersCount;
        Assert.assertEquals(0, generalPageLock.getWriters());
        Assert.assertEquals(0, generalPageLock.getReaders());
        ArrayList<Reader> readers = new ArrayList<Reader>();
        ArrayList<Writer> writers = new ArrayList<Writer>();
        ArrayList<ReaderWriter> readerWriters = new ArrayList<ReaderWriter>();
        for (int i = 0; i < readersCount; i++) {
            readers.add(new Reader(generalPageLock, i));
        }
        for (int i = 0; i < writersCount; i++) {
            writers.add(new Writer(generalPageLock, i));
        }
        for (int i = 0; i < readerWritersCount; i++) {
            readerWriters.add(new ReaderWriter(generalPageLock, i, expectingDeadlock));
        }
        Iterator<Reader> readersIterator = readers.iterator();
        Iterator<ReaderWriter> readerWritersIterator = readerWriters.iterator();
        Iterator<Writer> writersIterator = writers.iterator();
        int entered = 0;
        for (int i = 0; entered < totalThreads; i++) {
            if (i % 3 == 0) {
                if (readersIterator.hasNext()) {
                    new Thread(readersIterator.next()).start();
                    entered++;
                }
            } else if (i % 3 == 1) {
                if (writersIterator.hasNext()) {
                    new Thread(writersIterator.next()).start();
                    entered++;
                }
            } else {
                if (readerWritersIterator.hasNext()) {
                    new Thread(readerWritersIterator.next()).start();
                    entered++;
                }
            }
        }
        while (PageLockTest.finishedThreads < totalThreads) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Assert.fail("should not be interrupted");
            }
        }
        Assert.assertEquals("wrong final value!", finalvalue, PageLockTest.value + PageLockTest.failed);
        Assert.assertEquals(0, generalPageLock.getWriters());
        Assert.assertEquals(0, generalPageLock.getReaders());
    }

    public abstract class MockPageLockUser implements Runnable {

        protected PageLock pageLock;

        protected int id;

        public MockPageLockUser(PageLock pageLock, int id) {
            this.pageLock = pageLock;
            this.id = id;
        }
    }

    public class Reader extends MockPageLockUser {

        public Reader(PageLock pageLock, int id) {
            super(pageLock, id);
        }

        @Override
        public void run() {
            int value;
            try {
                this.pageLock.getSharedLock(30000);
                value = PageLockTest.value;
                Thread.sleep(PageLockTest.waitTime);
                Assert.assertEquals("Reader " + id + ": value should not change while im holding the lock", value, PageLockTest.value);
                this.pageLock.releaseSharedLock();
            } catch (InterruptedException e) {
                Assert.fail("should not be interrupted");
            } catch (ExceededWaitTimeException e) {
                Assert.fail("should not exceed wait time" + e);
            } catch (Exception e) {
                Assert.fail("unknown error " + e.getMessage());
            }
            PageLockTest.finished();
        }
    }

    public class Writer extends MockPageLockUser {

        public Writer(PageLock pageLock, int id) {
            super(pageLock, id);
        }

        @Override
        public void run() {
            try {
                int value;
                try {
                    this.pageLock.getExclusiveLock(30000);
                    PageLockTest.value++;
                    value = PageLockTest.value;
                    Thread.sleep(PageLockTest.waitTime);
                    Assert.assertEquals("Value should not change while im holding the lock", value, PageLockTest.value);
                    this.pageLock.releaseExclusiveLock();
                } catch (InterruptedException e) {
                    Assert.fail("should not be interrupted");
                } catch (ExceededWaitTimeException e) {
                    Assert.fail("should not exceed wait time" + e);
                } catch (Exception e) {
                    Assert.fail("unknown error " + e.getMessage());
                }
            } finally {
                PageLockTest.finished();
            }
        }
    }

    public class ReaderWriter extends MockPageLockUser {

        protected boolean expectingDeadlock;

        public ReaderWriter(PageLock pageLock, int id, boolean expectingDeadlock) {
            super(pageLock, id);
            this.expectingDeadlock = expectingDeadlock;
        }

        @Override
        public void run() {
            try {
                int value;
                try {
                    this.pageLock.getSharedLock(30000);
                    value = PageLockTest.value;
                    Thread.sleep(300);
                    Assert.assertEquals("Value should not change while im holding a shared lock", value, PageLockTest.value);
                    this.pageLock.upgradeToExclusiveLock(30000);
                    Assert.assertEquals("Value should not change while im holding the lock", value, PageLockTest.value);
                    PageLockTest.value++;
                    value = PageLockTest.value;
                    Thread.sleep(PageLockTest.waitTime);
                    Assert.assertEquals("Value should not change while im holding the lock", value, PageLockTest.value);
                    this.pageLock.releaseExclusiveLock();
                } catch (InterruptedException e) {
                    Assert.fail("should not be interrupted");
                    PageLockTest.failed();
                } catch (UpgradeWouldCauseDeadlockException e) {
                    if (!this.expectingDeadlock) {
                        Assert.fail("Deadlock occurred in ReaderWriter id " + id);
                    } else {
                        this.pageLock.releaseSharedLock();
                        PageLockTest.failed();
                    }
                }
            } catch (ExceededWaitTimeException e) {
                Assert.fail("should not exceed wait time" + e);
            } catch (Exception e) {
                Assert.fail("unknown error " + e.getMessage());
            } finally {
                PageLockTest.finished();
            }
        }
    }
}

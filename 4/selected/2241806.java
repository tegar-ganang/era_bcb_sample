package org.enerj.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests MROWLock. <p>
 *
 * @version $Id: MROWLockTest.java,v 1.3 2005/08/12 02:56:46 dsyrstad Exp $
 * @author <a href="mailto:dsyrstad@ener-j.org">Dan Syrstad</a>
 */
public class MROWLockTest extends TestCase {

    public MROWLockTest(String aTestName) {
        super(aTestName);
    }

    public static Test suite() {
        return new TestSuite(MROWLockTest.class);
    }

    /**
     * Performs local-only read lock assertions on the given aLock.
     */
    private void assertOnlyLocallyReadLocked(MROWLock aLock) throws Exception {
        assertTrue(aLock.isLocked());
        assertTrue(aLock.isReadLocked());
        assertTrue(aLock.isLocallyLocked());
        assertTrue(aLock.isLocallyReadLocked());
        assertTrue(!aLock.isWriteLocked());
        assertTrue(!aLock.isLocallyWriteLocked());
        assertTrue(!aLock.isExternallyLocked());
        assertTrue(!aLock.isExternallyReadLocked());
        assertTrue(!aLock.isExternallyWriteLocked());
    }

    /**
     * Performs local-only write lock assertions on the given aLock.
     */
    private void assertOnlyLocallyWriteLocked(MROWLock aLock) throws Exception {
        assertTrue(aLock.isLocked());
        assertTrue(aLock.isLocallyLocked());
        assertTrue(aLock.isWriteLocked());
        assertTrue(aLock.isLocallyWriteLocked());
        assertTrue(!aLock.isReadLocked());
        assertTrue(!aLock.isLocallyReadLocked());
        assertTrue(!aLock.isExternallyLocked());
        assertTrue(!aLock.isExternallyReadLocked());
        assertTrue(!aLock.isExternallyWriteLocked());
    }

    /**
     * Performs local-only read and write lock assertions on the given aLock.
     */
    private void assertOnlyLocallyReadAndWriteLocked(MROWLock aLock) throws Exception {
        assertTrue(aLock.isLocked());
        assertTrue(aLock.isReadLocked());
        assertTrue(aLock.isWriteLocked());
        assertTrue(aLock.isLocallyLocked());
        assertTrue(aLock.isLocallyWriteLocked());
        assertTrue(aLock.isLocallyReadLocked());
        assertTrue(!aLock.isExternallyLocked());
        assertTrue(!aLock.isExternallyReadLocked());
        assertTrue(!aLock.isExternallyWriteLocked());
    }

    /**
     * Performs local read lock assertions on the given aLock. External locks are not checked.
     */
    private void assertLocallyReadLocked(MROWLock aLock) throws Exception {
        assertTrue(aLock.isLocked());
        assertTrue(aLock.isReadLocked());
        assertTrue(aLock.isLocallyLocked());
        assertTrue(aLock.isLocallyReadLocked());
        assertTrue(!aLock.isWriteLocked());
        assertTrue(!aLock.isLocallyWriteLocked());
    }

    /**
     * Performs local write lock assertions on the given aLock. External locks are not checked.
     */
    private void assertLocallyWriteLocked(MROWLock aLock) throws Exception {
        assertTrue(aLock.isLocked());
        assertTrue(aLock.isLocallyLocked());
        assertTrue(aLock.isWriteLocked());
        assertTrue(aLock.isLocallyWriteLocked());
        assertTrue(!aLock.isReadLocked());
        assertTrue(!aLock.isLocallyReadLocked());
    }

    /**
     * Performs local read and write lock assertions on the given aLock. External locks are not checked.
     */
    private void assertLocallyReadAndWriteLocked(MROWLock aLock) throws Exception {
        assertTrue(aLock.isLocked());
        assertTrue(aLock.isReadLocked());
        assertTrue(aLock.isWriteLocked());
        assertTrue(aLock.isLocallyLocked());
        assertTrue(aLock.isLocallyWriteLocked());
        assertTrue(aLock.isLocallyReadLocked());
    }

    /**
     * Performs external-only read lock assertions on the given aLock.
     */
    private void assertOnlyExternallyReadLocked(MROWLock aLock) throws Exception {
        assertTrue(aLock.isLocked());
        assertTrue(aLock.isReadLocked());
        assertTrue(!aLock.isWriteLocked());
        assertTrue(!aLock.isLocallyLocked());
        assertTrue(!aLock.isLocallyReadLocked());
        assertTrue(!aLock.isLocallyWriteLocked());
        assertTrue(aLock.isExternallyLocked());
        assertTrue(aLock.isExternallyReadLocked());
        assertTrue(!aLock.isExternallyWriteLocked());
    }

    /**
     * Performs external-only write lock assertions on the given aLock.
     */
    private void assertOnlyExternallyWriteLocked(MROWLock aLock) throws Exception {
        assertTrue(aLock.isLocked());
        assertTrue(!aLock.isReadLocked());
        assertTrue(aLock.isWriteLocked());
        assertTrue(!aLock.isLocallyLocked());
        assertTrue(!aLock.isLocallyReadLocked());
        assertTrue(!aLock.isLocallyWriteLocked());
        assertTrue(aLock.isExternallyLocked());
        assertTrue(!aLock.isExternallyReadLocked());
        assertTrue(aLock.isExternallyWriteLocked());
    }

    /**
     * Performs external-only read and write lock assertions on the given aLock.
     */
    private void assertOnlyExternallyReadAndWriteLocked(MROWLock aLock) throws Exception {
        assertTrue(aLock.isLocked());
        assertTrue(aLock.isReadLocked());
        assertTrue(aLock.isWriteLocked());
        assertTrue(!aLock.isLocallyLocked());
        assertTrue(!aLock.isLocallyReadLocked());
        assertTrue(!aLock.isLocallyWriteLocked());
        assertTrue(aLock.isExternallyLocked());
        assertTrue(aLock.isExternallyReadLocked());
        assertTrue(aLock.isExternallyWriteLocked());
    }

    /**
     * Asserts that a lock is not locked.
     */
    private void assertNotLocked(MROWLock aLock) throws Exception {
        assertTrue(!aLock.isLocked());
        assertTrue(!aLock.isReadLocked());
        assertTrue(!aLock.isWriteLocked());
        assertTrue(!aLock.isLocallyLocked());
        assertTrue(!aLock.isLocallyReadLocked());
        assertTrue(!aLock.isLocallyWriteLocked());
        assertTrue(!aLock.isExternallyLocked());
        assertTrue(!aLock.isExternallyReadLocked());
        assertTrue(!aLock.isExternallyWriteLocked());
    }

    /**
     * Asserts that no local locks are held. External locks are not checked.
     */
    private void assertNotLocallyLocked(MROWLock aLock) throws Exception {
        assertTrue(!aLock.isLocallyLocked());
        assertTrue(!aLock.isLocallyReadLocked());
        assertTrue(!aLock.isLocallyWriteLocked());
    }

    /**
     * Acquire read locks on multiple threads.
     */
    private ReaderThread[] acquireReadLocks(MROWLock aLock) throws Exception {
        ReaderThread[] threads = new ReaderThread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new ReaderThread(aLock);
            threads[i].start();
        }
        int acquireCount;
        System.out.println("Waiting for threads to acquire locks");
        do {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
            }
            acquireCount = 0;
            for (int i = 0; i < threads.length; i++) {
                if (threads[i].mAcquired) {
                    ++acquireCount;
                }
                if (threads[i].mException != null) {
                    throw threads[i].mException;
                }
            }
        } while (acquireCount != threads.length);
        System.out.println("Acquired.");
        return threads;
    }

    /** 
     * Release and terminate read locking threads created by acquireReadLocks.
     */
    private void releaseReadLocks(ReaderThread[] someThreads) throws Exception {
        for (int i = 0; i < someThreads.length; i++) {
            someThreads[i].mRelease = true;
        }
        System.out.println("Waiting for release.");
        for (int i = 0; i < someThreads.length; i++) {
            someThreads[i].join();
            if (someThreads[i].mException != null) {
                throw someThreads[i].mException;
            }
        }
        System.out.println("Released.");
    }

    /**
     * Tests that read locks are reentrant and "is" methods work properly for read locks.
     */
    public void testReentrantRead() throws Exception {
        MROWLock lock = new MROWLock();
        assertNotLocked(lock);
        lock.acquireRead();
        assertOnlyLocallyReadLocked(lock);
        lock.releaseRead();
        assertNotLocked(lock);
        assertNotLocked(lock);
        lock.acquireRead();
        assertOnlyLocallyReadLocked(lock);
        lock.acquireRead();
        assertOnlyLocallyReadLocked(lock);
        lock.releaseRead();
        assertOnlyLocallyReadLocked(lock);
        lock.releaseRead();
        assertNotLocked(lock);
        assertNotLocked(lock);
        lock.acquireRead();
        assertOnlyLocallyReadLocked(lock);
        lock.acquireRead();
        assertOnlyLocallyReadLocked(lock);
        lock.acquireRead();
        assertOnlyLocallyReadLocked(lock);
        lock.releaseRead();
        assertOnlyLocallyReadLocked(lock);
        lock.releaseRead();
        assertOnlyLocallyReadLocked(lock);
        lock.releaseRead();
        assertNotLocked(lock);
        assertNotLocked(lock);
        try {
            lock.releaseRead();
            fail("Should have thrown a RuntimeException");
        } catch (RuntimeException e) {
        }
    }

    /**
     * Tests that write locks are reentrant and "is" methods work properly for write locks.
     */
    public void testReentrantWrite() throws Exception {
        MROWLock lock = new MROWLock();
        assertNotLocked(lock);
        lock.acquireWrite();
        assertOnlyLocallyWriteLocked(lock);
        lock.releaseWrite();
        assertNotLocked(lock);
        assertNotLocked(lock);
        lock.acquireWrite();
        assertOnlyLocallyWriteLocked(lock);
        lock.acquireWrite();
        assertOnlyLocallyWriteLocked(lock);
        lock.releaseWrite();
        assertOnlyLocallyWriteLocked(lock);
        lock.releaseWrite();
        assertNotLocked(lock);
        assertNotLocked(lock);
        lock.acquireWrite();
        assertOnlyLocallyWriteLocked(lock);
        lock.acquireWrite();
        assertOnlyLocallyWriteLocked(lock);
        lock.acquireWrite();
        assertOnlyLocallyWriteLocked(lock);
        lock.releaseWrite();
        assertOnlyLocallyWriteLocked(lock);
        lock.releaseWrite();
        assertOnlyLocallyWriteLocked(lock);
        lock.releaseWrite();
        assertNotLocked(lock);
        assertNotLocked(lock);
        try {
            lock.releaseWrite();
            fail("Should have thrown a RuntimeException");
        } catch (RuntimeException e) {
        }
    }

    /**
     * Tests that a read lock can be followed by a write lock and that after
     * releasing a write lock, the read lock is retained.
     */
    public void testReadFollowedByWrite() throws Exception {
        MROWLock lock = new MROWLock();
        assertNotLocked(lock);
        lock.acquireRead();
        assertOnlyLocallyReadLocked(lock);
        lock.acquireWrite();
        assertOnlyLocallyReadAndWriteLocked(lock);
        lock.releaseWrite();
        assertOnlyLocallyReadLocked(lock);
        lock.releaseRead();
        assertNotLocked(lock);
        assertNotLocked(lock);
        lock.acquireRead();
        assertOnlyLocallyReadLocked(lock);
        lock.acquireWrite();
        assertOnlyLocallyReadAndWriteLocked(lock);
        lock.releaseRead();
        assertOnlyLocallyWriteLocked(lock);
        lock.releaseWrite();
        assertNotLocked(lock);
    }

    /**
     * Tests multiple readers from multiple threads.
     */
    public void testMultipleReaders() throws Exception {
        System.out.println("---> testMultipleReaders");
        MROWLock lock = new MROWLock();
        assertNotLocked(lock);
        ReaderThread[] threads = acquireReadLocks(lock);
        assertOnlyExternallyReadLocked(lock);
        releaseReadLocks(threads);
        assertNotLocked(lock);
    }

    /**
     * Tests multiple readers blocking writer request.
     */
    public void testMultipleReadersBlockingWriter() throws Exception {
        System.out.println("---> testMultipleReadersBlockingWriter");
        MROWLock lock = new MROWLock();
        assertNotLocked(lock);
        ReaderThread[] readerThreads = acquireReadLocks(lock);
        assertOnlyExternallyReadLocked(lock);
        WriterThread writerThread = new WriterThread(lock);
        writerThread.start();
        System.out.println("Waiting for writer to block.");
        while (!writerThread.mInAcquire) {
            assertTrue(!writerThread.mAcquired);
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
            }
            if (writerThread.mException != null) {
                throw writerThread.mException;
            }
        }
        synchronized (lock) {
            assertTrue(writerThread.mInAcquire);
            assertTrue(!writerThread.mAcquired);
        }
        assertOnlyExternallyReadLocked(lock);
        System.out.println("Writer blocked. Telling readers to release...");
        releaseReadLocks(readerThreads);
        System.out.println("Waiting for writer to acquire...");
        while (!writerThread.mAcquired) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
            }
            if (writerThread.mException != null) {
                throw writerThread.mException;
            }
        }
        assertOnlyExternallyWriteLocked(lock);
        System.out.println("Write acquired.");
        writerThread.mRelease = true;
        System.out.println("Waiting for writer to finish.");
        writerThread.join();
        if (writerThread.mException != null) {
            throw writerThread.mException;
        }
        assertNotLocked(lock);
        System.out.println("Finished.");
    }

    /**
     * Tests writer lock blocking multiple readers.
     */
    public void testWriterBlockingMultipleReaders() throws Exception {
        System.out.println("---> testWriterBlockingMultipleReaders");
        MROWLock lock = new MROWLock();
        assertNotLocked(lock);
        lock.acquireWrite();
        assertOnlyLocallyWriteLocked(lock);
        ReaderThread[] threads = new ReaderThread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new ReaderThread(lock);
            threads[i].start();
        }
        int enterAcquireCount;
        System.out.println("Waiting for readers to block.");
        do {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
            }
            enterAcquireCount = 0;
            for (int i = 0; i < threads.length; i++) {
                synchronized (lock) {
                    if (threads[i].mInAcquire) {
                        ++enterAcquireCount;
                    }
                    assertTrue(!threads[i].mAcquired);
                }
                if (threads[i].mException != null) {
                    throw threads[i].mException;
                }
            }
        } while (enterAcquireCount != threads.length);
        System.out.println("Readers are blocked.");
        lock.releaseWrite();
        assertNotLocallyLocked(lock);
        System.out.println("Waiting for readers to acquire...");
        int acquireCount;
        do {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
            }
            acquireCount = 0;
            for (int i = 0; i < threads.length; i++) {
                if (threads[i].mAcquired) {
                    ++acquireCount;
                }
                if (threads[i].mException != null) {
                    throw threads[i].mException;
                }
            }
        } while (acquireCount != threads.length);
        System.out.println("Read locks acquired.");
        assertOnlyExternallyReadLocked(lock);
        System.out.println("Telling readers to release...");
        releaseReadLocks(threads);
        assertNotLocked(lock);
        System.out.println("Finished.");
    }

    /**
     * Tests writer lock blocking another writer.
     */
    public void testWriterBlockingWriter() throws Exception {
        System.out.println("---> testWriterBlockingWriter");
        MROWLock lock = new MROWLock();
        assertNotLocked(lock);
        lock.acquireWrite();
        assertOnlyLocallyWriteLocked(lock);
        WriterThread writerThread = new WriterThread(lock);
        writerThread.start();
        System.out.println("Waiting for second writer to block.");
        while (!writerThread.mInAcquire) {
            assertTrue(!writerThread.mAcquired);
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
            }
            if (writerThread.mException != null) {
                throw writerThread.mException;
            }
        }
        synchronized (lock) {
            assertTrue(writerThread.mInAcquire);
            assertTrue(!writerThread.mAcquired);
        }
        assertOnlyLocallyWriteLocked(lock);
        System.out.println("Second writer blocked. Releasing first writer...");
        lock.releaseWrite();
        System.out.println("Waiting for second writer to acquire...");
        while (!writerThread.mAcquired) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
            }
            if (writerThread.mException != null) {
                throw writerThread.mException;
            }
        }
        assertOnlyExternallyWriteLocked(lock);
        System.out.println("Second writer acquired.");
        writerThread.mRelease = true;
        System.out.println("Waiting for second writer to finish.");
        writerThread.join();
        if (writerThread.mException != null) {
            throw writerThread.mException;
        }
        assertNotLocked(lock);
        System.out.println("Finished.");
    }

    private final class ReaderThread extends Thread {

        private MROWLock mLock;

        volatile boolean mInAcquire = false;

        volatile boolean mAcquired = false;

        volatile boolean mRelease = false;

        volatile Exception mException = null;

        ReaderThread(MROWLock aLock) {
            mLock = aLock;
        }

        public void run() {
            mAcquired = false;
            try {
                assertNotLocallyLocked(mLock);
                mInAcquire = true;
                mLock.acquireRead();
                synchronized (mLock) {
                    mInAcquire = false;
                    mAcquired = true;
                }
                assertLocallyReadLocked(mLock);
                while (!mRelease) {
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException e) {
                    }
                }
                mLock.releaseRead();
                assertNotLocallyLocked(mLock);
            } catch (Exception e) {
                mException = e;
                if (mLock.isLocallyLocked()) {
                    mLock.releaseRead();
                }
            }
        }
    }

    private final class WriterThread extends Thread {

        private MROWLock mLock;

        volatile boolean mInAcquire = false;

        volatile boolean mAcquired = false;

        volatile boolean mRelease = false;

        volatile Exception mException = null;

        WriterThread(MROWLock aLock) {
            mLock = aLock;
        }

        public void run() {
            mAcquired = false;
            try {
                mInAcquire = true;
                mLock.acquireWrite();
                synchronized (mLock) {
                    mInAcquire = false;
                    mAcquired = true;
                }
                assertLocallyWriteLocked(mLock);
                while (!mRelease) {
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException e) {
                    }
                }
                mLock.releaseWrite();
                assertNotLocallyLocked(mLock);
            } catch (Exception e) {
                mException = e;
                if (mLock.isLocallyLocked()) {
                    mLock.releaseWrite();
                }
            }
        }
    }
}

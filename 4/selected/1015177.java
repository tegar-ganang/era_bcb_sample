package jsr166.contrib.uncontended;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * This read write lock class is designed to be used in situations where you believe no
 * synchronization is needed, but you want to verify that at runtime.
 * 
 * If one thread tries to acquire a lock while another thread already holds
 * the lock, and at least one of those locks is a write lock,
 * the attempt to acquire the lock will thrown an UnexpectedConcurrentAccessException, 
 * as will all future unlock or lock
 * operations on that lock.
 * 
 * In the JUnit tradition of useless JavaDoc, this code was written in front of the 
 * Gates of Hell (at the Rodan sculpture garden at Stanford University). 
 * 
 * @author pugh
 *
 */
public class UncontendedReadWriteLock implements ReadWriteLock {

    WriteLock writeLock = new WriteLock();

    ReadLock readLock = new ReadLock();

    static class MutableInt {

        int count;
    }

    public static ReadWriteLock getOptionalInstance() {
        if (AbstractUncontendedLock.ENFORCE_UNCONTENDED_LOCKS) return new UncontendedReadWriteLock();
        return new NoOpUncontendedReadWriteLock();
    }

    AtomicReference<Thread> threadHoldingLock = new AtomicReference<Thread>();

    AtomicInteger readLocks = new AtomicInteger();

    ThreadLocal<MutableInt> myReadLocks = new ThreadLocal<MutableInt>() {

        @Override
        public MutableInt initialValue() {
            return new MutableInt();
        }
    };

    int writeLocks = 0;

    public Lock readLock() {
        return readLock;
    }

    public Lock writeLock() {
        return writeLock;
    }

    class ReadLock extends AbstractUncontendedLock {

        public void lock() {
            if (writeLock.broken) writeLock.lockAlreadyBroken();
            readLocks.incrementAndGet();
            MutableInt myCount = myReadLocks.get();
            myCount.count++;
            Thread threadOwningLock = threadHoldingLock.get();
            if (threadOwningLock != null && threadOwningLock != Thread.currentThread()) {
                writeLock.broken = true;
                throw new UnexpectedConcurrentAccessException("Unable to get read lock, write lock already held", threadOwningLock);
            }
        }

        public void unlock() {
            MutableInt myCount = myReadLocks.get();
            if (writeLock.broken) writeLock.lockWasContended();
            myCount.count--;
            readLocks.decrementAndGet();
            Thread threadOwningLock = threadHoldingLock.get();
            if (threadOwningLock != null && threadOwningLock != Thread.currentThread()) {
                writeLock.broken = true;
                throw new UnexpectedConcurrentAccessException("Write lock obtained while read lock already held", threadOwningLock);
            }
        }
    }

    ;

    class WriteLock extends AbstractOwnedUncontendedLock {

        public void lock() {
            if (broken) lockAlreadyBroken();
            setOwningThread(threadHoldingLock);
            int myReadLockCount = myReadLocks.get().count;
            int totalReadLockCount = readLocks.get();
            if (myReadLockCount < totalReadLockCount) {
                broken = true;
                throw new UnexpectedConcurrentAccessException("Read lock held by another thread");
            }
            writeLocks++;
        }

        public void unlock() {
            if (broken) lockWasContended();
            writeLocks--;
            if (writeLocks == 0) threadHoldingLock.set(null);
            int myReadLockCount = myReadLocks.get().count;
            int totalReadLockCount = readLocks.get();
            if (myReadLockCount < totalReadLockCount) {
                broken = true;
                throw new UnexpectedConcurrentAccessException("Read lock obtained by another thread this thread held write lock");
            }
        }
    }
}

;

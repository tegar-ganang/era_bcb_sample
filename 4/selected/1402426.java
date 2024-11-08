package jaxlib.thread.lock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import javax.annotation.Nonnull;

/**
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: SimpleReadWriteLock.java 2993 2011-10-03 02:53:56Z joerg_wassmer $
 */
public class SimpleReadWriteLock extends AbstractQueuedSynchronizer {

    /**
   * @since JaXLib 1.0
   */
    private static final long serialVersionUID = 1L;

    static final int SHARED_SHIFT = 16;

    static final int SHARED_UNIT = (1 << SHARED_SHIFT);

    static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;

    public SimpleReadWriteLock() {
        super();
    }

    /**
   * @serialData
   * @since JaXLib 1.0
   */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        setState(0);
    }

    final ConditionObject createCondition() {
        return new ConditionObject();
    }

    final int getCount() {
        return getState();
    }

    final boolean nonfairTryAcquire(int acquires) {
        acquires &= EXCLUSIVE_MASK;
        final Thread current = Thread.currentThread();
        final int c = getState();
        final int w = c & EXCLUSIVE_MASK;
        if (w + acquires >= SHARED_UNIT) throw new Error("Maximum lock count exceeded");
        if (((c != 0) && ((w == 0) || (current != getExclusiveOwnerThread()))) || !compareAndSetState(c, c + acquires)) return false;
        setExclusiveOwnerThread(current);
        return true;
    }

    /**
   * Perform nonfair tryLock for read.
   */
    final int nonfairTryAcquireShared(final int acquires) {
        while (true) {
            final int c = getState();
            final int nextc = c + (acquires << SHARED_SHIFT);
            if (nextc < c) throw new Error("Maximum lock count exceeded");
            if (((c & EXCLUSIVE_MASK) != 0) && (getExclusiveOwnerThread() != Thread.currentThread())) return -1;
            if (compareAndSetState(c, nextc)) return 1;
        }
    }

    @Override
    protected final boolean tryAcquire(final int acquires) {
        return nonfairTryAcquire(acquires);
    }

    @Override
    protected final int tryAcquireShared(final int acquires) {
        return nonfairTryAcquireShared(acquires);
    }

    @Override
    protected final boolean tryRelease(final int releases) {
        if (getExclusiveOwnerThread() != Thread.currentThread()) throw new IllegalMonitorStateException();
        final int c = getState();
        if ((c & EXCLUSIVE_MASK) == releases) {
            setExclusiveOwnerThread(null);
            setState(c - releases);
            return true;
        }
        setState(c - releases);
        return false;
    }

    @Override
    protected final boolean tryReleaseShared(final int releases) {
        while (true) {
            final int c = getState();
            final int nextc = c - (releases << SHARED_SHIFT);
            if (nextc < 0) throw new IllegalMonitorStateException();
            if (compareAndSetState(c, nextc)) return nextc == 0;
        }
    }

    /**
   * Shortcut for {@code readLock().lock()}.
   *
   * @see ReadLock#lock()
   *
   * @since JaXLib 1.0
   */
    public final void beginRead() {
        acquireShared(1);
    }

    /**
   * Shortcut for {@code writeLock().lock()}.
   *
   * @see WriteLock#lock()
   *
   * @since JaXLib 1.0
   */
    public final void beginWrite() {
        if (compareAndSetState(0, 1)) setExclusiveOwnerThread(Thread.currentThread()); else acquire(1);
    }

    /**
   * Shortcut for {@code writeLock().lock()}.
   *
   * @see WriteLock#lock()
   *
   * @since JaXLib 1.0
   */
    public final void beginWriteNonReentrant() {
        final Thread thread = Thread.currentThread();
        if (thread == getExclusiveOwnerThread()) throw new IllegalMonitorStateException("current thread already holds write lock");
        if (compareAndSetState(0, 1)) setExclusiveOwnerThread(thread); else acquire(1);
    }

    /**
   * Shortcut for {@code readLock().unlock()}.
   *
   * @see ReadLock#unlock()
   *
   * @since JaXLib 1.0
   */
    public final void endRead() {
        releaseShared(1);
    }

    /**
   * Shortcut for {@code writeLock().unlock()}.
   *
   * @see WriteLock#unlock()
   *
   * @since JaXLib 1.0
   */
    public final void endWrite() {
        release(1);
    }

    /**
   * Queries the number of read locks held for this lock.
   * This method is designed for use in monitoring system state, not for synchronization control.
   *
   * @since JaXLib 1.0
   */
    public final int getReadLockCount() {
        return getState() >>> SHARED_SHIFT;
    }

    /**
   * Queries the number of reentrant write holds on this lock by the
   * current thread.  A writer thread has a hold on a lock for
   * each lock action that is not matched by an unlock action.
   *
   * @return the number of holds on the write lock by the current thread,
   *   or zero if the write lock is not held by the current thread.
   *
   * @since JaXLib 1.0
   */
    public final int getWriteHoldCount() {
        return isHeldExclusively() ? (getState() & EXCLUSIVE_MASK) : 0;
    }

    @Override
    public final boolean isHeldExclusively() {
        return ((getState() & EXCLUSIVE_MASK) != 0) && (getExclusiveOwnerThread() == Thread.currentThread());
    }

    public final boolean isWriteLocked() {
        return (getState() & EXCLUSIVE_MASK) != 0;
    }

    @Nonnull
    public final AbstractQueuedSynchronizer.ConditionObject newCondition() {
        return new ConditionObject();
    }

    @Override
    public String toString() {
        final int c = getState();
        return super.toString() + "[Write locks = " + (c & EXCLUSIVE_MASK) + ", Read locks = " + (c >>> SHARED_SHIFT) + "]";
    }

    /**
   * Shortcut for {@code readLock().tryLock()}.
   *
   * @see ReadLock#tryLock()
   *
   * @since JaXLib 1.0
   */
    public final boolean tryBeginRead() {
        return nonfairTryAcquireShared(1) >= 0;
    }

    /**
   * Shortcut for {@code writeLock().tryLock()}.
   *
   * @see WriteLock#tryLock()
   *
   * @since JaXLib 1.0
   */
    public final boolean tryBeginWrite() {
        return nonfairTryAcquire(1);
    }
}

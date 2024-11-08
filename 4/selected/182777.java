package de.grogra.util;

import de.grogra.xl.util.BooleanList;
import de.grogra.xl.util.LongList;
import de.grogra.xl.util.ObjectList;

public class LockableImpl implements Lockable {

    private final Object mutex = new Object();

    private final ObjectList tasks = new ObjectList();

    private final BooleanList isWriterTask = new BooleanList();

    private final LongList times = new LongList();

    private Thread writeLockOwner = null;

    private int activeWriteLocks = 0;

    private int retainedWriteLocks = 0;

    private int activeReadLocks = 0;

    private int retainedReadLocks = 0;

    private final ThreadLocal lockCounts = new ThreadLocal();

    private int waitingWriters = 0;

    private final class LockImpl implements Lock {

        final boolean write;

        boolean retained = false;

        boolean used = false;

        private AssertionError trace;

        LockImpl(boolean write) {
            this.write = write;
        }

        public Lockable getLockable() {
            return LockableImpl.this;
        }

        private boolean getStackTrace() {
            trace = new AssertionError();
            return true;
        }

        public void retain() {
            synchronized (mutex) {
                if (retained) {
                    throw new IllegalStateException("Lock has already been retained");
                }
                retained = true;
                if (write) {
                    retainedWriteLocks++;
                } else {
                    retainedReadLocks++;
                }
            }
            assert getStackTrace();
        }

        public boolean isWriteLock() {
            return write;
        }

        @Override
        protected void finalize() {
            if (retained && !used) {
                System.err.println("Lock " + this + " has not been used as argument to Lockable.execute");
                if (trace != null) {
                    trace.printStackTrace();
                }
            }
        }

        void dispose() {
            retained = true;
            used = true;
        }

        void use() {
            if (!retained) {
                throw new IllegalStateException("Only retained locks may be used later on");
            }
            if (used) {
                throw new IllegalStateException("Lock has already been used");
            }
            used = true;
            if (write) {
                retainedWriteLocks--;
            } else {
                retainedReadLocks--;
            }
            mutex.notifyAll();
        }
    }

    private static final int READ = 0;

    private static final int WRITE = 1;

    private int[] getLockCounts() {
        int[] a = (int[]) lockCounts.get();
        if (a == null) {
            lockCounts.set(a = new int[2]);
        }
        return a;
    }

    private LockImpl lock(boolean hadLock, boolean forWrite) {
        assert Thread.holdsLock(mutex);
        Thread t = Thread.currentThread();
        int[] a = getLockCounts();
        if (t == writeLockOwner) {
            activeWriteLocks++;
            a[WRITE]++;
            return new LockImpl(true);
        } else if ((activeWriteLocks == 0) && (hadLock || (retainedWriteLocks == 0))) {
            if (forWrite) {
                if ((activeReadLocks == a[READ]) && (hadLock || (a[READ] > 0) || (retainedReadLocks == 0))) {
                    assert writeLockOwner == null;
                    writeLockOwner = t;
                    activeWriteLocks++;
                    a[WRITE]++;
                    return new LockImpl(true);
                }
            } else {
                if (hadLock || (a[READ] > 0) || ((retainedReadLocks == 0) && (waitingWriters == 0))) {
                    activeReadLocks++;
                    a[READ]++;
                    return new LockImpl(false);
                }
            }
        }
        return null;
    }

    public void execute(LockProtectedRunnable task, boolean write) {
        task.getClass();
        LockImpl lock;
        synchronized (mutex) {
            if ((lock = lock(false, write)) == null) {
                enqueue(task, write);
            }
        }
        if (lock != null) {
            executeImpl(task, lock);
        }
    }

    private void enqueue(LockProtectedRunnable task, boolean write) {
        tasks.add(task);
        times.add(System.currentTimeMillis());
        isWriterTask.add(write);
        if (write) {
            waitingWriters++;
        }
    }

    private void executeImpl(LockProtectedRunnable task, LockImpl lock) {
        boolean sync = true;
        Throwable exception = null;
        int[] counts = getLockCounts();
        while (task != null) {
            boolean writeLockEntered = lock.write && (counts[WRITE] == 1);
            if (writeLockEntered) {
                try {
                    enterWriteLock();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            try {
                invokeRun0(task, sync, lock);
            } catch (Throwable t) {
                exception = t;
            }
            sync = false;
            task = null;
            if (writeLockEntered) {
                try {
                    leaveWriteLock();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            synchronized (mutex) {
                if (!lock.retained) {
                    lock.dispose();
                }
                if (lock.write) {
                    activeWriteLocks--;
                    if (--counts[WRITE] == 0) {
                        writeLockOwner = null;
                    }
                    assert (activeWriteLocks == 0) == (writeLockOwner == null);
                } else {
                    activeReadLocks--;
                    counts[READ]--;
                }
                mutex.notifyAll();
                if (!tasks.isEmpty()) {
                    if ((lock = lock(true, isWriterTask.get(0))) != null) {
                        task = (LockProtectedRunnable) tasks.removeAt(0);
                        if (isWriterTask.removeAt(0)) {
                            waitingWriters--;
                        }
                        times.removeAt(0);
                    }
                }
            }
        }
        Utils.rethrow(exception);
    }

    public void execute(LockProtectedRunnable task, Lock retained) {
        task.getClass();
        LockImpl lock = null;
        boolean write = ((LockImpl) retained).write;
        synchronized (mutex) {
            ((LockImpl) retained).use();
            if ((lock = lock(true, write)) == null) {
                enqueue(task, write);
            }
        }
        if (lock != null) {
            executeImpl(task, lock);
        }
    }

    private void checkThread(boolean write) {
        if (!isAllowedThread(write)) {
            throw new IllegalStateException("Current thread is not allowed to obtain a " + (write ? "write" : "read") + " lock on " + this);
        }
    }

    public void executeForcedly(LockProtectedRunnable task, boolean write) throws InterruptedException {
        checkThread(write);
        task.getClass();
        LockImpl lock = null;
        synchronized (mutex) {
            while ((lock = lock(false, write)) == null) {
                mutex.wait();
            }
        }
        executeImpl(task, lock);
    }

    public void executeForcedly(LockProtectedRunnable task, Lock retained) throws InterruptedException {
        boolean write = ((LockImpl) retained).write;
        checkThread(write);
        task.getClass();
        LockImpl lock = null;
        synchronized (mutex) {
            while ((lock = lock(true, write)) == null) {
                mutex.wait();
            }
            ((LockImpl) retained).use();
        }
        executeImpl(task, lock);
    }

    public boolean isLocked(boolean write) {
        int[] a = getLockCounts();
        return (a[WRITE] > 0) || (!write && (a[READ] > 0));
    }

    public int getQueueLength() {
        synchronized (mutex) {
            return tasks.size();
        }
    }

    public long getMaxWaitingTime() {
        synchronized (mutex) {
            return times.isEmpty() ? -1 : System.currentTimeMillis() - times.get(0);
        }
    }

    protected boolean isAllowedThread(boolean write) {
        return true;
    }

    protected void executeInAllowedThread(Runnable r) {
        throw new UnsupportedOperationException("Not implemented in " + getClass());
    }

    private void invokeRun0(final LockProtectedRunnable task, boolean sync, Lock lock) {
        final boolean write = lock.isWriteLock();
        class Helper implements Runnable, LockProtectedRunnable {

            private boolean executed;

            private Lock retainedLock;

            public void run() {
                synchronized (this) {
                    execute(this, write);
                    if (executed) {
                        return;
                    }
                    while (retainedLock == null) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    executeForcedly(task, retainedLock);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            public void run(boolean sync, Lock lo) {
                if (isAllowedThread(lo.isWriteLock())) {
                    executed = true;
                    invokeRun(task, false, lo);
                } else {
                    lo.retain();
                    retainedLock = lo;
                    synchronized (this) {
                        notifyAll();
                    }
                }
            }
        }
        if (task instanceof Helper) {
            task.run(sync, lock);
        } else if (isAllowedThread(write)) {
            invokeRun(task, sync, lock);
        } else {
            executeInAllowedThread(new Helper());
        }
    }

    protected void invokeRun(final LockProtectedRunnable task, boolean sync, Lock lock) {
        task.run(sync, lock);
    }

    protected void enterWriteLock() {
    }

    protected void leaveWriteLock() {
    }
}

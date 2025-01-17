package util.misc;

public class RWLock {

    private int givenLocks;

    private int waitingWriters;

    public static boolean TRACE = false;

    private Object mutex;

    public RWLock() {
        mutex = new Object();
        givenLocks = 0;
        waitingWriters = 0;
    }

    public void getReadLock() {
        synchronized (mutex) {
            try {
                while ((givenLocks == -1) || (waitingWriters != 0)) {
                    if (TRACE) System.out.println(Thread.currentThread().toString() + "waiting for readlock");
                    mutex.wait();
                }
            } catch (java.lang.InterruptedException e) {
                System.out.println(e);
            }
            givenLocks++;
            if (TRACE) System.out.println(Thread.currentThread().toString() + " got readlock, GivenLocks = " + givenLocks);
        }
    }

    public void getWriteLock() {
        synchronized (mutex) {
            waitingWriters++;
            try {
                while (givenLocks != 0) {
                    if (TRACE) System.out.println(Thread.currentThread().toString() + "waiting for writelock");
                    mutex.wait();
                }
            } catch (java.lang.InterruptedException e) {
                System.out.println(e);
            }
            waitingWriters--;
            givenLocks = -1;
            if (TRACE) System.out.println(Thread.currentThread().toString() + " got writelock, GivenLocks = " + givenLocks);
        }
    }

    public void releaseLock() {
        synchronized (mutex) {
            if (givenLocks == 0) return;
            if (givenLocks == -1) givenLocks = 0; else givenLocks--;
            if (TRACE) System.out.println(Thread.currentThread().toString() + " released lock, GivenLocks = " + givenLocks);
            mutex.notifyAll();
        }
    }
}

package de.fzi.herakles.anytime;

public class ReadWriteLock {

    private Object lockObj;

    private int totalReadLocksGiven;

    private boolean writeLockIssued;

    private int threadsWaitingForWriteLock;

    public ReadWriteLock() {
        lockObj = new Object();
        writeLockIssued = false;
    }

    public void getReadLock() {
        synchronized (lockObj) {
            while ((writeLockIssued) || (threadsWaitingForWriteLock != 0)) {
                try {
                    lockObj.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            totalReadLocksGiven++;
        }
    }

    public void getWriteLock() {
        synchronized (lockObj) {
            threadsWaitingForWriteLock++;
            while ((totalReadLocksGiven != 0) || (writeLockIssued)) {
                try {
                    lockObj.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            threadsWaitingForWriteLock--;
            writeLockIssued = true;
        }
    }

    public void done() {
        synchronized (lockObj) {
            if ((totalReadLocksGiven == 0) && (!writeLockIssued)) {
                System.out.println(" Error: Invalid call to release the lock");
                return;
            }
            if (writeLockIssued) writeLockIssued = false; else totalReadLocksGiven--;
            lockObj.notifyAll();
        }
    }
}

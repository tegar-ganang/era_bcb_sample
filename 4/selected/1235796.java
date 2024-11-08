package javax.media.j3d;

/**
 * Use this lock to allow multiple reads/single write synchronization.
 * To prevent deadlock a read/writeLock call must match with a read/writeUnlock call.
 * Write request has precedence over read request.
 */
class MRSWLock {

    static boolean debug = false;

    private int readCount;

    private boolean write;

    private int writeRequested;

    private int lockRequested;

    MRSWLock() {
        readCount = 0;
        write = false;
        writeRequested = 0;
        lockRequested = 0;
    }

    final synchronized void readLock() {
        lockRequested++;
        while ((write == true) || (writeRequested > 0)) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        lockRequested--;
        readCount++;
    }

    final synchronized void readUnlock() {
        if (readCount > 0) readCount--; else if (debug) System.err.println("ReadWriteLock.java : Problem! readCount is >= 0.");
        if (lockRequested > 0) notifyAll();
    }

    final synchronized void writeLock() {
        lockRequested++;
        writeRequested++;
        while ((readCount > 0) || (write == true)) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        write = true;
        lockRequested--;
        writeRequested--;
    }

    final synchronized void writeUnlock() {
        write = false;
        if (lockRequested > 0) notifyAll();
    }
}

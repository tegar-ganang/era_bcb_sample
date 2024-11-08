package iwork.util;

/** This class serves as a Mutex to provide access to whatever object
 * the programmers sets the readWriteObject to be.  Multiple
 * simultaneous reader threads are allowed to access the object at one
 * time, but only one writer thread.  Threads reading the
 * readWriteObject need to call getReadLock() before reading the
 * object and releaseReadLock() when they are done.  Threads modifying
 * the readWriteObject need to call getWriteLock() before modifying
 * the object and releaseWriteLock() after modifying the object. 
 */
public class ReaderWriterMutex {

    int readersWaiting = 0;

    int numReaders = 0;

    int writersWaiting = 0;

    boolean writeLockHeld = false;

    /** The object to which this mutex is controlling access */
    Object readWriteObject;

    /** Creates a new ReaderWriterMutext associated with the given
   * object.
   *
   * @param protectedObject The object for which this mutex is controlling
   * access.
   */
    public ReaderWriterMutex(Object protectedObject) {
        readWriteObject = protectedObject;
    }

    /** Prints out the current status if it doesn't match what is permissible */
    synchronized void printErrorStatus(String header) {
        if (numReaders > 0 && writeLockHeld) {
            System.out.println("Invalid state after " + header + "\n\tReaders Waiting:\t" + readersWaiting + "\n\tActive Readers:\t" + numReaders + "\n\tWriters Waiting:\t" + writersWaiting + "\n\tWrite Lock Held:\t" + writeLockHeld);
            System.out.flush();
        }
    }

    /** Gets a read lock on the object associated with this mutex and
   * returns it.  An arbitrary number of readers may hold a read lock
   * at any one time, but nobody will be allowed to hold a read lock
   * if the write lock is held.  
   */
    public synchronized Object getReadLock() {
        while (readersWaiting > 0 || writersWaiting > 0 || writeLockHeld) {
            try {
                readersWaiting++;
                wait();
                readersWaiting--;
                if (readersWaiting > 0 && writersWaiting == 0) {
                    notify();
                }
                break;
            } catch (InterruptedException e) {
            }
        }
        numReaders++;
        printErrorStatus("getReadLock:");
        return readWriteObject;
    }

    /** Releases a readLock.  If there are no other read locks held after
   * this is released, the write lock will become available 
   */
    public synchronized void releaseReadLock() {
        if (--numReaders == 0) notify();
        printErrorStatus("releaseReadLock:");
    }

    /** Gets the write lock and returns the object associated with this
   * mutex.  Only one thread may hold the write lock at one time, and
   * nobody will be allowed to hold the read lock while the write lock
   * is held */
    public synchronized Object getWriteLock() {
        while (readersWaiting > 0 || writersWaiting > 0 || writeLockHeld || numReaders > 0) {
            try {
                writersWaiting++;
                wait();
                if (numReaders == 0) {
                    writersWaiting--;
                    break;
                }
            } catch (InterruptedException e) {
            }
        }
        writeLockHeld = true;
        printErrorStatus("getWriteLock:");
        return readWriteObject;
    }

    /** Releases the write lock.  After this any blocked readers or writers
   * may compete to proceed */
    public synchronized void releaseWriteLock() {
        writeLockHeld = false;
        notify();
        printErrorStatus("releaseWriteLock:");
    }
}

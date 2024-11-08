package org.fudaa.ctulu.gis.shapefile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.memoire.fu.FuLog;

/**
 * A read-write lock for shapefiles so that OS file locking exceptions will not
 * ruin an attempt to update a shapefile. On windows there are often operating
 * system locking conflicts when writing to a shapefile. In order to not have
 * exceptions thrown everytime a write is made, geotools has implemented file
 * locking for shapefiles.
 * 
 * @author jeichar
 * @source $URL: http://svn.geotools.org/geotools/tags/2.2-RC3/plugin/shapefile/src/org/geotools/data/shapefile/Lock.java $
 */
public class Lock {

    /**
     * indicates a write is occurring.
     */
    int writeLocks_;

    /**
     * if not null a writer is waiting for the lock or is writing.
     */
    Thread writer_;

    /**
     * Thread->Owner map. If empty no read locks exist.
     */
    Map owners_ = new HashMap();

    /**
     * If the lock can be read locked the lock will be read and default
     * visibility for tests.
     * 
     * @return
     * @throws IOException
     */
    synchronized boolean canRead() throws IOException {
        if (writer_ != null && writer_ != Thread.currentThread()) {
            return false;
        }
        if (writer_ == null) {
            return true;
        }
        if (owners_.size() > 1) {
            return false;
        }
        return true;
    }

    /**
     * If the lock can be read locked the lock will be read and default
     * visibility for tests.
     * 
     * @return
     * @throws IOException
     */
    synchronized boolean canWrite() throws IOException {
        if (owners_.size() > 1) {
            return false;
        }
        if ((canRead()) && (writer_ == Thread.currentThread() || writer_ == null)) {
            if (owners_.isEmpty()) {
                return true;
            }
            if (owners_.containsKey(Thread.currentThread())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Called by shapefileReader before a read is started and before an IOStream
     * is openned.
     * 
     * @throws IOException
     */
    public synchronized void lockRead() throws IOException {
        if (!canRead()) {
            while (writeLocks_ > 0 || writer_ != null) {
                try {
                    wait();
                } catch (final InterruptedException e) {
                    throw (IOException) new IOException().initCause(e);
                }
            }
        }
        assertTrue("A write lock exists that is owned by another thread", canRead());
        final Thread current = Thread.currentThread();
        Owner owner = (Owner) owners_.get(current);
        if (owner != null) {
            owner.timesLocked_++;
        } else {
            owner = new Owner(current);
            owners_.put(current, owner);
        }
        FuLog.trace("CSG : Start Read Lock:" + owner);
    }

    private void assertTrue(final String _message, boolean _b) {
        if (!_b) {
            throw new AssertionError(_message);
        }
    }

    /**
     * Called by ShapefileReader after a read is complete and after the IOStream
     * is closed.
     */
    public synchronized void unlockRead() {
        assertTrue("Current thread does not have a readLock", owners_.containsKey(Thread.currentThread()));
        final Owner owner = (Owner) owners_.get(Thread.currentThread());
        assertTrue("Current thread has " + owner.timesLocked_ + "negative number of locks", owner.timesLocked_ > 0);
        owner.timesLocked_--;
        if (owner.timesLocked_ == 0) {
            owners_.remove(Thread.currentThread());
        }
        notifyAll();
        FuLog.trace("CSG : unlock Read:" + owner);
    }

    /**
     * Called by ShapefileDataStore before a write is started and before an
     * IOStream is openned.
     * 
     * @throws IOException
     */
    public synchronized void lockWrite() throws IOException {
        final Thread currentThread = Thread.currentThread();
        if (writer_ == null) {
            writer_ = currentThread;
        }
        while (!canWrite()) {
            try {
                wait();
            } catch (final InterruptedException e) {
                throw (IOException) new IOException().initCause(e);
            }
            if (writer_ == null) {
                writer_ = currentThread;
            }
        }
        if (writer_ == null) {
            writer_ = currentThread;
        }
        assertTrue("The current thread is not the writer", writer_ == currentThread);
        assertTrue("There are read locks not belonging to the current thread.", canRead());
        writeLocks_++;
        FuLog.trace("CSG : " + currentThread.getName() + " is getting write lock:" + writeLocks_);
    }

    private static class Owner {

        final Thread owner_;

        int timesLocked_;

        Owner(final Thread _owner) {
            this.owner_ = _owner;
            timesLocked_ = 1;
        }

        public String toString() {
            return owner_.getName() + " has " + timesLocked_ + " locks";
        }
    }

    /**
     * default visibility for tests.
     * 
     */
    synchronized int getReadLocks(final Thread _thread) {
        final Owner owner = (Owner) owners_.get(_thread);
        if (owner == null) {
            return -1;
        }
        return owner.timesLocked_;
    }

    public synchronized void unlockWrite() {
        if (writeLocks_ > 0) {
            assertTrue("current thread does not own the write lock", writer_ == Thread.currentThread());
            assertTrue("writeLock has already been unlocked", writeLocks_ > 0);
            writeLocks_--;
            if (writeLocks_ == 0) {
                writer_ = null;
            }
        }
        FuLog.trace("CSG : unlock write:" + Thread.currentThread().getName());
        notifyAll();
    }

    /**
     * default visibility for tests.
     * 
     */
    synchronized boolean ownWriteLock(final Thread _thread) {
        return writer_ == _thread && writeLocks_ > 0;
    }
}

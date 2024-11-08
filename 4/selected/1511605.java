package uk.ac.cam.caret.minibix.archive.impl.file.lock;

import java.io.*;
import java.nio.channels.*;
import uk.ac.cam.caret.minibix.archive.api.LowLevelStorageException;

/** @exclude */
public class Flock {

    private File file;

    private RandomAccessFile random;

    private FileChannel channel;

    private FileLock lock;

    Flock(FlockSet set, String name) {
        file = new File(set.getLocation(), name);
    }

    Flock(File in) {
        file = in;
    }

    void close() throws LowLevelStorageException {
        if (lock != null) {
            try {
                lock.release();
                random.close();
            } catch (IOException x) {
                throw new LowLevelStorageException("Could not open lock", x);
            }
        }
    }

    private void getFile() throws LowLevelStorageException {
        try {
            random = new RandomAccessFile(file, "rw");
            channel = random.getChannel();
        } catch (IOException x) {
            throw new LowLevelStorageException("Could not open lock", x);
        }
    }

    private void dropFile() throws LowLevelStorageException {
        try {
            random.close();
        } catch (IOException x) {
            throw new LowLevelStorageException("Could not close lock", x);
        }
    }

    private void getLock(boolean shared) throws LowLevelStorageException {
        try {
            lock = channel.lock(0, Long.MAX_VALUE, shared);
        } catch (IOException x) {
            throw new LowLevelStorageException("Could not open lock", x);
        }
    }

    private boolean getLockNoBlock(boolean shared) throws LowLevelStorageException {
        try {
            lock = channel.tryLock(0, Long.MAX_VALUE, shared);
            return lock != null;
        } catch (IOException x) {
            throw new LowLevelStorageException("Could not open lock", x);
        }
    }

    private void dropLock() throws LowLevelStorageException {
        try {
            lock.release();
            lock = null;
        } catch (IOException x) {
            throw new LowLevelStorageException("Could not open lock", x);
        }
    }

    public void acquireExclusive() throws LowLevelStorageException {
        getFile();
        getLock(false);
    }

    public void acquireShared() throws LowLevelStorageException {
        getFile();
        getLock(true);
    }

    public boolean tryAcquireExclusive() throws LowLevelStorageException {
        getFile();
        return getLockNoBlock(false);
    }

    public boolean tryAcquireShared() throws LowLevelStorageException {
        getFile();
        return getLockNoBlock(true);
    }

    public void drop() throws LowLevelStorageException {
        dropLock();
        dropFile();
    }
}

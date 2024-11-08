package org.javaseis.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.logging.Logger;
import org.javaseis.util.SeisException;

/**
 * Provide a lock facility based on Java NIO FileLock classes This type of
 * locking strategy should work reasonaly reliably for cases where the number of
 * transactions per second is low. If this locking class gets pushed to hard it
 * will fail.
 * <p>
 * 
 * @author Chuck Mosher for JavaSeis.org
 */
public class SeisLock implements ISeisLock {

    public static String EXTN = ".lock";

    private RandomAccessFile lockFile;

    private String _path;

    private int retry;

    private long maxSleep;

    private boolean isLocked;

    private boolean trackTime = false;

    private long lockTime = 0;

    private long t0 = 0;

    private FileChannel fc;

    private FileLock fl;

    public static final Logger log = Logger.getLogger("org.javaseis.io.FileLock");

    /**
	 * Create a lock manager for a particular file. A default retry count of 8
	 * is used for acquiring the lock.
	 * 
	 * @param path
	 *            of file to be locked
	 * @throws SeisException
	 *             if file is not found
	 */
    public SeisLock(String path) throws SeisException {
        this(path, 8, 100);
    }

    /**
	 * Create a lock manager for a particular file. retry count of 8 is used for
	 * acquiring the lock.
	 * 
	 * @param path
	 *            of regular file to protect with a lock
	 */
    public SeisLock(String path, int retryCount) throws SeisException {
        this(path, retryCount, 100l);
    }

    /**
	 * Create a lock manager for a particular file. retry count of 8 is used for
	 * acquiring the lock.
	 * 
	 * @param path
	 *            of regular file to protect with a lock
	 */
    public SeisLock(String path, int retryCount, long maxsleep) throws SeisException {
        if (path == null || path.length() < 1) throw new IllegalArgumentException("Invalid path");
        _path = path + EXTN;
        isLocked = false;
        if (retryCount < 1) throw new IllegalArgumentException("Retry count parameter is invalid");
        retry = retryCount;
        maxSleep = maxsleep > 10000 ? 10000 : maxsleep;
        try {
            lockFile = new RandomAccessFile(_path, "rw");
            fc = lockFile.getChannel();
        } catch (FileNotFoundException e) {
            throw new SeisException(e.toString());
        }
    }

    public boolean lock() throws SeisException {
        if (isLocked) throw new SeisException("File already locked by this process" + _path);
        if (trackTime) t0 = System.nanoTime();
        long isleep = 0;
        isLocked = false;
        for (int i = 0; i < retry; i++) {
            try {
                fl = fc.tryLock();
                if (fl != null) {
                    isLocked = true;
                    break;
                } else {
                    log.info("Retry " + (i + 1) + " to obtain lock on file " + _path);
                    isleep = (long) ((double) maxSleep * Math.random());
                    try {
                        Thread.sleep(isleep);
                    } catch (InterruptedException e) {
                        log.info("Retry " + (i + 1) + " interrupted");
                    }
                }
            } catch (IOException ex) {
                log.info("Retry " + (i + 1) + " to obtain lock on file " + _path);
                try {
                    Thread.sleep(isleep);
                } catch (InterruptedException e) {
                    log.info("Retry " + (i + 1) + " interrupted");
                }
            }
        }
        if (!isLocked) {
            log.severe("Could not obtain lock on file " + _path);
        }
        if (trackTime) lockTime += System.nanoTime() - t0;
        return isLocked;
    }

    public void release() throws SeisException {
        if (!isLocked) {
            log.severe("Attempt to release a lock that does not exist");
            return;
        }
        try {
            fl.release();
        } catch (IOException e) {
            throw new SeisException("Lock release failed: \n" + e.getMessage());
        }
        isLocked = false;
        return;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void trackTime(boolean trackTime) {
        this.trackTime = trackTime;
    }

    public float getTime() {
        return 1.e-9f * (float) lockTime;
    }
}

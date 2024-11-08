package com.faunos.util.cc;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import com.faunos.util.Validator;

/**
 * A cross-process, file-based locking protocol. The implementation
 * uses an exclusive lock on a <em>local</em> file.
 * <p/>
 * This implementation does not work on every OS/file system combination.
 * For example, if the local file is NTFS mounted through aufs on Linux, then
 * this implementation does not work. Must
 * experiment to find a more robust cross-process lock implementation for this
 * class. Consider this class for the time being a marker for a better
 * implementation.
 *
 * @author Babak Farhang
 */
public class XprocLock {

    private final File lockFile;

    private FileChannel channel;

    private FileLock lock;

    private boolean hookedToShutdown;

    private final Object runtimeLock = new Object();

    /**
     * Creates an instance with the specified lock file.
     * On return, the instance is <em>not</em> locked.
     * <p/>
     * Note the given <tt>lockFile</tt> must not be
     * locked from within this process any other way.
     * Also, the specified lock file must be a <em>local</em> file.
     * 
     * @see #lock
     */
    public XprocLock(File lockFile) {
        Validator.ARG.notNull(lockFile);
        this.lockFile = lockFile;
    }

    /**
     * Locks the instance by acquiring an exclusive lock on the
     * underlying file passed in at construction.  Repeated invocations
     * are OK; the lock is still released on a single invocation of
     * {@linkplain #release()}.
     * 
     * @throws IllegalStateException
     *         if the underlying file lock could not be acquired
     * @see #release()
     */
    public void lock() {
        synchronized (runtimeLock) {
            try {
                lockImpl();
            } catch (RuntimeException rx) {
                release();
                throw rx;
            } catch (IOException iox) {
                release();
                Validator.STATE.fail("failed to acquire lock file: " + lockFile, iox);
            }
        }
    }

    private void lockImpl() throws IOException {
        if (lock != null) return;
        if (lockFile.exists()) lockFile.delete();
        channel = new RandomAccessFile(lockFile, "rw").getChannel();
        lock = channel.tryLock();
        if (lock == null) {
            channel.close();
            Validator.STATE.fail("Lock file held by another process: " + lockFile);
        }
        if (!hookedToShutdown) {
            Runtime.getRuntime().addShutdownHook(new Thread() {

                public void run() {
                    release();
                }
            });
            hookedToShutdown = true;
        }
    }

    /**
     * Releases the lock.  The lock may be re-acquired.
     * 
     * @see #lock()
     */
    public void release() {
        synchronized (runtimeLock) {
            if (lock != null) {
                try {
                    lock.release();
                } catch (IOException iox) {
                }
                lock = null;
            }
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException iox) {
                }
                channel = null;
            }
            lockFile.delete();
        }
    }
}

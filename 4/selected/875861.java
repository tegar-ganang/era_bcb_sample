package jaxlib.io.file;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import jaxlib.col.ref.WeakValueHashMap;
import jaxlib.util.CheckArg;

/**
 * A lock to synchronize access on files by multiple threads and processes.
 * <p>
 * Instances of this class are backed by a {@link FileLock}. Behaviour of locks is the similar as described 
 * there. In difference {@code LockFile} instances are also working for locking files inside the same virtual
 * machine as long as the same {@code LockFile} instance is used to obtain the locks.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: LockFile.java 1416 2005-08-03 22:52:21Z joerg_wassmer $
 */
public class LockFile extends Object {

    static final Logger logger = Logger.getLogger("java.io");

    /**
   * Returns the same {@code LockFile} instance for equal files.
   * This method always uses the normalized absolute path.
   *
   * @see File#getAbsolutePath()
   * @see FilePaths#normalize(String)
   *
   * @since JaXLib 1.0
   */
    public static LockFile getSharedInstance(File file) {
        file = new File(FilePaths.local.normalize(file.getAbsolutePath()));
        String path = file.getPath();
        path.hashCode();
        synchronized (LockFile.SharedLockFile.instances) {
            LockFile.SharedLockFile lockFile = LockFile.SharedLockFile.instances.get(path);
            if (lockFile == null) {
                lockFile = new LockFile.SharedLockFile(file);
                LockFile.SharedLockFile.instances.put(path, lockFile);
            }
            return lockFile;
        }
    }

    /**
   * Returns the same {@code LockFile} instance for equal paths.
   * This method always uses the normalized absolute path.
   *
   * @see File#getAbsolutePath()
   * @see FilePaths#normalize(String)
   *
   * @since JaXLib 1.0
   */
    public static LockFile getSharedInstance(String path) {
        return getSharedInstance(new File(path));
    }

    final File file;

    private volatile FileLock lock;

    private volatile int lockCount = 0;

    private final Object sync = new Object();

    public LockFile(File file) {
        super();
        if (file == null) throw new NullPointerException("file");
        this.file = file;
    }

    public LockFile(String path) {
        this(new File(path));
    }

    private LockFile.Lock lock(long timeout, TimeUnit timeUnit, boolean shared) throws IOException {
        CheckArg.notNegative(timeout, "timeout");
        CheckArg.notNull(timeUnit, "timeUnit");
        FileLock lock = null;
        long time0 = 0;
        while (true) {
            lock = lockStage1(shared);
            if (lock != null) {
                break;
            } else {
                if ((timeout != 0) && (time0 == 0)) time0 = System.nanoTime();
                lock = lockStage2(shared, timeout, timeUnit);
                lock = lockStage3(lock, shared);
                if (lock != null) {
                    break;
                } else {
                    if (timeout > 0) {
                        timeout = timeUnit.toNanos(timeout);
                        timeUnit = TimeUnit.NANOSECONDS;
                        timeout -= System.nanoTime() - time0;
                    }
                    if (timeout > 0) continue; else break;
                }
            }
        }
        if (lock != null) return new LockFile.Lock(this, lock); else throw new FileLockTimeoutException("timeout waiting for file lock: " + this.file);
    }

    private FileLock lockStage1(boolean shared) {
        synchronized (this.sync) {
            FileLock lock = this.lock;
            if (lock == null) {
                return null;
            } else if (!lock.isValid()) {
                this.lock = null;
                return null;
            } else if (shared && lock.isShared()) {
                this.lockCount++;
                return lock;
            } else {
                return null;
            }
        }
    }

    private FileLock lockStage2(boolean shared, long timeout, TimeUnit timeUnit) throws IOException {
        if (shared) this.file.createNewFile();
        RandomAccessFile rf = new RandomAccessFile(this.file, shared ? "r" : "rw");
        return Files.createLock(rf, this.file, rf.getChannel(), shared, timeout, timeUnit);
    }

    private FileLock lockStage3(FileLock lock, boolean shared) throws IOException {
        synchronized (this.sync) {
            FileLock lock2 = this.lock;
            if ((lock2 == null) && this.file.exists()) {
                this.lock = lock;
                this.lockCount = 1;
                return lock;
            } else {
                if (shared) {
                    if (!this.file.exists()) {
                        lock.channel().close();
                        lock = null;
                    }
                    if (lock2.isValid()) {
                        if (lock2.isShared()) {
                            lock.channel().close();
                            this.lock = lock2;
                            this.lockCount++;
                            return lock2;
                        }
                    } else if (lock != null) {
                        this.lock = lock;
                        this.lockCount = 1;
                        return lock;
                    }
                }
                if (lock != null) lock.channel().close();
                return null;
            }
        }
    }

    public final int getLockCount() {
        return this.lockCount;
    }

    public final File getFile() {
        return this.file;
    }

    public final boolean isLocked() {
        return this.lockCount != 0;
    }

    public final FileLockState getState() {
        FileLock lock = this.lock;
        if (lock == null) return FileLockState.UNLOCKED; else if (lock.isShared()) return FileLockState.SHARED; else return FileLockState.EXCLUSIV;
    }

    /**
   * Lock the file exlusively.
   *
   * @return 
   *  A file lock, never {@code null}.
   *
   * @throws FileNotFoundException
   *  if the underlying file is an existing directory rather than a non-existing or a normal file.
   * @throws FileLockTimeoutException
   *  if another process outside of the vm or another user of this {@code LockFile} instance is holding the 
   *  the lock.
   * @throws IOException
   *  if an I/O error occurs.
   *
   * @see FileChannel#lock()
   *
   * @since JaXLib 1.0
   */
    public final LockFile.Lock lock() throws FileNotFoundException, FileLockTimeoutException, IOException {
        return lock(0, TimeUnit.MILLISECONDS);
    }

    /**
   * Lock the file exlusively.
   *
   * @return 
   *  A file lock, never {@code null}.
   *
   * @param timeout
   *  the maximum time to wait for the lock.
   * @param timeUnit
   *  the timeunit for the timeout value.
   *
   * @throws FileNotFoundException
   *  if the underlying file is an existing directory rather than a non-existing or a normal file.
   * @throws FileLockInterruptionException
   *  if the timeout is not zero and another thread interrupts the current one while it waits for 
   *  getting the lock onto the file.
   * @throws FileLockTimeoutException
   *  if another process outside of the vm or another user of this {@code LockFile} instance is not releasing 
   *  the lock before time is out.
   * @throws IOException
   *  if an I/O error occurs.
   * @throws IllegalArgumentException
   *  if {@code timeout < 0}.
   * @throws NullPointerException
   *  if {@code timeUnit == null}.
   *
   * @see FileChannel#lock()
   *
   * @since JaXLib 1.0
   */
    public LockFile.Lock lock(long timeout, TimeUnit timeUnit) throws FileNotFoundException, FileLockInterruptionException, FileLockTimeoutException, IOException {
        LockFile.Lock lock = lock(timeout, timeUnit, false);
        logger.finer(this.file.getPath());
        return lock;
    }

    /**
   * Lock the file in shared mode.
   *
   * @return 
   *  A file lock, never {@code null}.
   *
   * @throws FileNotFoundException
   *  if the underlying file is an existing directory rather than a non-existing or a normal file.
   * @throws FileLockTimeoutException
   *  if another process outside of the vm or another user of this {@code LockFile} instance is holding 
   *  an exclusive lock.
   * @throws IOException
   *  if an I/O error occurs.
   *
   * @see FileChannel#lock()
   *
   * @since JaXLib 1.0
   */
    public final LockFile.Lock lockShared() throws FileNotFoundException, FileLockTimeoutException, IOException {
        return lockShared(0, TimeUnit.MILLISECONDS);
    }

    /**
   * Lock the file in shared mode.
   *
   * @return 
   *  A file lock, never {@code null}.
   *
   * @param timeout
   *  the maximum time to wait for the lock.
   * @param timeUnit
   *  the timeunit for the timeout value.
   *
   * @throws FileNotFoundException
   *  if the underlying file is an existing directory rather than a non-existing or a normal file.
   * @throws FileLockInterruptionException
   *  if the timeout is not zero and another thread interrupts the current one while it waits for 
   *  getting the lock onto the file.
   * @throws FileLockTimeoutException
   *  if another process outside of the vm or another user of this {@code LockFile} instance is not releasing
   *  an exclusiv lock before time is out.
   * @throws IOException
   *  if an I/O error occurs.
   * @throws IllegalArgumentException
   *  if {@code timeout < 0}.
   * @throws NullPointerException
   *  if {@code timeUnit == null}.
   *
   * @see FileChannel#lock()
   *
   * @since JaXLib 1.0
   */
    public LockFile.Lock lockShared(long timeout, TimeUnit timeUnit) throws FileNotFoundException, FileLockInterruptionException, FileLockTimeoutException, IOException {
        LockFile.Lock lock = lock(timeout, timeUnit, true);
        logger.finer(this.file.getPath());
        return lock;
    }

    public static final class Lock extends Object {

        private final LockFile lockFile;

        private FileLock lock;

        private final boolean shared;

        Lock(LockFile lockFile, FileLock lock) {
            super();
            this.lockFile = lockFile;
            this.lock = lock;
            this.shared = lock.isShared();
        }

        private boolean release(boolean delete) throws IOException {
            FileLock lock;
            synchronized (this) {
                lock = this.lock;
                this.lock = null;
            }
            if (lock == null) return false;
            final File file = this.lockFile.file;
            synchronized (this.lockFile.sync) {
                int lockCount = this.lockFile.lockCount - 1;
                this.lockFile.lockCount = lockCount;
                if (lockCount != 0) return false;
                this.lockFile.lock = null;
                Throwable error = null;
                if (delete) {
                    try {
                        delete = !file.delete();
                    } catch (Throwable ex) {
                        error = ex;
                    }
                }
                try {
                    lock.release();
                } catch (Throwable ex) {
                    if (error == null) error = ex;
                }
                if (delete) {
                    try {
                        delete = file.isFile() && !file.delete();
                    } catch (Throwable ex) {
                        if (error == null) error = ex;
                    }
                }
                try {
                    lock.channel().close();
                } catch (Throwable ex) {
                    if (error == null) error = ex;
                }
                if (delete) {
                    try {
                        if (file.isFile()) file.delete();
                    } catch (Throwable ex) {
                        if (error == null) error = ex;
                    }
                }
                if (error == null) {
                    LockFile.logger.finer(this.lockFile.file.getPath());
                    return true;
                } else {
                    if (error instanceof Error) throw (Error) error; else if (error instanceof RuntimeException) throw (RuntimeException) error; else if (error instanceof IOException) throw (IOException) error; else throw new RuntimeException(error);
                }
            }
        }

        @Override
        protected void finalize() throws Throwable {
            if ((this.lock != null) && this.lock.isValid()) {
                try {
                    release(false);
                    if (LockFile.logger.isLoggable(Level.WARNING)) logger.warning("released forgotten lock of file: " + this.lockFile.file);
                } catch (Throwable ex) {
                    LockFile.logger.log(Level.SEVERE, "error releasing forgotten lock of file " + this.lockFile.file, ex);
                }
            }
        }

        public boolean deleteAndRelease() throws IOException {
            return release(true);
        }

        public File getFile() {
            return this.lockFile.file;
        }

        public LockFile getLockFile() {
            return this.lockFile;
        }

        public boolean isShared() {
            return this.shared;
        }

        public boolean isValid() {
            FileLock lock;
            synchronized (this) {
                lock = this.lock;
            }
            return (lock != null) && lock.isValid();
        }

        public boolean release() throws IOException {
            return release(false);
        }
    }

    private static final class SharedLockFile extends LockFile {

        static final WeakValueHashMap<String, SharedLockFile> instances = new WeakValueHashMap<String, SharedLockFile>();

        SharedLockFile(File file) {
            super(file);
        }

        @Override
        protected void finalize() throws Throwable {
            synchronized (instances) {
                instances.purge();
            }
        }
    }
}

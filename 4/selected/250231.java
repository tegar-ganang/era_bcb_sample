package edu.rabbit.kernel.fs;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import edu.rabbit.DbErrorCode;
import edu.rabbit.DbException;
import edu.rabbit.IOErrorCode;
import edu.rabbit.DbIOException;
import edu.rabbit.LogDefinitions;
import edu.rabbit.kernel.DeviceCharacteristics;
import edu.rabbit.kernel.LockType;
import edu.rabbit.kernel.SyncFlags;
import edu.rabbit.kernel.DbUtility;
import edu.rabbit.kernel.memory.IMemoryPointer;

/**
 * @author Yuanyan<yanyan.cao@gmail.com>
 * 
 * 
 */
public class DbFile implements IFile {

    private static final boolean DB_LOG_FILES = DbUtility.getBoolSysProp(LogDefinitions.DB_LOG_FILES, false);

    private static final boolean DB_LOG_FILES_PERFORMANCE = DbUtility.getBoolSysProp(LogDefinitions.DB_LOG_FILES_PERFORMANCE, false);

    private static Logger filesLogger = Logger.getLogger(LogDefinitions.DB_LOG_FILES);

    private static void OSTRACE(String format, Object... args) {
        if (DB_LOG_FILES) {
            DbUtility.log(filesLogger, format, args);
        }
    }

    public static final int DB_DEFAULT_SECTOR_SIZE = 512;

    private long timer_start = 0;

    private long timer_elapsed = 0;

    /**
     * @return
     */
    private long TIMER_ELAPSED() {
        return timer_elapsed;
    }

    /**
     * 
     */
    private void TIMER_END() {
        if (DB_LOG_FILES_PERFORMANCE) timer_elapsed = System.nanoTime() - timer_start;
    }

    /**
     * 
     */
    private void TIMER_START() {
        if (DB_LOG_FILES_PERFORMANCE) timer_start = System.nanoTime();
    }

    private static String pid = ManagementFactory.getRuntimeMXBean().getName();

    /**
     * @return
     */
    private String getpid() {
        return pid;
    }

    /**
     * @param lockType2
     * @return
     */
    private String locktypeName(LockType lockType) {
        return lockType != null ? lockType.name() : null;
    }

    /**
     * An instance of the following structure is allocated for each open inode
     * on each thread with a different process ID. A single inode can have
     * multiple file descriptors, so each unixFile structure contains a pointer
     * to an instance of this object and this object keeps a count of the number
     * of unixFile pointing to it.
     */
    private static class LockInfo {

        private LockType lockType = LockType.NONE;

        /** Number of SHARED locks held */
        private int sharedLockCount = 0;

        /** Number of pointers to this structure */
        private int numRef = 1;
    }

    ;

    /**
     * An instance of the following structure is allocated for each open inode.
     * This structure keeps track of the number of locks on that inode. If a
     * close is attempted against an inode that is holding locks, the close is
     * deferred until all locks clear by adding the file descriptor to be closed
     * to the pending list.
     */
    private static class OpenFile {

        /** Number of pointers to this structure */
        private int numRef = 1;

        /** Number of outstanding locks */
        private int numLock = 0;

        private Map<Thread, LockInfo> lockInfoMap = new ConcurrentHashMap<Thread, LockInfo>();

        /** Malloced space holding fd's awaiting a close() */
        private List<RandomAccessFile> pending = new ArrayList<RandomAccessFile>();
    }

    ;

    private static final Map<String, OpenFile> openFiles = new HashMap<String, OpenFile>();

    private FileType fileType;

    private Set<FileOpenPermission> permissions;

    private RandomAccessFile file;

    private File filePath;

    private String filePathResolved;

    private boolean noLock;

    private LockType lockType = LockType.NONE;

    private Map<LockType, FileLock> locks = new ConcurrentHashMap<LockType, FileLock>();

    private OpenFile openCount = null;

    private LockInfo lockInfo = null;

    private DbFileLockManager fileLockManager;

    /**
     * @param fileSystem
     * @param file
     * @param filePath
     * @param permissions
     * @param type
     * @param noLock
     */
    DbFile(final DbFileSystem fileSystem, final RandomAccessFile file, final File filePath, final FileType fileType, final Set<FileOpenPermission> permissions, final boolean noLock) {
        this.file = file;
        this.filePath = filePath;
        this.filePathResolved = filePath.getAbsolutePath();
        this.fileType = fileType;
        this.permissions = EnumSet.copyOf(permissions);
        this.noLock = noLock;
        this.fileLockManager = new DbFileLockManager(this.filePathResolved, file.getChannel());
        findLockInfo();
        OSTRACE("OPEN    %s\n", this.filePath);
    }

    public FileType getFileType() {
        return fileType;
    }

    public synchronized Set<FileOpenPermission> getPermissions() {
        HashSet<FileOpenPermission> permissionsCopy = new HashSet<FileOpenPermission>();
        for (FileOpenPermission permission : permissions) {
            permissionsCopy.add(permission);
        }
        return permissionsCopy;
    }

    public synchronized void close() throws DbException {
        if (null == file) return;
        synchronized (openFiles) {
            unlock(LockType.NONE);
            if (!noLock && null != openCount && null != openCount.lockInfoMap && openCount.lockInfoMap.size() > 0) {
                for (LockInfo l : openCount.lockInfoMap.values()) {
                    if (l.sharedLockCount > 0) {
                        openCount.pending.add(file);
                        return;
                    }
                }
            }
            releaseLockInfo();
            try {
                file.close();
            } catch (IOException e) {
                throw new DbException(DbErrorCode.IOERR, e);
            } finally {
                file = null;
            }
        }
        if (filePath != null && permissions.contains(FileOpenPermission.DELETEONCLOSE)) {
            if (!FileUtil.deleteFile(filePath)) {
                throw new DbIOException(IOErrorCode.IOERR_DELETE, String.format("Can't delete file '%s'", filePath.getPath()));
            }
        }
        OSTRACE("CLOSE   %s\n", this.filePath);
    }

    public synchronized int read(IMemoryPointer buffer, int amount, long offset) throws DbIOException {
        assert (amount > 0);
        assert (offset >= 0);
        assert (buffer != null);
        assert (buffer.remaining() >= amount);
        assert (file != null);
        try {
            TIMER_START();
            final int read = buffer.readFromFile(file, offset, amount);
            TIMER_END();
            OSTRACE("READ %s %5d %7d %d\n", this.filePath, read, offset, TIMER_ELAPSED());
            return read < 0 ? 0 : read;
        } catch (IOException e) {
            throw new DbIOException(IOErrorCode.IOERR_READ, e);
        }
    }

    public synchronized void write(IMemoryPointer buffer, int amount, long offset) throws DbIOException {
        assert (amount > 0);
        assert (offset >= 0);
        assert (buffer != null);
        assert (buffer.remaining() >= amount);
        assert (file != null);
        try {
            TIMER_START();
            final int write = buffer.writeToFile(file, offset, amount);
            TIMER_END();
            OSTRACE("WRITE %s %5d %7d %d\n", this.filePath, write, offset, TIMER_ELAPSED());
        } catch (IOException e) {
            throw new DbIOException(IOErrorCode.IOERR_WRITE, e);
        }
    }

    public synchronized void truncate(long size) throws DbIOException {
        assert (size >= 0);
        assert (file != null);
        try {
            file.setLength(size);
        } catch (IOException e) {
            throw new DbIOException(IOErrorCode.IOERR_TRUNCATE, e);
        }
    }

    public synchronized void sync(Set<SyncFlags> syncFlags) throws DbIOException {
        assert (file != null);
        try {
            OSTRACE("SYNC    %s\n", this.filePath);
            boolean syncMetaData = syncFlags != null && syncFlags.contains(SyncFlags.NORMAL);
            file.getChannel().force(syncMetaData);
        } catch (IOException e) {
            throw new DbIOException(IOErrorCode.IOERR_FSYNC, e);
        }
    }

    public synchronized long fileSize() throws DbException {
        assert (file != null);
        try {
            return file.getChannel().size();
        } catch (IOException e) {
            throw new DbException(DbErrorCode.IOERR, e);
        }
    }

    public synchronized LockType getLockType() {
        return lockType;
    }

    public synchronized boolean lock(final LockType lockType) throws DbIOException {
        assert (lockType != null);
        assert (file != null);
        if (noLock) return false;
        OSTRACE("LOCK    %s %s was %s(%s,%d) pid=%s\n", this.filePath, locktypeName(lockType), locktypeName(this.lockType), locktypeName(lockInfo.lockType), lockInfo.sharedLockCount, getpid());
        if (this.lockType.compareTo(lockType) > 0) {
            OSTRACE("LOCK    %s %s ok (already held)\n", this.filePath, locktypeName(lockType));
            return false;
        }
        assert (lockType != LockType.PENDING);
        assert (this.lockType != LockType.NONE || lockType == LockType.SHARED);
        assert (lockType != LockType.RESERVED || this.lockType == LockType.SHARED);
        assert (lockInfo != null);
        try {
            synchronized (openFiles) {
                if (this.lockType != lockInfo.lockType && (LockType.PENDING.compareTo(lockInfo.lockType) <= 0 || LockType.SHARED.compareTo(lockType) < 0)) {
                    return false;
                }
                if (lockType == LockType.SHARED && lockInfo.sharedLockCount > 0 && (lockInfo.lockType == LockType.SHARED || lockInfo.lockType == LockType.RESERVED)) {
                    this.lockType = LockType.SHARED;
                    lockInfo.sharedLockCount++;
                    openCount.numLock++;
                    return true;
                }
                if (lockType == LockType.SHARED || (lockType == LockType.EXCLUSIVE && this.lockType.compareTo(LockType.PENDING) < 0)) {
                    if (lockType != LockType.SHARED) {
                        final FileLock sharedLock = locks.get(LockType.SHARED);
                        if (null != sharedLock) {
                            sharedLock.release();
                            locks.remove(LockType.SHARED);
                        }
                    }
                    if (!locks.containsKey(LockType.PENDING)) {
                        final FileLock pendingLock = fileLockManager.tryLock(PENDING_BYTE, 1, lockType == LockType.SHARED);
                        if (null == pendingLock) return false;
                        locks.put(LockType.PENDING, pendingLock);
                    }
                }
                if (lockType == LockType.SHARED) {
                    final FileLock sharedLock = fileLockManager.tryLock(SHARED_FIRST, SHARED_SIZE, true);
                    locks.put(LockType.SHARED, sharedLock);
                    final FileLock pendingLock = locks.get(LockType.PENDING);
                    if (null != pendingLock) {
                        pendingLock.release();
                        locks.remove(LockType.PENDING);
                    }
                    if (null == sharedLock) return false;
                    this.lockType = LockType.SHARED;
                    openCount.numLock++;
                    lockInfo.sharedLockCount = 1;
                } else if (lockType == LockType.EXCLUSIVE && lockInfo.sharedLockCount > 1) {
                    return false;
                } else {
                    assert (LockType.NONE != this.lockType);
                    switch(lockType) {
                        case RESERVED:
                            final FileLock reservedLock = fileLockManager.tryLock(RESERVED_BYTE, 1, false);
                            if (null == reservedLock) return false;
                            locks.put(LockType.RESERVED, reservedLock);
                            break;
                        case EXCLUSIVE:
                            final FileLock exclusiveLock = fileLockManager.tryLock(SHARED_FIRST, SHARED_SIZE, false);
                            if (null == exclusiveLock) {
                                this.lockType = LockType.PENDING;
                                lockInfo.lockType = LockType.PENDING;
                                return false;
                            }
                            locks.put(LockType.EXCLUSIVE, exclusiveLock);
                            break;
                        default:
                            assert (false);
                    }
                }
                this.lockType = lockType;
                lockInfo.lockType = lockType;
                return true;
            }
        } catch (IOException e) {
            throw new DbIOException(IOErrorCode.IOERR_LOCK, e);
        } finally {
            OSTRACE("LOCK    %s %s %s\n", this.filePath, locktypeName(lockType), this.lockType == lockType ? "ok" : "failed");
        }
    }

    public synchronized boolean unlock(final LockType lockType) throws DbIOException {
        assert (lockType != null);
        assert (file != null);
        if (noLock) return false;
        OSTRACE("UNLOCK  %s %s was %s(%s,%s) pid=%s\n", this.filePath, locktypeName(lockType), locktypeName(this.lockType), locktypeName(lockInfo.lockType), lockInfo.sharedLockCount, getpid());
        assert (LockType.SHARED.compareTo(lockType) >= 0);
        if (this.lockType.compareTo(lockType) <= 0) return true;
        synchronized (openFiles) {
            assert (lockInfo != null);
            assert (lockInfo.sharedLockCount > 0);
            try {
                if (LockType.SHARED.compareTo(this.lockType) < 0) {
                    if (LockType.SHARED == lockType) {
                        final FileLock exclusiveLock = locks.get(LockType.EXCLUSIVE);
                        if (null != exclusiveLock) {
                            if (exclusiveLock.isValid()) exclusiveLock.release();
                            locks.remove(LockType.EXCLUSIVE);
                        }
                        if (null == locks.get(LockType.SHARED)) {
                            final FileLock sharedLock = fileLockManager.lock(SHARED_FIRST, SHARED_SIZE, true);
                            if (null == sharedLock) return false;
                            locks.put(LockType.SHARED, sharedLock);
                        }
                    }
                    final FileLock reservedLock = locks.get(LockType.RESERVED);
                    if (null != reservedLock) {
                        if (reservedLock.isValid()) reservedLock.release();
                        locks.remove(LockType.RESERVED);
                    }
                    final FileLock pendingLock = locks.get(LockType.PENDING);
                    if (null != pendingLock) {
                        if (pendingLock.isValid()) pendingLock.release();
                        locks.remove(LockType.PENDING);
                    }
                    lockInfo.lockType = LockType.SHARED;
                }
                if (lockType == LockType.NONE) {
                    lockInfo.sharedLockCount--;
                    if (lockInfo.sharedLockCount == 0) {
                        lockInfo.sharedLockCount = 1;
                        for (final FileLock l : locks.values()) {
                            if (l.isValid()) l.release();
                        }
                        locks.clear();
                        lockInfo.sharedLockCount = 0;
                        this.lockType = LockType.NONE;
                    }
                    openCount.numLock--;
                    assert (openCount.numLock >= 0);
                    if (openCount.numLock == 0 && null != openCount.pending && openCount.pending.size() > 0) {
                        for (final RandomAccessFile f : openCount.pending) {
                            f.close();
                        }
                        openCount.pending.clear();
                    }
                }
                this.lockType = lockType;
            } catch (IOException e) {
                throw new DbIOException(IOErrorCode.IOERR_LOCK, e);
            }
        }
        return true;
    }

    public synchronized boolean checkReservedLock() {
        boolean reserved = false;
        try {
            if (noLock) return false;
            if (null == file) return false;
            if (null == lockInfo) return false;
            synchronized (openFiles) {
                if (LockType.SHARED.compareTo(lockInfo.lockType) < 0) return true;
                try {
                    final FileLock reservedLock = fileLockManager.tryLock(RESERVED_BYTE, 1, false);
                    if (null == reservedLock) {
                        reserved = true;
                        return true;
                    }
                    reservedLock.release();
                } catch (IOException e) {
                }
            }
            return false;
        } finally {
            OSTRACE("TEST WR-LOCK %s %b\n", this.filePath, reserved);
        }
    }

    public int sectorSize() {
        return DB_DEFAULT_SECTOR_SIZE;
    }

    static final Set<DeviceCharacteristics> noDeviceCharacteristircs = DbUtility.noneOf(DeviceCharacteristics.class);

    public Set<DeviceCharacteristics> deviceCharacteristics() {
        return noDeviceCharacteristircs;
    }

    private synchronized void findLockInfo() {
        synchronized (openFiles) {
            if (null == openCount) {
                final OpenFile fileOpenCount = openFiles.get(filePathResolved);
                if (null != fileOpenCount) {
                    openCount = fileOpenCount;
                    openCount.numRef++;
                } else {
                    openCount = new OpenFile();
                    openFiles.put(filePathResolved, openCount);
                }
            }
            final LockInfo fileLockInfo = openCount.lockInfoMap.get(Thread.currentThread());
            if (null != fileLockInfo) {
                lockInfo = fileLockInfo;
                lockInfo.numRef++;
            } else {
                lockInfo = new LockInfo();
                openCount.lockInfoMap.put(Thread.currentThread(), lockInfo);
            }
        }
    }

    /**
     * 
     */
    private void releaseLockInfo() {
        synchronized (openFiles) {
            if (null != lockInfo) {
                lockInfo.numRef--;
                if (0 == lockInfo.numRef) {
                    if (null != openCount) {
                        openCount.lockInfoMap.remove(Thread.currentThread());
                    }
                    this.lockInfo = null;
                }
            }
            if (null != openCount) {
                openCount.numRef--;
                if (0 == openCount.numRef) {
                    openFiles.remove(filePathResolved);
                    this.openCount = null;
                }
            }
        }
    }

    public boolean isMemJournal() {
        return false;
    }
}

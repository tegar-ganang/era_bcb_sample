package net.sf.joafip.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import net.sf.joafip.Fortest;
import net.sf.joafip.NotStorableClass;
import net.sf.joafip.logger.JoafipLogger;

/**
 * 
 * @author luc peuvrier
 * 
 */
@NotStorableClass
public final class FilePersistenceLock {

    private static final JoafipLogger LOGGER = JoafipLogger.getLogger(FilePersistenceLock.class);

    private static final String LOCK = ".lock";

    private static final FilePersistenceLock INSTANCE = new FilePersistenceLock();

    private final Map<File, Exception> lockedDirMap = new HashMap<File, Exception>();

    private final Map<File, FileOutputStream> lockFileOutputStreamMap = new HashMap<File, FileOutputStream>();

    private final Map<File, FileLock> fileLockMap = new HashMap<File, FileLock>();

    public static FilePersistenceLock getInstance() {
        return INSTANCE;
    }

    private FilePersistenceLock() {
        super();
    }

    /**
	 * add lock on used file
	 * 
	 * @param file
	 *            the used file
	 * @throws FilePersistenceException
	 *             file already in use
	 */
    public void lock(final File file) throws FilePersistenceException {
        synchronized (this) {
            final Exception lockerTrace = new Exception("locker");
            final Exception previous = lockedDirMap.put(file, lockerTrace);
            if (previous != null) {
                throw new FilePersistenceException("File " + file + " already in use", previous);
            }
            file.mkdirs();
            final File lockFile = new File(file, LOCK);
            try {
                final FileOutputStream fileOutputStream = new FileOutputStream(lockFile);
                final FileChannel channel = fileOutputStream.getChannel();
                final FileLock fileLock = channel.tryLock();
                if (fileLock == null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException exception) {
                        LOGGER.error(exception);
                    }
                    throw new FilePersistenceException("File " + file + " already in use");
                }
                fileLockMap.put(lockFile, fileLock);
                lockFileOutputStreamMap.put(lockFile, fileOutputStream);
            } catch (IOException exception) {
                throw new FilePersistenceException(exception);
            }
        }
    }

    /**
	 * remove lock on no more used file
	 * 
	 * @param file
	 *            the no more used file
	 * @throws FilePersistenceException
	 *             file was not in use
	 */
    public void unlock(final File file) throws FilePersistenceException {
        synchronized (this) {
            if (lockedDirMap.remove(file) == null) {
                throw new FilePersistenceException("File " + file + " was not in use");
            }
            final File lockFile = new File(file, LOCK);
            final FileOutputStream fileOutputStream = lockFileOutputStreamMap.get(lockFile);
            try {
                fileLockMap.get(lockFile).release();
            } catch (IOException exception) {
                LOGGER.warn("unlocking " + lockFile, exception);
            }
            try {
                fileOutputStream.close();
            } catch (IOException exception) {
                LOGGER.warn("unlocking " + lockFile, exception);
            }
        }
    }

    @Fortest
    public static void unlockAll() {
        INSTANCE.localUnlockAll();
    }

    private void localUnlockAll() {
        synchronized (this) {
            for (FileLock fileLock : fileLockMap.values()) {
                try {
                    fileLock.release();
                } catch (Exception exception) {
                    LOGGER.warn("unlocking", exception);
                }
            }
            for (FileOutputStream fileOutputStream : lockFileOutputStreamMap.values()) {
                try {
                    fileOutputStream.close();
                } catch (IOException exception) {
                    LOGGER.warn("unlocking", exception);
                }
            }
            lockedDirMap.clear();
            lockFileOutputStreamMap.clear();
            fileLockMap.clear();
        }
    }
}

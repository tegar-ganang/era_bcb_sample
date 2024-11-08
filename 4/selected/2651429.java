package net.sf.poormans.tool;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import net.sf.poormans.Constants;

/**
 * A very simple file locker. <br>
 * {@link #lock()} tries to create a lock file. If it's already locked, {@link #isLocked} is false.<br>
 * All possible errors are catched, unless if in {@link #lock()} an unexpected error is happened. Then 
 * a {@link RuntimeException} will be thrown.
 *
 * @version $Id: Locker.java 1912 2010-01-16 18:11:58Z th-schwarz $
 * @author <a href="mailto:th-schwarz@users.sourceforge.net">Thilo Schwarz</a>
 */
public class Locker {

    /** File to lock for testing, if another instance is running. */
    private File lockFile = new File(Constants.TEMP_DIR, "poormans.lck");

    /** Need for file locking. */
    private static FileChannel fileChannel = null;

    /** Need for file locking. */
    private FileLock fileLock = null;

    /** True, if another instance of poormans is running. */
    private boolean isLocked = false;

    public Locker(File lockFile) {
        this.lockFile = lockFile;
    }

    /**
	 * Creates a file based lock. 
	 * 
	 * @throws RuntimeException, if an exception is happened while creating the lock
	 * @return <code>true</code>, if a lock already exists, otherwise false.
	 */
    public boolean lock() {
        try {
            if (!lockFile.exists()) {
                if (!lockFile.getParentFile().exists()) lockFile.getParentFile().mkdirs();
                lockFile.createNewFile();
                fileChannel = new RandomAccessFile(lockFile, "rw").getChannel();
                fileLock = fileChannel.lock();
            } else {
                fileChannel = new RandomAccessFile(lockFile, "rw").getChannel();
                try {
                    fileLock = fileChannel.tryLock();
                    if (fileLock == null) isLocked = true;
                } catch (Exception e) {
                    isLocked = true;
                }
            }
        } catch (Exception e) {
            System.out.println(e);
            throw new RuntimeException("Error while creating the lock [" + lockFile.getPath() + "]: " + e.getMessage(), e);
        }
        return isLocked;
    }

    public void unlock() {
        try {
            fileLock.release();
            fileChannel.close();
            lockFile.delete();
            isLocked = false;
        } catch (Exception e) {
        }
    }

    public boolean isLocked() {
        return isLocked;
    }
}

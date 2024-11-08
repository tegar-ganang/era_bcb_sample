package org.kablink.util;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Random;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LockFile {

    protected Log logger = LogFactory.getLog(getClass());

    private Random random = null;

    private FileLock fileLock = null;

    private FileChannel fileChannel = null;

    private File lockFile = null;

    public LockFile(File lockFile) {
        this.lockFile = lockFile;
    }

    public boolean getLock() {
        int tryCount = 0;
        if (!lockFile.exists()) {
            try {
                lockFile.createNewFile();
            } catch (Exception e) {
                logger.info(e.toString());
            }
        }
        try {
            fileChannel = new RandomAccessFile(lockFile, "rw").getChannel();
            while (tryCount < 5) {
                fileLock = fileChannel.tryLock();
                if (fileLock != null) return true;
                if (random == null) random = new Random();
                Thread.sleep(random.nextInt(20));
                tryCount++;
            }
            if (lockFile.exists()) {
                lockFile.delete();
                return getLock();
            }
        } catch (Exception ignore) {
        }
        return false;
    }

    public boolean releaseLock() {
        try {
            fileLock.release();
            fileChannel.close();
            return true;
        } catch (Exception e) {
            logger.info("Couldn't release lock " + lockFile);
        } finally {
            try {
                fileChannel.close();
            } catch (Exception ignore) {
            }
        }
        return false;
    }

    public void releaseLockIfValid() {
        if (fileLock.isValid()) releaseLock();
    }
}

package com.leclercb.commons.api.utils;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.logging.Level;
import com.leclercb.commons.api.logger.ApiLogger;

public final class SingleInstanceUtils {

    private SingleInstanceUtils() {
    }

    public static boolean isSingleInstance(final String lockFile) {
        try {
            final File file = new File(lockFile);
            final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            final FileLock fileLock = randomAccessFile.getChannel().tryLock();
            if (fileLock != null) {
                Runtime.getRuntime().addShutdownHook(new Thread() {

                    @Override
                    public void run() {
                        try {
                            fileLock.release();
                            randomAccessFile.close();
                            file.delete();
                        } catch (Exception e) {
                            ApiLogger.getLogger().log(Level.WARNING, "Unable to remove lock file: " + lockFile, e);
                        }
                    }
                });
                return true;
            }
        } catch (Exception e) {
            ApiLogger.getLogger().log(Level.WARNING, "Unable to create and/or lock file: " + lockFile, e);
        }
        return false;
    }
}

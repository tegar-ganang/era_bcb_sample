package net.sourceforge.hachikaduki.ui;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.EventObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.application.Application;

public class HachikadukiLocker implements Application.ExitListener {

    private static final Logger logger = Logger.getLogger(HachikadukiLocker.class.getName());

    private File lockFile = null;

    private FileChannel lockChannel = null;

    private FileLock lock = null;

    public HachikadukiLocker() {
    }

    public boolean lock() {
        lockFile = new File(".lock");
        if (!lockFile.exists()) {
            try {
                boolean created = lockFile.createNewFile();
                if (!created) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Lock file is not created");
                    }
                    return false;
                }
            } catch (IOException e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.throwing(HachikadukiLocker.class.getName(), "lock", e);
                }
                return false;
            }
        }
        try {
            lockChannel = new RandomAccessFile(lockFile, "rw").getChannel();
            lock = lockChannel.tryLock();
            if (lock == null) {
                return false;
            }
            return true;
        } catch (IOException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.throwing(HachikadukiLocker.class.getName(), "lock", e);
            }
            return false;
        } catch (OverlappingFileLockException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.throwing(HachikadukiLocker.class.getName(), "lock", e);
            }
            return false;
        }
    }

    public boolean canExit(EventObject event) {
        return true;
    }

    public void willExit(EventObject event) {
        if (lock != null) {
            try {
                lock.release();
            } catch (IOException e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.throwing(HachikadukiLocker.class.getName(), "willExit", e);
                }
            }
        }
        if (lockChannel != null) {
            try {
                lockChannel.close();
            } catch (IOException e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.throwing(HachikadukiLocker.class.getName(), "willExit", e);
                }
            }
        }
        if (lockFile != null) {
            lockFile.delete();
        }
    }
}

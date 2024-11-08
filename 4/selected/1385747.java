package org.jiopi.ibean.bootstrap.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.logging.Level;
import org.jiopi.ibean.bootstrap.BootstrapConstants;
import org.jiopi.ibean.bootstrap.Closeable;
import org.jiopi.ibean.bootstrap.CloseableManager;
import org.jiopi.ibean.loader.log.LoaderLogUtil;
import org.jiopi.ibean.share.ShareUtil.MyFileLock;
import org.jiopi.ibean.share.ShareUtil.FileUtil;

/**
 * 
 * 获得唯一目录锁
 * 
 * 如果已被锁定,则尝试锁定 dir.0 dir.1
 * 
 * 最大锁定到 MAX_LOCK_NUM
 * 
 * @since 2010.4.18
 *
 */
public class DirLoker {

    private static final int MAX_LOCK_NUM = 10;

    public static File getUniqueDirLock(String lockDirPath) throws IOException {
        File lockDir = FileUtil.confirmDir(lockDirPath, true);
        if (!lockDir.isDirectory()) return null;
        File newLockDir = lockDir;
        for (int i = -1; i <= MAX_LOCK_NUM; i++) {
            if (i > -1) {
                String newName = lockDir.getName() + "." + Integer.toString(i);
                newLockDir = new File(lockDir.getParentFile(), newName);
                newLockDir = FileUtil.confirmDir(newLockDir.getAbsolutePath(), true);
            }
            MyFileLock fl = getUniqueDirLock(newLockDir);
            if (fl != null) {
                LockClose lc = new LockClose(fl);
                CloseableManager.register(lc);
                return newLockDir;
            }
        }
        return null;
    }

    private static MyFileLock getUniqueDirLock(File lockDir) throws IOException {
        if (!lockDir.isDirectory()) return null;
        String lockFileName = lockDir.getName();
        lockFileName = "." + lockFileName;
        File lockFile = new File(lockDir.getParentFile(), lockFileName);
        FileUtil.createNewFile(lockFile.getAbsolutePath(), false);
        return tryLockFile(lockFile);
    }

    private static MyFileLock tryLockFile(File lockFile) throws IOException {
        if (!lockFile.isFile()) return null;
        RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
        FileLock fl = null;
        try {
            fl = raf.getChannel().tryLock();
        } catch (Exception e) {
        }
        if (fl != null) {
            return new MyFileLock(raf, fl, lockFile);
        }
        return null;
    }

    private static class LockClose implements Closeable {

        private final MyFileLock fl;

        public LockClose(MyFileLock fl) {
            this.fl = fl;
        }

        public void close() {
            try {
                fl.release();
            } catch (IOException e) {
                LoaderLogUtil.logExceptionTrace(BootstrapConstants.bootstrapLogger, Level.WARNING, e);
            }
        }
    }
}

package org.vramework.commons.io;

import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import org.vramework.commons.config.VConf;
import org.vramework.commons.exceptions.VRuntimeException;

/**
 * VFile utilities.
 * 
 * @author tmahring
 * 
 */
public class Files {

    /**
   * Same as {@link VFile#compareTimestamp(String, long)} but creates a file for the <code>absoulteFilePath1</code> and 2
   * autoamtically.
   * 
   * @param absoulteFilePath1
   * @param absoulteFilePath2
   * @param ignoreDiffInMillis
   * @return See {@link VFile#compareTimestamp(String, long)}
   */
    public static int compareTimestamps(String absoulteFilePath1, String absoulteFilePath2, long ignoreDiffInMillis) {
        VFile vFile = new VFile(absoulteFilePath1);
        return vFile.compareTimestamp(absoulteFilePath2, ignoreDiffInMillis);
    }

    /**
   * Tries to lock it until it can do so. Then waits for the given millisecs. This is intended to make sure that
   * different processes don't perform certain operations at the same time. <br />
   * <strong>Note:</strong> Uses the temp directory returned by System.getProperty("java.io.tmpdir") to locate or create
   * the file. The temp dir is usually user dependend. So processes running under different user contextes won't lock
   * each other.
   * 
   * @param fileName
   *          The name of the file to lock in the temp dir..
   * @param milliSecs
   *          The time to wait
   */
    public static void lockTempFileAndWait(String fileName, long milliSecs) {
        String tempdir = System.getProperty("java.io.tmpdir");
        String sep = VConf.getFileSep();
        if (!(tempdir.endsWith("/") || tempdir.endsWith("\\"))) tempdir = tempdir + sep;
        VFile f = new VFile(tempdir + fileName);
        try {
            f.createNewFile();
            lockFileAndWait(f, milliSecs);
        } catch (Exception e) {
            throw new VRuntimeException(e);
        }
    }

    /**
   * Tries to lock the file until successful. Then, it waits for milliSecs and releases the lock again.
   * 
   * @param f
   * @param milliSecs
   */
    public static void lockFileAndWait(VFile f, long milliSecs) {
        FileLock lock = null;
        try {
            FileChannel ch = new FileOutputStream(f).getChannel();
            lock = ch.tryLock();
            while (lock == null) {
                Thread.sleep(100);
                lock = ch.tryLock();
            }
            Thread.sleep(milliSecs);
            lock.release();
        } catch (Exception e) {
            throw new VRuntimeException(e);
        } finally {
            if (lock != null) {
                try {
                    lock.release();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
   * @param fileName
   * @return See {@link VFile#delete()}
   */
    public static boolean delete(String fileName) {
        VFile vFile = new VFile(fileName);
        return vFile.delete();
    }

    /**
   * See {@link #deleteDir(java.io.File)}.
   * 
   * @param dir
   * @return See {@link #deleteDir(java.io.File)}.
   */
    public static boolean deleteDir(String dir) {
        return deleteDir(new java.io.File(dir));
    }

    /**
   * @param fileName
   * @return True: VFile exists
   */
    public static boolean exists(String fileName) {
        VFile vFile = new VFile(fileName);
        return vFile.exists();
    }

    /**
   * @param allegedDirectoryName
   * @return True: The passed alleged directory is really one.
   */
    public static boolean isDirectory(String allegedDirectoryName) {
        VFile vFile = new VFile(allegedDirectoryName);
        return vFile.isDirectory();
    }

    /**
   * Deletes all files and subdirectories under dir. If a deletion fails, the method stops attempting to delete and
   * returns false. See http://javaalmanac.com/egs/java.io/DeleteDir.html
   * 
   * @param dir
   * @return true: All files and the dir itself were deleted; false: one of the deletions failed.
   */
    public static boolean deleteDir(java.io.File dir) {
        if (!dir.exists()) {
            return true;
        }
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                String child = children[i];
                boolean success = deleteDir(new VFile(dir, child));
                if (!success) {
                    return false;
                }
            }
        }
        boolean success = dir.delete();
        if (!success) {
            System.out.println("@@@@@ Could not delete: " + dir);
        }
        return success;
    }
}

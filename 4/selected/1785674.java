package com.android.cts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;

/**
 * Host lock to make sure just one CTS host is running.
 */
public class HostLock {

    private static FileOutputStream mFileOs;

    private static FileLock mLock;

    private static File mFile;

    /**
     * Lock the host.
     *
     * @return If succeed in locking the host, return true; else , return false.
     */
    public static boolean lock() {
        try {
            String tmpdir = System.getProperty("java.io.tmpdir");
            mFile = new File(tmpdir + File.separator + "ctsLockFile.txt");
            mFileOs = new FileOutputStream(mFile);
            mLock = mFileOs.getChannel().tryLock();
            if (mLock != null) {
                return true;
            } else {
                return false;
            }
        } catch (FileNotFoundException e1) {
            return false;
        } catch (IOException e1) {
            return false;
        }
    }

    /**
     * Release the host lock.
     */
    public static void release() {
        try {
            if (mLock != null) {
                mLock.release();
            }
            if (mFileOs != null) {
                mFileOs.close();
            }
            mFile.delete();
        } catch (IOException e) {
        }
    }
}

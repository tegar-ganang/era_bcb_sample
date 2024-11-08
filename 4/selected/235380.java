package ioabs;

import java.io.*;
import java.nio.channels.*;

public class JustOneLock {

    private String appName;

    private File file;

    private FileChannel channel;

    private FileLock lock;

    public JustOneLock(String appName) {
        this.appName = appName;
    }

    public boolean isAppActive() {
        try {
            file = new File(System.getProperty("user.home"), appName + ".tmp");
            channel = new RandomAccessFile(file, "rw").getChannel();
            try {
                lock = channel.tryLock();
            } catch (OverlappingFileLockException e) {
                closeLock();
                return true;
            }
            if (lock == null) {
                closeLock();
                return true;
            }
            Runtime.getRuntime().addShutdownHook(new Thread() {

                public void run() {
                    closeLock();
                    deleteFile();
                }
            });
            return false;
        } catch (Exception e) {
            closeLock();
            return true;
        }
    }

    private void closeLock() {
        try {
            lock.release();
        } catch (Exception e) {
        }
        try {
            channel.close();
        } catch (Exception e) {
        }
    }

    private void deleteFile() {
        try {
            file.delete();
        } catch (Exception e) {
        }
    }
}

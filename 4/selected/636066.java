package com.sqltablet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class SqlProFileLock {

    private static FileChannel channel = null;

    private static FileLock lock = null;

    public static boolean lock() {
        final String path = SqlTablet.getLocalPath();
        final String filename = "sqltablet.lock";
        File file = new File(path + filename);
        file.deleteOnExit();
        try {
            channel = new RandomAccessFile(file, "rw").getChannel();
        } catch (FileNotFoundException e) {
            RemoteLogger.getInstance().logError(e);
            return false;
        }
        try {
            lock = channel.lock();
        } catch (IOException e) {
            RemoteLogger.getInstance().logError(e);
            return false;
        }
        return true;
    }

    public static boolean release() {
        try {
            lock.release();
            channel.close();
            return true;
        } catch (IOException e) {
            RemoteLogger.getInstance().logError(e);
            return false;
        }
    }
}

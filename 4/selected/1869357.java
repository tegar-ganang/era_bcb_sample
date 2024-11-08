package com.siberhus.commons.io;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class FileLocker {

    private File file;

    private FileChannel fileChannel;

    private FileLock fileLock;

    boolean canLock;

    public FileLocker() {
    }

    public boolean lock(File file) {
        return lock(file, true);
    }

    public boolean lock(File file, boolean createNewIfNotExist) {
        try {
            this.file = file;
            if (createNewIfNotExist && !file.exists()) {
                file.createNewFile();
            }
            this.fileChannel = new RandomAccessFile(file, "rw").getChannel();
            fileLock = fileChannel.tryLock();
        } catch (Exception e) {
            canLock = false;
            e.printStackTrace();
        }
        if (fileLock == null) {
            canLock = false;
        } else {
            canLock = true;
        }
        return canLock;
    }

    public boolean canLock() {
        return canLock;
    }

    public File getFile() {
        return file;
    }

    public FileChannel getFileChannel() {
        return fileChannel;
    }

    public FileLock getFileLock() {
        return fileLock;
    }

    public void release() {
        try {
            if (fileLock != null) {
                fileLock.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (fileChannel != null) {
                fileChannel.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        FileLocker fileLocker = new FileLocker();
        boolean canLock = fileLocker.lock(new File("lock"), true);
        System.out.println(canLock);
    }
}

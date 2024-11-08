package com.magicpwd._comn.apps;

import com.magicpwd.__i.lock.ILockClient;

/**
 *
 * @author Amon
 */
public class FileReader implements ILockClient {

    private java.io.File file;

    private java.nio.channels.FileChannel fc;

    private java.nio.channels.FileLock fl;

    public FileReader() {
    }

    public FileReader(java.io.File file) {
        this.file = file;
    }

    @Override
    public boolean canRead() {
        if (file == null) {
            return false;
        }
        if (file.exists()) {
            file.delete();
        }
        try {
            fc = new java.io.RandomAccessFile(file, "rw").getChannel();
            fl = fc.tryLock();
            if (fl == null) {
                fc.close();
                return false;
            }
            return true;
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            return false;
        }
    }

    /**
     * @return the file
     */
    public java.io.File getFile() {
        return file;
    }

    /**
     * @param file the file to set
     */
    public void setFile(java.io.File file) {
        this.file = file;
    }
}

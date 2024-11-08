package org.gocha.textbox;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Простой файл для межпроц. взаимодействия
 * @author gocha
 */
public class PIDFile {

    private RandomAccessFile file = null;

    public PIDFile(RandomAccessFile pidFile) {
        if (pidFile == null) {
            throw new IllegalArgumentException("pidFile == null");
        }
        this.file = pidFile;
    }

    private FileLock fLock = null;

    public boolean lock() {
        try {
            if (fLock == null) {
                fLock = file.getChannel().tryLock(0, 1, false);
                return fLock != null;
            }
            return true;
        } catch (IOException ex) {
            Logger.getLogger(PIDFile.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public boolean inputExists() {
        return getInputSize() > 0;
    }

    public int getInputSize() {
        try {
            if (file.length() < 5) {
                return -1;
            }
            file.seek(1);
            int input = file.readInt();
            return input;
        } catch (IOException ex) {
            Logger.getLogger(PIDFile.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
    }

    public boolean writeToInput(byte[] data, boolean lock) {
        try {
            FileLock fl = null;
            if (lock) {
                fl = file.getChannel().lock(1, 4 + data.length, true);
            }
            file.seek(1);
            file.writeInt(data.length);
            file.seek(5);
            file.write(data);
            if (lock) fl.release();
            return true;
        } catch (IOException ex) {
            Logger.getLogger(PIDFile.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public byte[] readInput() {
        try {
            int size = getInputSize();
            if (size < 1) {
                return null;
            }
            file.seek(5);
            byte[] buff = new byte[size];
            int rd = file.read(buff);
            if (rd < 0) return null;
            if (rd == 0) return new byte[] {};
            if (rd < buff.length) return Arrays.copyOf(buff, rd);
            if (rd == buff.length) return buff;
            Logger.getLogger(PIDFile.class.getName()).log(Level.SEVERE, "read more then need");
            return buff;
        } catch (IOException ex) {
            Logger.getLogger(PIDFile.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public boolean clearInput(boolean lock) {
        try {
            FileLock fl = null;
            if (lock) fl = file.getChannel().lock(1, 4, true);
            file.seek(1);
            file.writeInt(0);
            if (lock) fl.release();
            return true;
        } catch (IOException ex) {
            Logger.getLogger(PIDFile.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public boolean isLocked() {
        return fLock != null;
    }

    public boolean unlock() {
        if (fLock == null) return true;
        try {
            fLock.release();
            fLock = null;
            return true;
        } catch (IOException ex) {
            Logger.getLogger(PIDFile.class.getName()).log(Level.SEVERE, null, ex);
            fLock = null;
            return false;
        }
    }
}

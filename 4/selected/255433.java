package org.sqlsplatter.tinyhorror.other;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import org.sqlsplatter.tinyhorror.other.exceptions.SQLIOException;

/**
 * Represents a locked file.
 */
public class LockedFile {

    private File fileHnd;

    private RandomAccessFile file;

    private FileChannel channel;

    private FileLock lock;

    private long savePosition;

    /**
	 * Interface to physical file.
	 *
	 * @param dataDir
	 *            data directory handle.
	 * @param fileName
	 *            file name.
	 * @param autoCreate
	 *            when the file is not present, if true, it will be created,
	 *            otherwise an exception will be thrown.
	 */
    public LockedFile(File dataDir, String fileName, boolean autoCreate) throws SQLIOException {
        this(new File(dataDir, fileName), autoCreate);
    }

    /**
	 * Interface to a physical file.
	 *
	 * @param fileHnd
	 *            data file handle.
	 * @param autoCreate
	 *            when the file is not present, if true, it will be created,
	 *            otherwise an exception will be thrown.
	 */
    public LockedFile(File fileHnd, boolean autoCreate) throws SQLIOException {
        this.fileHnd = fileHnd;
        lockAndInit(fileHnd, autoCreate);
        savePosition = 0;
    }

    private void lockAndInit(File fileHnd, boolean autoCreate) throws SQLIOException {
        try {
            if (!fileHnd.exists()) {
                if (autoCreate) {
                    fileHnd.createNewFile();
                } else {
                    throw new SQLIOException("File not found: " + fileHnd);
                }
            }
            file = new RandomAccessFile(fileHnd, "rw");
            channel = file.getChannel();
            lock = channel.tryLock();
        } catch (IOException e) {
            throw new SQLIOException(e);
        }
    }

    public void unlockAndClose() throws SQLIOException {
        try {
            lock.release();
            channel.close();
            file.close();
        } catch (IOException e) {
            throw new SQLIOException(e);
        }
    }

    /**
	 * Unlock the file and delete it.
	 */
    public void delete() throws SQLIOException {
        unlockAndClose();
        if (!fileHnd.delete()) {
            throw new SQLIOException("Cannot delete table file: " + fileHnd);
        }
    }

    /**
	 * Rename to new name, in the same directory.
	 *
	 * @param newName
	 *            new name, without neither path nor extension.
	 */
    public void rename(String newName) throws SQLIOException {
        unlockAndClose();
        File newFile = new File(fileHnd.getParent(), newName);
        boolean renameOk = fileHnd.renameTo(newFile);
        if (renameOk) {
            fileHnd = newFile;
            lockAndInit(fileHnd, false);
        } else {
            lockAndInit(fileHnd, false);
            throw new SQLIOException("Cannot rename table file.");
        }
    }

    /**
	 * Read a byte.
	 */
    public int read() throws SQLIOException {
        try {
            return file.read();
        } catch (IOException e) {
            throw new SQLIOException(e);
        }
    }

    /**
	 * Return the number of bytes read.
	 */
    public int read(byte[] buffer, int ofs, int len) throws SQLIOException {
        try {
            return file.read(buffer, ofs, len);
        } catch (IOException e) {
            throw new SQLIOException(e);
        }
    }

    /** Reads a number of bytes and returns an array of bytes */
    public byte[] read(int bytes) throws SQLIOException {
        byte[] buffer = new byte[bytes];
        read(buffer, 0, buffer.length);
        return buffer;
    }

    /**
	 * Read a line.<br>
	 * Very important: accepts all the three new line sequences ('\n', '\r',
	 * '\r\n'); changes to this screw up half of world as we currently know it.
	 */
    public String readLine() throws SQLIOException {
        try {
            return file.readLine();
        } catch (IOException e) {
            throw new SQLIOException(e);
        }
    }

    public long getPosition() throws SQLIOException {
        try {
            return file.getFilePointer();
        } catch (IOException e) {
            throw new SQLIOException(e);
        }
    }

    public void setPosition(long position) throws SQLIOException {
        try {
            file.seek(position);
        } catch (IOException e) {
            throw new SQLIOException(e);
        }
    }

    public void savePosition() throws SQLIOException {
        try {
            savePosition = file.getFilePointer();
        } catch (IOException e) {
            throw new SQLIOException(e);
        }
    }

    public void restorePosition() throws SQLIOException {
        setPosition(savePosition);
    }

    /**
	 * Returns back of the specified number of bytes.<br>
	 * <b>WARNING</b> remember to add 1 byte, if there is a deletion marker.
	 */
    public void seekBack(int len) throws SQLIOException {
        try {
            setPosition((int) file.getFilePointer() - len);
        } catch (IOException e) {
            throw new SQLIOException(e);
        }
    }

    public void seekEnd() throws SQLIOException {
        try {
            setPosition((int) channel.size());
        } catch (IOException e) {
            throw new SQLIOException(e);
        }
    }

    /**
	 * <b>WARNING!</b> Adds line terminator.
	 * 
	 * @param sb
	 *            string(builder) to write
	 * @param nl
	 *            new line sequence ('\r', '\n', '\r\n')
	 */
    public void writeWithNL(StringBuilder sb, String nl) throws SQLIOException {
        sb.append(nl);
        write(sb.toString().getBytes());
    }

    public void write(byte[] buffer) throws SQLIOException {
        try {
            file.write(buffer);
        } catch (IOException e) {
            throw new SQLIOException(e);
        }
    }

    public void write(byte[] buffer, int ofs, int len) throws SQLIOException {
        try {
            file.write(buffer, ofs, len);
        } catch (IOException e) {
            throw new SQLIOException(e);
        }
    }

    /** Writes a single byte */
    public void write(int b) throws SQLIOException {
        try {
            file.write(b);
        } catch (IOException e) {
            throw new SQLIOException(e);
        }
    }

    public void truncate() throws SQLIOException {
        try {
            file.setLength(file.getFilePointer());
        } catch (IOException e) {
            throw new SQLIOException(e);
        }
    }

    public int getSize() throws SQLIOException {
        try {
            return (int) file.length();
        } catch (IOException e) {
            throw new SQLIOException(e);
        }
    }

    @Override
    public String toString() {
        return fileHnd.toString();
    }
}

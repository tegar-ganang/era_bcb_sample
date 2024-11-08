package phex.common.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.ReentrantLock;
import phex.common.Phex;
import phex.common.log.NLogger;
import phex.io.buffer.ByteBuffer;
import phex.utils.FileUtils;

/**
 * Represents a file on the file system with managemend functionality to ensure
 * proper access.
 */
public class ManagedFile implements ReadOnlyManagedFile {

    private static final int MAX_WRITE_TRIES = 10;

    private static final int WRITE_RETRY_DELAY = 100;

    public enum AccessMode {

        READ_ONLY_ACCESS("r"), READ_WRITE_ACCESS("rwd");

        private String fileMode;

        AccessMode(String fileMode) {
            this.fileMode = fileMode;
        }
    }

    private ReentrantLock lock;

    private File fsFile;

    private AccessMode accessMode;

    private RandomAccessFile raFile;

    public ManagedFile(File file) {
        fsFile = file;
        lock = new ReentrantLock();
    }

    /**
     * Allows a thread to optain a file lock over a sequence of operations.
     * The thread needs to ensure the file lock is released otherwise the file
     * is locked foreever.
     */
    public void acquireFileLock() {
        NLogger.debug(ManagedFile.class, "Acquire file lock " + this);
        lock.lock();
        NLogger.debug(ManagedFile.class, "Acquired file lock " + this);
    }

    /**
     * Releases a file lock.
     */
    public void releaseFileLock() {
        NLogger.debug(ManagedFile.class, "Releasing " + this);
        lock.unlock();
    }

    public File getFile() {
        return fsFile;
    }

    public void setAccessMode(AccessMode newMode) throws ManagedFileException {
        lock.lock();
        try {
            if (newMode == AccessMode.READ_ONLY_ACCESS && accessMode == AccessMode.READ_WRITE_ACCESS) {
                return;
            }
            if (newMode == AccessMode.READ_WRITE_ACCESS && accessMode == AccessMode.READ_ONLY_ACCESS) {
                closeFile();
            }
            accessMode = newMode;
        } finally {
            lock.unlock();
        }
    }

    private void checkOpenFile() throws ManagedFileException {
        lock.lock();
        try {
            if (raFile != null) {
                Phex.getFileManager().trackFileInUse(this);
                return;
            }
            Phex.getFileManager().trackFileOpen(this);
            try {
                raFile = new RandomAccessFile(fsFile, accessMode.fileMode);
            } catch (Exception exp) {
                throw new ManagedFileException("failed to open", exp);
            }
        } finally {
            lock.unlock();
        }
    }

    public void closeFile() throws ManagedFileException {
        if (raFile == null) {
            return;
        }
        lock.lock();
        try {
            try {
                NLogger.debug(ManagedFile.class, "Closing file.");
                raFile.close();
            } catch (Exception exp) {
                throw new ManagedFileException("failed to close", exp);
            } finally {
                raFile = null;
                Phex.getFileManager().trackFileClose(this);
            }
        } finally {
            lock.unlock();
        }
    }

    public void write(ByteBuffer buffer, long pos) throws ManagedFileException {
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException exp) {
            Thread.currentThread().interrupt();
            throw new ManagedFileException("write failes: interrupted", exp);
        }
        try {
            checkOpenFile();
            if (raFile == null) {
                throw new ManagedFileException("write failes: raFile null");
            }
            FileChannel channel = raFile.getChannel();
            if (!channel.isOpen()) {
                throw new ManagedFileException("write failes: not open");
            }
            channel.position(pos);
            int tryCount = 0;
            while (buffer.position() != buffer.limit()) {
                int written = channel.write(buffer.internalBuffer());
                if (written > 0) {
                    tryCount = 0;
                } else {
                    if (tryCount >= MAX_WRITE_TRIES) {
                        throw new ManagedFileException("write failes: max retries");
                    }
                    try {
                        Thread.sleep(WRITE_RETRY_DELAY * tryCount);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new ManagedFileException("write failes: interrupted");
                    }
                }
            }
        } catch (Exception exp) {
            throw new ManagedFileException("write fails", exp);
        } finally {
            lock.unlock();
        }
    }

    public int read(ByteBuffer buffer, long pos) throws ManagedFileException {
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException exp) {
            Thread.currentThread().interrupt();
            throw new ManagedFileException("read failes: interrupted", exp);
        }
        try {
            checkOpenFile();
            if (raFile == null) {
                throw new ManagedFileException("read failes: raFile null");
            }
            FileChannel channel = raFile.getChannel();
            if (!channel.isOpen()) {
                throw new ManagedFileException("read failes: not open");
            }
            channel.position(pos);
            int totalRead = 0;
            int read;
            while (channel.position() < channel.size() && buffer.hasRemaining()) {
                read = channel.read(buffer.internalBuffer());
                if (read > 0) {
                    totalRead += read;
                }
            }
            return totalRead;
        } catch (Exception exp) {
            throw new ManagedFileException("read fails", exp);
        } finally {
            lock.unlock();
        }
    }

    public void setLength(long newLength) throws ManagedFileException {
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException exp) {
            Thread.currentThread().interrupt();
            throw new ManagedFileException("read failes: interrupted", exp);
        }
        try {
            checkOpenFile();
            if (raFile == null) {
                throw new ManagedFileException("read failes: raFile null");
            }
            raFile.setLength(newLength);
        } catch (Exception exp) {
            throw new ManagedFileException("setLength fails", exp);
        } finally {
            lock.unlock();
        }
    }

    public void renameFile(File destFile) throws ManagedFileException {
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException exp) {
            Thread.currentThread().interrupt();
            throw new ManagedFileException("rename failes: interrupted", exp);
        }
        try {
            if (fsFile.exists()) {
                closeFile();
                FileUtils.renameFileMultiFallback(fsFile, destFile);
            }
            fsFile = destFile;
        } catch (Exception exp) {
            throw new ManagedFileException("rename failed", exp);
        } finally {
            lock.unlock();
        }
    }

    public void deleteFile() throws ManagedFileException {
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException exp) {
            Thread.currentThread().interrupt();
            throw new ManagedFileException("delete failes: interrupted", exp);
        }
        try {
            if (fsFile.exists()) {
                closeFile();
                FileUtils.deleteFileMultiFallback(fsFile);
            }
        } catch (Exception exp) {
            throw new ManagedFileException("delete failed", exp);
        } finally {
            lock.unlock();
        }
    }

    public long getLength() {
        return fsFile.length();
    }

    public String getAbsolutePath() {
        return fsFile.getAbsolutePath();
    }

    public boolean exists() {
        return fsFile.exists();
    }

    @Override
    public String toString() {
        return super.toString() + ",File:" + fsFile + ",access:" + accessMode;
    }

    private StackTraceElement[] lastStackTraceElem;

    @Override
    protected void finalize() {
        if (raFile != null) {
            long p = -1;
            try {
                p = raFile.getFilePointer();
            } catch (IOException exp) {
                NLogger.error(ManagedFile.class, exp);
            }
            NLogger.error(ManagedFile.class, "raFile != null - " + p);
            for (StackTraceElement el : lastStackTraceElem) {
                NLogger.error(ManagedFile.class, el.toString());
            }
        }
    }
}

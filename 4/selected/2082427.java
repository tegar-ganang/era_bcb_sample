package net.sf.joafip.file.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.SortedMap;
import net.sf.joafip.NotStorableClass;

/**
 * direct file access<br>
 * 
 * @author luc peuvrier
 * 
 */
@NotStorableClass
public class RandomAccessFileDirect extends AbstractRandomAccessFile {

    private static final String FAILED_OPEN = "failed open ";

    /** read write mode for random file access */
    private static final String MODE_RWS = "rws";

    /** the file used for random access */
    private final File file;

    /** current position in file */
    private long currentPositionInFile;

    /** for random read write file access */
    private RandomAccessFile randomAccessFile;

    private FileLock fileLock;

    public RandomAccessFileDirect(final File file, final int maxRetry, final int retryMsDelay) {
        super(maxRetry, retryMsDelay);
        this.file = file;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public void openImpl() throws FileIOException {
        int tryCount = 0;
        boolean done = false;
        while (!done) {
            try {
                randomAccessFile = new RandomAccessFile(file, MODE_RWS);
                done = true;
            } catch (Exception exception) {
                if (++tryCount >= maxRetry) {
                    final String message = FAILED_OPEN + file;
                    logger.error(message);
                    throw HELPER_FILE_UTIL.fileIOException(message, file, exception);
                }
                logger.warn(FAILED_OPEN + file);
                try {
                    Thread.sleep(retryMsDelay);
                } catch (InterruptedException exception2) {
                    final String message = "wait interrupted, failed open " + file;
                    logger.error(message);
                    throw HELPER_FILE_UTIL.fileIOException(message, file, exception);
                }
            }
        }
        tryCount = 0;
        done = false;
        while (!done) {
            try {
                fileLock = randomAccessFile.getChannel().lock();
                if (fileLock == null) {
                    throw new IOException("no lock");
                } else {
                    done = true;
                }
            } catch (IOException exception) {
                if (++tryCount >= maxRetry) {
                    try {
                        randomAccessFile.close();
                    } catch (Exception exception2) {
                        logger.warn("while closing after acquire lock failure", exception2);
                    }
                    randomAccessFile = null;
                    throw HELPER_FILE_UTIL.fileIOException("file " + file + " failed lock", file, exception);
                }
                try {
                    Thread.sleep(retryMsDelay);
                } catch (InterruptedException exception2) {
                    final String message = "wait interrupted, file " + file + " failed lock";
                    logger.error(message, exception2);
                    throw HELPER_FILE_UTIL.fileIOException(message, file, exception);
                }
            }
        }
        currentPositionInFile = 0;
        if (logger.debugEnabled) {
            logger.debug("open " + file);
        }
    }

    @Override
    public void closeImpl() throws FileIOException {
        try {
            fileLock.release();
            randomAccessFile.close();
            randomAccessFile = null;
            if (logger.debugEnabled) {
                logger.debug("close " + file);
            }
        } catch (Exception exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed close " + file, file, exception);
        }
    }

    @Override
    public void flushImpl() throws FileIOException {
        closeImpl();
        openImpl();
    }

    @Override
    public void deleteIfExistsImpl() throws FileIOException {
        HELPER_FILE_UTIL.delete(file, maxRetry, retryMsDelay);
        Thread.yield();
    }

    @Override
    public void deleteIfExistsRenamingImpl() throws FileIOException {
        HELPER_FILE_UTIL.deleteRenaming(file, maxRetry, retryMsDelay);
        Thread.yield();
    }

    @Override
    public long lengthImpl() throws FileIOException {
        try {
            return randomAccessFile.length();
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed get length of " + file, file, exception);
        }
    }

    @Override
    public int readImpl(final byte[] data) throws FileIOException {
        int read;
        try {
            read = randomAccessFile.read(data);
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed read " + file, file, exception);
        }
        if (read > 0) {
            currentPositionInFile += read;
        }
        return read;
    }

    @Override
    protected int readImpl(byte[] data, int offset, int length) throws FileIOException {
        int read;
        try {
            read = randomAccessFile.read(data, offset, length);
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed read " + file, file, exception);
        }
        if (read > 0) {
            currentPositionInFile += read;
        }
        return read;
    }

    @Override
    public void seekImpl(final long positionInFile) throws FileIOException {
        if (currentPositionInFile != positionInFile) {
            try {
                randomAccessFile.seek(positionInFile);
            } catch (IOException exception) {
                throw HELPER_FILE_UTIL.fileIOException("failed seek in " + file, file, exception);
            }
            currentPositionInFile = positionInFile;
        }
    }

    @Override
    protected long currentPositionInFileImpl() throws FileIOException {
        return currentPositionInFile;
    }

    @Override
    public void setLengthImpl(final long newSize) throws FileIOException {
        try {
            randomAccessFile.setLength(newSize);
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed set length of " + file, file, exception);
        }
    }

    @Override
    public void writeImpl(final byte[] data) throws FileIOException {
        try {
            randomAccessFile.write(data);
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed write in " + file, file, exception);
        }
        currentPositionInFile += data.length;
    }

    @Override
    protected void writeImpl(final byte[] data, final int length) throws FileIOException {
        try {
            randomAccessFile.write(data, 0, length);
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed write in " + file, file, exception);
        }
        currentPositionInFile += length;
    }

    @Override
    protected void writeImpl(final byte[] data, final int offset, final int length) throws FileIOException {
        try {
            randomAccessFile.write(data, offset, length);
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed write in " + file, file, exception);
        }
        currentPositionInFile += length;
    }

    @Override
    public void clearCache() {
    }

    @Override
    public void copy(final String fileName) throws FileIOException {
        final long savedCurrentPositionInFile = currentPositionInFile;
        if (opened) {
            closeImpl();
        }
        final FileInputStream fis;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException exception) {
            throw HELPER_FILE_UTIL.fileIOException(FAILED_OPEN + file, file, exception);
        }
        final File destinationFile = new File(fileName);
        final FileOutputStream fos;
        try {
            fos = new FileOutputStream(destinationFile);
        } catch (FileNotFoundException exception) {
            throw HELPER_FILE_UTIL.fileIOException(FAILED_OPEN + destinationFile, destinationFile, exception);
        }
        try {
            final byte[] buf = new byte[1024];
            int readLength = 0;
            while ((readLength = fis.read(buf)) != -1) {
                fos.write(buf, 0, readLength);
            }
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed copy from " + file + " to " + destinationFile, null, exception);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (Exception exception) {
            }
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception exception) {
            }
        }
        if (opened) {
            openImpl();
            seek(savedCurrentPositionInFile);
        }
    }

    @Override
    public boolean differs(final String fileName, final SortedMap<Integer, Integer> diffMap) throws FileIOException {
        throw new UnsupportedOperationException("not implemented");
    }
}

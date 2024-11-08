package net.sf.joafip.file.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.SortedMap;
import net.sf.joafip.NotStorableClass;

/**
 * direct file access using 'nio'<br>
 * 
 * @author luc peuvrier
 * 
 */
@NotStorableClass
public class RandomAccessFileDirectNio extends AbstractRandomAccessFile {

    private static final String WAIT_INTERRUPTED = "wait interrupted. ";

    private static final String M_S = " mS";

    /** read write mode for random file access */
    private static final String MODE_RWS = "rws";

    /** the file used for random access */
    private final File file;

    /** current position in file */
    private long currentPositionInFile;

    /** for random read write file access */
    private RandomAccessFile randomAccessFile;

    /** the file channel associated with the random read write file */
    private FileChannel fileChannel;

    /** lock on the file */
    private FileLock fileLock;

    public RandomAccessFileDirectNio(final File file, final int maxRetry, final int retryMsDelay) {
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
        long startTime = System.currentTimeMillis();
        while (!done) {
            final String message = "failed open " + file;
            try {
                randomAccessFile = new RandomAccessFile(file, MODE_RWS);
                done = true;
                if (tryCount != 0) {
                    logger.warn("succeed open after " + tryCount + " try and " + (System.currentTimeMillis() - startTime) + M_S);
                }
            } catch (Exception exception) {
                if (++tryCount >= maxRetry) {
                    final String failureMessage = message + " try " + tryCount + " time, on " + (System.currentTimeMillis() - startTime) + M_S;
                    logger.fatal(failureMessage);
                    throw HELPER_FILE_UTIL.fileIOException(failureMessage, file, exception);
                }
                logger.error("failed open " + file);
                try {
                    Thread.sleep(retryMsDelay);
                } catch (InterruptedException exception2) {
                    logger.error(message);
                    throw HELPER_FILE_UTIL.fileIOException(WAIT_INTERRUPTED + message, file, exception);
                }
            }
        }
        tryCount = 0;
        done = false;
        startTime = System.currentTimeMillis();
        while (!done) {
            try {
                fileLock = randomAccessFile.getChannel().lock();
                if (fileLock == null) {
                    throw new IOException("no lock");
                } else {
                    done = true;
                    if (tryCount != 0) {
                        logger.warn("succeed look after " + tryCount + " try and " + (System.currentTimeMillis() - startTime) + M_S);
                    }
                }
            } catch (Exception exception) {
                final String message = "failed lock " + file;
                if (++tryCount >= maxRetry) {
                    try {
                        randomAccessFile.close();
                    } catch (Exception exception2) {
                        logger.warn("while closing after acquire lock failure", exception2);
                    }
                    final String failureMessage = message + " try " + tryCount + " time, on " + (System.currentTimeMillis() - startTime) + M_S;
                    logger.fatal(failureMessage, exception);
                    randomAccessFile = null;
                    throw HELPER_FILE_UTIL.fileIOException(failureMessage, file, exception);
                }
                logger.error(message, exception);
                try {
                    Thread.sleep(retryMsDelay);
                } catch (InterruptedException exception2) {
                    logger.error(message, exception2);
                    throw HELPER_FILE_UTIL.fileIOException(WAIT_INTERRUPTED + message, file, exception);
                }
            }
        }
        currentPositionInFile = 0;
        if (logger.debugEnabled) {
            logger.debug("open " + file);
        }
        fileChannel = randomAccessFile.getChannel();
    }

    @Override
    public void closeImpl() throws FileIOException {
        try {
            if (fileLock != null) {
                fileLock.release();
                fileLock = null;
            }
            fileChannel.close();
            fileChannel = null;
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
        try {
            fileChannel.force(true);
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed flush ", file, exception);
        }
    }

    @Override
    public void deleteIfExistsImpl() throws FileIOException {
        if (fileChannel != null) {
            throw HELPER_FILE_UTIL.fileIOException("failed delete " + file + " because opened", file, null);
        }
        HELPER_FILE_UTIL.delete(file, maxRetry, retryMsDelay);
        Thread.yield();
    }

    @Override
    public void deleteIfExistsRenamingImpl() throws FileIOException {
        if (fileChannel != null) {
            throw HELPER_FILE_UTIL.fileIOException("failed delete " + file + " because opened", file, null);
        }
        HELPER_FILE_UTIL.deleteRenaming(file, maxRetry, retryMsDelay);
        Thread.yield();
    }

    @Override
    public long lengthImpl() throws FileIOException {
        try {
            return fileChannel.size();
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed get length of " + file, file, exception);
        }
    }

    @Override
    public int readImpl(final byte[] data) throws FileIOException {
        try {
            final ByteBuffer byteBuffer = ByteBuffer.wrap(data, 0, data.length);
            final int read = fileChannel.read(byteBuffer);
            if (read > 0) {
                currentPositionInFile += read;
            }
            return read;
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed read in " + file, file, exception);
        }
    }

    @Override
    protected int readImpl(byte[] data, int offset, int length) throws FileIOException {
        try {
            final ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, length);
            final int read = fileChannel.read(byteBuffer);
            if (read > 0) {
                currentPositionInFile += read;
            }
            return read;
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed read in " + file, file, exception);
        }
    }

    @Override
    public void seekImpl(final long positionInFile) throws FileIOException {
        try {
            if (currentPositionInFile != positionInFile) {
                fileChannel.position(positionInFile);
                currentPositionInFile = positionInFile;
            }
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed seek in " + file, file, exception);
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
            final ByteBuffer byteBuffer = ByteBuffer.wrap(data);
            if (fileChannel.write(byteBuffer) != data.length) {
                throw HELPER_FILE_UTIL.fileIOException("failed write " + data.length + " bytes in " + file, file, null);
            }
            currentPositionInFile += data.length;
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed write in " + file, file, exception);
        }
    }

    @Override
    public void writeImpl(final byte[] data, final int length) throws FileIOException {
        try {
            final ByteBuffer byteBuffer = ByteBuffer.wrap(data, 0, length);
            if (fileChannel.write(byteBuffer) != length) {
                throw HELPER_FILE_UTIL.fileIOException("failed write " + length + " bytes in " + file, file, null);
            }
            currentPositionInFile += length;
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed write in " + file, file, exception);
        }
    }

    @Override
    protected void writeImpl(final byte[] data, final int offset, final int length) throws FileIOException {
        try {
            final ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, length);
            if (fileChannel.write(byteBuffer) != length) {
                throw HELPER_FILE_UTIL.fileIOException("failed write " + length + " bytes in " + file, file, null);
            }
            currentPositionInFile += length;
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed write in " + file, file, exception);
        }
    }

    @Override
    public void clearCache() {
    }

    @Override
    public void copy(final String fileName) throws FileIOException {
        try {
            if (opened) {
                fileChannel.position(0);
            } else {
                fileChannel = new FileInputStream(file).getChannel();
            }
            FileChannel dstChannel = null;
            try {
                dstChannel = new FileOutputStream(fileName).getChannel();
                dstChannel.transferFrom(fileChannel, 0, fileChannel.size());
            } finally {
                try {
                    if (dstChannel != null) {
                        dstChannel.close();
                    }
                } catch (Exception exception) {
                }
            }
            if (opened) {
                fileChannel.position(currentPositionInFile);
            } else {
                fileChannel.close();
            }
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed copy " + file + " to " + fileName, null, exception);
        }
    }

    private class DiffBuffer {

        private final SortedMap<Integer, Integer> diffMap;

        private int differsPosition = -1;

        private int differsLength;

        public DiffBuffer(final SortedMap<Integer, Integer> diffMap) {
            super();
            this.diffMap = diffMap;
        }

        public void byteState(final int position, final boolean differs) {
            if (diffMap != null) {
                if (differs) {
                    if (differsPosition == -1) {
                        differsPosition = position;
                        differsLength = 1;
                    } else {
                        differsLength++;
                    }
                } else {
                    endDifferencies();
                }
            }
        }

        private void endDifferencies() {
            if (differsPosition != -1) {
                diffMap.put(differsPosition, differsLength);
                differsPosition = -1;
            }
        }

        public void close() {
            if (diffMap != null) {
                endDifferencies();
            }
        }
    }

    @Override
    public boolean differs(final String fileName, final SortedMap<Integer, Integer> diffMap) throws FileIOException {
        try {
            if (opened) {
                fileChannel.position(0);
            } else {
                fileChannel = new FileInputStream(file).getChannel();
            }
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed differ " + file + " with " + fileName, file, exception);
        }
        boolean differs;
        FileChannel referenceChannel = null;
        try {
            referenceChannel = new FileInputStream(fileName).getChannel();
            if (fileChannel.size() == referenceChannel.size()) {
                final int length = 1024;
                final ByteBuffer byteBuffer = ByteBuffer.allocate(length);
                final ByteBuffer referenceByteBuffer = ByteBuffer.allocate(length);
                byte[] data;
                byte[] referenceData;
                int readPosition = 0;
                final DiffBuffer diffBuffer = new DiffBuffer(diffMap);
                differs = false;
                do {
                    data = read(byteBuffer, fileChannel);
                    referenceData = read(referenceByteBuffer, referenceChannel);
                    differs |= differs(data, referenceData, readPosition, diffBuffer);
                    readPosition += data.length;
                } while (data.length != 0);
                diffBuffer.close();
            } else {
                differs = true;
            }
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed differ " + file + " with " + fileName, file, exception);
        } finally {
            try {
                if (referenceChannel != null) {
                    referenceChannel.close();
                }
            } catch (Exception exception) {
            }
        }
        try {
            if (opened) {
                fileChannel.position(currentPositionInFile);
            } else {
                fileChannel.close();
            }
        } catch (IOException exception) {
            throw HELPER_FILE_UTIL.fileIOException("failed differ " + file + " with " + fileName, file, exception);
        }
        return differs;
    }

    private boolean differs(final byte[] data, final byte[] referenceData, final int readPosition, final DiffBuffer diffBuffer) {
        boolean differs = false;
        for (int index = 0; index < data.length; index++) {
            if (data[index] == referenceData[index]) {
                diffBuffer.byteState(readPosition + index, false);
            } else {
                diffBuffer.byteState(readPosition + index, true);
                differs = true;
            }
        }
        return differs;
    }

    private byte[] read(final ByteBuffer byteBuffer, final FileChannel fileChannel) throws IOException {
        byteBuffer.clear();
        final int read = fileChannel.read(byteBuffer);
        byteBuffer.flip();
        final byte[] bytes;
        if (read == -1) {
            bytes = new byte[0];
        } else {
            bytes = new byte[read];
            byteBuffer.get(bytes);
        }
        return bytes;
    }
}

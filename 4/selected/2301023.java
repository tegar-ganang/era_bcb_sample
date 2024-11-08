package net.sf.joafip.file.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import net.sf.joafip.NotStorableClass;
import net.sf.joafip.logger.JoafipLogger;

/**
 * utilities for file management
 * 
 * @author luc peuvrier
 * @date 3 août 2007
 * @version 1.0
 */
@NotStorableClass
public final class HelperFileUtil {

    private static final String M_S = " mS";

    private static final String TRY_ON = " try on ";

    private static final String COMMA = " , ";

    private static final String FAILED = " failed";

    private static final String WAITING_FOR_RETRY_INTERRUPTED = "waiting for retry interrupted. ";

    private static final JoafipLogger LOGGER = JoafipLogger.getLogger(HelperFileUtil.class);

    private static final String FAILED_CHECK_FILE_DIFFERENCE = "failed check file difference ";

    private static final String FAILED_TOUCH_FILE = "failed touch file: ";

    private static final String FAILED_DELETE_RENAMING_FILE = "failed delete renaming file: ";

    private static final String FAILED_DELETE_FILE = "failed delete file: ";

    private static final String COPY_FILE_FAILED = "copy file failed:";

    private static final HelperFileUtil INSTANCE = new HelperFileUtil();

    public static HelperFileUtil getInstance() {
        return INSTANCE;
    }

    private HelperFileUtil() {
        super();
    }

    public void copyFile(final File sourceFile, final File destinationFile) throws FileIOException {
        final FileChannel sourceChannel;
        try {
            sourceChannel = new FileInputStream(sourceFile).getChannel();
        } catch (FileNotFoundException exception) {
            final String message = COPY_FILE_FAILED + sourceFile + " -> " + destinationFile;
            LOGGER.fatal(message);
            throw fileIOException(message, sourceFile, exception);
        }
        final FileChannel destinationChannel;
        try {
            destinationChannel = new FileOutputStream(destinationFile).getChannel();
        } catch (FileNotFoundException exception) {
            final String message = COPY_FILE_FAILED + sourceFile + " -> " + destinationFile;
            LOGGER.fatal(message);
            throw fileIOException(message, destinationFile, exception);
        }
        try {
            destinationChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } catch (Exception exception) {
            final String message = COPY_FILE_FAILED + sourceFile + " -> " + destinationFile;
            LOGGER.fatal(message);
            throw fileIOException(message, null, exception);
        } finally {
            if (sourceChannel != null) {
                try {
                    sourceChannel.close();
                } catch (IOException exception) {
                    LOGGER.error("closing source", exception);
                }
            }
            if (destinationChannel != null) {
                try {
                    destinationChannel.close();
                } catch (IOException exception) {
                    LOGGER.error("closing destination", exception);
                }
            }
        }
    }

    public void touchFile(final File file, final int maxRetry, final int retryMsDelay) throws FileIOException {
        int tryCount = 0;
        boolean done = false;
        final long startTime = System.currentTimeMillis();
        while (!done) {
            try {
                file.createNewFile();
                if (tryCount != 0) {
                    LOGGER.warn("succeed touch after " + tryCount + TRY_ON + (System.currentTimeMillis() - startTime) + M_S);
                }
                done = true;
            } catch (Exception exception) {
                final String message = FAILED_TOUCH_FILE + file;
                if (++tryCount >= maxRetry) {
                    final String failureMessage = message + " after " + tryCount + TRY_ON + (System.currentTimeMillis() - startTime) + M_S;
                    LOGGER.fatal(failureMessage);
                    throw fileIOException(failureMessage, file, exception);
                } else {
                    LOGGER.error(message);
                    try {
                        Thread.sleep(retryMsDelay);
                    } catch (InterruptedException exception2) {
                        LOGGER.error(WAITING_FOR_RETRY_INTERRUPTED + FAILED_TOUCH_FILE + file, exception2);
                        throw fileIOException(WAITING_FOR_RETRY_INTERRUPTED + FAILED_TOUCH_FILE + file, file, exception);
                    }
                }
            }
        }
    }

    public void delete(final File file, final int maxRetry, final int retryMsDelay) throws FileIOException {
        if (file.exists()) {
            int tryCount = 0;
            boolean done = false;
            final long startTime = System.currentTimeMillis();
            while (!done) {
                try {
                    if (file.delete()) {
                        done = true;
                        if (tryCount != 0) {
                            LOGGER.warn("succeed delete after " + tryCount + TRY_ON + (System.currentTimeMillis() - startTime) + M_S);
                        }
                    } else {
                        throw new FileIOFailedDeleteException();
                    }
                } catch (Exception exception) {
                    final String message = FAILED_DELETE_FILE + file;
                    if (++tryCount >= maxRetry) {
                        final String failureMessage = message + " after " + tryCount + TRY_ON + (System.currentTimeMillis() - startTime) + M_S;
                        LOGGER.fatal(failureMessage, new Exception());
                        throw fileIOException(failureMessage, file, exception);
                    } else {
                        LOGGER.error(message);
                        try {
                            Thread.sleep(retryMsDelay);
                        } catch (InterruptedException exception2) {
                            LOGGER.error(WAITING_FOR_RETRY_INTERRUPTED + FAILED_DELETE_FILE + file, exception2);
                            throw fileIOException(WAITING_FOR_RETRY_INTERRUPTED + FAILED_DELETE_FILE + file, file, exception);
                        }
                    }
                }
            }
        }
    }

    public void deleteRenaming(final File file, final int maxRetry, final int retryMsDelay) throws FileIOException {
        if (file.exists()) {
            int suffixCount = 0;
            File dest;
            do {
                dest = new File(file.getPath() + "_toDelete" + suffixCount);
                suffixCount++;
            } while (dest.exists());
            int tryCount = 0;
            boolean done = false;
            final long startTime = System.currentTimeMillis();
            while (!done) {
                try {
                    if (file.renameTo(dest)) {
                        done = true;
                        if (tryCount != 0) {
                            LOGGER.warn("succeed rename for delete after " + tryCount + TRY_ON + (System.currentTimeMillis() - startTime) + M_S);
                        }
                    } else {
                        throw new FileIOFailedRenameException();
                    }
                } catch (Exception exception) {
                    final String message = FAILED_DELETE_RENAMING_FILE + file;
                    if (++tryCount >= maxRetry) {
                        final String failureMessage = message + " after " + tryCount + TRY_ON + (System.currentTimeMillis() - startTime) + M_S;
                        LOGGER.fatal(failureMessage);
                        throw fileIOException(failureMessage, file, exception);
                    } else {
                        LOGGER.error(message);
                        try {
                            Thread.sleep(retryMsDelay);
                        } catch (InterruptedException exception2) {
                            LOGGER.error(WAITING_FOR_RETRY_INTERRUPTED + FAILED_DELETE_RENAMING_FILE + file, exception2);
                            throw fileIOException(WAITING_FOR_RETRY_INTERRUPTED + FAILED_DELETE_RENAMING_FILE + file, file, exception);
                        }
                    }
                }
            }
            final File parent = file.getParentFile();
            if (parent != null) {
                final String toDelNameStart = file.getName() + "_toDelete";
                final File[] list = parent.listFiles(new FilenameFilter() {

                    @Override
                    public boolean accept(final File dir, final String name) {
                        return name.startsWith(toDelNameStart);
                    }
                });
                for (File toDel : list) {
                    try {
                        toDel.delete();
                    } catch (Exception exception) {
                    }
                }
            }
        }
    }

    public boolean diffFile(final File file1, final File file2) throws FileIOException {
        boolean diff = false;
        final FileInputStream fis1;
        try {
            fis1 = new FileInputStream(file1);
        } catch (FileNotFoundException exception) {
            final String message = FAILED_CHECK_FILE_DIFFERENCE + file1 + COMMA + file2;
            throw fileIOException(message, file1, exception);
        }
        final FileInputStream fis2;
        try {
            fis2 = new FileInputStream(file2);
        } catch (FileNotFoundException exception) {
            final String message = FAILED_CHECK_FILE_DIFFERENCE + file1 + COMMA + file2;
            throw fileIOException(message, file2, exception);
        }
        int value1;
        int value2;
        long index = -1;
        do {
            index++;
            try {
                value1 = fis1.read();
            } catch (IOException exception) {
                throw fileIOException(FAILED_CHECK_FILE_DIFFERENCE + file1 + COMMA + file2 + ": read " + file1 + FAILED, file1, exception);
            }
            try {
                value2 = fis2.read();
            } catch (IOException exception) {
                throw fileIOException(FAILED_CHECK_FILE_DIFFERENCE + file1 + COMMA + file2 + ": read " + file2 + FAILED, file2, exception);
            }
            diff = value1 != value2;
        } while (!diff && value1 != -1 && value2 != -1);
        try {
            fis1.close();
        } catch (IOException exception) {
            throw fileIOException(FAILED_CHECK_FILE_DIFFERENCE + file1 + COMMA + file2 + ": close " + file1 + FAILED, file1, exception);
        }
        try {
            fis2.close();
        } catch (IOException exception) {
            throw fileIOException(FAILED_CHECK_FILE_DIFFERENCE + file1 + COMMA + file2 + ": close " + file2 + FAILED, file2, exception);
        }
        if (diff) {
            LOGGER.warn("difference at position: " + index + " values: " + value1 + " " + value2);
        }
        return diff;
    }

    public FileIOException fileIOException(final String message, final File file, final Exception exception) {
        final FileIOException fileIOException;
        if (exception instanceof FileNotFoundException) {
            fileIOException = new FileIONotFoundException(message, file, exception);
        } else if (exception instanceof SecurityException) {
            fileIOException = new FileIOAccessDenyException(message, file, exception);
        } else if (exception instanceof FileIOException) {
            fileIOException = ((FileIOException) exception).create(message, file, exception);
        } else {
            fileIOException = new FileIOErrorException(message, file, exception);
        }
        return fileIOException;
    }
}

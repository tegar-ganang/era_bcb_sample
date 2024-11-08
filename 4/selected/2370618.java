package quickdisktest;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Locale;
import java.util.Random;
import myutils.ErrUtils;
import myutils.ErrUtils.ErrFileOp;
import myutils.FileUtils;
import myutils.HumanReadableSize;
import myutils.WorkerThreadBase;

/**
 *
 * @author rdiez
 */
public class WorkerThreadWriter extends WorkerThreadBase {

    private static final boolean enableDebugOutput = false;

    private static final int FLUSH_DELAY = 15 * 1000;

    private static final int RECALC_DELAY = 15 * 1000;

    private File basePath;

    private TestDataConfig config;

    private TestDataGenerator testDataGenerator;

    private Random rndGenerator = new Random();

    private SetOfTestDataFiles discoveredFiles;

    private RandomAccessFile currentFileRa;

    private File currentFilename;

    private int currentFileRandomDataSeed;

    private int currentFileSize;

    private int currentFileNumber;

    private ProgressUpdateWriting currentProgress = new ProgressUpdateWriting();

    private boolean shouldRecalcDiskSpaceAndTargetSize;

    private boolean shouldFlushAndSync;

    private long elapsedTimeMs;

    private long lastFlushTime;

    private long lastRecalcTime;

    private byte[] dataBuffer = new byte[Common.MAX_READ_WRITE_BLOCK_SIZE];

    WorkerThreadWriter(File testDataFilesPath, TestDataConfig config, TestDataGenerator testDataGenerator) throws InterruptedException {
        this.config = config;
        this.testDataGenerator = testDataGenerator;
        basePath = new File(SetOfTestDataFiles.prepareBasePath(testDataFilesPath.getAbsolutePath()));
        initWriting();
        if (currentProgress.targetDataSize == 0) {
            throw new RuntimeException("The given test data size cannot be reached.");
        }
        if (currentProgress.currentDataSize == currentProgress.targetDataSize) {
            throw new RuntimeException("The target test data size has already been reached.");
        }
        if (currentProgress.currentDataSize >= currentProgress.targetDataSize) {
            throw new RuntimeException("The existing test data is already larger than the target size.");
        }
        initWorker("TestDataWriter", currentProgress);
    }

    @Override
    protected WorkerThreadBase.ProgressUpdateBase workerThreadEntryPoint() {
        try {
            return mainLoop();
        } finally {
            closeCurrentFileHandle();
        }
    }

    private WorkerThreadBase.ProgressUpdateBase mainLoop() {
        openLastFile();
        discoveredFiles = null;
        for (; ; ) {
            updateProgress(currentProgress);
            switchFileIfNecessary();
            addTestData(currentProgress.targetDataSize - currentProgress.currentDataSize);
            assert currentProgress.currentDataSize <= currentProgress.targetDataSize : myutils.ErrUtils.assertionFailed();
            if (shouldRecalcDiskSpaceAndTargetSize || currentProgress.currentDataSize >= currentProgress.targetDataSize) {
                shouldRecalcDiskSpaceAndTargetSize = false;
                recalcTargetSize();
                if (currentProgress.currentDataSize >= currentProgress.targetDataSize) {
                    commitFileData(true);
                    return currentProgress;
                }
            }
            if (cancelAndPausePoint()) {
                if (enableDebugOutput) {
                    System.out.println("Cancelling the test data creation process...");
                }
                commitFileData(true);
                return currentProgress;
            }
            if (shouldFlushAndSync) {
                shouldFlushAndSync = false;
                commitFileData(false);
            }
        }
    }

    private synchronized void initWriting() {
        currentFileNumber = 0;
        shouldRecalcDiskSpaceAndTargetSize = false;
        shouldFlushAndSync = false;
        lastFlushTime = elapsedTimeMs;
        lastRecalcTime = elapsedTimeMs;
        assert discoveredFiles == null : ErrUtils.assertionFailed();
        discoveredFiles = new SetOfTestDataFiles();
        discoveredFiles.discoverExistingFiles(basePath, true);
        currentProgress.currentDataSize = discoveredFiles.totalCommittedSize;
        recalcTargetSize();
    }

    final void recalcTargetSize() {
        long totalDiskSpaceSnapshot = basePath.getTotalSpace();
        long freeDiskSpaceSnapshot = basePath.getUsableSpace();
        if (config.leaveFreeSelected) {
            long leaveFreeByteCount;
            if (config.leaveFreeAbsValue.equals(TestDataConfig.SIZE_ABS_VALUE_IS_CALCULATED_FROM_PERCENTAGE)) {
                double leaveFreeByteCountFloat = config.leaveFreePercent * totalDiskSpaceSnapshot / 100;
                if (leaveFreeByteCountFloat < 0 || leaveFreeByteCountFloat > Long.MAX_VALUE) throw new RuntimeException("Invalid target size.");
                leaveFreeByteCount = (long) leaveFreeByteCountFloat;
            } else {
                leaveFreeByteCount = HumanReadableSize.convertToByteCount(config.leaveFreeAbsValue, config.leaveFreeUnit, Locale.US);
            }
            if (freeDiskSpaceSnapshot <= leaveFreeByteCount) {
                currentProgress.targetDataSize = currentProgress.currentDataSize;
            } else {
                currentProgress.targetDataSize = currentProgress.currentDataSize + (freeDiskSpaceSnapshot - leaveFreeByteCount);
            }
        } else {
            if (config.maxSizeAbsValue.equals(TestDataConfig.SIZE_ABS_VALUE_IS_CALCULATED_FROM_PERCENTAGE)) {
                if (((int) config.maxSizePercent) == 100) {
                    currentProgress.targetDataSize = freeDiskSpaceSnapshot + currentProgress.currentDataSize;
                } else {
                    double targetSize = config.maxSizePercent * (freeDiskSpaceSnapshot + currentProgress.currentDataSize) / 100;
                    if (targetSize < 0 || targetSize > Long.MAX_VALUE) throw new RuntimeException("Invalid target size.");
                    currentProgress.targetDataSize = (long) targetSize;
                }
            } else {
                currentProgress.targetDataSize = HumanReadableSize.convertToByteCount(config.maxSizeAbsValue, config.maxSizeUnit, Locale.US);
            }
        }
        if (enableDebugOutput) {
            java.text.NumberFormat nf = java.text.NumberFormat.getInstance();
            nf.setGroupingUsed(true);
            System.out.println(String.format("Target size recalculated, target: %s - %s bytes, free disk: %s - %s bytes.", HumanReadableSize.format(currentProgress.targetDataSize, Common.HRS_DEC_COUNT), nf.format(currentProgress.targetDataSize), HumanReadableSize.format(freeDiskSpaceSnapshot, Common.HRS_DEC_COUNT), nf.format(freeDiskSpaceSnapshot)));
        }
    }

    final void closeCurrentFileHandle() {
        if (currentFileRa != null) {
            if (enableDebugOutput) {
                System.out.println(String.format("Closing file \"%s\".", currentFilename.getName()));
            }
            try {
                assert currentFileRa.length() == currentFileRa.getFilePointer() : ErrUtils.assertionFailed();
                assert currentFileRa.length() == currentFilename.length() : ErrUtils.assertionFailed();
            } catch (Throwable ex) {
                throw ErrUtils.asRuntimeException(ex, "");
            }
            FileUtils.closeRandomAccessFile(currentFilename, currentFileRa);
            currentFileRa = null;
        }
        currentFilename = null;
        currentFileSize = 0;
        currentFileRandomDataSeed = 0;
    }

    private void openLastFile() {
        if (discoveredFiles.isClean()) {
            return;
        }
        try {
            currentFilename = discoveredFiles.lastFilename;
            currentFileRa = new RandomAccessFile(discoveredFiles.lastFilename, "rw");
            if (currentFileRa.getChannel().size() != discoveredFiles.lastFileRealSize) {
                throw new RuntimeException("File size mismatch.");
            }
            currentProgress.currentFilename = currentFilename.getAbsolutePath();
            currentFileNumber = discoveredFiles.lastFileNumber;
            if (discoveredFiles.lastFileCommittedSize < SetOfTestDataFiles.MAX_HEADER_LEN) {
                currentFileRandomDataSeed = SetOfTestDataFiles.SMALL_FILE_SEED;
            } else {
                currentFileRandomDataSeed = discoveredFiles.lastFileSeed;
            }
            if (discoveredFiles.lastFileCommittedSize == discoveredFiles.lastFileRealSize) {
                if (enableDebugOutput) {
                    System.out.println(String.format("The last file \"%2$s\" has a total of %1$d committed bytes.", discoveredFiles.lastFileCommittedSize, currentFilename.getName()));
                }
                currentFileRa.seek(discoveredFiles.lastFileCommittedSize);
                currentFileSize = discoveredFiles.lastFileRealSize;
                return;
            }
            if (discoveredFiles.lastFileCommittedSize > discoveredFiles.lastFileRealSize) {
                throw new RuntimeException("The file contents are corrupt.");
            }
        } catch (Throwable ex) {
            throw ErrUtils.asRuntimeException(ex, String.format("Error opening the last test data file \"%s\": ", discoveredFiles.lastFilename));
        }
        try {
            int truncatedSize = discoveredFiles.lastFileRealSize - discoveredFiles.lastFileCommittedSize;
            if (enableDebugOutput) {
                System.out.println(String.format("Truncating %1$d bytes from file \"%2$s\".", truncatedSize, currentFilename.getName()));
            }
            currentFileRa.setLength(discoveredFiles.lastFileCommittedSize);
            currentFileRa.seek(discoveredFiles.lastFileCommittedSize);
            currentFileSize = discoveredFiles.lastFileCommittedSize;
        } catch (Throwable ex) {
            throw ErrUtils.asRuntimeException(ex, String.format("Error truncating non-committed data at the end of \"%s\": ", discoveredFiles.lastFilename));
        }
    }

    public synchronized void processTimerTick(int elapsedTickTimeInMs) {
        elapsedTimeMs += elapsedTickTimeInMs;
        if (!shouldFlushAndSync && elapsedTimeMs >= lastFlushTime + FLUSH_DELAY) {
            if (enableDebugOutput) {
                System.out.println("The time has come to flush the current file.");
            }
            shouldFlushAndSync = true;
            lastFlushTime = elapsedTimeMs;
        }
        if (!shouldRecalcDiskSpaceAndTargetSize && elapsedTimeMs >= lastRecalcTime + RECALC_DELAY) {
            if (enableDebugOutput) {
                System.out.println("The time has come to recalculate the target data size.");
            }
            shouldRecalcDiskSpaceAndTargetSize = true;
            lastRecalcTime = elapsedTimeMs;
        }
    }

    @Override
    public void timerTick(int elapsedTickTimeInMs) {
        processTimerTick(elapsedTickTimeInMs);
    }

    private void commitFileData(boolean syncHeaderToo) {
        if (currentFileRa == null) return;
        try {
            if (enableDebugOutput) {
                System.out.println(String.format("Flushing data to file \"%s\".", currentFilename.getName()));
            }
            currentFileRa.getChannel().force(true);
            assert (long) currentFileSize == currentFileRa.getFilePointer() : ErrUtils.assertionFailed();
            assert (long) currentFileSize == currentFileRa.getChannel().size();
            if (currentFileSize < SetOfTestDataFiles.MAX_HEADER_LEN) {
                return;
            }
            if (enableDebugOutput) {
                System.out.println(String.format("Writing an updated header to file \"%s\" with a size of %d bytes.", currentFilename.getName(), currentFileSize));
            }
            long savedPos = currentFileRa.getFilePointer();
            currentFileRa.seek(0);
            SetOfTestDataFiles.writeHeader(currentFileNumber, currentFileSize, testDataGenerator, currentFileRandomDataSeed, currentFilename, dataBuffer, currentFileRa);
            currentFileRa.seek(savedPos);
            if (syncHeaderToo) {
                if (enableDebugOutput) {
                    System.out.println(String.format("Flushing header to file \"%s\".", currentFilename.getName()));
                }
                currentFileRa.getChannel().force(true);
            }
        } catch (Throwable ex) {
            throw ErrUtils.asRuntimeExceptionWithFilename(ex, currentFilename, ErrFileOp.FE_FLUSHING);
        }
    }

    private void switchFileIfNecessary() {
        if (currentFileRa != null) {
            if (currentFileSize < SetOfTestDataFiles.MAX_FILE_SIZE) return;
            commitFileData(true);
            closeCurrentFileHandle();
        }
        assert currentFileRa == null : ErrUtils.assertionFailed();
        final int newFileNumber = currentFileNumber + 1;
        String newFilename = basePath.getPath();
        if (!newFilename.endsWith(File.separator)) newFilename += File.separator;
        newFilename += String.format("%s%05d", SetOfTestDataFiles.FILENAME_PREFIX, newFileNumber);
        File f = new File(newFilename);
        RandomAccessFile ra;
        try {
            if (f.exists()) throw new RuntimeException("The file already exists.");
            ra = new RandomAccessFile(f, "rw");
        } catch (Throwable ex) {
            throw ErrUtils.asRuntimeExceptionWithFilename(ex, f, ErrFileOp.FE_CREATING);
        }
        currentFileNumber = newFileNumber;
        currentFileSize = 0;
        currentFileRa = ra;
        currentFilename = f;
        currentFileRandomDataSeed = SetOfTestDataFiles.SMALL_FILE_SEED;
        currentProgress.currentFilename = newFilename;
        if (enableDebugOutput) {
            System.out.println(String.format("Created file \"%s\".", currentFilename.getName()));
        }
    }

    private void addTestData(long bytesToAdd) {
        try {
            assert bytesToAdd > 0 : ErrUtils.assertionFailed();
            assert currentFileSize < SetOfTestDataFiles.MAX_FILE_SIZE : ErrUtils.assertionFailed();
            assert currentFileRa.getFilePointer() == currentFileSize : ErrUtils.assertionFailed();
            assert currentFileRa.length() == currentFileSize : ErrUtils.assertionFailed();
            assert currentFilename.length() == currentFileSize : ErrUtils.assertionFailed();
            int spaceLeftInFile = SetOfTestDataFiles.MAX_FILE_SIZE - currentFileSize;
            long maxBs = Common.calculateMaxBlockSize(currentProgress.targetDataSize);
            int blockSize = Math.min((int) Math.min(spaceLeftInFile, maxBs), (int) Math.min(bytesToAdd, dataBuffer.length));
            final boolean writeOneByteAtATimeForDebuggingPurposes = false;
            if (writeOneByteAtATimeForDebuggingPurposes) blockSize = 1;
            assert blockSize > 0 : ErrUtils.assertionFailed();
            int bytesAdded;
            if (currentFileSize < SetOfTestDataFiles.MAX_HEADER_LEN && currentFileSize + blockSize >= SetOfTestDataFiles.MAX_HEADER_LEN) {
                assert currentFileRandomDataSeed == SetOfTestDataFiles.SMALL_FILE_SEED : ErrUtils.assertionFailed();
                currentFileRandomDataSeed = rndGenerator.nextInt();
                if (enableDebugOutput) {
                    System.out.println(String.format("Writing first header to file \"%s\".", currentFilename.getName()));
                }
                currentFileRa.seek(0);
                SetOfTestDataFiles.writeHeader(currentFileNumber, SetOfTestDataFiles.MAX_HEADER_LEN, testDataGenerator, currentFileRandomDataSeed, currentFilename, dataBuffer, currentFileRa);
                currentFileRa.getChannel().force(true);
                assert currentFileRa.getFilePointer() == SetOfTestDataFiles.MAX_HEADER_LEN : ErrUtils.assertionFailed();
                bytesAdded = SetOfTestDataFiles.MAX_HEADER_LEN - currentFileSize;
                assert bytesAdded >= 0 && bytesAdded <= SetOfTestDataFiles.MAX_HEADER_LEN : ErrUtils.assertionFailed();
            } else {
                int alignedBlockSize = testDataGenerator.alignDataSize(currentFileSize, blockSize);
                if (enableDebugOutput) {
                    String msg = String.format("Writing %1$d data bytes to file \"%2$s\".", alignedBlockSize, currentFilename.getName());
                    System.out.println(msg);
                }
                testDataGenerator.generateData(currentFileNumber, currentFileSize, currentFileRandomDataSeed, alignedBlockSize, dataBuffer);
                currentFileRa.write(dataBuffer, 0, alignedBlockSize);
                bytesAdded = alignedBlockSize;
            }
            currentFileSize += bytesAdded;
            currentProgress.currentDataSize += bytesAdded;
            currentProgress.processedDataSize += bytesAdded;
        } catch (Throwable ex) {
            throw ErrUtils.asRuntimeExceptionWithFilename(ex, currentFilename, ErrFileOp.FE_WRITING);
        }
    }
}

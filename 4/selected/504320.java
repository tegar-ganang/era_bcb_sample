package com.googlecode.quillen.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang.StringUtils;
import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import com.googlecode.quillen.util.*;
import static com.googlecode.quillen.util.Utils.logInfo;
import static com.googlecode.quillen.util.Utils.logError;
import static com.googlecode.quillen.util.Utils.logWarning;
import com.googlecode.quillen.domain.*;
import com.googlecode.quillen.service.ChunkService;
import com.googlecode.quillen.service.ShadowFileService;
import com.googlecode.quillen.service.SnapshotService;

/**
 * Created by IntelliJ IDEA.
 * User: greg
 * Date: Dec 2, 2008
 * Time: 9:13:27 PM
 */
public class BackupImpl implements Backup {

    private static final Log LOG = LogFactory.getLog(BackupImpl.class);

    private static final int MAX_SNAPSHOT_NAME_BYTES = 1009;

    private static final int MAX_FILENAME_BYTES = 1024;

    private final ChunkService chunkService;

    private final ShadowFileService shadowFileService;

    private final SnapshotService snapshotService;

    public BackupImpl(final ChunkService chunkService, final ShadowFileService shadowFileService, final SnapshotService snapshotService) {
        this.chunkService = chunkService;
        this.shadowFileService = shadowFileService;
        this.snapshotService = snapshotService;
    }

    public void backup(final String base, final String path, final Collection<String> snapshots, final ResultConsumer<FileInfo> consumer) throws IOException, WorkQueueAbortedException, ObjectStorageException, NoSuchAlgorithmException, AttributeStorageException, InterruptedException, ParseException {
        backupHelper(true, base, Arrays.asList(path), snapshots, consumer);
    }

    public void backup(final String base, final Collection<String> paths, final Collection<String> snapshots, final ResultConsumer<FileInfo> consumer) throws IOException, WorkQueueAbortedException, ObjectStorageException, NoSuchAlgorithmException, AttributeStorageException, InterruptedException, ParseException {
        backupHelper(false, base, paths, snapshots, consumer);
    }

    private void backupHelper(final boolean relative, String base, final Collection<String> paths, final Collection<String> snapshots, final ResultConsumer<FileInfo> consumer) throws IOException, WorkQueueAbortedException, ObjectStorageException, NoSuchAlgorithmException, AttributeStorageException, InterruptedException, ParseException {
        if (!base.endsWith(File.separator)) {
            base += File.separator;
        }
        final String basePath = base;
        final ArrayList<File> pathFiles = new ArrayList<File>(paths.size());
        for (String path : paths) {
            File file = relative ? new File(basePath + StringUtils.defaultString(path)) : new File(path);
            if (file.exists()) {
                pathFiles.add(file);
            }
        }
        if (pathFiles.isEmpty()) {
            logInfo(LOG, "nothing to backup");
            return;
        }
        for (String snapshotName : snapshots) {
            if (snapshotName.getBytes("utf-8").length > MAX_SNAPSHOT_NAME_BYTES) {
                throw new IllegalArgumentException(String.format("snapshot name must be less than %d bytes (utf-8): %s", MAX_SNAPSHOT_NAME_BYTES, snapshotName));
            }
            Snapshot snapshot = snapshotService.get(snapshotName);
            if (snapshot != null && snapshot.getFiles() > 0) {
                throw new IllegalArgumentException(String.format("snapshot %s already exists with %d files and backup date %s", snapshotName, snapshot.getFiles(), snapshot.getDate()));
            }
        }
        Date now = new Date();
        for (String snapshot : snapshots) {
            snapshotService.put(new Snapshot(snapshot, now, 0, 0));
        }
        ExceptionHandler exceptionHandler = new ExceptionHandler() {

            public boolean handleException(Exception ex) {
                logError(LOG, ex, consumer, "error while backing up");
                return true;
            }
        };
        final WorkQueue fileWorkQueue = new WorkQueue(10, 20, exceptionHandler);
        final WorkQueue shadowFileWorkQueue = new WorkQueue(10, 50, exceptionHandler);
        final WorkQueue chunkWorkQueue = new WorkQueue(10, 10, exceptionHandler);
        final MutexProvider mutexProvider = new MutexProvider();
        final String tempFilePrefix = TempFileUtils.getPrefix();
        final ShadowFileBatch shadowFileBatch = new ShadowFileBatch(shadowFileService, shadowFileWorkQueue, snapshots, consumer);
        long totalSize = 0;
        int totalFiles = 0;
        File lastFile = null;
        try {
            SortedFileList list = new SortedFileList(pathFiles);
            try {
                for (final File file : list) {
                    if (file.exists() && file.isFile() && file != lastFile) {
                        fileWorkQueue.enqueue(new WorkItem() {

                            public void run() throws Exception {
                                backupFile(basePath, file, chunkWorkQueue, mutexProvider, shadowFileBatch, tempFilePrefix);
                            }
                        });
                        totalSize += file.length();
                        ++totalFiles;
                        lastFile = file;
                    }
                }
            } finally {
                list.close();
            }
            fileWorkQueue.freezeAndBlockUntilEmpty();
            shadowFileBatch.flush(0);
            shadowFileWorkQueue.freezeAndBlockUntilEmpty();
            chunkWorkQueue.freezeAndBlockUntilEmpty();
            long totalTime = new Date().getTime() - now.getTime();
            now = new Date();
            for (String snapshot : snapshots) {
                snapshotService.put(new Snapshot(snapshot, now, totalFiles, totalSize));
            }
            logInfo(LOG, "backed up %d files with %d bytes to snapshot %s (~%.1f KB/s) (~%.1f files/s)", totalFiles, totalSize, StringUtils.join(snapshots, ", "), (double) totalSize / (double) totalTime, (double) totalFiles / (double) totalTime * 1000.0);
        } finally {
            if (!fileWorkQueue.isEmpty()) {
                fileWorkQueue.abort();
            }
            if (!shadowFileWorkQueue.isEmpty()) {
                shadowFileWorkQueue.abort();
            }
            if (!chunkWorkQueue.isEmpty()) {
                chunkWorkQueue.abort();
            }
            TempFileUtils.deleteAll(tempFilePrefix);
        }
    }

    private void backupFile(final String base, final File file, final WorkQueue chunkWorkQueue, final MutexProvider mutexProvider, final ShadowFileBatch shadowFileBatch, final String tempFilePrefix) throws NoSuchAlgorithmException, IOException, WorkQueueAbortedException, ObjectStorageException, AttributeStorageException, InterruptedException {
        final String filename = StringUtils.removeStart(file.getAbsolutePath(), base);
        if (filename.getBytes("utf-8").length > MAX_FILENAME_BYTES) {
            throw new IllegalArgumentException(String.format("file name must be less than %d bytes (utf-8): %s", MAX_FILENAME_BYTES, filename));
        }
        File fileToProcess;
        final File tempFile = TempFileUtils.createTempFile(tempFilePrefix);
        try {
            FileUtils.copyFile(file, tempFile);
            fileToProcess = tempFile;
        } catch (IOException e) {
            logWarning(LOG, "failed to create temp copy at %s (%s), continuing with file %s.  This can be dangerous if the file is changing during backup!", tempFile.getAbsolutePath(), e.getMessage(), file.getAbsolutePath());
            FileUtils.deleteQuietly(tempFile);
            fileToProcess = file;
        }
        if (fileToProcess.exists()) {
            final String shadowKey = shadowFileService.createShadowKey(new FileInputStream(fileToProcess));
            synchronized (mutexProvider.getMutex("shadow-" + shadowKey)) {
                final ShadowFile shadowFile = shadowFileBatch.add(shadowKey, filename, fileToProcess.length());
                if (shadowFile != null) {
                    final StreamChunker chunker = new StreamChunker(new FileInputStream(fileToProcess));
                    final Semaphore sem = new Semaphore(0);
                    Chunk c;
                    while ((c = chunker.getNextChunk()) != null) {
                        final Chunk chunk = c;
                        shadowFile.addChunk(chunk);
                        chunkWorkQueue.enqueue(new WorkItem() {

                            public void run() throws ObjectStorageException, AttributeStorageException, IOException {
                                synchronized (mutexProvider.getMutex("chunk-" + chunk.getKey())) {
                                    chunkService.put(shadowKey, chunk);
                                }
                                sem.release();
                            }
                        });
                    }
                    sem.acquire(shadowFile.getChunkKeys().size());
                    shadowFileBatch.signalDone(shadowKey);
                }
            }
            FileUtils.deleteQuietly(tempFile);
        } else {
            logWarning(LOG, "file %s seems to have disappeared!", fileToProcess.toString());
        }
    }
}

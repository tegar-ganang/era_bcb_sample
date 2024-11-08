package net.sf.joafip.service.rel400;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Timer;
import java.util.TimerTask;
import net.sf.joafip.AbstractDeleteFileTestCase;
import net.sf.joafip.NotStorableClass;
import net.sf.joafip.StorableAccess;
import net.sf.joafip.TestException;
import net.sf.joafip.entity.EnumFilePersistenceCloseAction;
import net.sf.joafip.entity.EnumNoMoreDataAction;
import net.sf.joafip.file.service.FileIOException;
import net.sf.joafip.file.service.HelperFileUtil;
import net.sf.joafip.service.FilePersistenceBuilder;
import net.sf.joafip.service.FilePersistenceClassNotFoundException;
import net.sf.joafip.service.FilePersistenceDataCorruptedException;
import net.sf.joafip.service.FilePersistenceException;
import net.sf.joafip.service.FilePersistenceInvalidClassException;
import net.sf.joafip.service.FilePersistenceNotSerializableException;
import net.sf.joafip.service.FilePersistenceTooBigForSerializationException;
import net.sf.joafip.service.IDataAccessSession;
import net.sf.joafip.service.IFilePersistence;

/**
 * 
 * @author luc peuvrier
 * 
 */
@NotStorableClass
@StorableAccess
public class TestFileManagementOptions extends AbstractDeleteFileTestCase {

    private static final String KEY = "key";

    private static final HelperFileUtil HELPER_FILE_UTIL = HelperFileUtil.getInstance();

    private IFilePersistence filePersistence;

    private boolean stop;

    public TestFileManagementOptions() throws TestException {
        super();
    }

    public TestFileManagementOptions(final String name) throws TestException {
        super(name);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            filePersistence.close();
        } catch (final Exception e) {
        }
        super.tearDown();
    }

    public void testFileName() throws FilePersistenceException, FilePersistenceInvalidClassException, FilePersistenceNotSerializableException, FilePersistenceClassNotFoundException, FilePersistenceDataCorruptedException, FilePersistenceTooBigForSerializationException {
        final FilePersistenceBuilder builder = new FilePersistenceBuilder();
        builder.setPathName(path.getPath());
        builder.setDataFileName("dataFile");
        builder.setBackupDataFileName("backupDataFile");
        builder.setStateOkFlagFileName("stateOkFlagFile");
        builder.setStateBackupOkFlagFileName("stateBackupOkFlagFile");
        builder.setGlobalStateFlagFileName("globalStateFlagFile");
        builder.setProxyMode(true);
        builder.setRemoveFiles(true);
        builder.setGarbageManagement(false);
        builder.setCrashSafeMode(true);
        filePersistence = builder.build();
        final IDataAccessSession session = filePersistence.createDataAccessSession();
        session.open();
        session.setObject(KEY, new Object());
        session.close(EnumFilePersistenceCloseAction.SAVE);
        filePersistence.close();
        assertFalse("\"store.data\" must not exists", new File(path, "store.data").exists());
        assertFalse("\"store.bak\" must not exists", new File(path, "store.bak").exists());
        assertFalse("\"data.flag\" must not exists", new File(path, "data.flag").exists());
        assertFalse("\"backup.flag\" must not exists", new File(path, "backup.flag").exists());
        assertFalse("\"all.flag\" must not exists", new File(path, "all.flag").exists());
        assertTrue("\"dataFile\" must exists", new File(path, "dataFile").exists());
        assertTrue("\"backupDataFile\" must exists", new File(path, "backupDataFile").exists());
        assertTrue("\"stateOkFlagFile\" must exists", new File(path, "stateOkFlagFile").exists());
        assertTrue("\"stateBackupOkFlagFile\" must exists", new File(path, "stateBackupOkFlagFile").exists());
        assertTrue("\"globalStateFlagFile\" must exists", new File(path, "globalStateFlagFile").exists());
    }

    public void testFileOperationRetry() throws FilePersistenceException, FilePersistenceInvalidClassException, FilePersistenceNotSerializableException, FilePersistenceClassNotFoundException, FilePersistenceDataCorruptedException, FileIOException, IOException, FilePersistenceTooBigForSerializationException {
        final FilePersistenceBuilder builder = new FilePersistenceBuilder();
        builder.setPathName(path.getPath());
        builder.setProxyMode(true);
        builder.setRemoveFiles(false);
        builder.setGarbageManagement(false);
        builder.setCrashSafeMode(false);
        builder.setMaxFileOperationRetry(4);
        builder.setFileOperationRetryMsDelay(5000);
        final File file = new File(path, "store.data");
        HELPER_FILE_UTIL.touchFile(file, 1, 0);
        final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rws");
        final FileLock lock = randomAccessFile.getChannel().tryLock();
        assertNotNull("must lock", lock);
        final int unLockDelay = 10000;
        final Timer timer = new Timer(true);
        try {
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    try {
                        lock.release();
                    } catch (final IOException e) {
                    }
                    try {
                        randomAccessFile.close();
                    } catch (final IOException e) {
                    }
                }
            }, unLockDelay);
            filePersistence = builder.build();
            final IDataAccessSession session = filePersistence.createDataAccessSession();
            session.open();
            session.setObject(KEY, new Object());
            session.close(EnumFilePersistenceCloseAction.SAVE);
            filePersistence.close();
        } finally {
            try {
                timer.cancel();
            } catch (final Exception e) {
            }
            try {
                Thread.sleep(unLockDelay);
            } catch (final InterruptedException e) {
            }
            try {
                lock.release();
            } catch (final IOException e) {
            }
            try {
                randomAccessFile.close();
            } catch (final IOException e) {
            }
        }
    }

    @SuppressWarnings("PMD")
    public void testPreserveFileNoCache() throws FilePersistenceException, FilePersistenceInvalidClassException, FilePersistenceNotSerializableException, FilePersistenceClassNotFoundException, FilePersistenceDataCorruptedException, InterruptedException, FilePersistenceTooBigForSerializationException {
        final NoMoreDataActionFileSize noMoreDataActionFileSize = testForNoMoreDataAction(EnumNoMoreDataAction.PRESERVE_FILE, false);
        assertEquals("size must not change", noMoreDataActionFileSize.before, noMoreDataActionFileSize.after);
    }

    @SuppressWarnings("PMD")
    public void testPreserveFileWithCache() throws FilePersistenceException, FilePersistenceInvalidClassException, FilePersistenceNotSerializableException, FilePersistenceClassNotFoundException, FilePersistenceDataCorruptedException, InterruptedException, FilePersistenceTooBigForSerializationException {
        final NoMoreDataActionFileSize noMoreDataActionFileSize = testForNoMoreDataAction(EnumNoMoreDataAction.PRESERVE_FILE, true);
        assertTrue("size must not change", noMoreDataActionFileSize.before <= noMoreDataActionFileSize.after);
    }

    @SuppressWarnings("PMD")
    public void testResizeFileNoCache() throws FilePersistenceException, FilePersistenceInvalidClassException, FilePersistenceNotSerializableException, FilePersistenceClassNotFoundException, FilePersistenceDataCorruptedException, InterruptedException, FilePersistenceTooBigForSerializationException {
        final NoMoreDataActionFileSize noMoreDataActionFileSize = testForNoMoreDataAction(EnumNoMoreDataAction.RESIZE_FILE, false);
        assertResizeFileSize(noMoreDataActionFileSize);
    }

    @SuppressWarnings("PMD")
    public void testResizeFileWithCache() throws FilePersistenceException, FilePersistenceInvalidClassException, FilePersistenceNotSerializableException, FilePersistenceClassNotFoundException, FilePersistenceDataCorruptedException, InterruptedException, FilePersistenceTooBigForSerializationException {
        final NoMoreDataActionFileSize noMoreDataActionFileSize = testForNoMoreDataAction(EnumNoMoreDataAction.RESIZE_FILE, true);
        assertResizeFileSize(noMoreDataActionFileSize);
    }

    private void assertResizeFileSize(final NoMoreDataActionFileSize noMoreDataActionFileSize) {
    }

    @SuppressWarnings("PMD")
    public void testRenameFailure() throws FilePersistenceException, FilePersistenceInvalidClassException, FilePersistenceNotSerializableException, FilePersistenceClassNotFoundException, FilePersistenceDataCorruptedException, InterruptedException {
        try {
            testForNoMoreDataAction(EnumNoMoreDataAction.RENAME_FILE, true);
            fail("must fail");
        } catch (final Exception e) {
        }
    }

    @SuppressWarnings("PMD")
    private class NoMoreDataActionFileSize {

        long before;

        long after;
    }

    private NoMoreDataActionFileSize testForNoMoreDataAction(final EnumNoMoreDataAction noMoreDataAction, final boolean useCache) throws FilePersistenceException, FilePersistenceInvalidClassException, FilePersistenceNotSerializableException, FilePersistenceClassNotFoundException, FilePersistenceDataCorruptedException, InterruptedException, FilePersistenceTooBigForSerializationException {
        final FilePersistenceBuilder builder = new FilePersistenceBuilder();
        builder.setPathName(path.getPath());
        builder.setProxyMode(true);
        builder.setRemoveFiles(true);
        builder.setGarbageManagement(false);
        builder.setCrashSafeMode(false);
        builder.setNoMoreDataAction(noMoreDataAction);
        if (useCache) {
            builder.setFileCache(1024, 1024);
        }
        filePersistence = builder.build();
        final Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                synchronized (TestFileManagementOptions.this) {
                    TestFileManagementOptions.this.notifyAll();
                    final File file = new File(path, "store.data");
                    RandomAccessFile randomAccessFile;
                    try {
                        randomAccessFile = new RandomAccessFile(file, "rws");
                        FileLock lock = null;
                        try {
                            do {
                                try {
                                    lock = randomAccessFile.getChannel().tryLock();
                                } catch (final Exception e) {
                                    lock = null;
                                }
                                if (lock == null && !stop) {
                                    TestFileManagementOptions.this.wait(0);
                                }
                            } while (!stop && lock == null);
                            if (!stop) {
                                TestFileManagementOptions.this.wait();
                            }
                        } catch (final Exception e) {
                        }
                        try {
                            lock.release();
                        } catch (final Exception e) {
                        }
                        try {
                            randomAccessFile.close();
                        } catch (final Exception e) {
                        }
                    } catch (final Exception e1) {
                    }
                    TestFileManagementOptions.this.notifyAll();
                }
            }
        });
        synchronized (this) {
            stop = false;
            thread.start();
            notifyAll();
            wait();
        }
        final NoMoreDataActionFileSize noMoreDataActionFileSize = new NoMoreDataActionFileSize();
        try {
            final IDataAccessSession session = filePersistence.createDataAccessSession();
            session.open();
            session.setObject(KEY, new Object());
            session.close(EnumFilePersistenceCloseAction.SAVE);
            final File storageFile = new File(filePersistence.getStorageFileName());
            noMoreDataActionFileSize.before = storageFile.length();
            session.open();
            session.removeObject(KEY);
            session.close(EnumFilePersistenceCloseAction.SAVE);
            noMoreDataActionFileSize.after = storageFile.length();
            session.open();
            final Object read = session.getObject(KEY);
            assertNull("must be erased", read);
            session.setObject(KEY, new Object());
            session.close(EnumFilePersistenceCloseAction.SAVE);
            filePersistence.close();
        } finally {
            synchronized (this) {
                stop = true;
                notifyAll();
                wait();
            }
        }
        return noMoreDataActionFileSize;
    }

    @SuppressWarnings("PMD")
    public void testRenameMode() throws FilePersistenceException, FilePersistenceInvalidClassException, FilePersistenceNotSerializableException, FilePersistenceClassNotFoundException, FilePersistenceDataCorruptedException, FilePersistenceTooBigForSerializationException {
        final FilePersistenceBuilder builder = new FilePersistenceBuilder();
        builder.setPathName(path.getPath());
        builder.setProxyMode(true);
        builder.setRemoveFiles(false);
        builder.setGarbageManagement(false);
        builder.setCrashSafeMode(false);
        builder.setNoMoreDataAction(EnumNoMoreDataAction.RENAME_FILE);
        builder.setFileCache(1024, 1024);
        filePersistence = builder.build();
        final IDataAccessSession session = filePersistence.createDataAccessSession();
        session.open();
        session.setObject(KEY, new Object());
        session.close(EnumFilePersistenceCloseAction.SAVE);
        session.open();
        session.removeObject(KEY);
        session.close(EnumFilePersistenceCloseAction.SAVE);
        session.open();
        session.setObject(KEY, new Object());
        session.close(EnumFilePersistenceCloseAction.SAVE);
        filePersistence.close();
    }
}

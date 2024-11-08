package jogamp.common.util.locks;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import com.jogamp.common.util.locks.SingletonInstance;

public class SingletonInstanceFileLock extends SingletonInstance {

    static final String temp_file_path;

    static {
        String s = null;
        try {
            File tmpFile = File.createTempFile("TEST", "tst");
            String absTmpFile = tmpFile.getCanonicalPath();
            tmpFile.delete();
            s = absTmpFile.substring(0, absTmpFile.lastIndexOf(File.separator));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        temp_file_path = s;
    }

    public static String getCanonicalTempPath() {
        return temp_file_path;
    }

    public static String getCanonicalTempLockFilePath(String basename) {
        return getCanonicalTempPath() + File.separator + basename;
    }

    public SingletonInstanceFileLock(long poll_ms, String lockFileBasename) {
        super(poll_ms);
        file = new File(getCanonicalTempLockFilePath(lockFileBasename));
        setupFileCleanup();
    }

    public SingletonInstanceFileLock(long poll_ms, File lockFile) {
        super(poll_ms);
        file = lockFile;
        setupFileCleanup();
    }

    public final String getName() {
        return file.getPath();
    }

    private void setupFileCleanup() {
        file.deleteOnExit();
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                unlock();
            }
        });
    }

    @Override
    protected boolean tryLockImpl() {
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            fileLock = randomAccessFile.getChannel().tryLock();
            if (fileLock != null) {
                return true;
            }
        } catch (Exception e) {
            System.err.println("SLOCK " + System.currentTimeMillis() + " EEE " + getName() + " - Unable to create and/or lock file");
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected boolean unlockImpl() {
        try {
            if (null != fileLock) {
                fileLock.release();
                fileLock = null;
            }
            if (null != randomAccessFile) {
                randomAccessFile.close();
                randomAccessFile = null;
            }
            if (null != file) {
                file.delete();
            }
            return true;
        } catch (Exception e) {
            System.err.println("SLOCK " + System.currentTimeMillis() + " EEE " + getName() + " - Unable to remove lock file");
            e.printStackTrace();
        } finally {
            fileLock = null;
            randomAccessFile = null;
        }
        return false;
    }

    private final File file;

    private RandomAccessFile randomAccessFile = null;

    private FileLock fileLock = null;
}

package mil.army.usace.ehlschlaeger.rgik.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.concurrent.Semaphore;

public class LockFile {

    private static Semaphore sem = new Semaphore(1);

    ;

    protected File file;

    protected RandomAccessFile raf;

    protected FileLock lock;

    public LockFile(File lockFile) {
        this.file = lockFile;
    }

    public void lock() throws InterruptedException, IOException {
        if (this.lock != null) return;
        try {
            sem.acquire();
            while (lock == null) {
                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        this.raf = new RandomAccessFile(file, "rw");
                        this.lock = raf.getChannel().lock();
                    }
                }
                Thread.sleep(1000);
            }
        } finally {
            if (lock == null) sem.release();
        }
    }

    public void unlock() {
        try {
            lock.release();
        } catch (IOException e) {
        }
        lock = null;
        try {
            raf.close();
        } catch (IOException e) {
        }
        raf = null;
        sem.release();
    }
}

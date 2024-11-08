package ch.jester.common.utility;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * Utility Klasse um einen exklusiven FileLock zu erhalten.
 */
public class FileLocker {

    FileLock lock;

    File file;

    public FileLocker(File f) {
        file = f;
    }

    /**
	 * @return  den Lock oder null, wenn kein Lock gemacht werden konnte
	 */
    public FileLock getLock() {
        return lock;
    }

    /**
	 * Versucht das File zu locken.
	 * Der Lock geschieht über einen FileChannel, so dass dieser beim Beenden der App oder beim Absturz der VM/des Rechners
	 * automatisch wieder released ist.
	 * @return true wenn der Lock erfolgreich war. Sonst false
	 */
    public boolean lock() {
        lock = createFileLock(file);
        return lock != null ? true : false;
    }

    /**
	 * Gibt das File wieder frei.
	 */
    public void unlock() {
        releaseLock(lock);
    }

    private FileLock createFileLock(File f) {
        try {
            File file = f;
            FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
            return channel.tryLock();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private File releaseLock(FileLock lock) {
        try {
            lock.release();
            lock.channel().close();
        } catch (Exception e) {
        }
        return null;
    }
}

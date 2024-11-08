package tracker;

import java.io.*;
import java.nio.channels.FileLock;

public class MultipleInstancesLock {

    private File lockFile;

    private FileLock multipleInstanceLock;

    public MultipleInstancesLock(File lockFile) {
        this.lockFile = lockFile;
    }

    /**
     * Locks lockFile if a lock can be acquired. If the lock cannot
     * be acquired, this method logs a message and throws an exception. 
     * TinyTimeTracker should not continue if a lock cannot be acquired.
     * Be nice and allowOtherInstances before exiting.
     * @param lockFile
     * @throws MultipleInstancesException
     * @see Tracker#allowOtherInstances
     */
    public void preventMultipleInstances() throws MultipleInstancesException {
        try {
            FileOutputStream fos = new FileOutputStream(lockFile);
            multipleInstanceLock = fos.getChannel().tryLock();
        } catch (Exception e) {
            final String message = "Multiple instances must be running because we got an exception trying to acquire a lock on " + lockFile.toString();
            System.err.println(message);
            e.printStackTrace();
            throw new MultipleInstancesException(message, e);
        }
        if (multipleInstanceLock == null) {
            final String message = "Multiple instances must be running because I could not acquire a lock on " + lockFile.toString();
            System.err.println(message);
            throw new MultipleInstancesException(message);
        }
    }

    /**
     * Allows other instances of TinyTimeTracker to run. Be nice and call this
     * before exiting. However, if this isn't called, the lock will be released
     * by the OS when the jvm exits.
     */
    public void allowOtherInstances() {
        try {
            if (multipleInstanceLock != null) {
                multipleInstanceLock.release();
                multipleInstanceLock.channel().close();
                lockFile.delete();
            }
        } catch (IOException ignore) {
            ignore.printStackTrace();
        }
    }
}

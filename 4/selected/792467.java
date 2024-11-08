package org.sourceforge.milinuxcoach;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

public class StatusControl {

    public static final int INVALID = -1;

    public static final int OK = 0;

    public static final int CANCEL = 1;

    public static final int OPEN = 2;

    public static final int ALREADY_OPEN = 3;

    /**
   * Get instance
   */
    public static synchronized StatusControl getInstance() {
        if (instance == null) {
            instance = new StatusControl();
            instance.init();
        }
        return instance;
    }

    /**
   * @return the status
   */
    public int getStatus() {
        return status;
    }

    /**
   * @param status the status to set
   */
    public void setStatus(int status) {
        this.status = status;
        switch(status) {
            case CANCEL:
                File cancelFile = new File(Constants.CANCEL_FILE);
                try {
                    cancelFile.createNewFile();
                } catch (Exception e) {
                    System.out.println("Error: Cannot create close file" + e.getMessage());
                    e.printStackTrace();
                }
                break;
        }
    }

    /**
   * Constructor. The only task is check if the application is already open or not, and remove the cancel flag
   */
    public synchronized void init() {
        File openFile = new File(Constants.OPEN_FILE);
        openFile.getParentFile().mkdirs();
        try {
            lockingFile = new RandomAccessFile(openFile, "rwd");
            lock = lockingFile.getChannel().tryLock();
        } catch (Exception e) {
            System.out.println("Error trying to determine the status: " + e.getMessage());
            e.printStackTrace();
        }
        if (lock != null) {
            status = OPEN;
            openFile.deleteOnExit();
        } else {
            status = ALREADY_OPEN;
        }
        File cancelFile = new File(Constants.CANCEL_FILE);
        if (cancelFile.exists()) {
            cancelFile.delete();
        }
    }

    /**
   * Current status
   */
    private int status = INVALID;

    /**
   * Singleton instance
   */
    private static StatusControl instance = null;

    /**
   * Locking file
   */
    private RandomAccessFile lockingFile;

    /**
   * Lock
   */
    private FileLock lock;
}

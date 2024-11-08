package org.turms.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * @author spe_ra (raffaele@speraprojects.com)
 *
 */
public class AppMonitor implements Runnable {

    private String appName;

    private FileChannel channel;

    private FileLock lock;

    private File tmp;

    public AppMonitor(String appName) {
        this.appName = appName;
    }

    public boolean isApplicationActive() {
        tmp = new File(System.getProperty("user.dir") + File.separator + appName + ".lock");
        try {
            channel = new RandomAccessFile(tmp, "rw").getChannel();
            try {
                lock = channel.tryLock();
                if (lock == null) {
                    close();
                    return true;
                }
                Runtime.getRuntime().addShutdownHook(new Thread(this));
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                close();
                return true;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return true;
        }
    }

    @Override
    public void run() {
        close();
        deleteFile();
    }

    private void close() {
        if (lock != null) {
            try {
                lock.release();
            } catch (IOException e) {
            }
        }
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
            }
        }
    }

    private void deleteFile() {
        if (tmp != null) System.out.println(tmp.delete());
    }
}

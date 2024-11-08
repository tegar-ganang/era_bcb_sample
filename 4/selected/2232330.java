package file;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonWritableChannelException;
import timer.ExecutionTimer;
import timer.TimerRecordFile;

/**
 * @author Divyesh
 *
 */
public class FileWriter {

    FileLock lock;

    String Path;

    String mode;

    public RandomAccessFile file;

    public FileWriter(String path, String mode) throws FileNotFoundException {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        this.Path = path;
        this.mode = mode;
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "FileWriter", "FileWriter", t.duration());
    }

    public RandomAccessFile fileOpen(boolean force) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        try {
            while (true) {
                try {
                    file = new RandomAccessFile(Path, mode);
                    FileChannel channel = file.getChannel();
                    channel.force(force);
                    lock = channel.lock();
                    break;
                } catch (NonWritableChannelException e1) {
                    System.out.println("file:FileWriter:fileOpen(force):Exception::" + e1.getMessage());
                } catch (Exception e) {
                    System.out.println("file:FileWriter:fileOpen(force):Exception::" + e.getMessage());
                    Thread.sleep(2000);
                }
            }
        } catch (Exception e) {
            System.out.println("file:FileWriter:fileOpen(force):Exception::" + e.getMessage());
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "FileWriter", "fileOpen", t.duration());
        return file;
    }

    public RandomAccessFile fileOpen() {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        try {
            while (true) {
                file = new RandomAccessFile(Path, mode);
                try {
                    FileChannel channel = file.getChannel();
                    lock = channel.lock();
                    break;
                } catch (NonWritableChannelException e1) {
                    System.out.println("file:FileWriter:fileOpen():Exception::" + e1.getMessage());
                } catch (Exception e) {
                    System.out.println("file:FileWriter:fileOpen():Exception::" + e.getMessage());
                    Thread.sleep(2000);
                }
            }
        } catch (Exception e) {
            System.out.println("file:FileWriter:fileOpen():Exception::" + e.getMessage());
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "FileWriter", "fileOpen", t.duration());
        return file;
    }

    public void fileClose() {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        try {
            try {
                lock.release();
            } catch (Exception e) {
            }
            file.close();
        } catch (IOException e) {
            System.out.println("file:FileWriter:fileClose():Exception::" + e.getMessage());
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "FileWriter", "fileClose", t.duration());
    }
}

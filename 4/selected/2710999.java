package se.sics.mspsim.chip;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import se.sics.mspsim.core.MSP430Core;

public class FileM25P80 extends M25P80 {

    private String filename;

    private RandomAccessFile file;

    private FileChannel fileChannel;

    private FileLock fileLock;

    private long pos = 0;

    public FileM25P80(MSP430Core cpu, String filename) {
        super(cpu);
        if (filename == null) {
            filename = "flash.bin";
        }
        this.filename = filename;
    }

    private boolean ensureOpen(boolean write) {
        if (fileChannel != null) {
            return true;
        }
        if (!write) {
            File fp = new File(filename);
            if (!fp.exists()) {
                return false;
            }
        }
        if (!openFile(filename)) {
            Matcher m = Pattern.compile("(.+?)(\\d*)(\\.[^.]+)").matcher(filename);
            if (m.matches()) {
                String baseName = m.group(1);
                String c = m.group(2);
                String extName = m.group(3);
                int count = 1;
                if (c != null && c.length() > 0) {
                    count = Integer.parseInt(c) + 1;
                }
                for (int i = 0; !openFile(baseName + count + extName) && i < 100; i++, count++) ;
            }
        }
        if (fileLock == null) {
            if (write) {
                logw("failed to open flash file '" + filename + '\'');
            }
            return false;
        }
        try {
            file.setLength(MEMORY_SIZE);
            if (pos > 0) {
                file.seek(pos);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean openFile(String filename) {
        try {
            file = new RandomAccessFile(filename, "rw");
            fileChannel = file.getChannel();
            fileLock = fileChannel.tryLock();
            if (fileLock != null) {
                if (DEBUG) log("using flash file '" + filename + '\'');
                return true;
            }
            fileChannel.close();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            closeFile();
            return false;
        }
    }

    private void closeFile() {
        try {
            file = null;
            if (fileLock != null) {
                fileLock.release();
                fileLock = null;
            }
            if (fileChannel != null) {
                fileChannel.close();
                fileChannel = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void seek(long pos) throws IOException {
        if (file != null) {
            file.seek(pos);
        }
        this.pos = pos;
    }

    public int readFully(byte[] b) throws IOException {
        if (file != null || ensureOpen(false)) {
            pos += b.length;
            return file.read(b);
        }
        Arrays.fill(b, (byte) 0);
        pos += b.length;
        return b.length;
    }

    public void write(byte[] b) throws IOException {
        if (file != null || ensureOpen(true)) {
            file.write(b);
        }
        pos += b.length;
    }

    public void stateChanged(int state) {
    }
}

package repeatmap.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;
import java.nio.channels.FileChannel;

/**
 * Generic class for writing files using the nio package.
 *
 * @author Eugene
 */
public class NioFileWriter {

    protected ByteBuffer mb;

    protected File file;

    protected FileChannel out;

    protected FileLock lock;

    private FileOutputStream fout;

    private RandomAccessFile faout;

    /** Creates a new instance of NioFileWriter */
    public NioFileWriter() {
    }

    /**
   * Creates a new instance of NioFileWriter with file initialized.
   *
   * @param fname the file path.
   */
    public NioFileWriter(String fname) {
        file = new File(fname);
    }

    /**
   * Creates a new instance of NioFileWriter with file initialized.
   *
   * @param file the file.
   */
    public NioFileWriter(File file) {
        this.file = file;
    }

    public void setFile(String fname) {
        if (mb != null) {
            mb.clear();
            try {
                out.close();
                if (fout != null) fout.close();
                if (faout != null) faout.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            mb = null;
            out = null;
            fout = null;
        }
        file = new File(fname);
    }

    public void loadFile(float mmb, boolean append) {
        try {
            if (append) {
                fout = new FileOutputStream(file, append);
                out = fout.getChannel();
            } else {
                file.delete();
                faout = new RandomAccessFile(file, "rw");
                out = faout.getChannel();
            }
            mb = ByteBuffer.allocate((int) (1024 * 1024 * mmb));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public boolean lockAndLoadFile(float mmb, boolean append) {
        if (!file.exists()) return false;
        try {
            if (append) {
                fout = new FileOutputStream(file, append);
                out = fout.getChannel();
            } else {
                file.delete();
                faout = new RandomAccessFile(file, "rw");
                out = faout.getChannel();
            }
            lock = out.tryLock();
            if (lock == null) return false;
            mb = ByteBuffer.allocate((int) (1024 * 1024 * mmb));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
            return false;
        }
    }

    public void close() {
        if (isLocked()) unlock();
        try {
            out.close();
            if (fout != null) fout.close();
            if (faout != null) faout.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        mb.clear();
        mb = null;
        out = null;
        fout = null;
    }

    public boolean isLocked() {
        if (lock == null) return false;
        return true;
    }

    public void unlock() {
        if (lock == null) {
            try {
                lock.release();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        lock = null;
    }

    public boolean isOpen() {
        if (out == null) return false;
        return out.isOpen();
    }

    public int spaceLeft() {
        return mb.remaining();
    }

    public void spill() {
        try {
            if (mb.position() > 0) {
                mb.flip();
                out.write(mb);
                mb.clear();
                return;
            }
            out.write(mb);
            mb.clear();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}

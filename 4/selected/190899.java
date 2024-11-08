package ijaux.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.FileChannel.MapMode;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cursor backed by a MappedByteBuffer accessing a one RandomAccessFile at a time
 * 
 * @author dprodanov
 *
 */
public class FileListCursor extends MappedCursor implements Iterator<File> {

    private File currentFile, headerFile;

    private RandomAccessFile raf;

    private FileChannel inChannel;

    private FileLock lock;

    private int fileIndex = 0;

    public FileListCursor(Class<?> c) {
        super(c);
    }

    public FileListCursor(File[] list, int ind, Class<?> c) {
        this(c);
        registerFiles(list);
        if (ind > 0) fileIndex = ind;
        currentFile = list[fileIndex];
    }

    public void open(String mode) throws IOException {
        System.out.println(currentFile.getAbsolutePath());
        accessMode(mode);
        openCurrent();
    }

    /**
	 * @param mode
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public int openCurrent() throws FileNotFoundException, IOException {
        if (debug) System.out.println(currentFile.getAbsolutePath());
        if (currentFile.exists() && currentFile.isFile() && currentFile.canRead()) {
            raf = new RandomAccessFile(currentFile.getAbsolutePath(), accessmode);
            inChannel = raf.getChannel();
            isOpen = true;
            if ((accessmode.equals("rw") || accessmode.equals("rwd") || accessmode.equals("rwd")) && currentFile.canWrite()) {
                axmode = MapMode.READ_WRITE;
                try {
                    lock = inChannel.tryLock();
                } catch (OverlappingFileLockException e) {
                }
            }
            if (accessmode.equals("r")) axmode = MapMode.READ_ONLY;
            capacity = (int) (inChannel.size() / bytesPerPixel);
            int size = (int) inChannel.size();
            if (debug) System.out.println("size " + size);
            fpointer = 0;
            if (currentFile.equals(headerFile)) fpointer = headerSize;
            buffer = inChannel.map(axmode, 0, size);
            buffer.order(bo);
            return buffer.limit();
        }
        return -1;
    }

    public void open(File f, String mode) throws IOException {
        if (f != null && files.contains(f)) {
            currentFile = f;
            open(mode);
        }
    }

    public void openNext() {
        setFile(fileIndex++);
    }

    public boolean setFile(int i) {
        if (i < nFiles) {
            File f = currentFile;
            try {
                close();
                currentFile = files.get(i);
                return (openCurrent() > 0);
            } catch (IOException e) {
                currentFile = f;
                e.printStackTrace();
            }
        }
        return false;
    }

    public long length() throws IOException {
        if (isOpen) return inChannel.size(); else return -1;
    }

    public void clear() {
        buffer.clear();
        fpointer = 0;
    }

    public void close() throws IOException {
        if (isOpen) {
            if (lock != null) lock.release();
            inChannel.close();
            raf.close();
            isOpen = false;
        }
    }

    public void configHeader(File f, int headerSize) {
        if (f.exists() && f.isFile() && f.canRead()) {
            headerFile = f;
            this.headerSize = headerSize;
        }
    }

    public void config(String mode, ByteOrder bo, boolean loadHeader) {
        if (bo != null) this.bo = bo;
        accessMode(mode);
        if (headerSize > 0 && loadHeader) try {
            mapHeader(headerSize, bo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasNext() {
        return fileIndex < nFiles;
    }

    public File next() {
        currentFile = files.get(fileIndex++);
        return currentFile;
    }

    public void remove() {
        files.remove(fileIndex);
        currentFile = files.get(fileIndex);
        nFiles--;
    }
}

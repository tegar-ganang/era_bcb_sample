package storage.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import static storage.file.Page.BLOCK_SIZE;

public class FileMgr {

    private File dbDir;

    private boolean isNew;

    private Map<String, FileChannel> openFiles = new HashMap<String, FileChannel>();

    public FileMgr(String dbName) {
        dbDir = new File(dbName);
        isNew = !dbDir.exists();
        if (isNew && !dbDir.mkdir()) {
            throw new RuntimeException("Exception: cannot creat " + dbName);
        }
        for (String filename : dbDir.list()) {
            if (filename.startsWith("temp")) {
                new File(dbDir, filename).delete();
            }
        }
    }

    synchronized void read(Block blk, ByteBuffer bb) {
        try {
            bb.clear();
            FileChannel ff = getFile(blk.getFileName());
            ff.read(bb, blk.getBlkNum() * BLOCK_SIZE);
        } catch (IOException e) {
            throw new RuntimeException("Exception: cannot read block " + blk);
        }
    }

    synchronized void write(Block blk, ByteBuffer bb) {
        try {
            bb.rewind();
            FileChannel ff = getFile(blk.getFileName());
            ff.write(bb, blk.getBlkNum() * BLOCK_SIZE);
        } catch (IOException e) {
            throw new RuntimeException("Exception: cannot wirite block " + blk);
        }
    }

    synchronized Block append(String filename, ByteBuffer bb) {
        Block blk = new Block(filename, size(filename));
        write(blk, bb);
        return blk;
    }

    public synchronized int size(String filename) {
        try {
            FileChannel fc = getFile(filename);
            return (int) (fc.size() / BLOCK_SIZE);
        } catch (IOException e) {
            throw new RuntimeException("Exception: cannot access " + filename);
        }
    }

    public boolean isNew() {
        return isNew;
    }

    private FileChannel getFile(String filename) throws IOException {
        FileChannel fc = openFiles.get(filename);
        if (fc == null) {
            File dbTable = new File(dbDir, filename);
            RandomAccessFile f = new RandomAccessFile(dbTable, "rws");
            fc = f.getChannel();
            openFiles.put(filename, fc);
        }
        return fc;
    }
}

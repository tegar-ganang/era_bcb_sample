package panda.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;

public class FileManager {

    private Directory dbRootDirectroy;

    private HashMap<String, FileChannel> openFiles = new HashMap<String, FileChannel>();

    public FileManager(String dirName) {
        dbRootDirectroy = new Directory(dirName);
        if (dbRootDirectroy.isnew()) {
            if (!dbRootDirectroy.createDir()) {
                throw new Error("cannot create directory " + dirName);
            }
        }
        dbRootDirectroy.clearDir();
    }

    private synchronized FileChannel openFile(String filename) throws IOException {
        FileChannel fc = openFiles.get(filename);
        if (fc == null) {
            File dbTable = new File(dbRootDirectroy.getDirName(), filename);
            RandomAccessFile f = new RandomAccessFile(dbTable, "rws");
            fc = f.getChannel();
            openFiles.put(filename, fc);
        }
        return fc;
    }

    public synchronized int write(Block blk, ByteBuffer bb) {
        try {
            bb.rewind();
            FileChannel fc = openFile(blk.getFileName());
            return fc.write(bb, blk.getBlockNumber() * Block.BLOCK_SIZE);
        } catch (IOException e) {
            return -1;
        }
    }

    public synchronized int read(Block blk, ByteBuffer bb) {
        try {
            bb.clear();
            FileChannel fc = openFile(blk.getFileName());
            return fc.read(bb, blk.getBlockNumber() * Block.BLOCK_SIZE);
        } catch (IOException e) {
            return -1;
        }
    }

    public synchronized Block append(String fileName, ByteBuffer bb) {
        int blockNumber = size(fileName);
        Block blk = new Block(fileName, blockNumber);
        write(blk, bb);
        return blk;
    }

    public synchronized int size(String filename) {
        try {
            FileChannel fc = openFile(filename);
            return (int) (fc.size() / Block.BLOCK_SIZE);
        } catch (IOException e) {
            throw new RuntimeException("cannot access " + filename);
        }
    }

    public void delete(String fileName) {
        panda.server.Panda.getBufferManager().clear(fileName);
        if (openFiles.containsKey(fileName)) {
            try {
                openFiles.get(fileName).close();
                openFiles.remove(fileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        dbRootDirectroy.deleteFile(fileName);
    }

    public Directory getDirectory() {
        return dbRootDirectroy;
    }
}

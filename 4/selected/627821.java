package io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class DBFiles {

    private File dbDirectory;

    private boolean isNew;

    private Map<String, FileChannel> openFiles = new HashMap<String, FileChannel>();

    public DBFiles(String dirname) {
        dbDirectory = new File(dirname);
        isNew = !dbDirectory.exists();
        if (isNew && !dbDirectory.mkdirs()) throw new RuntimeException("cannot create " + dirname);
        for (String filename : dbDirectory.list()) if (filename.startsWith("temp")) new File(dbDirectory, filename).delete();
    }

    synchronized void read(Block blk, ByteBuffer bb) {
        try {
            bb.clear();
            FileChannel fc = getFile(blk.fileName());
            fc.read(bb, blk.number() * Page.BLOCK_SIZE);
        } catch (IOException e) {
            throw new RuntimeException("cannot read block " + blk);
        }
    }

    synchronized void write(Block blk, ByteBuffer bb) {
        try {
            bb.rewind();
            FileChannel fc = getFile(blk.fileName());
            fc.write(bb, blk.number() * Page.BLOCK_SIZE);
        } catch (IOException e) {
            throw new RuntimeException("cannot write block" + blk);
        }
    }

    synchronized Block append(String filename, ByteBuffer bb) {
        int newblknum = fileSize(filename);
        Block blk = new Block(filename, newblknum);
        write(blk, bb);
        return blk;
    }

    public synchronized int fileSize(String filename) {
        try {
            FileChannel fc = getFile(filename);
            return (int) (fc.size() / Page.BLOCK_SIZE);
        } catch (IOException e) {
            throw new RuntimeException("cannot access " + filename);
        }
    }

    public boolean isNew() {
        return isNew;
    }

    private synchronized FileChannel getFile(String filename) throws IOException {
        FileChannel fc = openFiles.get(filename);
        if (fc == null) {
            File dbTable = new File(dbDirectory, filename);
            RandomAccessFile f = new RandomAccessFile(dbTable, "rws");
            fc = f.getChannel();
            openFiles.put(filename, fc);
        }
        return fc;
    }

    public void Close() throws IOException {
        for (FileChannel fc : openFiles.values()) {
            fc.close();
        }
    }
}

package cn.edu.thss.iise.beehivez.server.index.petrinetindex.invertedindex.fileAccess;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class BufferedRAFile {

    private final boolean readOnly;

    private final long bufferLength;

    private RandomAccessFile file;

    private MappedByteBuffer buffer;

    private FileChannel channel;

    private boolean bufferModified;

    public BufferedRAFile(String path, boolean onlyRead, long bufferLen) throws FileNotFoundException {
        readOnly = onlyRead;
        bufferLength = bufferLen;
        file = new RandomAccessFile(path, readOnly ? "r" : "rw");
        this.channel = file.getChannel();
        try {
            buffer = channel.map(readOnly ? FileChannel.MapMode.READ_ONLY : FileChannel.MapMode.READ_WRITE, 0, bufferLength);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long length() throws IOException {
        return file.length();
    }

    public void seek(long pos) {
        buffer.position((int) pos);
    }

    public long getFilePointer() {
        return buffer.position();
    }

    public int readInt() {
        return buffer.getInt();
    }

    public long readLong() {
        return buffer.getLong();
    }

    public void read(byte[] b, int offset, int len) {
        buffer.get(b, offset, len);
    }

    public void read(byte[] b) {
        buffer.get(b);
    }

    public boolean readBoolean() {
        byte b = buffer.get();
        if (b == (byte) 0) return false;
        return true;
    }

    public void writeInt(int v) {
        bufferModified = true;
        buffer.putInt(v);
    }

    public void writeLong(long v) {
        bufferModified = true;
        buffer.putLong(v);
    }

    public void write(byte[] b) {
        bufferModified = true;
        buffer.put(b);
    }

    public void writeBoolean(boolean v) {
        bufferModified = true;
        if (v) buffer.put((byte) 1); else buffer.put((byte) 0);
    }

    public void close() throws IOException {
        if (buffer != null && bufferModified) {
            try {
                buffer.force();
            } catch (Throwable t) {
                try {
                    buffer.force();
                } catch (Throwable t1) {
                }
            }
        }
        buffer = null;
        channel = null;
        file.close();
        System.gc();
    }
}

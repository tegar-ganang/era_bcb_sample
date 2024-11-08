package net.oesterholt.jndbm.streams;

import java.io.*;
import java.nio.channels.FileChannel;

public class NDbmRandomAccessFile {

    private RandomAccessFile _raf = null;

    public void writeInt(int i) throws IOException {
        byte[] b = NDbmDataOutputStream.makeInt(i);
        _raf.write(b);
    }

    public int readInt() throws IOException {
        byte[] b = new byte[NDbmDataInputStream.sizeOfInt()];
        _raf.read(b);
        return NDbmDataInputStream.getInt(b);
    }

    public int sizeOfInt() {
        return NDbmDataInputStream.sizeOfInt();
    }

    public void seek(long x) throws IOException {
        _raf.seek(x);
    }

    public FileChannel getChannel() {
        return _raf.getChannel();
    }

    public NDbmRandomAccessFile(File arg0, String arg1) throws FileNotFoundException {
        _raf = new RandomAccessFile(arg0, arg1);
    }

    public void write(byte[] k) throws IOException {
        _raf.write(k);
    }

    public void write(byte[] data, int i, int lengthOfBlob) throws IOException {
        _raf.write(data, i, lengthOfBlob);
    }

    public long length() throws IOException {
        return _raf.length();
    }

    public void close() throws IOException {
        _raf.close();
    }

    public long getFilePointer() throws IOException {
        return _raf.getFilePointer();
    }

    public void writeByte(byte b) throws IOException {
        _raf.writeByte(b);
    }

    public void read(byte[] data) throws IOException {
        _raf.read(data);
    }
}

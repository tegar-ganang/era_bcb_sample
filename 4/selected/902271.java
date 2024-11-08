package com.atolsystems.atolutilities;

import java.io.*;
import java.nio.channels.FileChannel;

public class RandomAccessFileInputStream extends InputStream {

    protected RandomAccessFile randomFile;

    public RandomAccessFileInputStream(String fileName) throws IOException {
        this(new File(fileName));
    }

    public RandomAccessFileInputStream(File file) throws IOException {
        super();
        file = file.getAbsoluteFile();
        randomFile = new RandomAccessFile(file, "r");
    }

    public RandomAccessFileInputStream(RandomAccessFile file) throws IOException {
        super();
        randomFile = file;
    }

    @Override
    public void close() throws IOException {
        randomFile.close();
    }

    public int skipBytes(int n) throws IOException {
        return randomFile.skipBytes(n);
    }

    public void seek(long pos) throws IOException {
        randomFile.seek(pos);
    }

    public final int readUnsignedShort() throws IOException {
        return randomFile.readUnsignedShort();
    }

    public final int readUnsignedByte() throws IOException {
        return randomFile.readUnsignedByte();
    }

    public final String readUTF() throws IOException {
        return randomFile.readUTF();
    }

    public final short readShort() throws IOException {
        return randomFile.readShort();
    }

    public final long readLong() throws IOException {
        return randomFile.readLong();
    }

    public final String readLine() throws IOException {
        return randomFile.readLine();
    }

    public final int readInt() throws IOException {
        return randomFile.readInt();
    }

    public final void readFully(byte[] b, int off, int len) throws IOException {
        randomFile.readFully(b, off, len);
    }

    public final void readFully(byte[] b) throws IOException {
        randomFile.readFully(b);
    }

    public final float readFloat() throws IOException {
        return randomFile.readFloat();
    }

    public final double readDouble() throws IOException {
        return randomFile.readDouble();
    }

    public final char readChar() throws IOException {
        return randomFile.readChar();
    }

    public final byte readByte() throws IOException {
        return randomFile.readByte();
    }

    public final boolean readBoolean() throws IOException {
        return randomFile.readBoolean();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return randomFile.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return randomFile.read(b, off, len);
    }

    public int read() throws IOException {
        return randomFile.read();
    }

    public long length() throws IOException {
        return randomFile.length();
    }

    public long getFilePointer() throws IOException {
        return randomFile.getFilePointer();
    }

    public final FileDescriptor getFD() throws IOException {
        return randomFile.getFD();
    }

    public final FileChannel getChannel() {
        return randomFile.getChannel();
    }
}

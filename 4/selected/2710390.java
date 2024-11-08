package swisseph;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * This class is meant to be a wrapper to some read functionality of the
 * memory mapped RandomAccessFile class.
 */
public class FilePtrMap implements FilePtr {

    private RandomAccessFile fp;

    private FileChannel fc;

    private MappedByteBuffer mbb = null;

    private String fnamp;

    private long baseOffset;

    private long length;

    public static FilePtr get(String fnamp) throws IOException {
        try {
            RandomAccessFile fp = new java.io.RandomAccessFile(fnamp, SwissData.BFILE_R_ACCESS);
            return new FilePtrMap(fp, fnamp, 0, fp.length());
        } catch (FileNotFoundException ex) {
        }
        return null;
    }

    protected FilePtrMap(RandomAccessFile fp, String fnamp, long baseOffset, long nBytes) throws IOException {
        this.fp = fp;
        this.fnamp = fnamp;
        this.baseOffset = baseOffset;
        this.length = nBytes;
        fc = fp.getChannel();
        mbb = fc.map(FileChannel.MapMode.READ_ONLY, baseOffset, length());
    }

    public final void setBigendian(boolean bigendian) {
        mbb.order(bigendian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
    }

    public final byte readByte() throws IOException, EOFException {
        return mbb.get();
    }

    public final int readUnsignedByte() throws IOException, EOFException {
        return ((int) readByte()) & 0xff;
    }

    public final short readShort() throws IOException, EOFException {
        return mbb.getShort();
    }

    public final int readInt() throws IOException, EOFException {
        return mbb.getInt();
    }

    public final double readDouble() throws IOException, EOFException {
        return mbb.getDouble();
    }

    public final String readLine() throws IOException, EOFException {
        fp.seek(mbb.position() + baseOffset);
        String s = fp.readLine();
        mbb.position((int) (fp.getFilePointer() - baseOffset));
        return s;
    }

    public final void close() throws IOException {
        fnamp = "";
        RandomAccessFile fpTemp = fp;
        fp = null;
        mbb = null;
        fpTemp.close();
    }

    public final boolean isClosed() {
        return fp == null;
    }

    public final long getFilePointer() {
        return mbb.position();
    }

    public final long length() {
        return length;
    }

    public final void seek(long pos) {
        mbb.position((int) pos);
    }
}

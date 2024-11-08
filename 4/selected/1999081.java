package lrf.buffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

public class MappedReader implements Reader {

    FileChannel fc = null;

    MappedByteBuffer mbb = null;

    int ndx = 0;

    public MappedReader(File f) throws Exception {
        fc = new FileInputStream(f).getChannel();
        mbb = fc.map(MapMode.READ_ONLY, 0, fc.size());
        mbb.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public boolean canGet(int i) {
        try {
            return ndx + i < fc.size();
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void copy(int srcOff, byte[] dest, int destOff, int length) {
        mbb.position(ndx + srcOff);
        mbb.get(dest, destOff, length);
    }

    @Override
    public int get(int pos) {
        return mbb.get(ndx + pos);
    }

    @Override
    public int getByte() {
        return mbb.get(ndx);
    }

    @Override
    public int getByte(int offset) {
        return mbb.get(ndx + offset);
    }

    @Override
    public int getInt() {
        return mbb.getInt(ndx);
    }

    @Override
    public int getInt(boolean advance) {
        if (advance) {
            ndx += 4;
            return mbb.getInt(ndx - 4);
        }
        return mbb.getInt(ndx);
    }

    @Override
    public int getInt(int offset) {
        return mbb.getInt(ndx + offset);
    }

    @Override
    public int getPos() {
        return ndx;
    }

    @Override
    public int getShort() {
        return mbb.getShort(ndx);
    }

    @Override
    public int getShort(boolean advance) {
        if (advance) {
            ndx += 2;
            return mbb.getShort(ndx - 2);
        }
        return mbb.getShort(ndx);
    }

    @Override
    public int getShort(int offset) {
        return mbb.getShort(ndx + offset);
    }

    @Override
    public String getString(boolean advance) throws UnsupportedEncodingException {
        return null;
    }

    @Override
    public byte[] getSubBuf(int off, int len) {
        byte ret[] = new byte[len];
        copy(ndx + off, ret, 0, len);
        return ret;
    }

    @Override
    public Reader getSubReader(int off, int len) {
        byte ret[] = getSubBuf(off, len);
        return new ByteReader(ret, 0);
    }

    @Override
    public boolean isEmpty() {
        try {
            return ndx < (int) fc.size();
        } catch (IOException e) {
            return true;
        }
    }

    @Override
    public void reset() {
        ndx = 0;
    }

    @Override
    public long size() {
        try {
            return fc.size();
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public void skip(int skippedBytes) {
        ndx += skippedBytes;
    }

    @Override
    public int getByte(boolean advance) {
        int ret = getByte();
        if (advance) skip(1);
        return ret;
    }
}

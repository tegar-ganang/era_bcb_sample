package edu.rabbit.kernel.memory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;
import edu.rabbit.kernel.DbUtility;

/**
 * @author Yuanyan<yanyan.cao@gmail.com>
 * 
 * 
 */
public class DbByteBuffer implements IMemoryBuffer {

    protected ByteBuffer buffer;

    public DbByteBuffer() {
    }

    /**
     * @param b
     */
    public DbByteBuffer(ByteBuffer b) {
        buffer = b;
    }

    public void allocate(int size) {
        assert (size >= 0);
        buffer = ByteBuffer.allocate(size);
    }

    public void free() {
        assert (buffer != null);
        buffer = null;
    }

    public boolean isAllocated() {
        return buffer != null;
    }

    public int getSize() {
        assert (buffer != null);
        return buffer.capacity();
    }

    public IMemoryPointer getPointer(int pointer) {
        assert (buffer != null);
        assert (pointer >= 0);
        assert (pointer <= buffer.capacity());
        return new MemoryPointer(this, pointer);
    }

    public byte getByte(int pointer) {
        assert (buffer != null);
        assert (pointer >= 0);
        assert (pointer < buffer.capacity());
        return buffer.get(pointer);
    }

    public int getByteUnsigned(int pointer) {
        assert (buffer != null);
        assert (pointer >= 0);
        assert (pointer < buffer.capacity());
        return BytesUtility.toUnsignedByte(getByte(pointer));
    }

    public int getInt(int pointer) {
        assert (buffer != null);
        assert (pointer >= 0);
        assert (pointer <= buffer.capacity() - IMemoryManager.INT_SIZE);
        return buffer.getInt(pointer);
    }

    public long getIntUnsigned(int pointer) {
        assert (buffer != null);
        assert (pointer >= 0);
        assert (pointer <= buffer.capacity() - IMemoryManager.INT_SIZE);
        return BytesUtility.toUnsignedInt(getInt(pointer));
    }

    public long getLong(int pointer) {
        assert (buffer != null);
        assert (pointer >= 0);
        assert (pointer <= buffer.capacity() - IMemoryManager.LONG_SIZE);
        return buffer.getLong(pointer);
    }

    public short getShort(int pointer) {
        assert (buffer != null);
        assert (pointer >= 0);
        assert (pointer <= buffer.capacity() - IMemoryManager.SHORT_SIZE);
        return buffer.getShort(pointer);
    }

    public int getShortUnsigned(int pointer) {
        assert (buffer != null);
        assert (pointer >= 0);
        assert (pointer <= buffer.capacity() - IMemoryManager.SHORT_SIZE);
        return BytesUtility.toUnsignedShort(getShort(pointer));
    }

    public void putByte(int pointer, byte value) {
        assert (buffer != null);
        assert (pointer >= 0);
        assert (pointer < buffer.capacity());
        buffer.put(pointer, value);
    }

    public void putByteUnsigned(int pointer, int value) {
        assert (buffer != null);
        assert (pointer >= 0);
        assert (pointer < buffer.capacity());
        putByte(pointer, (byte) BytesUtility.toUnsignedByte(value));
    }

    public void putInt(int pointer, int value) {
        assert (buffer != null);
        assert (pointer >= 0);
        assert (pointer <= buffer.capacity() - IMemoryManager.INT_SIZE);
        buffer.putInt(pointer, value);
    }

    public void putIntUnsigned(int pointer, long value) {
        assert (buffer != null);
        assert (pointer >= 0);
        assert (pointer <= buffer.capacity() - IMemoryManager.INT_SIZE);
        putInt(pointer, (int) BytesUtility.toUnsignedInt(value));
    }

    public void putLong(int pointer, long value) {
        assert (buffer != null);
        assert (pointer >= 0);
        assert (pointer <= buffer.capacity() - IMemoryManager.LONG_SIZE);
        buffer.putLong(pointer, value);
    }

    public void putShort(int pointer, short value) {
        assert (buffer != null);
        assert (pointer >= 0);
        assert (pointer <= buffer.capacity() - IMemoryManager.SHORT_SIZE);
        buffer.putShort(pointer, value);
    }

    public void putShortUnsigned(int pointer, int value) {
        assert (buffer != null);
        assert (pointer >= 0);
        assert (pointer <= buffer.capacity() - IMemoryManager.SHORT_SIZE);
        putShort(pointer, (short) BytesUtility.toUnsignedShort(value));
    }

    public int readFromFile(int pointer, RandomAccessFile file, long position, int count) throws IOException {
        assert (buffer != null);
        assert (pointer >= 0);
        assert (pointer < buffer.capacity());
        assert (file != null);
        assert (position >= 0);
        assert (count > 0);
        buffer.limit(pointer + count).position(pointer);
        try {
            return file.getChannel().read(buffer, position);
        } finally {
            buffer.clear();
        }
    }

    public int writeToFile(int pointer, RandomAccessFile file, long position, int count) throws IOException {
        assert (buffer != null);
        assert (pointer >= 0);
        assert (pointer < buffer.capacity());
        assert (file != null);
        assert (position >= 0);
        assert (count > 0);
        buffer.limit(pointer + count).position(pointer);
        try {
            return file.getChannel().write(buffer, position);
        } finally {
            buffer.clear();
        }
    }

    public byte[] asArray() {
        return buffer.array();
    }

    public void copyFrom(int dstPos, IMemoryBuffer src, int srcPos, int count) {
        if (src instanceof DbByteBuffer && !(src instanceof DirectByteBuffer)) {
            final DbByteBuffer srcBuf = (DbByteBuffer) src;
            System.arraycopy(srcBuf.buffer.array(), srcPos, buffer.array(), dstPos, count);
        } else {
            final byte[] b = new byte[count];
            src.getBytes(srcPos, b, 0, count);
            putBytes(dstPos, b, 0, count);
        }
    }

    public void fill(int from, int count, byte value) {
        Arrays.fill(buffer.array(), from, from + count, value);
    }

    public void getBytes(int pointer, byte[] bytes, int to, int count) {
        System.arraycopy(buffer.array(), pointer, bytes, to, count);
    }

    public void putBytes(int pointer, byte[] bytes, int from, int count) {
        System.arraycopy(bytes, from, buffer.array(), pointer, count);
    }

    public int compareTo(int pointer, IMemoryBuffer buffer, int bufferPointer) {
        final int thisCount = getSize() - pointer;
        final int bufferCount = buffer.getSize() - bufferPointer;
        if (thisCount != bufferCount) {
            if (thisCount > bufferCount) {
                return 1;
            } else {
                return -1;
            }
        }
        if (buffer instanceof DbByteBuffer && !(buffer instanceof DirectByteBuffer)) {
            final DbByteBuffer b = (DbByteBuffer) buffer;
            return DbUtility.memcmp(this.buffer.array(), pointer, b.buffer.array(), bufferPointer, thisCount);
        } else {
            final byte[] b = new byte[thisCount];
            buffer.getBytes(bufferPointer, b, 0, thisCount);
            return DbUtility.memcmp(this.buffer.array(), pointer, b, bufferPointer, thisCount);
        }
    }
}

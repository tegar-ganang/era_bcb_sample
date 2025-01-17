package jpcsp.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import jpcsp.Memory;

public class FIFOByteBuffer {

    private byte[] buffer;

    private int bufferReadOffset;

    private int bufferWriteOffset;

    private int bufferLength;

    public FIFOByteBuffer() {
        buffer = new byte[0];
        clear();
    }

    public FIFOByteBuffer(byte[] buffer) {
        this.buffer = buffer;
        bufferReadOffset = 0;
        bufferWriteOffset = 0;
        bufferLength = buffer.length;
    }

    private int incrementOffset(int offset, int n) {
        offset += n;
        if (offset >= buffer.length) {
            offset -= buffer.length;
        } else if (offset < 0) {
            offset += buffer.length;
        }
        return offset;
    }

    public void clear() {
        bufferReadOffset = 0;
        bufferWriteOffset = 0;
        bufferLength = 0;
    }

    private void checkBufferForWrite(int length) {
        if (bufferLength + length > buffer.length) {
            byte[] extendedBuffer = new byte[bufferLength + length];
            if (bufferReadOffset + bufferLength <= buffer.length) {
                System.arraycopy(buffer, bufferReadOffset, extendedBuffer, 0, bufferLength);
            } else {
                int lengthEndBuffer = buffer.length - bufferReadOffset;
                System.arraycopy(buffer, bufferReadOffset, extendedBuffer, 0, lengthEndBuffer);
                System.arraycopy(buffer, 0, extendedBuffer, lengthEndBuffer, bufferLength - lengthEndBuffer);
            }
            buffer = extendedBuffer;
            bufferReadOffset = 0;
            bufferWriteOffset = bufferLength;
        }
    }

    private void copyToBuffer(int offset, int length, Buffer src) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, offset, length);
        Utilities.putBuffer(byteBuffer, src, ByteOrder.LITTLE_ENDIAN, length);
    }

    public void write(Buffer src, int length) {
        if (buffer == null) {
            return;
        }
        checkBufferForWrite(length);
        if (bufferWriteOffset + length <= buffer.length) {
            copyToBuffer(bufferWriteOffset, length, src);
        } else {
            int lengthEndBuffer = buffer.length - bufferWriteOffset;
            copyToBuffer(bufferWriteOffset, lengthEndBuffer, src);
            copyToBuffer(0, length - lengthEndBuffer, src);
        }
        bufferWriteOffset = incrementOffset(bufferWriteOffset, length);
        bufferLength += length;
    }

    public void write(int address, int length) {
        if (length > 0 && Memory.isAddressGood(address)) {
            Buffer memoryBuffer = Memory.getInstance().getBuffer(address, length);
            write(memoryBuffer, length);
        }
    }

    public void write(ByteBuffer src) {
        write(src, src.remaining());
    }

    public void write(byte[] src) {
        write(ByteBuffer.wrap(src), src.length);
    }

    public void write(byte[] src, int offset, int length) {
        write(ByteBuffer.wrap(src, offset, length), length);
    }

    public int readByteBuffer(ByteBuffer dst) {
        if (buffer == null) {
            return 0;
        }
        int length = dst.remaining();
        if (length > bufferLength) {
            length = bufferLength;
        }
        if (bufferReadOffset + length > buffer.length) {
            int lengthEndBuffer = buffer.length - bufferReadOffset;
            dst.put(buffer, bufferReadOffset, lengthEndBuffer);
            dst.put(buffer, 0, length - lengthEndBuffer);
        } else {
            dst.put(buffer, bufferReadOffset, length);
        }
        bufferReadOffset = incrementOffset(bufferReadOffset, length);
        bufferLength -= length;
        return length;
    }

    public boolean forward(int length) {
        if (buffer == null || length < 0) {
            return false;
        }
        if (length == 0) {
            return true;
        }
        if (length > bufferLength) {
            return false;
        }
        bufferLength -= length;
        bufferReadOffset = incrementOffset(bufferReadOffset, length);
        return true;
    }

    public boolean rewind(int length) {
        if (buffer == null || length < 0) {
            return false;
        }
        if (length == 0) {
            return true;
        }
        int maxRewindLength = buffer.length - bufferLength;
        if (length > maxRewindLength) {
            return false;
        }
        bufferLength += length;
        bufferReadOffset = incrementOffset(bufferReadOffset, -length);
        return true;
    }

    public int length() {
        return bufferLength;
    }

    public void delete() {
        buffer = null;
    }

    @Override
    public String toString() {
        return String.format("FIFOByteBuffer(size=%d, bufferLength=%d, readOffset=%d, writeOffset=%d)", buffer.length, bufferLength, bufferReadOffset, bufferWriteOffset);
    }
}

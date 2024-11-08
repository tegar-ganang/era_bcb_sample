package se.slackers.locality.data;

import java.nio.ByteBuffer;

public class CircularBuffer {

    private ByteBuffer buffer;

    private int readIndex = 0;

    private int writeIndex = 0;

    public CircularBuffer(int bufferSize) {
        buffer = ByteBuffer.allocateDirect(bufferSize);
    }

    public void reset() {
        readIndex = 0;
        writeIndex = 0;
    }

    public int read(byte[] dest, int offset, int length) {
        assert length < buffer.capacity() : "The requested read is bigger than the buffer";
        if (writeIndex == readIndex) {
            return 0;
        }
        buffer.position(readIndex);
        if (writeIndex < readIndex) {
            int remainder = buffer.remaining();
            if (remainder < length) {
                buffer.get(dest, offset, remainder);
                offset += remainder;
                length -= remainder;
                readIndex = 0;
                buffer.position(readIndex);
                int space = writeIndex - readIndex;
                if (space <= length) {
                    length = space;
                }
                buffer.get(dest, offset, length);
                readIndex += length;
                return remainder + length;
            } else {
                buffer.get(dest, offset, remainder);
                readIndex += remainder;
                return remainder;
            }
        } else {
            int space = writeIndex - readIndex;
            if (space <= length) {
                length = space;
            }
            buffer.get(dest, offset, length);
            readIndex += length;
            return length;
        }
    }

    public boolean write(byte[] source, int offset, int length) {
        assert length < buffer.capacity() : "The requested write is bigger than the buffer";
        buffer.position(writeIndex);
        if ((readIndex <= writeIndex && writeIndex + length < buffer.capacity()) || (writeIndex < readIndex && length < readIndex - writeIndex)) {
            buffer.put(source, offset, length);
            writeIndex += length;
            return true;
        } else {
            int remainder = buffer.remaining();
            if (readIndex < writeIndex && length > readIndex + remainder) {
                return false;
            }
            if (writeIndex < readIndex && length > readIndex - writeIndex) {
                return false;
            }
            buffer.put(source, offset, remainder);
            offset += remainder;
            length -= remainder;
            writeIndex = 0;
            buffer.position(writeIndex);
            assert length < readIndex : "There is not enough room for this write operation";
            buffer.put(source, offset, length);
            writeIndex += length;
            return true;
        }
    }

    public boolean isEmpty() {
        return writeIndex == readIndex;
    }

    public boolean isFull() {
        if (writeIndex + 1 <= buffer.capacity() && writeIndex + 1 == readIndex) return true;
        if (writeIndex == buffer.capacity() - 1 && readIndex == 0) return true;
        return false;
    }
}

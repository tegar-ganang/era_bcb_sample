package se.slackers.locality.data;

import java.nio.ByteBuffer;
import se.slackers.locality.exception.InvalidBufferPositionException;

public class ExpandOnWriteCircularBuffer {

    private ByteBuffer buffer;

    private int readOffset = 0;

    private int readIndex = 0;

    private int writeIndex = 0;

    public ExpandOnWriteCircularBuffer(int bufferSize) {
        buffer = ByteBuffer.allocateDirect(bufferSize);
    }

    public void reset() {
        readOffset = 0;
        readIndex = 0;
        writeIndex = 0;
    }

    public synchronized int read(int position, byte[] dest, int destOffset, int destLength) {
        assert destLength < buffer.capacity() : "The requested read is bigger than the buffer";
        if (position < readOffset) {
            throw new InvalidBufferPositionException("Read position " + position + " is smaller than [" + readOffset + "]");
        }
        int offset = position - readOffset;
        if (offset > getBytesInBuffer()) {
            throw new InvalidBufferPositionException("Read position " + position + " is larger then [" + (readOffset + getBytesInBuffer()) + "]");
        }
        if (writeIndex == readIndex) {
            return 0;
        }
        buffer.position(offset);
        if (writeIndex < readIndex) {
            int remainder = buffer.remaining();
            if (remainder < destLength) {
                buffer.get(dest, destOffset, remainder);
                destOffset += remainder;
                destLength -= remainder;
                buffer.position(0);
                int space = writeIndex - 0;
                if (space <= destLength) {
                    destLength = space;
                }
                buffer.get(dest, destOffset, destLength);
                return remainder + destLength;
            } else {
                buffer.get(dest, destOffset, remainder);
                return remainder;
            }
        } else {
            int space = writeIndex - offset;
            if (space <= destLength) {
                destLength = space;
            }
            buffer.get(dest, destOffset, destLength);
            return destLength;
        }
    }

    public synchronized boolean write(byte[] source, int offset, int length) {
        assert length < buffer.capacity() : "The requested write is bigger than the buffer";
        buffer.position(writeIndex);
        if (length < buffer.capacity() - getBytesInBuffer()) {
            if (writeIndex <= readIndex) {
                assert (readIndex - writeIndex) == (buffer.capacity() - getBytesInBuffer()) : "Buffer size is invalid";
                buffer.put(source, offset, length);
                writeIndex += length;
                return true;
            } else {
                int remainder = buffer.remaining();
                if (length < remainder) {
                    buffer.put(source, offset, length);
                    writeIndex += length;
                    return true;
                } else {
                    buffer.put(source, offset, remainder);
                    buffer.position(0);
                    buffer.put(source, offset + remainder, length - remainder);
                    writeIndex = length - remainder;
                    return true;
                }
            }
        } else {
            int remainder = buffer.remaining();
            if (length < remainder) {
                buffer.put(source, offset, length);
                writeIndex += length;
            } else {
                buffer.put(source, offset, remainder);
                buffer.position(0);
                buffer.put(source, offset + remainder, length - remainder);
                writeIndex = length - remainder;
            }
            int oldRead = readIndex;
            readIndex = writeIndex + 1;
            if (readIndex >= buffer.capacity()) {
                readIndex -= buffer.capacity();
            }
            if (readIndex < oldRead) {
                readOffset += buffer.capacity() - oldRead + readIndex;
            } else {
                readOffset += readIndex - oldRead;
            }
            return true;
        }
    }

    public int getReadOffset() {
        return readOffset;
    }

    public boolean isEmpty() {
        return writeIndex == readIndex;
    }

    public boolean isFull() {
        if (writeIndex + 1 <= buffer.capacity() && writeIndex + 1 == readIndex) return true;
        if (writeIndex == buffer.capacity() - 1 && readIndex == 0) return true;
        return false;
    }

    private int getBytesInBuffer() {
        if (writeIndex < readIndex) {
            return (buffer.capacity() - readIndex) + writeIndex;
        }
        return writeIndex - readIndex;
    }
}

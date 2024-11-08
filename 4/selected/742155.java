package net.sf.asyncobjects.io;

/**
 * actually JDK 1.4 buffer should be used instead of this one, but there is an
 * intention to make sources compatible with 1.3 and personal profile platform.
 * 
 * Also there is only one kind of buffer to simplify compatibility issues.
 * 
 * This class is not thread safe.
 * 
 * @author const
 */
public final class ByteQueue {

    /** read position in buffer */
    int readPosition;

    /**
	 * write position in buffer, write position could be more then buffer length
	 * this will mean that buffer cyclically wrapped
	 */
    int writePosition;

    /** data of buffer */
    byte data[];

    /** size of data to allocate */
    final int dataSize;

    /**
	 * size of buffer
	 * 
	 * @param size
	 *            buffer size
	 */
    public ByteQueue(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("wanted buffer it too short: " + size);
        }
        dataSize = size;
    }

    /**
	 * avaiable for get
	 * 
	 * @return amount of bytes avaiable for get
	 */
    public int availableForGet() {
        return writePosition - readPosition;
    }

    /**
	 * @return amount of bytes available for put
	 */
    public int availableForPut() {
        return dataSize - (writePosition - readPosition);
    }

    /**
	 * consume some bytes in buffer
	 * 
	 * @param n
	 *            amount of bytes to consume
	 */
    public void skip(int n) {
        if (n > availableForGet()) {
            throw new IllegalArgumentException("cannot skip so much");
        }
        readPosition += n;
        if (readPosition == writePosition) {
            readPosition = 0;
            writePosition = 0;
        } else if (readPosition >= data.length) {
            readPosition -= data.length;
            writePosition -= data.length;
        }
    }

    /**
	 * Get bytes into array
	 * 
	 * @param buffer
	 *            an target array
	 * @return amount of received bytes
	 */
    public int get(final byte buffer[]) {
        return get(buffer, 0, buffer.length);
    }

    /**
	 * Get bytes into array
	 * 
	 * @param buffer
	 *            an target array
	 * @param start
	 *            start position in target array
	 * @param length
	 *            maximum amount of bytes to copy
	 * @return amount of received bytes
	 */
    public int get(final byte buffer[], final int start, final int length) {
        final int rc = peek(0, buffer, start, length);
        skip(rc);
        return rc;
    }

    /**
	 * Peek bytes
	 * 
	 * @param offset
	 *            a position inside data
	 * @param buffer
	 *            a target buffer
	 * @param start
	 *            a start position in the buffer
	 * @param length
	 *            max amount to get
	 * @return amount of actually recived data
	 */
    public int peek(final int offset, final byte buffer[], final int start, final int length) {
        if (offset > availableForGet()) {
            throw new IllegalArgumentException("offset too big " + offset + " > " + availableForGet());
        }
        final int toPeek = Math.min(availableForGet() - offset, length);
        if (toPeek > 0) {
            if (readPosition + offset + toPeek < data.length) {
                System.arraycopy(data, readPosition + offset, buffer, start, toPeek);
            } else if (readPosition + offset >= data.length) {
                System.arraycopy(data, readPosition + offset - data.length, buffer, start, toPeek);
            } else {
                final int half = data.length - (readPosition + offset);
                System.arraycopy(data, readPosition + offset, buffer, start, half);
                System.arraycopy(data, 0, buffer, start + half, toPeek - half);
            }
        }
        return toPeek;
    }

    /**
	 * Peek bytes
	 * 
	 * @param buffer
	 *            a target buffer
	 * @return amount of actually recived data
	 */
    public int put(final byte buffer[]) {
        return put(buffer, 0, buffer.length);
    }

    /**
	 * Peek bytes
	 * 
	 * @param buffer
	 *            a target buffer
	 * @param start
	 *            a start position in the buffer
	 * @param length
	 *            max amount to get
	 * @return amount of actually recived data
	 */
    public int put(final byte buffer[], final int start, final int length) {
        ensureBacked();
        final int toPut = Math.min(availableForPut(), length);
        if (toPut > 0) {
            if (writePosition + toPut < data.length) {
                System.arraycopy(buffer, start, data, writePosition, toPut);
            } else if (writePosition >= data.length) {
                System.arraycopy(buffer, start, data, writePosition - data.length, toPut);
            } else {
                final int half = data.length - writePosition;
                System.arraycopy(buffer, start, data, writePosition, half);
                System.arraycopy(buffer, start + half, data, 0, toPut - half);
            }
            writePosition += toPut;
        }
        return toPut;
    }

    /**
	 * transfer data from one buffer to another, this method gets data from
	 * first buffer by method gets, so data is consumed.
	 * 
	 * @param buffer
	 *            source buffer
	 * @return amount of bytes put into buffer
	 */
    public int transferFrom(ByteQueue buffer) {
        ensureBacked();
        final int toPut = Math.min(availableForPut(), buffer.availableForGet());
        if (toPut > 0) {
            if (writePosition + toPut < data.length) {
                buffer.get(data, writePosition, toPut);
            } else if (writePosition >= data.length) {
                buffer.get(data, writePosition - data.length, toPut);
            } else {
                final int half = data.length - writePosition;
                buffer.get(data, writePosition, half);
                buffer.get(data, 0, toPut - half);
            }
            writePosition += toPut;
        }
        return toPut;
    }

    /**
	 * @return true if queue is empty
	 */
    public boolean isEmpty() {
        return availableForGet() == 0;
    }

    /**
	 * @return true if queue is full
	 */
    public boolean isFull() {
        return availableForPut() == 0;
    }

    /**
	 * clear queue from all elements
	 */
    public void clear() {
        writePosition = 0;
        readPosition = 0;
    }

    /**
	 * Ensure that queue is backed
	 */
    void ensureBacked() {
        if (data == null) {
            data = new byte[dataSize];
        }
    }

    /**
	 * @return capacity of bytequeue
	 */
    public int capacity() {
        return dataSize;
    }
}

package se.slackers.locality.data;

public class TimeOffsetBuffer {

    private int[] offset;

    private long[] time;

    private int bufferSize;

    private int readIndex;

    private int writeIndex;

    public TimeOffsetBuffer(int bufferSize) {
        this.bufferSize = bufferSize;
        this.offset = new int[bufferSize];
        this.time = new long[bufferSize];
        readIndex = 0;
        writeIndex = 0;
    }

    public synchronized void write(int offset, int time) {
        this.offset[writeIndex] = offset;
        this.time[writeIndex] = time;
        increaseWriteIndex();
    }

    /**
	 * Get the closest offset for the given time.
	 * @param time
	 * @return -1 if the time is larger than any time in the buffer
	 */
    public synchronized int getOffset(long time) {
        for (int i = 1; i < getBufferContentSize(); i++) {
            int index = (readIndex + i) % bufferSize;
            if (this.time[index] > time) {
                if (index > 0) {
                    return this.offset[index - 1];
                } else {
                    return this.offset[bufferSize - 1];
                }
            }
        }
        return -1;
    }

    public long getMinTime() {
        return this.time[readIndex];
    }

    public long getMaxTime() {
        if (writeIndex > 0) {
            return this.offset[writeIndex - 1];
        } else {
            return this.offset[bufferSize - 1];
        }
    }

    /**
	 * Returns the number of elements in the buffer.
	 * @return
	 */
    private int getBufferContentSize() {
        if (readIndex <= writeIndex) {
            return writeIndex - readIndex;
        }
        return bufferSize - readIndex + writeIndex;
    }

    /**
	 * Increases the write index by one. This method also adjusts the
	 * readIndex.
	 */
    private void increaseWriteIndex() {
        writeIndex++;
        if (writeIndex == readIndex) {
            readIndex++;
        }
        if (writeIndex >= bufferSize) {
            writeIndex -= bufferSize;
        }
        if (readIndex >= bufferSize) {
            readIndex -= bufferSize;
        }
    }
}

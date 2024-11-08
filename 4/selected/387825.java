package de.dfki.lt.signalproc.util;

import de.dfki.lt.signalproc.process.InlineDataProcessor;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class BufferedDoubleDataSource extends BaseDoubleDataSource {

    public static final int DEFAULT_BUFFERSIZE = 8192;

    protected double[] buf;

    protected int readPos = 0;

    protected int writePos = 0;

    protected InlineDataProcessor dataProcessor = null;

    public BufferedDoubleDataSource(double[] inputData) {
        this(inputData, null);
    }

    public BufferedDoubleDataSource(double[] inputData, InlineDataProcessor dataProcessor) {
        super();
        buf = new double[inputData.length];
        System.arraycopy(inputData, 0, buf, 0, buf.length);
        writePos = buf.length;
        dataLength = buf.length;
        this.dataProcessor = dataProcessor;
        if (dataProcessor != null) dataProcessor.applyInline(buf, 0, writePos);
    }

    public BufferedDoubleDataSource(DoubleDataSource inputSource) {
        this(inputSource, null);
    }

    public BufferedDoubleDataSource(DoubleDataSource inputSource, InlineDataProcessor dataProcessor) {
        super(inputSource);
        buf = new double[DEFAULT_BUFFERSIZE];
        this.dataProcessor = dataProcessor;
    }

    /**
     * Whether or not any more data can be read from this data source.
     * @return true if another call to getData() will return data, false otherwise.
     */
    public boolean hasMoreData() {
        if (currentlyInBuffer() > 0 || inputSource != null && inputSource.hasMoreData()) return true;
        return false;
    }

    /**
     * Amount of data currently in the buffer. If hasMoreData() returns true,
     * this number may increase.
     * @return the number of doubles that can currently be read without recourse
     * to an input source.
     */
    public int currentlyInBuffer() {
        assert writePos >= readPos;
        return writePos - readPos;
    }

    /**
     * The number of doubles that can currently be read from this 
     * double data source without blocking. This number can change over time.
     * @return the number of doubles that can currently be read without blocking 
     */
    public int available() {
        int available = currentlyInBuffer();
        if (inputSource != null) available += inputSource.available();
        return available;
    }

    protected int bufferSpaceLeft() {
        return buf.length - currentlyInBuffer();
    }

    /**
     * Try to get length doubles from this DoubleDataSource, and copy them into target, starting from targetPos.
     * @param target the double array to write into
     * @param targetPos position in target where to start writing
     * @param length the amount of data requested
     * @return the amount of data actually delivered. If the returned value is less than length,
     * only that many data items have been copied into target; further calls will return 0 and not copy anything.
     */
    public int getData(double[] target, int targetPos, int length) {
        if (currentlyInBuffer() < length) {
            readIntoBuffer(length - currentlyInBuffer());
        }
        int toDeliver = length;
        if (currentlyInBuffer() < length) toDeliver = currentlyInBuffer();
        System.arraycopy(buf, readPos, target, targetPos, toDeliver);
        readPos += toDeliver;
        return toDeliver;
    }

    /**
     * Attempt to get more data from the input source. If less than this can be read,
     * the possible amount will be read, but canReadMore() will return false afterwards.
     * @param minLength the amount of data to get from the input source
     * @return true if the requested amount could be read, false if none or less data could be read.
     */
    protected boolean readIntoBuffer(int minLength) {
        if (inputSource == null) {
            return false;
        }
        if (!inputSource.hasMoreData()) {
            return false;
        }
        if (bufferSpaceLeft() < minLength) {
            increaseBufferSize(minLength + currentlyInBuffer());
        } else if (buf.length - writePos < minLength) {
            compact();
        }
        int readSum = 0;
        readSum = inputSource.getData(buf, writePos, minLength);
        writePos += readSum;
        if (dataProcessor != null) {
            dataProcessor.applyInline(buf, writePos - readSum, readSum);
        }
        return readSum == minLength;
    }

    /**
     * Increase the underlying buffer array in size, so that the new size is
     * at least minSize
     * @param minSize the minimum new size of the array.
     */
    protected void increaseBufferSize(int minSize) {
        int newLength = buf.length;
        while (newLength < minSize) newLength *= 2;
        double[] newBuf = new double[newLength];
        int avail = currentlyInBuffer();
        System.arraycopy(buf, readPos, newBuf, 0, avail);
        buf = newBuf;
        readPos = 0;
        writePos = avail;
    }

    /**
     * Compact the buffer, so that the data in the buffer starts at the beginning of the
     * underlying array. 
     *
     */
    protected void compact() {
        if (readPos == 0) return;
        int avail = writePos - readPos;
        System.arraycopy(buf, readPos, buf, 0, avail);
        readPos = 0;
        writePos = avail;
    }
}

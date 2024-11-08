package org.caleigo.toolkit.util;

public class CircularByteBuffer {

    protected byte[] mBuffer;

    protected int mBufferSize;

    protected int mCurrentReadPosition;

    protected int mCurrentWritePosition;

    protected int mAvailableSpace;

    public CircularByteBuffer(int bufferSize) {
        mBuffer = new byte[bufferSize];
        mBufferSize = bufferSize;
        mAvailableSpace = mBufferSize;
    }

    public void addToBuffer(byte[] b, int off, int len) {
        synchronized (mBuffer) {
            while (this.getAvailableBufferSpace() < len) this.resizeBuffer();
            int readIndex = off;
            int writeIndex = mCurrentWritePosition;
            while (readIndex < off + len && writeIndex < mBuffer.length) {
                mBuffer[writeIndex] = b[readIndex];
                readIndex++;
                writeIndex++;
            }
            if (readIndex < off + len) {
                writeIndex = 0;
                while (readIndex < off + len) {
                    mBuffer[writeIndex] = b[readIndex];
                    readIndex++;
                    writeIndex++;
                }
            }
            mCurrentWritePosition = (writeIndex < mBuffer.length ? writeIndex : 0);
            mAvailableSpace -= len;
        }
    }

    public int getFromBuffer(byte[] b, int off, int len) {
        int nbrOfBytesRead = 0;
        synchronized (mBuffer) {
            int readIndex = mCurrentReadPosition;
            int writeIndex = off;
            if (mCurrentReadPosition >= mCurrentWritePosition) {
                while (readIndex < mBuffer.length && nbrOfBytesRead < len) {
                    b[writeIndex] = mBuffer[readIndex];
                    readIndex++;
                    writeIndex++;
                    nbrOfBytesRead++;
                }
                if (nbrOfBytesRead < len) readIndex = 0;
            }
            while (readIndex < mCurrentWritePosition && nbrOfBytesRead < len) {
                b[writeIndex] = mBuffer[readIndex];
                readIndex++;
                writeIndex++;
                nbrOfBytesRead++;
            }
            mCurrentReadPosition = (readIndex < mBuffer.length ? readIndex : 0);
            mAvailableSpace += nbrOfBytesRead;
        }
        return nbrOfBytesRead;
    }

    public int getBufferSize() {
        synchronized (mBuffer) {
            return (mBuffer.length - mAvailableSpace);
        }
    }

    protected void resizeBuffer() {
        synchronized (mBuffer) {
            byte[] newBuffer = new byte[mBuffer.length + mBufferSize / 2];
            int bufferSize = this.getBufferSize();
            this.getFromBuffer(newBuffer, 0, bufferSize);
            mCurrentReadPosition = 0;
            mCurrentWritePosition = bufferSize;
            mBuffer = newBuffer;
            mAvailableSpace = mBuffer.length - bufferSize;
        }
    }

    protected int getAvailableBufferSpace() {
        synchronized (mBuffer) {
            return mAvailableSpace;
        }
    }
}

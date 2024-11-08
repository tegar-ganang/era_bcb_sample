package alt.viiigine.formats.utils;

/**
 *
 * @author Dmitry S. Vorobiev
 */
public class CyclicBuffer {

    protected byte[] buffer = null;

    protected int readPosition = 0;

    protected int writePosition = 0;

    public CyclicBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

    public CyclicBuffer(byte[] buffer, int position) {
        this.buffer = buffer;
        this.readPosition = position;
        this.writePosition = position;
    }

    public CyclicBuffer(byte[] buffer, int readPosition, int writePosition) {
        this.buffer = buffer;
        this.readPosition = readPosition;
        this.writePosition = writePosition;
    }

    public int read(byte[] dstBuffer, int offset, int length) {
        int bytesRead = 0;
        if (!(this.readPosition + length < this.buffer.length)) {
            for (int i = 0; i < this.buffer.length - this.readPosition; i++) dstBuffer[offset + i] = this.buffer[this.readPosition + i];
            bytesRead += this.buffer.length - this.readPosition;
            length -= bytesRead;
            offset += bytesRead;
            this.readPosition = 0;
        }
        for (int i = 0; i < length; i++) dstBuffer[offset + i] = this.buffer[this.readPosition + i];
        bytesRead += length;
        this.readPosition += length;
        this.readPosition = this.readPosition % this.buffer.length;
        return bytesRead;
    }

    public int readWrite(byte[] dstBuffer, int offset, int length) {
        int bytesReadWritten = 0;
        while (this.readPosition != this.writePosition && bytesReadWritten < length) {
            dstBuffer[offset + bytesReadWritten] = this.buffer[this.readPosition];
            this.buffer[this.writePosition] = dstBuffer[offset + bytesReadWritten];
            bytesReadWritten++;
            this.readPosition++;
            this.writePosition++;
            this.readPosition = this.readPosition % this.buffer.length;
            this.writePosition = this.writePosition % this.buffer.length;
        }
        return bytesReadWritten;
    }

    public int write(byte[] srcBuffer, int offset, int length) {
        int bytesWritten = 0;
        if (!(this.writePosition + length < this.buffer.length)) {
            for (int i = 0; i < this.buffer.length - this.writePosition; i++) this.buffer[this.writePosition + i] = srcBuffer[offset + i];
            bytesWritten += this.buffer.length - this.writePosition;
            length -= bytesWritten;
            offset += bytesWritten;
            this.writePosition = 0;
        }
        for (int i = 0; i < length; i++) this.buffer[this.writePosition + i] = srcBuffer[offset + i];
        bytesWritten += length;
        this.writePosition += length;
        this.writePosition = this.writePosition % this.buffer.length;
        return bytesWritten;
    }

    /**
     * @return the readPosition
     */
    public int getReadPosition() {
        return readPosition;
    }

    /**
     * @param readPosition the readPosition to set
     */
    public void setReadPosition(int readPosition) {
        if (readPosition < 0) {
            while (readPosition < 0) readPosition += this.buffer.length;
        }
        if (readPosition >= this.buffer.length) {
            readPosition = readPosition % this.buffer.length;
        }
        this.readPosition = readPosition;
    }

    /**
     * @return the writePosition
     */
    public int getWritePosition() {
        return writePosition;
    }

    /**
     * @param writePosition the writePosition to set
     */
    public void setWritePosition(int writePosition) {
        if (writePosition < 0) {
            while (writePosition < 0) writePosition += this.buffer.length;
        }
        if (writePosition >= this.buffer.length) {
            writePosition = writePosition % this.buffer.length;
        }
        this.writePosition = writePosition;
    }
}

package se.slackers.locality.media.reader;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.apache.log4j.Logger;

public class ByteStreamReader {

    private static final Logger log = Logger.getLogger(ByteStreamReader.class);

    private InputStream inputStream = null;

    private ByteBuffer buffer = null;

    private byte[] tempbuffer = null;

    private int readIndex = 0;

    private int writeIndex = 0;

    private int indexOffset = 0;

    public ByteStreamReader() {
        buffer = ByteBuffer.allocateDirect(1024 * 1024);
        tempbuffer = new byte[65536];
    }

    /**
	 * Returns the position of the read-cursor 
	 * @return
	 */
    public int getOffset() {
        return indexOffset + readIndex;
    }

    /**
	 * Gets one byte from the current position.
	 * @return
	 * @throws IOException
	 */
    public byte read() throws IOException {
        if (readIndex >= writeIndex) {
            if (0 == readData()) {
                throw new EOFException();
            }
        }
        return buffer.get(readIndex++);
    }

    /**
	 * 
	 * @param destbuffer
	 * @param offset
	 * @param length
	 * @throws IOException
	 */
    public void read(byte[] destbuffer, int offset, int length) throws IOException {
        assert length < tempbuffer.length : "The requested data is bigger than the tempbuffer";
        if (readIndex + length >= writeIndex) {
            if (0 == readData()) {
                throw new EOFException();
            }
        }
        buffer.position(readIndex);
        buffer.get(destbuffer, offset, length);
        readIndex += length;
    }

    private int readData() throws IOException {
        if (buffer.limit() - writeIndex < tempbuffer.length) {
            recycleBuffer();
        }
        int bytes = inputStream.read(tempbuffer, 0, tempbuffer.length);
        buffer.position(writeIndex);
        buffer.put(tempbuffer, 0, bytes);
        writeIndex += bytes;
        log.info("[" + bytes + " bytes read]");
        return bytes;
    }

    /**
	 * Recycle the buffer
	 */
    private void recycleBuffer() {
        if (readIndex <= 0) {
            return;
        }
        log.info("Recycling ByteBuffer");
        int indexAdjustment = readIndex;
        int recycleIndex = 0;
        while (writeIndex - readIndex > tempbuffer.length) {
            buffer.position(readIndex);
            buffer.get(tempbuffer, 0, tempbuffer.length);
            buffer.position(recycleIndex);
            buffer.put(tempbuffer, 0, tempbuffer.length);
            recycleIndex += tempbuffer.length;
            readIndex += tempbuffer.length;
        }
        if (writeIndex - readIndex > 0) {
            buffer.position(readIndex);
            buffer.get(tempbuffer, 0, writeIndex - readIndex);
            buffer.position(recycleIndex);
            buffer.put(tempbuffer, 0, writeIndex - readIndex);
        }
        indexOffset += indexAdjustment;
        writeIndex -= indexAdjustment;
        readIndex = 0;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }
}

package net.sf.asyncobjects.io;

import java.nio.ByteBuffer;

/**
 * These are utility classes that help to adapt to JDK 1.4 buffers
 */
public class ByteQueueUtils {

    /**
	 * get byte buffer for write if there is one available. Note that buffer
	 * could be smaller then avaiable for put, in this case, successive call
	 * will return new buffer.
	 */
    public static ByteBuffer[] getWritableBuffers(ByteQueue b, ByteBuffer rc[]) {
        b.ensureBacked();
        if (b.readPosition == 0) {
            rc[0] = ByteBuffer.wrap(b.data, b.writePosition, b.data.length - b.writePosition);
            rc[1] = null;
        } else if (b.writePosition >= b.data.length) {
            rc[0] = ByteBuffer.wrap(b.data, b.writePosition - b.data.length, b.readPosition);
            rc[1] = null;
        } else {
            int half = b.data.length * 2 - b.writePosition;
            rc[0] = ByteBuffer.wrap(b.data, b.writePosition, half);
            rc[1] = ByteBuffer.wrap(b.data, 0, b.readPosition);
        }
        return rc;
    }

    /** ajust data buffer according to changes in byte buffer */
    public static void adjustWritableBuffers(ByteQueue b, ByteBuffer bb[]) {
        if (b.readPosition == 0) {
            b.writePosition = bb[0].position();
        } else if (b.writePosition >= b.data.length) {
            b.writePosition = bb[0].position() + b.data.length;
        } else {
            if (bb[1].position() == 0) {
                b.writePosition = bb[0].position();
            } else {
                b.writePosition = bb[1].position() + b.data.length;
            }
        }
    }

    /**
	 * get byte buffer for read if there is one available. Note that buffer
	 * could be smaller then avaiable for put, in this case, successive call
	 * will return new buffer.
	 */
    public static ByteBuffer[] getReadableBuffers(ByteQueue b, ByteBuffer[] rc) {
        b.ensureBacked();
        if (b.writePosition <= b.data.length) {
            rc[0] = ByteBuffer.wrap(b.data, b.readPosition, b.writePosition - b.readPosition);
            rc[1] = null;
        } else {
            rc[0] = ByteBuffer.wrap(b.data, b.readPosition, b.data.length - b.readPosition);
            rc[1] = ByteBuffer.wrap(b.data, 0, b.writePosition - b.data.length);
            ;
        }
        return rc;
    }

    /** ajust data buffer according to changes in byte buffer */
    public static void adjustReadableBuffers(ByteQueue b, ByteBuffer bb[]) {
        if (b.writePosition <= b.data.length) {
            b.skip(bb[0].position() - b.readPosition);
        } else {
            b.skip((bb[0].position() - b.readPosition) + bb[1].position());
        }
    }
}

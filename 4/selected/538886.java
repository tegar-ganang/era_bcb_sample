package com.tomgibara.crinch.bits;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * A {@link BitReader} that sources bits from a <code>FileChannel</code>. This
 * class operates with a byte buffer. This will generally improve performance in
 * applications that skip forwards or backwards across the file.
 * 
 * @author Tom Gibara
 */
public class FileChannelBitReader extends ByteBasedBitReader {

    private final FileChannel channel;

    private final ByteBuffer buffer;

    private long bufferPosition;

    /**
	 * Constructs a new BitReader over the specified FileChannel. Using a direct
	 * ByteBuffer should generally yield better performance.
	 * 
	 * @param channel
	 *            the file channel from which bits are to be read
	 * @param bufferSize
	 *            the size, in bytes, of the buffer used to store file data
	 * @param direct
	 *            whether the byte buffer should be allocated directly
	 */
    public FileChannelBitReader(FileChannel channel, int bufferSize, boolean direct) {
        if (channel == null) throw new IllegalArgumentException("null channel");
        if (bufferSize < 1) throw new IllegalArgumentException("non-positive buffer size");
        this.channel = channel;
        buffer = direct ? ByteBuffer.allocateDirect(bufferSize) : ByteBuffer.allocate(bufferSize);
        buffer.position(buffer.limit());
        bufferPosition = -1L;
    }

    @Override
    protected int readByte() throws BitStreamException {
        if (buffer.hasRemaining()) return buffer.get() & 0xff;
        buffer.limit(buffer.capacity()).position(0);
        try {
            bufferPosition = channel.position();
            channel.read(buffer);
        } catch (IOException e) {
            throw new BitStreamException(e);
        }
        buffer.flip();
        return buffer.hasRemaining() ? buffer.get() & 0xff : -1;
    }

    @Override
    protected long seekByte(long index) throws BitStreamException {
        if (bufferPosition >= 0) {
            long offset = index - bufferPosition;
            if (offset >= 0 && offset <= buffer.limit()) {
                buffer.position((int) offset);
                return index;
            }
        }
        return seekSlow(index);
    }

    @Override
    protected long skipBytes(long count) throws BitStreamException {
        if (count <= buffer.remaining()) {
            buffer.position(buffer.position() + (int) count);
            return count;
        }
        long position;
        if (bufferPosition >= 0) {
            position = bufferPosition + buffer.position();
        } else {
            try {
                position = channel.position();
            } catch (IOException e) {
                throw new BitStreamException(e);
            }
        }
        return seekSlow(position + count) - position;
    }

    /**
	 * The file channel underlying this BitReader
	 * 
	 * @return a FileChannel, never null
	 */
    public FileChannel getChannel() {
        return channel;
    }

    private long seekSlow(long index) throws BitStreamException {
        try {
            long length = channel.size();
            if (index >= length) index = length;
            channel.position(index);
            buffer.position(buffer.limit());
            bufferPosition = -1L;
            return index;
        } catch (IOException e) {
            throw new BitStreamException(e);
        }
    }
}

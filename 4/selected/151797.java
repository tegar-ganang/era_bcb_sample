package com.faunos.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.WritableByteChannel;
import com.faunos.util.ThrowFactory;
import com.faunos.util.Validator;

/**
 * A utility class for reading and writing to <code>
 * 
 */
public class ChannelUtil<E extends RuntimeException> {

    private static final ChannelUtil<RuntimeException> defaultInstance = new ChannelUtil<RuntimeException>(RuntimeException.class);

    public static final ChannelUtil<RuntimeException> defaultInstance() {
        return defaultInstance;
    }

    protected final ThrowFactory<RuntimeException, E> exceptionFactory;

    public ChannelUtil(Class<E> exceptionClass) {
        this(new ThrowFactory<RuntimeException, E>(exceptionClass));
    }

    public ChannelUtil(ThrowFactory<RuntimeException, E> exceptionFactory) {
        Validator.defaultInstance().notNull(exceptionFactory);
        this.exceptionFactory = exceptionFactory;
    }

    /**
     * Sets the file size.  This is a workaround for the fact that you can't
     * increase a file's size trivially through its <code>FileChannel</code>.
     */
    public void setSize(FileChannel channel, long size) throws IOException {
        final long currentSize = channel.size();
        if (currentSize > size) channel.truncate(size); else if (currentSize < size) {
            ByteBuffer zero = ByteBuffer.allocate(1);
            zero.put((byte) 0).flip();
            writeRemaining(channel, zero, size - 1);
        }
    }

    /**
     * Reads bytes from the given file channel into the specified
     * buffer.  The number of bytes read is equal to the number
     * of remaining bytes in the specified buffer: if EOF is reached
     * before filling the buffer, an app-specific exception is raised.
     * Bytes are read starting from the specified position.  The current
     * position of the file channel is <i>not modified.</i>
     * 
     * @see #readRemaining(ReadableByteChannel, ByteBuffer)
     * @see #readRemaining(ScatteringByteChannel, ByteBuffer[])
     */
    public void readRemaining(FileChannel channel, ByteBuffer buffer, long position) throws IOException {
        while (true) {
            final int amountRead = channel.read(buffer, position);
            if (amountRead == -1) throw exceptionFactory.create("Channel underflow");
            if (buffer.hasRemaining()) position += amountRead; else break;
        }
    }

    /**
     * Reads bytes from the given generic channel into the specified
     * buffer.  The number of bytes read is equal to the number
     * of remaining bytes in the specified buffer: if EOF is reached
     * before filling the buffer, an app-specific exception is raised.
     * Bytes are read starting from the current (possibly implicit)
     * position of the channel. On return, the current position of the
     * channel is incremented by the number of bytes read.
     * 
     * @see #readRemaining(ScatteringByteChannel, ByteBuffer[])
     * @see #readRemaining(FileChannel, ByteBuffer, long)
     */
    public void readRemaining(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            final int amountRead = channel.read(buffer);
            if (amountRead == -1) throw exceptionFactory.create("Channel underflow.\n" + "pos = " + buffer.position() + "\n" + "limit = " + buffer.limit() + "\n" + "channel = " + channel);
        }
    }

    /**
     * Reads bytes from the given scattering channel into the specified
     * buffer array.  The number of bytes read is equal to the sum of the
     * of remaining bytes in the given buffers: if EOF is reached
     * before filling the buffers, an app-specific exception is raised.
     * Bytes are read starting from the current (possibly implicit)
     * position of the channel. On return, the current position of the
     * channel is incremented by the number of bytes read.
     * 
     * @see #readRemaining(ReadableByteChannel, ByteBuffer)
     * @see #readRemaining(FileChannel, ByteBuffer, long)
     */
    public void readRemaining(ScatteringByteChannel channel, ByteBuffer[] buffers) throws IOException {
        int ir = BufferUtil.INSTANCE.indexOfHasRemaining(buffers);
        final int len = buffers.length;
        while (ir != -1) {
            final long amountRead = channel.read(buffers, ir, len - ir);
            if (amountRead == -1) throw exceptionFactory.create("Channel underflow");
            ir = BufferUtil.INSTANCE.indexOfHasRemaining(buffers, ir);
        }
    }

    /**
     * Writes the remaining bytes in the given buffer to the specified generic
     * channel.  The bytes are written starting at the (possibly implicit)
     * current position of the channel.  On return, the current position of
     * the channel is incremented by the number of bytes written.
     * 
     * @see #writeRemaining(FileChannel, ByteBuffer, long)
     */
    public void writeRemaining(WritableByteChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            final int amountWritten = channel.write(buffer);
            if (amountWritten == 0) Thread.yield();
        }
    }

    /**
     * Writes the remaining bytes in the given buffer to the specified
     * file channel starting at the specified position.  This method
     * does not modify the file channel's current position.
     * 
     * @see #writeRemaining(WritableByteChannel, ByteBuffer)
     */
    public void writeRemaining(FileChannel channel, ByteBuffer buffer, long position) throws IOException {
        while (buffer.hasRemaining()) {
            final int amountWritten = channel.write(buffer, position);
            if (amountWritten == 0) Thread.yield(); else position += amountWritten;
        }
    }

    /**
     * Transfers the specified number of bytes from the given
     * source <code>channel</code> into the given <code>sink</code>.
     * The arguments are checked for bounds before invocation.
     * 
     * @param channel
     *        the source channel
     * @param position
     *        the starting position from which bytes will be read
     * @param count
     *        the number of bytes to be transfered
     * @param sink
     *        the destination channel
     */
    public void transferTo(FileChannel channel, long position, long count, WritableByteChannel sink) throws IOException {
        if (position + count > channel.size()) throw exceptionFactory.create("position [" + position + "] + count [" + count + "] > channel.size() [" + channel.size() + "]");
        while (true) {
            final long amountRead = channel.transferTo(position, count, sink);
            if (amountRead == 0) Thread.yield();
            count -= amountRead;
            if (count > 0) position += amountRead; else if (count == 0) break; else throw exceptionFactory.create();
        }
    }

    /**
     * Transfers the specified number of bytes from the given
     * <code>source</code> into the given destination <code>channel</code>.
     * The arguments are checked for bounds before invocation.  If it turns
     * out the given <code>source</code> does not have enough remaining
     * bytes, then an instance-specific exception is raised.
     *  
     * @param channel
     *        the destination channel
     * @param position
     *        the starting position in the destination <code>channel</code>
     *        at which bytes will be written
     * @param count
     *        the number of bytes to transfer
     * @param source
     *        the source channel
     */
    public void transferFrom(FileChannel channel, long position, long count, ReadableByteChannel source) throws IOException {
        if (position > channel.size()) throw exceptionFactory.create("position [" + position + "] > channel.size() [" + channel.size() + "]");
        while (true) {
            final long amountWritten = channel.transferFrom(source, position, count);
            if (amountWritten == 0) Thread.yield();
            count -= amountWritten;
            if (count > 0) position += amountWritten; else if (count == 0) break; else throw exceptionFactory.create();
        }
    }
}

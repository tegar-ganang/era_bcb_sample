package net.sourceforge.tuned;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class ByteBufferOutputStream extends OutputStream {

    private ByteBuffer buffer;

    private final float loadFactor;

    public ByteBufferOutputStream(long initialCapacity) {
        this((int) initialCapacity);
    }

    public ByteBufferOutputStream(int initialCapacity) {
        this(initialCapacity, 1.0f);
    }

    public ByteBufferOutputStream(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity must not be negative");
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) throw new IllegalArgumentException("loadFactor must be greater than 0");
        this.buffer = ByteBuffer.allocate(initialCapacity);
        this.loadFactor = loadFactor;
    }

    @Override
    public void write(int b) throws IOException {
        ensureCapacity(buffer.position() + 1);
        buffer.put((byte) b);
    }

    @Override
    public void write(byte[] src) throws IOException {
        ensureCapacity(buffer.position() + src.length);
        buffer.put(src);
    }

    public void write(ByteBuffer src) throws IOException {
        ensureCapacity(buffer.position() + src.remaining());
        buffer.put(src);
    }

    @Override
    public void write(byte[] src, int offset, int length) throws IOException {
        ensureCapacity(buffer.position() + length);
        buffer.put(src, offset, length);
    }

    public void ensureCapacity(int minCapacity) {
        if (minCapacity <= buffer.capacity()) return;
        int newCapacity = (int) (buffer.capacity() * (1 + loadFactor));
        if (newCapacity < minCapacity) newCapacity = minCapacity;
        ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        buffer = newBuffer;
    }

    public ByteBuffer getByteBuffer() {
        ByteBuffer result = buffer.duplicate();
        result.flip();
        return result;
    }

    public byte[] getByteArray() {
        ByteBuffer data = getByteBuffer();
        byte[] bytes = new byte[data.remaining()];
        data.get(bytes);
        return bytes;
    }

    public int transferFrom(ReadableByteChannel channel) throws IOException {
        ensureCapacity(buffer.position() + 1);
        return channel.read(buffer);
    }

    public int transferFully(InputStream inputStream) throws IOException {
        return transferFully(Channels.newChannel(inputStream));
    }

    public int transferFully(ReadableByteChannel channel) throws IOException {
        int total = 0, read = 0;
        while ((read = transferFrom(channel)) >= 0) {
            total += read;
        }
        return total;
    }

    public int position() {
        return buffer.position();
    }

    public int capacity() {
        return buffer.capacity();
    }

    public void rewind() {
        buffer.rewind();
    }
}

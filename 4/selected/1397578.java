package de.ufinke.cubaja.io;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Flexible byte array buffer.
 * Functions similar to {@link java.io.RandomAccessFile};
 * an alternative to {@link java.io.ByteArrayOutputStream}
 * and {@link java.io.ByteArrayInputStream} 
 * when more flexibility is required. 
 * <p>
 * The buffer is backed by an automatically growing byte array.
 * The position is the current offset within the buffer
 * where the next write operation inserts data or the next
 * read operation reads data from. The position increments 
 * automatically when reading or writing.
 * The size is the maximum count of filled bytes within the buffer; 
 * it may be less than its capacity.
 * The size is set automatically during write operations
 * according to the resulting position.
 * Read operations never can read beyond the size. If such happens,
 * an <tt>EOFException</tt> is thrown.
 * @author Uwe Finke
 */
public class RandomAccessBuffer implements DataInput, DataOutput {

    private static class Output extends OutputStream {

        private RandomAccessBuffer master;

        public Output(RandomAccessBuffer master) {
            this.master = master;
        }

        public void write(int b) throws IOException {
            master.write(b);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            master.write(b, off, len);
        }
    }

    private static class Input extends InputStream {

        private RandomAccessBuffer master;

        public Input(RandomAccessBuffer master) {
            this.master = master;
        }

        public int read() throws IOException {
            return master.read();
        }

        public int read(byte[] b, int off, int len) throws IOException {
            return master.read(b, off, len);
        }
    }

    private static final int DEFAULT_CAPACITY = 4096;

    /**
   * Byte buffer.
   */
    protected byte[] buffer;

    private int position;

    private int capacity;

    private int growthCapacity;

    private int size;

    private Input inputStream;

    private Output outputStream;

    /**
   * Default constructor with default capacity.
   * Default initial and additional capacity are both <tt>4096</tt> bytes.
   */
    public RandomAccessBuffer() {
        capacity = DEFAULT_CAPACITY;
        growthCapacity = capacity;
        buffer = new byte[capacity];
    }

    /**
   * Constructor with specified capacity.
   * @param initialCapacity initial capacity
   * @param growthCapacity additional capacity when buffer must grow
   */
    public RandomAccessBuffer(int initialCapacity, int growthCapacity) {
        capacity = initialCapacity;
        this.growthCapacity = growthCapacity;
        buffer = new byte[capacity];
    }

    /**
   * Sets position and size to <tt>0</tt>.
   */
    public void reset() {
        position = 0;
        size = 0;
    }

    /**
   * Sets new position.
   * Capacity and size are adjusted if required.
   * If the new position exceeds the previous size,
   * the buffer may contain uninitialized bytes.
   * @param position
   */
    public void setPosition(int position) {
        this.position = position;
        if (position > size) {
            if (position > capacity) {
                grow(position + 1);
            }
            size = position;
        }
    }

    /**
   * Retrieves the current position.
   * @return current position
   */
    public int getPosition() {
        return position;
    }

    /**
   * Retrieves the current size.
   * The buffers size is determined by the highest reached position.
   * There are some methods which reset the size to zero.
   * @return size
   */
    public int size() {
        return size;
    }

    /**
   * Retrieves the current capacity.
   * The capacity is the amount of allocated, but not necessarily filled, bytes.
   * @return current capacity
   */
    public int capacity() {
        return capacity;
    }

    /**
   * Cuts the content and sets position and size.
   * It is expected to be <tt>0 <= from <= to <= size</tt>.
   * If <tt>from = 0</tt>, then size and position are both set to the <tt>to</tt> value.
   * If <tt>from > 0</tt>, then all bytes between from (inclusive) and to (exclusive) 
   * are copied to the buffers beginning; size and position are set to the amount of copied
   * bytes.
   * @param from
   * @param to
   */
    public void cut(int from, int to) {
        if (from < 0 || to < from || to > size) {
            throw new IllegalArgumentException("from=" + from + ", to=" + to + ", size=" + size + " (expected 0 <= from <= to <= size)");
        }
        if (from == 0) {
            size = to;
            position = to;
            return;
        }
        int len = to - from;
        int offset = from;
        for (int i = 0; i < len; i++) {
            buffer[i] = buffer[offset++];
        }
        size = len;
        position = len;
    }

    /**
   * Copies the buffers content up to the current size to a byte array.
   * @return byte array
   */
    public byte[] toByteArray() {
        byte[] copy = new byte[size];
        System.arraycopy(buffer, 0, copy, 0, size);
        return copy;
    }

    /**
   * Ensures buffer capacity and calculates new position and size values.
   * @param bytesToWrite
   * @return new position
   * @throws IOException
   */
    protected int newWritePosition(int bytesToWrite) throws IOException {
        int newPosition = position + bytesToWrite;
        if (newPosition > capacity) {
            grow(newPosition);
        }
        if (newPosition > size) {
            size = newPosition;
        }
        return newPosition;
    }

    private void grow(int minCapacity) {
        int newCapacity = Math.min(minCapacity, capacity + growthCapacity);
        byte[] newBuffer = new byte[newCapacity];
        System.arraycopy(buffer, 0, newBuffer, 0, size);
        capacity = newCapacity;
        buffer = newBuffer;
    }

    /**
   * Returns an <tt>OutputStream</tt> which writes into this buffer.
   * Needed for <tt>FilterOutputStream</tt>s.
   * @return output stream
   */
    public OutputStream getOutputStream() {
        if (outputStream == null) {
            outputStream = new Output(this);
        }
        return outputStream;
    }

    /**
   * Writes a single byte into the buffer.
   */
    public void write(int b) throws IOException {
        int pos = newWritePosition(1);
        buffer[position] = (byte) b;
        position = pos;
    }

    /**
   * Writes a byte array into the buffer.
   */
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
   * Writes a portion of a byte array into the buffer.
   */
    public void write(byte[] b, int off, int len) throws IOException {
        int pos = newWritePosition(len);
        System.arraycopy(b, off, buffer, position, len);
        position = pos;
    }

    /**
   * Writes a <tt>boolean</tt> into the buffer.
   */
    public void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    /**
   * Writes a <tt>byte</tt> into the buffer.
   */
    public void writeByte(int v) throws IOException {
        write(v);
    }

    /**
   * Writes a <tt>short</tt> into the buffer.
   */
    public void writeShort(int v) throws IOException {
        newWritePosition(2);
        int pos = position;
        buffer[pos++] = (byte) (v >>> 8);
        buffer[pos++] = (byte) v;
        position = pos;
    }

    /**
   * Writes a <tt>char</tt> into the buffer.
   */
    public void writeChar(int v) throws IOException {
        writeShort(v);
    }

    /**
   * Writes an <tt>int</tt> into the buffer.
   */
    public void writeInt(int v) throws IOException {
        newWritePosition(4);
        int pos = position;
        buffer[pos++] = (byte) (v >>> 24);
        buffer[pos++] = (byte) (v >>> 16);
        buffer[pos++] = (byte) (v >>> 8);
        buffer[pos++] = (byte) v;
        position = pos;
    }

    /**
   * Writes a <tt>long</tt> into the buffer.
   */
    public void writeLong(long v) throws IOException {
        newWritePosition(8);
        int pos = position;
        int n = (int) (v >>> 32);
        buffer[pos++] = (byte) (n >>> 24);
        buffer[pos++] = (byte) (n >>> 16);
        buffer[pos++] = (byte) (n >>> 8);
        buffer[pos++] = (byte) n;
        n = (int) v;
        buffer[pos++] = (byte) (n >>> 24);
        buffer[pos++] = (byte) (n >>> 16);
        buffer[pos++] = (byte) (n >>> 8);
        buffer[pos++] = (byte) n;
        position = pos;
    }

    /**
   * Writes a <tt>float</tt> into the buffer.
   */
    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    /**
   * Writes a <tt>double</tt> into the buffer.
   */
    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    /**
   * Writes a <tt>String</tt> as bytes into the buffer.
   */
    public void writeBytes(String s) throws IOException {
        int len = s.length();
        newWritePosition(len);
        int pos = position;
        for (int i = 0; i < len; i++) {
            buffer[pos++] = (byte) s.charAt(i);
        }
        position = pos;
    }

    /**
   * Writes a <tt>String</tt> as characters into the buffer.
   */
    public void writeChars(String s) throws IOException {
        int len = s.length();
        newWritePosition(len);
        int pos = position;
        for (int i = 0; i < len; i++) {
            int c = s.charAt(i);
            buffer[pos++] = (byte) (c >>> 8);
            buffer[pos++] = (byte) c;
        }
        position = pos;
    }

    /**
   * Writes an UTF string into the buffer.
   */
    public void writeUTF(String s) throws IOException {
        DataOutputStream dos = new DataOutputStream(getOutputStream());
        dos.writeUTF(s);
        dos.flush();
    }

    /**
   * Checks buffer size and calculates new position for read operations.
   * @param bytesToRead
   * @return new position
   * @throws IOException
   */
    protected int newReadPosition(int bytesToRead) throws IOException {
        int newPosition = position + bytesToRead;
        if (newPosition > size) {
            throw new EOFException();
        }
        return newPosition;
    }

    /**
   * Returns an <tt>InputStream</tt> which reads from this buffer.
   * Needed for <tt>FilterInputStream</tt>.
   * @return input stream
   */
    public InputStream getInputStream() {
        if (inputStream == null) {
            inputStream = new Input(this);
        }
        return inputStream;
    }

    /**
   * Reads one byte as <tt>InputStream</tt> would do.
   * @return one byte
   * @throws IOException
   */
    public int read() throws IOException {
        if (position == size) {
            return -1;
        }
        return buffer[position++] & 0xFF;
    }

    /**
   * Reads an array of bytes as <tt>InputStream</tt> would do.
   * Reads at most up to the current size; therefore the returned
   * value may be less than the requested length.
   * @param b byte array to fill
   * @param off start offset within byte array
   * @param len maximum number of bytes to transfer
   * @return number of bytes read, <tt>-1</tt> if the position had reached the current size
   * @throws IOException
   */
    public int read(byte[] b, int off, int len) throws IOException {
        if (position == size) {
            return -1;
        }
        int copySize = Math.min(len, size - position);
        System.arraycopy(buffer, position, b, off, copySize);
        position = position + copySize;
        return copySize;
    }

    /**
   * Reads a byte array from the buffer.
   */
    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    /**
   * Reads a portion of a byte array from the buffer.
   */
    public void readFully(byte[] b, int off, int len) throws IOException {
        int pos = newReadPosition(len);
        System.arraycopy(buffer, position, b, off, len);
        position = pos;
    }

    /**
   * Advances the buffers position.
   */
    public int skipBytes(int n) throws IOException {
        int skipped = Math.min(n, size - position);
        position += skipped;
        return skipped;
    }

    /**
   * Reads a <tt>boolean</tt> from the buffer.
   */
    public boolean readBoolean() throws IOException {
        return readByte() != 0;
    }

    /**
   * Reads a <tt>byte</tt> from the buffer.
   */
    public byte readByte() throws IOException {
        int pos = newReadPosition(1);
        byte result = buffer[position];
        position = pos;
        return result;
    }

    /**
   * Reads an unsigned byte from the buffer.
   */
    public int readUnsignedByte() throws IOException {
        int pos = newReadPosition(1);
        int result = buffer[position] & 0xFF;
        position = pos;
        return result;
    }

    /**
   * Reads a <tt>short</tt> from the buffer.
   */
    public short readShort() throws IOException {
        newReadPosition(2);
        int pos = position;
        int result = buffer[pos++] << 8 | (buffer[pos++] & 0xFF);
        position = pos;
        return (short) result;
    }

    /**
   * Reads an unsigned short from the buffer.
   */
    public int readUnsignedShort() throws IOException {
        newReadPosition(2);
        int pos = position;
        int result = (buffer[pos++] & 0xFF) << 8 | (buffer[pos++] & 0xFF);
        position = pos;
        return result;
    }

    /**
   * Reads a <tt>char</tt> from the buffer.
   */
    public char readChar() throws IOException {
        return (char) readUnsignedShort();
    }

    /**
   * Reads an <tt>int</tt> from the buffer.
   */
    public int readInt() throws IOException {
        newReadPosition(4);
        int pos = position;
        int result = buffer[pos++] << 24 | (buffer[pos++] & 0xFF) << 16 | (buffer[pos++] & 0xFF) << 8 | (buffer[pos++] & 0xFF);
        position = pos;
        return result;
    }

    /**
   * Reads a <tt>long</tt> from the buffer.
   */
    public long readLong() throws IOException {
        newReadPosition(8);
        int pos = position;
        int high = buffer[pos++] << 24 | (buffer[pos++] & 0xFF) << 16 | (buffer[pos++] & 0xFF) << 8 | (buffer[pos++] & 0xFF);
        int low = buffer[pos++] << 24 | (buffer[pos++] & 0xFF) << 16 | (buffer[pos++] & 0xFF) << 8 | (buffer[pos++] & 0xFF);
        position = pos;
        return (((long) high) << 32) | (low & 0xFFFFFFFFL);
    }

    /**
   * Reads a <tt>float</tt> from the buffer.
   */
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    /**
   * Reads a <tt>double</tt> from the buffer.
   */
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    /**
   * Reads a line from the buffer.
   */
    public String readLine() throws IOException {
        BufferedReader d = new BufferedReader(new InputStreamReader(getInputStream()));
        return d.readLine();
    }

    /**
   * Reads an UTF string from the buffer.
   */
    public String readUTF() throws IOException {
        DataInputStream d = new DataInputStream(getInputStream());
        return d.readUTF();
    }

    /**
   * Copies the content up to the current size to a stream and resets this buffer.
   * @param out
   * @throws IOException
   */
    public void drainTo(OutputStream out) throws IOException {
        out.write(buffer, 0, size);
        reset();
    }

    /**
   * Copies the content up to the current size to a data output and resets this buffer.
   * @param out
   * @throws IOException
   */
    public void drainTo(DataOutput out) throws IOException {
        out.write(buffer, 0, size);
        reset();
    }

    /**
   * Copies the content starting at current position to an <tt>OutputStream</tt> without resetting this buffer.
   * @param out
   * @param len
   * @throws IOException
   */
    public void transferTo(OutputStream out, int len) throws IOException {
        int pos = newReadPosition(len);
        out.write(buffer, position, len);
        position = pos;
    }

    /**
   * Copies the content starting at current position to a <tt>DataOutput</tt> without resetting this buffer.
   * @param out
   * @param len
   * @throws IOException
   */
    public void transferFullyTo(DataOutput out, int len) throws IOException {
        int pos = newReadPosition(len);
        out.write(buffer, position, len);
        position = pos;
    }

    /**
   * Reads up to <tt>len</tt> bytes from an <tt>InputStream</tt> into this buffer.
   * The starting position within this buffer is its current position.
   * The stream's <tt>read(byte[] b, int off, int len)</tt> method
   * is called repeatedly until all bytes requested with the 
   * <tt>len</tt> parameter are read or <tt>-1</tt> is returned from the stream.
   * @param in
   * @param len
   * @return number of bytes effectively been read
   * @throws IOException
   */
    public int transferFrom(InputStream in, int len) throws IOException {
        int minCapacity = position + len;
        if (minCapacity > capacity) {
            grow(minCapacity);
        }
        int totalBytesTransferred = 0;
        while (len > 0) {
            int bytesTransferred = in.read(buffer, position, len);
            if (bytesTransferred == -1) {
                len = 0;
            } else {
                totalBytesTransferred += bytesTransferred;
                position += bytesTransferred;
                len -= bytesTransferred;
            }
        }
        if (position > size) {
            size = position;
        }
        return totalBytesTransferred;
    }

    /**
   * Reads exactly <tt>len</tt> bytes from a <tt>DataInput</tt> into this buffer.
   * @param in
   * @param len
   * @throws IOException
   */
    public void transferFullyFrom(DataInput in, int len) throws IOException {
        int pos = newWritePosition(len);
        in.readFully(buffer, position, len);
        position = pos;
    }
}

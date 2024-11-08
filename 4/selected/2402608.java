package net.sf.jgamelibrary.util;

import java.util.Arrays;

/**
 * Alternative to the {@code ByteBuffer} class in java.nio.
 * Note that the provided methods perform no error/bounds checking.
 * @author Vlad Firoiu
 */
public class ByteBuffer {

    public static void main(String[] args) {
        ByteBuffer buf = new ByteBuffer(10);
        buf.putString("");
        buf.putString("Hello.");
        buf.putString("");
        System.out.println(buf.getString());
        System.out.println(buf.getString());
        System.out.println(buf.getString());
        System.out.println(buf.remaining());
    }

    protected byte[] buf;

    protected int reader = 0, writer = 0;

    public ByteBuffer(int capacity) {
        buf = new byte[capacity];
    }

    public ByteBuffer(ByteBuffer other) {
        this.buf = other.buf;
        this.reader = other.reader;
        this.writer = other.writer;
    }

    /**
	 * @return The size of this buffer.
	 */
    public int size() {
        return buf.length;
    }

    /**
	 * @return The starting index.
	 */
    public int getReader() {
        return reader;
    }

    /**
	 * Sets the reader position.
	 * @param position The desired position.
	 * @return Thus {@code ByteBuffer}.
	 */
    public ByteBuffer setReader(int position) {
        reader = position;
        return this;
    }

    /**
	 * Advances the reader.
	 * @param amount The amount by which to advance.
	 * @return This {@code ByteBuffer}.
	 */
    public ByteBuffer advanceReader(int amount) {
        reader += amount;
        return this;
    }

    /**
	 * @return The ending index.
	 */
    public int getWriter() {
        return writer;
    }

    /**
	 * Sets the writer position.
	 * @param position The desired position.
	 * @return This {@code ByteBuffer}.
	 */
    public ByteBuffer setWriter(int position) {
        writer = position;
        return this;
    }

    /**
	 * @return The size of the data in this buffer.
	 */
    public int length() {
        return writer - reader;
    }

    /**
	 * @return The number of bytes that can be written.
	 */
    public int remaining() {
        return buf.length - writer;
    }

    /**
	 * Clears this buffer.
	 */
    public void clear() {
        reader = writer = 0;
    }

    /**
	 * Compacts this buffer.
	 */
    public void compact() {
        System.arraycopy(buf, reader, buf, 0, writer -= reader);
        reader = 0;
    }

    /**
	 * @return The backing array of this buffer.
	 */
    public byte[] getArray() {
        return buf;
    }

    /**
	 * Puts a byte into this buffer at the writer position.
	 * @param b The byte.
	 * @return This {@code ByteBuffer}
	 */
    public ByteBuffer putByte(byte b) {
        buf[writer++] = b;
        return this;
    }

    /**
	 * Puts a byte into this buffer at the desired position.
	 * @param index The absolute position.
	 * @param b The byte.
	 * @return This {@code ByteBuffer}
	 */
    public ByteBuffer putByte(int index, byte b) {
        buf[index] = b;
        return this;
    }

    /**
	 * Gets a byte from this buffer at the reader position.
	 * The reader position is incremented.
	 * @return The byte.
	 */
    public byte getByte() {
        return buf[reader++];
    }

    /**
	 * Gets an unsigned byte from this buffer at the reader position.
	 * @return The unsigned byte.
	 */
    public short getUnsignedByte() {
        return Bits.getUnsignedByte(getByte());
    }

    public byte peekByte() {
        return buf[reader];
    }

    public short peekUnsignedByte() {
        return Bits.getUnsignedByte(peekByte());
    }

    public byte getByte(int index) {
        return buf[index];
    }

    public byte peekByte(int index) {
        return buf[reader + index];
    }

    public ByteBuffer putChar(char c) {
        buf[writer++] = Bits.char1(c);
        buf[writer++] = Bits.char0(c);
        return this;
    }

    public ByteBuffer putChar(int index, char c) {
        Bits.putChar(c, buf, index);
        return this;
    }

    public char getChar() {
        return Bits.makeChar(buf[reader++], buf[reader++]);
    }

    public char peekChar() {
        return Bits.getChar(buf, reader);
    }

    public char getChar(int index) {
        return Bits.getChar(buf, index);
    }

    public char peekChar(int index) {
        return Bits.getChar(buf, reader + index);
    }

    public ByteBuffer putShort(short s) {
        buf[writer++] = Bits.short1(s);
        buf[writer++] = Bits.short0(s);
        return this;
    }

    public ByteBuffer putShort(int index, short s) {
        Bits.putShort(s, buf, index);
        return this;
    }

    public short getShort() {
        return Bits.makeShort(buf[reader++], buf[reader++]);
    }

    public int getUnsignedShort() {
        return Bits.getUnsignedShort(getShort());
    }

    public short peekShort() {
        return Bits.getShort(buf, reader);
    }

    public int peekUnsignedShort() {
        return Bits.getUnsignedShort(peekShort());
    }

    public short getShort(int index) {
        return Bits.getShort(buf, index);
    }

    public short peekShort(int index) {
        return Bits.getShort(buf, reader + index);
    }

    public ByteBuffer putInt(int i) {
        buf[writer++] = Bits.int3(i);
        buf[writer++] = Bits.int2(i);
        buf[writer++] = Bits.int1(i);
        buf[writer++] = Bits.int0(i);
        return this;
    }

    public ByteBuffer putInt(int index, int i) {
        Bits.putInt(i, buf, index);
        return this;
    }

    public int getInt() {
        return Bits.makeInt(buf[reader++], buf[reader++], buf[reader++], buf[reader++]);
    }

    public long getUnsignedInt() {
        return Bits.getUnsignedInt(getInt());
    }

    public int peekInt() {
        return Bits.getInt(buf, reader);
    }

    public long peekUnsignedInt() {
        return Bits.getUnsignedInt(peekInt());
    }

    public int getInt(int index) {
        return Bits.getInt(buf, index);
    }

    public int peekInt(int index) {
        return Bits.getInt(buf, reader + index);
    }

    public ByteBuffer putLong(long l) {
        buf[writer++] = Bits.long7(l);
        buf[writer++] = Bits.long6(l);
        buf[writer++] = Bits.long5(l);
        buf[writer++] = Bits.long4(l);
        buf[writer++] = Bits.long3(l);
        buf[writer++] = Bits.long2(l);
        buf[writer++] = Bits.long1(l);
        buf[writer++] = Bits.long0(l);
        return this;
    }

    public long getLong() {
        return Bits.makeLong(buf[reader++], buf[reader++], buf[reader++], buf[reader++], buf[reader++], buf[reader++], buf[reader++], buf[reader++]);
    }

    public ByteBuffer putLong(int index, long l) {
        Bits.putLong(l, buf, index);
        return this;
    }

    public long peekLong() {
        return Bits.getLong(buf, reader);
    }

    public long getLong(int index) {
        return Bits.getLong(buf, index);
    }

    public long peekLong(int index) {
        return Bits.getLong(buf, reader + index);
    }

    public ByteBuffer putFloat(float f) {
        putInt(Float.floatToRawIntBits(f));
        return this;
    }

    public ByteBuffer putFloat(int index, float f) {
        Bits.putFloat(f, buf, index);
        return this;
    }

    public float getFloat() {
        return Bits.makeFloat(buf[reader++], buf[reader++], buf[reader++], buf[reader++]);
    }

    public float peekFloat() {
        return Bits.getFloat(buf, reader);
    }

    public float getFloat(int index) {
        return Bits.getFloat(buf, index);
    }

    public float peekFloat(int index) {
        return Bits.getFloat(buf, reader + index);
    }

    public ByteBuffer putDouble(double d) {
        putLong(Double.doubleToRawLongBits(d));
        return this;
    }

    public ByteBuffer putDouble(int index, double d) {
        Bits.putDouble(d, buf, index);
        return this;
    }

    public double getDouble() {
        return Bits.makeDouble(buf[reader++], buf[reader++], buf[reader++], buf[reader++], buf[reader++], buf[reader++], buf[reader++], buf[reader++]);
    }

    public double peekDouble() {
        return Bits.getDouble(buf, reader);
    }

    public double getDouble(int index) {
        return Bits.getDouble(buf, index);
    }

    public double peekDouble(int index) {
        return Bits.getDouble(buf, reader + index);
    }

    public ByteBuffer putBytes(byte[] src, int off, int len) {
        System.arraycopy(src, off, buf, writer, len);
        writer += len;
        return this;
    }

    public ByteBuffer putBytes(byte[] src) {
        return putBytes(src, 0, src.length);
    }

    public ByteBuffer putBytes(ByteBuffer other) {
        return putBytes(other.buf, other.reader, other.length());
    }

    public ByteBuffer putBytes(ByteBuffer other, int len) {
        return putBytes(other.buf, other.reader, len);
    }

    public byte[] getBytes(byte[] dest, int off, int len) {
        System.arraycopy(buf, reader, dest, off, len);
        reader += len;
        return dest;
    }

    public static final byte NULL_CHARACTER = 0;

    /**
	 * Puts a {@code String} into this buffer using the ASCII encoding.
	 * A null character is appended if necessary.
	 * @param s The desired string.
	 * @return This {@code ByteBuffer}.
	 */
    public ByteBuffer putString(String s) {
        for (int i = 0; i < s.length(); i++) {
            putByte((byte) s.charAt(i));
        }
        if (s.isEmpty() || s.charAt(s.length() - 1) != NULL_CHARACTER) {
            putByte(NULL_CHARACTER);
        }
        return this;
    }

    /**
	 * Reads a {@code String} from this buffer using ASCII encoding.
	 * @return The {@code String} read, or null if none found.
	 * The null character is not included in the string read.
	 */
    public String getString() {
        for (int i = reader; i < writer; i++) {
            if (buf[i] == NULL_CHARACTER) {
                char[] temp = new char[i - reader];
                for (int j = reader; j < i; j++) {
                    temp[j - reader] = (char) buf[j];
                }
                reader = i + 1;
                return new String(temp);
            }
        }
        return null;
    }

    public ByteBuffer duplicate() {
        return new ByteBuffer(this);
    }

    @Override
    public String toString() {
        return Arrays.toString(Arrays.copyOfRange(buf, reader, writer));
    }
}

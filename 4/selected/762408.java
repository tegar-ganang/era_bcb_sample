package org.artsProject.mcop;

import java.io.*;
import java.util.*;
import gnu.java.io.decode.*;
import gnu.java.io.encode.*;

/** a variable-size rotating byte buffer */
public class Buffer {

    protected static final int MINSIZE = 32;

    private static final byte[] EMPTYARRAY = new byte[0];

    protected static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private static final DecoderUTF8 stringDecoder = new DecoderUTF8(null);

    private static final EncoderUTF8 stringEncoder = new EncoderUTF8(null);

    protected byte[] data = EMPTYARRAY;

    protected int readPos, writePos;

    protected Vector<BufferListener> listeners = null;

    public Buffer() {
    }

    public Buffer(String hex) {
        fromString(hex);
    }

    public Buffer(String hex, String name) {
        if (!fromString(hex, name)) throw new MCOPException(hex + " doesn't start with '" + name + ":'");
    }

    public void fromString(String hex) {
        willWrite(hex.length() / 2);
        for (int i = 0; i < hex.length(); i += 2) {
            writeByte((byte) ((fromHexNibble(hex.charAt(i)) << 4) | (fromHexNibble(hex.charAt(i + 1)))));
        }
    }

    public Buffer(File file) throws IOException {
        writePos = (int) file.length();
        DataInputStream in = new DataInputStream(new FileInputStream(file));
        data = new byte[writePos];
        in.readFully(data);
        in.close();
    }

    public Buffer(byte[] data) {
        writeBytes(data);
    }

    public Buffer(byte[] data, int offset, int length) {
        writeBytes(data, offset, length);
    }

    public Buffer(Buffer buffer) {
        writeBytes(buffer.data, buffer.readPos, buffer.remaining());
    }

    public Buffer(Buffer buffer, int offset, int length) {
        writeBytes(buffer.data, buffer.readPos + offset, length);
    }

    protected static int fromHexNibble(char c) {
        if (c >= '0' && c <= '9') return (int) (c - '0'); else if (c >= 'a' && c <= 'f') return (int) (c - 'a') + 10; else if (c >= 'A' && c <= 'F') return (int) (c - 'A') + 10; else throw new IllegalArgumentException("No hex char: " + c);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(remaining() * 2);
        for (int i = readPos; i < writePos; i++) {
            buf.append(HEX[(data[i] >> 4) & 0x0f]);
            buf.append(HEX[data[i] & 0x0f]);
        }
        return buf.toString();
    }

    public String toString(String name) {
        return name + ":" + toString();
    }

    public void writeByte(byte b) {
        willWrite(1);
        data[writePos] = b;
        wrote(1);
    }

    public byte readByte() throws EndOfBufferException {
        willRead(1);
        return data[readPos++];
    }

    public void willWrite(int bytes) {
        int space = space();
        if (space < bytes) enlarge(bytes);
    }

    protected void willRead(int bytes) throws EndOfBufferException {
        if (bytes > remaining()) throw new EndOfBufferException("Tried to read " + bytes + " bytes, but only " + remaining() + " are available");
    }

    protected int space() {
        return data.length - writePos;
    }

    public int remaining() {
        return writePos - readPos;
    }

    protected void enlarge(int minimum) {
        int newSize = Math.max(data.length, MINSIZE);
        while (newSize < remaining() + minimum) newSize <<= 1;
        byte[] newData = new byte[newSize];
        System.arraycopy(data, readPos, newData, 0, writePos - readPos);
        writePos -= readPos;
        readPos = 0;
        data = newData;
    }

    public String readString() {
        int l = readLong();
        if (l == 0) {
            return null;
        } else {
            willRead(l);
            String s = bytesToString(data, readPos, l - 1);
            readPos += l;
            return s;
        }
    }

    public void writeString(String s) {
        if (s == null) {
            writeLong(0);
        } else {
            byte[] bytes = stringToBytes(s);
            writeLong(bytes.length + 1);
            writeBytes(bytes);
            writeByte((byte) 0);
        }
    }

    public byte[] readBytes(int num) {
        willRead(num);
        byte[] result = new byte[num];
        System.arraycopy(data, readPos, result, 0, num);
        readPos += num;
        return result;
    }

    /** Attention: an MCOP "long" is a Java int! */
    public int readLong() {
        int l = peekLong(0);
        readPos += 4;
        return l;
    }

    protected void putLong(int pos, int l) {
        data[pos] = (byte) (l >> 24);
        data[pos + 1] = (byte) (l >> 16);
        data[pos + 2] = (byte) (l >> 8);
        data[pos + 3] = (byte) (l);
    }

    protected void wrote(int num) {
        writePos += num;
        fireBytesWritten();
    }

    public void writeLong(int l) {
        willWrite(4);
        putLong(writePos, l);
        wrote(4);
    }

    public void patchLength() {
        putLong(4, remaining());
    }

    public void writeBytes(byte[] b) {
        willWrite(b.length);
        System.arraycopy(b, 0, data, writePos, b.length);
        wrote(b.length);
    }

    public void writeBytes(byte[] b, int start, int len) {
        willWrite(len);
        System.arraycopy(b, start, data, writePos, len);
        wrote(len);
    }

    public void writeBuffer(Buffer b) {
        int l = b.writePos - b.readPos;
        willWrite(l);
        System.arraycopy(b.data, b.readPos, data, writePos, l);
        wrote(l);
    }

    public byte[] toByteArray() {
        byte[] result = new byte[remaining()];
        System.arraycopy(data, readPos, result, 0, remaining());
        return result;
    }

    public void addBufferListener(BufferListener l) {
        if (listeners == null) listeners = new Vector<BufferListener>();
        listeners.add(l);
    }

    protected void fireBytesWritten() {
        if (listeners != null) for (Enumeration<BufferListener> e = listeners.elements(); e.hasMoreElements(); ) e.nextElement().bytesWritten(this);
    }

    public int peekLong(int ofs) {
        willRead(ofs + 4);
        return (((int) data[readPos + ofs] & 0xff) << 24) | (((int) data[readPos + ofs + 1] & 0xff) << 16) | (((int) data[readPos + ofs + 2] & 0xff) << 8) | (((int) data[readPos + ofs + 3] & 0xff));
    }

    public boolean fromString(String s, String name) {
        if (s.startsWith(name + ":")) {
            fromString(s.substring(name.length() + 1));
            return true;
        } else return false;
    }

    public void writeBoolean(boolean b) {
        willWrite(1);
        data[writePos] = (byte) (b ? 1 : 0);
        wrote(1);
    }

    public boolean readBoolean() {
        willRead(1);
        return data[readPos++] != 0;
    }

    public void writeFloat(float f) {
        writeLong(Float.floatToIntBits(f));
    }

    public float readFloat() {
        return Float.intBitsToFloat(readLong());
    }

    public long readLongLong() {
        long l = peekLongLong(0);
        readPos += 8;
        return l;
    }

    public void writeLongLong(long l) {
        willWrite(8);
        data[writePos] = (byte) (l >> 56);
        data[writePos + 1] = (byte) (l >> 48);
        data[writePos + 2] = (byte) (l >> 40);
        data[writePos + 3] = (byte) (l >> 32);
        data[writePos + 4] = (byte) (l >> 24);
        data[writePos + 5] = (byte) (l >> 16);
        data[writePos + 6] = (byte) (l >> 8);
        data[writePos + 7] = (byte) (l);
        wrote(8);
    }

    public long peekLongLong(int ofs) {
        willRead(ofs + 8);
        return (((long) data[readPos + ofs] & 0xff) << 56) | (((long) data[readPos + ofs + 1] & 0xff) << 48) | (((long) data[readPos + ofs + 2] & 0xff) << 40) | (((long) data[readPos + ofs + 3] & 0xff) << 32) | (((long) data[readPos + ofs + 4] & 0xff) << 24) | (((long) data[readPos + ofs + 5] & 0xff) << 16) | (((long) data[readPos + ofs + 6] & 0xff) << 8) | (((long) data[readPos + ofs + 7] & 0xff));
    }

    public boolean contentEquals(Buffer b) {
        if (remaining() != b.remaining()) return false;
        for (int i = readPos, j = b.readPos; i < writePos; i++, j++) if (data[i] != b.data[j]) return false;
        return true;
    }

    /** bytesToString and stringToBytes provide a reversable way of String<->byte[] conversion
   */
    public static String bytesToString(byte[] bytes) {
        return bytesToString(bytes, 0, bytes.length);
    }

    /** bytesToString and stringToBytes provide a reversable way of String<->byte[] conversion
   */
    public static String bytesToString(byte[] bytes, int start, int len) {
        try {
            return new String(stringDecoder.convertToChars(bytes, start, start + len));
        } catch (CharConversionException e) {
            try {
                return new String(bytes, start, len, "ISO8859_1");
            } catch (UnsupportedEncodingException e2) {
                throw new RuntimeException(e2.toString());
            }
        }
    }

    /** bytesToString and stringToBytes provide a reversable way of String<->byte[] conversion
   */
    public static byte[] stringToBytes(String s) {
        try {
            return stringEncoder.convertToBytes(s.toCharArray());
        } catch (CharConversionException e) {
            throw new RuntimeException(e.toString());
        }
    }
}

package com.peterhi.io;

import java.awt.Color;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author Administrator
 */
public class IO {

    public static final String DEFAULT_CHARSET = "UTF-8";

    public static void copyFile(InputStream in, File out) throws IOException {
        FileOutputStream fos = new FileOutputStream(out);
        try {
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = in.read(buf)) != -1) fos.write(buf, 0, i);
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (in != null) in.close();
            if (fos != null) fos.close();
        }
    }

    private static boolean isEmpty(String s) {
        return (s == null || s.length() <= 0);
    }

    public static byte[] getBytes(char[] chars, String charset) {
        if (isEmpty(charset)) {
            charset = DEFAULT_CHARSET;
        }
        Charset cs = Charset.forName(charset);
        CharBuffer cb = CharBuffer.allocate(chars.length);
        cb.put(chars);
        cb.flip();
        ByteBuffer bb = cs.encode(cb);
        return bb.array();
    }

    public static char[] getChars(byte[] bytes, String charset) {
        if (isEmpty(charset)) {
            charset = DEFAULT_CHARSET;
        }
        Charset cs = Charset.forName(charset);
        ByteBuffer bb = ByteBuffer.allocate(bytes.length);
        bb.put(bytes);
        bb.flip();
        CharBuffer cb = cs.decode(bb);
        return cb.array();
    }

    public static void writeByteArrays(DataOutput out, List<byte[]> arr) throws IOException {
        int len = (arr == null) ? 0 : arr.size();
        out.writeInt(len);
        for (int i = 0; i < len; i++) {
            byte[] cur = arr.get(i);
            int sublen = (cur == null) ? 0 : cur.length;
            out.writeInt(sublen);
            out.write(cur);
        }
    }

    public static List<byte[]> readByteArrays(DataInput in) throws IOException {
        List<byte[]> ret = new Vector<byte[]>();
        int len = in.readInt();
        if (len <= 0) {
            return null;
        }
        for (int i = 0; i < len; i++) {
            int sublen = in.readInt();
            if (sublen <= 0) {
                ret.add(new byte[0]);
                continue;
            } else {
                byte[] cur = new byte[sublen];
                read_from(in, cur);
                ret.add(cur);
            }
        }
        return ret;
    }

    public static void writeStrings(DataOutput out, List<String> arr) throws IOException {
        int len = (arr == null) ? 0 : arr.size();
        out.writeInt(len);
        for (int i = 0; i < len; i++) {
            writeString(out, arr.get(i));
        }
    }

    public static void writeStringArray(DataOutput out, String[] arr) throws IOException {
        int len = (arr == null) ? 0 : arr.length;
        out.writeInt(len);
        for (int i = 0; i < len; i++) {
            writeString(out, arr[i]);
        }
    }

    public static List<String> readStrings(DataInput in) throws IOException {
        int len = in.readInt();
        if (len <= 0) {
            return null;
        }
        List<String> ret = new Vector<String>();
        for (int i = 0; i < len; i++) {
            ret.add(readString(in));
        }
        return ret;
    }

    public static String[] readStringArray(DataInput in) throws IOException {
        int len = in.readInt();
        if (len <= 0) {
            return null;
        }
        String[] ret = new String[len];
        for (int i = 0; i < len; i++) {
            ret[i] = readString(in);
        }
        return ret;
    }

    public static void writeIntArray(DataOutput out, int[] arr) throws IOException {
        int len = (arr == null) ? 0 : arr.length;
        out.writeInt(len);
        for (int i = 0; i < len; i++) {
            out.writeInt(arr[i]);
        }
    }

    public static void writeLongArray(DataOutput out, long[] arr) throws IOException {
        int len = (arr == null) ? 0 : arr.length;
        out.writeInt(len);
        for (int i = 0; i < len; i++) {
            out.writeLong(arr[i]);
        }
    }

    public static int[] readIntArray(DataInput in) throws IOException {
        int len = in.readInt();
        if (len <= 0) {
            return null;
        }
        int[] ret = new int[len];
        for (int i = 0; i < len; i++) {
            ret[i] = in.readInt();
        }
        return ret;
    }

    public static long[] readLongArray(DataInput in) throws IOException {
        int len = in.readInt();
        if (len <= 0) {
            return null;
        }
        long[] ret = new long[len];
        for (int i = 0; i < len; i++) {
            ret[i] = in.readLong();
        }
        return ret;
    }

    public static void writeShortArray(DataOutput out, short[] arr) throws IOException {
        int len = (arr == null) ? 0 : arr.length;
        out.writeInt(len);
        for (int i = 0; i < len; i++) {
            out.writeShort(arr[i]);
        }
    }

    public static short[] readShortArray(DataInput in) throws IOException {
        int len = in.readInt();
        if (len <= 0) {
            return null;
        }
        short[] ret = new short[len];
        for (int i = 0; i < len; i++) {
            ret[i] = in.readShort();
        }
        return ret;
    }

    public static boolean[] readBooleanArray(DataInput in) throws IOException {
        int len = in.readInt();
        if (len <= 0) {
            return null;
        }
        boolean[] ret = new boolean[len];
        for (int i = 0; i < len; i++) {
            ret[i] = in.readBoolean();
        }
        return ret;
    }

    public static void writeByteArray(DataOutput out, byte[] arr) throws IOException {
        int len = (arr == null) ? 0 : arr.length;
        out.writeInt(len);
        if (len > 0) {
            out.write(arr);
        }
    }

    public static void writeByteArray(DataOutput out, byte[] arr, int alength) throws IOException {
        int len = (arr == null) ? 0 : arr.length;
        out.writeInt(alength);
        if (len > 0) {
            out.write(arr, 0, alength);
        }
    }

    public static byte[] readByteArray(DataInput in) throws IOException {
        int len = in.readInt();
        if (len <= 0) {
            return null;
        }
        byte[] ret = new byte[len];
        read_from(in, ret);
        return ret;
    }

    public static void writeBooleanArray(DataOutput out, boolean[] arr) throws IOException {
        int len = (arr == null) ? 0 : arr.length;
        out.writeInt(len);
        for (int i = 0; i < len; i++) {
            out.writeBoolean(arr[i]);
        }
    }

    public static void writeFloatArray(DataOutput out, float[] arr) throws IOException {
        int len = (arr == null) ? 0 : arr.length;
        out.writeInt(len);
        for (int i = 0; i < len; i++) {
            out.writeFloat(arr[i]);
        }
    }

    public static void writeDoubleArray(DataOutput out, double[] arr) throws IOException {
        int len = (arr == null) ? 0 : arr.length;
        out.writeInt(len);
        for (int i = 0; i < len; i++) {
            out.writeDouble(arr[i]);
        }
    }

    public static float[] readFloatArray(DataInput in) throws IOException {
        int len = in.readInt();
        if (len <= 0) {
            return null;
        }
        float[] ret = new float[len];
        for (int i = 0; i < len; i++) {
            ret[i] = in.readFloat();
        }
        return ret;
    }

    public static double[] readDoubleArray(DataInput in) throws IOException {
        int len = in.readInt();
        if (len <= 0) {
            return null;
        }
        double[] ret = new double[len];
        for (int i = 0; i < len; i++) {
            ret[i] = in.readDouble();
        }
        return ret;
    }

    public static void writeCharArray(DataOutput out, char[] arr) throws IOException {
        int len = (arr == null) ? 0 : arr.length;
        out.writeInt(len);
        for (int i = 0; i < len; i++) {
            out.writeChar(arr[i]);
        }
    }

    public static char[] readCharArray(DataInput in) throws IOException {
        int len = in.readInt();
        if (len <= 0) {
            return null;
        }
        char[] ret = new char[len];
        for (int i = 0; i < len; i++) {
            ret[i] = in.readChar();
        }
        return ret;
    }

    public static void writeColor(DataOutput out, Color cr) throws IOException {
        if (cr == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeShort((short) cr.getRed());
            out.writeShort((short) cr.getGreen());
            out.writeShort((short) cr.getBlue());
            out.writeShort((short) cr.getAlpha());
        }
    }

    public static Color readColor(DataInput in) throws IOException {
        boolean hasColor = in.readBoolean();
        Color ret = null;
        if (hasColor) {
            ret = new Color(in.readShort(), in.readShort(), in.readShort(), in.readShort());
        }
        return ret;
    }

    public static void writeString(DataOutput out, String s) throws IOException {
        if (isEmpty(s)) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeUTF(s);
        }
    }

    public static String readString(DataInput in) throws IOException {
        boolean hasString = in.readBoolean();
        if (hasString) {
            return in.readUTF();
        } else {
            return null;
        }
    }

    public static int getInt(byte[] buf, int offset) {
        return getInt(buf, 4, offset);
    }

    public static short getShort(byte[] buf, int offset) {
        return (short) getInt(buf, 2, offset);
    }

    public static int getInt(byte[] buf, int length, int offset) {
        int result = 0;
        for (int i = 0; i < length; i++) {
            int cur = (buf[offset + i] < 0 ? (int) buf[offset + i] + 256 : (int) buf[offset + i]) << (8 * i);
            result += cur;
        }
        return result;
    }

    public static void putInt(byte[] buf, int offset, int v) {
        putInt(buf, 4, offset, v);
    }

    public static void putShort(byte[] buf, int offset, short v) {
        putInt(buf, 2, offset, v);
    }

    public static void putInt(byte[] buf, int length, int offset, int v) {
        for (int i = 0; i < length; i++) {
            buf[offset + i] = (byte) (v >> (8 * i));
        }
    }

    public static boolean getBoolean(byte[] buf, int offset) {
        int ch = buf[offset];
        return (ch != 0);
    }

    public static void putBoolean(byte[] buf, int offset, boolean v) {
        buf[offset] = (byte) (v ? 1 : 0);
    }

    public static long getLong(byte[] buf, int offset) {
        return (((long) buf[offset] << 56) + ((long) (buf[offset + 1] & 255) << 48) + ((long) (buf[offset + 2] & 255) << 40) + ((long) (buf[offset + 3] & 255) << 32) + ((long) (buf[offset + 4] & 255) << 24) + ((buf[offset + 5] & 255) << 16) + ((buf[offset + 6] & 255) << 8) + ((buf[offset + 7] & 255) << 0));
    }

    public static void putLong(byte[] buf, int offset, long v) {
        buf[offset] = (byte) (v >>> 56);
        buf[offset + 1] = (byte) (v >>> 48);
        buf[offset + 2] = (byte) (v >>> 40);
        buf[offset + 3] = (byte) (v >>> 32);
        buf[offset + 4] = (byte) (v >>> 24);
        buf[offset + 5] = (byte) (v >>> 16);
        buf[offset + 6] = (byte) (v >>> 8);
        buf[offset + 7] = (byte) (v >>> 0);
    }

    private static void read_from(DataInput in, byte[] arr) throws IOException, ClassCastException {
        if (in instanceof InputStream) {
            InputStream is = (InputStream) in;
            is.read(arr);
        } else if (in instanceof RandomAccessFile) {
            RandomAccessFile raf = (RandomAccessFile) in;
            raf.read(arr);
        } else {
            throw new ClassCastException("unsupported " + in.getClass());
        }
    }
}

package ao.util.persist;

import ao.util.math.Calc;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * User: alex
 * Date: 12-Jul-2009
 * Time: 8:53:35 PM
 * 
 * File-system memory map
 */
public class Mmap {

    private Mmap() {
    }

    private static final int BUFFER_SIZE = 2 * 4 * 8 * 256;

    private static final int DOUBLE_BYTES = Double.SIZE / 8;

    private static final int CHAR_BYTES = Character.SIZE / 8;

    private static final int INT_BYTES = Integer.SIZE / 8;

    public static byte[] bytes(int offset, int length, File readFrom) {
        byte mapped[] = new byte[length];
        FileInputStream in = null;
        try {
            try {
                in = new FileInputStream(readFrom);
                FileChannel ch = in.getChannel();
                bytes(mapped, 0, offset, length, ch);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            throw new Error(e);
        }
        return mapped;
    }

    public static int bytes(byte into[], int offset, FileChannel using) throws IOException {
        return bytes(into, offset, offset, (into.length - offset), using);
    }

    public static int bytes(byte into[], int arrayOffset, long fileOffset, int length, FileChannel using) throws IOException {
        MappedByteBuffer mb = using.map(FileChannel.MapMode.READ_ONLY, fileOffset, length);
        int nextIn = arrayOffset;
        byte buffer[] = new byte[BUFFER_SIZE];
        while (mb.hasRemaining()) {
            int nGet = Math.min(mb.remaining(), BUFFER_SIZE);
            mb.get(buffer, 0, nGet);
            for (int i = 0; i < nGet; i++) {
                into[nextIn++] = buffer[i];
            }
        }
        return length;
    }

    public static char[] chars(int charOffset, int length, File readFrom) {
        char mapped[] = new char[length];
        FileInputStream in = null;
        try {
            try {
                in = new FileInputStream(readFrom);
                FileChannel ch = in.getChannel();
                chars(mapped, 0, charOffset * CHAR_BYTES, length, ch);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            throw new Error(e);
        }
        return mapped;
    }

    public static int chars(char into[], int offset, FileChannel using) throws IOException {
        return chars(into, offset, (long) offset * CHAR_BYTES, (into.length - offset), using);
    }

    public static int chars(char into[], int arrayOffset, long fileOffset, int length, FileChannel using) throws IOException {
        int toMapBytes = (int) Math.min(Math.min((using.size() - fileOffset) / CHAR_BYTES, length) * CHAR_BYTES, Integer.MAX_VALUE);
        MappedByteBuffer mb = using.map(FileChannel.MapMode.READ_ONLY, fileOffset, toMapBytes);
        int nextIn = arrayOffset;
        byte buffer[] = new byte[BUFFER_SIZE];
        while (mb.hasRemaining()) {
            int nGet = Math.min(mb.remaining(), BUFFER_SIZE);
            mb.get(buffer, 0, nGet);
            for (int i = 0; i < nGet; i += CHAR_BYTES) {
                into[nextIn++] = Calc.toChar(buffer, i);
            }
        }
        return toMapBytes / CHAR_BYTES;
    }

    public static int[] ints(int intOffset, int length, File readFrom) {
        int mapped[] = new int[length];
        FileInputStream in = null;
        try {
            try {
                in = new FileInputStream(readFrom);
                FileChannel ch = in.getChannel();
                ints(mapped, 0, intOffset * INT_BYTES, length, ch);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            throw new Error(e);
        }
        return mapped;
    }

    public static int ints(int into[], int offset, FileChannel using) throws IOException {
        return ints(into, offset, (long) offset * INT_BYTES, (into.length - offset), using);
    }

    public static int ints(int into[], int arrayOffset, long fileOffset, int length, FileChannel using) throws IOException {
        int toMapBytes = (int) Math.min(Math.min((using.size() - fileOffset) / INT_BYTES, length) * INT_BYTES, Integer.MAX_VALUE);
        MappedByteBuffer mb = using.map(FileChannel.MapMode.READ_ONLY, fileOffset, toMapBytes);
        int nextIn = arrayOffset;
        byte buffer[] = new byte[BUFFER_SIZE];
        while (mb.hasRemaining()) {
            int nGet = Math.min(mb.remaining(), BUFFER_SIZE);
            mb.get(buffer, 0, nGet);
            for (int i = 0; i < nGet; i += INT_BYTES) {
                into[nextIn++] = Calc.toInt(buffer, i);
            }
        }
        return toMapBytes / INT_BYTES;
    }

    public static double[] doubles(int doubleOffset, int length, File readFrom) {
        double mapped[] = new double[length];
        FileInputStream in = null;
        try {
            try {
                in = new FileInputStream(readFrom);
                FileChannel ch = in.getChannel();
                doubles(mapped, 0, doubleOffset * DOUBLE_BYTES, length, ch);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            throw new Error(e);
        }
        return mapped;
    }

    public static int doubles(double into[], int offset, FileChannel using) throws IOException {
        return doubles(into, offset, (long) offset * DOUBLE_BYTES, (into.length - offset), using);
    }

    public static int doubles(double into[], int arrayOffset, long fileOffset, int length, FileChannel using) throws IOException {
        int toMapBytes = (int) Math.min(Math.min((using.size() - fileOffset) / DOUBLE_BYTES, length) * DOUBLE_BYTES, Integer.MAX_VALUE);
        MappedByteBuffer mb = using.map(FileChannel.MapMode.READ_ONLY, fileOffset, toMapBytes);
        int nextIn = arrayOffset;
        byte buffer[] = new byte[BUFFER_SIZE];
        while (mb.hasRemaining()) {
            int nGet = Math.min(mb.remaining(), BUFFER_SIZE);
            mb.get(buffer, 0, nGet);
            for (int i = 0; i < nGet; i += DOUBLE_BYTES) {
                into[nextIn++] = Calc.toDouble(buffer, i);
            }
        }
        return toMapBytes / DOUBLE_BYTES;
    }
}

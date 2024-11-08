package org.proteomecommons.mzml.zip;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import org.apache.xerces.impl.dv.util.Base64;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author Takis
 */
public class Util {

    private static final int version = 1;

    public static final byte[] OPEN_TAG = "<".getBytes();

    public static final byte[] OPEN_END_TAG = "</".getBytes();

    public static final byte[] CLOSE_TAG = ">".getBytes();

    public static final byte[] CLOSE_EMPTY_TAG = "/>".getBytes();

    public static final byte[] NULL = new byte[] { -1 };

    public static final byte[] SLASH = "/".getBytes();

    public static final int IO_BUFFER = 1024;

    /**
     * <p>I'm not sure that getFloatFromDecodedArray is correct. Need to figure out. This works.</p>
     * @param input Base64-encoded 32-bit float array (zlib compressed)
     * @return 32-bit float array
     * @throws java.lang.Exception
     * @see #getFloatFromDecodedArray(byte[], int) 
     */
    private static float[] getFloatArrayFromCompressedBase64String(String input) throws Exception {
        byte[] binArray = Base64.decode(input);
        {
            ByteBuffer bbuf = ByteBuffer.allocate(binArray.length);
            bbuf.put(binArray);
            binArray = bbuf.order(ByteOrder.LITTLE_ENDIAN).array();
        }
        byte[] decompressedData = null;
        {
            Inflater decompressor = new Inflater();
            decompressor.setInput(binArray);
            ByteArrayOutputStream bos = null;
            try {
                bos = new ByteArrayOutputStream(binArray.length);
                byte[] buf = new byte[1024];
                while (!decompressor.finished()) {
                    int count = decompressor.inflate(buf);
                    bos.write(buf, 0, count);
                }
            } finally {
                try {
                    bos.close();
                } catch (Exception nope) {
                }
            }
            decompressedData = bos.toByteArray();
        }
        final int totalFloats = decompressedData.length / 4;
        float[] floatValues = new float[totalFloats];
        int floatIndex = 0;
        for (int nextFloatPosition = 0; nextFloatPosition < decompressedData.length; nextFloatPosition += 4) {
            char c1 = (char) decompressedData[nextFloatPosition + 0];
            char c2 = (char) decompressedData[nextFloatPosition + 1];
            char c3 = (char) decompressedData[nextFloatPosition + 2];
            char c4 = (char) decompressedData[nextFloatPosition + 3];
            int b1 = (int) (c1 & 0xFF);
            int b2 = (int) (c2 & 0xFF);
            int b3 = (int) (c3 & 0xFF);
            int b4 = (int) (c4 & 0xFF);
            int intBits = (b4 << 0) | (b3 << 8) | (b2 << 16) | (b1 << 24);
            floatValues[floatIndex] = Float.intBitsToFloat(intBits);
            floatIndex++;
        }
        return floatValues;
    }

    /**
     * 
     * @param buffer
     * @param position
     * @return
     */
    public static float getFloatFromDecodedArray(byte[] buffer, int position) {
        char c1 = (char) buffer[position + 0];
        char c2 = (char) buffer[position + 1];
        char c3 = (char) buffer[position + 2];
        char c4 = (char) buffer[position + 3];
        int b1 = (int) (c1 & 0xFF);
        int b2 = (int) (c2 & 0xFF);
        int b3 = (int) (c3 & 0xFF);
        int b4 = (int) (c4 & 0xFF);
        int intBits = (b4 << 0) | (b3 << 8) | (b2 << 16) | (b1 << 24);
        float flt = Float.intBitsToFloat(intBits);
        return flt;
    }

    /**
     * 
     * @param buffer
     * @param position
     * @param f
     */
    public static void putFloatIntoDecodedArray(byte[] buffer, int position, float f) {
        int intBits = Float.floatToIntBits(f);
        buffer[position + 3] = (byte) ((intBits & 0x000000ff) >> 0);
        buffer[position + 2] = (byte) ((intBits & 0x0000ff00) >> 8);
        buffer[position + 1] = (byte) ((intBits & 0x00ff0000) >> 16);
        buffer[position + 0] = (byte) ((intBits & 0xff000000) >> 24);
    }

    /**
     * 
     * @param i
     * @return
     */
    public static byte[] convertIntToBytes(int i) {
        byte[] intBytes = new byte[4];
        intBytes[0] = (byte) ((i & 0x000000ff) >> 0);
        intBytes[1] = (byte) ((i & 0x0000ff00) >> 8);
        intBytes[2] = (byte) ((i & 0x00ff0000) >> 16);
        intBytes[3] = (byte) ((i & 0xff000000) >> 24);
        return intBytes;
    }

    /**
     * 
     * @param b
     * @return
     */
    public static int convertBytesToInt(byte[] b) {
        if (b.length != 4) {
            throw new RuntimeException("Expecting 4 bytes in convertBytesToInt, instead found: " + b.length);
        }
        int i = (b[0] << 0) & 0x000000ff | (b[1] << 8) & 0x0000ff00 | (b[2] << 16) & 0x00ff0000 | (b[3] << 24) & 0xff000000;
        return i;
    }

    /**
     * 
     * @param bytes
     * @return
     */
    public static byte[] putLittleEndian(byte[] bytes) {
        ByteBuffer bbuf = ByteBuffer.allocate(bytes.length);
        bbuf.put(bytes);
        return bbuf.order(ByteOrder.LITTLE_ENDIAN).array();
    }

    /**
     * 
     */
    private static String newLineCache = null;

    /**
     * 
     * @return
     */
    public static synchronized String newline() {
        if (newLineCache == null) {
            newLineCache = System.getProperty("line.separator");
            if (newLineCache == null) {
                newLineCache = "\n";
            }
        }
        return newLineCache;
    }

    /**
     * <p>Tries to create a file with a numeric suffix (e.g., "my-file.txt.1"). If exists, increments number and tries again.</p>
     * <p>Once tried 100, throws exception -- too many files!</p>
     * @param parentDirectory
     * @param desiredFileName
     * @return
     */
    public static File createFileWithUniqueSuffix(File parentDirectory, String desiredFileName, String desiredSuffix) throws IOException {
        if (desiredFileName.contains(".mzml")) {
            desiredFileName = desiredFileName.substring(0, desiredFileName.indexOf(".mzml"));
        }
        desiredFileName += desiredSuffix;
        final int MAX = 100;
        for (int i = 1; i <= MAX; i++) {
            File f = new File(parentDirectory, desiredFileName + "." + i);
            if (!f.exists()) {
                f.createNewFile();
                return f;
            }
        }
        throw new IOException("Tried to create " + new File(parentDirectory, desiredFileName + ".[number] with numeric values between 1 and 100, but failed. Check directory -- is it litered with tmp files?"));
    }

    /**
     * <p>Fill an array with data from InputStream. Stops when:</p>
     * <ul>
     *   <li>Array is full</li>
     *   <li>InputStream returns -1 when reading</li>
     * </ul>
     * @param arr
     * @throws java.io.IOException
     */
    public static int read(byte[] arr, InputStream is) throws IOException {
        int offset = 0;
        int remainingBytesToRead = arr.length;
        int bytesJustRead = 0;
        while (remainingBytesToRead > 0 && (bytesJustRead = is.read(arr, offset, remainingBytesToRead)) != -1) {
            offset += bytesJustRead;
            remainingBytesToRead = arr.length - offset;
        }
        if (offset == 0) {
            return -1;
        }
        return offset;
    }

    /**
     * 
     * @param arr1
     * @param arr2
     * @return
     */
    public static boolean bytesMatch(byte[] arr1, byte[] arr2) {
        if (arr1.length != arr2.length) {
            return false;
        }
        for (int i = 0; i < arr1.length; i++) {
            if (arr1[i] != arr2[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 
     * @param gzip
     * @param to
     * @throws java.io.IOException
     */
    public static void decompressGZIP(File gzip, File to) throws IOException {
        decompressGZIP(gzip, to, 0);
    }

    /**
     * 
     * @param gzip
     * @param to
     * @param skip
     * @throws java.io.IOException
     */
    public static void decompressGZIP(File gzip, File to, long skip) throws IOException {
        GZIPInputStream gis = null;
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(to));
            FileInputStream fis = new FileInputStream(gzip);
            fis.skip(skip);
            gis = new GZIPInputStream(fis);
            final byte[] buffer = new byte[IO_BUFFER];
            int read = -1;
            while ((read = gis.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
        } finally {
            try {
                gis.close();
            } catch (Exception nope) {
            }
            try {
                bos.flush();
            } catch (Exception nope) {
            }
            try {
                bos.close();
            } catch (Exception nope) {
            }
        }
    }

    public static byte[] compressZLIB(byte[] uncompressed) throws IOException, DataFormatException {
        ByteArrayOutputStream bos = null;
        try {
            Deflater compressor = new Deflater();
            compressor.setLevel(Deflater.BEST_COMPRESSION);
            compressor.setInput(uncompressed);
            compressor.finish();
            bos = new ByteArrayOutputStream(uncompressed.length);
            byte[] buf = new byte[1024];
            while (!compressor.finished()) {
                int count = compressor.deflate(buf);
                bos.write(buf, 0, count);
            }
            try {
                bos.close();
            } catch (Exception e) {
            }
            return bos.toByteArray();
        } finally {
            try {
                bos.close();
            } catch (Exception e) {
            }
        }
    }

    public static int getVersion() {
        return version;
    }

    /**
     * 
     * @param mzsquashInput
     * @return
     * @throws java.io.IOException
     */
    public static int getVersion(File mzsquashInput) throws IOException {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(mzsquashInput));
            byte[] bytes = new byte[4];
            Util.read(bytes, bis);
            return Util.convertBytesToInt(bytes);
        } finally {
            try {
                bis.close();
            } catch (Exception nope) {
            }
        }
    }
}

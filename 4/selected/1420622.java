package util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * This class includes static methods that are used to transfer data over TCP/IP
 * protocol as byte streams. It is mainly used to upload and download files.
 * @author CUNEYT
 */
public class ByteStream {

    /**
     * Converts the given integer value to an array of bytes
     *
     * @param in_int the integer value
     * @return the byte representation of the given integer as array
     *
     */
    private static byte[] toByteArray(int in_int) {
        byte a[] = new byte[4];
        for (int i = 0; i < 4; i++) {
            int b_int = (in_int >> (i * 8)) & 255;
            byte b = (byte) (b_int);
            a[i] = b;
        }
        return a;
    }

    /**
     * Converts the given byte array to integer
     *
     * @param byte_array_4 the byte array of size 4 to be converted to integer
     * @return the integer value of the given byte array
     */
    private static int toInt(byte[] byte_array_4) {
        int ret = 0;
        for (int i = 0; i < 4; i++) {
            int b = (int) byte_array_4[i];
            if (i < 3 && b < 0) {
                b = 256 + b;
            }
            ret += b << (i * 8);
        }
        return ret;
    }

    /**
     * Reads the first 4 bytes from the given input stream and converts to integer.
     *
     * @param in the input stream to read
     * @return integer value of the first 4 bytes read from in
     * @throws java.io.IOException when I/O error occurs
     */
    public static int toInt(InputStream in) throws java.io.IOException {
        byte[] byte_array_4 = new byte[4];
        byte_array_4[0] = (byte) in.read();
        byte_array_4[1] = (byte) in.read();
        byte_array_4[2] = (byte) in.read();
        byte_array_4[3] = (byte) in.read();
        return toInt(byte_array_4);
    }

    /**
     * Converts the data read from the given input stream to string.
     *
     * @param ins the input stream from which to read
     * @return the string value read from the input stream
     * @throws java.io.IOException when I/O error occurs
     */
    public static String toString(InputStream ins) throws java.io.IOException {
        int len = toInt(ins);
        return toString(ins, len);
    }

    /**
     * Converts the data read from the given input stream to a string of specified
     * length.
     *
     * @param ins the input stream to read
     * @param len the length to be read
     * @return the string value of specified length read from the given input stream
     * @throws IOException when I/O error occurs
     */
    private static String toString(InputStream ins, int len) throws java.io.IOException {
        String ret = new String();
        for (int i = 0; i < len; i++) {
            ret += (char) ins.read();
        }
        return ret;
    }

    /**
     * Writes the given integer value to the specified output stream.
     * @param os the output stream to write
     * @param i the integer value to be written
     * @throws java.io.IOException when I/O error occurs
     */
    public static void toStream(OutputStream os, int i) throws java.io.IOException {
        byte[] byte_array_4 = toByteArray(i);
        os.write(byte_array_4);
    }

    /**
     * Writes the given string to the specified output stream.
     * @param os the output stream to write
     * @param s the string value to be written
     * @throws java.io.IOException when I/O error occurs
     */
    public static void toStream(OutputStream os, String s) throws java.io.IOException {
        int len_s = s.length();
        toStream(os, len_s);
        for (int i = 0; i < len_s; i++) {
            os.write((byte) s.charAt(i));
        }
        os.flush();
    }

    /**
     * Writes the data of specified length read from the given input stream to the
     * given file output stream.
     * @param buf_size the buffer size to read at once from the stream
     * @param fos the file output stream to write the data
     * @param ins the input stream to read data from
     * @param len the length of the file in bytes
     * @throws FileNotFoundException when the file to be written is not found
     * @throws IOException when I/O error occurs
     */
    private static void toFile(InputStream ins, FileOutputStream fos, int len, int buf_size) throws java.io.FileNotFoundException, java.io.IOException {
        byte[] buffer = new byte[buf_size];
        int len_read = 0;
        int total_len_read = 0;
        while (total_len_read + buf_size <= len) {
            len_read = ins.read(buffer);
            total_len_read += len_read;
            fos.write(buffer, 0, len_read);
            fos.flush();
        }
        if (total_len_read < len) {
            toFile(ins, fos, len - total_len_read, buf_size / 2);
        }
    }

    /**
     * Writes the data read from the given input stream of specified length
     * to the given file.
     * @param ins the input stream to read data from
     * @param file the file to store the data
     * @param len the length of the data to read
     * @throws FileNotFoundException when the given file does not exist
     * @throws IOException when I/O error occurs
     */
    private static void toFile(InputStream ins, File file, int len) throws java.io.FileNotFoundException, java.io.IOException {
        FileOutputStream fos = new FileOutputStream(file);
        toFile(ins, fos, len, 1024);
        fos.close();
    }

    /**
     * Saves the data read from the given input stream to the specified file
     * @param ins the input stream to read data from
     * @param file the file to save the data
     * @throws java.io.FileNotFoundException when the specified file does not exist
     * @throws java.io.IOException when I/O error occurs
     */
    public static void toFile(InputStream ins, File file) throws java.io.FileNotFoundException, java.io.IOException {
        int len = toInt(ins);
        toFile(ins, file, len);
    }

    /**
     * Writes the given file to the specified output stream
     * @param os the output stream to write the data
     * @param file the file of which the contents will be written
     * @throws java.io.FileNotFoundException when the given file does not exist
     * @throws java.io.IOException when I/O error occurs
     */
    public static void toStream(OutputStream os, File file) throws java.io.FileNotFoundException, java.io.IOException {
        toStream(os, (int) file.length());
        byte b[] = new byte[1024];
        InputStream is = new FileInputStream(file);
        int numRead = 0;
        while ((numRead = is.read(b)) > 0) {
            os.write(b, 0, numRead);
        }
        os.flush();
    }
}

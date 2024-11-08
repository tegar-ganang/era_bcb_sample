package cz.cuni.mff.ufal.volk.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Some auxiliary methods.
 * @author Bart�omiej Etenkowski
 */
public class Utils {

    private Utils() {
    }

    /**
   * Default buffer size (set to 65536).
   */
    public static final int DEFAULT_BUFFER_SIZE = 65536;

    /**
   * Reads all bytes contained in a file. The {@code readAllBytes(file)} call has the same
   * effect as {@code readAllBytes(file, DEFAULT_BUFFER_SIZE)}.
   * @param  file the file to read
   * @return the byte array that contains all bytes contained in the file
   * @throws IOException if an I/O exception occurs
   */
    public static byte[] readAllBytes(File file) throws IOException {
        return readAllBytes(file, DEFAULT_BUFFER_SIZE);
    }

    /**
	 * Reads all bytes contained in a file.
	 *
	 * @param file
	 *          the file to read
	 * @param bufferSize
	 *          the size of the buffer used to read the file
	 * @return the byte array that contains all bytes contained in the file
	 * @throws IOException
	 *           if an I/O exception occurs
	 */
    public static byte[] readAllBytes(File file, int bufferSize) throws IOException {
        if (bufferSize < 1) {
            throw new IllegalArgumentException("The bufferSize parameter is less than 1");
        }
        InputStream in = new FileInputStream(file);
        try {
            return readAllBytes(in, bufferSize);
        } finally {
            in.close();
        }
    }

    public static byte[] readAllBytes(InputStream in, int bufferSize, int maxSize) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[bufferSize];
        int length;
        int totalSize = 0;
        while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
            totalSize += length;
            if (maxSize > 0 && totalSize >= maxSize) break;
        }
        return out.toByteArray();
    }

    public static byte[] readAllBytes(InputStream in, int bufferSize) throws IOException {
        return readAllBytes(in, bufferSize, -1);
    }

    /**
   * Writes all bytes contained in the b array into the desired file.
   * @param  file the file to be opened for writing
   * @param  b the data
   * @throws IOException if an I/O exception occurs
   * @throws NullPointerException if any of the parameters is {@code null}
   */
    public static void writeAllBytes(File file, byte[] b) throws IOException {
        file.getClass();
        b.getClass();
        OutputStream out = new FileOutputStream(file);
        try {
            out.write(b);
        } finally {
            out.close();
        }
    }

    public static void writeAllBytes(File file, ByteArray b) throws IOException {
        file.getClass();
        b.getClass();
        byte[] buf = new byte[b.size()];
        b.copyTo(buf, 0);
        writeAllBytes(file, buf);
    }

    public static String getExtension(String format) {
        format.getClass();
        if (format.length() == 0) {
            return format;
        }
        return "." + format.split("/")[0];
    }

    private static final int DEFAULT_RMI_PORT = 1039;

    public static int getPort(String name) throws MalformedURLException {
        Pattern p = Pattern.compile(":[\\d]+");
        Matcher m = p.matcher(name);
        if (m.find()) {
            return Integer.parseInt(m.group().substring(1));
        } else {
            return DEFAULT_RMI_PORT;
        }
    }

    public static <T> String arrayToString(T[] arr) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (T item : arr) {
            if (first) first = false; else sb.append(" ");
            sb.append(item);
        }
        return sb.toString();
    }

    /**
   * Reads all the data accessible in the input stream and writes them to the output stream.
   * It uses the buffer of the defined size. None of the stream will be closed after copying.
   *
   * @param in the input stream that the data is read from.
   * @param out the output stream that the data is written to.
   * @param bufferSize the size of the buffer
   * @return total number of copied bytes
   * @throws IOException if an {@code IOException} occurred during reading or writing data
   */
    public static long copy(InputStream in, OutputStream out, int bufferSize) throws IOException {
        long total = 0;
        byte[] buf = new byte[bufferSize];
        int read;
        while ((read = in.read(buf)) > -1) {
            out.write(buf, 0, read);
            total += read;
        }
        return total;
    }

    /**
   * Reads all the data accessible in the input stream and writes them to the output stream.
   * It uses the buffer of the default size of 4096 bytes. None of the stream will be closed after
   * copying.
   *
   * @param in the input stream that the data is read from.
   * @param out the output stream that the data is written to.
   * @return total number of copied bytes
   * @throws IOException if an {@code IOException} occurred during reading or writing data
   */
    public static long copy(InputStream in, OutputStream out) throws IOException {
        return copy(in, out, 1024);
    }
}

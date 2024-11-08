package jomm.utils;

import java.io.*;

/**
 * @author Jorge Machado
 * @date 27/Mai/2008
 * @see jomm.utils
 */
public class StreamsUtils {

    public static byte[] readBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int readedBytes;
        byte[] buf = new byte[1024];
        while ((readedBytes = stream.read(buf)) > 0) {
            b.write(buf, 0, readedBytes);
        }
        b.close();
        return b.toByteArray();
    }

    public static String readString(InputStream stream) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int readedBytes;
        byte[] buf = new byte[1024];
        while ((readedBytes = stream.read(buf)) > 0) {
            b.write(buf, 0, readedBytes);
        }
        b.close();
        return b.toString();
    }

    public static String readString(InputStream stream, String encoding) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int readedBytes;
        byte[] buf = new byte[1024];
        while ((readedBytes = stream.read(buf)) > 0) {
            b.write(buf, 0, readedBytes);
        }
        b.close();
        return b.toString(encoding);
    }

    public static void inputStream2File(InputStream stream, File f) throws IOException {
        f.getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(f);
        inputStream2OutputStream(stream, out);
    }

    public static void inputStream2OutputStream(InputStream stream, OutputStream out) throws IOException {
        int readedBytes;
        byte[] buf = new byte[1024];
        while ((readedBytes = stream.read(buf)) > 0) {
            out.write(buf, 0, readedBytes);
        }
        stream.close();
        out.close();
    }
}

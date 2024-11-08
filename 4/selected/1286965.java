package org.monet.docservice.core.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.nio.channels.FileChannel;

public class StreamHelper {

    public static void close(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception e) {
            }
        }
    }

    public static void close(OutputStream stream) {
        if (stream != null) {
            try {
                stream.flush();
                stream.close();
            } catch (Exception e) {
            }
        }
    }

    public static void close(FileChannel fc) {
        if (fc != null) {
            try {
                fc.close();
            } catch (Exception e) {
            }
        }
    }

    public static void close(RandomAccessFile raf) {
        if (raf != null) {
            try {
                raf.close();
            } catch (Exception e) {
            }
        }
    }

    public static void copy(InputStream inputStream, FileOutputStream outputStream) throws IOException {
        try {
            byte[] buffer = new byte[4096];
            int readed = 0;
            while ((readed = inputStream.read(buffer, 0, buffer.length)) > 0) {
                outputStream.write(buffer, 0, readed);
            }
        } finally {
            close(inputStream);
            close(outputStream);
        }
    }

    public static String toString(InputStream inputStream) throws IOException {
        StringWriter writer = new StringWriter();
        try {
            InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
            char[] buffer = new char[4096];
            int readed = 0;
            while ((readed = reader.read(buffer, 0, buffer.length)) > 0) {
                writer.write(buffer, 0, readed);
            }
        } finally {
            close(inputStream);
        }
        return writer.toString();
    }
}

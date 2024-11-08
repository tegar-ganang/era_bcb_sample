package org.maestroframework.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public class StreamUtils {

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
    }

    public static void copyStream(Reader reader, Writer writer) throws IOException {
        char[] buf = new char[8192];
        int len;
        while ((len = reader.read(buf)) > 0) {
            writer.write(buf, 0, len);
        }
        writer.flush();
    }

    public static void copyStream(InputStream in, Writer writer) throws IOException {
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) > 0) {
            writer.write(new String(buf, "UTF-8"), 0, len);
        }
        writer.flush();
    }

    public static Reader inputStreamToReader(InputStream in) throws IOException {
        if (!in.markSupported()) {
            return new InputStreamReader(in);
        }
        in.mark(3);
        int byte1 = in.read();
        int byte2 = in.read();
        if (byte1 == 0xFF && byte2 == 0xFE) {
            return new InputStreamReader(in, "UTF-16LE");
        } else if (byte1 == 0xFF && byte2 == 0xFF) {
            return new InputStreamReader(in, "UTF-16BE");
        } else {
            int byte3 = in.read();
            if (byte1 == 0xEF && byte2 == 0xBB && byte3 == 0xBF) {
                return new InputStreamReader(in, "UTF-8");
            } else {
                in.reset();
                return new InputStreamReader(in);
            }
        }
    }
}

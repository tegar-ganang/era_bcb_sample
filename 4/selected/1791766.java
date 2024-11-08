package net.woodstock.rockapi.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public abstract class IOUtils {

    private IOUtils() {
    }

    public static void copy(InputStream input, OutputStream output) throws IOException {
        IOUtils.copy(input, output, true);
    }

    public static void copy(InputStream input, OutputStream output, boolean close) throws IOException {
        int b = -1;
        do {
            b = input.read();
            if (b != -1) {
                output.write(b);
            }
        } while (b != -1);
        if (close) {
            input.close();
            output.flush();
            output.close();
        }
    }

    public static void copy(Reader reader, Writer writer) throws IOException {
        IOUtils.copy(reader, writer, true);
    }

    public static void copy(Reader reader, Writer writer, boolean close) throws IOException {
        int b = -1;
        do {
            b = reader.read();
            if (b != -1) {
                writer.write(b);
            }
        } while (b != -1);
        if (close) {
            reader.close();
            writer.flush();
            writer.close();
        }
    }
}

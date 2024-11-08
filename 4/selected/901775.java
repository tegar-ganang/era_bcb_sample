package ch.oblivion.comixviewer.ui.avd.utils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

public class IOUtils {

    private static final int BUFFER_SIZE = 1024 * 4;

    public static void copy(InputStream input, StringWriter writer) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        char[] charBuffer = new char[BUFFER_SIZE];
        try {
            int read;
            while ((read = reader.read(charBuffer)) != -1) {
                writer.write(charBuffer, 0, read);
            }
        } finally {
            close(writer);
            close(reader);
            close(input);
        }
    }

    private static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }
}

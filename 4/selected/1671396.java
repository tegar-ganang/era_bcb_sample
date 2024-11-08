package org.fpse.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import org.fpse.server.TaggedOutputStream;

public class IOUtils {

    public static void writeIntoFile(File file, String text) throws IOException {
        File parent = file.getParentFile();
        if (!parent.exists()) {
            if (!parent.mkdirs()) throw new IOException("Could not create the folder: " + parent.getAbsolutePath());
        }
        OutputStream out = null;
        Writer writer = null;
        try {
            out = new FileOutputStream(file);
            writer = new OutputStreamWriter(out, TaggedOutputStream.DEFAULT_CHARSET);
            writer.write(text);
        } finally {
            if (null != writer) {
                try {
                    writer.close();
                } catch (IOException _) {
                }
            }
            if (null != out) {
                try {
                    out.close();
                } catch (IOException _) {
                }
            }
        }
    }

    public static String readFromFile(File file) throws IOException {
        StringWriter writer = new StringWriter(2048);
        InputStream in = null;
        Reader reader = null;
        try {
            in = new FileInputStream(file);
            reader = new InputStreamReader(in, TaggedOutputStream.DEFAULT_CHARSET);
            char[] temp = new char[2048];
            int read = reader.read(temp);
            while (read > 0) {
                writer.write(temp, 0, read);
                read = reader.read(temp);
            }
            return writer.toString();
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException _) {
                }
            }
            if (null != in) {
                try {
                    in.close();
                } catch (IOException _) {
                }
            }
        }
    }
}

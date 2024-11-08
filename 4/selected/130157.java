package org.alcatel.jsce.util;

import java.io.*;
import java.util.zip.*;

/**
 * <p>Title: JAIN Slee for the OSP</p>
 * <p>Description: JAIN Slee implementation on top of the Alcatel OSP platform</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: Alcatel Namur</p>
 * @author Dominique Gallot
 * @version 1.0
 */
public class StreamUtils {

    private StreamUtils() {
    }

    public static void copyStream(Reader reader, Writer writer) throws IOException {
        int len;
        char[] buffer = new char[1024];
        while ((len = reader.read(buffer)) >= 0) {
            writer.write(buffer, 0, len);
        }
    }

    public static void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        int len;
        byte[] buffer = new byte[1024];
        while ((len = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, len);
        }
    }

    public static void safeClose(InputStream inputStream) {
        if (inputStream == null) return;
        try {
            inputStream.close();
        } catch (IOException ex) {
        }
    }

    public static void safeClose(OutputStream outputStream) {
        if (outputStream == null) return;
        try {
            outputStream.close();
        } catch (IOException ex) {
        }
    }

    public static void safeClose(Writer writer) {
        if (writer == null) return;
        try {
            writer.close();
        } catch (IOException ex) {
        }
    }

    public static void safeClose(ZipFile zipFile) {
        if (zipFile == null) return;
        try {
            zipFile.close();
        } catch (IOException ex) {
        }
    }
}

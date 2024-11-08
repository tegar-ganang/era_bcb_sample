package org.granite.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Franck WOLFF
 */
public class StreamUtil {

    public static byte[] getResourceAsBytes(String path, ClassLoader loader) throws IOException {
        if (loader == null) loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream(path);
        if (is == null) throw new FileNotFoundException("Resource not found: " + path);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
        try {
            int b = -1;
            while ((b = is.read()) != -1) baos.write(b);
        } finally {
            is.close();
        }
        return baos.toByteArray();
    }

    public static ByteArrayInputStream getResourceAsStream(String path, ClassLoader loader) throws IOException {
        return new ByteArrayInputStream(getResourceAsBytes(path, loader));
    }

    public static String getResourceAsString(String path, ClassLoader loader) throws IOException {
        return new String(getResourceAsBytes(path, loader));
    }

    public static String getStreamAsString(InputStream is) throws IOException {
        if (is == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
        try {
            int b = -1;
            while ((b = is.read()) != -1) baos.write(b);
        } finally {
            is.close();
        }
        return new String(baos.toByteArray());
    }
}

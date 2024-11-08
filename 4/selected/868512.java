package org.csdgn.asia.tools;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The ones starting with _ are the "REALLY LAZY OMG" versions.
 * @author Chase
 *
 */
public class LazyIO {

    public static final String _loadResourceAsString(String filename) {
        try {
            return new String(loadResource(new FileInputStream(filename)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static final String loadResourceAsString(String filename, String charsetName) throws IOException {
        return new String(loadResource(new FileInputStream(filename)), charsetName);
    }

    public static final String loadResourceAsString(String filename) throws IOException {
        return new String(loadResource(new FileInputStream(filename)));
    }

    public static final byte[] loadResource(String filename) throws IOException {
        return loadResource(new FileInputStream(filename));
    }

    public static final byte[] loadResource(InputStream stream) throws IOException {
        if (stream != null) {
            final int BUFFER_SIZE = 4096;
            final BufferedInputStream input = new BufferedInputStream(stream);
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            final byte[] reader = new byte[BUFFER_SIZE];
            int r = 0;
            while ((r = input.read(reader, 0, BUFFER_SIZE)) != -1) buffer.write(reader, 0, r);
            return buffer.toByteArray();
        }
        return null;
    }
}

package dproxy.server.common.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import dproxy.server.common.exceptions.AppInfrastructureException;

/**
 * Utility methods to read from impot streams.
 */
public final class UtilRead {

    /**
     * Private constructor to avoid instantiation.
     */
    private UtilRead() {
    }

    /**
     * Read a stream to a String
     * @param stream input stream
     * @return Content
     */
    public static String readToString(InputStream stream) {
        StringBuffer buffer = new StringBuffer();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        int character;
        try {
            character = reader.read();
            while (character != -1) {
                buffer.append((char) character);
                character = reader.read();
            }
        } catch (IOException e) {
            throw new AppInfrastructureException(e);
        }
        return buffer.toString();
    }

    public static byte[] readToByteArray(InputStream stream) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            int read = stream.read();
            while (read != -1) {
                baos.write(read);
                read = stream.read();
            }
        } catch (IOException e) {
            throw new AppInfrastructureException(e);
        }
        return baos.toByteArray();
    }
}

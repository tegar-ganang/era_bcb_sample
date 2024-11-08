package org.sourceforge.jemm.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Generic Stream Utilities
 * 
 * @author <a href="mailto:csuml@yahoo.co.uk">Paul Keeble</a>
 * 
 */
public final class IOUtil {

    /** Private constructor to prevent instantiation */
    private IOUtil() {
    }

    /**
     * Reads a Stream from beginning to end using a 4K buffer.
     * 
     * @param is The stream that is tread
     * @return The bytes from the Stream
     * @throws IOException Thrown if the underlying stream fails to read.
     */
    public static byte[] readFully(InputStream is) throws IOException {
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        int read = 0;
        while ((read = is.read(buffer)) > 1) result.write(buffer, 0, read);
        return result.toByteArray();
    }
}

package be.roam.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * Utility methods for I/O operations.
 */
public class IOUtil {

    /**
     * Converts an input stream to a string.
     * 
     * @param inputStream input stream to convert
     * 
     * @return string representation of the contents of the input stream
     * 
     * @throws IOException
     */
    public static String convertInputStreamToString(InputStream inputStream) throws IOException {
        StringWriter stringWriter = new StringWriter();
        int readCharacter = 0;
        while ((readCharacter = inputStream.read()) != -1) {
            stringWriter.write(readCharacter);
        }
        return stringWriter.toString();
    }

    private IOUtil() {
        super();
    }
}

package dendrarium.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Pomocnicze operacje na strumieniach
 *
 * @author Piotr Achinger <piotr.achinger at gmail.com>
 */
public class StreamUtils {

    /**
     * Constructs a byte array and fills it with data that is read from the
     * specified resource.
     * @param filename the path to the resource
     * @return the specified resource as a byte array
     * @throws java.io.IOException if the resource cannot be read, or the
     *   bytes cannot be written, or the streams cannot be closed
     */
    public static byte[] obtainByteData(String filename) throws IOException {
        InputStream inputStream = StreamUtils.class.getResourceAsStream(filename);
        byte[] ret = obtainByteDataFromStream(inputStream);
        inputStream.close();
        return ret;
    }

    public static byte[] obtainByteDataFromStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024);
        byte[] bytes = new byte[512];
        int readBytes;
        while ((readBytes = inputStream.read(bytes)) > 0) {
            outputStream.write(bytes, 0, readBytes);
        }
        byte[] byteData = outputStream.toByteArray();
        outputStream.close();
        return byteData;
    }
}

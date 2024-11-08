package net.sf.derquinsej.io;

import static java.lang.Math.max;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.google.common.base.Preconditions;

/**
 * Utility methods for streams.
 * @author Andres Rodriguez
 */
public final class Streams {

    /**
	 * Not instantiable.
	 */
    private Streams() {
        throw new AssertionError();
    }

    public static int DEFAULT_BUFFER_SIZE = 4096;

    /**
	 * Consumes an input stream, writing its contents into an output stream.
	 * @param input Input stream.
	 * @param output Output stream.
	 * @param closeInput If the input stream must be closed after the operation.
	 * @param bufferSize Buffer size.
	 * @return The number of bytes read.
	 * @throws IOException If an I/O error occurs.
	 */
    public static long consume(InputStream input, OutputStream output, boolean closeInput, int bufferSize) throws IOException {
        Preconditions.checkNotNull(input);
        Preconditions.checkNotNull(output);
        long total = 0L;
        int read;
        final byte[] buffer = new byte[max(32, bufferSize)];
        while ((read = input.read(buffer)) > -1) {
            output.write(buffer, 0, read);
            total += read;
        }
        if (closeInput) {
            input.close();
        }
        return total;
    }

    /**
	 * Consumes an input stream, writing its contents into an output stream.
	 * @param input Input stream.
	 * @param output Output stream.
	 * @param closeInput If the input stream must be closed after the operation.
	 * @return The number of bytes read.
	 * @throws IOException If an I/O error occurs.
	 */
    public static long consume(InputStream input, OutputStream output, boolean closeInput) throws IOException {
        return consume(input, output, closeInput, DEFAULT_BUFFER_SIZE);
    }

    /**
	 * Consumes an input stream, writing its contents into a byte array.
	 * @param input Input stream.
	 * @param closeInput If the input stream must be closed after the operation.
	 * @param bufferSize Buffer size.
	 * @return The bytes read.
	 * @throws IOException If an I/O error occurs.
	 */
    public static byte[] consume(InputStream input, boolean closeInput, int bufferSize) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        consume(input, baos, closeInput, bufferSize);
        return baos.toByteArray();
    }

    /**
	 * Consumes an input stream, writing its contents into a byte array.
	 * @param input Input stream.
	 * @param closeInput If the input stream must be closed after the operation.
	 * @return The bytes read.
	 * @throws IOException If an I/O error occurs.
	 */
    public static byte[] consume(InputStream input, boolean closeInput) throws IOException {
        return consume(input, closeInput, DEFAULT_BUFFER_SIZE);
    }
}

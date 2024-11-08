package com.trazere.util.io;

import com.trazere.util.function.Function1;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * DOCME
 */
public class IOUtils {

    /**
	 * Builds a function which builds files.
	 * 
	 * @param <X> Type of the exceptions.
	 * @return The built function.
	 */
    @SuppressWarnings("unchecked")
    public static <X extends Exception> Function1<String, File, X> buildFileFunction() {
        return (Function1<String, File, X>) _BUILD_FILE_FUNCTION;
    }

    private static Function1<String, File, ?> _BUILD_FILE_FUNCTION = new Function1<String, File, RuntimeException>() {

        public File evaluate(final String path) {
            return new File(path);
        }
    };

    /**
	 * Read the data from the given input stream and write it to the given output stream.
	 * 
	 * @param input Input stream to read.
	 * @param output Output stream to write into.
	 * @throws IOException
	 */
    public static void copyStream(final InputStream input, final OutputStream output) throws IOException {
        assert null != input;
        assert null != output;
        final byte[] buffer = new byte[512];
        while (true) {
            final int n = input.read(buffer);
            if (n > 0) {
                output.write(buffer, 0, n);
            } else {
                break;
            }
        }
    }

    /**
	 * Read the text from the given reader and write it to the given writer.
	 * 
	 * @param reader Reader to read from.
	 * @param writer Writer to write into.
	 * @throws IOException
	 */
    public static void copyText(final Reader reader, final Writer writer) throws IOException {
        assert null != reader;
        assert null != writer;
        final char[] buffer = new char[512];
        while (true) {
            final int n = reader.read(buffer);
            if (n > 0) {
                writer.write(buffer, 0, n);
            } else {
                break;
            }
        }
    }

    private IOUtils() {
    }
}

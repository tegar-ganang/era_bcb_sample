package com.enerjy.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import com.enerjy.common.EnerjyException;

/**
 * Utility methods dealing with streams, readers, and writers.
 * 
 * @author michael
 */
public class StreamUtils {

    private static final int COPY_BUF_SIZE = 1024;

    /**
     * Load a properties object from a stream. The input stream will be closed by this method.
     * 
     * @param stream Stream to load properties from.
     * @return Loaded properties.
     * @throws IllegalArgumentException if stream is null.
     * @throws EnerjyException if the stream could not be read.
     */
    public static Properties loadProperties(InputStream stream) {
        if (null == stream) {
            throw new IllegalArgumentException("No stream provided");
        }
        try {
            Properties answer = new Properties();
            answer.load(stream);
            return answer;
        } catch (IOException e) {
            throw new EnerjyException("Could not load propertes from stream", e);
        } finally {
            close(stream);
        }
    }

    /**
     * Close the given reader, ignoring any exceptions.
     * 
     * @param in Reader to close.
     */
    public static void close(Reader in) {
        if (null == in) {
            return;
        }
        try {
            in.close();
        } catch (IOException e) {
        }
    }

    /**
     * Close the given writer, ignoring any exceptions.
     * 
     * @param out Writer to close.
     */
    public static void close(Writer out) {
        if (null == out) {
            return;
        }
        try {
            out.close();
        } catch (IOException e) {
        }
    }

    /**
     * Close the given input stream, ignoring any exceptions.
     * 
     * @param in Stream to close.
     */
    public static void close(InputStream in) {
        if (null == in) {
            return;
        }
        try {
            in.close();
        } catch (IOException e) {
        }
    }

    /**
     * Close the given output stream, ignoring any exceptions.
     * 
     * @param out The stream to close.
     */
    public static void close(OutputStream out) {
        if (null == out) {
            return;
        }
        try {
            out.close();
        } catch (IOException e) {
        }
    }

    /**
     * Read a string from an input stream.
     * 
     * @param is Stream to read.
     * @return String read from the stream.
     * @throws IOException If the stream could not be read.
     */
    public static String readString(InputStream is) throws IOException {
        Reader reader = null;
        Writer writer = null;
        try {
            reader = new InputStreamReader(is);
            writer = new StringWriter();
            copy(reader, writer);
            return writer.toString();
        } finally {
            close(reader);
            close(writer);
        }
    }

    /**
     * Copy data between streams.
     * 
     * @param reader Source of data.
     * @param writer Destination of data.
     * @throws IOException If a problem occurs with the streams
     */
    public static void copy(Reader reader, Writer writer) throws IOException {
        char[] buf = new char[COPY_BUF_SIZE];
        int read = 0;
        while ((read = reader.read(buf)) != -1) {
            writer.write(buf, 0, read);
        }
    }

    /**
     * Copy data between streams.
     * 
     * @param is Source of data.
     * @param os Destination of data.
     * @throws IOException If a problem occurs with the streams
     */
    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[COPY_BUF_SIZE];
        int read = 0;
        while ((read = is.read(buf)) != -1) {
            os.write(buf, 0, read);
        }
    }

    private StreamUtils() {
    }
}

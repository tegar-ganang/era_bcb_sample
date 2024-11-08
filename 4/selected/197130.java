package eu.pisolutions.io;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import eu.pisolutions.lang.Validations;

/**
 * Utilities to work with {@link java.io.InputStream}s, {@link java.io.OutputStream}s, {@link java.io.Reader}s and {@link java.io.Writer}s.
 *
 * @author Laurent Pireyn
 */
public final class Streams extends Object {

    public static void skipFully(InputStream in, long count) throws IOException {
        Streams.validateInputStream(in);
        while (count > 0) {
            final long partialCount = in.skip(count);
            if (partialCount <= 0) {
                throw new EOFException();
            }
            count -= partialCount;
        }
    }

    public static void skipFully(Reader reader, long count) throws IOException {
        Streams.validateReader(reader);
        while (count > 0) {
            final long partialCount = reader.skip(count);
            if (partialCount <= 0) {
                throw new EOFException();
            }
            count -= partialCount;
        }
    }

    public static long copy(InputStream in, OutputStream out) throws IOException {
        Streams.validateInputStream(in);
        Streams.validateOutputStream(out);
        long count = 0;
        while (true) {
            final int b = in.read();
            if (b == -1) {
                break;
            }
            out.write(b);
            ++count;
        }
        return count;
    }

    public static long copy(InputStream in, OutputStream out, int bufferLength) throws IOException {
        Streams.validateInputStream(in);
        Streams.validateOutputStream(out);
        Streams.validateBufferLength(bufferLength);
        final byte[] buffer = new byte[bufferLength];
        int totalCount = 0;
        while (true) {
            final int count = in.read(buffer);
            if (count == -1) {
                break;
            }
            out.write(buffer, 0, count);
            totalCount += count;
        }
        return totalCount;
    }

    public static byte[] readAll(InputStream in) throws IOException {
        return Streams.readAll(in, 1024);
    }

    public static byte[] readAll(InputStream in, int bufferLength) throws IOException {
        Streams.validateInputStream(in);
        final ByteArrayOutputStream bufferOut = new ByteArrayOutputStream(bufferLength);
        Streams.copy(in, bufferOut);
        return bufferOut.toByteArray();
    }

    public static long discardAll(InputStream in) throws IOException {
        return Streams.copy(in, NullOutputStream.INSTANCE);
    }

    public static long discardAll(Reader reader) throws IOException {
        return Streams.copy(reader, NullWriter.INSTANCE);
    }

    public static long discardAll(InputStream in, int bufferLength) throws IOException {
        return Streams.copy(in, NullOutputStream.INSTANCE, bufferLength);
    }

    public static long discardAll(Reader reader, int bufferLength) throws IOException {
        return Streams.copy(reader, NullWriter.INSTANCE, bufferLength);
    }

    public static long copy(Reader reader, Writer writer) throws IOException {
        Streams.validateReader(reader);
        Streams.validateWriter(writer);
        long count = 0;
        while (true) {
            final int b = reader.read();
            if (b == -1) {
                break;
            }
            writer.write(b);
            ++count;
        }
        return count;
    }

    public static long copy(Reader reader, Writer writer, int bufferLength) throws IOException {
        Streams.validateReader(reader);
        Streams.validateWriter(writer);
        Streams.validateBufferLength(bufferLength);
        final char[] buffer = new char[bufferLength];
        int totalCount = 0;
        while (true) {
            final int count = reader.read(buffer);
            if (count == -1) {
                break;
            }
            writer.write(buffer, 0, count);
            totalCount += count;
        }
        return totalCount;
    }

    private static void validateInputStream(InputStream in) {
        Validations.notNull(in, "input stream");
    }

    private static void validateOutputStream(OutputStream out) {
        Validations.notNull(out, "output stream");
    }

    private static void validateReader(Reader reader) {
        Validations.notNull(reader, "reader");
    }

    private static void validateWriter(Writer writer) {
        Validations.notNull(writer, "writer");
    }

    private static void validateBufferLength(int bufferLength) {
        Validations.greaterThan(bufferLength, 0, "buffer length");
    }

    private Streams() {
        super();
    }
}

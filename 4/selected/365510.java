package org.gamegineer.client.internal.ui.console.displays;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import net.jcip.annotations.ThreadSafe;
import org.gamegineer.client.ui.console.IDisplay;

/**
 * Implementation of {@link org.gamegineer.client.ui.console.IDisplay} that uses
 * an arbitrary input stream reader and output stream writer.
 * 
 * <p>
 * Note that this implementation cannot guarantee that the
 * {@code readSecureLine} methods will not echo characters.
 * </p>
 * 
 * <p>
 * This class is thread-safe.
 * </p>
 */
@ThreadSafe
public final class DefaultDisplay implements IDisplay {

    /** The input stream reader. */
    private final SecureBufferedReader m_reader;

    /** The output stream writer. */
    private final PrintWriter m_writer;

    /**
     * Initializes a new instance of the {@code DefaultDisplay} class that uses
     * the standard input and output streams.
     */
    public DefaultDisplay() {
        this(new InputStreamReader(System.in), new OutputStreamWriter(System.out));
    }

    /**
     * Initializes a new instance of the {@code DefaultDisplay} class using the
     * specified input stream reader and output stream writer.
     * 
     * @param reader
     *        The input stream reader; must not be {@code null}.
     * @param writer
     *        The output stream writer; must not be {@code null}.
     */
    DefaultDisplay(final Reader reader, final Writer writer) {
        assert reader != null;
        assert writer != null;
        m_reader = new SecureBufferedReader(reader);
        m_writer = new PrintWriter(writer, true) {

            @Override
            public void close() {
            }
        };
    }

    public void flush() {
        m_writer.flush();
    }

    public IDisplay format(final String format, final Object... args) {
        m_writer.format(format, args);
        return this;
    }

    public Reader getReader() {
        return m_reader;
    }

    public PrintWriter getWriter() {
        return m_writer;
    }

    public String readLine() {
        try {
            return m_reader.readLine();
        } catch (final IOException e) {
            throw new IOError(e);
        }
    }

    public String readLine(final String format, final Object... args) {
        format(format, args);
        return readLine();
    }

    public char[] readSecureLine() {
        try {
            return m_reader.readSecureLine();
        } catch (final IOException e) {
            throw new IOError(e);
        }
    }

    public char[] readSecureLine(final String format, final Object... args) {
        format(format, args);
        return readSecureLine();
    }

    /**
     * A buffered reader that provides the ability to read lines in a secure
     * manner.
     */
    private static final class SecureBufferedReader extends BufferedReader {

        /**
         * Creates a new instance of the {@code SecureBufferedReader} class.
         * 
         * @param reader
         *        The underlying reader; must not be {@code null}.
         * 
         * @throws java.lang.NullPointerException
         *         If {@code reader} is {@code null}.
         */
        SecureBufferedReader(final Reader reader) {
            super(reader);
        }

        @Override
        public void close() {
        }

        char[] readSecureLine() throws IOException {
            final StringBuilder sb = new StringBuilder();
            synchronized (lock) {
                boolean isEndOfLine = false, wasLastCharCR = false;
                while (!isEndOfLine) {
                    final int ch = read();
                    if (ch == -1) {
                        if (sb.length() == 0) {
                            return null;
                        }
                        isEndOfLine = true;
                    } else if ((ch == '\r') && !wasLastCharCR) {
                        mark(1);
                        wasLastCharCR = true;
                    } else if (ch == '\n') {
                        isEndOfLine = true;
                    } else {
                        if (wasLastCharCR) {
                            reset();
                            isEndOfLine = true;
                        } else {
                            sb.append((char) ch);
                        }
                    }
                }
            }
            final char[] line = new char[sb.length()];
            sb.getChars(0, sb.length(), line, 0);
            sb.setLength(0);
            return line;
        }
    }
}

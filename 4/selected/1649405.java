package eu.pisolutions.io;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import eu.pisolutions.lang.Validations;

/**
 * {@link eu.pisolutions.io.ReaderWrapper} that writes all characters read to a {@link java.io.Writer}.
 * <p>
 * Skipped characters are <em>not</em> written to the <code>Writer</code>.
 * </p>
 *
 * @author Laurent Pireyn
 */
public final class TeeReader extends ReaderWrapper {

    private final Writer writer;

    private boolean writeExceptionsIgnored;

    public TeeReader(Reader reader, Writer writer) {
        super(reader);
        Validations.notNull(writer, "writer");
        this.writer = writer;
    }

    public boolean isWriteExceptionsIgnored() {
        return this.writeExceptionsIgnored;
    }

    public void setWriteExceptionsIgnored(boolean writeExceptionsIgnored) {
        this.writeExceptionsIgnored = writeExceptionsIgnored;
    }

    @Override
    public int read() throws IOException {
        final int c = this.reader.read();
        if (c != -1) {
            try {
                this.writer.write(c);
            } catch (IOException exception) {
                this.handleWriteException(exception);
            }
        }
        return c;
    }

    @Override
    public int read(char[] array, int offset, int length) throws IOException {
        final int actualCount = this.reader.read(array, offset, length);
        if (actualCount != -1) {
            try {
                this.writer.write(array, offset, actualCount);
            } catch (IOException exception) {
                this.handleWriteException(exception);
            }
        }
        return actualCount;
    }

    private void handleWriteException(IOException exception) throws IOException {
        if (!this.writeExceptionsIgnored) {
            throw exception;
        }
    }
}

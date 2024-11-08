package org.dellroad.stuff.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Presents an {@link java.io.InputStream InputStream} interface given a {@link WriteCallback} that can write to an
 * {@link OutputStream}. A separate thread is created to perform the actual writing.
 *
 * @since 1.0.74
 */
public class NullModemInputStream extends FilterInputStream {

    private final PipedOutputStream output;

    /**
     * Constructor.
     *
     * @param writer    {@link OutputStream} writer callback
     * @param name      name for this instance; used to create the name of the background thread
     */
    public NullModemInputStream(final WriteCallback writer, String name) {
        super(new PipedInputStream());
        if (writer == null) throw new IllegalArgumentException("null writer");
        try {
            this.output = new PipedOutputStream(this.getPipedInputStream());
        } catch (IOException e) {
            throw new RuntimeException("unexpected exception", e);
        }
        Thread thread = new WriterThread(writer, this.output, name);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Get the wrapped stream cast as a {@link PipedInputStream}.
     */
    protected PipedInputStream getPipedInputStream() {
        return (PipedInputStream) this.in;
    }

    /**
     * Ensure input stream is closed when this instance is no longer referenced.
     *
     * <p>
     * This ensures the writer thread wakes up (and exits, avoiding a memory leak) when an instance of this class
     * is created but never read from.
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            try {
                this.getPipedInputStream().close();
            } catch (IOException e) {
            }
        } finally {
            super.finalize();
        }
    }

    /**
     * Callback interface used by {@link NullModemInputStream}.
     */
    public interface WriteCallback {

        /**
         * Write the output to the given output stream.
         *
         * <p>
         * This method will be invoked (once) asynchronously in a dedicated writer thread.
         * </p>
         *
         * @param output output that sends data to the corresponding {@link NullModemInputStream}
         * @throws IOException if an I/O error occurs
         */
        void writeTo(OutputStream output) throws IOException;
    }

    /**
     * Writer thread. This is designed to not hold a reference to the {@link NullModemInputStream}.
     */
    private static class WriterThread extends Thread {

        private final WriteCallback writer;

        private final PipedOutputStream output;

        WriterThread(WriteCallback writer, PipedOutputStream output, String name) {
            super(name);
            this.writer = writer;
            this.output = output;
        }

        @Override
        public void run() {
            try {
                this.writer.writeTo(this.output);
            } catch (IOException e) {
            } finally {
                try {
                    this.output.close();
                } catch (IOException e) {
                }
            }
        }
    }
}

package com.volantis.mcs.migrate.impl.framework.io;

import com.volantis.synergetics.io.IOUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An object which accepts a byte array and then fully writes that byte array
 * into an output stream.
 * <p>
 * This is the logical inverse of {@link java.io.ByteArrayOutputStream}.
 */
public class OutputStreamByteArray {

    /**
     * The buffer/stream that we read from.
     */
    private ByteArrayInputStream stream;

    /**
     * Initialise.
     *
     * @param buffer the buffer that we read from.
     */
    public OutputStreamByteArray(byte[] buffer) {
        this.stream = new ByteArrayInputStream(buffer);
    }

    /**
     * Write the contents of the byte array provided during construction to
     * the output stream provided.
     *
     * @param output the output stream to write the byte array contents to.
     * @throws IOException if there was an I/O error during the write.
     */
    public void writeTo(OutputStream output) throws IOException {
        IOUtils.copyAndClose(stream, output);
    }
}

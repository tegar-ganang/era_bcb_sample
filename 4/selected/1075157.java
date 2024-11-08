package com.volantis.mcs.migrate.impl.framework.io;

import com.volantis.synergetics.io.IOUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An object which fully reads an input stream and allows access to it's
 * content as a byte array.
 * <p>
 * This is the logical inverse of {@link java.io.ByteArrayInputStream}.
 */
public class InputStreamByteArray {

    /**
     * The buffer we read into.
     */
    private byte[] buffer;

    /**
     * Initialise.
     *
     * @param input the input stream we fully read.
     * @throws IOException if there was an I/O error during the read.
     */
    public InputStreamByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
        IOUtils.copyAndClose(input, baos);
        buffer = baos.toByteArray();
    }

    /**
     * Return the byte array that we read the input stream content into.
     *
     * @return the byte array containing the input stream content.
     */
    public byte[] getByteArray() {
        return buffer;
    }
}

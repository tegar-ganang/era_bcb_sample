package org.restlet.representation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

/**
 * Empty representation with no content.
 * 
 * @author Jerome Louvel
 */
public class EmptyRepresentation extends Representation {

    /**
     * Constructor.
     */
    public EmptyRepresentation() {
        setAvailable(true);
        setTransient(true);
        setSize(0);
    }

    @Override
    public java.nio.channels.ReadableByteChannel getChannel() throws IOException {
        return null;
    }

    @Override
    public Reader getReader() throws IOException {
        return null;
    }

    @Override
    public InputStream getStream() throws IOException {
        return null;
    }

    @Override
    public String getText() throws IOException {
        return null;
    }

    @Override
    public void write(java.io.Writer writer) throws IOException {
    }

    @Override
    public void write(java.nio.channels.WritableByteChannel writableChannel) throws IOException {
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
    }
}

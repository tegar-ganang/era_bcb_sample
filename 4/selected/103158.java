package org.restlet.representation;

import java.io.IOException;
import java.io.Reader;
import org.restlet.data.MediaType;
import org.restlet.engine.io.BioUtils;

/**
 * Representation based on a BIO stream.
 * 
 * @author Jerome Louvel
 */
public abstract class StreamRepresentation extends Representation {

    /**
     * Constructor.
     * 
     * @param mediaType
     *            The media type.
     */
    public StreamRepresentation(MediaType mediaType) {
        super(mediaType);
    }

    @Override
    public java.nio.channels.ReadableByteChannel getChannel() throws IOException {
        return org.restlet.engine.io.NioUtils.getChannel(getStream());
    }

    @Override
    public Reader getReader() throws IOException {
        return BioUtils.getReader(getStream(), getCharacterSet());
    }

    @Override
    public void write(java.nio.channels.WritableByteChannel writableChannel) throws IOException {
        write(org.restlet.engine.io.NioUtils.getStream(writableChannel));
    }

    @Override
    public void write(java.io.Writer writer) throws IOException {
        write(BioUtils.getStream(writer));
    }
}

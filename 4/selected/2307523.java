package org.restlet.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.restlet.data.MediaType;
import org.restlet.util.ByteUtils;

/**
 * Representation based on a NIO byte channel.
 * 
 * @author Jerome Louvel (contact@noelios.com)
 */
public abstract class ChannelRepresentation extends Representation {

    /**
     * Constructor.
     * 
     * @param mediaType
     *            The media type.
     */
    public ChannelRepresentation(MediaType mediaType) {
        super(mediaType);
    }

    /**
     * Returns a stream with the representation's content.
     * 
     * @return A stream with the representation's content.
     */
    @Override
    public InputStream getStream() throws IOException {
        return ByteUtils.getStream(getChannel());
    }

    /**
     * Writes the representation to a byte stream.
     * 
     * @param outputStream
     *            The output stream.
     */
    @Override
    public void write(OutputStream outputStream) throws IOException {
        write(ByteUtils.getChannel(outputStream));
    }
}

package org.restlet.resource;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import org.restlet.data.MediaType;
import org.restlet.util.ByteUtils;

/**
 * Representation based on a writable NIO byte channel. The
 * write(WritableByteChannel) method needs to be overriden in subclasses.
 * 
 * @author Jerome Louvel (contact@noelios.com)
 */
public abstract class WritableRepresentation extends ChannelRepresentation {

    /**
     * Constructor.
     * 
     * @param mediaType
     *            The representation's media type.
     */
    public WritableRepresentation(MediaType mediaType) {
        super(mediaType);
    }

    /**
     * Returns a readable byte channel. If it is supported by a file a read-only
     * instance of FileChannel is returned.
     * 
     * @return A readable byte channel.
     */
    @Override
    public ReadableByteChannel getChannel() throws IOException {
        return ByteUtils.getChannel(this);
    }

    /**
     * Writes the representation to a byte channel. This method is ensured to
     * write the full content for each invocation unless it is a transient
     * representation, in which case an exception is thrown.
     * 
     * @param writableChannel
     *            A writable byte channel.
     * @throws IOException
     */
    @Override
    public abstract void write(WritableByteChannel writableChannel) throws IOException;
}

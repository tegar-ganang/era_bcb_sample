package org.restlet.representation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import org.restlet.data.MediaType;
import org.restlet.engine.io.BioUtils;
import org.restlet.engine.io.NioUtils;

/**
 * Representation based on a NIO byte channel.
 * 
 * @author Jerome Louvel
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

    @Override
    public Reader getReader() throws IOException {
        return BioUtils.getReader(getStream(), getCharacterSet());
    }

    @Override
    public InputStream getStream() throws IOException {
        return NioUtils.getStream(getChannel());
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        write(NioUtils.getChannel(outputStream));
    }

    @Override
    public void write(Writer writer) throws IOException {
        write(BioUtils.getStream(writer));
    }
}

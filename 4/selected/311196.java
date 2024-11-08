package org.restlet.representation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.engine.io.BioUtils;

/**
 * Representation based on a BIO character stream.
 * 
 * @author Jerome Louvel
 */
public abstract class CharacterRepresentation extends Representation {

    /**
     * Constructor.
     * 
     * @param mediaType
     *            The media type.
     */
    public CharacterRepresentation(MediaType mediaType) {
        super(mediaType);
        setCharacterSet(CharacterSet.UTF_8);
    }

    @Override
    public java.nio.channels.ReadableByteChannel getChannel() throws IOException {
        return org.restlet.engine.io.NioUtils.getChannel(getStream());
    }

    @Override
    public InputStream getStream() throws IOException {
        return BioUtils.getStream(getReader(), getCharacterSet());
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        BioUtils.copy(getStream(), outputStream);
    }

    @Override
    public void write(java.nio.channels.WritableByteChannel writableChannel) throws IOException {
        write(org.restlet.engine.io.NioUtils.getStream(writableChannel));
    }
}

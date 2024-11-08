package org.restlet.representation;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.engine.io.NioUtils;

/**
 * Transient representation based on a readable NIO byte channel.
 * 
 * @author Jerome Louvel
 */
public class ReadableRepresentation extends ChannelRepresentation {

    /** The representation's input stream. */
    private volatile ReadableByteChannel channel;

    /**
     * Constructor.
     * 
     * @param readableChannel
     *            The representation's channel.
     * @param mediaType
     *            The representation's media type.
     */
    public ReadableRepresentation(ReadableByteChannel readableChannel, MediaType mediaType) {
        this(readableChannel, mediaType, UNKNOWN_SIZE);
    }

    /**
     * Constructor.
     * 
     * @param channel
     *            The representation's channel.
     * @param mediaType
     *            The representation's media type.
     * @param expectedSize
     *            The expected stream size.
     */
    public ReadableRepresentation(ReadableByteChannel channel, MediaType mediaType, long expectedSize) {
        super(mediaType);
        setSize(expectedSize);
        this.channel = channel;
        setAvailable(channel != null);
        setTransient(true);
    }

    @Override
    public ReadableByteChannel getChannel() throws IOException {
        ReadableByteChannel result = this.channel;
        setAvailable(false);
        return result;
    }

    /**
     * Closes and releases the readable channel.
     */
    @Override
    public void release() {
        if (this.channel != null) {
            try {
                this.channel.close();
            } catch (IOException e) {
                Context.getCurrentLogger().log(Level.WARNING, "Error while releasing the representation.", e);
            }
            this.channel = null;
        }
        super.release();
    }

    /**
     * Sets the readable channel.
     * 
     * @param channel
     *            The readable channel.
     */
    public void setChannel(ReadableByteChannel channel) {
        this.channel = channel;
    }

    @Override
    public void write(WritableByteChannel writableChannel) throws IOException {
        NioUtils.copy(getChannel(), writableChannel);
    }
}

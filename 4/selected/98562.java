package net.sf.syracuse.net.impl;

import net.sf.syracuse.net.NetworkRequest;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;

/**
 * Concrete implementation of {@link NetworkRequest}.
 *
 * @author Chris Conrad
 * @since 1.0.0
 */
public final class NetworkRequestImpl implements NetworkRequest {

    private SelectableChannel channel;

    private ByteBuffer requestBuffer;

    private ByteBuffer responseBuffer;

    /**
     * Instantiates a new {@code NetworkRequestImpl} associated with the provided {@code Channel}.
     *
     * @param selectableChannel the {@code Channel} to associate with the new {@code NetworkRequestImpl}
     */
    public NetworkRequestImpl(SelectableChannel selectableChannel) {
        this.channel = selectableChannel;
    }

    public SelectableChannel getChannel() {
        return channel;
    }

    public ByteBuffer getRequestBuffer() {
        return requestBuffer;
    }

    public void storeRequestBuffer(ByteBuffer requestBuffer) {
        this.requestBuffer = requestBuffer;
    }

    public ByteBuffer getResponseBuffer() {
        return responseBuffer;
    }

    public void storeResponseBuffer(ByteBuffer responseBuffer) {
        this.responseBuffer = responseBuffer;
    }
}

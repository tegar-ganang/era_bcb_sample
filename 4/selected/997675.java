package net.sf.syracuse.net.impl;

import net.sf.syracuse.net.NetworkRequest;
import net.sf.syracuse.net.WriteServicer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Concrete implementation of {@code WriteEventHandler}.
 *
 * @author Chris Conrad
 * @since 1.0.0
 */
public final class WriteEventHandlerImpl extends AbstractHandler implements WriteEventHandler {

    private WriteServicer writeServicer;

    public void handleWrite(NetworkRequest networkRequest) {
        ByteBuffer buffer = networkRequest.getResponseBuffer();
        SocketChannel socketChannel = (SocketChannel) networkRequest.getChannel();
        try {
            socketChannel.write(buffer);
            if (buffer.hasRemaining()) {
                networkEventThread.addChannelInterestOps(socketChannel, SelectionKey.OP_WRITE);
            } else {
                writeServicer.write(networkRequest);
            }
        } catch (IOException e) {
            log.error("Error while writing data", e);
            closeSocket(socketChannel);
        }
    }

    /**
     * Sets the {@code WriteServicer} pipeline.
     *
     * @param writeServicer the {@code WriteServicer} pipeline
     */
    public void setWriteServicer(WriteServicer writeServicer) {
        this.writeServicer = writeServicer;
    }
}

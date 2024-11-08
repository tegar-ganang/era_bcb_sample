package net.sf.syracuse.net.impl;

import net.sf.syracuse.net.DecoderException;
import net.sf.syracuse.net.NetworkRequest;
import net.sf.syracuse.net.ProtocolDecoder;
import net.sf.syracuse.net.ReadServicer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Concrete implementation of {@code ReadEventHandler}.
 *
 * @author Chris Conrad
 * @since 1.0.0
 */
public final class ReadEventHandlerImpl extends AbstractHandler implements ReadEventHandler {

    private ProtocolDecoder protocolDecoder;

    private ReadServicer readServicer;

    public void handleRead(NetworkRequest networkRequest) {
        ByteBuffer buffer = networkRequest.getRequestBuffer();
        if (buffer == null) {
            buffer = ByteBuffer.allocateDirect(4096);
            networkRequest.storeRequestBuffer(buffer);
        }
        SocketChannel socketChannel = (SocketChannel) networkRequest.getChannel();
        int read;
        try {
            read = socketChannel.read(buffer);
        } catch (IOException e) {
            log.error("Error while reading from socket", e);
            closeSocket(socketChannel);
            return;
        }
        if (read == -1) {
            log.info("Client disconnected: " + socketChannel.socket().getInetAddress());
            closeSocket(socketChannel);
        } else if (read != 0) {
            buffer.flip();
            ByteBuffer requestBuffer;
            try {
                requestBuffer = protocolDecoder.decode(buffer);
            } catch (DecoderException e) {
                log.debug("DecoderException thrown while decoding networkRequest. Closing socket" + socketChannel, e);
                closeSocket(socketChannel);
                return;
            }
            if (requestBuffer != null) {
                log.debug("NetworkRequest decoded");
                networkRequest.storeRequestBuffer(requestBuffer);
                readServicer.read(networkRequest);
            } else {
                log.debug("NetworkRequest not decoded, continuing reading");
                buffer.position(buffer.limit());
                buffer.limit(buffer.capacity());
                networkEventThread.addChannelInterestOps(socketChannel, SelectionKey.OP_READ);
            }
        }
    }

    /**
     * Sets the {@code ProtocolDecoder}.
     *
     * @param protocolDecoder the {@code ProtocolDecoder}
     */
    public void setProtocolDecoder(ProtocolDecoder protocolDecoder) {
        this.protocolDecoder = protocolDecoder;
    }

    /**
     * Sets the {@code ReadServicer} pipeline.
     *
     * @param readServicer the {@code ReadServicer} pipelin
     */
    public void setReadServicer(ReadServicer readServicer) {
        this.readServicer = readServicer;
    }
}

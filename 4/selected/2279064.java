package org.szegedi.nioserver.protocols;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import org.szegedi.nbpipe.*;
import org.szegedi.nioserver.*;

public class EchoProtocolHandlerFactory implements ProtocolHandlerFactory {

    private final ByteBufferPool bufferPool;

    public EchoProtocolHandlerFactory(ByteBufferPool bufferPool) {
        this.bufferPool = bufferPool;
    }

    public ProtocolHandler createProtocolHandler(final SocketChannel socketChannel) {
        return new EchoProtocolHandler(socketChannel);
    }

    private final class EchoProtocolHandler extends AbstractPipedProtocolHandler {

        EchoProtocolHandler(SocketChannel socketChannel) {
            super(socketChannel, bufferPool);
        }

        protected boolean doRead() throws IOException {
            return outputPipe.transferFrom(socketChannel) == -1;
        }

        protected void doEndRead() throws IOException {
            outputPipe.closeForWriting();
            if (outputPipe.isExhausted()) {
                doEndWrite();
            }
        }

        protected void doEndWrite() throws IOException {
            if (socketChannel.isOpen()) {
                socketChannel.close();
            }
        }
    }
}

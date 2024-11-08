package org.dancres.blitz.remote.nio;

import org.dancres.blitz.SpaceImpl;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.io.IOException;

/**
 */
public class DispatcherImpl implements Dispatcher {

    private static final int GET_HEADER = 1;

    private static final int GET_BODY = 2;

    private int _state = GET_HEADER;

    private int _msgSize;

    private int _reqId;

    private SpaceImpl _space;

    private ByteBuffer _headerBuffer = ByteBuffer.allocate(8);

    private ByteBuffer _bodyBuffer;

    DispatcherImpl(SpaceImpl aSpace) {
        _space = aSpace;
    }

    public void process(ControlBlock aBlock) throws IOException {
        SocketChannel myChannel = aBlock.getChannel();
        if (_state == GET_HEADER) {
            if (myChannel.read(_headerBuffer) == -1) throw new IOException("Socket closed");
            if (!_headerBuffer.hasRemaining()) {
                _state = GET_BODY;
                _headerBuffer.flip();
                _reqId = _headerBuffer.getInt();
                _msgSize = _headerBuffer.getInt();
                _bodyBuffer = ByteBuffer.allocate(_msgSize);
                _headerBuffer.clear();
            }
        }
        if (_state == GET_BODY) {
            if (myChannel.read(_bodyBuffer) == -1) throw new IOException("Socket closed");
            if (!_bodyBuffer.hasRemaining()) {
                _bodyBuffer.flip();
                try {
                    Pool.execute(new MessageDecoder(_reqId, _space, aBlock, _bodyBuffer.array()));
                } catch (InterruptedException anIE) {
                    System.err.println("Interrupted during message dispatch");
                    anIE.printStackTrace(System.err);
                }
                _state = GET_HEADER;
            }
        }
    }
}

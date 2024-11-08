package org.dancres.blitz.remote.nio;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Responsible for processing selector messages associated with a particular
 * endpoint (socket) as defined by a SelectionKey.  Uses a dispatcher to
 * actually process the data received (it may also invoke methods etc) and to
 * generate responses to be transmitted back through the endpoint.
 */
public class ControlBlock {

    private SelectionKey _key;

    private ArrayList _outputBuffers = new ArrayList();

    private Dispatcher _dispatcher;

    ControlBlock(SelectionKey aKey, Dispatcher aDispatcher) {
        _key = aKey;
        _dispatcher = aDispatcher;
    }

    /**
     * Processes the SelectionKey's events according to internal state machine
     * and dispatches work to threads accordingly
     */
    public void process() throws IOException {
        SocketChannel myChannel = (SocketChannel) _key.channel();
        if (_key.isWritable()) {
            synchronized (_outputBuffers) {
                while (_outputBuffers.size() > 0) {
                    ByteBuffer myBuffer = (ByteBuffer) _outputBuffers.get(0);
                    myChannel.write(myBuffer);
                    if (myBuffer.hasRemaining()) {
                        break;
                    } else _outputBuffers.remove(0);
                }
                if (_outputBuffers.size() == 0) {
                    _key.interestOps(_key.interestOps() ^ SelectionKey.OP_WRITE);
                }
            }
        }
        if (_key.isReadable()) {
            _dispatcher.process(this);
        }
    }

    public SocketChannel getChannel() {
        return (SocketChannel) _key.channel();
    }

    public void send(ByteBuffer[] aBuffers) {
        boolean enableWrites;
        synchronized (_outputBuffers) {
            enableWrites = (_outputBuffers.size() == 0);
            for (int i = 0; i < aBuffers.length; i++) {
                _outputBuffers.add(aBuffers[i]);
            }
        }
        if (enableWrites) {
            if (_key.isValid()) {
                _key.interestOps(_key.interestOps() | SelectionKey.OP_WRITE);
                _key.selector().wakeup();
            }
        }
    }
}

package com.ohua.clustering.operators;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import com.ohua.checkpoint.framework.operators.OperatorCheckpoint;
import com.ohua.clustering.serialization.NetworkOutputStream;
import com.ohua.engine.flowgraph.elements.operator.SystemOperator;

public abstract class AbstractNetworkOperator extends SystemOperator implements OperatorCheckpoint {

    private SocketChannel _channel = null;

    private NetworkOutputStream _networkOutputStream = null;

    public void setChannel(SocketChannel channel) {
        _channel = channel;
    }

    public NetworkOutputStream getOutputStream() {
        try {
            if (_networkOutputStream == null) {
                _networkOutputStream = new NetworkOutputStream(1024, _channel);
            }
            return _networkOutputStream;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public SocketChannel getChannel() {
        return _channel;
    }
}

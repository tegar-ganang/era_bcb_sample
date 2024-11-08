package com.ohua.clustering.operators;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import com.ohua.checkpoint.framework.operatorcheckpoints.AbstractCheckPoint;
import com.ohua.clustering.testing.tests.testInputOutputByteBuffer;
import com.ohua.clustering.testing.tests.testInputOutputByteBuffer.MyObjectInputStream;
import com.ohua.engine.flowgraph.elements.operator.OutputPort;
import com.ohua.engine.flowgraph.elements.operator.OperatorCore.PushDataReturnValue;
import com.ohua.engine.flowgraph.elements.packets.IMetaDataPacket;
import com.ohua.engine.flowgraph.elements.packets.IStreamPacket;
import com.ohua.engine.operators.categories.InputIOOperator;

public class NetworkReaderOperator extends AbstractNetworkOperator implements InputIOOperator {

    private SocketChannel _networkChannel = null;

    private ByteBuffer _buffer = null;

    private ByteArrayInputStream _byteInputStream = null;

    private MyObjectInputStream _objInputStream = null;

    private int _localPort = -1;

    private String _address = null;

    @Override
    public void cleanup() {
        try {
            getChannel().close();
            _objInputStream.close();
            _byteInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void prepare() {
    }

    @Override
    public void runProcessRoutine() {
        int count = 0;
        if (getChannel() == null) {
            return;
        }
        try {
            byte[] bytes = new byte[1024];
            _buffer = ByteBuffer.wrap(bytes);
            assert _buffer.hasArray() && _buffer.isDirect();
            _byteInputStream = new ByteArrayInputStream(bytes);
            while ((count = getChannel().read(_buffer)) > 0) {
                _objInputStream = new testInputOutputByteBuffer().new MyObjectInputStream(_byteInputStream);
                IStreamPacket packet = (IStreamPacket) _objInputStream.readObject();
                while (packet != null) {
                    PushDataReturnValue returnValue = null;
                    if (packet instanceof IMetaDataPacket) {
                        returnValue = broadcastMetadata((IMetaDataPacket) packet);
                    } else {
                    }
                    _objInputStream.readStreamHeader();
                    packet = (IStreamPacket) _objInputStream.readObject();
                }
                _buffer.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (count < 0) {
        }
    }

    private PushDataReturnValue broadcastMetadata(IMetaDataPacket packet) {
        for (OutputPort outPort : getOutputPortsImpl()) {
            outPort.enqueueMetaData(packet);
        }
        return PushDataReturnValue.BOUNDARY_NOT_YET_REACHED;
    }

    public AbstractCheckPoint checkpoint() {
        return null;
    }

    public void restart(AbstractCheckPoint checkpoint) {
    }

    public int getLocalPort() {
        return _localPort;
    }

    public void setLocalPort(int port) {
        _localPort = port;
    }

    public String getAddress() {
        return _address;
    }

    public void setAddress(String address) {
        _address = address;
    }
}

package com.ohua.clustering.operators;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import com.ohua.checkpoint.framework.operatorcheckpoints.AbstractCheckPoint;
import com.ohua.checkpoint.framework.operatorcheckpoints.EmptyCheckpoint;
import com.ohua.clustering.client.LocalConnectionManager;
import com.ohua.clustering.protocols.SimpleWriterReaderPacketProtocol;
import com.ohua.engine.data.model.daapi.InputPortControl;
import com.ohua.engine.flowgraph.elements.packets.AbstractDataPacket;
import com.ohua.engine.operators.categories.OutputIOOperator;

public class SimpleNetworkWriterOperator extends AbstractNetworkOperator implements OutputIOOperator {

    private String _remoteIP = null;

    private int _remorePort = -1;

    private ByteBuffer _byteBuffer = null;

    private boolean _hasUnsendDataInBuffer = false;

    private InputPortControl _inPortControl = null;

    @Override
    public void cleanup() {
        System.out.println("CLEANUP in writer");
        writeToNetwork();
        getLogger().log(Level.ALL, "end packet send (over the wire) from " + getOperatorName() + " to " + _remoteIP + ":" + _remorePort);
        System.out.println("end packet send (over the wire) from " + getOperatorName() + " to " + _remoteIP + ":" + _remorePort);
        _inPortControl = getDataLayer().getInputPortController("input");
    }

    @Override
    public void prepare() {
        _byteBuffer = ByteBuffer.allocateDirect(1024);
    }

    @Override
    public void runProcessRoutine() {
        if (!cleanPreviousBufferContent()) {
            LocalConnectionManager.getInstance().waitingForWrite(this);
            return;
        }
        while (_inPortControl.next()) {
            getLogger().log(Level.ALL, "sending ...");
            getLogger().log(Level.ALL, "packet send over wire: " + _inPortControl.dataToString("XML"));
        }
    }

    private boolean cleanPreviousBufferContent() {
        if (!_hasUnsendDataInBuffer) {
            return true;
        }
        return writeToNetworkNoFlip();
    }

    public boolean sendDataDownstreamOverWire(AbstractDataPacket data) {
        SimpleWriterReaderPacketProtocol.writeDataPacket(_byteBuffer, data);
        return writeToNetwork();
    }

    public boolean writeToNetwork() {
        int amountOfBytesToBeSent = _byteBuffer.position();
        _byteBuffer.flip();
        int bytesWritten;
        try {
            bytesWritten = getChannel().write(_byteBuffer);
            if (bytesWritten != amountOfBytesToBeSent) {
                _hasUnsendDataInBuffer = true;
                return false;
            }
            _byteBuffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean writeToNetworkNoFlip() {
        if (!_byteBuffer.hasRemaining()) {
            throw new RuntimeException("Invariant broken: cleaning empty buffer!");
        }
        int amountOfBytesToBeSent = _byteBuffer.remaining();
        int bytesWritten = 0;
        try {
            bytesWritten = getChannel().write(_byteBuffer);
            if (bytesWritten != amountOfBytesToBeSent) {
                return false;
            }
            _byteBuffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
        _hasUnsendDataInBuffer = false;
        return true;
    }

    /**
   * This operator is completely stateless.
   */
    public AbstractCheckPoint checkpoint() {
        getLogger().log(Level.ALL, "CHECKPOINT OF SimpleNetworkWriter taken");
        EmptyCheckpoint ec = new EmptyCheckpoint();
        return ec;
    }

    /**
   * As I said: The operator is stateless!
   */
    public void restart(AbstractCheckPoint checkpoint) {
    }

    public boolean isLastMessage() {
        return false;
    }

    public String getRemoteIP() {
        return _remoteIP;
    }

    public void setRemoteIP(String remoteip) {
        _remoteIP = remoteip;
    }

    public int getRemotePort() {
        return _remorePort;
    }

    public void setRemorePort(int port) {
        _remorePort = port;
    }

    public ByteBuffer getByteBuffer() {
        return _byteBuffer;
    }

    private SocketChannel _metadataChannel = null;

    public void setMetadataChannel(SocketChannel channel) {
        _metadataChannel = channel;
    }

    public SocketChannel getMetadataChannel() {
        return _metadataChannel;
    }
}

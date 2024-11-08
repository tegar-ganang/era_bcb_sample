package com.ohua.clustering.operators;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import com.ohua.checkpoint.framework.markerpackets.FastTravelerMarkerPacket;
import com.ohua.checkpoint.framework.operatorcheckpoints.AbstractCheckPoint;
import com.ohua.checkpoint.framework.operatorcheckpoints.EmptyCheckpoint;
import com.ohua.clustering.client.LocalConnectionManager;
import com.ohua.clustering.protocols.SimpleWriterReaderPacketProtocol;
import com.ohua.clustering.testing.Experimenter;
import com.ohua.engine.extension.points.PacketExtensionLayer.DataSignal;
import com.ohua.engine.flowgraph.elements.operator.Arc;
import com.ohua.engine.flowgraph.elements.operator.OutputPort;
import com.ohua.engine.flowgraph.elements.operator.OperatorCore.PushDataReturnValue;
import com.ohua.engine.flowgraph.elements.packets.EndOfStreamPacket;
import com.ohua.engine.flowgraph.elements.packets.IMetaDataPacket;
import com.ohua.engine.flowgraph.elements.packets.IStreamPacket;
import com.ohua.engine.graphanalysis.cycleDetection.ICycleDetectionMarker;
import com.ohua.engine.operators.categories.InputIOOperator;

/**
 * The level of this operator will be just the same as the level of the operator upstream of the
 * network writer on the other node!
 */
public class SimpleNetworkReaderOperator extends AbstractNetworkOperator implements InputIOOperator {

    private ByteBuffer _buffer = null;

    private ByteBuffer _tmpBuffer = null;

    private ByteBuffer _dataBuffer = null;

    private ByteBuffer _metadataBuffer = null;

    private int _localPort = -1;

    private String _address = null;

    private SimpleWriterReaderPacketProtocol _protocol = null;

    private Set<String> _receivedFTs = new HashSet<String>();

    @Override
    public void prepare() {
        _dataBuffer = ByteBuffer.allocateDirect(1024);
        _dataBuffer.flip();
        _tmpBuffer = ByteBuffer.allocateDirect(10);
        _metadataBuffer = ByteBuffer.allocateDirect(128);
        _metadataBuffer.flip();
        _buffer = _dataBuffer;
    }

    @Override
    public void runProcessRoutine() {
    }

    private void activateWaitingOnChannels() {
        if (getDataChannel() != null) {
            LocalConnectionManager.getInstance().waitingForRead(getDataChannel());
        }
        if (getMetadataChannel() != null) {
            LocalConnectionManager.getInstance().waitingForRead(getMetadataChannel());
        }
    }

    private boolean isChannelReady() {
        return !_activeChannels.isEmpty();
    }

    private PushDataReturnValue handleFT(FastTravelerMarkerPacket packet) {
        if (_receivedFTs.contains(packet.getId())) {
            _receivedFTs.remove(packet.getId());
            return PushDataReturnValue.BOUNDARY_NOT_YET_REACHED;
        }
        _receivedFTs.add(packet.getId());
        return broadcastMetadata(packet);
    }

    private PushDataReturnValue handleCycleDetectionPacket(IStreamPacket packet) {
        PushDataReturnValue returnValue;
        int readerLevel = ((ICycleDetectionMarker) packet).getReaderLevel();
        for (OutputPort outPort : getOutputPortsImpl()) {
            for (Arc outArc : outPort.getOutgoingArcs()) {
                outArc.setLevel(readerLevel + 1);
            }
        }
        return null;
    }

    /**
   * Returns true iff all outgoing arcs won't see data anymore.
   * <p>
   * Assumption for now: There is only one output port with one outgoing arc!
   * @param packet
   * @return
   */
    private boolean handleEOSPacket(EndOfStreamPacket packet) {
        System.out.println("EOS packet received! id=" + packet.getLevelToClose());
        boolean done = true;
        OutputPort outPort = getOutputPortsImpl().get(0);
        Arc outArc = outPort.getOutgoingArcs().get(0);
        if (packet.getLevelToClose() < outArc.getTarget().getLevel()) {
            done = false;
            outPort.sendDataPacketNew(packet);
        }
        return done;
    }

    private void printPacket(IStreamPacket packet) {
        if (packet instanceof DataSignal) {
            Experimenter.getInstance().addArrivalTime();
        } else {
            getLogger().log(Level.ALL, "received packet: " + packet);
        }
    }

    private void prepareBufferForNewRead() {
        _tmpBuffer.clear();
        if (_buffer.hasRemaining()) {
            _tmpBuffer = _tmpBuffer.put(_buffer);
        }
        _buffer.clear();
        _tmpBuffer.flip();
        _buffer.put(_tmpBuffer);
    }

    private PushDataReturnValue broadcastMetadata(IMetaDataPacket packet) {
        for (OutputPort outPort : getOutputPortsImpl()) {
            outPort.enqueueMetaData(packet);
            for (Arc outArc : outPort.getOutgoingArcs()) {
            }
        }
        return PushDataReturnValue.BOUNDARY_NOT_YET_REACHED;
    }

    /**
   * This is a stateless operator.
   */
    public AbstractCheckPoint checkpoint() {
        getLogger().log(Level.ALL, "CHECKPOINT OF SimpleNetworkReader taken");
        EmptyCheckpoint ec = new EmptyCheckpoint();
        return ec;
    }

    /**
   * As I said: The operator is stateless!
   */
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

    public SimpleWriterReaderPacketProtocol getProtocol() {
        return _protocol;
    }

    public void setProtocol(SimpleWriterReaderPacketProtocol protocol) {
        _protocol = protocol;
    }

    private SocketChannel _dataChannel = null;

    private SocketChannel _metaDataChannel = null;

    private PriorityBlockingQueue<SocketChannel> _activeChannels = new PriorityBlockingQueue<SocketChannel>(3, new ChannelComparator());

    class ChannelComparator implements Comparator<SocketChannel> {

        public int compare(SocketChannel arg0, SocketChannel arg1) {
            if (arg0.socket().getPort() > arg1.socket().getPort()) {
                return -1;
            }
            return 1;
        }
    }

    @Override
    public void setChannel(SocketChannel channel) {
        if (channel == null) {
            throw new RuntimeException("Invariant broken: channel set is null!");
        }
        if (_dataChannel == null) {
            _dataChannel = channel;
            return;
        }
        _metaDataChannel = channel;
        if (_dataChannel == _metaDataChannel) {
            throw new RuntimeException("Invariant broken: Data and Metadata channel in reader are the same!");
        }
    }

    public void activateChannel(SocketChannel channel) {
        if (channel == null) {
            throw new RuntimeException("Invariant broken: channel to be activated is null!");
        }
        if (channel == _metaDataChannel) {
        }
        _activeChannels.put(channel);
    }

    @Override
    public SocketChannel getChannel() {
        SocketChannel poll = _activeChannels.poll();
        if (poll == _dataChannel) {
            _buffer = _dataBuffer;
        } else if (poll == _metaDataChannel) {
            _buffer = _metadataBuffer;
        } else {
            throw new RuntimeException("Unknown channel was activated!");
        }
        return poll;
    }

    @Override
    public void cleanup() {
        try {
            _metaDataChannel.close();
            _dataChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
   * @return
   */
    protected SocketChannel getMetadataChannel() {
        return _metaDataChannel;
    }

    protected void readMetaData(SocketChannel channel) {
        try {
            int count = 1;
            while (count > 0) {
                _tmpBuffer.clear();
                count = channel.read(_tmpBuffer);
                _tmpBuffer.flip();
            }
            LocalConnectionManager.getInstance().waitingForRead(channel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SocketChannel getDataChannel() {
        return _dataChannel;
    }
}

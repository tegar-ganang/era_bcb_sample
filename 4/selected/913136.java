package com.ohua.clustering.operators;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import com.ohua.checkpoint.framework.operatorcheckpoints.AbstractCheckPoint;
import com.ohua.clustering.network.OhuaSocketFactory;
import com.ohua.clustering.serialization.NetworkOutputStream;
import com.ohua.engine.flowgraph.elements.packets.IStreamPacket;
import com.ohua.engine.operators.categories.OutputIOOperator;

public class NetworkWriterOperator extends AbstractNetworkOperator implements OutputIOOperator {

    private NetworkOutputStream _outStream = null;

    private Socket _socket = null;

    private String _remoteIP = null;

    private int _remorePort = -1;

    @Override
    public void cleanup() {
        getLogger().log(Level.ALL, "end packet send (over the wire) from " + getOperatorName() + " to " + _remoteIP + ":" + _remorePort);
        try {
            _outStream.flush();
            _outStream.close();
            _socket.getChannel().close();
            _socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void prepare() {
        try {
            _socket = OhuaSocketFactory.getInstance().createSocket(_remoteIP, _remorePort);
            _socket.getChannel().configureBlocking(false);
            _socket.setOOBInline(true);
            setChannel(_socket.getChannel());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        _outStream = getOutputStream();
    }

    @Override
    public void runProcessRoutine() {
        int amountSent = 0;
    }

    public void sendDataDownstreamOverWire(IStreamPacket data) {
        try {
            _outStream.writeObject(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AbstractCheckPoint checkpoint() {
        return null;
    }

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

    public int getRemorePort() {
        return _remorePort;
    }

    public void setRemorePort(int port) {
        _remorePort = port;
    }
}

package org.yawlfoundation.yawl.procletService.models.procletModel;

import org.yawlfoundation.yawl.procletService.persistence.StoredPortConnection;

public class PortConnection {

    private ProcletPort iPort = null;

    private ProcletPort oPort = null;

    private String channel = "";

    public PortConnection(ProcletPort iPort, ProcletPort oPort, String channel) {
        this.iPort = iPort;
        this.oPort = oPort;
        this.channel = channel;
    }

    public ProcletPort getIPort() {
        return this.iPort;
    }

    public ProcletPort getOPort() {
        return this.oPort;
    }

    public String getChannel() {
        return this.channel;
    }

    public String toString() {
        return iPort.getPortID() + "," + oPort.getPortID();
    }

    public StoredPortConnection newStoredPortConnection() {
        return new StoredPortConnection(iPort.getPortID(), channel, oPort.getPortID());
    }
}

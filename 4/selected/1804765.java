package org.yawlfoundation.yawl.procletService.models.procletModel;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import org.yawlfoundation.yawl.procletService.persistence.DBConnection;
import org.yawlfoundation.yawl.procletService.persistence.StoredPortConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PortConnections extends DirectedSparseGraph {

    private static PortConnections pconns = null;

    private List<PortConnection> pcs = new ArrayList<PortConnection>();

    private List<String> channels = new ArrayList<String>();

    private PortConnections() {
    }

    public static PortConnections getInstance() {
        if (pconns == null) {
            pconns = new PortConnections();
            pconns.buildPConnsFromDB();
        }
        return pconns;
    }

    public void addChannel(String channel) {
        if (!this.channels.contains(channel)) {
            this.channels.add(channel);
        }
    }

    public void removeChannel(String channel) {
        this.channels.remove(channel);
    }

    public List<String> getChannels() {
        return this.channels;
    }

    public void addPortConnection(PortConnection conn) {
        if (!this.pcs.contains(conn)) {
            this.pcs.add(conn);
            this.addVertex(conn.getIPort());
            this.addVertex(conn.getOPort());
            this.addEdge(conn, conn.getIPort(), conn.getOPort(), EdgeType.DIRECTED);
        }
    }

    public void deletePortConnection(PortConnection pc) {
        this.pcs.remove(pc);
        this.removeVertex(pc.getIPort());
        this.removeVertex(pc.getOPort());
        this.removeEdge(pc);
    }

    public List<PortConnection> getPortConnections() {
        return this.pcs;
    }

    public PortConnection getPortConnectionIPort(PortConnection iPort) {
        Iterator<PortConnection> it = pcs.iterator();
        while (it.hasNext()) {
            PortConnection next = it.next();
            if (next.getIPort().equals(iPort)) {
                return next;
            }
        }
        return null;
    }

    public PortConnection getPortConnectionIPort(String iPortID) {
        Iterator<PortConnection> it = pcs.iterator();
        while (it.hasNext()) {
            PortConnection next = it.next();
            if (next.getIPort().getPortID().equals(iPortID)) {
                return next;
            }
        }
        return null;
    }

    public PortConnection getPortConnectionOPort(PortConnection oPort) {
        Iterator<PortConnection> it = pcs.iterator();
        while (it.hasNext()) {
            PortConnection next = it.next();
            if (next.getOPort().equals(oPort)) {
                return next;
            }
        }
        return null;
    }

    public PortConnection getPortConnectionOPort(String oPortID) {
        Iterator<PortConnection> it = pcs.iterator();
        while (it.hasNext()) {
            PortConnection next = it.next();
            if (next.getOPort().getPortID().equals(oPortID)) {
                return next;
            }
        }
        return null;
    }

    public ProcletPort getIPort(String portID) {
        Iterator<PortConnection> it = pcs.iterator();
        while (it.hasNext()) {
            PortConnection port = it.next();
            if (port.getIPort().getPortID().equals(portID)) {
                return port.getIPort();
            }
        }
        return null;
    }

    public ProcletPort getOPort(String portID) {
        Iterator<PortConnection> it = pcs.iterator();
        while (it.hasNext()) {
            PortConnection port = it.next();
            if (port.getOPort().getPortID().equals(portID)) {
                return port.getOPort();
            }
        }
        return null;
    }

    public boolean buildPConnsFromDB() {
        pcs.clear();
        List<ProcletPort> ports = ProcletModels.getInstance().getPorts();
        List items = DBConnection.getObjectsForClass("StoredPortConnection");
        for (Object o : items) {
            StoredPortConnection spc = (StoredPortConnection) o;
            String iportid = spc.getInput();
            String channel = spc.getChannel();
            String oportid = spc.getOutput();
            ProcletPort iPort = null;
            ProcletPort oPort = null;
            for (ProcletPort port : ports) {
                if (port.getPortID().equals(iportid)) {
                    iPort = port;
                }
                if (port.getPortID().equals(oportid)) {
                    oPort = port;
                }
            }
            PortConnection pconn = new PortConnection(iPort, oPort, channel);
            addPortConnection(pconn);
        }
        for (PortConnection pconn : pcs) {
            if (!channels.contains(pconn.getChannel())) {
                channels.add(pconn.getChannel());
            }
        }
        return true;
    }

    public void persistPConns() {
        this.deletePConnsFromDB();
        for (PortConnection pConn : getPortConnections()) {
            DBConnection.insert(pConn.newStoredPortConnection());
        }
    }

    public void deletePConnsFromDB() {
        DBConnection.deleteAll("StoredPortConnection");
    }

    public static void main(String[] args) {
        PortConnections pconns = PortConnections.getInstance();
        pconns.buildPConnsFromDB();
        pconns.persistPConns();
        System.out.println("done");
    }
}

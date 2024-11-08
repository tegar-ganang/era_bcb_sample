package org.psepr.PsEPRServer.LeasePeer;

import java.lang.Thread;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import org.psepr.PsEPRServer.ConnectionManager;
import org.psepr.PsEPRServer.ConnectionReader;
import org.psepr.PsEPRServer.Connections;
import org.psepr.PsEPRServer.DebugLogger;
import org.psepr.PsEPRServer.Global;
import org.psepr.PsEPRServer.Outter;
import org.psepr.PsEPRServer.ParamCollection;
import org.psepr.PsEPRServer.ParamFile;
import org.psepr.PsEPRServer.ParamServer;
import org.psepr.PsEPRServer.PsEPRServerException;
import org.psepr.PsEPRServer.ServerEvent;
import org.psepr.PsEPRServer.ServerEventChannel;
import org.psepr.PsEPRServer.Utilities;
import org.psepr.PsEPRServer.Peer.ConnectionReaderPeer;
import org.psepr.PsEPRServer.Peer.ConnectionReaderPeerFactory;
import org.psepr.PsEPRServer.Peer.PeerMessageNumbered;
import org.psepr.services.service.ChannelUseDescription;
import org.psepr.services.service.ChannelUseDescriptionCollection;
import org.psepr.services.service.EventDescription;
import org.psepr.services.service.EventDescriptionCollection;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.mxp1.MXParser;

/**
 * Manage the peer connections for the simple "Lease" management.
 * Two things are done: this listens to the connection port for incoming
 * peer connections and we keep a set of peers that we want to be
 * connected to and we keep trying to connect to them.
 * <p>
 * This only handles the connection.  The decision to make a connection,
 * who to talk to and what to say to them is handled at the event processing
 * level (ProcessorLeasePeer).  This code merely tries to keep a TCPIP connection
 * to the speficied peer.
 * </p>
 * <p>
 * This module also gets a subscription for the LeasePeer configuration channel
 * and receives configuration over that channel on who to be connected to.
 * </p>
 * 
 * @author Robert.Adams@intel.com
 */
public class ConnectionManagerLeasePeer extends ConnectionManager implements Outter {

    public static final String LEASE_PEER_CONFIG_NAMESPACE = "http://psepr.org/xs/lease/peer/servers";

    public static final String LEASE_PEER_CONFIG_CHANNEL = "/org/psepr/server/leasepeer/servers/";

    public static final int TYPE_PEER_LEASEPEER = 0x0010;

    private DebugLogger log;

    HashMap<String, PeerInfo> shouldPeer = new HashMap<String, PeerInfo>();

    /**
	 * 
	 */
    public ConnectionManagerLeasePeer() {
        super();
        this.init();
    }

    private void init() {
        log = new DebugLogger(managerName);
        this.connectionTypeName = "LeasePeer";
        this.setPort(23341);
        if (Global.serviceStatus != null) {
            ChannelUseDescriptionCollection cudc = Global.serviceStatus.getServiceDescription().getChannelUses();
            cudc.add(new ChannelUseDescription(LEASE_PEER_CONFIG_CHANNEL, "leases between message routing peers", 30L, 300L, new EventDescriptionCollection(new EventDescription(LEASE_PEER_CONFIG_NAMESPACE, "lease messages for access to messages from another peer")), new EventDescriptionCollection(new EventDescription(LEASE_PEER_CONFIG_NAMESPACE, "lease messages for access to messages from another peer"))));
        }
    }

    public void start() {
        super.start();
        class ProcessInboundConnections implements Runnable {

            public void run() {
                Thread.currentThread().setName("ProcessInboundConnection/" + managerName);
                processInboundConnections();
            }
        }
        Global.threads.execute(new ProcessInboundConnections());
        class CheckPeerConfiguration implements Runnable {

            public void run() {
                Thread.currentThread().setName("CheckPeerConfiguration/" + managerName);
                checkPeerConfiguration();
            }
        }
        Global.threads.execute(new CheckPeerConfiguration());
        Global.routeTable.addRoute("LeasePeerConnectionParam", LEASE_PEER_CONFIG_CHANNEL, LEASE_PEER_CONFIG_NAMESPACE, ConnectionReader.TYPE_CLIENT, this);
    }

    /**
	 * return the status and configuration as an XML fragement
	 */
    public String getStatusXML() {
        StringBuffer buff = new StringBuffer();
        buff.append("<ConnectionManagerLeasePeer>");
        buff.append("</ConnectionManagerLeasePeer>");
        return buff.toString();
    }

    /**
	 * Accept peer connections, create new instances of ConnectionManagerLeasePeer
	 * for the peers and create a thread to process the input queue through
	 * the router.
	 */
    private void processInboundConnections() {
        ServerSocket sock;
        try {
            sock = new ServerSocket(iPort);
        } catch (Exception e) {
            throw new PsEPRServerException(managerName + ": cannot open ServerSocket:" + e.toString());
        }
        Socket peerSock = null;
        while (managerRunning) {
            try {
                if (!shouldAcceptPeerConnections()) {
                    log.log(log.IO, "shouldAcceptPeerConnections returned false so not accepting");
                    Thread.sleep(5000);
                    continue;
                }
                sock.setSoTimeout(1000);
                peerSock = sock.accept();
                if (peerSock != null) {
                    int remotePort = 0;
                    String remoteHost = "unknown";
                    try {
                        remotePort = peerSock.getPort();
                        remoteHost = peerSock.getInetAddress().getHostAddress();
                        remoteHost = peerSock.getInetAddress().getHostName();
                    } catch (Exception e) {
                        log.log(log.PEER, "processInboundConnection: exception getting remote name. remote host =" + remoteHost);
                    }
                    String remoteHostID = Connections.makeName(remoteHost, remotePort);
                    log.log(log.PEER, "processInboundConnection: Accepted Peer connection from " + remoteHostID);
                    ConnectionReaderPeer newReader = ConnectionReaderPeerFactory.getConnectionReaderPeer(null, remoteHost, remotePort, peerSock);
                    newReader.setConnectionType(ConnectionReader.TYPE_PEER + ConnectionReader.TYPE_IN + ConnectionManagerLeasePeer.TYPE_PEER_LEASEPEER);
                    newReader.setSocket(peerSock);
                    newReader.setRemoteHostID(remoteHostID);
                    newReader.setMessageProcessor(new PeerMessageNumbered());
                    Global.connections.addConnection(newReader);
                    newReader.connectionReaderStart();
                    peerSock = null;
                }
            } catch (SocketTimeoutException ste) {
                peerSock = null;
                continue;
            } catch (Exception e) {
                log.log(log.PEER, "processInboundConnections: error creating reader:" + e.toString());
            }
        }
        if (sock != null) {
            try {
                sock.close();
            } catch (Exception e) {
            }
            sock = null;
        }
        log.log(log.PEER, "processInboundConnections: leaving");
    }

    /**
	 * Private class used to hold peer info from the configuration file
	 */
    private class PeerInfo {

        private String host = null;

        private String hostAuthKey = null;

        private String hostID = null;

        private String connectionType = null;

        private int port = 0;

        public PeerInfo() {
        }

        public PeerInfo(String hh, int pp) {
            host = hh;
            port = pp;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getHostAuthKey() {
            return hostAuthKey;
        }

        public void setHostAuthKey(String hostAuthKey) {
            this.hostAuthKey = hostAuthKey;
        }

        public String getHostID() {
            return hostID;
        }

        public void setHostID(String hostID) {
            this.hostID = hostID;
        }

        public int getPort() {
            return port;
        }

        public String getConnectionType() {
            return connectionType;
        }

        public void setConnectionType(String connectionType) {
            this.connectionType = connectionType;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public void setPort(String pt) {
            try {
                this.port = Integer.parseInt(pt);
            } catch (Exception e) {
                this.port = 0;
            }
        }
    }

    /**
	 * All of the connections to the other lease peers are listed in the peerState
	 * array.  This routine keeps looping through that list and attempting to
	 * keep the connections alive.
	 */
    private void checkPeerConfiguration() {
        while (managerRunning) {
            try {
                if (Global.params().getParamString(ParamServer.LEASEPEER_CONFIGURL) != null) {
                    URL theURL = new URL(Global.params().getParamString(ParamServer.LEASEPEER_CONFIGURL));
                    URLConnection theConnection = theURL.openConnection();
                    Reader theReader = new InputStreamReader(theConnection.getInputStream());
                    HashMap<String, PeerInfo> shouldPeerNew = new HashMap<String, PeerInfo>();
                    if (Global.params().getParamString(ParamServer.LEASEPEER_CONFIG_FORMAT).equalsIgnoreCase("PERSITE")) {
                        ParamCollection paramCollection = new ParamCollection();
                        ParamFile para = new ParamFile(paramCollection);
                        para.parseFile(theReader);
                        String myParam = "/leasePeer/" + Global.serviceHostname + "/";
                        Global.myID = paramCollection.getParam(myParam + "ID", null);
                        Global.myAuthKey = paramCollection.getParam(myParam + "authKey", null);
                        if (Global.myID == null || Global.myAuthKey == null) {
                            log.log(log.BADERROR, "I cannot configure -- I am not in the LeasePeer file. Me=" + Global.serviceHostname);
                            Global.acceptConnections = false;
                        } else {
                            for (int ii = 0; ii < 100; ii++) {
                                String peerParam = "/leasePeer/" + Global.serviceHostname + "/peer" + ParamCollection.threeDig(ii);
                                String peerName = paramCollection.getParam(peerParam + "/peer", null);
                                if (peerName != null) {
                                    if (!peerName.equalsIgnoreCase(Global.serviceHostname)) {
                                        PeerInfo pi = new PeerInfo();
                                        pi.setHost(peerName);
                                        pi.setPort(paramCollection.getParam(peerParam + "/port", getPort()));
                                        pi.setHostID(paramCollection.getParam(peerParam + "/ID", null));
                                        pi.setHostAuthKey(paramCollection.getParam(peerParam + "/authKey", null));
                                        pi.setConnectionType(paramCollection.getParam(peerParam + "/connectionType", null));
                                        shouldPeerNew.put(peerName, pi);
                                    }
                                }
                            }
                            Global.acceptConnections = true;
                        }
                    }
                    this.shouldPeer = shouldPeerNew;
                    justifyConnections();
                }
            } catch (Exception e) {
                log.log(log.BADERROR, "Could not read LeasePeer config file:" + e.toString());
            }
            try {
                Thread.sleep((Global.params().getParamInt(ParamServer.LEASEPEER_CONFIG_CHECK_SEC) + Utilities.plusMinus(2)) * 1000);
            } catch (Exception e) {
            }
        }
        return;
    }

    /**
	 * Passed a Map of PeerInfo objects that we should be connected to,
	 * look through the current connections and connect or disconnect to
	 * those destinations.
	 */
    private void justifyConnections() {
        for (Iterator jj = shouldPeer.keySet().iterator(); jj.hasNext(); ) {
            try {
                PeerInfo pi = shouldPeer.get(jj.next());
                String peerName = pi.getHost();
                int peerPort = pi.getPort();
                log.log(log.PEERDETAIL, "justifyConnections: checking on connection to " + pi.getHostID());
                if (!Global.connections.hasConnection(pi.getHostID(), ConnectionReader.TYPE_OUT)) {
                    log.log(log.PEER, "justifyConnections: creating connection to " + pi.getHostID());
                    ConnectionReaderPeer newReader = ConnectionReaderPeerFactory.getConnectionReaderPeer(peerName, peerPort, null);
                    newReader.setConnectionType(ConnectionReader.TYPE_PEER + ConnectionManagerLeasePeer.TYPE_PEER_LEASEPEER + ConnectionReader.TYPE_OUT);
                    newReader.setMessageProcessor(new PeerMessageNumbered());
                    newReader.setRemoteHostID(pi.getHostID());
                    newReader.setRemoteHostAuthKey(pi.getHostAuthKey());
                    Global.connections.addConnection(newReader);
                    newReader.connectionReaderStart();
                }
            } catch (Exception e) {
                log.log(log.PEER, "justifyConnections: exception creating new:" + e.toString());
            }
        }
        Collection<ConnectionReader> conns = Global.connections.getConnectionsClone();
        for (Iterator<ConnectionReader> kk = conns.iterator(); kk.hasNext(); ) {
            try {
                ConnectionReader cr = kk.next();
                if (cr instanceof ConnectionReaderPeer) {
                    ConnectionReaderPeer crp = (ConnectionReaderPeer) cr;
                    if (!shouldPeer.containsKey(crp.getRemoteHostID())) {
                        if (crp.ifConnectionType(ConnectionReader.TYPE_PEER + ConnectionManagerLeasePeer.TYPE_PEER_LEASEPEER)) {
                            crp.checkConnection();
                            if (crp.ifState(ConnectionReader.STATE_DISCONNECTED)) {
                                log.log(log.PEER, "justifyConnections: should not be peering with " + crp.getRemoteHostID());
                                crp.connectionReaderStop();
                                crp.closeConnection();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.log(log.PEER, "justifyConnections: exception removing old:" + e.toString());
            }
        }
        return;
    }

    /**
	 * Routine to process configuration information sent on the leasePeer information channel
	 */
    public void send(ServerEvent se) {
        if (!(se instanceof ServerEventChannel)) {
            log.log(log.SERVICES, "Received server event of unknown type: " + se.getClass().getName());
            return;
        }
        ServerEventChannel sec = (ServerEventChannel) se;
        if (sec.getToChannel() == null || sec.getPayload() == null) {
            log.log(log.PEER, "Received peer message for no channel or no payload");
            return;
        }
        if (sec.getPayloadNamespace().equalsIgnoreCase(LEASE_PEER_CONFIG_NAMESPACE)) {
            processPeerConfig(se);
        } else {
            log.log(log.PEER, "Received message on peer channel that was not peer namespace");
        }
        return;
    }

    /**
	 * The event seems to be the peer configuration event.
	 * Parse it and pass the information up to the ConnectionManager.
	 * <p>
	 * We are expecting a body like:
	 * <pre>
	 * &lt;leasePeerConnections&gt;
	 *   &lt;connection&gt;
	 *     &lt;peer&gt;hostname&lt;/peer&gt;
	 *     &lt;port&gt;portNumber&lt;/port&gt;
	 *   &lt;/connection&gt;
	 *   ... many more 'connection's
	 * &lt;/leasePeerConnections&gt;
	 * </pre>
	 * </p>
	 * @param se
	 */
    private void processPeerConfig(ServerEvent se) {
        if (!(se instanceof ServerEventChannel)) {
            log.log(log.SERVICES, "Received server event of unknown type: " + se.getClass().getName());
            return;
        }
        ServerEventChannel sec = (ServerEventChannel) se;
        HashMap<String, PeerInfo> shouldPeerNew = new HashMap<String, PeerInfo>();
        PeerInfo pi = null;
        try {
            XmlPullParser parser = new MXParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(new StringReader(sec.getPayload()));
            int et = parser.next();
            while (et != XmlPullParser.END_DOCUMENT) {
                if (et == XmlPullParser.START_TAG) {
                    String elem = parser.getName();
                    if (elem.equalsIgnoreCase("leasePeerConnections")) {
                    } else if (elem.equalsIgnoreCase("connection")) {
                        pi = new PeerInfo();
                    } else if (elem.equalsIgnoreCase("peer")) {
                        pi.setHost(Utilities.cleanXMLText(parser.getText()));
                    } else if (elem.equalsIgnoreCase("port")) {
                        pi.setPort(Utilities.cleanXMLText(parser.getText()));
                    } else if (elem.equalsIgnoreCase("ID")) {
                        pi.setHostID(Utilities.cleanXMLText(parser.getText()));
                    } else if (elem.equalsIgnoreCase("authKey")) {
                        pi.setHostAuthKey(Utilities.cleanXMLText(parser.getText()));
                    }
                } else if (et == XmlPullParser.END_TAG) {
                    if (parser.getName().equalsIgnoreCase("connection")) {
                        if (pi != null) {
                            shouldPeerNew.put(pi.host, pi);
                            pi = null;
                        }
                    } else if (parser.getName().equalsIgnoreCase("payload")) {
                        break;
                    }
                }
                if (et != XmlPullParser.END_DOCUMENT) {
                    et = parser.next();
                }
            }
        } catch (Exception e) {
            log.log(log.PEER, "error parsing lease:" + e.toString());
            throw new PsEPRServerException("LeasePeerProcessor: failure parsing peer s2s:" + e.toString());
        }
        this.shouldPeer = shouldPeerNew;
        return;
    }
}

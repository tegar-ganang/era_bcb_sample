package net.sf.peervibes.rendezvous.client;

import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.EventQualifier;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.common.RegisterSocketEvent;
import net.sf.appia.protocols.utils.ParseUtils;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;
import net.sf.peervibes.protocols.membership.OverlayNetwork;
import net.sf.peervibes.protocols.p2p.events.P2PInitEvent;
import net.sf.peervibes.rendezvous.events.ContactReplyEvent;
import net.sf.peervibes.rendezvous.events.ContactRequestEvent;
import net.sf.peervibes.rendezvous.events.HeartbeatTimer;
import net.sf.peervibes.rendezvous.events.RendezVousHeartbeatEvent;
import net.sf.peervibes.utils.Peer;

/**
 * The <i>peer-to-peer communication</i> RendezVous (client) Session.
 * <br>
 * This Session implements the two basic functionalities of this layer:
 * <br>
 * It provides the local node with one (or more) contact nodes  (used by the 
 * node to join a given overlay) which are extracted from a RendezVousServer
 * (or several) that are executed in well known hosts. This request supports a
 * high level interface, which allows the requesting local peer to specify constraints
 * to the contact node, for instance, that it should be physically close; 
 * <br>
 * It is also used to periodically notify some of these RendezVous servers that 
 * the local node is still active and in which overlays networks, making
 * it a potential target for join requests of new peers.
 *
 * @version 0.1
 * @author Joao Leitao
 */
public class RendezVousSession extends Session implements InitializableSession {

    private SocketAddress server;

    private ArrayList<OverlayNetwork> activeOverlays = new ArrayList<OverlayNetwork>();

    private Random random;

    private double heartbeatThreshold = 0.4;

    private Channel timerChannel = null;

    private List<Channel> channelList = new ArrayList<Channel>(2);

    private Peer myPeer;

    public RendezVousSession(Layer layer) {
        super(layer);
        random = new Random(System.currentTimeMillis());
    }

    public void init(SessionProperties parameters) {
        if (parameters.containsKey("rvn_address")) {
            try {
                server = ParseUtils.parseSocketAddress(parameters.getProperty("rvn_address"), null, 12000);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (parameters.containsKey("heartbeat_threshold")) {
            heartbeatThreshold = parameters.getDouble("heartbeat_threshold");
            if (heartbeatThreshold < 0) heartbeatThreshold = 0; else if (heartbeatThreshold > 1) heartbeatThreshold = 1;
        }
    }

    @Override
    public void handle(Event ev) {
        if (ev instanceof ChannelInit) handleChannelInit((ChannelInit) ev); else if (ev instanceof ChannelClose) handleChannelClose((ChannelClose) ev); else if (ev instanceof RegisterSocketEvent) handleRSE((RegisterSocketEvent) ev); else if (ev instanceof ContactRequestEvent) handleRequest((ContactRequestEvent) ev); else if (ev instanceof ContactReplyEvent) handleReply((ContactReplyEvent) ev); else if (ev instanceof P2PInitEvent) handleP2PInit((P2PInitEvent) ev); else if (ev instanceof HeartbeatTimer) handleHBTimer((HeartbeatTimer) ev); else try {
            ev.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    private void handleHBTimer(HeartbeatTimer ev) {
        if (random.nextInt(100) < heartbeatThreshold) sendHeartbeat(ev.getChannel());
    }

    private void sendHeartbeat(Channel c) {
        RendezVousHeartbeatEvent hb;
        try {
            hb = new RendezVousHeartbeatEvent(c, Direction.DOWN, this);
            hb.dest = server;
            hb.setPeerID(myPeer);
            hb.addOverlayNetwork(activeOverlays);
            hb.serializeToMessage();
            hb.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    private void handleP2PInit(P2PInitEvent ev) {
        myPeer = ev.getLocalPeer();
        try {
            ev.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    private void handleRequest(ContactRequestEvent ev) {
        ev.serializeToMessage();
        ev.dest = server;
        try {
            ev.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    private void handleReply(ContactReplyEvent ev) {
        ev.unserializeToMessage();
        try {
            ev.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    private void handleRSE(RegisterSocketEvent ev) {
        try {
            ev.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    private void handleChannelClose(ChannelClose ev) {
        channelList.remove(ev.getChannel());
        if (timerChannel != null && timerChannel.equals(ev.getChannel())) {
            try {
                new HeartbeatTimer("heartbeattimer", 10000, ev.getChannel(), Direction.DOWN, this, EventQualifier.OFF).go();
                timerChannel = null;
            } catch (AppiaEventException e) {
                e.printStackTrace();
            } catch (AppiaException e) {
                e.printStackTrace();
            }
        }
        try {
            ev.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
        if (channelList.size() > 0) {
            timerChannel = channelList.get(0);
            try {
                new HeartbeatTimer("heartbeattimer", 10000, timerChannel, Direction.DOWN, this, EventQualifier.ON).go();
            } catch (AppiaEventException e) {
                timerChannel = null;
                e.printStackTrace();
            } catch (AppiaException e) {
                timerChannel = null;
                e.printStackTrace();
            }
        }
    }

    private void handleChannelInit(ChannelInit ev) {
        try {
            ev.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
        channelList.add(ev.getChannel());
        if (timerChannel == null) {
            try {
                new HeartbeatTimer("heartbeattimer", 10000, ev.getChannel(), Direction.DOWN, this, EventQualifier.ON).go();
                timerChannel = ev.getChannel();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            } catch (AppiaException e) {
                e.printStackTrace();
            }
        }
    }
}

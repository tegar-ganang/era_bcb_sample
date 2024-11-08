package net.sf.peervibes.test.protocols.pushbcast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;
import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.EventQualifier;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;
import net.sf.peervibes.protocols.membership.events.GetPeersEvent;
import net.sf.peervibes.protocols.p2p.events.BroadcastSendableEvent;
import net.sf.peervibes.protocols.p2p.events.P2PInitEvent;
import net.sf.peervibes.test.protocols.pushbcast.events.CleanUpTimer;
import net.sf.peervibes.utils.Peer;

public class SimplePushBCastSession extends Session implements InitializableSession {

    public static int fanout;

    public static int cleanUpPeriod = 20000;

    private static final int DEFAULT_FANOUT = 10;

    private Peer myNode;

    private Channel channel;

    private LinkedList<UUID> deliveredMessages;

    private LinkedList<BroadcastSendableEvent> pendingMessages;

    private int lastSeen;

    public SimplePushBCastSession(Layer layer) {
        super(layer);
        this.myNode = null;
        this.channel = null;
        this.deliveredMessages = new LinkedList<UUID>();
        this.pendingMessages = new LinkedList<BroadcastSendableEvent>();
        SimplePushBCastSession.fanout = SimplePushBCastSession.DEFAULT_FANOUT;
        this.lastSeen = 0;
    }

    public void init(SessionProperties args) {
        if (args.containsKey("fanout")) SimplePushBCastSession.fanout = args.getInt("fanout");
        if (args.containsKey("cleanUpPeriod")) SimplePushBCastSession.cleanUpPeriod = args.getInt("cleanUpPeriod");
    }

    public void handle(Event ev) {
        if (ev instanceof BroadcastSendableEvent) handleBroadcastSendableEvent((BroadcastSendableEvent) ev); else if (ev instanceof GetPeersEvent) handleGetPeersEvent((GetPeersEvent) ev); else if (ev instanceof CleanUpTimer) handleGetPeersEvent((CleanUpTimer) ev); else if (ev instanceof P2PInitEvent) handleP2PInitEvent((P2PInitEvent) ev); else if (ev instanceof ChannelInit) {
            this.channel = ((ChannelInit) ev).getChannel();
            try {
                ev.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
        } else {
            try {
                ev.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleGetPeersEvent(CleanUpTimer ev) {
        for (int i = 0; i < this.lastSeen; i++) this.deliveredMessages.remove(0);
        this.lastSeen = this.deliveredMessages.size();
    }

    private void handleP2PInitEvent(P2PInitEvent ev) {
        if (ev.getDir() == Direction.DOWN) this.myNode = ev.getLocalPeer();
        try {
            new CleanUpTimer("PushBCast_CleanUp", SimplePushBCastSession.cleanUpPeriod, this.channel, Direction.DOWN, this, EventQualifier.ON).go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        } catch (AppiaException e) {
            e.printStackTrace();
        }
        try {
            ev.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    private void handleGetPeersEvent(GetPeersEvent ev) {
        if (ev.getDir() == Direction.UP) {
            if (this.pendingMessages.size() == 0) {
                try {
                    ev.go();
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                }
            } else {
                BroadcastSendableEvent next = this.pendingMessages.removeFirst();
                if (ev.getN() > 0) {
                    next.setDir(Direction.DOWN);
                    next.setSourceSession(this);
                    next.dest = new ArrayList<Peer>(ev.getAnswerCollection());
                    try {
                        next.init();
                        next.go();
                    } catch (AppiaEventException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        try {
            ev.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    private void handleBroadcastSendableEvent(BroadcastSendableEvent ev) {
        try {
            if (ev.getDir() == Direction.DOWN) {
                if (ev.getOriginalSender() == null) ev.setOriginalSender(this.myNode);
                if (ev.getSender() == null) ev.setSender(this.myNode);
                this.pendingMessages.addLast((BroadcastSendableEvent) ev.cloneEvent());
                GetPeersEvent req = new GetPeersEvent(this.channel, Direction.DOWN, this, SimplePushBCastSession.fanout);
                req.go();
                this.deliveredMessages.addLast(ev.getMsgID());
                ev.setDir(Direction.invert(ev.getDir()));
                ev.setSourceSession(this);
                ev.init();
                ev.go();
            } else {
                if (!this.deliveredMessages.contains(ev.getMsgID())) {
                    this.deliveredMessages.addLast(ev.getMsgID());
                    this.pendingMessages.addLast((BroadcastSendableEvent) ev.cloneEvent());
                    GetPeersEvent req = new GetPeersEvent(this.channel, Direction.DOWN, this, SimplePushBCastSession.fanout, ev.getSender());
                    req.go();
                    ev.go();
                }
            }
        } catch (AppiaEventException e) {
            e.printStackTrace();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }
}

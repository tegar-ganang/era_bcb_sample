package net.sf.peervibes.rendezvous.server;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.common.RegisterSocketEvent;
import net.sf.peervibes.protocols.membership.OverlayNetwork;
import net.sf.peervibes.rendezvous.events.ContactReplyEvent;
import net.sf.peervibes.rendezvous.events.ContactRequestEvent;
import net.sf.peervibes.rendezvous.events.RendezVousHeartbeatEvent;
import net.sf.peervibes.utils.Peer;

/**
 * The <i>peer-to-peer communication</i> RendezVousServer session.
 * <br>
 * This session implements the basic operation of the RendezVousServer layer.
 * <br>
 *
 * @version 0.1
 * @author Joao Leitao
 */
public class RendezVousServerSession extends Session {

    private static final int MAX_LIST_SIZE = 20;

    private Hashtable<OverlayNetwork, List<Peer>> knownPeers = new Hashtable<OverlayNetwork, List<Peer>>();

    public RendezVousServerSession(Layer layer) {
        super(layer);
    }

    @Override
    public void handle(Event ev) {
        if (ev instanceof ChannelInit) handleChannelInit((ChannelInit) ev); else if (ev instanceof ChannelClose) handleChannelClose((ChannelClose) ev); else if (ev instanceof RegisterSocketEvent) handleRSE((RegisterSocketEvent) ev); else if (ev instanceof ContactRequestEvent) handleRequest((ContactRequestEvent) ev); else if (ev instanceof RendezVousHeartbeatEvent) handleHeartbeat((RendezVousHeartbeatEvent) ev); else try {
            ev.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    private void handleHeartbeat(RendezVousHeartbeatEvent ev) {
        ev.unserializeFromMessage();
        Peer peer = ev.getPeerID();
        for (OverlayNetwork overlay : ev.getOverlayNetworks()) {
            List<Peer> peerList = null;
            if ((peerList = knownPeers.get(overlay)) == null) addNewOverlay(overlay, peer); else {
                peerList.remove(peer);
                peerList.add(peer);
                while (peerList.size() > MAX_LIST_SIZE) peerList.remove(0);
            }
        }
    }

    private void handleRequest(ContactRequestEvent ev) {
        ev.unserializeFromMessage();
        List<Peer> list = knownPeers.get(ev.getTargetOverLay());
        int numContacts = ev.getNumberOfContacts();
        int listSize = list.size();
        List<Peer> subList = null;
        if (list != null) {
            if (listSize > numContacts) subList = new ArrayList<Peer>(list.subList(listSize - numContacts, listSize)); else {
                subList = new ArrayList<Peer>(list);
            }
            subList.remove(ev.getRequesterID());
        } else subList = new ArrayList<Peer>();
        try {
            ContactReplyEvent reply = new ContactReplyEvent(ev.getTargetOverLay(), subList, ev.getChannel(), Direction.invert(ev.getDir()), this);
            reply.serializeToMessage();
            reply.dest = ev.source;
            reply.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    private void handleRSE(RegisterSocketEvent ev) {
    }

    private void handleChannelClose(ChannelClose ev) {
        try {
            ev.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    private void handleChannelInit(ChannelInit ev) {
        try {
            ev.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    private void addNewOverlay(OverlayNetwork overlay, Peer peer) {
        List<Peer> list = new ArrayList<Peer>();
        list.add(peer);
        knownPeers.put(overlay, list);
    }
}
